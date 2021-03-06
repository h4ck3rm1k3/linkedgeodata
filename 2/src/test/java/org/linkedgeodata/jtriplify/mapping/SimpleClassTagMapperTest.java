package org.linkedgeodata.jtriplify.mapping;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.impl.ISimpleOneOneTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleDataTypeTagMapper;
import org.linkedgeodata.osm.mapping.impl.SimpleTagPattern;
import org.linkedgeodata.util.ModelUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.XSD;


public class SimpleClassTagMapperTest
{
	private static final Logger logger = Logger.getLogger(SimpleClassTagMapperTest.class);


	@Before
	public void init()
	{
		PropertyConfigurator.configure("log4j.properties");
	}
	
	/*
	@Test
	public void testRoundTrip()
		throws Exception
	{
		String classURI = "http://linkedgeodata.org/AirportTower";

		SimpleClassTagMapper mapper = new SimpleClassTagMapper(classURI, new SimpleTagPattern("aerial", "tower"), false);
	
		
		
		Model model = mapper.map("http://s.org", new Tag("aerial", "tower"), null);
		System.out.println(ModelUtil.toString(model, "N3"));

		model = mapper.map("http://s.org", new Tag("aerial", "airport"), null);
		System.out.println(ModelUtil.toString(model, "N3"));

		
		Triple t = TripleUtil.auto(URI.create("http://s.org"), RDF.type, URI.create("http://linkedgeodata.org/AirportTower"));
		
		//System.out.println(new Tag("aerial", "airport"));
		//Tag tag = mapper.reverseMap(t);
		
		//System.out.println(tag);
	}
	*/
	
	/*
	@Test
	public void testRoundTrip2()
		throws Exception
	{
		String classURI = "http://linkedgeodata.org/arial";
		
		SimpleClassTagMapper mapper = new SimpleClassTagMapper(classURI, new SimpleTagPattern("aerial", null), false);
	
		
		Model model = mapper.map("http://s.org", new Tag("aerial", "tower"), null);
		System.out.println(ModelUtil.toString(model, "N3"));

		model = mapper.map("http://s.org", new Tag("aerial", "airport"), null);
		System.out.println(ModelUtil.toString(model, "N3"));

		
		Triple t = TripleUtil.auto(URI.create("http://s.org"), RDF.type, URI.create("http://linkedgeodata.org/AirportTower"));
		
		//System.out.println(new Tag("aerial", "airport"));
		//Tag tag = mapper.reverseMap(t);
		
		//System.out.println(tag);
	}
	*/
	
	/*
	@Test
	public void saveLoadRoundTripIndividual()
		throws Exception
	{
		logger.info("saveLoadRoundTriple");

		String classURI = "http://linkedgeodata.org/arial";
		
		Object obj = new SimpleClassTagMapper(classURI, new SimpleTagPattern("aerial", null), false);

		SerializationUtil.serializeXML(obj, new File("/tmp/Mapper.xml"));
		Object x = SerializationUtil.deserializeXML(new File("/tmp/Mapper.xml"));
		
		//Assert.assertEquals(obj, x);		
	}*/

	@Test
	public void saveLoadRoundTripCollection()
		throws Exception
	{
		logger.info("saveLoadRoundTriple2");

		String classURI = "http://c";
		
		//IOneOneTagMapper obj = new SimpleClassTagMapper(classURI, new TagPattern("a", "b"), false);
		//TypeMapper typeMapper = new TypeMapper();
		//RDFDatatype dt = typeMapper.getSafeTypeByName();
		
		ISimpleOneOneTagMapper obj = new SimpleDataTypeTagMapper(classURI, new SimpleTagPattern("a", "b"), XSD.xint.toString(), false);

		InMemoryTagMapper tm = new InMemoryTagMapper();
		tm.index(obj);
		
		tm.save(new File("/tmp/TagMapping.xml"));
		
		InMemoryTagMapper tm2 = new InMemoryTagMapper();
		tm2.load(new File("/tmp/TagMapping.xml"));
		
		Model m = tm.map("www.x.org", new Tag("a", "b"), null);		
		System.out.println(ModelUtil.toString(m));

		
		m = tm2.map("www.x.org", new Tag("a", "b"), null);		
		System.out.println(ModelUtil.toString(m));
	}

}
