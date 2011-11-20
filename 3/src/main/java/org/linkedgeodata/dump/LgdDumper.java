/**
 * Dumper for the simple schema of postgres.
 * 
 * 
 */
package org.linkedgeodata.dump;


import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.commons.collections.SinglePrefetchIterator;
import org.aksw.commons.util.strings.StringUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.PropertyConfigurator;
import org.openjena.atlas.lib.Sink;
import org.openjena.riot.out.SinkTripleOutput;
import org.postgis.PGgeometry;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.vocabulary.RDF;

interface GeometryRdfSerializer {
	void write(PGgeometry geometry, Sink<Triple> sink);
}

class GeoVocabRdfSerializer implements GeometryRdfSerializer {

	public void write(PGgeometry geometry, Sink<Triple> sink) {
	}
}

/**
 * Post process prefixed values, such as
 * (http://en.wikipedia.org/wiki, <some title>)
 * 
 * @author raven
 *
 */
interface PostProcessor
{
	Node process(String prefix, String value);
}

class DefaultPostProcessor
	implements PostProcessor
{
	private static DefaultPostProcessor instance = null;
	
	public static DefaultPostProcessor getInstance() {
		if(instance == null) {
			synchronized(DefaultPostProcessor.class) {
				if(instance == null) {
					instance = new DefaultPostProcessor();
				}
			}
		}
		
		return instance;
	}
	
	public Node process(String prefix, String value) {
		return Node.createURI(prefix + value);
	}
}


class UCamelizeAndUrlEncodePostProcessor
	implements PostProcessor
{

	public Node process(String prefix, String value) {
		String camelized = StringUtils.toUpperCamelCase(value.replace(' ', '_'));
		String encoded = StringUtils.urlEncode(camelized);
		
		return Node.createURI(prefix + encoded);
	}
}

class MediaWikiTitlePostProcessor
	implements PostProcessor
{
	// Namespace names must be ucFirst and with underscores
	private static Set<String> namespaces = new HashSet<String>(Arrays.asList(
			"Media",
			"Special",
			"Talk",
			"User",
			"User_talk",
			"Wikipedia",
			"Wikipedia_talk",
			"File",
			"File_talk",
			"Mediawiki",
			"Mediawiki_talk",
			"Template",
			"Template_talk",
			"Help",
			"Help_talk",
			"Category",
			"Category_talk",
			"Portal",
			"Portal_talk",
			"Book",
			"Book_talk"			
			));
	
	public Node process(String prefix, String value) {
		String canonicalized = MediawikiUtils.toCanonicalWikiCase(value, namespaces);
		
		// Check if it already forms a valid uri
		try {
			URL url = new URL(prefix + canonicalized);
			
			return Node.createURI(url.toString());
		} catch(Exception e) {
			String encoded = StringUtils.urlEncode(canonicalized);
			
			return Node.createURI(prefix + encoded);
		}
	}
}


class ResultSetIterator
	extends SinglePrefetchIterator<ResultSet>
{
	private ResultSet rs;
	
	public ResultSetIterator(ResultSet rs) {
		this.rs = rs;
	}
	
	@Override
	protected ResultSet prefetch() throws Exception {
		return rs.next() ? rs : finish();
	}	
}



/**
 * Hello world!
 * 
 */
public class LgdDumper {
	public static void printHelpAndExit(int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(LgdDumper.class.getName(), cliOptions);
		System.exit(exitCode);
	}

	private static final Logger logger = LoggerFactory
			.getLogger(LgdDumper.class);
	private static final Options cliOptions = new Options();

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");

		/*
		 * LogManager.getLogManager().readConfiguration( new
		 * FileInputStream("jdklog.properties"));
		 */

		CommandLineParser cliParser = new GnuParser();

