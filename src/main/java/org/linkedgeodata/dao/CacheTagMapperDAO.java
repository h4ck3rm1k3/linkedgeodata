package org.linkedgeodata.dao;

import java.util.Collection;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.ITagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public class CacheTagMapperDAO
	implements ITagMapper
{
	@Override
	public Model map(String subject, Tag tag, Model model)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<? extends IOneOneTagMapper> lookup(String k, String v)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<? extends IOneOneTagMapper> getAllMappers()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}
