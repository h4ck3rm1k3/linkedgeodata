package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.graph.Triple;

public interface IGraph
{
	/**
	 * Adds triples to the graph
	 * 
	 * @param triple
	 */
	void add(Collection<Triple> triple);
	void remove(Collection<Triple> triple);
	
	Collection<IGraphListener> getGraphListeners();
	ICacheProvider getCacheProvider();

	Collection<Triple> bulkFind(Collection<List<Object>> keys, int[] indexColumns);
	//Multimap<List<Object>, List<Object>> bulkFind(Collection<List<Object>> keys, int[] indexColumns);	
	
	Collection<Triple> uncachedBulkFind(Collection<List<Object>> keys, int[] indexColumns);
	//Multimap<List<Object>, List<Object>> uncachedBulkFind(List<List<Object>> keys, int[] indexColumns);
			
	/**
	 * An explicitely uncached version of bulkFind.
	 * 
	 * Idealy, only a cache provider should call this method.
	 * 
	 * @param pattern
	 * @return
	 */
	//Collection<Triple> uncachedBulkFind(Collection<Triple> patterns); 
}