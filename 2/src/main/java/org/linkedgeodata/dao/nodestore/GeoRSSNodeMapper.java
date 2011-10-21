package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.osm.osmosis.plugins.LgdRdfUtils;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class GeoRSSNodeMapper
		extends AbstractNodeMapper
{
	public GeoRSSNodeMapper(ILGDVocab vocab)
	{
		super(vocab, GeoRSS.point);
	}

	protected RDFNode toRDFLiteral(Point2D point)
	{
		return ResourceFactory.createPlainLiteral(point.getX() + " "
				+ point.getY());
	}

	protected Point2D toPoint(RDFNode node)
	{
		
		return LgdRdfUtils
				.tryParseGeoRSSPointValue(node.asNode().getLiteralLexicalForm());
	}

}
