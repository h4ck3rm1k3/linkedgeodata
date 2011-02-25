package org.linkedgeodata.core;

import org.apache.commons.lang.NotImplementedException;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class DeprecatedLGDVocab
	implements ILGDVocab
{
	public static final String NS = "http://linkedgeodata.org/";
	
	public static final String RESOURCE = NS + "triplify/";
	
	public static final String NODE = RESOURCE + "node";
	public static final String WAY = RESOURCE + "way";
	
	public static final String ONTOLOGY = NS + "ontology/";
	
	public static final Property MEMBER_OF_WAY = ResourceFactory.createProperty(ONTOLOGY + "memberOfWay");
	public static final Property HAS_NODES = ResourceFactory.createProperty(ONTOLOGY + "hasNodes");
	

	public static final Resource NODE_CLASS = ResourceFactory.createResource(ONTOLOGY + "Node");
	public static final Resource WAY_CLASS = ResourceFactory.createResource(ONTOLOGY + "Way");

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
