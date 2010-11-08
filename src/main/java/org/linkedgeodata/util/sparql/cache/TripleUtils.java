package org.linkedgeodata.util.sparql.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections15.map.LRUMap;
import org.linkedgeodata.util.collections.CacheSet;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.graph.GraphFactory;

public class TripleUtils
{
	public static Model toModel(Iterable<Triple> triples) {
		Graph graph = GraphFactory.createDefaultGraph();
		for(Triple triple : triples) {
			graph.add(triple);
		}
		return ModelFactory.createModelForGraph(graph);
	}

	public static Model toModel(Iterable<Triple> triples, Model model) {
		
		Model part = toModel(triples);
		model.add(part);
		
		return model;
	}

	public static Node get(Triple triple, int index)
	{
		switch(index) {
		case 0: return triple.getSubject();
		case 1: return triple.getPredicate();
		case 2: return triple.getObject();
		default: throw new IndexOutOfBoundsException();
		}
	}
}
