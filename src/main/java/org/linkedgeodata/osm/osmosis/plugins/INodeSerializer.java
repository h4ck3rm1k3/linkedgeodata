package org.linkedgeodata.osm.osmosis.plugins;

import java.awt.geom.Point2D;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


/**
 * Currently we need the following operations:
 * .) Write a resource with its position to a model
 * .) Read that data from a model
 * 
 * @author raven
 *
 */
public interface INodeSerializer
{
	void write(Model model, Resource subject, Point2D point);
	//Map<Resource, Point2D> read(IGraph graph, Set<Resource> subjects);
	Map<Resource, Point2D> createNodePosMap(Model model);
}
