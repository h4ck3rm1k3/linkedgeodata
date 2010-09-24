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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Abstract Mapping rule between a single tag pattern and a resource.
 * 
 * 
 * TODO remove method - its always null anyway
 * 
 * @author raven
 *
 */
public abstract class AbstractSimpleOneOneTagMapper
	implements ISimpleOneOneTagMapper, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String property;
	private String method;
	private SimpleTagPattern tagPattern;

	// Whether the tag pertains to the OSM entity, or the concept that
	// the resource represents
	private boolean describesOSMEntity = false;
	
	protected AbstractSimpleOneOneTagMapper(String resource, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.property = resource;
		this.method = null;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}

	protected AbstractSimpleOneOneTagMapper(String resource, String method, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.property = resource;
		this.method = method;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}
	
	public boolean matches(Tag tag)
	{
		return tagPattern.matches(tag);
	}
	
	public boolean describesOSMEntity()
	{
		return describesOSMEntity;
	}

	public String getProperty()
	{
		return property;
	}

	public SimpleTagPattern getTagPattern()
	{
		return tagPattern;
	}


	public String getMethod()
	{
		return method;
	}

	@Override
	public Model map(String subject, Tag tag, Model model)
	{
		if(!tagPattern.matches(tag)) {
			return null;
		}

		/* Disable this feature for now
		if(!describesOSMEntity())
			subject += "#id";
		*/

		if(model == null)
			model = ModelFactory.createDefaultModel(); 
		
		return _map(subject, tag, model);
	}

	
	/**
	 * Method for reverse mapping an URI to a tag
	 * Not sure if this method belongs here
	 *  
	 */ 
	public Tag getTag(String res)
	{
		if(!res.startsWith(property))
			return null;
		
		return new Tag(tagPattern.getKey(), tagPattern.getValue()); 
	}


	/**
	 * Returns the resource that may be build on the tag
	 * 
	 */
	//@Override
	//public String getObject(Tag tag);
	/*
	{
		if(!tagPattern.matches(tag)) {
			return null;
		}

		String result = property;
		
		return result;
	}*/
	

	protected abstract Model _map(String subject, Tag tag, Model model);

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (describesOSMEntity ? 1231 : 1237);
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		result = prime * result
				+ ((tagPattern == null) ? 0 : tagPattern.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractSimpleOneOneTagMapper))
			return false;
		AbstractSimpleOneOneTagMapper other = (AbstractSimpleOneOneTagMapper) obj;
		if (describesOSMEntity != other.describesOSMEntity)
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		if (tagPattern == null) {
			if (other.tagPattern != null)
				return false;
		} else if (!tagPattern.equals(other.tagPattern))
			return false;
		return true;
	}
}
