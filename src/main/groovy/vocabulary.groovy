import org.openstreetmap.osmosis.core.pgsql.v0_6.impl.TagMapper;

import java.net.URI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.apache.log4j.Logger 
import org.apache.log4j.PropertyConfigurator 
import org.linkedgeodata.util.ModelUtil
import org.linkedgeodata.util.StringUtil 
import org.linkedgeodata.util.SerializationUtil
import org.linkedgeodata.jtriplify.TagEntityMap 
import org.linkedgeodata.jtriplify.mapping.IOneOneTagMapper 
import org.linkedgeodata.jtriplify.mapping.TagPattern
import org.linkedgeodata.jtriplify.mapping.SimpleClassTagMapper
import org.linkedgeodata.jtriplify.mapping.SimpleDataTypeTagMapper 
import org.linkedgeodata.jtriplify.mapping.SimpleTextTagMapper 
import org.openstreetmap.osmosis.core.domain.v0_6.Tag 

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property 
import com.hp.hpl.jena.rdf.model.Resource 
import com.hp.hpl.jena.vocabulary.OWL 
import com.hp.hpl.jena.vocabulary.RDF 
import com.hp.hpl.jena.vocabulary.RDFS
import com.hp.hpl.jena.vocabulary.XSD 
import org.apache.commons.collections15.MultiMap
import org.apache.commons.collections15.multimap.MultiHashMap


/***
 * Some thoughts on the datamodel
 * 
 * As the keys and values are semistructured, its in general impossible to
 * define a datatype which captures the actual values.
 * However, its possble to define functions which map p% of the fields to
 * a datatype.
 * This is what the method field in the entity map is for.
 * Whether the range of the property and range of the method are compatible
 * needs to be resolved at a different level.
 * This allows for specifying multiple methods for the generating the same property
 * However actually then each method needs a priority.
 * Methods with the same priority are all invoked in order to generate triples,
 * however if none of them succeeds, then methods in a lower priorty level are used.
 * For now I'll leave that to future work.
 * 
 * 
 * About the language tag:
 * Luckily some keys are actually typed with the language - so we don't have
 * to infer them in magical ways (e.g. letting google decide).
 * However this means that all keys with different languages should be
 * related to the same property, and the language should be taken from
 * the language defined for the key.
 * In concrete it looks like:
 * 
 * EntityMap(resource = 'myProp', k = 'myProp:en', v = null)
 * LanguageMap('myProp:en', 'en')
 * 
 * Another question: what is the context/scope of the language map?
 * Is it only for the mapping to the resource myProp, or is it global?
 * 
 * Also, if we do something like
 * TypeMap('myProp', 'int')
 * what does this mean?
 * . try to interpret myProp as integer?
 * . values of myProp are supposed to be integers?
 * . 
 * 
 * 
 * (Btw: as k and v should/could be seen as regex patterns (and not simple strings)):
 * or even more general: these are predicate object - ok, but in that case we can't optimize anything anymore)
 * Ok, for now lets got with simple strings - as this allows joining on the database
 * 
 * Hm, i just noticed: actually this is Method centric:
 * And simple methods only produces a single property
 * Method
 * 		type	 simple;
 * 		produces (prop1, obj1);
 * 		produces (prop2, obj2);
 * 		based on (k1, v1);
 * 		based on (k2, v2).
 * 
 * 
 * 
 */

/**
 * The purpose of this script is to automatically generate suggestions for
 * OWL classes, and object and datatype properties based on statistics on tags.
 *
 * The output of this script is twofold:
 * On the one hand it generates a model containing the properties, classes
 * and ranges for these properties.
 * On the other hand it generates a simple mapping table which relates
 * keys to the properties.
 * 
 * The advantage of the simple mapping table is, that given a certain key,
 * the property can be determined. complex mappings may involve regexes.
 * 
 * 
 * The result of this script is intended to be used as a fallback in cases where
 * manually designed rules fail to match.
 * 
 * Some rules: TODO maybe document that somewhere else put the refernce here
 * As a general rule: keys that match in one of the steps will be excluded from
 * subsequent ones.
 * 
 * The algorithm proceeds with the following steps
 * . TODO Optional: Specify a set of keys that should be ignored
 * 
 * . Keys that show a significant usage of (absolute) URIs are related to
 *   object properties
 *
 * . The values of a key are checked whether they would be suitable
 *   datatype properties (int/float).
 *  
 * . All keys that end with a language tags become datatype properties.
 *
 *   
 * . Keys 
 *   
 * . All remaining keys will becomed datatype properties
 *   
 *  
 */

