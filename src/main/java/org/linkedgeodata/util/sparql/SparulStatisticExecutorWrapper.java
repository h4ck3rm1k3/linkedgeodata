package org.linkedgeodata.util.sparql;

import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;

public class SparulStatisticExecutorWrapper
	extends AbstractSparqlStatisticExecutorWrapper
	implements ISparulExecutor
{
	private ISparulExecutor	delegate;

	public SparulStatisticExecutorWrapper(ISparulExecutor delegate)
	{
		this.delegate = delegate;
	}

	@Override
	protected ISparulExecutor getDelegate()
	{
		return delegate;
	}

	@Override
	public void executeUpdate(String query)
		throws Exception
	{
		if (query == null)
			return;

		logger.trace("Update =\n" + query);
		StopWatch sw = new StopWatch();
		sw.start();

		//boolean result =
		getDelegate().executeUpdate(query);

		sw.stop();
		logger.trace("Update took: " + sw.getTime() + "ms.");
		
		//return result;
	}

	@Override
	public boolean insert(Model model, String graphName)
		throws Exception
	{
		logger.trace("Insert " + model.size() + " triples");
		StopWatch sw = new StopWatch();
		sw.start();

		boolean result = getDelegate().insert(model, graphName);

		sw.stop();
		logger.trace("Insert took: " + sw.getTime() + "ms.");
	
		return result;
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
