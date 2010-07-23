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
@Table(name="lgd_tag_mapping_simple_data_type")
public class SimpleDataTypeTagMapperState
	extends AbstractSimpleTagMapperState
	implements Serializable, IsSerializable
{		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String dataType;

	public SimpleDataTypeTagMapperState()
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
	public SimpleDataTypeTagMapperState(String property, SimpleTagPattern tagPattern, String dataType, boolean isOSMEntity)
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tagPattern, isOSMEntity);

		this.dataType = dataType;
	}
	
	public String getDataType()
	{
		return dataType;
	}
	
	public void setDataType(String dataType)
	{
		this.dataType = dataType;
	}
	
	@Override
	public <T> T accept(IEntityVisitor<T> visitor)
	{
		return visitor.visit(this);
	}
}