class MyClass
{
	private Connection conn = null;

	//private static final String LGD_RES = "http://linkedgeodata.org/triplify/"
	private static final String LGD_VOCAB = "http://linkedgeodata.org/vocabulary#"

	private Set<String> languageTags = ["en", "cz", "fr", "ru", "jp", "it", "de"]
	
	// The set of keys that needs to be processed. Keys that are not in the
	// set are ignored
	private Set<String> keys = new HashSet<String>()
	                 
	
	private List<IOneOneTagMapper> tagMappers = new ArrayList<IOneOneTagMapper>();
	                                    
	private Map<String, String> keyToLanguageMap = new HashMap<String, String>()
	Model model = ModelFactory.createDefaultModel()
	
	def getRanges(Model model, String subject)
	{
		return model.getProperty(subject).
			listProperties(RDFS.range).collect{item -> item.getObject()}
	}

	def createDataTypeProperty(Model model, String subject, Resource range)
	{
		Property prop = model.getProperty(subject)
		prop.addProperty(RDFS.domain, range)
		prop.addProperty(RDF.type, OWL.DatatypeProperty)
		//model.add(subject, RDFS.domain, range)
		//model.add(subject, RDF.type, OWL.DatatypeProperty);
	}


	def createClass(Model model, String subject)
	{
		Resource sub = model.getResource(subject);
		sub.addProperty(RDF.type, OWL.Class);
	}
	
	
	def String toCamelCase(String s)
	{
		int offset = 0;
		String result = "";
		for(;;) {
			int newOffset = s.indexOf('_', offset);
			if(newOffset == -1) {
				result += StringUtil.ucFirst(s.substring(offset));
				break;
			}
			
			result += StringUtil.ucFirst(s.substring(offset + 1));
			offset = newOffset;
		}
		
		return result;
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

		TypeMapper tm = TypeMapper.getInstance();
		RDFDatatype dataType = tm.getSafeTypeByName(range.toString());

		SimpleDataTypeTagMapper tagMapper = new SimpleDataTypeTagMapper(resource, new TagPattern(k, null), dataType, false);
		tagMappers.add(tagMapper);
		
		createDataTypeProperty(model, resource, range);
	}
	
	
	
	
	def String keyToURI(String key)
	{
		return LGD_VOCAB + URLEncoder.encode(key, "UTF-8");
	}

	def Connection connectPostGIS(String hostName, String dbName, String userName, String passWord)
		throws Exception
	{
		String url = "jdbc:postgresql://" + hostName + "/" + dbName;
		//Connection connection = DriverManager.getConnection(url);
		Connection conn = DriverManager.getConnection(url, userName, passWord);
		
		return conn;
	}
	
