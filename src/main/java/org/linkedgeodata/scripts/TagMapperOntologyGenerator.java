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
import org.linkedgeodata.jtriplify.mapping.IOneOneTagMapperVisitor;
import org.linkedgeodata.jtriplify.mapping.SimpleClassTagMapper;
import org.linkedgeodata.jtriplify.mapping.SimpleDataTypeTagMapper;
import org.linkedgeodata.jtriplify.mapping.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.jtriplify.mapping.SimpleTextTagMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

class OntologyGeneratorVistor
	implements IOneOneTagMapperVisitor<Void>
{
	private Model model;
	private TagMapper tagMapper;
	
	public OntologyGeneratorVistor(TagMapper tagMapper)
	{
		this.model = ModelFactory.createDefaultModel();
		this.tagMapper = tagMapper;
	}

	@Override
	public Void accept(SimpleClassTagMapper m)
	{
		if(m.getTagPattern().getKey() != null) {
			model.getResource(m.getResource()).addProperty(RDF.type, OWL.Class);


			if(m.getTagPattern().getValue() != null) {
				model.createResource(m.getResource()).addProperty(RDF.type, OWL.Class);
				
				// Check if there might be a parent class
				Set<IOneOneTagMapper> candidates = tagMapper.lookup(m.getTagPattern().getKey(), null);
				for(IOneOneTagMapper item : candidates) {
					if(item instanceof SimpleClassTagMapper) {
						//SimpleClassTagMapper classMapper = (SimpleClassTagMapper)item;
						
						model.createResource(item.getResource())
							.addProperty(RDF.type, OWL.Class)
							.addProperty(RDFS.subClassOf, item.getResource());
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
	
		//Model result = ModelFactory.createDefaultModel();
		OntologyGeneratorVistor visitor = new OntologyGeneratorVistor(tagMapper);
		
		for(IOneOneTagMapper item : list) {
			item.accept(visitor);
		}
		
		FileOutputStream out = new FileOutputStream(new File("output/Schema.ttl"));
		
		visitor.getModel().write(out, "N3");
	}
}
