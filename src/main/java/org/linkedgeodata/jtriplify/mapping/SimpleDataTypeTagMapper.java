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

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.XSD;

public class SimpleDataTypeTagMapper
	extends AbstractOneOneTagMapper
{		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);

	private String dataType;
	private transient RDFDatatype rdfDataType;

	// TODO add switch for yes/no - true/false representations

	private Object readResolve()
	{
		TypeMapper tm = TypeMapper.getInstance();
		rdfDataType = tm.getSafeTypeByName(dataType);
		
		return this;
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
	public SimpleDataTypeTagMapper(String property, TagPattern tagPattern, String dataType, boolean isOSMEntity)
		throws Exception
	{
		//URI.create("http://linkedgeodata.org/method/simple?type=dt&dataType=" + URLEncoder.encode(dataType.toString(), "UTF-8")),
		super(property, tagPattern, isOSMEntity);

		this.dataType = dataType;
		
		readResolve();
	}
	
	public String getDataType()
	{
		return dataType;
	}

	@Override
	public Model _map(String subject, Tag tag, Model model)
	{
		String str = tag.getValue().trim().toLowerCase();
		
		// If the datatype is boolean
		if(rdfDataType.getURI().equals(XSD.xboolean.getURI())) {
			if(str.equals("yes")) str = "true";
			if(str.equals("no")) str = "false";
		}
				
				
		if(!rdfDataType.isValid(str)) {
			//logger.info("Failed to parse to'" + dataType + "', value: '" + str + "'");		
			return null;
		}

		model.add(
				model.getResource(subject),
				model.getProperty(super.getResource().toString()),
				str,
				rdfDataType);
		
		return model;
	}

	@Override
	public <T> T accept(IOneOneTagMapperVisitor<T> visitor)
	{
		return visitor.accept(this);
	}

	/*
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildDataTypeURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue(), dataType.getURI()).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	*/
}

