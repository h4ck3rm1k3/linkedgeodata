

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleTagPattern;
import org.linkedgeodata.jtriplify.mapping.simple.SimpleTextTagMapper;
import org.linkedgeodata.util.ModelUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;


public class SimpleTextTagMapperTest
{
	private static final Logger logger = Logger.getLogger(SimpleTextTagMapperTest.class);

	@Before
	public void init()
	{
		PropertyConfigurator.configure("log4j.properties");
	}

	@Test
	@Ignore
	public void testGeoRSSLineMapping()
		throws Exception
	{
		SimpleTextTagMapper m = new SimpleTextTagMapper(
				"http://www.georss.org/georss/line",
				new SimpleTagPattern("@@geoRSSLine", null),
				null,
				false);
		
		Tag tag = new Tag("@@geoRSSLine", "0 1 2 3 4 5");
		
		Model model = m.map("http://ex.org", tag, null);
	
		logger.trace(ModelUtil.toString(model));
	}

	@Test
	@Ignore
	public void testGeoRSSPolygonMapping()
		throws Exception
	{
		SimpleTextTagMapper m = new SimpleTextTagMapper(
				"http://www.georss.org/georss/polygon",
				new SimpleTagPattern("@@geoRSSPolygon", null),
				null,
				false);
		
		Tag tag = new Tag("@@geoRSSPolygon", "0 1 2 3 4 5");
		
		Model model = m.map("http://ex.org", tag, null);
	
		logger.trace(ModelUtil.toString(model));
	}
	
	
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		SimpleTextTagMapperTest test = new SimpleTextTagMapperTest();
		test.testGeoRSSLineMapping();
		test.testGeoRSSPolygonMapping();
	}
}
