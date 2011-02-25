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
package org.linkedgeodata.core;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class LGDVocab
	implements ILGDVocab
{
	public static final String NS = "http://linkedgeodata.org/";
	
	public static final String RESOURCE = NS + "triplify/";
	
	public static final String NODE = RESOURCE + "node";
	public static final String WAY = RESOURCE + "way";
	
	public static final String ONTOLOGY_NS = NS + "ontology/";
	
	public static final Property MEMBER_OF_WAY = ResourceFactory.createProperty(ONTOLOGY_NS + "memberOfWay");
	public static final Property HAS_NODES = ResourceFactory.createProperty(ONTOLOGY_NS + "hasNodes");
	
	public static final Resource NODE_CLASS = ResourceFactory.createResource(ONTOLOGY_NS + "Node");
	public static final Resource WAY_CLASS = ResourceFactory.createResource(ONTOLOGY_NS + "Way");

	public Resource getNodeClass()
	{
		return NODE_CLASS;
	}

	public Resource getWayClass()
	{
		return WAY_CLASS;
	}
	
	// NIR = Non-Information-Resource
	@Override
	public Resource createNIRNodeURI(long id)
	{
		//return NODE + "/_" + id;
		return ResourceFactory.createResource(NODE + id);
	}
	
	@Override
	public Resource createOSMNodeURI(long id)
	{
		return createNIRNodeURI(id);
	}
	
	@Override
	public Resource createNIRWayURI(long id)
	{
		//return WAY + "/_" + id;
		return ResourceFactory.createResource(WAY + id);
	}	

	@Override
	public Resource createOSMWayURI(long id)
	{
		return createNIRWayURI(id);
	}

	@Override
	public String getBaseNS()
	{
		return NS;
	}

	@Override
	public Property getHasNodesPred()
	{
		return HAS_NODES;
	}

	@Override
	public Property getMemberOfWayPred()
	{
		return MEMBER_OF_WAY;
	}

	@Override
	public String getResourceNS()
	{
		return RESOURCE;
	}

	@Override
	public String getOntologyNS()
	{
		return ONTOLOGY_NS;
	}

	@Override
	public Resource createResource(Entity entity)
	{
		Resource resource = null;
		if(entity instanceof Node) {
			resource = createNIRNodeURI(entity.getId());
		}
		else if(entity instanceof Way) {
			resource = createNIRWayURI(entity.getId());
		}
		
		return resource;
	}

	@Override
	public Resource getHasNodesResource(Long wayId)
	{
		// http://linkedgeodata.org/resource/way123/nodes
		String res = createNIRWayURI(wayId) + "/nodes";

		Resource result = ResourceFactory.createResource(res);
		return result;
	}	
	
	
	public Resource wayNodeToWay(Resource res)
	{
		String str = res.getURI().toString();
		String suffix = "/nodes";
		if(!str.endsWith(suffix))
			return null;
		
		str = str.substring(0, str.length() - suffix.length());
		
		return ResourceFactory.createResource(str);
	}

	// TODO: Validate whether this is a way node
	public Resource wayToWayNode(Resource res)
	{
		return ResourceFactory.createResource(res.getURI().toString() + "/nodes");
	}
}
