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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Table;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Abstract Mapping rule between a single tag pattern and a resource.
 * 
 * 
 * TODO remove method - its always null anyway
 * 
 * @author raven
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Table(
		appliesTo="AbstractSimpleTagMapperState",
		indexes = {
				@Index(name="idx_lgd_abstract_simple_tag_mapper_state_k_v", columnNames={"key", "value"}),
				@Index(name="idx_lgd_abstract_simple_tag_mapper_state_r", columnNames={"resource"})
		}
)
public abstract class AbstractSimpleTagMapperState
	extends AbstractTagMapperState
	implements Serializable, IsSerializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SimpleTagPattern tagPattern;
	
	private String resource;

	// Whether the tag pertains to the OSM entity, or the concept that
	// the resource represents
	private boolean describesOSMEntity = false;
	
	public AbstractSimpleTagMapperState()
	{
	}
	
	
	protected AbstractSimpleTagMapperState(String resource, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		//this.method = null;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}

	protected AbstractSimpleTagMapperState(String resource, String method, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		//this.method = method;
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

	public String getResource()
	{
		return resource;
	}

	public SimpleTagPattern getTagPattern()
	{
		return tagPattern;
	}


	/*
	public String getMethod()
	{
		return method;
	}
	public void setMethod(String method)
	{
		this.method = method;
	}
	*/

	public boolean isDescribesOSMEntity()
	{
		return describesOSMEntity;
	}

	public void setDescribesOSMEntity(boolean describesOSMEntity)
	{
		this.describesOSMEntity = describesOSMEntity;
	}

	public void setResource(String resource)
	{
		this.resource = resource;
	}

	public void setTagPattern(SimpleTagPattern tagPattern)
	{
		this.tagPattern = tagPattern;
	}
	
	@Override
	public String toString()
	{
		return tagPattern.getKey() + ", " + tagPattern.getValue() + ", " + resource + ", " + describesOSMEntity; 
	}
	
	@Transient
	public String getK()
	{
		return tagPattern.getKey();
	}
	
	@Transient
	public String getV()
	{
		return tagPattern.getValue();
	}
	
	/*
	public void setK(String k)
	{
		this.tagPattern.setKey(k);
	}
	
	public void setV(String v)
	{
		this.tagPattern.setValue(v);
	}*/
	

	/*
	public String getKey()
	{
		return tagPattern.getKey();
	}
	
	public String getValue()
	{
		return tagPattern.getValue();
	}*/
}
