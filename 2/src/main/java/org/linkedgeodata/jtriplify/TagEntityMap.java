package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * subject to removal - superseeded by IOneOneTagMapper
 * 
 * @author raven
 *
 */
public class TagEntityMap
{
	private URI method;
	private URI resource;
	private Set<Tag> tags;

	public TagEntityMap(URI resource, URI method, Tag tag)
	{
		this.resource = resource;
		this.method = method;
		this.tags = Collections.singleton(tag);
	}
	
	public TagEntityMap(URI resource, URI method, Set<Tag> tags)
	{
		this.resource = resource;
		this.method = method;
		this.tags = tags;
	}

	public URI getResource()
	{
		return resource;
	}

	public Set<Tag> getTags()
	{
		return tags;
	}
	
	public URI getMethod()
	{
		return method;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result
				+ ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagEntityMap other = (TagEntityMap) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		return true;
	}

}