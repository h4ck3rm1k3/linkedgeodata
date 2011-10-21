package org.linkedgeodata.util.sparql.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class CacheTest
{
	@Test
	public void testLookupAfterDeltaInsert()
		throws Exception
	{
		System.out.println("testLookupAfterDeltaInsert");
		
		Set<Triple> baseTriples = new HashSet<Triple>();
		Node subject = Node.createURI("http://s.org");
		baseTriples.add(new Triple(subject, Node.createURI("http://p.org"), Node.createURI("http://o.org")));

		Set<Triple> addedTriples = new HashSet<Triple>();
		addedTriples.add(new Triple(subject, Node.createURI("http://y.org"), Node.createURI("http://z.org")));
		
		Set<Triple> all = Sets.union(baseTriples, addedTriples);
		

		Set<List<Object>> keys = Collections.singleton(Collections.singletonList((Object)subject));
		int[] keyColumns = new int[]{0};

		MemoryGraph base = new MemoryGraph();
		base.add(baseTriples);

		
		TripleCacheIndexImpl.create(base, 1000, 1000, 100, 0);
		DeltaGraph delta = new DeltaGraph(base);

		delta.bulkFind(keys, keyColumns);
		System.out.println(base);
		
		
		
		delta.add(addedTriples);
		
		//delta.remove(baseTriples);
		
		//delta.remove(addedTriples);
	
		//delta.commit();
		
		Set<Triple> triples = delta.bulkFind(keys, keyColumns);
		System.out.println(all.equals(triples));
		
		System.out.println(triples);
		

		// PRE-COMMIT
		Set<Triple> x = base.bulkFind(keys, keyColumns);
		System.out.println(x);
		
		delta.commit();

		//POST-COMMIT
		x = base.bulkFind(null, new int[]{});
		System.out.println(x);

		System.out.println(base);
		
		base.remove(all);
		System.out.println(base);
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
		ITripleCacheIndex index = TripleCacheIndexImpl.create(base, 1000, 0, 100, 0); 
		
		// Add the base triples
		// This should make them partial
		base.add(baseTriples);
		//System.out.println("Status: " + base);
		Assert.assertEquals(index.getState(), new CacheState(0, 1, 0));
		

		// Remove the base triple again
		// This should make it empty
		base.remove(baseTriples);
		//System.out.println("Status: " + base);
		//assertTrue(base.isEmpty());
		Assert.assertEquals(index.getState(), new CacheState(0, 0, 0));
		
		
		Set<Triple> triples = base.bulkFind(Collections.singleton(Collections.singletonList((Object)subject)), new int[]{0});
		
		//System.out.println("Status: " + base);
		//assertTrue(triples.isEmpty());
		Assert.assertEquals(index.getState(), new CacheState(0, 0, 1));
		
		
		
		base.add(baseTriples);
		
		
		triples = base.bulkFind(Collections.singleton(Collections.singletonList((Object)subject)), new int[]{0});
		//System.out.println(base);
		
		//System.out.println("Status: " + base);
		//assertFalse(triples.isEmpty());
		Assert.assertEquals(index.getState(), new CacheState(1, 0, 0));

		
		base.remove(triples);

		//System.out.println("Status: " + base);
		Assert.assertEquals(index.getState(), new CacheState(0, 0, 1));
		
		triples = base.bulkFind(Collections.singleton(Collections.singletonList((Object)Node.createURI("http://miss.me"))), new int[]{0});
		//System.out.println("Status: " + base);		
		Assert.assertEquals(index.getState(), new CacheState(0, 0, 2));

	
		baseTriples.add(new Triple(Node.createURI("http://aeou.org"), Node.createURI("http://p.org"), Node.createURI("http://o.org")));
		base.add(baseTriples);
		
		//System.out.println("Status: " + base);
		Assert.assertEquals(index.getState(), new CacheState(1, 1, 1));
	
	}
	
	
}
