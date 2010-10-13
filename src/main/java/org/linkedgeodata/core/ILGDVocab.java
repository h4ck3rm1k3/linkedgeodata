package org.linkedgeodata.core;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Resource;

public interface ILGDVocab
{
	String getBaseNS();
	String getResourceNS();
	//String getNode
	
	String getHasNodesPred();
	String getMemberOfWayPred();

	// NIR = Non-Information-Resource
	Resource createNIRNodeURI(long id);
	Resource createOSMNodeURI(long id);	
	Resource createNIRWayURI(long id);
	Resource createOSMWayURI(long id);
 
	Resource createResource(Entity entity);
	
	String getOntologyNS();

	Resource getHasNodesResource(Long wayId);
	
	Resource wayToWayNode(Resource res);
	Resource wayNodeToWay(Resource res);
}
