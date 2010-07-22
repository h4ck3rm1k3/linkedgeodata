/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */ 
package org.linkedgeodata.osm.mapping;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.util.SerializationUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TagMapper
//	implements ITagMapper
{
	// k -> v -> property
	private Map<String, Map<String, Set<ISimpleOneOneTagMapper>>> kvp = new HashMap<String, Map<String, Set<ISimpleOneOneTagMapper>>>();
	
	private List<IOneOneTagMapper> complexMappers = new ArrayList<IOneOneTagMapper>();
	
	public TagMapper()
	{
	}
	
	@SuppressWarnings("unchecked")
	public void load(File file)
		throws Exception
	{
		List<IOneOneTagMapper> list = (List<IOneOneTagMapper>)
			SerializationUtil.deserializeXML(file);

		for(IOneOneTagMapper item : list) {
			add(item);
		}
	}
	
	public void save(File file)
		throws IOException, JAXBException
	{
		List<IOneOneTagMapper> list = asList();
		
		SerializationUtil.serializeXML(list, file);
	}
	
	public List<IOneOneTagMapper> asList()
	{
		List<IOneOneTagMapper> list = new ArrayList<IOneOneTagMapper>();
		
		for(Map<String, Set<ISimpleOneOneTagMapper>> a : kvp.values()) {
			for(Set<ISimpleOneOneTagMapper> b : a.values()) {
				list.addAll(b);
			}
		}
		
		list.addAll(complexMappers);
		
		return list;
	}
	
	public void add(IOneOneTagMapper item) {
		if(item instanceof ISimpleOneOneTagMapper)
			index((ISimpleOneOneTagMapper)item);
		else
			complexMappers.add(item);
	}
	
	public  void index(ISimpleOneOneTagMapper tagMapper)
	{
		String k = tagMapper.getTagPattern().getKey();
		String v = tagMapper.getTagPattern().getValue();
		
		Map<String, Set<ISimpleOneOneTagMapper>> m = kvp.get(k);
		if(m == null) {
			m = new HashMap<String, Set<ISimpleOneOneTagMapper>>();
			kvp.put(k, m);
		}
		
		Set<ISimpleOneOneTagMapper> ps = m.get(v);
		if(ps == null) {
			ps = new HashSet<ISimpleOneOneTagMapper>();
			m.put(v, ps);
		}
		
		ps.add(tagMapper);
	}

	public Set<ISimpleOneOneTagMapper> lookup(String k, String v)
	{
		// check if a mapping for k exits
		for(String kk : new String[]{k, null}) {
			Map<String, Set<ISimpleOneOneTagMapper>> x = kvp.get(kk);
		
			if(x == null)
				continue;
			
			for(String vv : new String[]{v, null})
			{
				Set<ISimpleOneOneTagMapper> y = x.get(vv);
				
				if(y == null)
					continue;
								
				return y;
			}		
		}
		
		return null;
	}
	
	public Model map(String subject, Tag tag, Model model)
	{
		List<IOneOneTagMapper> candidates = new ArrayList<IOneOneTagMapper>();

		// Check if any of the complex mappers matches - as they have precedence
		for(IOneOneTagMapper item : complexMappers) {
			if(item.matches(tag))
				candidates.add(item);
		}
		
		// If no match was found, use the simple mappers
		if(candidates.isEmpty()) {
			Set<ISimpleOneOneTagMapper> x = lookup(tag.getKey(), tag.getValue());
		
			if(x != null)
				candidates.addAll(x);
		}

		//candidates.addAll(complexMappers);
		
		//if(candidates == null)
		//	return null;
		
		// Only set the result to non-null if there was at least some partial result
		Model result = null;
		for(IOneOneTagMapper mapper : candidates) {
			Model tmp = mapper.map(subject, tag, model);
			
			if(tmp != null) {
				result = tmp;
				
				if(model == null)
					model = result;
				/*
				if(result == null) {
					if(model == null) {
					result = ModelFactory.createDefaultModel();
				}

				if(model == null)
				result.add(tmp);
				*/
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