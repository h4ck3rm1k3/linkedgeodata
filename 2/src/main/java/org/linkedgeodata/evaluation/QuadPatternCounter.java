package org.linkedgeodata.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.linkedgeodata.core.LGDVocab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class QuadPatternCounter
{
	private static final Logger logger = LoggerFactory.getLogger(QuadPatternCounter.class);
	

	public static void main(String[] args)
		throws IOException
	{
		Map<String, String> defaultPrefixes = new HashMap<String, String>();
		defaultPrefixes.put("bif", "http://bif/");
		defaultPrefixes.put("rdf", RDF.getURI());
		defaultPrefixes.put("rdfs", RDFS.getURI());
		defaultPrefixes.put("geo", RDF.getURI());
		defaultPrefixes.put("lgdo", LGDVocab.ONTOLOGY_NS);

		
		PrefixMapping defaultPrefixMapping = new PrefixMappingImpl();
		defaultPrefixMapping.setNsPrefixes(defaultPrefixes);


		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(
						"/home/raven/Desktop/LGDSparql.txt"))));
		
		Multimap<Integer, String> queryMap = HashMultimap.create();
		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
		
		
		String line;
		int i = 0;
		int errorCount = 0;
		int selectCount = 0;
		while((line = reader.readLine()) != null) {
			
			String str = "";
			try {
				
				String[] parts = line.split("\t");

				String ip = parts[1];
				String url = parts[2];
	
				str = QueryLog.getSparqlQueryFromUri(url);
				// HACK for ruling out some virtuoso specific features.
				str = str.replaceAll("SELECT[^{<]*", "SELECT * ");

				
				if(ip.equals("139.18.2.138")) {
					continue;
				}

			//String str = StringUtils.decodeUtf8(line);
		
			//System.out.println(str);
				++i;
			
			
			
			//Query query = QueryFactory.create(str);
			Query query = new Query();
			query.setPrefixMapping(defaultPrefixMapping);
			QueryFactory.parse(query, str, null, Syntax.syntaxSPARQL);				

			
			if(!query.isSelectType()) {
				continue;
			}

			

			if(query.isSelectType()) {
				++selectCount;
				query.setDistinct(false);
				query.setQueryResultStar(true);
				query.getProject().getVars().clear();
				query.getProject().getExprs().clear();
			}
							

			
			query.setLimit(Query.NOLIMIT);
			query.setOffset(Query.NOLIMIT);

			
			Op op = Algebra.compile(query);
			op = Algebra.toQuadForm(op);

			if(op == null) {
				return;
			}
			
			int numQuads = PatternUtils.collectQuads(op).size();
		
			
			QueryLog.increment(map, numQuads);
			
			if(numQuads >= 13) {
				queryMap.put(numQuads, str);
			}
			
			}
			catch(Exception e) {
				//System.err.println(str);
				//logger.error("Skipping a query due to some error");
				//e.printStackTrace();
				++errorCount;
			}
			//map.put(numQuads, str);
		}
		
		System.out.println("Total: " + i);
		System.out.println("Select queries: " + selectCount);
		System.out.println("Error count: " + errorCount);
		for(Entry<Integer, Integer> entry : map.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
		}
		

		List<Integer> keys = new ArrayList<Integer>(queryMap.keySet());
		Collections.sort(keys);

		for(Integer key : keys) {
			System.out.println(">>> " + key);
			for(String str : queryMap.asMap().get(key)) {
				System.out.println(str);
				System.out.println();
			}
		
		}		
	}
}
