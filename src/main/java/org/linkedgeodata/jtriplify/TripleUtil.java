package org.linkedgeodata.jtriplify;

import java.net.URI;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


/**
 *  
 * @author raven
 *
 */
public class TripleUtil
{
	/**
	 * Simple helper function for adding triples to a graph
	 * 
	 * @param g
	 * @param s
	 * @param p
	 * @param o
	 * @return
	 */
	public static Triple add(Graph g, Object s, Object p, Object o)
	{
		Triple t = auto(s, p, o);
		g.add(t);
		
		return t;
	}
	
	/*
	public static Quad add(NamedGraphSet out, Object g, Triple t)
	{
		/*
		if(g == null) {
			Graph tmp = out.getGraph(Node.NULL);
			tmp.add(t);
		}
		* /
		
		Node a = autoNode(g);
		Quad q = new Quad(a, t);
	
		out.addQuad(q);
		return q;
	}
	
	public static Quad add(NamedGraphSet gs, Object g, Object s, Object p, Object o)
	{
		Node a = autoNode(g);
		Node b = autoNode(s);
		Node c = autoNode(p);
		Node d = autoObject(o); 

		Quad q = new Quad(a, b, c, d);
		gs.addQuad(q);
		
		return q;
	}
*/
	
	/**
	 * Auto generates a Jena Triple.
	 * 
	 * The magic it does: Automatically detects
	 * Jena Resource Nodes, Nodes, URIs and Strings are subject and predicate.
	 * 
	 * @param s
	 * @param p
	 * @param o
	 * @return
	 */
	public static Triple auto(Object s, Object p, Object o)
	{
		Node a = autoNode(s);
		Node b = autoNode(p);
		Node c = autoObject(o); 

		Triple t = new Triple(a, b, c);
		return t;
	}
	
	// Name of this method is a bit misleading:
	// If the argument is already a node, it will return that node
	// Therefore this might as will be a literal node.
	public static Node autoURINode(Object o)
	{
		if(Node.class.isAssignableFrom(o.getClass())) {
			Node node = (Node)o;
			return node;
		}
		else if(Resource.class.isAssignableFrom(o.getClass())) {
			Resource resource = (Resource)o;
			return Node.createURI(resource.getURI());
		}		
		if(URI.class.isAssignableFrom(o.getClass())) {
			URI uri = (URI)o;
			return Node.createURI(uri.toString());
		}
		
		return null;
	}
	
	public static Node autoNode(Object o)
	{
		if(o == null)
			return Node.NULL;
		
		Node uriNode = autoURINode(o);
		if(uriNode != null) {
			return uriNode;
		}
		else if(o.getClass() == String.class) {
			String str = (String)o;
			return Node.createURI(str);
		}
		else if(o.getClass() == Integer.class) {
			int id = (Integer)o;
			return Node.createAnon(new AnonId(Integer.toString(id)));
		}
		
		return null;
	}
	
	/**
	 * Todo: Map the java datatype to an xsd datatype. For now just use strings.
	 * @param m
	 * @param o
	 * @return
	 */
	public static Node autoObject(Object o)
	{
		Node uriNode = autoURINode(o);
		if(uriNode != null) {
			return uriNode;
		}
		else if(Node.class.isAssignableFrom(o.getClass()))
		{
			return (Node)o;
		}
		else if(o.getClass() == String.class) {
			String str = (String)o;
			return Node.createLiteral(str);
		} 
		
		return null;
	}
	
	
	

	public static Statement add(Model m, Object s, Object p, Object o)
	{
		Resource a = autoSubject(m, s);
		Property b = autoProperty(m, p);
		RDFNode  c = autoObject(m, o); 
		
		Statement stmt =
			m.createStatement(a, b, c);
		
		m.add(stmt);
		return stmt;
	}

	public static Resource autoSubject(Model m, Object o)
	{
		if(Resource.class.isAssignableFrom(o.getClass())) {
			Resource resource = (Resource)o;
			return resource;
			//return m.createResource(uri.toString());
		}		
		if(URI.class.isAssignableFrom(o.getClass())) {
			URI uri = (URI)o;
			return m.createResource(uri.toString());
		}
		else if(o.getClass() == String.class) {
			String str = (String)o;
			return m.createResource(str);
		}
		else if(o.getClass() == Integer.class) {
			int id = (Integer)o;
			return m.createResource(new AnonId(Integer.toString(id)));
		}
		
		return null;
	}
	
	public static Property autoProperty(Model m, Object o)
	{
		if(Property.class.isAssignableFrom(o.getClass())) {
			Property property = (Property)o;
			return property;
		}		
		if(Resource.class.isAssignableFrom(o.getClass())) {
			Resource resource = (Resource)o;
			return m.createProperty(resource.toString());
		}		
		if(o.getClass() == URI.class) {
			URI uri = (URI)o;
			return m.createProperty(uri.toString());
		}
		else if(o.getClass() == String.class) {
			String str = (String)o;
			return m.createProperty(str);
		} 

		return null;
	}

	/**
	 * Todo: Map the java datatype to an xsd datatype. For now just use strings.
	 * @param m
	 * @param o
	 * @return
	 */
	public static RDFNode autoObject(Model m, Object o)
	{
		if(o.getClass() == URI.class) {
			URI uri = (URI)o;
			return m.createResource(uri.toString());
		}
		else if(o.getClass() == String.class) {
			String str = (String)o;
			return m.createLiteral(str);
		} 
		
		return null;
	}
}