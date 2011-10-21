package org.linkedgeodata.osm.osmosis.plugins;

import java.awt.geom.Point2D;
import java.util.Map;

import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class VirtuosoOseNodeSerializer
		implements INodeSerializer
{
	@Override
	public void write(Model model, Resource subject, Point2D node)
	{
		SimpleNodeToRDFTransformer.generateGeoRSS(model, subject, node);
	}

	@Override
	public Map<Resource, Point2D> createNodePosMap(Model model)
	{
		return LgdRdfUtils.createNodePosMapFromNodesGeoRSS(model);
	}
}