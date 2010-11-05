package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;

public class DeltaGraph
		extends BaseIndexedGraph
{
	private IGraph		baseGraph;

	private Set<Triple>	additionTriples;
	private Set<Triple>	removalTriples;

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
		baseGraph.remove(removalTriples);
		baseGraph.add(additionTriples);

		removalTriples.clear();
		additionTriples.clear();
	}

	public void addTriple(Triple triple)
	{
		removalTriples.remove(triple);
		additionTriples.add(triple);
	}

	public void removeTriple(Triple triple)
	{
		additionTriples.remove(triple);
		removalTriples.add(triple);
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