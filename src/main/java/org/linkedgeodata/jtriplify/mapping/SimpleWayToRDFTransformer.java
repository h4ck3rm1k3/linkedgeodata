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

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.jtriplify.TagMapper;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class SimpleWayToRDFTransformer
	implements ITransformer<Way, Model>
{
	//private static final String WGS84_WAY = "";

	private TagMapper tagMapper;
	private ILGDVocab vocab;
	
	public SimpleWayToRDFTransformer(TagMapper tagMapper, ILGDVocab vocab)
	{
		this.tagMapper = tagMapper;
		this.vocab = vocab;
	}
	
	@Override
	public Model transform(Model model, Way way)
	{
		
		String subject = getSubject(way);
		//Resource subjectRes = model.getResource(subject + "#id");
		
		//generateWGS84(model, subjectRes, node);
		//generateGeoRSS(model, subjectRes, node);
		SimpleNodeToRDFTransformer.generateTags(tagMapper, model, subject, way.getTags());

		//model.createResource(subject).addProperty(RDF.type, model.createResource(WGS84_WAY));
		
		return model;
	}
	
	@Override
	public Model transform(Way way)
	{
		Model model = ModelFactory.createDefaultModel();
		
		return transform(model, way);
	}
	
	private String getSubject(long id)
	{
		return vocab.createNIRWayURI(id);
	}
	
	private String getSubject(Way way)
	{
		return getSubject(way.getId());
	}
	
	//public static void generateGeoRSS(Model model, Resource subjectRes, node);

}
