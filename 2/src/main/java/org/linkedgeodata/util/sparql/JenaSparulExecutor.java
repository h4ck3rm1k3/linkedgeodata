package org.linkedgeodata.util.sparql;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.linkedgeodata.util.sparql.cache.TripleUtils;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;

public class JenaSparulExecutor
	implements ISparulExecutor
{
	private Model model;
	
	
	public JenaSparulExecutor(Model model)
	{
		this.model = model;
	}

	@Override
	public List<QuerySolution> executeSelect(String query) throws Exception
	{
		QueryExecution eq = QueryExecutionFactory.create(query, model);
		return ResultSetFormatter.toList(eq.execSelect());
	}

	@Override
	public boolean executeAsk(String query) throws Exception
	{
		return QueryExecutionFactory.create(query, model).execAsk();
	}

	@Override
	public Model executeConstruct(String query) throws Exception
	{
		return QueryExecutionFactory.create(query, model).execConstruct();
	}

	@Override
	public String getGraphName()
	{
		return null;
	}

	@Override
	public void executeUpdate(String query) throws Exception
	{
		throw new NotImplementedException();		
	}

	@Override
	public boolean insert(Model other, String graphName) throws Exception
	{
		model.add(other);
		return true;
	}

	@Override
	public boolean remove(Model other, String graphName) throws Exception
	{
		model.remove(other);
		return true;
	}

	@Override
	public boolean insert(Collection<Triple> triples, String graphName)
			throws Exception
	{
		return insert(TripleUtils.toModel(triples), graphName);
	}

	@Override
	public boolean remove(Collection<Triple> triples, String graphName)
			throws Exception
	{
		return remove(TripleUtils.toModel(triples), graphName);
	}

}
