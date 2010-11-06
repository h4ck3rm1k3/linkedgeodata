package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;


/**
 * 
 * A wrapper for an underlying graph, whereas the uncachedBulkFind
 * method respects the added and removed triples.
 * 
 * @author raven
 *
 */
public class DeltaGraph
		extends BaseIndexedGraph
{
	private IGraph		baseGraph;

	//private Set<Triple>	additionTriples;
	//private Set<Triple>	removalTriples;

	
	private IGraph additionGraph = new MemoryGraph();
	private IGraph removalGraph = new MemoryGraph();
	
	public DeltaGraph(IGraph baseGraph) {
		this.baseGraph = baseGraph;
	}
	
	/**
	 * Performs the addition/removal on the base graph. Note that this does not
	 * change the set of triples in the graph.
	 * 
	 * Warning: Make sure that triples added to the base graph remain the same
	 * on retrieval - otherwise this would cause problems.
	 * 
	 * For instance, datatype information for certain literals get lost in
	 * virtuoso. Such critical triples should not be inserted. Wrapp this graph
	 * to transform critical triples to safe ones beforehand.
	 * 
	 */
	public void applyDelta()
	{
		baseGraph.remove(removalGraph.bulkFind(null, new int[]{}));
		baseGraph.add(additionGraph.bulkFind(null, new int[]{}));

		removalGraph.clear();
		additionGraph.clear();
	}

	public void addTriple(Triple triple)
	{
		removalGraph.remove(Collections.singleton(triple));
		additionGraph.add(Collections.singleton(triple));
	}

	public void removeTriple(Triple triple)
	{
		additionGraph.remove(Collections.singleton(triple));
		removalGraph.add(Collections.singleton(triple));
	}

	public IGraph getBaseGraph()
	{
		return baseGraph;
	}
	
	public IGraph getAdditionGraph()
	{
		return additionGraph;
	}
	
	public IGraph getRemovalGraph()
	{
		return removalGraph;
	}

	@Override
	public void add(Collection<Triple> triples)
	{
		for (Triple triple : triples) {
			addTriple(triple);
		}
	}

	@Override
	public void remove(Collection<Triple> triples)
	{
		for (Triple triple : triples) {
			removeTriple(triple);
		}
	}

	/*
	@Override
	public Collection<Triple> bulkFind(Collection<List<Object>> keys,
			int[] indexColumns)
	{
		
		
		// TODO Auto-generated method stub
		return null;
	}*/

	
	/**
	 * FIXME: Uncached bulkFind does NOT mean, that no caches will be used.
	 * It only means that the caches of the called graph object will not be
	 * used.
	 * 
	 */
	@Override
	public Set<Triple> uncachedBulkFind(Set<List<Object>> keys,
			int[] indexColumns)
	{
		
		// Perform a lookup on the database and merge the result with
		// the addition/removal graphs
		
		
		// Note: The base graph may have indexes on it,
		// but the addition/removal graphs need their own.
		
		// Ideally, the addition/removal graph would automatically
		// receive the same indexes as the base graph.
		
		
		// Ok, for now we ignore indexes completely
		Set<Triple> addTriples = additionGraph.bulkFind(keys, indexColumns);
		Set<Triple> removalTriples = removalGraph.bulkFind(keys, indexColumns); 
		
		Set<Triple> result = baseGraph.bulkFind(keys, indexColumns);
		
		result.removeAll(removalTriples);
		result.addAll(addTriples);
		
		return result;
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub
		additionGraph.clear();
		removalGraph.clear();
		baseGraph.clear();
	}
}