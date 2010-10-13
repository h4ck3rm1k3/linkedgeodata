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

import java.util.Collection;
import java.util.Map;

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.linkedgeodata.util.ITransformer;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class SimpleWayToRDFTransformer
	implements ITransformer<Way, Model>
{
	//private static final String WGS84_WAY = "";

	private ITagMapper tagMapper;
	private ILGDVocab vocab;
	
	public SimpleWayToRDFTransformer(ITagMapper tagMapper, ILGDVocab vocab)
	{
		this.tagMapper = tagMapper;
		this.vocab = vocab;
	}
	
	@Override
	public Model transform(Model model, Way way)
	{

		Resource subject = getSubject(way);
		//Resource subjectRes = model.getResource(subject + "#id");
		
		//generateWGS84(model, subjectRes, node);
		//generateGeoRSS(model, subjectRes, node);

		SimpleNodeToRDFTransformer.generateTags(tagMapper, model, subject.toString(), way.getTags());

		generateWayNodes(model, vocab, way);
		//model.createResource(subject).addProperty(RDF.type, model.createResource(WGS84_WAY));
		
		return model;
	}
	
	@Override
	public Model transform(Way way)
	{
		Model model = ModelFactory.createDefaultModel();
		
		return transform(model, way);
	}
	
	private Resource getSubject(long id)
	{
		return vocab.createNIRWayURI(id);
	}
	
	private Resource getSubject(Way way)
	{
		return getSubject(way.getId());
	}
	
	private static void generateWayNodes(Model model, ILGDVocab vocab, Way way)
	{
		//for(Map.Entry<Long, Collection<Long>> entry : members.entrySet()) {
		Long wayId = way.getId();
			
		//Resource memberRes = model.createResource(new AnonId());
		Resource memberRes = vocab.getHasNodesResource(wayId);
		
		model.add(
				vocab.createOSMWayURI(wayId),
				model.createProperty(vocab.getHasNodesPred()),
				memberRes);

		model.add(memberRes, RDF.type, RDF.Seq);
		
		int i = 0;
		for(WayNode wayNode : way.getWayNodes()) {
			Long nodeId = wayNode.getNodeId();

			model.add(
					memberRes,
					model.createProperty(RDF.getURI() + "_" + (++i)),
					vocab.createOSMNodeURI(nodeId));
		}	
	}
	
	//public static void generateGeoRSS(Model model, Resource subjectRes, node);

}
