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
import java.net.URLEncoder;

import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;


public class SimpleClassTagMapper
	extends AbstractSimpleOneOneTagMapper
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	
	
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
	public SimpleClassTagMapper(String clazz, SimpleTagPattern tagPattern, boolean isOSMEntity)
		//throws Exception
	{
		super(clazz, tagPattern, isOSMEntity);	
	}
	
	@Override
	public Model _map(String subject, Tag tag, Model model)
	{
		model.add(
				model.getResource(subject.toString()),
				RDF.type,
				model.getResource(super.getProperty().toString())
		);
		
		return model;
	}

	/*
	@Override
	public <T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor)
	{
		return visitor.accept(this);
	}
	*/

	@Override
	public String getObject(Tag tag)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
/*
	public Tag reverseMap(Triple triple)
	{
		// Predicate must be rdf:type
		if(!RDF.type.equals(triple.getPredicate()) && !triple.getObject().isURI())
		{
			return null;
		}
		
		String classURI = triple.getObject().getURI();

		String v = super.getTagPattern().getValue();
		if(v == null) {
			if(!classURI.startsWith(super.getResource().toString())) {
				return null;
			}
			
			v = classURI.substring(super.getResource().toString().length());
		}
		else {
			if(!classURI.equals(super.getResource().toString())) {
				return null;
			}
		}

		Tag result = new Tag(super.getTagPattern().getKey(), v);
		
		if(!super.matches(super.getTagPattern(), result))
			result = null;
			
		return result;
	}
*/

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

