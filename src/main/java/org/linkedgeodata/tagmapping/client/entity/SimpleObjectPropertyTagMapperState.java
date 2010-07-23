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
package org.linkedgeodata.tagmapping.client.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.google.gwt.user.client.rpc.IsSerializable;

@Entity
@Table(name="lgd_tag_mapping_simple_object_property")
public class SimpleObjectPropertyTagMapperState
	extends AbstractSimpleTagMapperState
	implements Serializable, IsSerializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	private String object;
	
	public SimpleObjectPropertyTagMapperState()
	{
	}

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
	public SimpleObjectPropertyTagMapperState(String property, String object, SimpleTagPattern tagPattern, boolean isOSMEntity)
	{
		super(property, tagPattern, isOSMEntity);
		this.object = object;
	}
	
	public String getObject()
	{
		return object;
	}

	public void setObject(String object)
	{
		this.object = object;
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
	@Override
	public <T> T accept(IEntityVisitor<T> visitor)
	{
		return visitor.visit(this);
	}
}
