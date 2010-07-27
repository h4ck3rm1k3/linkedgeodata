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

import java.net.URLEncoder;

import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public class SimpleTextTagMapper
	extends AbstractSimpleOneOneTagMapper
{		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleTextTagMapper.class);

	private String langTag;
	

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
	public SimpleTextTagMapper(String property, SimpleTagPattern tagPattern, String langTag, boolean isOSMEntity)
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tagPattern, isOSMEntity);

		this.langTag = langTag;
	}

	public String getLanguageTag()
	{
		return langTag;
	}
	
	/*
	public Tag reverseMap(Triple triple)
	{
		if(!triple.getObject().isLiteral())
			return false;
		
		if(super.getTagPattern().getKey() != null) {
			
		}
	}*/
	
	
	public Model _map(String subject, Tag tag, Model model)
	{
		String suffix = "";
		
		if(super.getTagPattern().getKey() == null) {
			//suffix = tag.getKey();			
			suffix = URIUtil.myEncode(tag.getKey());
		}


		/*
		if(langTag == null) {
			result.add(
					result.getResource(subject.toString()),
					result.getProperty(super.getResource().toString() + suffix),
					tag.getValue());
		}
		else {*/
		model.add(
				model.getResource(subject),
				model.getProperty(super.getProperty().toString() + suffix),
				tag.getValue(),
				langTag);
			
		//}

		return model;
	}
	
	@Override
	public <T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor)
	{
		return visitor.accept(this);
	}

	@Override
	public String getObject(Tag tag)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildTextURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue(), langTag).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	 */
}

