package org.linkedgeodata.util.sparql;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SparulEndpointExecutor
	extends SparqlEndpointExecutor
	implements ISparulExecutor
{
	public SparulEndpointExecutor(String service, String graph)
	{
		super(service, graph);
	}

	@Override
	public void executeUpdate(String query)
	{
		if (query == null)
			return;

		QueryEngineHTTP queryExecution = new QueryEngineHTTP(service, query);
		queryExecution.addDefaultGraph(graph);

		queryExecution.execSelect();
	}


	@Override
	public boolean insert(Model model, String graphName)
	{
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public boolean remove(Model model, String graphName)
		throws Exception
	{
		throw new RuntimeException("Not implemented yet");
	}
}