package org.linkedgeodata.scripts;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.linkedgeodata.osm.osmosis.plugins.EntityFilter;
import org.linkedgeodata.osm.osmosis.plugins.EntityFilterPlugin;
import org.linkedgeodata.osm.osmosis.plugins.IUpdateStrategy;
import org.linkedgeodata.osm.osmosis.plugins.OptimizedDiffUpdateStrategy;
import org.linkedgeodata.osm.osmosis.plugins.RDFDiffWriter;
import org.linkedgeodata.osm.osmosis.plugins.TagFilter;
import org.linkedgeodata.osm.osmosis.plugins.TagFilterPlugin;
import org.linkedgeodata.osm.osmosis.plugins.TreeSetDiff;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.URIUtil;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.linkedgeodata.util.sparql.cache.DeltaGraph;
import org.linkedgeodata.util.sparql.cache.IGraph;
import org.linkedgeodata.util.sparql.cache.SparqlEndpointFilteredGraph;
import org.linkedgeodata.util.sparql.cache.TripleCacheIndexImpl;
import org.linkedgeodata.util.sparql.cache.TripleIndexUtils;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.xml.common.CompressionActivator;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.impl.OsmChangeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.graph.TripleCache;
import com.hp.hpl.jena.graph.compose.Delta;
import com.hp.hpl.jena.rdf.model.Model;

/*
class EntityClassifier
	implements Transformer<Entity, String>
{
	private Predicate<EntityFilter> entityFilter
	
	@Override
	public String transform(Entity input)
	{
		
		
	}
	 
}
*/


class DiffResult
{
	private IDiff<Model> mainDiff;
	private TreeSetDiff<Node> nodeDiff;
	
	
	
	public DiffResult(IDiff<Model> mainDiff, TreeSetDiff<Node> nodeDiff)
	{
		super();
		this.mainDiff = mainDiff;
		this.nodeDiff = nodeDiff;
	}
	public IDiff<Model> getMainDiff()
	{
		return mainDiff;
	}
	public TreeSetDiff<Node> getNodeDiff()
	{
		return nodeDiff;
	}
	
	
}


public class LiveSync
{
	private static final Logger logger = LoggerFactory.getLogger(LiveSync.class);
	
    protected static Options cliOptions;

	private Map<String, String> config;
	
	private IUpdateStrategy diffStrategy;
	
	
	private long sequenceNumber;
	
	private String graphName;
	
	private String publishDiffBaseName;
	
	private SAXParser parser = createParser();

	private ISparulExecutor graphDAO;
	
	private ChangeSink workFlow;
	
	private NodePositionDAO nodePositionDao;

	

	//private RDFDiffWriter rdfDiffWriter;
	
	public static Map<String, String> loadIniFile(File file)
		throws IOException
	{
		Map<String, String> config = new HashMap<String, String>();
		
		loadIniFile(file, config);
		
		return config;
	}
	
	public static void loadIniFile(File file, Map<String, String> out)
		throws IOException
	{
		loadIniFile(new BufferedReader(new FileReader(file)), out);
	}
	
