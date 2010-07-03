/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */ 
package org.linkedgeodata.scripts;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;

import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.jtriplify.mapping.IOneOneTagMapper;
import org.linkedgeodata.jtriplify.mapping.simple.ISimpleOneOneTagMapper;
import org.linkedgeodata.jtriplify.mapping.simple.ISimpleOneOneTagMapperVisitor;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleClassTagMapper;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleDataTypeTagMapper;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleTextTagMapper;
import org.linkedgeodata.util.ModelUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

class OntologyGeneratorVistor
	implements ISimpleOneOneTagMapperVisitor<Void>
{
	private Model model;
	private TagMapper tagMapper;
	
	public OntologyGeneratorVistor(TagMapper tagMapper)
	{
		this.model = ModelFactory.createDefaultModel();
		this.tagMapper = tagMapper;
	}

	public OntologyGeneratorVistor(Model model, TagMapper tagMapper)
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
				Set<ISimpleOneOneTagMapper> candidates = tagMapper.lookup(m.getTagPattern().getKey(), null);
				for(ISimpleOneOneTagMapper item : candidates) {
					if(item instanceof SimpleClassTagMapper) {
						//SimpleClassTagMapper classMapper = (SimpleClassTagMapper)item;
						Resource parentClass = model.createResource(item.getResource());
						
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


public class TagMapperOntologyGenerator
{
	public static void main(String[] args)
		throws Exception
	{
		TagMapper tagMapper = new TagMapper();
		tagMapper.load(new File("output/LGDMappingRules.xml"));
	
		List<IOneOneTagMapper> list = tagMapper.asList();

		Model result = ModelFactory.createDefaultModel();
		ModelUtil.read(result, new File("Namespaces.ttl"), "TTL");
		
		OntologyGeneratorVistor visitor = new OntologyGeneratorVistor(result, tagMapper);
		
		for(IOneOneTagMapper item : list) {
			if(item instanceof ISimpleOneOneTagMapper)
				((ISimpleOneOneTagMapper)item).accept(visitor);
		}
		
		FileOutputStream out = new FileOutputStream(new File("output/Schema.ttl"));
		
		visitor.getModel().write(out, "N3");
	}
}
