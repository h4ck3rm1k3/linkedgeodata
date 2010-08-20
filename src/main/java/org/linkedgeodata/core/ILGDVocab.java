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
	String createNIRNodeURI(long id);
	String createOSMNodeURI(long id);	
	String createNIRWayURI(long id);
	String createOSMWayURI(long id);
 
	String createResource(Entity entity);
	
	String getOntologyNS();

	Resource getHasNodesResource(Long wayId);
}
