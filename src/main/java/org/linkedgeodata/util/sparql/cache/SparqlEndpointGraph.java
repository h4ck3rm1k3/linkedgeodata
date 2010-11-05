package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.linkedgeodata.util.sparql.ISparqlExecutor;

import com.hp.hpl.jena.graph.Triple;


/**
 * A graph over a sparql endpoint.
 * 
 * Warning: Public Sparql Endpoints may impose several limitations, such as
 * limiting the size of result sets. Make sure that the bulkFind method
 * always retrieves complete result sets for your uses.
 * 
 * @author raven
 *
 */
public class SparqlEndpointGraph
	extends BaseIndexedGraph
{

	@Override
	public Collection<Triple> bulkFind(Collection<List<Object>> keys,
			int[] indexColumns)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Triple> uncachedBulkFind(Collection<List<Object>> keys,
			int[] indexColumns)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
/*
	private ISparqlExecutor sparqlEndpoint;
	private Set<String> graphNames;

	@Override
	public Set<Triple> bulkFind(Collection<Triple> patterns)
	{
		Set<Triple> result = new HashSet<Triple>();
		
		
		// Patterns could be {(s1, null, null), (s2, null, null), ...}
		
		
		return result;
	}


	@Override
	public void add(Iterable<Triple> triple)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(Iterable<Triple> triple)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<Triple> uncachedBulkFind(Collection<Triple> patterns)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
*/

