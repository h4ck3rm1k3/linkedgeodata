package org.linkedgeodata.core;

public interface ILGDVocab
{
	String GetBaseNS();
	String getResourceNS();
	//String getNode
	
	String getHasNodesPred();
	String getMemberOfWayPred();
	
		

	// NIR = Non-Information-Resource
	String createNIRNodeURI(long id);
	String createOSMNodeURI(long id);	
	String createNIRWayURI(long id);
	String createOSMWayURI(long id);
}
