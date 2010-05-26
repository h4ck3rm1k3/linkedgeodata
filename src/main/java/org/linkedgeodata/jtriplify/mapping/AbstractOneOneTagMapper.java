package org.linkedgeodata.jtriplify.mapping;

import java.io.Serializable;
import java.net.URI;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Abstract Mapping rule between a single tag pattern and a resource.
 * 
 * 
 * TODO remove method - its always null anyway
 * 
 * @author raven
 *
 */
public class AbstractOneOneTagMapper
	implements IOneOneTagMapper, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String resource;
	private String method;
	private TagPattern tagPattern;

	// Whether the tag pertains to the OSM entity, or the concept that
	// the resource represents
	private boolean describesOSMEntity = false;
	
	protected AbstractOneOneTagMapper(String resource, TagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		this.method = null;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}

	protected AbstractOneOneTagMapper(String resource, String method, TagPattern tagPattern, boolean describesOSMEntity)
	{
		this.resource = resource;
		this.method = method;
		this.tagPattern = tagPattern;
		this.describesOSMEntity = describesOSMEntity;
	}
	
	public boolean describesOSMEntity()
	{
		return describesOSMEntity;
	}

	public String getResource()
	{
		return resource;
	}

	public TagPattern getTagPattern()
	{
		return tagPattern;
	}


	public String getMethod()
	{
		return method;
	}

	protected boolean matches(Tag given)
	{
		return matches(tagPattern, given);
	}
	
	protected boolean matches(TagPattern pattern, Tag given)
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

	@Override
	public Model map(String subject, Tag tag) {
		// TODO Auto-generated method stub
		return null;
	}
}
