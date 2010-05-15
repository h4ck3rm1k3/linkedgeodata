package org.linkedgeodata.jtriplify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.linkedgeodata.jtriplify.mapping.IOneOneTagMapper;
import org.linkedgeodata.jtriplify.mapping.TagMapperFactory;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TagMapper
{
	// k -> v -> property
	private Map<String, Map<String, Set<IOneOneTagMapper>>> kvp = new HashMap<String, Map<String, Set<IOneOneTagMapper>>>();
	
	public TagMapper()
	{
	}
	
	public void load(File file)
		throws Exception
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		
		TagMapperFactory factory = new TagMapperFactory();
		
		String line = null;
		while((line = reader.readLine()) != null) {
			IOneOneTagMapper mapper = factory.createInstance(line);
		
			index(mapper);
		}
	}
	
	public  void index(IOneOneTagMapper tagMapper)
	{
		String k = tagMapper.getTagPattern().getKey();
		String v = tagMapper.getTagPattern().getValue();
		
		Map<String, Set<IOneOneTagMapper>> m = kvp.get(k);
		if(m == null) {
			m = new HashMap<String, Set<IOneOneTagMapper>>();
			kvp.put(k, m);
		}
		
		Set<IOneOneTagMapper> ps = m.get(v);
		if(ps == null) {
			ps = new HashSet<IOneOneTagMapper>();
			m.put(v, ps);
		}
		
		ps.add(tagMapper);
	}

	private Set<IOneOneTagMapper> get(String k, String v)
	{
		// check if a mapping for k exits
		for(String kk : new String[]{k, null}) {
			Map<String, Set<IOneOneTagMapper>> x = kvp.get(kk);
		
			if(x == null)
				continue;
			
			for(String vv : new String[]{v, null})
			{
				Set<IOneOneTagMapper> y = x.get(vv);
				
				if(y == null)
					continue;
								
				return y;
			}		
		}
		
		return null;
	}
	
	public Model map(URI subject, Tag tag)
	{
		Set<IOneOneTagMapper> candidates = get(tag.getKey(), tag.getValue());
		if(candidates == null)
			return null;
		
		Model result = null; 
		for(IOneOneTagMapper mapper : candidates) {
			Model tmp = mapper.map(subject, tag);
			
			if(tmp != null) {
				// Only set the result to non-null if there was at least some partial result
				if(result == null)
					result = ModelFactory.createDefaultModel();

				result.add(tmp);
			}
		}
		
		return result;
	}
	
	/*
	public Set<TagEntityMap> get(Set<Tag> tags)
	{
		Set<TagEntityMap> result = new HashSet<TagEntityMap>();
		for(Tag tag : tags) {
			Set<TagEntityMap> candidates = get(tag.getKey(), tag.getValue());
			
			result.addAll(candidates);
		}
	
		return result;
	}*/
}