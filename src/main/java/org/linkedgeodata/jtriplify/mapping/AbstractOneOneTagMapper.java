package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * Abstract Mapping rule between a single tag pattern and a resource.
 * 
 * 
 * TODO remove method - its always null anyway
 * 
 * @author raven
 *
 */
public abstract class AbstractOneOneTagMapper
	implements IOneOneTagMapper
{
	private URI resource;
	private URI method;
	private Tag tagPattern;

	public AbstractOneOneTagMapper(URI resource, Tag tagPattern)
	{
		this.resource = resource;
		this.method = null;
		this.tagPattern = tagPattern;
	}

	public AbstractOneOneTagMapper(URI resource, URI method, Tag tagPattern)
	{
		this.resource = resource;
		this.method = method;
		this.tagPattern = tagPattern;
	}

	public URI getResource()
	{
		return resource;
	}

	public Tag getTagPattern()
	{
		return tagPattern;
	}


	public URI getMethod()
	{
		return method;
	}


	protected boolean matches(Tag pattern, Tag given)
	{
		boolean matchKey = pattern.getKey() == null
			? true
			: pattern.getKey().equals(given.getKey());

		if(matchKey == false)
			return false;
		
		boolean matchValue = pattern.getValue() == null
			? true :
			pattern.getValue().equals(given.getValue());

		return matchValue;
	}
}
