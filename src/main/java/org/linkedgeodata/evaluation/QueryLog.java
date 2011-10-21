package org.linkedgeodata.evaluation;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.aksw.commons.sparql.core.SparqlEndpoint;
import org.aksw.commons.sparql.core.impl.HttpSparqlEndpoint;
import org.aksw.commons.util.Pair;
import org.aksw.commons.util.apache.ApacheLogDirectory;
import org.aksw.commons.util.apache.ApacheLogEntry;
import org.aksw.commons.util.apache.ApacheLogRangeEntryIterator;
import org.aksw.commons.util.strings.StringUtils;
import org.apache.commons.collections15.MultiMap;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.util.URIUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

// TODO properly acknowlegde these two sources:
// http://www.java2s.com/Code/Java/Development-Class/ParseanApachelogfilewithRegularExpressions.htm
// http://www.java2s.com/Code/Java/Regular-Expressions/ParseanApachelogfilewithStringTokenizer.htm




public class QueryLog
{

	
	public static boolean isEquivalent(Op a, Op b)
	{
		Set<Map<Node, Node>> isos = Equivalence.findIsomorphy(a, b);
		
		
		String xa = a.toString();
		String xb = b.toString();

		//System.out.println("XXXAAA " + xa);
		
		if (isos.isEmpty()) {
			return false;
		}

		for (Map.Entry<Node, Node> entry : isos.iterator().next().entrySet()) {
			xa = xa.replace(entry.getKey().toString(), entry.getValue()
					.toString());
		}

		return xa.equals(xb);
	}

	
	public static String createPrefixString(Map<String, String> prefixMap, String separator)
	{
		String result = "";
		for(Map.Entry<String, String> entry : prefixMap.entrySet()) {
			result += "PREFIX " + entry.getKey() + ":<" + entry.getValue() + ">" + separator;
		}
		
		return result;
	}
	
	
	public static String getSparqlQueryFromUri(String uri)
	{
		String values = uri.substring(uri.indexOf("?") + 1);

		MultiMap<String, String> queryMap = URIUtil.getQueryMap(values);

		String queryString = StringUtils.urlDecode(queryMap.get("query").iterator().next());
		
		return queryString;
	}
	
	
	
