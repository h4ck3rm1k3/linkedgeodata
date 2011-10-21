package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import org.linkedgeodata.core.ILGDVocab;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

abstract class AbstractNodeMapper
		implements INodeMapper
{
	private ILGDVocab	vocab;

	private Property	property;

	// private RDFDatatype rdfDataType =
	// TypeMapper.getInstance().getSafeTypeByName(");

	public AbstractNodeMapper(ILGDVocab vocab, Property property)
	{
		this.vocab = vocab;
		this.property = property;
	}

	protected abstract RDFNode toRDFLiteral(Point2D point);

	protected abstract Point2D toPoint(RDFNode node);

	@Override
	public void transform(Map<Long, Point2D> points, Model out)
	{
		for (Map.Entry<Long, Point2D> entry : points.entrySet()) {
			Long id = entry.getKey();
			Point2D point = entry.getValue();

			out.add(vocab.createNIRNodeURI(id), property, toRDFLiteral(point));
		}
	}

	@Override
	public Map<Long, Point2D> extract(Model model)
	{
		Map<Long, Point2D> result = new HashMap<Long, Point2D>();

		for (Statement stmt : model.listStatements(null, property,
				(RDFNode) null).toList()) {
			Long id = RDFNodePositionDAO.resourceToId(stmt.getSubject());

			Point2D point = toPoint(stmt.getObject());

			if (id == null || point == null)
				continue;

			result.put(id, point);
		}

		return result;
	}
}