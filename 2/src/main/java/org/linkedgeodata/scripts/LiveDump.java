package org.linkedgeodata.scripts;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.cache.util.CacheBulkMap;
import org.jboss.cache.util.DeltaBulkMap;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.dao.nodestore.GeoRSSNodeMapper;
import org.linkedgeodata.dao.nodestore.NodePositionDAO;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.osm.osmosis.plugins.EntityFilter;
import org.linkedgeodata.osm.osmosis.plugins.EntityFilterPlugin;
import org.linkedgeodata.osm.osmosis.plugins.INodeSerializer;
import org.linkedgeodata.osm.osmosis.plugins.LiveDumpChangeSink;
import org.linkedgeodata.osm.osmosis.plugins.OptimizedDiffUpdateStrategy;
import org.linkedgeodata.osm.osmosis.plugins.TagFilter;
import org.linkedgeodata.osm.osmosis.plugins.TagFilterPlugin;
import org.linkedgeodata.osm.osmosis.plugins.VirtuosoCommercialNodeSerializer;
import org.linkedgeodata.osm.osmosis.plugins.VirtuosoOseNodeSerializer;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.linkedgeodata.util.sparql.cache.DeltaGraph;
import org.linkedgeodata.util.sparql.cache.IGraph;
import org.linkedgeodata.util.sparql.cache.SparqlEndpointFilteredGraph;
import org.linkedgeodata.util.sparql.cache.TripleCacheIndexImpl;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.xml.v0_6.impl.OsmHandler;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;


class SinkToChangeSinkBridge
		implements Sink
{
	private ChangeSink	sink;

	public SinkToChangeSinkBridge(ChangeSink sink)
	{
		this.sink = sink;
	}

	@Override
	public void complete()
	{
		sink.complete();
	}

	@Override
	public void release()
	{
		sink.release();
	}

	@Override
	public void process(EntityContainer ec)
	{
		sink.process(new ChangeContainer(ec, ChangeAction.Create));
	}
}

/**
 * Note: This class is not for creating a dump from a Live-Sync'd database.
 * Instead it is used for creating the initial dump the live sync can work upon.
 * Also, it uses the same code as the live sync.
 * 
 * @author raven
 * 
 */
public class LiveDump
{
	private static SAXParser	parser	= createParser();

	private static Options cliOptions = new Options()
		.addOption("c", "config", true, "Config filename")
		.addOption("f", "dump file", true, "Dump filename");

	
	public static InputStream openFile(File file)
		throws IOException
	{
		String name = file.getName().toLowerCase();
		
		if(name.endsWith("bz2")) {
			Process process = Runtime
			.getRuntime()
			.exec("bzcat " + file.getAbsolutePath());
			return process.getInputStream();			
		}
		else {
			return new FileInputStream(file);
		}
	}
	
	private static SAXParser createParser()
	{
		try {
			return SAXParserFactory.newInstance().newSAXParser();

		} catch (ParserConfigurationException e) {
			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
		} catch (SAXException e) {
			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
		}
	}


	public static void main(String[] args) throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		System.out.println("Starting live dump");


		//File outFile = new File("/tmp/lgdump.nt");
		//OutputStream out = new FileOutputStream(outFile);

		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		String configFileName = commandLine.getOptionValue("c", "config.ini");
		File configFile = new File(configFileName);
		
		String osmFileName = commandLine.getOptionValue("f");
		File osmFile = new File(osmFileName);
		
		InputStream inputStream = openFile(osmFile);
		
		
		
		Map<String, String> config = LiveSync.loadIniFile(configFile);

		File osmConfigFile = new File(config.get("osmReplicationConfigPath")
				+ "/configuration.txt");
		LiveSync.loadIniFile(osmConfigFile, config);

		
		//LiveRDFDeltaPluginFactory factory.create();		
		Connection conn = VirtuosoUtils.connect(
				config.get("rdfStore_hostName"),
				config.get("rdfStore_userName"),
				config.get("rdfStore_passWord"));

		String graphName = config.get("rdfStore_graphName");
		

		ISparulExecutor graphDAO = new VirtuosoJdbcSparulExecutor(conn,
				graphName);