	public static void loadIniFile(BufferedReader reader, Map<String, String> out)
		throws IOException
	{
		String SOURCE = "source";
		Pattern pattern = Pattern.compile("\\s*([^=]*)\\s*=\\s*(.*)\\s*");
		
		String line;
		List<String> loadFileNames = new ArrayList<String>();

		String tmp = ""; 
		
		while((line = reader.readLine()) != null) {
			line.trim();
			if(line.startsWith(SOURCE)) {
				String fileName = line.substring(SOURCE.length()).trim();
				
				loadFileNames.add(fileName);
				
			} else {
				Matcher m = pattern.matcher(line);
				if(m.find()) {
					String key = m.group(1);
					String value = m.group(2);
					
					value = StringUtil.strip(value, "\"").trim();
					
					out.put(key, value);
				}
			}
		}
		
		System.out.println(tmp);
		System.out.println(loadFileNames);
		
		for(String loadFileName : loadFileNames) {
			File file = new File(loadFileName);
			loadIniFile(file, out);
		}
	}
	
	
	public LiveSync(Map<String, String> config)
		throws Exception
	{
		this.config = config;
		
		publishDiffBaseName = config.get("publishDiffRepoPath");
		
		//LiveRDFDeltaPluginFactory factory.create();		
		Connection conn = VirtuosoUtils.connect(
				config.get("rdfStore_hostName"),
				config.get("rdfStore_userName"),
				config.get("rdfStore_passWord"));

		graphName = config.get("rdfStore_graphName");
		
		graphDAO =
			new VirtuosoJdbcSparulExecutor(conn, graphName);

		
		Connection nodeConn = PostGISUtil.connectPostGIS(
				config.get("osmDb_hostName"),
				config.get("osmDb_dataBaseName"),
				config.get("osmDb_userName"),
				config.get("osmDb_passWord"));
		
		//RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(outputBaseName);
		//rdfDiffWriter = new RDFDiffWriter();
		
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		
		tagMapper.load(new File(config.get("tagMappings")));
		
		boolean loadTagMappingsFromDb = false;
		if(loadTagMappingsFromDb) {
			Session session = TagMappingDB.getSession();
			Transaction tx = session.beginTransaction();
			
			for(Object o : session.createCriteria(AbstractTagMapperState.class).list()) {
				IOneOneTagMapper item = TagMapperInstantiator.getInstance().instantiate((IEntity)o);
				
				tagMapper.add(item);
			}
			
			tx.commit();
		}
		
		//File diffRepo = new File("/tmp/lgddiff");
		//diffRepo.mkdirs();
		
		//RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(diffRepo, 0);
		
		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer =
			new OSMEntityToRDFTransformer(tagMapper, vocab);
		
		nodePositionDao = new NodePositionDAO("node_position");
		nodePositionDao.setConnection(nodeConn);
		
		
		GeoRSSNodeMapper nodeMapper = new GeoRSSNodeMapper(vocab);
		RDFNodePositionDAO rdfNodePositionDao = new RDFNodePositionDAO(nodePositionDao, vocab, nodeMapper);
		
		
		IGraph baseGraph = new SparqlEndpointFilteredGraph(graphDAO);
		DeltaGraph deltaGraph = new DeltaGraph(baseGraph);
		
		// Create an index by s and o
		TripleCacheIndexImpl.create(baseGraph, 100000, 10000, 10000, new int[]{0}); 
		TripleCacheIndexImpl.create(baseGraph, 100000, 10000, 10000, new int[]{2}); 

		/*
		diffStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName, rdfNodePositionDao);
				*/
		diffStrategy = new OptimizedDiffUpdateStrategy(vocab, entityTransformer, deltaGraph, rdfNodePositionDao);
		

		// Load the entity tag filter
		TagFilter entityTagFilter = new TagFilter();
		entityTagFilter.load(new File(config.get("entityFilter")));

		EntityFilterPlugin entityFilterPlugin = new EntityFilterPlugin(new EntityFilter(entityTagFilter));
		
		TagFilter tagFilter = new TagFilter();
		tagFilter.load(new File(config.get("tagFilter")));
		
		TagFilterPlugin tagFilterPlugin = new TagFilterPlugin(tagFilter);
		
		tagFilterPlugin.setChangeSink(diffStrategy);
		entityFilterPlugin.setChangeSink(tagFilterPlugin);
		
		workFlow = entityFilterPlugin;
	}


	public static void main(String[] args)
		throws Throwable
	{
		PropertyConfigurator.configure("log4j.properties");
		
		initCLIOptions();
		
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		String configFileName = commandLine.getOptionValue("c", "config.ini");

		File configFile = new File(configFileName);

		Map<String, String> config = loadIniFile(configFile);

		File osmConfigFile = new File(config.get("osmReplicationConfigPath") + "/configuration.txt");
		loadIniFile(osmConfigFile, config);
		
		
		System.out.println(config);
		
		LiveSync liveSync = new LiveSync(config);
		
		long stepCount = 0;
		long totalStartTime = System.nanoTime();
		for(;;) {
			try {
				long stepStartTime = System.nanoTime();
				
				++stepCount;
				liveSync.step();
				
				long now = System.nanoTime();
				double stepDuration = (now - stepStartTime) / 1000000000.0;
				double totalDuration = (now - totalStartTime) / 1000000000.0;
				double avgStepDuration = totalDuration / stepCount;
				logger.info("Step #" + stepCount + " took " + stepDuration + "sec; Average step duration is " + avgStepDuration + "sec.");
				
			} catch(Throwable t) {
				logger.error("An exception was encountered in the LiveSync update loop", t);
				throw t;
			}
			 
		}
	}

	
	private SAXParser createParser() {
		try {
			return SAXParserFactory.newInstance().newSAXParser();
			
		} catch (ParserConfigurationException e) {
			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
		} catch (SAXException e) {
			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
		}
	}

