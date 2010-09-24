package org.linkedgeodata.core;

import org.apache.commons.lang.NotImplementedException;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.Resource;

public class DeprecatedLGDVocab
	implements ILGDVocab
{
	public static final String NS = "http://linkedgeodata.org/";
	
	public static final String RESOURCE = NS + "triplify/";
	
	public static final String NODE = RESOURCE + "node";
	public static final String WAY = RESOURCE + "way";
	
	public static final String ONTOLOGY = NS + "ontology/";
	
	public static final String MEMBER_OF_WAY = ONTOLOGY + "memberOfWay";
	public static final String HAS_NODES = ONTOLOGY + "hasNodes";
	
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
		return ONTOLOGY;
	}

	@Override
	public Resource createResource(Entity entity)
	{
		throw new NotImplementedException();
	}

	@Override
	public Resource getHasNodesResource(Long wayId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource wayToWayNode(Resource res)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Resource wayNodeToWay(Resource res)
	{
		// TODO Auto-generated method stub
		return null;
	}	
}
