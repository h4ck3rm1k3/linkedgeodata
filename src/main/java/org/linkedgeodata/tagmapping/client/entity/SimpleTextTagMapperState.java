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

import javax.persistence.Entity;


@Entity
public class SimpleTextTagMapperState
	extends AbstractSimpleTagMapperState
{		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleTextTagMapper.class);

	private String langTag;
	
	public SimpleTextTagMapperState()
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
	public SimpleTextTagMapperState(String property, SimpleTagPattern tagPattern, String langTag, boolean isOSMEntity)
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tagPattern, isOSMEntity);

		this.langTag = langTag;
	}

	public String getLanguageTag()
	{
		return langTag;
	}
	
	public void setLanguageTag(String langTag)
	{
		this.langTag = langTag;
	}
	
	@Override
	public <T> T accept(IEntityVisitor<T> visitor)
	{
		return visitor.visit(this);
	}

}

