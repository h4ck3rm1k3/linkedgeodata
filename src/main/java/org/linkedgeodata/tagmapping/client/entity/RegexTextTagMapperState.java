package org.linkedgeodata.tagmapping.client.entity;

import java.io.Serializable;

import javax.persistence.Entity;

import com.google.gwt.user.client.rpc.IsSerializable;


/**
 * The regular expression for the keyPattern must contain exactly one
 * group for matching the language (e.g. name:?{3}).
 * 
 * 
 * 
 * @author raven
 *
 */
@Entity
public class RegexTextTagMapperState
	extends AbstractTagMapperState
	implements Serializable, IsSerializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7573969687628794907L;
	private String keyPattern;
	private String  property;
	private boolean describesOSMEntity;
	
	public RegexTextTagMapperState()
	{
	}
	
	public String getKeyPattern()
	{
		return keyPattern;
	}
	public void setKeyPattern(String keyPattern)
	{
		this.keyPattern = keyPattern;
	}
	public String getProperty()
	{
		return property;
	}
	public void setProperty(String property)
	{
		this.property = property;
	}
	public boolean getDescribesOSMEntity()
	{
		return describesOSMEntity;
	}
	public void setDescribesOSMEntity(boolean describesOSMEntity)
	{
		this.describesOSMEntity = describesOSMEntity;
	}

	@Override
	public <T> T accept(IEntityVisitor<T> visitor)
	{
		return visitor.visit(this);
	}
}
