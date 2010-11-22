package org.linkedgeodata.util.sparql.cache;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
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
	
	
	/**
	 * Tests the insertion of a triple after a previous lookup for the same
	 * subject yeld no results.
	 * 
	 * The expectation is, that this triple will go into the "full" index.
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInsertAfterCacheMiss()
		throws Exception
	{
		System.out.println("testInsertAfterCacheMiss");
		Set<Triple> baseTriples = new HashSet<Triple>();		
		Node subject = Node.createURI("http://s.org");
		baseTriples.add(new Triple(subject, Node.createURI("http://p.org"), Node.createURI("http://o.org")));

		
		MemoryGraph base = new MemoryGraph();
		TripleCacheIndexImpl.create(base, 1000, 1000, 100, 0); 

		
		// Assumed: partial.
		base.add(baseTriples);
		System.out.println("Status: " + base);
		
		
		base.remove(baseTriples);
		System.out.println("Status: " + base);
		//assertTrue(base..isEmpty());
		
		
		Set<Triple> triples = base.bulkFind(Collections.singleton(Collections.singletonList((Object)subject)), new int[]{0});
		
		System.out.println("Status: " + base);
		assertTrue(triples.isEmpty());
		
		
		
		base.add(baseTriples);
		
		
		triples = base.bulkFind(Collections.singleton(Collections.singletonList((Object)subject)), new int[]{0});
		System.out.println(base);
		
		System.out.println("Status: " + base);
		assertFalse(triples.isEmpty());

		
		base.remove(triples);

		System.out.println("Status: " + base);
		assertTrue(triples.isEmpty());
	}
	
	
}
