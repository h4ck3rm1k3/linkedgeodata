package org.linkedgeodata.osm.osmosis.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.dao.nodestore.GeoRSSNodeMapper;
import org.linkedgeodata.dao.nodestore.NodePositionDAO;
import org.linkedgeodata.dao.nodestore.RDFNodePositionDAO;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.scripts.LiveSync;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.xml.common.CompressionActivator;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
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

		System.out.println("Trying live dump");

		Process process = Runtime
				.getRuntime()
				.exec("bzcat /home/raven/Documents/Data/openstreetmap.org/planet-100224.osm.bz2");
		InputStream inputStream = process.getInputStream();


		File outFile = new File("/tmp/lgdump.nt");
		OutputStream out = new FileOutputStream(outFile);

		File configFile = new File("config.ini");

		Map<String, String> config = LiveSync.loadIniFile(configFile);

		File osmConfigFile = new File(config.get("osmReplicationConfigPath")
				+ "/configuration.txt");
		LiveSync.loadIniFile(osmConfigFile, config);

		String publishDiffBaseName = config.get("publishDiffRepoPath");

		// LiveRDFDeltaPluginFactory factory.create();
		Connection conn = VirtuosoUtils.connect(
				config.get("rdfStore_hostName"),
				config.get("rdfStore_userName"),
				config.get("rdfStore_passWord"));

		String graphName = config.get("rdfStore_graphName");

		ISparulExecutor graphDAO = new VirtuosoJdbcSparulExecutor(conn,
				graphName);

		Connection nodeConn = PostGISUtil.connectPostGIS("localhost",
				"lgdnodes", "postgres", "postgres");

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

		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer = new OSMEntityToRDFTransformer(
				tagMapper, vocab);

		NodePositionDAO nodePositionDao = new NodePositionDAO("node_position");
		nodePositionDao.setConnection(nodeConn);

		GeoRSSNodeMapper nodeMapper = new GeoRSSNodeMapper(vocab);
		RDFNodePositionDAO rdfNodePositionDAO = new RDFNodePositionDAO(
				nodePositionDao, vocab, nodeMapper);

		// InputStream inputStream = new FileInputStream(new File(""));
		// inputStream = new
		// CompressionActivator(CompressionMethod.GZip).createCompressionInputStream(inputStream);

		IgnoreModifyDeleteDiffUpdateStrategy diffStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName,
				rdfNodePositionDAO);

		LiveDumpChangeSink dumpSink = new LiveDumpChangeSink(diffStrategy, out);

		// Load the entity tag filter
		TagFilter entityTagFilter = new TagFilter();
		entityTagFilter.load(new File(config.get("entityFilter")));

		EntityFilterPlugin entityFilterPlugin = new EntityFilterPlugin(
				new EntityFilter(entityTagFilter));

		TagFilter tagFilter = new TagFilter();
		tagFilter.load(new File(config.get("tagFilter")));

		TagFilterPlugin tagFilterPlugin = new TagFilterPlugin(tagFilter);

		tagFilterPlugin.setChangeSink(dumpSink);
		entityFilterPlugin.setChangeSink(tagFilterPlugin);

		Sink workFlow = new SinkToChangeSinkBridge(entityFilterPlugin);

		parser.parse(inputStream, new OsmHandler(workFlow, true));
		out.close();

	}

}
