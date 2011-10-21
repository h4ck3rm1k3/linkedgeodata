package org.linkedgeodata.osm.osmosis.plugins;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections15.MultiMap;
import org.jboss.cache.util.SetMultiHashMap;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class LgdRdfUtils
{
	private static final Logger logger =
		LoggerFactory.getLogger(LgdRdfUtils.class);
	

	public static Point2D tryParseGeoRSSPointValue(String value)
	{
		try {
			String str = value.toString();
			String[] ps = str.split("\\s+", 2);

			double lat = Double.parseDouble(ps[0]);
			double lon = Double.parseDouble(ps[1]);

			return new Point2D.Double(lon, lat);
		} catch (Exception e1) {
			return null;
		}
	}


	static Pattern rdfSeqPattern = Pattern.compile(RDF.getURI() + "_(\\d+)$");

	public static Integer tryParseSeqPredicate(Resource res)
	{
		String pred = res.toString();
		Matcher m = rdfSeqPattern.matcher(pred);
		if (m.find()) {
			String indexStr = m.group(1);
			Integer index = Integer.parseInt(indexStr);

			return index;
		}

		return null;
	}
	
	// FIXME: Clearify semantics: If a resource does not have a seq associated
	// should it appear in the result?
	// Currently the answer is "no" (therefore only resources with seqs are
	// returned.
	// However, actually all resources that are "a rdf:Seq" should be in the map
	// (i guess)
	public static Map<Resource, TreeMap<Integer, RDFNode>> indexRdfMemberships(Model model)
	{
		Map<Resource, TreeMap<Integer, RDFNode>> result = new HashMap<Resource, TreeMap<Integer, RDFNode>>();

		processSeq(result, model);

		return result;
	}

	public Set<Resource> extractNodes(
			Map<Resource, TreeMap<Integer, RDFNode>> map)
	{
		Set<Resource> result = new HashSet<Resource>();
		for (TreeMap<Integer, RDFNode> tmpB : map.values()) {
			for (RDFNode tmpC : tmpB.values()) {
				result.add((Resource) tmpC);
			}
		}

		return result;
	}
	
	public static void processSeq(Map<Resource, TreeMap<Integer, RDFNode>> result,
			Model model)
	{
		// Map<Resource, SortedMap<Integer, RDFNode>> result = new
		// HashMap<Resource, SortedMap<Integer, RDFNode>>();

		for (Resource subject : model.listSubjects().toSet()) {
			TreeMap<Integer, RDFNode> part = processSeq(subject, model);
			if (!part.isEmpty())
				result.put(subject, part);
		}

		// return result;
	}
	
	public static TreeMap<Integer, RDFNode> processSeq(Resource res, Model model)
	{
		TreeMap<Integer, RDFNode> indexToObject = new TreeMap<Integer, RDFNode>();
		StmtIterator it = model.listStatements(res, null, (RDFNode) null);
		try {
			while (it.hasNext()) {
				Statement stmt = it.next();

				String pred = stmt.getPredicate().toString();
				Matcher m = rdfSeqPattern.matcher(pred);
				if (m.find()) {
					String indexStr = m.group(1);
					int index = Integer.parseInt(indexStr);

					indexToObject.put(index, stmt.getObject());
				}
			}

		} finally {
			it.close();
		}

		return indexToObject;
	}

	
	public static ArrayList<String> tryParseGeoRRSPointList(String value)
	{
		ArrayList<String> result = new ArrayList<String>();

		String parts[] = value.split("\\s+");
		for (int i = 0; i < (parts.length / 2); ++i) {
			result.add(parts[2 * i] + " " + parts[2 * i + 1]);
		}

		return result;
	}

	private static Pattern	pointPattern	= Pattern.compile(
													"POINT\\s+\\(([^)]+)\\)",
													Pattern.CASE_INSENSITIVE);

	private static String tryParsePoint(String value)
	{
		Matcher m = pointPattern.matcher(value);

		return m.find() ? m.group(1) : null;
	}

	public static Point2D tryParseVirtuosoPointValue(String value)
	{
		String raw = tryParsePoint(value);

		String[] parts = raw.split("\\s+");
		if (parts.length != 2)
			return null;

		try {
			double x = Double.parseDouble(parts[0]);
			double y = Double.parseDouble(parts[1]);
			return new Point2D.Double(x, y);
		} catch (Throwable t) {
			return null;
		}
	}
	
	public static void populatePointPosMappingVirtuoso(Model model,
			Map<Resource, String> nodeToPos)
	{
		StmtIterator it = model
				.listStatements(null, GeoRSS.geo, (RDFNode) null);
		try {
			while (it.hasNext()) {
				Statement stmt = it.next();

				String value = stmt.getLiteral().getValue().toString();
				String pointStr = tryParsePoint(value);
				if (pointStr == null)
					continue;

				nodeToPos.put(stmt.getSubject(), pointStr);
			}
		} finally {
			it.close();
		}
	}

	
	public static Map<Resource, Point2D> createNodePosMapFromNodesVirtuoso(Model model)
	{
		Map<Resource, Point2D> result = new HashMap<Resource, Point2D>();

		for (Statement stmt : model.listStatements(null, GeoRSS.geo,
				(RDFNode) null).toList()) {
			String value = stmt.getLiteral().getValue().toString();
			
			Point2D point = tryParseVirtuosoPointValue(value);
			if(point != null) {
				result.put(stmt.getSubject(), point);
			}
		}

		return result;
	}

	
	/*
	public static Map<Resource, String> createNodePosMapFromNodesGeoRSS(Model model)
	{
		Map<Resource, String> result = new HashMap<Resource, String>();

		for (Statement stmt : model.listStatements(null, GeoRSS.point,
				(RDFNode) null).toList()) {
			String value = stmt.getLiteral().getValue().toString();
			result.put(stmt.getSubject(), value);
		}

		return result;
	}
	*/
	public static Map<Resource, Point2D> createNodePosMapFromNodesGeoRSS(Model model)
	{
		Map<Resource, Point2D> result = new HashMap<Resource, Point2D>();

		for (Statement stmt : model.listStatements(null, GeoRSS.point,
				(RDFNode) null).toList()) {
			String value = stmt.getLiteral().getValue().toString();
			
			Point2D point = tryParseGeoRSSPointValue(value);
			
			result.put(stmt.getSubject(), point);
		}

		return result;
	}
	
	
	

	/**
	 * Creates a Node-Pos Map by mapping a georss:node-string to triples of the
	 * corresponding nodes.
	 * 
	 * 
	 */
	public static Map<Resource, Point2D> createNodePosMapFromWays(Model model, ILGDVocab vocab)
	{
		Map<Resource, Point2D> result = new HashMap<Resource, Point2D>();

		Set<Statement> ways = model
				.listStatements(null, GeoRSS.line, (RDFNode) null)
				.andThen(
						model.listStatements(null, GeoRSS.polygon,
								(RDFNode) null)).toSet();

		for (Statement stmt : ways) {
			Resource wayNode = vocab.wayToWayNode(stmt.getSubject());
			SortedMap<Integer, RDFNode> seq = LgdRdfUtils.processSeq(wayNode, model);

			String value = stmt.getObject().as(Literal.class).getValue()
					.toString();
			List<String> positions = LgdRdfUtils.tryParseGeoRRSPointList(value);

			if (positions == null) {
				logger.warn("Error parsing a geoRSS point list from statement "
						+ stmt);
				continue;
			}

			for (Map.Entry<Integer, RDFNode> entry : seq.entrySet()) {
				Resource node = entry.getValue().as(Resource.class);
				int index = entry.getKey() - 1;

				// FIXME This sometime fails for some reason.
				// Most likely the nodelist of a way is out of sync with its
				// polygon
				// Add error checking so its possible to investigate
				if (index >= positions.size()) {
					logger.warn("Out of sync: Georss of " + wayNode + " has "
							+ positions.size() + " coordinates, node " + node
							+ " has index " + index);
					continue;
				}

				String str = positions.get(index);
				Point2D val = tryParseGeoRSSPointValue(str);

				result.put(node, val);
			}
		}

		return result;
	}

	public static Map<Resource, Point2D> createNodePosMapFromEntities(
			Iterable<? extends Entity> entities, ILGDVocab vocab)
	{
		Map<Resource, Point2D> result = new HashMap<Resource, Point2D>();

		for (Entity entity : entities) {
			if (entity instanceof Node) {
				Node node = (Node) entity;

				Resource resource = vocab.createResource(node);
				//String pos = node.getLongitude() + " " + node.getLatitude();
				String pos = node.getLatitude() + " " + node.getLongitude();
				
				Point2D val = tryParseGeoRSSPointValue(pos);
				
				result.put(resource, val);
			}
		}

		return result;
	}

	/**
	 * Returns mappings from ways to nodes and relations to ways and nodes.
	 * 
	 * Note: Each item is mapped to the entities it depends on. For instance,
	 * nodes are mapped to the set of ways they depend on.
	 * 
	 * 
	 * TODO relations not implemented
	 * 
	 * @param model
	 * @return
	 */
	public static MultiMap<Resource, Resource> extractDependencies(Model model, ILGDVocab vocab)
	{
		MultiMap<Resource, Resource> result = new SetMultiHashMap<Resource, Resource>();
		Map<Resource, TreeMap<Integer, RDFNode>> index = LgdRdfUtils.indexRdfMemberships(model);

		for (Map.Entry<Resource, TreeMap<Integer, RDFNode>> entry : index
				.entrySet()) {
			Resource wayNode = entry.getKey();
			Resource way = vocab.wayNodeToWay(wayNode);

			if (way == null) {
				continue;
			}

			for (RDFNode node : entry.getValue().values()) {
				result.put((Resource) node, way);
			}
		}

		return result;
	}

	public static Model createModelFromStatements(Iterable<Statement> statements)
	{
		Model result = ModelFactory.createDefaultModel();
		
		for(Statement stmt : statements) {
			result.add(stmt);
		}
		
		return result;
	}

}
