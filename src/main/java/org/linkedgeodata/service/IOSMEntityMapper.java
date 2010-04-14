package org.linkedgeodata.service;

import java.net.URI;

/*
class OntologyInfo
{
	// The original request
	private String key;
	private String value;
	
	
	private Tag tag;
}
*/


public interface IOSMEntityMapper
{	
	enum OntologyType
	{
		CLASS,
		PROPERTY
	}
	
	OntologyType getOntologyType(String key, String value);
	
	URI resolve(String key, String value);
}
