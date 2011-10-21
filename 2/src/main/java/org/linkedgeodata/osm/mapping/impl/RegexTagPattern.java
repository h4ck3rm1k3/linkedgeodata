package org.linkedgeodata.osm.mapping.impl;

import java.util.regex.Pattern;

import org.linkedgeodata.osm.mapping.ITagPattern;
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
