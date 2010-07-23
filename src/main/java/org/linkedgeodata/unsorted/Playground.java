package org.linkedgeodata.unsorted;

import java.io.File;
import java.util.regex.Pattern;

import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.RegexTextTagMapper;
import org.linkedgeodata.util.ModelUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Playground
{
	public static void main(String[] args)
		throws Exception
	{
		InMemoryTagMapper mapper = new InMemoryTagMapper();
		mapper.add(new RegexTextTagMapper("http://rdfslabel.org", Pattern.compile("name:([^:]+)"), false));
		
		mapper.save(new File("/tmp/tmp.txt"));
		
		
		Model model = ModelFactory.createDefaultModel();
		mapper.map("http://s", new Tag("name:en", "hi"), model);
		mapper.map("http://s", new Tag("name:de", "hi"), model);
		
		System.out.println(ModelUtil.toString(model));
	}
}
