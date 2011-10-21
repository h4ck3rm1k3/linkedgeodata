package org.linkedgeodata.dao;

import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.impl.IOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
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
	implements IOneOneTagMapperVisitor<Void>
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
	
	/*
	@Override
	public Void accept(SimpleClassTagMapper m)
	{
		if(m.getTagPattern().getKey() != null) {
			Resource subClass = model.createResource(m.getProperty());
			subClass.addProperty(RDF.type, OWL.Class);
	
	
			if(m.getTagPattern().getValue() != null) {
				
				// Check if there might be a parent class
				Collection<? extends IOneOneTagMapper> candidates = tagMapper.lookup(m.getTagPattern().getKey(), null);
				for(IOneOneTagMapper item : candidates) {
					if(item instanceof SimpleClassTagMapper) {
						SimpleClassTagMapper classMapper = (SimpleClassTagMapper)item;
						
						Resource parentClass = model.createResource(classMapper.getProperty());
						
						parentClass.addProperty(RDF.type, OWL.Class);
						
						subClass.addProperty(RDFS.subClassOf, parentClass);
					}
				}
			}
		}
		
		return null;
	}
	*/
	private boolean isClassMapping(SimpleObjectPropertyTagMapper m)
	{
		return RDF.type.toString().equals(m.getProperty());
	}
	
	public Void processClass(SimpleObjectPropertyTagMapper m)
	{
		if(!isClassMapping(m))
			return null;
		
		if(m.getTagPattern().getKey() != null) {
			Resource subClass = model.createResource(m.getObject());
			subClass.addProperty(RDF.type, OWL.Class);
	
	
			if(m.getTagPattern().getValue() != null) {
				
				// Check if there might be a parent class
				Collection<? extends IOneOneTagMapper> candidates = tagMapper.lookup(m.getTagPattern().getKey(), null);
				for(IOneOneTagMapper item : candidates) {
					if(item instanceof SimpleObjectPropertyTagMapper) {
						SimpleObjectPropertyTagMapper classMapper = (SimpleObjectPropertyTagMapper)item;
						
						if(!isClassMapping(classMapper))
							continue;
						
						Resource parentClass = model.createResource(classMapper.getObject());
						
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
		model.createProperty(m.getProperty())
			.addProperty(RDF.type, OWL.DatatypeProperty)
			.addProperty(RDFS.range, model.createResource(m.getDataType()));
	
		return null;
	}
	
	@Override
	public Void accept(SimpleTextTagMapper m)
	{
		if(m.getTagPattern().getKey() == null && m.getTagPattern().getValue() == null)
			return null;
		
		model.createProperty(m.getProperty())
			.addProperty(RDF.type, OWL.DatatypeProperty);
		
		return null;
	}
	
	@Override
	public Void accept(SimpleObjectPropertyTagMapper m)
	{
		/*
		model.createProperty(m.getProperty())
			.addProperty(RDF.type, OWL.ObjectProperty);
		*/

		if(isClassMapping(m)) {
			processClass(m);
		}
		else {
			model.createProperty(m.getProperty())
			.addProperty(RDF.type, OWL.ObjectProperty);
		}
		
		return null;
	}
	
	public Model getModel()
	{
		return model;
	}

	@Override
	public Void accept(RegexTextTagMapper mapper)
	{
		throw new NotImplementedException();
	}
}