		/*
		cliOptions.addOption("t", "type", true,
				"Database type (posgres, mysql,...)");
		*/
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "username", true, "");
		cliOptions.addOption("p", "password", true, "");
		cliOptions.addOption("h", "hostname", true, "");


		CommandLine commandLine = cliParser.parse(cliOptions, args);

		// Parsing of command line args

		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName = commandLine.getOptionValue("d", "");
		String userName = commandLine.getOptionValue("u", "");
		String passWord = commandLine.getOptionValue("p", "");

		PGSimpleDataSource dataSource = new PGSimpleDataSource();

		dataSource.setDatabaseName(dbName);
		dataSource.setServerName(hostName);
		dataSource.setUser(userName);
		dataSource.setPassword(passWord);


		Connection conn = dataSource.getConnection();

		conn.setAutoCommit(false);
		
		OutputStream out = System.out;
		Sink<Triple> sink = new SinkTripleOutput(out);
		
		
		Map<String, PostProcessor> postProcessorMap = new HashMap<String, PostProcessor>();
		postProcessorMap.put("ucamelize&urlEncode", new UCamelizeAndUrlEncodePostProcessor());
		postProcessorMap.put("mediawikiTitle", new UCamelizeAndUrlEncodePostProcessor());
		
		LgdDumper dumper = new LgdDumper();
		
		dumper.dumpNodes(conn, sink);
		
		/*
		dumper.dumpTagsDatatype(conn, "boolean", null, sink);
		dumper.dumpTagsDatatype(conn, "int", null, sink);
		dumper.dumpTagsDatatype(conn, "float", null, sink);
		
		dumper.dumpResourceTags(conn, "lgd_tags_resource_k", null, sink);
		dumper.dumpResourceTags(conn, "lgd_tags_resource_kv", null, sink);
		
		dumper.dumpResourceTagsPrefixed(conn, "lgd_tags_resource_prefix", null, postProcessorMap, sink);
		dumper.dumpResourceTags(conn, "lgd_tags_property", null, sink);
		*/

		conn.close();
	}

	private LgdVocab vocab = new LgdVocabDefault();
	private Calendar cal = new GregorianCalendar();

	private TypeMapper tm = TypeMapper.getInstance();
	private RDFDatatype virtRdf = tm
			.getSafeTypeByName("http://www.openlinksw.com/schemas/virtrdf#");

	//private Sink<Triple> sink;
	
	public LgdDumper() {
		
	}
	
	public void dumpResourceTags(Connection conn, String tableName, String osmEntityType, Sink<Triple> sink)
		throws Exception
	{		
		ResultSet rs = conn
				.createStatement()
				.executeQuery(
						"SELECT osm_entity_type, osm_entity_id, property, object FROM " + tableName + whereClause(osmEntityType));
		
		while (rs.next()) {
			Node subject = getResource(rs.getString("osm_entity_type"), rs.getLong("osm_entity_id"));
			Node predicate = Node.createURI(rs.getString("property"));
			Node object = Node.createURI(rs.getString("object"));

			sink.send(new Triple(subject, predicate, object));
		}
	}

	public void dumpResourceTagsPrefixed(Connection conn, String tableName, String osmEntityType, Map<String, PostProcessor> postProcessorMap, Sink<Triple> sink)
			throws Exception
		{		
			ResultSet rs = conn
					.createStatement()
					.executeQuery(
							"SELECT osm_entity_type, osm_entity_id, property, object_prefix, v, post_processing FROM " + tableName + whereClause(osmEntityType));
			
			while (rs.next()) {
				Node subject = getResource(rs.getString("osm_entity_type"), rs.getLong("osm_entity_id"));
				Node predicate = Node.createURI(rs.getString("property"));
				String objectPrefix = rs.getString("object_prefix");
				String postProcessing = rs.getString("post_processing");
				String v = rs.getString("v");

				PostProcessor postProcessor = postProcessorMap.get(postProcessing);
				if(postProcessor == null) {
					postProcessor = DefaultPostProcessor.getInstance();
				}
				
				Node object = postProcessor.process(objectPrefix, v);
				
				sink.send(new Triple(subject, predicate, object));
			}
		}
	
	/*
	public void dumpUriLiterals(Connection conn, String osmEntityType, Sink<Triple> sink)
			throws Exception
		{		
			ResultSet rs = conn
					.createStatement()
					.executeQuery(
							"SELECT osm_entity_type, osm_entity_id, property, object FROM lgd_tags_property " + whereClause(osmEntityType));
			
			while (rs.next()) {
				Node subject = getResource(rs.getString("way_id"), rs.getLong("osm_entity_id"));
				Node predicate = Node.createURI(rs.getString("property"));
				Node object = Node.createURI(rs.getString("v"));

				sink.send(new Triple(subject, predicate, object));
			}
		}
		*/


	
	
	

	public void dumpTagsDatatype(Connection conn, String suffix, String osmEntityType, Sink<Triple> sink)
		throws Exception
	{		
		ResultSet rs = conn
				.createStatement()
				.executeQuery(
						"SELECT osm_entity_type, osm_entity_id, k, v FROM lgd_tags_" + suffix + whereClause(osmEntityType));
		
		while (rs.next()) {
			Node subject = getResource(rs.getString("osm_entity_type"), rs.getLong("osm_entity_id"));
			
			// Auto generate properties for now
			String p = rs.getString("k");
			String[] parts = p.split(":");

			int last = parts.length - 1;

			for(int i = 0; i < last; ++i) {
				parts[i] = StringUtils.toCamelCase(parts[i], false);
			}
			
			parts[last] = "boolean".equals(suffix)
					? "is" + StringUtils.toCamelCase(parts[last], true)
					: StringUtils.toCamelCase(p, false);

					
			p = "http://linkedgeodata.org/ontology/" + Joiner.on('/').join(parts);
				
			
			
			Node predicate = Node.createURI(p);
			Node value = ResourceFactory.createTypedLiteral(rs.getObject("v")).asNode();
			
			sink.send(new Triple(subject, predicate, value));
		}
		
	}
	
	public String whereClause(String osmEntityTypeFilter) {
		return osmEntityTypeFilter == null
				? ""
				: " WHERE osm_entity_type = '" + osmEntityTypeFilter + "'";
	}
	
	public Node getResource(String osmEntityType, long id) {
		if("node".equals(osmEntityType)) {
			return vocab.createNode(id);
		} else if("way".equals(osmEntityType)) {
			return vocab.createWay(id);
		} else if("relation".equals(osmEntityType)) {
			return vocab.createRelation(id);
		} else {
			throw new RuntimeException("Unknown entity type: " + osmEntityType);
		}
	}
	
	public void dumpWayGeometriesNeoGeo(Connection conn, Sink<Triple> sink)
			throws Exception
	{
		ResultSet rs = conn
				.createStatement()
				.executeQuery(
						"SELECT way_id, node_id FROM way_nodes ORDER BY way_id, sequence_id");

		Long currentWayId = null;
		List<Long> currentNodeIds = new ArrayList<Long>();
		
		while (rs.next()) {
			Long wayId = rs.getLong("way_id");
			if(!wayId.equals(currentWayId)) {
				if(currentWayId == null) {
					currentWayId = wayId;					
				} else {
					writeNeoGeo(currentWayId, currentNodeIds, sink);
					
					currentNodeIds.clear();
					currentWayId = wayId;
				}
			}
			
			currentNodeIds.add(rs.getLong("node_id"));
		}

		writeNeoGeo(currentWayId, currentNodeIds, sink);
	}
	
	public void writePointNeoGeo(long nodeId, Double lon, Double lat, Sink<Triple> sink) {
		Node node = vocab.createNode(nodeId);

		Node geo = vocab.createNodeGeometry(nodeId);
		
		sink.send(new Triple(node, GeoVocab.geometry, geo));
		sink.send(new Triple(geo, RDF.type.asNode(), WGS84Pos.Point.asNode()));

		sink.send(new Triple(geo, WGS84Pos.xlong.asNode() , NodeValue.makeDouble(lon).asNode()));
		sink.send(new Triple(geo, WGS84Pos.xlat.asNode(), NodeValue.makeDouble(lat).asNode()));
		
	}
	
	public void writeNeoGeo(Long wayId, List<Long> nodeIds, Sink<Triple> sink) {
		
		Node way = vocab.createWay(wayId);
		
		Node geo = vocab.createWayGeometry(wayId);
		
		
		sink.send(new Triple(way, GeoVocab.geometry, geo));
		//sink.send(new Triple(geo, RDF.type.asNode(), GeoVocab.LineString));

		Node first = nodeIds.isEmpty()
				? RDF.nil.asNode()
				: Node.createURI(vocab.createNodeGeometry(nodeIds.get(0)).toString() + "-0");

		
		sink.send(new Triple(geo, GeoVocab.posList, first));
	
		for(int i = 0; i < nodeIds.size(); ++i) {
			sink.send(new Triple(first, RDF.type.asNode(), RDF.List.asNode()));
			sink.send(new Triple(first, RDF.first.asNode(), vocab.createNode(nodeIds.get(i))));
			
			
			Node rest = (i + 1 != nodeIds.size())
					? Node.createURI(vocab.createNodeGeometry(nodeIds.get(0)).toString() + "-" + (i + 1))
					: RDF.nil.asNode();
						
			sink.send(new Triple(first, RDF.rest.asNode(), rest));
			
			first = rest;
		}		
	}
	
	
	
	public void dumpNodes(Connection conn, Sink<Triple> sink) throws Exception {
		Statement stmt = conn.createStatement();
		
		stmt.setFetchSize(50000);
		
		ResultSet rs = stmt
				.executeQuery(
						"SELECT id, version, user_id, tstamp, changeset_id, ST_AsText(geom) geom FROM nodes");

		while (rs.next()) {
			long id = rs.getLong("id");

			Node subject = vocab.createNode(id);
			Node version = NodeValue.makeInteger(rs.getInt("version")).asNode();
			Node user = NodeValue.makeInteger(rs.getInt("user_id")).asNode();
			Date date = rs.getDate("tstamp");
			cal.setTime(date);
			Node tstamp = NodeValue.makeDateTime(cal).asNode();
			Node changeset = NodeValue.makeDecimal(rs.getInt("changeset_id")).asNode();
			Node wkt = Node.createLiteral(rs.getString("geom"), null, virtRdf);
			
			sink.send(new Triple(subject, vocab.version(), version));
			sink.send(new Triple(subject, vocab.user(), user));
			sink.send(new Triple(subject, vocab.tstamp(), tstamp));
			sink.send(new Triple(subject, vocab.changeSet(), changeset));
			sink.send(new Triple(subject, vocab.geometryLiteral(), wkt));
		}		
	}

	
	public void dumpWays(Connection conn, Sink<Triple> sink) throws Exception {
		ResultSet rs = conn
				.createStatement()
				.executeQuery(
						"SELECT id, version, user_id, tstamp, changeset_id, ST_AsText(linestring) linestring FROM ways");

		while (rs.next()) {
			long id = rs.getLong("id");

			Node subject = vocab.createWay(id);
			Node version = NodeValue.makeInteger(rs.getInt("version")).asNode();
			Node user = NodeValue.makeInteger(rs.getInt("user_id")).asNode();
			Date date = rs.getDate("tstamp");
			cal.setTime(date);
			Node tstamp = NodeValue.makeDateTime(cal).asNode();
			Node changeset = NodeValue.makeDecimal(rs.getInt("changeset_id")).asNode();
			Node wkt = Node.createLiteral(rs.getString("linestring"), null, virtRdf);
			
			sink.send(new Triple(subject, vocab.version(), version));
			sink.send(new Triple(subject, vocab.user(), user));
			sink.send(new Triple(subject, vocab.tstamp(), tstamp));
			sink.send(new Triple(subject, vocab.changeSet(), changeset));
			sink.send(new Triple(subject, vocab.geometryLiteral(), wkt));
		}		
	}
	
	
	
	
}

