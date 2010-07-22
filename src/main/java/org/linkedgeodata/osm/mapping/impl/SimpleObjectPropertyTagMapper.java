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
package org.linkedgeodata.osm.mapping.impl;

import java.io.Serializable;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;


public class SimpleObjectPropertyTagMapper
	extends AbstractSimpleOneOneTagMapper
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	private final String object;
	
	/**
	 * Valid types are:
	 * type=int
	 * type=float
	 * type=bool
	 * type=uri
	 * type=lang, langTag=<>
	 * type=class
	 * 
	 * @param resource
	 * @param method
	 * @param tag
	 */
	public SimpleObjectPropertyTagMapper(String property, String object, SimpleTagPattern tagPattern, boolean isOSMEntity)
	{
		super(property, tagPattern, isOSMEntity);
		this.object = object;
	}
	
	public String getObject()
	{
		return object;
	}
	
	@Override
	public Model _map(String subject, Tag tag, Model model)
	{
		String suffix = "";
		
		if(super.getTagPattern().getValue() == null) {
			suffix = tag.getValue();
		}
		
		model.add(
				model.getResource(subject.toString()),
				model.getProperty(this.object + suffix),
				model.getResource(super.getResource().toString() + suffix)
		);
		
		return model;
	}
	
	@Override
	public <T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor)
	{
		return visitor.accept(this);
	}

	/*
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildClassURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue()).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	*/
}