		Connection nodeConn = PostGISUtil.connectPostGIS(
				config.get("osmDb_hostName"),
				config.get("osmDb_dataBaseName"),
				config.get("osmDb_userName"),
				config.get("osmDb_passWord"));

		// RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(outputBaseName);
		// rdfDiffWriter = new RDFDiffWriter();

		InMemoryTagMapper tagMapper = new InMemoryTagMapper();

		tagMapper.load(new File(config.get("tagMappings")));

		boolean loadTagMappingsFromDb = false;
		if (loadTagMappingsFromDb) {
			Session session = TagMappingDB.getSession();
			Transaction tx = session.beginTransaction();

			for (Object o : session
					.createCriteria(AbstractTagMapperState.class).list()) {
				IOneOneTagMapper item = TagMapperInstantiator.getInstance()
						.instantiate((IEntity) o);

				tagMapper.add(item);
			}

			tx.commit();
		}

		// File diffRepo = new File("/tmp/lgddiff");
		// diffRepo.mkdirs();

		// RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(diffRepo, 0);
		INodeSerializer nodeSerializer = null;
		if(config.get("nodeSerializer").equalsIgnoreCase("georss")) {
			nodeSerializer = new VirtuosoOseNodeSerializer();
		} else if(config.get("nodeSerializer").equalsIgnoreCase("virtuoso")) {
			nodeSerializer = new VirtuosoCommercialNodeSerializer();
		}

		
		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer = new OSMEntityToRDFTransformer(
				tagMapper, vocab, nodeSerializer);

		
		String nodeTableName = config.get("nodePositionTableName");
		if(nodeTableName == null)
			throw new NullPointerException("Table name must not be null");

		
		NodePositionDAO nodePositionDaoCore = new NodePositionDAO(nodeTableName);
		
		nodePositionDaoCore.setConnection(nodeConn);

		CacheBulkMap<Long, Point2D> nodePositionDaoCache = CacheBulkMap.create(nodePositionDaoCore, 1000000, 1000000);
		DeltaBulkMap<Long, Point2D> nodePositionDao = DeltaBulkMap.create(nodePositionDaoCache);

		//NodePositionDAO rdfNodePositionDAO = new RDFNodePositionDAO(
		//		nodePositionDao, vocab, nodeMapper);

		// InputStream inputStream = new FileInputStream(new File(""));
		// inputStream = new
		// CompressionActivator(CompressionMethod.GZip).createCompressionInputStream(inputStream);

		ITransformer<Model, Model> postTransformer = LiveSync.getPostTransformer(config, vocab);
		

		// Load the entity tag filter
		TagFilter entityTagFilter = new TagFilter();
		entityTagFilter.load(new File(config.get("entityFilter")));

		EntityFilterPlugin entityFilterPlugin = new EntityFilterPlugin(
				new EntityFilter(entityTagFilter));

		TagFilter tagFilter = new TagFilter();
		tagFilter.load(new File(config.get("tagFilter")));

		
		TagFilter relevanceFilter = new TagFilter();
		relevanceFilter.load(new File(config.get("relevanceFilter")));
		
		
		IGraph baseGraph = new SparqlEndpointFilteredGraph(graphDAO, graphName);
		DeltaGraph deltaGraph = new DeltaGraph(baseGraph);

		// Create an index by s and o
		TripleCacheIndexImpl.create(baseGraph, 1000000, 1000, 1000000,
				new int[] { 0 });
		
		
		OptimizedDiffUpdateStrategy diffStrategy = new OptimizedDiffUpdateStrategy(vocab,
				entityTransformer, nodeSerializer, deltaGraph, nodePositionDao, relevanceFilter, postTransformer);

		LiveDumpChangeSink dumpSink = new LiveDumpChangeSink(diffStrategy, deltaGraph, nodePositionDao);

		TagFilterPlugin tagFilterPlugin = new TagFilterPlugin(tagFilter);
		
		tagFilterPlugin.setChangeSink(dumpSink);
		entityFilterPlugin.setChangeSink(tagFilterPlugin);

		Sink workFlow = new SinkToChangeSinkBridge(entityFilterPlugin);

		parser.parse(inputStream, new OsmHandler(workFlow, true));
		//out.close();

	}

}
