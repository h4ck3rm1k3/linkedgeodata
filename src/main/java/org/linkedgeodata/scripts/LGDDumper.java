package org.linkedgeodata.scripts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.util.PrefetchIterator;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;


class SimpleStatsTracker
{
	private static final Logger logger = Logger.getLogger(SimpleStatsTracker.class);

	// TODO Make this a weak map
	private static Map<String, SimpleStatsTracker> nameToTracker =
		new HashMap<String, SimpleStatsTracker>();
	
	public static SimpleStatsTracker get(String name)
	{
		SimpleStatsTracker result = nameToTracker.get(name);
		if(result == null) {
			result = new SimpleStatsTracker();
			nameToTracker.put(name, result);
		}
		
		return result;
	}

	
	private StopWatch stopWatch = new StopWatch();
	private float lastTime = 0.0f;
	private long counter = 0;
	boolean isStarted = false;
	
	
	public void update(int delta)
	{
		if(!isStarted)
			start();
		
		counter += delta;
		float time = stopWatch.getTime() / 1000.0f;
		if(time - lastTime > 5.0f) {
			float ratio = counter / time;	
			logger.info("Time: " + time + ", Counter: " + counter + ", ratio = " + ratio);
			lastTime = time;
		}		
	}
	
	public void start()
	{
		stopWatch.start();
		isStarted = true;
	}
}


/**
 * An iterator for the modified node_tags table.
 * Modified means, that the geom column was added to this table.
 * 
 * Assumes the following columns:
 * id   : long
 * k    : String
 * v    : String
 * geom : geometry
 * 
 * @author raven
 *
 */
class NodeTagIterator
	extends PrefetchIterator<Node>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	public NodeTagIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public NodeTagIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Node> prefetch()
		throws Exception
	{
		String sql = "SELECT DISTINCT node_id FROM node_tags ";
		if(offset != null)
			sql += "WHERE node_id > " + offset + " ";
		
		sql += "ORDER BY node_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";

		String s = "SELECT node_id, k, v, Y(geom::geometry) AS lat, X(geom::geometry) AS lon FROM node_tags WHERE node_id IN (" + sql + ")";

		//System.out.println(s);
		ResultSet rs = conn.createStatement().executeQuery(s);
		
		//List<Node> result = new ArrayList<Node>();

		Map<Long, Node> idToNode = new HashMap<Long, Node>();
		
		int counter = 0;
		while(rs.next()) {
			++counter;

			long nodeId = rs.getLong("node_id");		
			String k = rs.getString("k");
			String v = rs.getString("v");
			double lat = rs.getFloat("lat");
			double lon = rs.getFloat("lon");

			offset = offset == null ? nodeId : Math.max(offset, nodeId);
			
			Node node = idToNode.get(nodeId);
			if(node == null) {
				node = new Node(nodeId, 0, (Date)null, null, -1, lat, lon);
				idToNode.put(nodeId, node);
			}

			Tag tag = new Tag(k, v);
			node.getTags().add(tag);
		}
		
		if(counter == 0)
			return null;
		else
			return idToNode.values().iterator();
	}
	
}




/**
 * TODO Move the transformation functions into a static utility class so they
 * can be reused
 * 
 * @author raven
 *
 */
class SimpleNodeToRDFTransformer
	implements Transformer<Node, Model>
{
	private static final Logger logger = Logger.getLogger(SimpleNodeToRDFTransformer.class);
	private TagMapper tagMapper;

	
	private int parseErrorCount = 0;
	
	public SimpleNodeToRDFTransformer(TagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}
	
	@Override
	public Model transform(Node node) {
		Model model = ModelFactory.createDefaultModel();
		
		URI subjectURI = getSubject(node);
		Resource subject = model.getResource(subjectURI.toString());
		
		generateWGS84(model, subject, node);
		generateGeoRSS(model, subject, node);
		generateTags(model, subject, node.getTags());

		return model;
	}
	
	private void generateTags(Model model, Resource subject, Collection<Tag> tags)
	{
		//if(tags == null)

		// Generate RDF for the tags
		for(Tag tag : tags) {
			Model subModel = tagMapper.map(URI.create(subject.getURI()), tag);
			if(subModel == null) {
				++parseErrorCount;
				logger.warn("Failed mapping: " + tag + ", Failed mapping count: " + parseErrorCount);
				continue;
			}

			model.add(subModel);
		}		
	}
	
	private URI getSubject(Node node)
	{
		String prefix = "http://linkedgeodata.org/";
		URI subject = URI.create(prefix + "node/" + node.getId() + "#id");
		
		return subject;
	}

	
	private static final String geoRSSPoint = "http://www.georss.org/georss/point";
	
	public static void generateGeoRSS(Model model, Resource subject, Node node)
	{
		String str = node.getLatitude() + " " + node.getLongitude();		
		model.add(subject, model.getProperty(geoRSSPoint), str);
	}
	
	
	private static final String wgs84NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private static final String wgs84Lat = wgs84NS + "lat";
	private static final String wgs84Long = wgs84NS + "long";
	
	public static  void generateWGS84(Model model, Resource subject, Node node)
	{
		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dataType = tm.getSafeTypeByName(XSD.decimal.getURI());

		model.add(subject, model.getProperty(wgs84Lat), Double.toString(node.getLatitude()), dataType);
		model.add(subject, model.getProperty(wgs84Long), Double.toString(node.getLongitude()), dataType);		
	}

}


public class LGDDumper
{
	private static final Logger logger = Logger.getLogger(LGDDumper.class);	
    protected static Options cliOptions;


	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		initCLIOptions();
	
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName   = commandLine.getOptionValue("d", "lgd");
		String userName = commandLine.getOptionValue("u", "lgd");
		String passWord = commandLine.getOptionValue("w", "lgd");
		String outFileName = commandLine.getOptionValue("o", "out.n3");
	
		String batchSizeStr = commandLine.getOptionValue("n", "1000");
	
		int batchSize = Integer.parseInt(batchSizeStr);
		if(batchSize <= 0)
			throw new RuntimeException("Invalid argument for batchsize");
		
		Connection conn = LineStringUpdater.connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");
		
	
		logger.info("Loading mapping rules");
		TagMapper tagMapper = new TagMapper();
		tagMapper.load(new File("LGDMappingRules.txt"));

		SimpleNodeToRDFTransformer nodeTransformer =
			new SimpleNodeToRDFTransformer(tagMapper);

		logger.info("Opening output stream: " + outFileName);
		OutputStream out = new FileOutputStream(outFileName);


		run(conn, batchSize, nodeTransformer, out);		
		
		out.flush();
		out.close();
	}
	
	public static void run(Connection conn, int batchSize, Transformer<Node, Model> nodeTransformer, OutputStream out)
		throws Exception
	{	
		NodeTagIterator it = new NodeTagIterator(conn, batchSize);
	
		SimpleStatsTracker tracker = new SimpleStatsTracker();
	
		while(it.hasNext()) {
			Node node = it.next();
	
			Model model = nodeTransformer.transform(node);
	
			model.write(out, "N3");
			out.write('\n');

			//System.out.println(ModelUtil.toString(model));
			
			tracker.update(1);
		}
	}

	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("w", "password", true, "");
		cliOptions.addOption("h", "host", true, "");
		cliOptions.addOption("n", "batchSize", true, "Batch size");
		cliOptions.addOption("o", "outfile", true, "Output filename");		
	}
}