class GeoVocab {
	public static final String ns = "http://geovocab.org/geometry#";
	public static final Node geometry = Node.createURI(ns + "geometry");
	public static final Node posList = Node.createURI(ns + "posList");
	public static final Node LineString = Node.createURI(ns + "LineString");

}

interface LgdVocab {

	Node createNode(long id);
	Node createRelation(long id);
	Node createWay(long id);

	Node createNodeGeometry(long id);
	Node createWayGeometry(long id);
	
	// Predicates
	Node version();
	Node changeSet();
	Node user();
	Node tstamp();
	Node geometryLiteral();
}

class LgdVocabDefault implements LgdVocab {
	private static final String ns = "http://linkedgeodata.org/";
	private static final String resourceNs = ns + "triplify/";
	private static final String ontologyNs = ns + "ontology/";

	private static final String nodeNs = resourceNs + "node";
	private static final String wayNs = resourceNs + "way";
	private static final String wayNodeNs = resourceNs + "waynode";
	private static final String relationNs = resourceNs + "relation";

	private static final String geometryNodeNs = ns + "geometry/node";
	private static final String geometryWayNs = ns + "geometry/way";

	
	private static final String geometryLiteralNs = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	private static final Node geometryLiteral = Node.createURI(geometryLiteralNs);
	
