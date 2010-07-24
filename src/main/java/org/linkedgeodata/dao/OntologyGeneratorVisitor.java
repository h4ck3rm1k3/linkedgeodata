package org.linkedgeodata.dao;

import java.util.Collection;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class OntologyGeneratorVisitor
	implements ISimpleOneOneTagMapperVisitor<Void>
{
	private Model model;
	private ITagMapper tagMapper;
	
	public OntologyGeneratorVisitor(ITagMapper tagMapper)
	{
		this.model = ModelFactory.createDefaultModel();
		this.tagMapper = tagMapper;
	}
	
	public OntologyGeneratorVisitor(Model model, ITagMapper tagMapper)
	{
		this.model = model;
		this.tagMapper = tagMapper;
	}
	
	@Override
	public Void accept(SimpleClassTagMapper m)
	{
		if(m.getTagPattern().getKey() != null) {
			Resource subClass = model.createResource(m.getResource());
			subClass.addProperty(RDF.type, OWL.Class);
	
	
			if(m.getTagPattern().getValue() != null) {
				
				// Check if there might be a parent class
				Collection<? extends IOneOneTagMapper> candidates = tagMapper.lookup(m.getTagPattern().getKey(), null);
				for(IOneOneTagMapper item : candidates) {
					if(item instanceof SimpleClassTagMapper) {
						SimpleClassTagMapper classMapper = (SimpleClassTagMapper)item;
						
						Resource parentClass = model.createResource(classMapper.getResource());
						
						parentClass.addProperty(RDF.type, OWL.Class);
						
						subClass.addProperty(RDFS.subClassOf, parentClass);
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public Void accept(SimpleDataTypeTagMapper m)
	{
		model.createProperty(m.getResource())
			.addProperty(RDF.type, OWL.DatatypeProperty)
			.addProperty(RDFS.domain, model.createResource(m.getDataType()));
	
		return null;
	}
	
	@Override
	public Void accept(SimpleTextTagMapper m)
	{
		model.createProperty(m.getResource())
			.addProperty(RDF.type, OWL.DatatypeProperty);
		
		return null;
	}
	
	@Override
	public Void accept(SimpleObjectPropertyTagMapper m)
	{
		model.createProperty(m.getResource())
			.addProperty(RDF.type, OWL.ObjectProperty);
	
		return null;
	}
	
	public Model getModel()
	{
		return model;
	}
}

