package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Transformer;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TriplifyUtil
{
	public static Model triplify(ResultSet rs, Transformer<String, URI> uriResolver)
		throws Exception
	{
		Model model = ModelFactory.createDefaultModel();
		
		ResultSetMetaData metaData = rs.getMetaData();
		int subjectIndex = -1;
		
		for(int i = 1; i <= metaData.getColumnCount(); ++i) {
			if(metaData.getColumnName(i).equals("id")) {
				subjectIndex = i - 1;
			}
		}
		
		if(subjectIndex == -1)
			throw new RuntimeException("No id column found");
		
		// TODO move Triplifier.getColumnNames to SQL Utils
		List<String> columnNames = Triplifier.getColumnNames(rs);

		ResultSetIterator it = new ResultSetIterator(rs);
		while(it.hasNext()) {
			List<?> row = it.next();
			
			Set<Triple> triples = Triplifier.triplify(subjectIndex, columnNames, row, uriResolver);
		
			for(Triple triple : triples) {
				model.getGraph().add(triple);
			}
		}
		
		return model;
	}
}
