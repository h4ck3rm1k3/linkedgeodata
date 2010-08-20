package org.linkedgeodata.osm.mapping.impl;

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


/**
 * A class for generating RDF from OSM entities.
 * 
 * @author raven
 *
 */
public class OSMEntityToRDFTransformer
	implements ITransformer<Entity, Model>
{
	private ITransformer<Node, Model> nodeTransformer;
	private ITransformer<Way, Model> wayTransformer;
	
	public OSMEntityToRDFTransformer(
			ITransformer<Node, Model> nodeTransformer,
			ITransformer<Way, Model> wayTransformer)
	{
		this.nodeTransformer = nodeTransformer;
		this.wayTransformer = wayTransformer; 
	}
	
	
	/**
	 * Convenience constructor
	 * 
	 * @param tagMapper
	 * @param vocab
	 */
	public OSMEntityToRDFTransformer(ITagMapper tagMapper, ILGDVocab vocab)
	{
		this.nodeTransformer = new SimpleNodeToRDFTransformer(tagMapper, vocab);
		this.wayTransformer = new SimpleWayToRDFTransformer(tagMapper, vocab);
	}


	@Override
	public Model transform(Entity in)
	{
		return transform(null, in);
	}


	@Override
	public Model transform(Model out, Entity in)
	{
		if(out == null)
			out = ModelFactory.createDefaultModel();
		
		
		if(in instanceof Node) {
			nodeTransformer.transform(out, (Node)in);
		} else if (in instanceof Way) {
			wayTransformer.transform(out, (Way)in);
		}
			
		return out;
	}
}
