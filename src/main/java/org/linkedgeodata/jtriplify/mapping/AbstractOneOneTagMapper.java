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
package org.linkedgeodata.jtriplify.mapping;

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
public abstract class AbstractOneOneTagMapper
	implements IOneOneTagMapper, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String resource;
	private String method;
	private TagPattern tagPattern;

	// Whether the tag pertains to the OSM entity, or the concept that
	// the resource represents
	private boolean describesOSMEntity = false;
	
	protected AbstractOneOneTagMapper(String resource, TagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		this.method = null;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}

	protected AbstractOneOneTagMapper(String resource, String method, TagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		this.method = method;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}
	
	public boolean describesOSMEntity()
	{
		return describesOSMEntity;
	}

	public String getResource()
	{
		return resource;
	}

	public TagPattern getTagPattern()
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
	

	protected abstract Model _map(String subject, Tag tag, Model model);
}
