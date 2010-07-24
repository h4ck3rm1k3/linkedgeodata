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
import java.util.Collection;
import java.util.List;

import org.linkedgeodata.dao.OntologyGeneratorVisitor;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapperVisitor;
import org.linkedgeodata.osm.mapping.impl.SimpleClassTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleObjectPropertyTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTextTagMapper;
import org.linkedgeodata.util.ModelUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;


public class TagMapperOntologyGenerator
{	
	public static void main(String[] args)
		throws Exception
	{
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		tagMapper.load(new File("output/LGDMappingRules.xml"));
	
		List<IOneOneTagMapper> list = tagMapper.asList();

		Model result = ModelFactory.createDefaultModel();
		ModelUtil.read(result, new File("Namespaces.ttl"), "TTL");
		
		OntologyGeneratorVisitor visitor = new OntologyGeneratorVisitor(result, tagMapper);
		
		for(IOneOneTagMapper item : list) {
			if(item instanceof ISimpleOneOneTagMapper)
				((ISimpleOneOneTagMapper)item).accept(visitor);
		}
		
		FileOutputStream out = new FileOutputStream(new File("output/Schema.ttl"));
		
		visitor.getModel().write(out, "N3");
	}
}
