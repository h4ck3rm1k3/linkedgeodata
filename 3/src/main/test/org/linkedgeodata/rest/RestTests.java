package org.linkedgeodata.rest;

import java.awt.geom.Point2D;

import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RestTests {

	@Ignore
	@Test
	public void testOntology() {
		
		Point2D.Double a = new Point2D.Double(12.34593062612, 51.33298118419);
		Point2D.Double b = new Point2D.Double(12.404552986346, 51.348557018545);

		// Rectangle2D.Double c = new Rectangle2D.Double(a.x, a.y, b.x - a.x,
		// b.y - a.y);

		String polygon = a.y + "-" + b.y + "," + a.x + "-" + b.y;
		
		
		String url = "http://localhost:9998/api/3/ontology";
		
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

		String polygon = a.y + "-" + b.y + "," + a.x + "-" + b.y;
		
		
		String url = "http://localhost:9998/api/3/intersects/" + polygon;
		
		Model model = ModelFactory.createDefaultModel();
		model.read(url);
		
		model.write(System.out, "N-TRIPLES");
	}
}
