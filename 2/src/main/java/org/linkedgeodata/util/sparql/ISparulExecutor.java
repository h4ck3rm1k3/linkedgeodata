package org.linkedgeodata.util.sparql;

import java.util.Collection;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

public interface ISparulExecutor
	extends ISparqlExecutor
{
	void executeUpdate(String query)
		throws Exception;
	
	boolean insert(Model model, String graphName)
		throws Exception;

	boolean remove(Model model, String graphName)
		throws Exception;

	boolean insert(Collection<Triple> triples, String graphName)
		throws Exception;

	boolean remove(Collection<Triple> triples, String graphName)
		throws Exception;

}