	private static final Node version = Node.createURI(ontologyNs + "version");
	private static final Node user = Node.createURI(ontologyNs + "contributor");
	private static final Node tstamp = Node.createURI(ontologyNs + "date");
	private static final Node changeset = Node.createURI(ontologyNs + "changeset");
	
	public Node createNode(long id) {
		return Node.createURI(nodeNs + id);
	}

	public Node createWay(long id) {
		return Node.createURI(wayNs + id);
	}

	public Node createRelation(long id) {
		return Node.createURI(relationNs + id);
	}

	public Node version() {
		return version;
	}

	public Node changeSet() {
		return changeset;
	}

	public Node user() {
		return user;
	}

	public Node tstamp() {
		return tstamp;
	}

	public Node geometryLiteral() {
		return geometryLiteral;
	}

	public Node createNodeGeometry(long id) {
		return Node.createURI(geometryNodeNs + id);
	}

	public Node createWayGeometry(long id) {
		return Node.createURI(geometryWayNs + id);
	}
}


class WGS84Pos
{
	protected static final String uri ="http://www.w3.org/2003/01/geo/wgs84_pos#";

    public static String getURI()
    {
    	return uri;
    }

    protected static final Resource resource(String local)
    {
    	return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local)
    {
    	return ResourceFactory.createProperty(uri, local);
    }

