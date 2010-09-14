package org.linkedgeodata.osm.mapping;

import java.util.Collection;
import java.util.Date;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public class CachingTagMapper
	implements ITagMapper
{
	private Date cacheTime = null;
	private InMemoryTagMapper cache;
	private ITagMapper source;
	private long updateInterval = 60000;
	
	
	public CachingTagMapper(ITagMapper source)
	{
		this.source = source;
	}

	public CachingTagMapper(ITagMapper source, long updateInterval)
	{
		this.source = source;
		this.updateInterval = updateInterval;
	}
	
	public ITagMapper getSource()
	{
		return source;
	}
	
	// FIXME: The updates could run in a separate thread
	private void updateCache()
	{
		Date now = new Date();

		if(cacheTime == null || (updateInterval >= 0 && (now.getTime() - cacheTime.getTime() > updateInterval))) {

			// TODO This always loads all all mapping rules into memory
			cache = new InMemoryTagMapper();

			for(IOneOneTagMapper item : source.getAllMappers())
				cache.add(item);
			
			cacheTime = new Date(); 
		}
		
	}
	
	@Override
	public Model map(String subject, Tag tag, Model model)
	{
		updateCache();
		
		return cache.map(subject, tag, model);
	}

	@Override
	public Collection<? extends IOneOneTagMapper> lookup(String k, String v)
	{
		updateCache();
		
		return cache.lookup(k, v);
	}

	@Override
	public Collection<? extends IOneOneTagMapper> getAllMappers()
	{
		updateCache();
		
		return cache.getAllMappers();
	}

}