	private void step()
		throws Exception
	{
		// Load the state config
		File osmStateFile = new File(config.get("osmReplicationConfigPath") + "/state.txt");
		loadIniFile(osmStateFile, config);
		sequenceNumber = Long.parseLong(config.get("sequenceNumber"));
		
		logger.info("Processing: " + sequenceNumber);
		
		// Get the stream to the OSC file
		//InputStream in = getChangeSetStream(sequenceNumber);
		//File changeFile = getChangeFile(sequenceNumber);
		
		
		//XmlChangeReader reader = new XmlChangeReader(changeFile, true, CompressionMethod.GZip);
			
		//IDiff<Model> diff = computeDiff(sequenceNumber);
		DiffResult diff = computeDiff(sequenceNumber);
		
		
		logger.info("Applying main diff (added/removed) = " + diff.getMainDiff().getAdded().size() + "/" + diff.getMainDiff().getRemoved().size());
		applyDiff(diff.getMainDiff());
		
		logger.info("Applying node diff (added/removed) = " + diff.getNodeDiff().getAdded().size() + "/" + diff.getNodeDiff().getRemoved().size());
		applyNodeDiff(diff.getNodeDiff());
		
		
		logger.info("Publishing diff");
		publishDiff(sequenceNumber, diff.getMainDiff());
		
		
		logger.info("Downloading new state");
		advance(sequenceNumber + 1);
	}
	
	private void advance(long id)
		throws IOException
	{
		URL sourceURL = new URL(config.get("baseUrl") + "/" + getFragment(id) + ".state.txt");
		File targetFile = new File(config.get("osmReplicationConfigPath") + "/state.txt");
		
		URIUtil.download(sourceURL, targetFile);
	}

	
	private void applyDiff(IDiff<Model> diff)
		throws Exception
	{
		graphDAO.remove(diff.getRemoved(), graphName);
		graphDAO.insert(diff.getAdded(), graphName);
	}
	
	
	public static Map<Long, Point2D> getNodeToPositionMap(Iterable<Node> nodes)
	{
		Map<Long, Point2D> result = new TreeMap<Long, Point2D>();
		for(Node node : nodes) {
			result.put(node.getId(), new Point2D.Double(node.getLongitude(), node.getLatitude()));
		}
		return result;
	}
	
	private void applyNodeDiff(TreeSetDiff<Node> diff)
		throws SQLException
	{
		nodePositionDao.remove(getNodeToPositionMap(diff.getRemoved()).keySet());
		nodePositionDao.updateOrInsert(getNodeToPositionMap(diff.getAdded()));
	}
	
	
	private void publishDiff(long id, IDiff<Model> diff)
		throws IOException
	{
		String fileName = publishDiffBaseName + "/" + getFragment(id);
		File parent = new File(fileName).getParentFile();
		if(parent != null)
			parent.mkdirs();

		RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(fileName);
		rdfDiffWriter.write(diff);
		//RDFDiffWriter.writ
	}
	
	/*
	private void getCurrentState() {
		//ReplicationState repState = new ReplicationState()
	}
	*/
	
	private String format(long value) {
		String result = Long.toString(value);
		if(value < 100) 
			result = "0" + result;

		if(value < 10) 
			result = "0" + result;
		
		return result;
	}
	
	String getFragment(long id)
	{
		List<Long> parts = RDFDiffWriter.chunkValue(id, 1000, 1000);
		
		String fragment = ""; //Long.toString(parts.get(0));
		for(Long part : parts) {
			fragment += "/" + format(part);
		}

		return fragment;
	}
	
	InputStream getChangeSetStream(long id)
		throws IOException
	{
		URL url = getChangeSetURL(id);
		return url.openStream();
	}
	
	File getChangeFile(long id)
		throws IOException
	{
		URL url = getChangeSetURL(id);
		File file = new File(config.get("tmpPath") + ".diff.osc.gz");
		
		URIUtil.download(url, file);
		
		return file;
	}
	
	URL getStateURL(long id)
		throws MalformedURLException
	{
		return new URL(getBaseURL(id) + ".state.txt");
	}
	
	URL getChangeSetURL(long id)
		throws MalformedURLException
	{
		return new URL(getBaseURL(id) + ".osc.gz");
	}
	
	String getBaseURL(long id)
	{
		
		String urlStr = config.get("baseUrl") + "/" + getFragment(id);
		
		return urlStr;
	}


	/*
	private void downloadChangeSet()
	{
		//URIUtil.download(
	}*/
	
	private DiffResult computeDiff(long id)
		throws SAXException, IOException
	{
		InputStream inputStream = getChangeSetStream(id);		
		inputStream = new CompressionActivator(CompressionMethod.GZip).createCompressionInputStream(inputStream);
		
		parser.parse(inputStream, new OsmChangeHandler(workFlow, true));
		
		//diffStrategy.complete();
		workFlow.complete();
		
		IDiff<Model> diff = diffStrategy.getMainGraphDiff();
		TreeSetDiff<Node> nodeDiff = diffStrategy.getNodeDiff();
		
		//diffStrategy.release();
		workFlow.release();
		
		return new DiffResult(diff, nodeDiff);
	}
	
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("c", "config", true, "Config filename");
	}

}