	public static void main(String[] args)
	{
		MyClass c = new MyClass();
		c.run();
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
	def String[] languageSplit(String k)
	{	
		int index = k.lastIndexOf(':')
		if(index == -1)
			return null
		
		String tag = k.substring(index + 1).trim().toLowerCase()			
		String prefix = k.substring(0, index)
		
		return [prefix, tag]
	}

	private void loadKeys()
	{
		ResultSet rs = null;
		rs = conn.createStatement().executeQuery(
				"""\
				SELECT 
						p.k
				FROM
						lgd_properties p
				""")
		while(rs.next())
		{
			String k = rs.getString("k")

			keys.add(k);
		}
	}

	/* Does not work - too much noise
	private loadLanguageTags()
	{
		ResultSet rs = null;
		rs = conn.createStatement().executeQuery(
				"""\
				SELECT 
						p.k
				FROM
						lgd_properties p
				""")
		while(rs.next())
		{
			String k = rs.getString("k")
			
			String[] parts = getLanguageKey(k)
			if(parts == null)
				continue
			
			if(languageTagSamples.contains(parts[1])) {
				println
				languageKeys.add(parts[0])
			}
		}
				
		//println languageKeys
				
		// Iterate again and find all additional languages
		rs = conn.createStatement().executeQuery(
				"""\
				SELECT 
						p.k
				FROM
						lgd_properties p
				""")
		while(rs.next())
		{
			String k = rs.getString("k")
			
			String[] parts = getLanguageKey(k)
			if(parts == null)
				continue
				
			if(languageKeys.contains(parts[0])) {
				String tag = parts[1].trim()
				if(!tag.isEmpty()) {
					println parts[0] + " --- " + parts[1]
					languageTags.add(tag)
				}
			}
		}		
		
		println languageTags
	}
	*/
	
	public void run()
	{
		println "Starting"

		PropertyConfigurator.configure("log4j.properties")
		Logger logger = Logger.getLogger("vocab")
		
		//select distinct k from node_tags where v = 'no' or v='yes';
		//select * from properties where usage_count > 10 * distinct_value_count and distinct_value_count > 10 ORDER BY k;		
		
		conn = connectPostGIS("localhost", "lgd", "postgres", "postgres")
		ResultSet rs
		
		
		
		// Phase 0: Gather list of keys to process
		
		
		
		Set<TagEntityMap> temSet = new HashSet<TagEntityMap>();
		
		
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
		List<String> classes = new ArrayList<String>(["highway","barrier","cycleway","waterway","lock","railway","aeroway","aerialway","power","man_made","building","leisure","amenity","shop","tourism","historic","landuse","military","natural","route","boundary","sport"])
		rs = conn.createStatement().executeQuery(
				"""\
				SELECT 
						k, distinct_value_count
				FROM
						lgd_properties
				""")
		while(rs.next())
		{
			def(String k, int dvc) = [rs.getString("k"), rs.getInt("distinct_value_count")];

			if(!keys.contains(k))
				continue;
			
			if(!classes.contains(k))
				continue;

			registerClass(k);
			logger.debug("Registered class: $k");

			
			String escapedK = k.replace("'", "''");
			ResultSet rs2 = conn.createStatement().executeQuery(
					"""\
					SELECT 
						v, COUNT(*) AS count
					FROM
						node_tags
					WHERE
						k = '$escapedK'
					GROUP BY
						v
					""")
		
			while(rs2.next()) {
				def(String v, int c) = [rs2.getString("v"), rs2.getInt("count")];

				if(c > 10) {
					kv.put(k, v);
					vk.put(v, k);
				}
			}

			keys.remove(k);
		}

		for(Map.Entry<String, Collection<String>> item : kv.entrySet()) {
			def(String k, Collection<String> vs) = [item.getKey(), item.getValue()];
			
			for(String v : vs) {
				// This avoids values we use as subclasses here to become
				// datatype properties later
				// (e.g. amenity biergarten ; biergarten yes)
				keys.remove(v);
				
				String subClassStr = v;
				if(vk.get(v).size() != 1) {
					logger.info("Disambiguating: $k $v: $vk");
					// arialway, tower becomes arialwayTower
					// the register(sub)class method than makes the first letter of
					// the class upper case too
					//v = k + StringUtil.ucFirst(v);
					
					// or better leave case as is, and separate with underscore
					// TODO Check if compound name is unique
					subClassStr = k + "_" + v;
				}
			                           
				registerSubClass(k, v, subClassStr)
				logger.debug("Registered Sub-class: $k, $v");
			}
		}
		kv = null;
		vk = null;
		
		
		// Phase 1: Identify integer and float datatype properties
		// TODO Create table lgd_stats_datatypes if it doesn't exist
		logger.info("Analyzing integers and float")
		
		rs = conn.createStatement().executeQuery(
		"""\
		SELECT 
				sd.k, sd.count_total, sd.count_int, sd.count_float
		FROM
				lgd_stats_datatypes sd
		""")
		
		
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
		while(rs.next())
		{			
			def(String k, int countTotal, int countInt, int countFloat) =
				[rs.getString("k"), rs.getInt("count_total"), rs.getInt("count_int"), rs.getInt("count_float")]
		
			if(!keys.contains(k))
				continue;

			
			// Skip non frequently use properties in order to avoid conflicts
			// E.g. the key clock is used as a key maybe 5 times in the full dataset,
			// however amenity clock is mapped to the class clock
			// without skipping clock here it, clock would become a class
			// and a datatype property
			if(countTotal < 20)
				continue;
			
			// Test float errors
			int floatErrorAbs = countTotal - countFloat;	
			float floatErrorRatio = floatErrorAbs / (float)countTotal
			
			if(!(floatErrorRatio < 0.01f && floatErrorAbs < 5000)) {
				// Non-value property
				continue
			}
		
			// Test integer errors - note these are relative to the float errors:
			// Using integers instead of float may only increase the error 
			int relIntErrorAbs = countFloat - countInt
			float relIntErrorRatio = relIntErrorAbs / (float)countFloat
			
			if(!(relIntErrorRatio < 0.01f && relIntErrorAbs < 5000)) {
				logger.trace "Identified datatype property (float): {$k}"
				registerDatatypeProperty(k, XSD.xfloat)
				//createDatatypeProperty(model, keyToURI(k), XSD.xfloat)
				++numFloats;
				keys.remove(k);
			}
			else { // Int
				logger.trace "Identified datatype property (int): {$k}"			
				registerDatatypeProperty(k, XSD.xint)
				//createDatatypeProperty(model, keyToURI(k), XSD.xint)
				++numInts;
				keys.remove(k);
			}
		}
		
		logger.info("Identified {$numInts} integers, {$numFloats} floats")
		
		
		logger.info("Analyzing bools")
		rs = conn.createStatement().executeQuery(
		"""\
		SELECT 
				sb.k, sb.count_total, sb.count_yes, sb.count_no, sb.count_true, sb.count_false
		FROM
				lgd_stats_boolean sb
		""")
		
		int numBools = 0
		while(rs.next())
		{
			def(String k, int countTotal, int countYes, int countNo, int countTrue, int countFalse) =
				[rs.getString("k"), rs.getInt("count_total"), rs.getInt("count_yes"), rs.getInt("count_no"),
				 rs.getInt("count_true"), rs.getInt("count_false")]
		
			if(!keys.contains(k))
				continue;
		 
			if(countTotal < 20)
				continue;
		
			int countBool  = countYes + countTrue + countNo + countFalse;	
				 
			// Test float errors
			int boolErrorAbs = countTotal - countBool;	
			float boolErrorRatio = boolErrorAbs / (float)countTotal
			
			if(!(boolErrorRatio < 0.01f && boolErrorAbs < 5000)) {
				// Non-value property
				continue
			}
		
			logger.info "Identified datatype property (bool): {$k}"
			//createDatatypeProperty(model, keyToURI(k), XSD.xboolean)
			registerDatatypeProperty(k, XSD.xboolean);
			++numBools

			keys.remove(k);
		}
		logger.info("Identified {$numBools} as booleans")
		
		
		
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
			String[] parts = languageSplit(k)
			if(parts == null)
				continue;

			def(String prefix, String langTag) = [parts[0], parts[1]];
			

			if(languageTags.contains(langTag)) {
				keyToLanguageMap.put(k, langTag)

				registerTextProperty(k, prefix, langTag);
				
				keys.remove(k);

				++numLangProps;
				prefixSet.add(prefix);
			}
		}
		logger.info("Identified {$numLangProps} as language properties, counted " + prefixSet.size() + " distinct prefixes")
		
		

		/*
		
		tmpKeys = new ArrayList<String>(keys);
		for(String k : tmpKeys) {
			if(!classes.contains(k))
				continue;

			registerClass(k);
			
			keys.remove(k);
		}
		*/
		
		println "Remaining keys: " + keys.size()
		
		
		// Rule for defaulting everthing to text-datatype properties
		TagPattern tagPattern = new TagPattern((String)null, (String)null);
		SimpleTextTagMapper tagMapper = new SimpleTextTagMapper(LGD_VOCAB, tagPattern, null, false);
		tagMappers.add(tagMapper);
		
		println "Rules registered: " + tagMappers.size();
		
		
		// Serialize the model
		PrintStream out = new PrintStream(new File("Schema.n3"));
		out.println(ModelUtil.toString(model, "N3"));
		

		// Serialize the rules
		SerializationUtil.serializeXML(tagMappers, new File("LGDMappingRules.xml"));
		
		//println ModelUtil.toString(model, "N3")
	}
}

