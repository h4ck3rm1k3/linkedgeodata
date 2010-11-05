package org.linkedgeodata.util.sparql.cache;

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
