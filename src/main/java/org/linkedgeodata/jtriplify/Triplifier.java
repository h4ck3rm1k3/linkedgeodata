package org.linkedgeodata.jtriplify;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.linkedgeodata.util.SinglePrefetchIterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class Triplifier
{
	public static final String USE_NEXT_COLUMN = "t:unc";

	public static void main(String[] args)
		throws Exception
	{
		System.out.println("JTriplifyTest");

		String fileName = "NamespaceResolv.ini";
		File file = new File(fileName);
		if(!file.exists()) {
			throw new FileNotFoundException(fileName);
		}
		
		Transformer<String, URI> uriResolver = new URIResolver(file);
		
		System.out.println(uriResolver.transform("default"));
		
		Object subject = uriResolver.transform("foaf:Test");
		
		//System.out.println(triplify(subject, "hasNode->node", "X", uriResolver));
		System.out.println(triplify(subject, "wgs84_pos:lat^^xsd:decimal", "10", uriResolver));
	}
	
	public static List<String> getColumnNames(ResultSet rs)
		throws SQLException
	{
		ResultSetMetaData metaData = rs.getMetaData();

		List<String> result = new ArrayList<String>();
		for(int i = 1; i <= metaData.getColumnCount(); ++i) {
			String columnName = metaData.getColumnName(i);
			result.add(columnName);
		}

		return result;
	}
	
	public static Set<Triple> processResultSet(ResultSet rs, Transformer<String, URI> uriResolver)
		throws Exception
	{
		Set<Triple> result = new HashSet<Triple>();

		List<String> columnNames = getColumnNames(rs);	
		Iterator<List<?>> it = new ResultSetIterator(rs);
		
		while(it.hasNext()) {
			List<?> row = it.next();

			Set<Triple> rowTriples = triplify(0, columnNames, row, uriResolver);

			result.addAll(rowTriples);
		}
		
		return result;
	}


	public static Set<Triple> triplify(int subjectIndex, List<String> columnNames, List<? extends Object> row, Transformer<String, URI> uriResolver)
		throws Exception
	{
		Set<Triple> result = new HashSet<Triple>();
		
		int n = Math.min(columnNames.size(), row.size());
	
		if(n == 0)
			return result;
		
		if(subjectIndex > n) {
			throw new IndexOutOfBoundsException("Subject column " + subjectIndex + " > " + n);
		}
		
		if(columnNames.get(columnNames.size() - 1).equals("t:unc")) {
			throw new RuntimeException("t:unc as last column");
		}
		
		Object subjectVal = row.get(subjectIndex);
		URI subject = uriResolver.transform(subjectVal.toString());
		

		for(int i = 0; i < n; ++i) {
			if(i == subjectIndex)
				continue;
	
			Object v = row.get(i);
			Object p = columnNames.get(i);
			if(p.equals("t:unc")) {
				++i;

				p = row.get(i);
			}

			
			Triple triple = triplify(subject, p.toString(), v.toString(), uriResolver);
			
			result.add(triple);
		}
		
		return result;
	}


	/**
	 * This function mimics the original PHP-triplify behaviour.
	 * 
	 * FIXME Add special treatment for SQL data objects
	 * 
	 * 
	 * @param subject
	 * @param raw
	 * @param val
	 * @param uriResolver
	 * @return
	 */
	public static Triple triplify(
			Object subject,
			String raw,
			String val,
			Transformer<String, URI> uriResolver)
	{
		
		String predicateStr = raw;
		String dataType = null;
		String languageTag = null;
		String objectPrefix = "";
		Object object = null;
		
		boolean isPredicateAnObjectProperty = false;
		
		// Check for data type on the raw predicate
		int dataTypeSymbolIndex = raw.indexOf("^^");
		if(dataTypeSymbolIndex != -1) {
			dataType = raw.substring(dataTypeSymbolIndex + 2);
			predicateStr = raw.substring(0, dataTypeSymbolIndex);
		}

		// Check for language tag on the raw predicate
		int languageTagSymbolIndex = raw.indexOf('@');
		if(languageTagSymbolIndex != -1) {
			languageTag = raw.substring(languageTagSymbolIndex + 1);
			predicateStr = raw.substring(0, languageTagSymbolIndex);
		}

		// The '->' operator seems to be a shortcut for defining
		// a prefix for object-values
		int mappingSymbolIndex = raw.indexOf("->");
		if(mappingSymbolIndex != -1) {
			objectPrefix = raw.substring(mappingSymbolIndex + 2);
			predicateStr = raw.substring(0, mappingSymbolIndex);
			isPredicateAnObjectProperty = true;
		}
		
		
		if(isPredicateAnObjectProperty) {
			if(objectPrefix == null) {
				object = uriResolver.transform(val);
			}
			else {
				// Attempt to resolve the objectPrefix
				// e.g. the objectPrefix could be something like foaf
				URI testResolve = uriResolver.transform(objectPrefix + ":" + val);
				if(testResolve != null)
					object = testResolve;
				else
					object = uriResolver.transform(objectPrefix) + "/" + val;
			}
		}
		else { // Datatype property
			if(languageTag != null) {
				object = Node.createLiteral(val, languageTag, false);
			}
			else if(dataType != null) {
				// TODO It's too greate instancing a model for each triple
				Model model = ModelFactory.createDefaultModel();

				object = model.createTypedLiteral(val, dataType).asNode();
			}
			else {
				object = val;
			}
		}

		URI predicateURI = uriResolver.transform(predicateStr);
		
		Triple result = TripleUtil.auto(subject, predicateURI, object);
		
		return result;
	}
}

