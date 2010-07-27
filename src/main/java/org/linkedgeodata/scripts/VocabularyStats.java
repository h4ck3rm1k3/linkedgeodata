package org.linkedgeodata.scripts;

import java.io.File;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTagPattern;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SerializationUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;


class VocabularyStats
{
	private static final Logger logger = Logger.getLogger(VocabularyStats.class);
	
	private Connection conn = null;

	private Set<String> languageTags = new HashSet<String>(Arrays.asList(new String[]{"en", "cz", "fr", "ru", "jp", "it", "de"}));
	
	// The set of keys that needs to be processed. Keys that are not in the
	// set are ignored
	private Set<String> keys = new HashSet<String>();
	                 
	
	private List<ISimpleOneOneTagMapper> tagMappers = new ArrayList<ISimpleOneOneTagMapper>();
	                                    
	private Map<String, String> keyToLanguageMap = new HashMap<String, String>();
	private Model model = ModelFactory.createDefaultModel();
	
	private static List<RDFNode> getRanges(Model model, String subject)
	{
		Iterator<Statement> it = model.getProperty(subject).listProperties(RDFS.range);
		
		List<RDFNode> result = new ArrayList<RDFNode>();
		while(it.hasNext()) {
			result.add(it.next().getObject());
		}
		
		return result;
	}

	
	public void registerDatatypeProperty(String k, String dataType)
	{
		String resource = keyToURI(k);
		SimpleDataTypeTagMapper tagMapper = new SimpleDataTypeTagMapper(resource, new SimpleTagPattern(k, null), dataType, false);
		tagMappers.add(tagMapper);		
	}

	/*
	private void createDataTypeProperty(String subject, String dataType)
	{
		//Property prop = model.getProperty(subject)
		//prop.addProperty(RDFS.domain, range)
		//prop.addProperty(RDF.type, OWL.DatatypeProperty)
		//model.add(subject, RDFS.domain, range)
		//model.add(subject, RDF.type, OWL.DatatypeProperty);
		new SimpleDataTypeTagMapper(
	}*/


	/*
	def createClass(Model model, String subject)
	{
		Resource sub = model.getResource(subject);
		sub.addProperty(RDF.type, OWL.Class);
	}
	
	
	
	def registerClass(String k)
	{
		//k = StringUtil.ucFirst(toCamelCase(k));

		String resource = keyToURI(k);

		SimpleClassTagMapper tagMapper = new SimpleClassTagMapper(resource, new TagPattern(k, null), false)
		tagMappers.add(tagMapper);
		
		createClass(model, resource);
	}
	
	
	def registerSubClass(String k, String v, String subClassStr)
	{
		String parentClass = keyToURI(k);
		
		//String subClassStr = v;
		//if(isCombined)
		//	subClassStr = k + "_" + v;

		String subClass = keyToURI(subClassStr);

		SimpleClassTagMapper tagMapper = new SimpleClassTagMapper(subClass, new TagPattern(k, v), false)
		tagMappers.add(tagMapper);
		
		createClass(model, subClass);
		
		Resource sub = model.getResource(subClass);
		sub.addProperty(RDFS.subClassOf, model.getResource(parentClass));
	}
	
	def registerTextProperty(String k, String prefix, String lang)
	{
		String resource = keyToURI(prefix);
		
		SimpleTextTagMapper tagMapper = new SimpleTextTagMapper(resource, new TagPattern(k, null), lang, false);
		
		tagMappers.add(tagMapper);
	}

	def registerDatatypeProperty(String k, Resource range)
	{
		String resource = keyToURI(k);

		//TypeMapper tm = TypeMapper.getInstance();
		//RDFDatatype dataType = tm.getSafeTypeByName(range.toString());

		SimpleDataTypeTagMapper tagMapper = new SimpleDataTypeTagMapper(resource, new TagPattern(k, null), range.toString(), false);
		tagMappers.add(tagMapper);
		
		createDataTypeProperty(model, resource, range);
	}
	
	
	
	

	Connection connectPostGIS(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		String url = "jdbc:postgresql://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}
	
	*/
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		String hostName = commandLine.getOptionValue("h", "localhost");
		String dbName   = commandLine.getOptionValue("d", "lgd");
		String userName = commandLine.getOptionValue("u", "lgd");
		String passWord = commandLine.getOptionValue("w", "lgd");
		
