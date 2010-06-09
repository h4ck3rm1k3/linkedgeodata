package org.linkedgeodata.jtriplify.mapping;

import java.io.Serializable;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class TagPattern
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String key;
	private String value;
	
	public TagPattern()
	{
	}
	
	public TagPattern(String key, String value)
	{
		this.key = key;
		this.value = value;
	}
	
	public String getKey()
	{
		return key;
	}

	public String getValue()
	{
		return value;
	}
	
	public void setKey(String key)
	{
		this.key = key;
	}
	
	public boolean matches(Tag given)
	{
		boolean matchKey = key == null
			? true
			: key.equals(given.getKey());

		if(matchKey == false)
			return false;
		
		boolean matchValue = value == null
			? true :
			value.equals(given.getValue());

		return matchValue;
	}
}
