package org.linkedgeodata.util.sparql.cache;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class CacheTest
{
	@Test
	public void test()
		throws Exception
	{
		Set<Triple> baseTriples = new HashSet<Triple>();
		baseTriples.add(new Triple(Node.createURI("http://s.org"), Node.createURI("http://p.org"), Node.createURI("http://o.org")));

		Set<Triple> addedTriples = new HashSet<Triple>();
		addedTriples.add(new Triple(Node.createURI("http://x.org"), Node.createURI("http://y.org"), Node.createURI("http://z.org")));
		
		Set<Triple> all = Sets.union(baseTriples, addedTriples);
		
		
		MemoryGraph base = new MemoryGraph();
		base.add(baseTriples);

		
		TripleCacheIndexImpl.create(base, 1000, 1000, 100, 0); 
		
		DeltaGraph delta = new DeltaGraph(base);
		
		delta.add(addedTriples);
		//delta.remove(baseTriples);
		
		//delta.remove(addedTriples);
	
		//delta.commit();
		
		Set<Triple> triples = delta.bulkFind(null, new int[]{});
		System.out.println(all.equals(triples));
		
		System.out.println(triples);
		
		Set<Triple> x = base.bulkFind(null, new int[]{});
		
		System.out.println(x);
		
	}
	
	
	
}
