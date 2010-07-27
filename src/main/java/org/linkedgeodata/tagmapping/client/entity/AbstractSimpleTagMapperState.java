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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

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
@Table(name="lgd_tag_mapping_simple_base")
@org.hibernate.annotations.Table(
		appliesTo="lgd_tag_mapping_simple_base",
		indexes = {
				@Index(name="idx_lgd_abstract_simple_tag_mapper_state_k_v", columnNames={"key", "value"}),
				@Index(name="idx_lgd_abstract_simple_tag_mapper_state_r", columnNames={"property"})
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
	
	/*
	@Embedded
    @AttributeOverrides( {
            @AttributeOverride(name="key", column = @Column(name="k") ),
            @AttributeOverride(name="value", column = @Column(name="v") )
    })
    */
	//@OneToOne
	@Embedded
	private SimpleTagPattern tagPattern;
	
	//boolean isPropertyPrefixMode = false;
	private String property;

	// Whether the tag pertains to the OSM entity, or the concept that
	// the resource represents
	private boolean describesOSMEntity = false;
	
	public AbstractSimpleTagMapperState()
	{
	}
	
	
	protected AbstractSimpleTagMapperState(String property, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.property = property;
		//this.method = null;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}

	protected AbstractSimpleTagMapperState(String property, String method, SimpleTagPattern tagPattern, boolean describesOSMEntity)
	{
		this.property = property;
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

	public String getProperty()
	{
		return property;
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

	public void setProperty(String property)
	{
		this.property = property;
	}

	public void setTagPattern(SimpleTagPattern tagPattern)
	{
		this.tagPattern = tagPattern;
	}
	
	@Override
	public String toString()
	{
		return tagPattern.getKey() + ", " + tagPattern.getValue() + ", " + property + ", " + describesOSMEntity; 
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
