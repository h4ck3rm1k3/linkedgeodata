package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Triple;

public class DefaultCacheProvider
		implements ICacheProvider
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultCacheProvider.class);
	

	private IGraph	graph;

	public DefaultCacheProvider(IGraph graph)
	{
		this.graph = graph;
	}

	@Override
	public void clear()
	{
		for (ITripleCacheIndex index : tripleCacheIndexes) {
			index.clear();
		}
	}

	// private Multimap<Triple, ITripleCache> tripleCaches =
	// HashMultimap.create();
	private Set<ITripleCacheIndex>	tripleCacheIndexes	= new HashSet<ITripleCacheIndex>();

	
	// FIXME: Deal with cache misses
	@Override
	public Set<Triple> bulkFind(Set<List<Object>> keys, int[] indexColumns)
	{		
		// Find the set of indexes that are fully compatbile with our columns
		Set<ITripleCacheIndex> fullIndexes = new HashSet<ITripleCacheIndex>();
		Set<ITripleCacheIndex> partialIndexes = new HashSet<ITripleCacheIndex>();
		
		for (ITripleCacheIndex index : tripleCacheIndexes) {

			IndexCompatibilityLevel level = index
					.getCompatibilityLevel(indexColumns);

			if (level.equals(IndexCompatibilityLevel.FULL)) {
				fullIndexes.add(index);
			} else if(level.equals(IndexCompatibilityLevel.PARTIAL)) {
				// Note: An index that does not support partial partitions
				// returns compatiblity level NONE
				partialIndexes.add(index);
			}
		}
		
		
		
		Set<Triple> result = new HashSet<Triple>();
		Set<List<Object>> remaining = new HashSet<List<Object>>(keys);

		
		Iterator<List<Object>> it = remaining.iterator();
		while (it.hasNext()) {
			List<Object> key = it.next();

			for (ITripleCacheIndex index : fullIndexes) {
				Collection<Triple> triples = index.lookup(key);
				if(triples != null) { // cache hit
					result.addAll(triples);
					it.remove();
					break;
				}
			}
		}

		
		logger.trace("Cache hits: " + (keys.size() - remaining.size()));
		
		
		Collection<Triple> furtherTriples = graph.uncachedBulkFind(remaining, indexColumns);
		
		// FIXME Register cache misses
		
		
		// Register the triples at the caches
		for(ITripleCacheIndex index : fullIndexes) {
			index.index(furtherTriples);
		}
		
		for(ITripleCacheIndex index : partialIndexes) {
			index.addSeen(furtherTriples);
		}

		result.addAll(furtherTriples);
		
		return result;
	}
	

	@Override
	public void addSeen(Collection<Triple> triples)
	{
		for (ITripleCacheIndex index : tripleCacheIndexes) {
			index.addSeen(triples);
		}
	}

	@Override
	public void removeSeen(Collection<Triple> triples)
	{
		for (ITripleCacheIndex index : tripleCacheIndexes) {
			index.removeSeen(triples);
		}
	}

	@Override
	public IGraph getGraph()
	{
		return graph;
	}

	@Override
	public Collection<ITripleCacheIndex> getIndexes()
	{
		return tripleCacheIndexes;
	}
	
	@Override
	public String toString() {
		String result = "";
		
		for(ITripleCacheIndex index : tripleCacheIndexes) {
			result += index.toString() + "\n";
		}
		
		return result;
	}
}
