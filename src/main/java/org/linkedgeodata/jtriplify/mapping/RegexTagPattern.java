package org.linkedgeodata.jtriplify.mapping;

import java.util.regex.Pattern;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class RegexTagPattern
	implements ITagPattern
{
	private Pattern keyPattern;
	private Pattern valuePattern;
	
		
	@Override
	public boolean matches(Tag given)
	{
		//keyPattern.compile(regex)
		return false;
	}
}
