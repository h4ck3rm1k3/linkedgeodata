package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;

import org.junit.Test;
import org.linkedgeodata.jtriplify.TripleUtil;
import org.linkedgeodata.util.ModelUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.RDF;


public class SimpleClassTagMapperTest
{

	@Test
	public void testRoundTrip()
		throws Exception
	{
		URI classURI = new URI("http://linkedgeodata.org/AirportTower");
		
		SimpleClassTagMapper mapper = new SimpleClassTagMapper(classURI, new Tag("aerial", "tower"));
	
		
		
		Model model = mapper.map(URI.create("http://s.org"), new Tag("aerial", "tower"));
		System.out.println(ModelUtil.toString(model, "N3"));

		model = mapper.map(URI.create("http://s.org"), new Tag("aerial", "airport"));
		System.out.println(ModelUtil.toString(model, "N3"));

		
		Triple t = TripleUtil.auto(URI.create("http://s.org"), RDF.type, URI.create("http://linkedgeodata.org/AirportTower"));
		
		//System.out.println(new Tag("aerial", "airport"));
		Tag tag = mapper.reverseMap(t);
		
		System.out.println(tag);
	}
	
	
	@Test
	public void testRoundTrip2()
		throws Exception
	{
		URI classURI = new URI("http://linkedgeodata.org/arial");
		
		SimpleClassTagMapper mapper = new SimpleClassTagMapper(classURI, new Tag("aerial", null));
	
		
		
		Model model = mapper.map(URI.create("http://s.org"), new Tag("aerial", "tower"));
		System.out.println(ModelUtil.toString(model, "N3"));

		model = mapper.map(URI.create("http://s.org"), new Tag("aerial", "airport"));
		System.out.println(ModelUtil.toString(model, "N3"));

		
		Triple t = TripleUtil.auto(URI.create("http://s.org"), RDF.type, URI.create("http://linkedgeodata.org/AirportTower"));
		
		//System.out.println(new Tag("aerial", "airport"));
		Tag tag = mapper.reverseMap(t);
		
		System.out.println(tag);
	}


}