	public static <K> Integer increment(Map<K, Integer> map, K key) {
		Integer value = map.get(key);
		Integer newValue = value == null ? 1 : value + 1; 
		
		return map.put(key, newValue);
	}
	
	
	public static void main(String[] args) throws IOException
	{
		mainDistinctIps();
	}
	
	
	public static void mainDistinctIps() throws IOException
	{
		Map<String, Integer> ipToTotalQueryCount = new HashMap<String, Integer>();
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(
						"/home/raven/Documents/LinkedGeoData/QueryLogs/lgd-sparql.24-Nov-2010.22-Jun-2011.txt"))));
		
		

		String line;
		while ((line = reader.readLine()) != null) {
			


			String[] parts = line.split(" ", 2);

			String ip = parts[0];
			//String url = parts[2];
			
			
			/**
			 * Count distinct ips
			 */
			Integer queryCount = ipToTotalQueryCount.get(ip);
			if(queryCount == null) {
				queryCount = 0;
			}
			ipToTotalQueryCount.put(ip, queryCount + 1);
		}

		reader.close();

		SortedSet<Entry<String, Integer>> set = new TreeSet<Entry<String, Integer>>(
				new Comparator<Entry<String, Integer>>() {

					@Override
					public int compare(Entry<String, Integer> a,
							Entry<String, Integer> b)
					{
						return a.getValue().equals(b.getValue()) ? a.getKey().compareTo(b.getKey()) : b.getValue().compareTo(a.getValue());
					}
					
					
				});
		
		for(Entry<String, Integer> entry : ipToTotalQueryCount.entrySet()) {
			set.add(entry);
		}
		
		for(Entry<String, Integer> entry : set) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}

		long total = 0;
		for(Entry<String, Integer> entry : set) {
			total += entry.getValue();
		}
		
		
		System.out.println("distinct ips: " + ipToTotalQueryCount.keySet().size());
		System.out.println("total queryCount: " + total);		
		
	}
	
	
	/**
	 * TODO This method could be generalized to a sparql query log cleaner:
	 * Given an iterator over query strings, return:
	 * 
	 * . distinct queries
	 * . distinct errors
	 * . jena syntax errors
	 * . real syntax errors (syntax errors that are not due to jena not supporting some vendor specific syntax extension)
	 * 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void mainUniqueQueries(String[] args) throws Exception
	{
		/**
		 * Set up default prefixes
		 * 
		 */
		Map<String, String> defaultPrefixes = new HashMap<String, String>();
		defaultPrefixes.put("bif", "http://bif/");
		defaultPrefixes.put("rdf", RDF.getURI());
		defaultPrefixes.put("rdfs", RDFS.getURI());
		defaultPrefixes.put("geo", RDF.getURI());
		defaultPrefixes.put("lgdo", LGDVocab.ONTOLOGY_NS);

		
		PrefixMapping defaultPrefixMapping = new PrefixMappingImpl();
		defaultPrefixMapping.setNsPrefixes(defaultPrefixes);

		
		String prefixStr = createPrefixString(defaultPrefixes, "\n");
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(
						"/home/raven/Desktop/LGDSparql.txt"))));

		PrintWriter out = new PrintWriter(new FileOutputStream(new File("/home/raven/Desktop/LGDSparqlValid.txt")));

		
		//Set<String> ips = new HashSet<String>();
		Map<String, Integer> ipToTotalQueryCount = new HashMap<String, Integer>();
		Map<String, Integer> ipToToDistinctValidQueryCount = new HashMap<String, Integer>();
		//Map<String, Integer> ipToToDistinctValidQueryNonEmptyResultCount = new HashMap<String, Integer>();
		
		//SparqlEndpoint sparqlEndpoint = new VirtuosoSparqlEndpoint(new VirtGraph(
		
		// This endpoint is used to find out whether jena-parse errors are also
		// parse errors for virtuoso.
		SparqlEndpoint parserValidation = new HttpSparqlEndpoint("http://139.18.2.230:8950/sparql", "http://linkedgeodata.org");
		
		
		
		SparqlEndpoint sparqlEndpoint = new HttpSparqlEndpoint(
				"http://139.18.2.230:8950/sparql", "http://linkedgeodata.org");

		Map<Query, Pair<Op, Integer>> queryToCount = new HashMap<Query, Pair<Op, Integer>>();

		String line;
		int failCount = 0;
		
		while ((line = reader.readLine()) != null) {
			
			String originalQueryString = "";
			String queryString = "";
			try {
				String[] parts = line.split("\t");

				String ip = parts[1];
				String url = parts[2];

				if(ip.contains("139.18.")) {
					continue;
				}
				
				
				/**
				 * Count distinct ips
				 */
				Integer queryCount = ipToTotalQueryCount.get(ip);
				if(queryCount == null) {
					queryCount = 0;
				}
				ipToTotalQueryCount.put(ip, queryCount + 1);

				if(true) {
					//continue;
				}
				
				/**
				 * Get the sparql query string from the uri
				 */
				originalQueryString = getSparqlQueryFromUri(url);

				// HACK for ruling out some virtuoso specific features.
				queryString = originalQueryString.replaceAll("SELECT[^{<]*", "SELECT * ");
				
				//queryString = "SELECT ?school ?schoolgeo WHERE { ?school   rdf:type      lgdo:amenity_school ; geo:geometry  ?schoolgeo ; rdfs:label    ?schoolname . ?coffeeshop  rdf:type   lgdo:coffeeshop ; geo:geometry  ?coffeeshopgeo ; rdfs:label    ?coffeeshopname . FILTER ( bif:st_intersects(?schoolgeo, bif:st_point(4.892222, 52.373056), 5) && bif:st_intersects(?coffeeshopgeo, ?schoolgeo, 1) ) } Limit 13 Offset10";

				/**
				 * Parse the query and do some normalization:
				 * 
				 * We are NOT interested in:
				 * . the exact projection
				 * . the limit
				 * . the offset
				 */				
				Query query = new Query();
				query.setPrefixMapping(defaultPrefixMapping);
				QueryFactory.parse(query, queryString, null, Syntax.syntaxSPARQL);				
				
				query.setLimit(Query.NOLIMIT);
				query.setOffset(Query.NOLIMIT);

				if(query.isSelectType()) {
					query.setDistinct(false);
					query.setQueryResultStar(true);
					query.getProject().getVars().clear();
					query.getProject().getExprs().clear();
				}
								

				Op opQuery = Algebra.compile(query);
				opQuery = Algebra.toQuadForm(opQuery);
				
				boolean found = false;
				for (Map.Entry<Query, Pair<Op, Integer>> entry : queryToCount.entrySet()) {
					Op opPrevious = entry.getValue().getKey();
					
					if (isEquivalent(opQuery, opPrevious)) {
						
						//entry.getValue().setValue(entry.getValue().getValue() + 1);
						entry.setValue(Pair.create(entry.getValue().getKey(), entry.getValue().getValue() + 1));
						found = true;

						System.out.println("EQUIVALENT - " + queryToCount.size() + " - " + entry.getValue());
						System.out.println(opPrevious);
						System.out.println(query);
					}
				}

				if (found == false) {
					System.out.println("NEW - " + queryToCount.size());
					System.out.println(query);
					queryToCount.put(query, Pair.create(opQuery, 1));
					
					
					out.println(StringUtils.urlDecode(query.toString()));
				}

				/*
				 * Monitor mon = MonitorFactory.start();
				 * 
				 * ResultSet rs; try { rs = sparqlEndpoint.executeSelect(query);
				 * } catch(Throwable t) { ++failCount;
				 * System.out.println("failCount: " + failCount); continue; }
				 * 
				 * int i = 0; while(rs.hasNext()) { QuerySolution qs =
				 * rs.next(); ++i; //System.out.println(rs.next()); }
				 * mon.stop();
				 * 
				 * if(i == 0) { continue; }
				 * 
				 * System.out.println(mon);
				 */
			} catch (Throwable t) {
				try {
					parserValidation.executeSelect(queryString);
					
					//System.out.println("Failcount: " + (++failCount));
					System.out.println("Query was: " + queryString);
					t.printStackTrace(); 
					
				} catch(Exception e) {
					System.out.println("Validated Failcount: " + (++failCount));
					//System.out.println("Query was: " + queryString);
				}				
			}
		}

		reader.close();
		out.flush();
		out.close();

		for(Map.Entry<String, Integer> entry : ipToTotalQueryCount.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
		//System.out.println("distinct ips: " + ips.size());
		//System.out.println("queryCount: " + queryCount);
	}

	public static void filterLog(String[] args) throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		File dir = new File("/home/raven/Desktop/lgd/QueryLogs/all/");

		ApacheLogDirectory logDir = new ApacheLogDirectory(dir, Pattern.compile("access.*"));

		Date low = new GregorianCalendar(2011, 3, 10, 0, 0, 0).getTime();
		Date high = new GregorianCalendar(2011, 3, 17, 12, 0, 0).getTime();

		// low = new GregorianCalendar(2011, 3, 17, 0, 0, 0).getTime();
		// high = new GregorianCalendar(2011, 3, 19, 12, 0, 0).getTime();

		low = null;
		high = null;

		File outFile = new File("/home/raven/Desktop/LGDSparql.txt");
		PrintWriter writer = new PrintWriter(outFile);

		ApacheLogRangeEntryIterator it = logDir.getIterator(low, high, true,
				true);

		DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

		int i = 0;
		while (it.hasNext()) {
			ApacheLogEntry entry = it.next();

			String uri = entry.getRequest().getUrl();
			// *
			try {
				StringUtils.urlDecode(uri);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}// */

			if (!(uri.contains("sparql") && uri.contains("query=") && uri
					.contains("linkedgeodata")))
				continue;

			writer.println(dateFormat.format(entry.getDate()) + "\t"
					+ entry.getHostname() + "\t" + uri);
			// ++i;

			// System.out.println(i + " --- " + entry.getDate() + " --- ");
		}

		writer.flush();
		writer.close();
		// processFile(new
		// File("/home/raven/Desktop/lgd/QueryLogs/access.log"));
	}

	public static void processFile(File file) throws IOException,
			ParseException
	{

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));

		String line;
		while ((line = reader.readLine()) != null) {

			ApacheLogEntry entry = ApacheLogEntry.parse(line);

			String uri = entry.getRequest().getUrl();

			if (!(uri.contains("sparql") && uri.contains("query=") && uri
					.contains("linkedgeodata")))
				continue;

			System.out.println(StringUtils.urlDecode(uri));

			// String uri = parts[]
		}
	}
}
