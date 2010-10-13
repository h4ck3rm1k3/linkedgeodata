package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;
import org.linkedgeodata.osm.osmosis.plugins.IgnoreModifyDeleteDiffUpdateStrategy;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class VirtuosoNodeMapper
		extends AbstractNodeMapper
{
	public VirtuosoNodeMapper(ILGDVocab vocab)
	{
		super(vocab, GeoRSS.geo);
	}

	protected RDFNode toRDFLiteral(Point2D point)
	{
		return SimpleNodeToRDFTransformer.generateVirtuosoLiteral(point);
	}

	protected Point2D toPoint(RDFNode node)
	{
		return IgnoreModifyDeleteDiffUpdateStrategy
				.tryParseVirtuosoPointValue(node.asNode()
						.getLiteralLexicalForm());
	}
}
