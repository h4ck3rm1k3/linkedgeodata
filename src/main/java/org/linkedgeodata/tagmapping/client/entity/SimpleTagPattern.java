package org.linkedgeodata.tagmapping.client.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.hibernate.annotations.Index;

import com.google.gwt.user.client.rpc.IsSerializable;

//import org.linkedgeodata.jtriplify.mapping.ITagPattern;

//@Entity
@Embeddable
public class SimpleTagPattern
	implements Serializable, IsSerializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Index(name = "kv_index", columnNames={"key", "value"})
	private String key;
	private String value;
	
	public SimpleTagPattern()
	{
	}
	
	public SimpleTagPattern(String key, String value)
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

	public void setValue(String value)
	{
		this.value = value;
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
