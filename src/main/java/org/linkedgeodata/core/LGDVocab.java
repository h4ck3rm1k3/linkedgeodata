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

public class LGDVocab
	implements ILGDVocab
{
	public static final String NS = "http://linkedgeodata.org/";
	
	public static final String RESOURCE = NS + "triplify/";
	
	public static final String NODE = RESOURCE + "node";
	public static final String WAY = RESOURCE + "way";
	
	public static final String ONTOLOGY_NS = NS + "ontology/";
	
	public static final String MEMBER_OF_WAY = ONTOLOGY_NS + "memberOfWay";
	public static final String HAS_NODES = ONTOLOGY_NS + "hasNodes";
	
	// NIR = Non-Information-Resource
	@Override
	public String createNIRNodeURI(long id)
	{
		//return NODE + "/_" + id;
		return NODE + id;
	}
	
	@Override
	public String createOSMNodeURI(long id)
	{
		return createNIRNodeURI(id);
	}
	
	@Override
	public String createNIRWayURI(long id)
	{
		//return WAY + "/_" + id;
		return WAY + id;
	}	

	@Override
	public String createOSMWayURI(long id)
	{
		return createNIRWayURI(id);
	}

	@Override
	public String getBaseNS()
	{
		return NS;
	}

	@Override
	public String getHasNodesPred()
	{
		return HAS_NODES;
	}

	@Override
	public String getMemberOfWayPred()
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
	public String createResource(Entity entity)
	{
		if(entity instanceof Node) {
			return createNIRNodeURI(entity.getId());
		}
		else if(entity instanceof Way) {
			return createNIRWayURI(entity.getId());
		}
		
		return null;
	}	
}