		Connection conn = PostGISUtil.connectPostGIS(hostName, dbName, userName, passWord);
		logger.info("Connected to db");
		
		
		
		VocabularyStats c = new VocabularyStats();
		c.run(conn);
	}
	
	String keyToURI(String key)
	{
		try {
			return LGDVocab.ONTOLOGY_NS + URLEncoder.encode(key, "UTF-8");
		}
		catch(Exception e) {
			return null;
		}
	}

	
	private void  registerTextProperty(String k, String prefix, String lang)
	{
		String resource = keyToURI(prefix);
		
		SimpleTextTagMapper tagMapper = new SimpleTextTagMapper(resource, new SimpleTagPattern(k, null), lang, false);
		
		tagMappers.add(tagMapper);
	}

	
 	private void registerClass(String k)
	{
		String resource = keyToURI(k);

		SimpleObjectPropertyTagMapper tagMapper = new SimpleObjectPropertyTagMapper(RDF.type.toString(), resource, false, new SimpleTagPattern(k, null), false);
		//SimpleClassTagMapper tagMapper = new SimpleClassTagMapper(resource, new SimpleTagPattern(k, null), false);
		tagMappers.add(tagMapper);		
	}

	private void registerSubClass(String k, String v, String subClassStr)
	{
		//String parentClass = keyToURI(k);
		
		//String subClassStr = v;
		//if(isCombined)
		//	subClassStr = k + "_" + v;

		String subClass = keyToURI(subClassStr);

		//SimpleClassTagMapper tagMapper = new SimpleClassTagMapper(subClass, new SimpleTagPattern(k, v), false);
		SimpleObjectPropertyTagMapper tagMapper = new SimpleObjectPropertyTagMapper(RDF.type.toString(), subClass, false, new SimpleTagPattern(k, null), false);

		tagMappers.add(tagMapper);
	}

	
	/**
	 * In general this method should for a given key return a reduced version
	 * of the key suitable for generating a property and a language tag.
	 * 
	 * Specifically this method looks for the pattern
	 * <key>:<tag>
	 * 
	 * and returns a two element array for matching strings, or null otherwise
	 * 
	 * @param k
	 * @return
	 */
	private String[] languageSplit(String k)
	{	
		int index = k.lastIndexOf(':');
		if(index == -1)
			return null;
		
		String tag = k.substring(index + 1).trim().toLowerCase();
		String prefix = k.substring(0, index);
		
		return new String[]{prefix, tag};
	}

	private void loadKeys()
		throws SQLException
	{
		ResultSet rs = null;
		rs = conn.createStatement().executeQuery("SELECT k FROM lgd_stats_tags_datatypes");
		while(rs.next())
		{
			String k = rs.getString("k");

			keys.add(k);
		}
	}

	
	public void run(Connection conn)
		throws Exception
	{
		this.conn = conn;
		
		logger.info("Starting vocabulary generation");

		PropertyConfigurator.configure("log4j.properties");
		Logger logger = Logger.getLogger("vocab");
		
		//select distinct k from node_tags where v = 'no' or v='yes';
		//select * from properties where usage_count > 10 * distinct_value_count and distinct_value_count > 10 ORDER BY k;		
		
		//conn = PostGISUtil.connectPostGIS("localhost", "lgd", "postgres", "postgres");

		ResultSet rs;
		
		// Phase 0: Gather list of keys to process
		//Set<TagEntityMap> temSet = new HashSet<TagEntityMap>();
		
		
		// Phase 0.5 Determine keys that end with a language tag
		loadKeys();
		
		// Gather classes
		/**
		 * The default rule for classes is:
		 * id | k | v -> id type k (k, null)
		 * 
		 * For each frequently used value a new rule is created:
		 * id | k | v - > id type v(k, v)
		 * in this case v becomes a sub-class of k
		 * 
		 * 
		 * 
		 */
		MultiMap<String, String> kv = new MultiHashMap<String, String>();
		MultiMap<String, String> vk = new MultiHashMap<String, String>();

		List<String> classes = Arrays.asList(new String[]{"highway","barrier","cycleway","waterway","lock","railway","aeroway","aerialway","power","man_made","building","leisure","amenity","shop","tourism","historic","landuse","military","natural","route","boundary","sport"});
		
		String sql = "SELECT k, distinct_value_count FROM lgd_stats_k";
		rs = conn.createStatement().executeQuery(sql);
		while(rs.next())
		{
			String k = rs.getString(1);
			int dvc = rs.getInt(2);

			if(!keys.contains(k))
				continue;
			
			if(!classes.contains(k))
				continue;

			registerClass(k);
			logger.debug("Registered class: " + k);

			
			String escapedK = k.replace("'", "''");
			ResultSet rs2 = conn.createStatement().executeQuery(
					"SELECT v, COUNT(*) AS count FROM node_tags WHERE k = '" + escapedK + "' GROUP BY v");
		
			while(rs2.next()) {
				String v = rs2.getString("v");
				int c = rs2.getInt("count");

				if(c > 10) {
					kv.put(k, v);
					vk.put(v, k);
				}
			}

			keys.remove(k);
		}

		for(Map.Entry<String, Collection<String>> item : kv.entrySet()) {
			String k = item.getKey();
			Collection<String> vs = item.getValue();
			
			for(String v : vs) {
				// This avoids values we use as subclasses here to become
				// datatype properties later
				// (e.g. amenity biergarten ; biergarten yes)
				keys.remove(v);
				
				String subClassStr = v;
				if(vk.get(v).size() != 1) {
					logger.info("Disambiguating: " + k + " " + v + ":" +  vk);
					// arialway, tower becomes arialwayTower
					// the register(sub)class method than makes the first letter of
					// the class upper case too
					//v = k + StringUtil.ucFirst(v);
					
					// or better leave case as is, and separate with underscore
					// TODO Check if compound name is unique
					subClassStr = k + "_" + v;
				}
			                           
				registerSubClass(k, v, subClassStr);
				logger.debug("Registered Sub-class: " + k + ", " + v);
			}
		}
		kv = null;
		vk = null;
		
		
		// Phase 1: Identify integer and float datatype properties
		// TODO Create table lgd_stats_tags_datatypes if it doesn't exist
		logger.info("Analyzing integers, floats and boolean");
		
		rs = conn.createStatement().executeQuery(
			"SELECT  k, count_total, count_int, count_float, count_boolean FROM lgd_stats_tags_datatypes");
		
		
		/**
		 * In order for something to be classified as either a float or interger
		 * datatype-property, the absolute and relative error must be within
		 * certain limits.
		 * Note that the integers are a subset of the floats. Therefore if the errors
		 * for floats are already too big, it can't be an integer either.
		 * 
		 * TODO also check for subsets of integers - e.g. postive integer
		 */
		int numFloats = 0;
		int numInts = 0;
		int numBooleans = 0;

		while(rs.next())
		{
			String k = rs.getString("k");
			int countTotal = rs.getInt("count_total");
			int countInt = rs.getInt("count_int");
			int countFloat = rs.getInt("count_float");
			int countBoolean = rs.getInt("count_boolean");
		
			if(!keys.contains(k))
				continue;

			
			// Skip non frequently use properties in order to avoid conflicts
			// E.g. the key clock is used as a key maybe 5 times in the full dataset,
			// however amenity clock is mapped to the class clock
			// without skipping clock here it, clock would become a class
			// and a datatype property
			if(countTotal < 20)
				continue;
			
			
			// Test boolean errors
			int boolErrorAbs = countTotal - countBoolean;	
			float boolErrorRatio = boolErrorAbs / (float)countTotal;
			
			if(boolErrorRatio < 0.01f && boolErrorAbs < 5000) {
				logger.info("Identified datatype property (bool): " + k);
				//createDatatypeProperty(model, keyToURI(k), XSD.xboolean)
				registerDatatypeProperty(k, XSD.xboolean.getLocalName());
				++numBooleans;
				keys.remove(k);
				continue;
			}
			
			// Test float errors
			int floatErrorAbs = countTotal - countFloat;	
			float floatErrorRatio = floatErrorAbs / (float)countTotal;
			
			if(!(floatErrorRatio < 0.01f && floatErrorAbs < 5000)) {
				// Non-value property
				continue;
			}
		
			// Test integer errors - note these are relative to the float errors:
			// Using integers instead of float may only increase the error 
			int relIntErrorAbs = countFloat - countInt;
			float relIntErrorRatio = relIntErrorAbs / (float)countFloat;
			
			if(!(relIntErrorRatio < 0.01f && relIntErrorAbs < 5000)) {
				logger.trace("Identified datatype property (float): " + k);
				registerDatatypeProperty(k, XSD.xfloat.toString());
				//createDatatypeProperty(model, keyToURI(k), XSD.xfloat)
				++numFloats;
				keys.remove(k);
			}
			else { // Int
				logger.trace("Identified datatype property (int): " + k);			
				registerDatatypeProperty(k, XSD.xint.toString());
				//createDatatypeProperty(model, keyToURI(k), XSD.xint)
				++numInts;
				keys.remove(k);
			}
		}
		
		logger.info("Identified " + numInts + " integers, " + numFloats + " floats, " + numBooleans + " booleans");
		
		
		// TODO Analyze URIs		
		// Wiki links
		/*
		for(String key : keys) {
			String parts[] = languageSplit(key)
			if(parts == null)
				continue;
			
			if(parts[0].equalsIgnoreCase("Wikipedia")) {
				registerProperty("http://linkedgeodata.org/wikiLink", "http://linkedgeodata.org/method/wikipedia");
			}
		}
		*/
		
		
		
		// Analyze language properties
		int numLangProps = 0;
		List<String> tmpKeys = new ArrayList<String>(keys);
		Set<String> prefixSet = new HashSet<String>();
		for(String k : tmpKeys) {
			String[] parts = languageSplit(k);
			if(parts == null)
				continue;

			String prefix = parts[0];
			String langTag = parts[1];			

			if(languageTags.contains(langTag)) {
				keyToLanguageMap.put(k, langTag);

				registerTextProperty(k, prefix, langTag);
				
				keys.remove(k);

				++numLangProps;
				prefixSet.add(prefix);
			}
		}
		logger.info("Identified " + numLangProps + " as language properties, counted " + prefixSet.size() + " distinct prefixes");
		
		

		/*
		
		tmpKeys = new ArrayList<String>(keys);
		for(String k : tmpKeys) {
			if(!classes.contains(k))
				continue;

			registerClass(k);
			
			keys.remove(k);
		}
		*/
		
		logger.info("Remaining keys: " + keys.size());
		
		
		// Rule for defaulting everthing to text-datatype properties
		SimpleTagPattern tagPattern = new SimpleTagPattern((String)null, (String)null);
		SimpleTextTagMapper tagMapper = new SimpleTextTagMapper(LGDVocab.RESOURCE, tagPattern, null, false);
		tagMappers.add(tagMapper);
		
		logger.info("Number of rules registered: " + tagMappers.size());
		

		// Serialize the rules
		logger.info("Writing output");
		SerializationUtil.serializeXML(tagMappers, new File("LGDMappingRules.xml"));
		
		logger.info("Done.");
	}
	
	
	private static final Options cliOptions = initCLIOptions();
	
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static Options initCLIOptions()
	{
		Options options = new Options();
		
		options.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		options.addOption("d", "database", true, "Database name");
		options.addOption("u", "user", true, "");
		options.addOption("w", "password", true, "");
		options.addOption("h", "host", true, "");
		options.addOption("n", "batchSize", true, "Batch size");
		options.addOption("o", "outfile", true, "Output filename");		
		
		return options;
	}
}

