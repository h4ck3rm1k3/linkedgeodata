package org.linkedgeodata.rest;

import java.awt.geom.Point2D;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RestTests {

	private static final Logger logger = LoggerFactory.getLogger(RestTests.class);
	
	//@Ignore
	@Test
	public void testOntology() {
		
		Point2D.Double a = new Point2D.Double(12.34593062612, 51.33298118419);
		Point2D.Double b = new Point2D.Double(12.404552986346, 51.348557018545);

		// Rectangle2D.Double c = new Rectangle2D.Double(a.x, a.y, b.x - a.x,
		// b.y - a.y);

		String polygon = a.y + "-" + b.y + "," + a.x + "-" + b.y;
		
		
		String url = "http://localhost:9998/api/3/ontology";
		//String url = "http://test.linkedgeodata.org/sparql";
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");
	}
	
	@Test
	public void testIntersects() {
		
		Point2D.Double a = new Point2D.Double(12.34593062612, 51.33298118419);
		Point2D.Double b = new Point2D.Double(12.404552986346, 51.348557018545);

		// Rectangle2D.Double c = new Rectangle2D.Double(a.x, a.y, b.x - a.x,
		// b.y - a.y);

		String polygon = a.y + "-" + b.y + "," + a.x + "-" + b.x;
		
		
		String url = "http://localhost:9998/api/3/intersects/" + polygon;
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");
	}

	@Ignore
	@Test
	public void testGeocode() {
		
		
		String url = "http://localhost:9998/api/3/geocode?q=Leipzig";
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");
	}

	
	@Test
	public void testRecent() {
		String url = "http://localhost:9998/api/3/recent";
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");		
	}
	

	@Test
	public void testIntersectsClass() {
		
		Random r = new Random(System.currentTimeMillis());
		
		for(int i = 0; i < 1; ++i) {
			float e = r.nextFloat();
			
			intersectsClass(-20 + i * 10 + e, 30 + i * 10 + e, 0.01f ,0.01f);
		}
	}
	
	public void intersectsClass(float x, float y, float width, float height) {
		
		/*
		Point2D.Double a = new Point2D.Double(12.34593062612, 51.33298118419);
		Point2D.Double b = new Point2D.Double(12.404552986346, 51.348557018545);
		*/
		Point2D.Double a = new Point2D.Double(x, y);
		Point2D.Double b = new Point2D.Double(x + width, y + height);

		// Rectangle2D.Double c = new Rectangle2D.Double(a.x, a.y, b.x - a.x,
		// b.y - a.y);

		String polygon = a.y + "-" + b.y + "," + a.x + "-" + b.x;
		
		
		String url = "http://localhost:9998/api/3/intersects/" + polygon + "/Bakery";
		logger.debug("Requesting: " + url);
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");
	}

}
