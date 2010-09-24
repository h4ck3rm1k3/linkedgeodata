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

import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * Mapper for relating a single tag to a triple with an object property.
 * 
 * If the value in tagPattern is NULL, the following mapping will be made:
 * (k, v) -> $subject resource prefix:UTF8Encode(v)
 * 
 * Otherwise
 * (k, v) -> $subject resource prefix
 * 
 * Note: super.resource is used for the object of the generated triples.
 * This is because then the SimpleObjectPropertyTagMapper can be seen as a more
 * general version of the SimpleClassTagMapper, where the property is not
 * restricted to rdf:type.
 * 
 * 
 * 
 * @author raven
 *
 */
public class SimpleObjectPropertyTagMapper
	extends AbstractSimpleOneOneTagMapper
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	private boolean objectAsPrefix;
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
	public SimpleObjectPropertyTagMapper(String property, String object, boolean objectAsPrefix, SimpleTagPattern tagPattern, boolean isOSMEntity)
	{
		super(property, tagPattern, isOSMEntity);
		this.object = object;
		this.objectAsPrefix = objectAsPrefix;
	}
	
	public String getObject()
	{
		return object;
	}
	
	public boolean isObjectAsPrefix()
	{
		return objectAsPrefix;		
	}
	
	@Override
	public Model _map(String subject, Tag tag, Model model)
	{
		String suffix = "";
		
		if(super.getTagPattern().getValue() == null) {
			suffix = URIUtil.encodeUTF8(tag.getValue());
		}
		
		model.add(
				model.getResource(subject.toString()),
				model.getProperty(super.getProperty()),
				model.getResource(this.object + suffix)
		);
		
		return model;
	}
	
	@Override
	public <T> T accept(IOneOneTagMapperVisitor<T> visitor)
	{
		return visitor.accept(this);
	}

	
	public Tag getTag(String uri)
	{
		if(getTagPattern().getValue() != null && object.equals(uri)) {
			return new Tag(getTagPattern().getKey(), getTagPattern().getValue()); 
		}
		else if(objectAsPrefix == false && object.equals(uri)) {
			return new Tag(getTagPattern().getKey(), getTagPattern().getValue()); 			
		}
		else if(objectAsPrefix == false && uri.startsWith(object)) {
			String value = uri.substring(object.length());

			value = URIUtil.decodeUTF8(value);
			
			return new Tag(getTagPattern().getKey(), value); 
		}
		
		return null;		
	}

	
	@Override
	public String getObject(Tag tag)
	{
		if(!getTagPattern().matches(tag)) {
			return null;
		}

		String suffix = (isObjectAsPrefix() && (getTagPattern().getValue() == null))
			? URIUtil.encodeUTF8(tag.getValue())
			: "";
		
		String result = getObject() + suffix;
		
		return result;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + (objectAsPrefix ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SimpleObjectPropertyTagMapper))
			return false;
		SimpleObjectPropertyTagMapper other = (SimpleObjectPropertyTagMapper) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (objectAsPrefix != other.objectAsPrefix)
			return false;
		return true;
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
