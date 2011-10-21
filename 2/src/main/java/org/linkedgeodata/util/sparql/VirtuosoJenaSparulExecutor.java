package org.linkedgeodata.util.sparql;

import java.util.Collection;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

public class VirtuosoJenaSparulExecutor
	extends VirtuosoSparqlExecutor
	implements ISparulExecutor
{
	public VirtuosoJenaSparulExecutor(VirtGraph graph)
	{
		super(graph);
	}

	@Override
	public void executeUpdate(String query)
		throws Exception
	{
		if(getGraphName() != null) {
			query = "define input:default-graph-uri <" + getGraphName() + "> \n"
				+ query;
		}

		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, graph);
		vur.exec();
	}


	@Override
	public boolean insert(Model model, String graphName)
		throws Exception
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean remove(Model model, String graphName)
		throws Exception
	{
		throw new RuntimeException("Not implemented yet");
	}
	
	@Override
	public boolean insert(Collection<Triple> triples, String graphName)
			throws Exception
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean remove(Collection<Triple> triples, String graphName)
			throws Exception
	{
		throw new RuntimeException("Not implemented yet");
	}

}