    /*
    public static Property li( int i )
	        { return property( "_" + i ); }
     */
   	public static final Property xlat = property("lat");
	public static final Property xlong = property("long");
	
	public static final Property geometry = property("geometry");
	public static final Property Point = property("Point");

}

class MediawikiUtils
{
	public static MediawikiTitle parseTitle(String name, Set<String> namespaces)
	{
	       String[] parts = name.split(":", 2);
	       String namespaceName = "";
	       String articleName = null;
	       
	       // Note: Just because there are 2 parts, it doesn't mean that we have
	       // namespace. e.g. Mission:Impossible - 'Mission' is not a namespace.
	       if(parts.length == 2) {
	           namespaceName = canonicalWikiTrim(parts[0]);
	           namespaceName = StringUtils.ucFirst(namespaceName.toLowerCase());

	           if(namespaces.contains(namespaceName))
	               articleName = StringUtils.ucFirst(canonicalWikiTrim(parts[1]));
	       }
	       
	       // if there is no articleName yet, the whole name is the articleName
	       if(articleName == null) {
	    	   namespaceName = "";
	    	   articleName = StringUtils.ucFirst(canonicalWikiTrim(name));
	       }
	       
	       return new MediawikiTitle(namespaceName, articleName);
	}
	
   /**
    *
    * Converts string to canonical wiki representation
    * Namespace is only recognized if there is an entry in namespaces
    * Namespace part and name part will be trimmed
    * Remaining whitespaces will be replaced by underscores
    * TODO Multiple consequtive underscores will be replaced by a single underscore
    * The whole namespace name will be turned lowercase except for the first letter
    * The first letter of the name will be made uppercase
    *
    * Example
    *    mYnameSPACE  :     wHat     EVER
    * will currently become:
    * MYnameSPACE:WHat_____EVER
    * should become
    * MYnameSPACE:WHat_EVER
    *
    *
    * @param <type> $str The source string
    * @param <type> $namespaces An array containing the names of namespaces
    * @return <type> A canonical representation of the wiki name
    *
    */
   public static String toCanonicalWikiCase(String name, Set<String> namespaces)
   {
       return parseTitle(name, namespaces).toString();
   }

   /**
    * Removes heading and trailing whitespaces
    * Replaces remaining white spaces with underscore
    * Replaces consecutive underscores with a single underscore
    *
    * @param <type> $name
    * @return <type>
    */
   public static String canonicalWikiTrim(String name)
   {
   	String result = name.trim();
   	result = result.replace(' ', '_');
   	result = result.replaceAll("_+", "_");
       
       return result;
   }

}


class MediawikiTitle
{
	private String namespaceName;
	private String articleName;
	
	public MediawikiTitle(String namespaceName, String articleName)
	{
		this.namespaceName = namespaceName;
		this.articleName = articleName;
	}
	
	public String getNamespaceName()
	{
		return namespaceName;
	}
	public String getArticleName()
	{
		return articleName;
	}
	
	public String toString()
	{
		if(namespaceName.isEmpty())
			return articleName;
		else
			return namespaceName + ":" + articleName;
	}
}


