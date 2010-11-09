/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.osm.osmosis.plugins;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.commons.jena.ModelSetView;
import org.aksw.commons.util.collections.TransformCollection;
import org.aksw.commons.util.collections.TransformIterable;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.core.vocab.WGS84Pos;
import org.linkedgeodata.dao.nodestore.RDFNodePositionDAO;
import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.sort.v0_6.EntityByTypeThenIdComparator;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;


/**
 * TODO Move elsewhere Notes on the nodegraph:
 * 
 * Tagless nodes are put into the NodeGraph, all other nodes go into the
 * MainGraph.
 * 
 * Whenever a node is removed, if a lookup on the main graph reveals no data, a
 * lookup on the node graph must be made.
 * 
 * This lookup also returns triples, however, the problem is how to create the
 * diff.
 * 
 * 
 * So: How to work with the node-side graph:
 * 
 * .) Whenever a new tagless node is *created*, add it to the nodeGraph. ok.
 * 
 * .) Whenever a node is *modified* or *deleted*, what happens is, that all data
 * from the main graph is fetched.
 * 
 * If that lookup reveals no data, we can either conclude that the node doesn't
 * exist in the store, or that it exists in the side graph. As we are not
 * publishing diffs from the side graph, we could simply state that such nodes
 * are deleted from the side graph, without checking whether they actually exist
 * there.
 * 
 * 
 * 
 * .) Whenever the nodes of a *wayNode are modified*, the formerly referenced
 * nodes may have become orphaned. If that is the case, add them to:
 * mainGraph.removed & nodeDiff.added
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author raven
 * 
 */

class NodeWayCache
{
	private SetMultiHashMap<Resource, Resource>	nodeToWays	= new SetMultiHashMap<Resource, Resource>();
	private MultiHashMap<Resource, Resource>	wayToNodes	= new MultiHashMap<Resource, Resource>();
	private Set<Resource>						negCache;

	/*
	 * public Collection<Resource> lookupWaysByNode(Iterable<Resource> nodes) {
	 * for(Resource node : nodes) {
	 * 
	 * } }
	 */
}

class LGDCache
{
	enum State
	{
		Complete, // Every statement about a resource is in the cache
		Partial, // Not all statements about a resoruce are in the cache
		None, // The resource doesn't exist
		Unknown
		// Cache miss
	}

	private Model					posCache		= ModelFactory
															.createDefaultModel();
	private LRUMap<Resource, State>	resourceToState	= new LRUMap<Resource, State>();

	private Set<Resource>			negCache;
	private String					graphName;
	private ISparulExecutor			graphDAO;

	// private LRUMap<Long, Point2D> nodeToPosition = new LRUMap<Long,
	// Point2D>();

	// private LinkedList<Resource> lru = new LinkedList<Resource>();

	private int						maxSize			= 1000000;

	public LGDCache()
	{
	}

	public void insert(Model model)
	{
		while (Math.max(0, model.size() - (maxSize - posCache.size())) > 0) {

		}

		// if(cache.size() + model.size() > maxSize) {

		// }
		StmtIterator it = model.listStatements();
		try {
			while (it.hasNext()) {

			}
		} finally {
			it.close();
		}
	}

	public void remove(Model model)
	{
	}

	public String createSubjectQuery(Iterable<Resource> subjects)
	{
		// String resources = "<" + StringUtil.implode(">,<", uris) + ">";
		String resources = StringUtil.implode(",", subjects);

		String fromPart = (graphName != null) ? "From <" + graphName + "> "
				: "";

		String result = "Construct { ?s ?p ?o . } " + fromPart
				+ "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";

		// return Collections.singletonList(result);
		return result;
	}

	public void lookup(Model out, Iterable<Resource> subjects)
	{
		List<Resource> cacheMisses = coreLookup(out, subjects);

		String query = createSubjectQuery(cacheMisses);
		// Model model = graphDAO.executeConstruct(query);

		// model.listSubjects().toSet()

		for (Resource miss : cacheMisses) {

		}

	}

	public List<Resource> coreLookup(Model out, Iterable<Resource> subjects)
	{
		List<Resource> cacheMisses = new ArrayList<Resource>();

		for (Resource subject : subjects) {
			if (negCache.contains(subject))
				continue;

			StmtIterator it = posCache.listStatements(subject, null,
					(RDFNode) null);

			if (!it.hasNext()) {
				cacheMisses.add(subject);
			} else {
				while (it.hasNext()) {
					out.add(it.next());
				}
			}
		}

		return cacheMisses;
	}
}

/**
 * Some notes on the update process:
 * 
 * Entities are being passed to this Change sink. Entities then have their tags
 * filtered, and are then themselves classified as either "accept", "reject", or
 * "position accept".
 * 
 * 
 * 
 * 
 * Each entity is transformed to RDF, however not all RDF data can be generated
 * at once.
 * 
 * 
 * 
 * 
 * For each entity, all data from the main graph is collected.
 * 
 * 
 * 
 * 
 * In the last steps
 * 
 * 
 * 
 * 
 * 
 * 
 * @author raven
 * 
 * @param <T>
 */

/*
 * class ChunkIterator<T> implements Iterator<Collection<T>> { private
 * Collection<T> source; private int batchSize;
 * 
 * public CollectionChunker(Collection<T> source, int batchSize) { this.source =
 * source; this.batchSize = batchSize; }
 * 
 * @Override public boolean hasNext() { // TODO Auto-generated method stub
 * return false; }
 * 
 * @Override public Collection<T> next() { // TODO Auto-generated method stub
 * return null; }
 * 
 * @Override public void remove() { throw new UnsupportedOperationException(); }
 * }
 * 
 * 
 * class CollectionChunker<T> extends AbstractCollection<Collection<T>> {
 * private Collection<T> source; private int batchSize;
 * 
 * public CollectionChunker(Collection<T> source, int batchSize) { this.source =
 * source; this.batchSize = batchSize; }
 * 
 * @Override public Iterator<Collection<T>> iterator() { // TODO Auto-generated
 * method stub return null; }
 * 
 * @Override public int size() { return source.size() / batchSize; } }
 */





/**
 * FIXME Implement a class like this in a revised version of the LiveSync. For
 * now I leave it here as a stub in order that it reminds me of my intentions.
 * 
 * Eventually, this class should hold all information that is being gathered in
 * the LiveSync step.
 * 
 * The information so for is: . Created, Modified and Delted Nodes (as Entities)
 * . Created, Modified and Delted Ways (as Entities) . The two above as
 * resources, addtionally to ways also the corresponding way nodes
 * 
 * . The new main model . The old main model . The diff between the two (maybe
 * this can be implemented as a cached view (observer pattern), so no explicit
 * materializiation would be required?!?!)
 * 
 * . NodeResource - Position Mapping . WayNode to Node Mapping . Node to Way
 * Mapping (in order to determine (un)referenced nodes) . Dangling node
 * (candidates)
 * 
 * . Helper methods that e.g. compute sets of resources based on the state of
 * this object
 * 
 * @author raven
 * 
 */
class LiveUpdateContext
{
	public Set<Node>	createdNodes;
	public Set<Node>	modifiedNodes;
	public Set<Node>	removedNodes;

	public Set<Way>		createdWays;
	public Set<Way>		modifiedWays;
	public Set<Way>		removedWays;

	public Model		newModel;
	public Model		oldModel;

}





/**
 * Update Strategy which ignores the information of deleted tags - and in
 * consequence triples - from modified elements. Therefore this strategy always
 * performs a diff.
 * 
 * @author raven
 * 
 *         FIXME Somehow separate the store update code from the timely diff
 *         code
 * 
 */
public class IgnoreModifyDeleteDiffUpdateStrategy
		implements IUpdateStrategy
{
	private static final Logger			logger					= Logger.getLogger(IUpdateStrategy.class);

	private ILGDVocab					vocab;
	private ITransformer<Entity, Model>	entityTransformer;
	private ISparqlExecutor				graphDAO;
	private String						mainGraphName;
	// private String nodeGraphName;

	private RDFNodePositionDAO			nodePositionDAO;

	private ITransformer<Model, Model>	postProcessTransformer	= new VirtuosoStatementNormalizer();

	// This diff is a view with underlying models
	private RDFDiff						mainGraphDiff;

	private TreeSetDiff<Node> nodeDiff = new TreeSetDiff<Node>();

	
	// private Predicate<Entity> entityFilter;
	// private Predicate<Tag> tagFilter;

	// private LRUMap<Long, Point2D> nodeToPosition = new LRUMap<Long,
	// Point2D>();
	// private LRUMap<Long, Long> nodeToWay = new LRUMap<Long, Long>();

	// The nodes which are removed/inserted into the NodeStore
	// This graph only contains triples of the form <node> geo:geometry
	// "value"^^virtrdf:geo
	// private RDFDiff						nodeGraphDiff;
	// private IDiff<Map<Long, Point2D>> nodeDiff = new Diff<Map<Long,
	// Point2D>>(new HashMap<Long, Point2D>(), new HashMap<Long, Point2D>(),
	// null);

	// Unfortunately we need to make the entityFilter a property of this class.
	// As nodes that will be filtered out need to be treated in a special way:
	// Whenever nodes are filtered out, they are written to a separate graph
	// (or table - doesn't matter). This way it is always possible to retrieve
	// the positions of nodes. This is necessairy for computing the polygons
	// of ways.
	// private Predicate<Tag> tagFilter;
	// private Predicate<Entity> entityFilter;

	// private Set<Entity> entities = new HashSet<Entity>();

	// SetDiff<EntityContainer> entityDiff = new SetDiff<EntityContainer>(new
	// EntityByTypeThenIdComparator());

	// Map<ChangeAction, Set<EntityContainer>> entityDiff = new
	// HashMap<ChangeAction, Set<EntityContainer>>();

	// /*
	Set<EntityContainer>				createdEntities			= new TreeSet<EntityContainer>(
																		new EntityByTypeThenIdComparator());
	Set<EntityContainer>				deletedEntities			= new TreeSet<EntityContainer>(
																		new EntityByTypeThenIdComparator());
	Set<EntityContainer>				modifiedEntities		= new TreeSet<EntityContainer>(
																		new EntityByTypeThenIdComparator());

	public void clear()
	{
		createdEntities.clear();
		modifiedEntities.clear();
		deletedEntities.clear();
	}

	public void addEntity(ChangeAction ca, EntityContainer ec)
	{
		// If an entity became deleted but the same becomes recreated (shouldn't
		// happen?)
		// it must be treated as modified - on other words:
		// Subsequent recreates must be treated as modified
		createdEntities.remove(ec);

		boolean didRemove = modifiedEntities.remove(ec)
				|| deletedEntities.remove(ec);

		switch (ca) {
		case Create:
			if (didRemove)
				modifiedEntities.add(ec);
			else
				createdEntities.add(ec);
			break;
		case Modify:
			modifiedEntities.add(ec);
			break;
		case Delete:
			deletedEntities.add(ec);
			break;
		}

		/*
		 * if(ca.equals(ChangeAction.Modify)) {
		 * 
		 * if(createdEntities.contains(ec)) { // note the element in the set may
		 * be different from the one we insert, however the comparator returns
		 * the same result createdEntities.remove(ec); createdEntities.add(ec);
		 * } else { modifiedEntities.add(ec); }
		 * 
		 * } else if(ca.equals(ChangeAction.Create)) {
		 * 
		 * if(modifiedEntities.contains(ec)) { logger.warn("Adding " + ec +
		 * " for creation, however it is already being modified - leaving on modified"
		 * ); //createdEntities.remove(ec); //createdEntities.add(ec); } else
		 * if(deletedEntities.contains(ec)) { deletedEntities.remove(ec);
		 * modifiedEntities.add(ec); } else { createdEntities.add(ec); } } else
		 * if(ca.equals(ChangeAction.Delete)) { createdEntities.remove(ec);
		 * modifiedEntities.remove(ec); deletedEntities.add(ec); }
		 */
	}

	/*
	 * public static void main(String[] args) { //SetDiff<EntityContainer>
	 * entityDiff = new SetDiff<EntityContainer>(new
	 * EntityByTypeThenIdComparator()); Comparator<EntityContainer> c = new
	 * EntityByTypeThenIdComparator();
	 * 
	 * CommonEntityData ced1 = new CommonEntityData(1l, 1, new Date(), new
	 * OsmUser(2, "blah"), 123l); Node e1 = new Node(ced1, 1.0, 2.0);
	 * 
	 * 
	 * 
	 * 
	 * CommonEntityData ced2 = new CommonEntityData(0l, 1, new Date(), new
	 * OsmUser(2, "blah"), 123l, Arrays.asList(new Tag("hi", "world"))); Node e2
	 * = new Node(ced2, 2.0, 3.0f);
	 * 
	 * EntityContainer ec1 = new NodeContainer(e1); EntityContainer ec2 = new
	 * NodeContainer(e2);
	 * 
	 * 
	 * System.out.println("Comparator: " + c.compare(ec1, ec2));
	 * System.out.println("Equals: " + ec1.equals(ec2));
	 * 
	 * }
	 */

	// Number of entities that should be processed as a batch
	private int	maxEntityBatchSize	= 500;

	/*
	 * long entityDiffTimeSpan = 60000; private Date timeStamp = null;
	 */

	public IgnoreModifyDeleteDiffUpdateStrategy(ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			ISparqlExecutor graphDAO, String mainGraphName,
			RDFNodePositionDAO nodePositionDAO)
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.graphDAO = graphDAO;
		this.mainGraphName = mainGraphName;
		this.nodePositionDAO = nodePositionDAO;
		// this.nodeGraphName = nodeGraphName;
	}

	/**
	 * NOTE Does not set retained triples
	 * 
	 * @param o
	 * @param n
	 * @return
	 */
	public static IDiff<Model> diff(Model o, Model n)
	{
		Model added = ModelFactory.createDefaultModel();
		added.add(n);
		added.remove(o);

		Model removed = ModelFactory.createDefaultModel();
		removed.add(o);
		removed.remove(n);

		IDiff<Model> result = new Diff<Model>(added, removed, null);

		return result;
	}

	@Override
	public void process(ChangeContainer c)
	{
		addEntity(c.getAction(), c.getEntityContainer());

		/*
		 * if(c.getAction().equals(ChangeAction.Delete)) {
		 * entityDiff.remove(c.getEntityContainer()); } else {
		 * entityDiff.add(c.getEntityContainer()); }
		 */
	}

	private static Model executeConstruct(ISparqlExecutor graphDAO,
			Collection<String> queries) throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		int i = 1;
		for (String query : queries) {
			logger.info("Executing query " + (i++) + "/" + queries.size());
			// logger.info("Query = " + query);

			Model tmp = graphDAO.executeConstruct(query);

			result.add(tmp);
		}

		return result;
	}

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

	private static Pattern	rdfSeqPattern	= Pattern.compile(RDF.getURI()
													+ "_(\\d+)$");

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

	/**
	 * Scans the given models for triples having a rdf:_n predicate and returns
	 * a map of subject-> index -> object
	 * 
	 * @param model
	 * @return
	 */
	/*
	 * public Map<Resource, SortedMap<Integer, RDFNode>> processSeq(Model model)
	 * { Map<Resource, SortedMap<Integer, RDFNode>> result = new
	 * HashMap<Resource, SortedMap<Integer, RDFNode>>();
	 * 
	 * 
	 * for(Resource subject : model.listSubjects().toSet()) { SortedMap<Integer,
	 * RDFNode> part = processSeq(subject, model); result.put(subject, part); }
	 * 
	 * return result; }
	 */
	// FIXME: Clearify semantics: If a resource does not have a seq associated
	// should it appear in the result?
	// Currently the answer is "no" (therefore only resources with seqs are
	// returned.
	// However, actually all resources that are "a rdf:Seq" should be in the map
	// (i guess)
	public Map<Resource, TreeMap<Integer, RDFNode>> indexRdfMemberships(
			Model model)
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

	public void processSeq(Map<Resource, TreeMap<Integer, RDFNode>> result,
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

	private TreeMap<Integer, RDFNode> processSeq(Resource res, Model model)
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

	private void populatePointPosMappingVirtuoso(Model model,
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

	/*
	 * private void populatePointPosMappingGeoRSS(Model model, Map<Resource,
	 * String> nodeToPos) { StmtIterator it = model.listStatements(null,
	 * GeoRSS.point, (RDFNode)null); try { while(it.hasNext()) { Statement stmt
	 * = it.next();
	 * 
	 * String value = stmt.getLiteral().getValue().toString();
	 * nodeToPos.put(stmt.getSubject(), value); } } finally { it.close(); } }
	 */

	public Map<Resource, String> createNodePosMapFromNodesGeoRSS(Model model)
	{
		Map<Resource, String> result = new HashMap<Resource, String>();

		for (Statement stmt : model.listStatements(null, GeoRSS.point,
				(RDFNode) null).toList()) {
			String value = stmt.getLiteral().getValue().toString();
			result.put(stmt.getSubject(), value);
		}

		return result;
	}

	// Map<Resource, List<Resource>> wayToNodes
	/**
	 * Creates a Node-Pos Map by mapping a georss:node-string to triples of the
	 * corresponding nodes.
	 * 
	 * 
	 */
	public Map<Resource, String> createNodePosMapFromWays(Model model)
	{
		Map<Resource, String> result = new HashMap<Resource, String>();

		Set<Statement> ways = model
				.listStatements(null, GeoRSS.line, (RDFNode) null)
				.andThen(
						model.listStatements(null, GeoRSS.polygon,
								(RDFNode) null)).toSet();

		for (Statement stmt : ways) {
			Resource wayNode = vocab.wayToWayNode(stmt.getSubject());
			SortedMap<Integer, RDFNode> seq = processSeq(wayNode, model);

			String value = stmt.getObject().as(Literal.class).getValue()
					.toString();
			List<String> positions = tryParseGeoRRSPointList(value);

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

				result.put(node, positions.get(index));
			}
		}

		return result;
	}

	/**
	 * transforms a waynode resource to the corresponding way resource
	 * 
	 * 
	 * @param res
	 * @return
	 */

	public boolean hasRelevantTag(Model model, Resource subject)
	{
		for (Statement stmt : model.listStatements(subject, null,
				(RDFNode) null).toSet()) {
			if (!(stmt.getPredicate().equals(GeoRSS.point)
					|| stmt.getPredicate().equals(GeoRSS.geo)
					|| stmt.getPredicate().equals(WGS84Pos.xlat) || stmt
					.getPredicate().equals(WGS84Pos.xlong))) {
				return true;
			}
		}

		return false;
	}

	Model fetchStatementsBySubject(Iterable<Resource> subjects,
			String graphName, int chunkSize) throws Exception
	{
		Model result = ModelFactory.createDefaultModel();

		for (List<Resource> item : Iterables.partition(subjects, chunkSize)) {
			String query = GraphDAORDFEntityDAO.constructBySubject(item,
					graphName);
			Model part = graphDAO.executeConstruct(query);
			result.add(part);
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
	private MultiMap<Resource, Resource> extractDependencies(Model model)
	{
		MultiMap<Resource, Resource> result = new SetMultiHashMap<Resource, Resource>();
		Map<Resource, TreeMap<Integer, RDFNode>> index = indexRdfMemberships(model);

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

	private Map<Resource, String> createNodePosMapFromEntities(
			Iterable<EntityContainer> ecs)
	{
		Map<Resource, String> result = new HashMap<Resource, String>();

		for (EntityContainer ec : ecs) {
			Entity entity = ec.getEntity();

			if (entity instanceof Node) {
				Node node = (Node) entity;

				Resource resource = vocab.createResource(node);
				//String pos = node.getLongitude() + " " + node.getLatitude();
				String pos = node.getLatitude() + " " + node.getLongitude();
				
				result.put(resource, pos);
			}
		}

		return result;
	}

	private void createMinimalStatements(Iterable<EntityContainer> ecs,
			Model model)
	{
		for (EntityContainer ec : ecs) {
			Entity entity = ec.getEntity();

			if (entity instanceof Node) {
				Node node = (Node) entity;
				Resource subject = vocab.createResource(node);
				SimpleNodeToRDFTransformer.generateGeoRSS(model, subject, node);
			}
		}
	}
	
	
	
	public static Collection<Entity> entityView(Collection<EntityContainer> src)
	{
		return new TransformCollection<EntityContainer, Entity>(src,
				new Transformer<EntityContainer, Entity>() {
					@Override
					public Entity transform(EntityContainer input)
					{
						return input.getEntity();
					}
		});
	}
	
	public static Iterable<Entity> entityView(Iterable<EntityContainer> src)
	{
		return new TransformIterable<EntityContainer, Entity>(src,
				new Transformer<EntityContainer, Entity>() {
					@Override
					public Entity transform(EntityContainer input)
					{
						return input.getEntity();
					}
		});
	}
	

	Model createModelFromStatements(Iterable<Statement> statements)
	{
		Model result = ModelFactory.createDefaultModel();
		
		for(Statement stmt : statements) {
			result.add(stmt);
		}
		
		return result;
	}

	/*
	 * private void updatePredicate(Model model, Resource subject, Predicate
	 * predicate, RDFNode object) { }
	 */

	/**
	 * Some issues i recently noticed (as of 20 sept 2010): .) The nodeToPos map
	 * is only populated from the diff, However, all positions that are avaiable
	 * in the new set should be placed there.
	 * 
	 * 
	 * @param inDiff
	 * @param outDiff
	 * @param batchSize
	 * @throws Exception
	 */
	private void process(IDiff<? extends Collection<EntityContainer>> inDiff, RDFDiff outDiff, int batchSize)
		throws Exception
	{
		logger.info("Processing entities. Created/Modified/Deleted = " + createdEntities.size() + "/" + modifiedEntities.size() + "/" + deletedEntities.size());
		long start = System.nanoTime();

		Set<Resource> createdResources = GraphDAORDFEntityDAO.getInvolvedResources(createdEntities, vocab);
		Set<Resource> modifiedResources = GraphDAORDFEntityDAO.getInvolvedResources(modifiedEntities, vocab);
		Set<Resource> deletedResources = GraphDAORDFEntityDAO.getInvolvedResources(deletedEntities, vocab);
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed converting entities to resources."); 
		
		Set<Resource> deletedOrModifiedResources = Sets.union(deletedResources, modifiedResources);
		Set<EntityContainer> createdOrModifiedEntities = Sets.union(createdEntities, modifiedEntities);
		
		Model oldMainModel = fetchStatementsBySubject(deletedOrModifiedResources, mainGraphName, 512);				
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for " + deletedOrModifiedResources.size() + " modified or deleted entities, " + oldMainModel.size() + " triples fetched");
				
		Model newMainModel = ModelFactory.createDefaultModel();
		Set<EntityContainer> danglingEntities = transformToModel(createdOrModifiedEntities, newMainModel);
		//Set<Resource> danglingResources = GraphDAORDFEntityDAO.getInvolvedResources(danglingEntities, vocab);
		Set<Resource> danglingResources = new HashSet<Resource>();
		for(EntityContainer ec : danglingEntities) {
			Resource resource = vocab.createResource(ec.getEntity());
			if(resource != null) {
				danglingResources.add(resource);
			}
		}
		
		
		// Hint: This set corresponds to all new, non-dangling items
		Set<Resource> newMainSubjects = newMainModel.listSubjects().toSet();
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed RDF transformation of entities");
		
		
		// Based on the old model, it is possible to assign positions to nodes
		// as follows:
		// For each way (in the old model) retrieve the georss:line / polygon triples
		// Also retrieve all node memberships (they are also always completly part of the old model)
		// We parse the string, and map all points to the corresponding nodes
		
		
		// Afterwards, we scan the diff, whether any of the positions of the nodes were changed
		// and update our node-position mapping accordingly
		//Map<Resource, String> nodeToPos = new HashMap<Resource, String>();
		//Map<Resource, List<Resource>> wayToNodes = new HashMap<Resource, List<Resource>>();
		
		// Deduce PointPos mappings based on georss:(line|polygon)s from the old model
		
		// GAH! Do we need to the following two calls?
		// Yes: We need them in order to create way polygons for ways that don't
		// have this triple in the old model.
		// In that case we can't patch an existing point list, but we rather
		// have to create it anew
		//populatePointPosMapping(oldModel.listStatements(null, GeoRSS.line, (RDFNode)null), oldModel, nodeToPos);
		//populatePointPosMapping(oldModel.listStatements(null, GeoRSS.polygon, (RDFNode)null), oldModel, nodeToPos);

		Set<Statement> oldMainModelSetView = new ModelSetView(oldMainModel);
		Set<Statement> newMainModelSetView = new ModelSetView(newMainModel);
		
		SetDiff<Statement> mainDiff = new SetDiff<Statement>(newMainModelSetView, oldMainModelSetView);
		//IDiff<Model> diff = diff(oldMainModel, newMainModel);
		
		//outDiff.remove(diff.getRemoved());
		//outDiff.add(diff.getAdded());
		
		
		
		

		
		//Map<Resource, Resource> wayNodeReferences
		

		// Determine the set set of nodes that have been repositioned
		// These are nodes that have a position-related triple in the diff
		Map<Resource, String> repositionedNodes = createNodePosMapFromNodesGeoRSS(createModelFromStatements(mainDiff.getAdded()));
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed populating " +  repositionedNodes.size() + " repositioned nodes");
			
		// FIXME: Add an option to exclude newly created nodes here - as they can't affect
		// any previously existing ways anyway (however, this may cause problems:
		// if the store is initialized from a dump, and then change sets are applied starting from before the dump,
		// then a create wouldn't cause the store to be checked for existing data.
		// hm, ok this is no problem: as the store would contain the same data then, and only duplicates would be
		// inserted.
		
		// Remove all deleted and created resources from repositionedNodes
		// - thus all non-modified ones - this can be done more nicely:
		// Note that a modified node does not imply a repositioned one (as e.g. only the tags could have changed).		
		for(Resource res : Iterables.concat(createdResources, deletedResources))
			repositionedNodes.remove(res);		
		
		
		Model wayPatchSet = selectWayNodesByNodes(graphDAO, mainGraphName, repositionedNodes.keySet());
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching ways for " + repositionedNodes.keySet().size() + " repositioned nodes, " + wayPatchSet.size() + " waynodes affected");

		Map<Resource, TreeMap<Integer, RDFNode>> affectedWays = indexRdfMemberships(wayPatchSet);
		Map<Resource, TreeMap<Integer, RDFNode>> newWays = indexRdfMemberships(newMainModel);

		// If an affected way is also a new way, only treat it as a new one
		// (As only new ways undergo structural changes)
		for(Resource newWay : newWays.keySet())
			affectedWays.remove(newWay);
		
		// Create inferred node-pos mappings based on georss data in the old model
		Map<Resource, String> nodeToPos = createNodePosMapFromWays(oldMainModel);
		
		// Index the positions of all newly created/modified nodes
		// Note that we overwrite the inferred data with data from the new model
		Map<Resource, String> nodeToPosTmp = createNodePosMapFromEntities(Iterables.concat(createdEntities, modifiedEntities));
		nodeToPos.putAll(nodeToPosTmp);


		
		// Whenever a reference from a way to a node gets removed, the node
		// may become dangling,
		// unless it either
		//   a) is already a subject in the newMain graph
		//   b) is referenced by another way (this will be checked for in the dependencies section)
		MultiMap<Resource, Resource> lostNodeLinks = extractDependencies(createModelFromStatements(mainDiff.getRemoved()));
		danglingResources.addAll(Sets.difference(lostNodeLinks.keySet(), newMainSubjects));

		
		// A map for which resource depends on which
		MultiMap<Resource, Resource> dependencies = new SetMultiHashMap<Resource, Resource>(); 
		
		
		/****/
		// For each dangling entity, determine whether it is referenced by a non-dangling
		// entity
		// In concrete this means: a dangling node must be referenced by a non-dangling way
		// and a dangling way must be referenced by a non-dangling relation.
		
		
		
		
		
		
		// TODO ONLY IF THERE WERE RELATIONS:
		// UWa) Undangle all currently dangling ways that are referenced by non-danling relations
		// UWb) For the remaining ways, check whether they are referenced in the database
		
		
		// Undangle all currently danling nodes that are referenced by ways of this changeset
		
		// UNa) Undangle all currently danling nodes that are referenced by relations or ways of this changeset
		//Set<Resource> undangledResources = new HashSet<Resource>();
		dependencies.putAll(extractDependencies(newMainModel));
		
		// FIXME Do we have to remove dependencies based on the old model? I'd say no
		
		//undangledResources.addAll(dependencies.keySet());
		danglingResources.removeAll(dependencies.keySet());

		
		
		// UNb)
		// Note: We are selecting node references from the database,
		// Note: We do not have to lookup dangling resources that were created in this batch
		// however some of the references may just about to become deleted
		// FIXME Replace dangling resources with dangling nodes - as We might undangle ways here, although only nodes are undangled here
		Set<Resource> danglingResourceReferenceLookup = Sets.difference(danglingResources, createdResources);
		Model resourceDependencyModel = selectReferencedNodes(graphDAO, mainGraphName, danglingResourceReferenceLookup);
		resourceDependencyModel.remove(Lists.newArrayList(mainDiff.getRemoved()));
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching references for " + danglingResourceReferenceLookup.size() + " dangling resources");
		
		
		
		MultiMap<Resource, Resource> dependenciesTmp = extractDependencies(resourceDependencyModel); 
		danglingResources.removeAll(dependenciesTmp.keySet());

		dependencies.putAll(dependenciesTmp);
		
		
		// The remaining dangling items will not go into the main graph
		// Now we know which positions we need to retrieve
		
		

		// TODO Separate the set into nodes and ways from the beginning
		Set<Resource> danglingNodes = new HashSet<Resource>();
		for(Resource resource : danglingResources) {
			if(resource.toString().contains("/node"))
				danglingNodes.add(resource);
		}
		
		// TODO As long as there are no relations, only nodes can be danling
		danglingResources = danglingNodes;
		
		// Set<Resource> danglingWays
		
		
		
		//nodeGraphDiff.getRemoved().addAll();
		//nodeGraphDiff.
		for(Node node : Iterables.filter(entityView(deletedEntities), Node.class))
			nodeDiff.getRemoved().add(node);
				
		// We need to retrieve everything of all modified items that are
		// dangling and add them to the old model, so that they get deleted
		// properly
		Set<Resource> danglingResourceDataLookup = Sets.intersection(danglingResources, modifiedResources);
		Model danglingModel = fetchStatementsBySubject(danglingResourceDataLookup, mainGraphName, 1024);
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for " + danglingResourceDataLookup.size() + " modified dangling resources, " + danglingModel.size() + " triples fetched");
		oldMainModel.add(danglingModel);
		
		
		
		// The positions of all undangled nodes need to go into the main graph
		Model minimalStatementModel = ModelFactory.createDefaultModel();
		for(EntityContainer ecc : Iterables.concat(createdEntities, modifiedEntities)) {
			Resource resource = vocab.createResource(ecc.getEntity());
			if(dependencies.keySet().contains(resource) && !newMainSubjects.contains(resource)) {
				
				createMinimalStatements(Collections.singleton(ecc), minimalStatementModel);
			}
		}
		newMainModel.add(minimalStatementModel);


		// FIXME whenever a modified node goes into the main graph, it should be removed from the NodePositionGraph
		// However, actually this is only optional!

		

		// FIXME: Add all nodes to the node diff that did not go into the main graph
		// (Hm, but these are exactly all the dangling nodes, aren't they?)
		for(Node node : Iterables.filter(entityView(Iterables.concat(createdEntities, modifiedEntities)), Node.class)) {
			if(danglingResources.contains(vocab.createResource(node)))
					nodeDiff.getAdded().add(node);
		}
		
		
		// Determine which node positions we yet have to retrieve
		// These are all nodes of newWays that do not yet have a position
		// assigned
		Set<Resource> unindexedNodes = new HashSet<Resource>();
		//unindexedNodes.addAll(danglingNodes);
		
		for(SortedMap<Integer, RDFNode> indexToNode : newWays.values()) {
			for(RDFNode node : indexToNode.values()) {
				if(!(node instanceof Resource)) {
					logger.error("Not a node: " + node);
				}
				
				if(!nodeToPos.containsKey(node))
					unindexedNodes.add((Resource)node);
			}
		}
		Map<Resource, RDFNode> mappings = fetchNodePositions(graphDAO, mainGraphName, unindexedNodes);
		
		
		for(Map.Entry<Resource, RDFNode> mapping : mappings.entrySet()) {
			nodeToPos.put(mapping.getKey(), mapping.getValue().toString());
		}
		
		
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching positions for " + mappings.size() + " additional nodes");


		unindexedNodes.remove(mappings.keySet());
		
		
		// Check the node store for additional node positions
		// For all unindexed nodes that do not have a position yet, try
		// to retrieve them from the nodePositionDAO
		Map<Resource, Point2D> nodePositionDAOLookups = nodePositionDAO.lookup(unindexedNodes);
		for(Map.Entry<Resource, Point2D> entry : nodePositionDAOLookups.entrySet()) {
			Resource resource = entry.getKey();
			Point2D point = entry.getValue();

			nodeToPos.put(resource, point.getX() + " " + point.getY());
		}		
		unindexedNodes.remove(nodePositionDAOLookups.keySet());
		
		
		// Retrieve georss of affected ways
		// Note: There is no point in attempting to reuse georrs from the old model,
		// as this georss has already been decomposed into the 'nodeToPos' map.
		// TODO Clearify
		
		
		// FIXME: If the positions of all nodes of a way are already known,
		// there is no need to fetch its georss, as it can be computed
		// directly. (However this is only the case for ways that were edited, where
		// all nodes are in the same changeset)
		Model georssOfAffectedWays = constructGeoRSSLinePolygon(graphDAO, mainGraphName, affectedWays.keySet());
		
		
		
		// Now patch the georss
		// Note We need to take the removed triples into account when patching
		// the georss point list for that case that n nodes are removed
		// from the end of a way.
		for(Map.Entry<Resource, TreeMap<Integer, RDFNode>> affectedWay : affectedWays.entrySet()) {
			Resource wayNode = affectedWay.getKey();
			Resource way = vocab.wayNodeToWay(wayNode);
			
			if(way == null) {
				logger.error(wayNode + " is not a wayNode");
				continue;
			}
			
			
			TreeMap<Integer, RDFNode> fixes = affectedWay.getValue();

			if(way.getURI().toString().equals("http://linkedgeodata.org/triplify/way54871694")) {
				System.out.println("GOT IT");
			}
			
			
			Set<Statement> georssStmts =
				georssOfAffectedWays.listStatements(null, GeoRSS.line, (RDFNode)null).andThen(
						georssOfAffectedWays.listStatements(null, GeoRSS.polygon, (RDFNode)null)).toSet();

			if(georssStmts.isEmpty()) {
				logger.warn("Cannot update georss of way " + way + " because no pre-existing georss found.");
				continue;
			} else if(georssStmts.size() > 1) {
				logger.warn("Cannot update georss of way " + way + " because multiple georss found.");
				continue;
			}
			
			Statement base = georssStmts.iterator().next();
			
			String geoStr = base.getLiteral().getValue().toString();
			ArrayList<String> positions = tryParseGeoRRSPointList(geoStr);
							
			int highestUpdateIndex = fixes.isEmpty()
				? -1
				: fixes.lastEntry().getKey() - 1;				
				
			for(Map.Entry<Integer, RDFNode> fix : fixes.entrySet()) {
				int index = fix.getKey() - 1;
				
				String value = nodeToPos.get(fix.getValue());
				if(value == null) {
					logger.warn("Cannot patch way " + way + " because its point list references node " + fix.getValue() + " for which no position was found");
					positions = null;
					break;
				}
				
				if(index >= positions.size()) {
					while(index > positions.size()) {							
						// FIXME: The positions.size() that is printed out might be inaccuracte, as some nodes may have already been added, before this error occurred.
						// The fix would be to print out only the original number of positions in the georrs. 
						logger.warn("Error patching way " + way + " because its georrs:line/polygon property is out of sync with the actual nodes: It was attempted to patch the georrs value on index " + index + ", although there are only " + positions.size() + " positions.");
						positions = null;
						break;
					}					
					positions.add(value);
				}
				else {					
					positions.set(index, value);
				}
			}
			
			
			if(positions != null) {
				// Check against diff.removed whether we need to remove some positions
				
				for(Statement check : createModelFromStatements(mainDiff.getRemoved()).listStatements(wayNode, null, (RDFNode)null).toList()) {
					Integer index = tryParseSeqPredicate(check.getPredicate());
					if(index == null)
						continue;
					--index;
					
					// Remove all indexes that are above highestUpdateIndex
					if(index > highestUpdateIndex)
						positions.remove(index); // FIXME This does array shifting
				}

				Property predicate = base.getPredicate(); 
				
				// WRONG: A line may have become a polygon and vice versa - check what type we have
				// When repositioning nodes, the above can't happen
				/*
				Resource wayNode2 = wayToWayNode(base.getSubject());
				TreeMap<Integer, RDFNode> indexToNode = ws.get(wayNode2);
				if(indexToNode != null && !indexToNode.isEmpty()) {
					predicate = indexToNode.firstEntry().getValue().equals(indexToNode.lastEntry().getValue())
							? GeoRSS.polygon
							: GeoRSS.line;
				}
				*/
				
				// Check whether the way (and therefore everything of it)
				// is part of the newModel				
				
				String newValue = StringUtil.implode(" ", positions);

				//outDiff.remove(base); 
				//Statement stmt = newMainModel.createStatement(base.getSubject(), predicate, newValue);
				//newMainModel.add(stmt);
				
				newMainModel.add(base.getSubject(), predicate, newValue);
				//V outDiff.add(stmt);
			}
			
		}

		
		// Process new ways
		
		// Finally, for each way generate the georss
		//for(Map.Entry<Resource, TreeMap<Integer, RDFNode>> w : ws.entrySet()) {
		for(Map.Entry<Resource, TreeMap<Integer, RDFNode>> newWay : newWays.entrySet()) {
			Resource wayNode = newWay.getKey();
			Resource way = vocab.wayNodeToWay(wayNode);
			
			if(way == null) {
				logger.error(way + " is not a way");
				continue;
			}
			
			TreeMap<Integer, RDFNode> fixes = newWay.getValue();
			
			
			List<String> geoRSSParts = new ArrayList<String>();				
			int i = 1;
			/*
			if(wayNode.getURI().toString().equals("http://linkedgeodata.org/triplify/way54871694/nodes")) {
				System.out.println("GOT IT");
			}*/

			int lookupErrors = 0;

			if(fixes.isEmpty())
				continue;
			
			for(Map.Entry<Integer, RDFNode> fix : fixes.entrySet()) {
				if(fix.getKey() != (i++)) {
					logger.warn("Index out of sync: " + fix);
				}
				
				String value = nodeToPos.get(fix.getValue());
				if(value == null) {
					++lookupErrors;
				}
				
				if(lookupErrors == 0)
					geoRSSParts.add(value);
			}
			
			if(lookupErrors > 0) {
				logger.warn("Cannot create georrs for way " + wayNode + " because no positions were found for " + lookupErrors + " nodes"); 
				continue;
			}
			
			String geoRSS = StringUtil.implode(" ", geoRSSParts);
			
			Property predicate = fixes.firstEntry().getValue().equals(fixes.lastEntry().getValue())
				? GeoRSS.polygon
				: GeoRSS.line;
			
			Statement stmt = newMainModel.createStatement(way, predicate, geoRSS);
			newMainModel.add(stmt);
			//V outDiff.add(stmt);
		}
				
		
		
		// Finally: In the diff: Remove the georss:line/polygon triples
		// from the "remove" set if the resource exists in the new set but
		// doesn't have that triple
		//V TODO Update description above: We add those tripes to the newMainGraph
		// so that these triples disappear from the diff
		for(Statement stmt : mainDiff.getRemoved()) {
			if(stmt.getPredicate().equals(GeoRSS.line) || stmt.getPredicate().equals(GeoRSS.polygon)) {

				if(newMainModel.contains(stmt.getSubject(), null) &&
						!(	newMainModel.contains(stmt.getSubject(), GeoRSS.line)
								|| newMainModel.contains(stmt.getSubject(), GeoRSS.polygon))) {
					newMainModel.add(stmt);
				}
			}
		}
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed processing of georss");
		
		/*
		ExtendedIterator<Statement> x =
			outDiff.getRemoved().listStatements(null, GeoRSS.line, (RDFNode)null).andThen(
				outDiff.getRemoved().listStatements(null, GeoRSS.polygon, (RDFNode)null));
		
		while(x.hasNext()) {
			Statement stmt = x.next();
			
			if(newMainModel.contains(stmt.getSubject(), null) &&
					!(	newMainModel.contains(stmt.getSubject(), GeoRSS.line)
							|| newMainModel.contains(stmt.getSubject(), GeoRSS.polygon))) {
				//x.remove();
				newMainModel.add(stmt);
			}
		}
*/
		
		
		outDiff.getAdded().add(Lists.newArrayList(mainDiff.getAdded()));
		outDiff.getRemoved().add(Lists.newArrayList(mainDiff.getRemoved()));
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed processing of entities");
	}

	/*
	 * public static boolean isTaglessNode(Entity entity) { if(!(entity
	 * instanceof Node)) return false;
	 * 
	 * return entity.getTags().isEmpty(); }
	 */

	/**
	 * Given a set of entities, generates RDF from them, and returns the set of
	 * entities for which no RDF was generated.
	 */
	private Set<EntityContainer> transformToModel(
			Iterable<EntityContainer> ecs, Model mainModel) throws Exception
	{
		Set<EntityContainer> result = new HashSet<EntityContainer>();

		Model newModel = ModelFactory.createDefaultModel();
		for (EntityContainer ec : ecs) {
			Entity entity = ec.getEntity();

			// FIXME Doesn't work this way, as ways have at least two resources
			// So the hack is to simple remove any tagless items
			if (entity.getTags().isEmpty()) {
				result.add(ec);
				continue;
			}

			// Resource subject = vocab.createResource(entity);
			entityTransformer.transform(newModel, entity);

			// Remove entities that do not have any relevant tags.
			/*
			 * if(!hasRelevantTag(newModel, subject)) {
			 * newModel.remove(newModel.listStatements(subject, null,
			 * (RDFNode)null)); result.add(ec); }
			 */
		}

		// Virtuoso-specific transforms for the triples that were added
		postProcessTransformer.transform(mainModel, newModel);
		// System.out.println(ModelUtil.toString(added));

		return result;
	}

	@Override
	public void complete()
	{
		// logger.info(this.getClass() + " completed");
		try {
			mainGraphDiff = new RDFDiff();

			nodeDiff = new TreeSetDiff<Node>();
			//nodeGraphDiff = new RDFDiff();

			// process(entityDiff, mainGraphDiff, maxEntityBatchSize);
			process(null, mainGraphDiff, maxEntityBatchSize);
			// entityDiff.clear();
		} catch (Exception e) {
			logger.error("An error occurred at the completion phase of a task",
					e);
			throw new RuntimeException(e);
		} finally {
			clear();
		}
	}

	public RDFDiff getMainGraphDiff()
	{
		return mainGraphDiff;
	}

	public TreeSetDiff<Node> getNodeDiff()
	{
		return nodeDiff;
	}
	
	/*
	public RDFDiff getNodeGraphDiff()
	{
		return nodeGraphDiff;
	}*/

	@Override
	public void release()
	{
		mainGraphDiff = null;
		nodeDiff = null;
	}

	public static Map<Resource, RDFNode> fetchNodePositions(
			ISparqlExecutor graphDAO, String graphName, Set<Resource> nodes)
			throws Exception
	{
		Map<Resource, RDFNode> result = new HashMap<Resource, RDFNode>();

		if (nodes.isEmpty())
			return result;

		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		for (List<Resource> chunk : chunks) {
			String query = "Select ?n ?o From <" + graphName + "> { ?n <"
					+ GeoRSS.point + "> ?o . Filter(?n In (<"
					+ StringUtil.implode(">,<", chunk) + ">)) . }";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			// Model tmp = graphDAO.executeConstruct(query);
			List<QuerySolution> qs = graphDAO.executeSelect(query);
			for (QuerySolution q : qs) {
				result.put(q.getResource("n"), q.get("o"));
			}
		}

		return result;
	}

	public static Model constructGeoRSSLinePolygon(ISparqlExecutor graphDAO,
			String graphName, Set<Resource> ways) throws Exception
	{
		if (ways.isEmpty())
			return ModelFactory.createDefaultModel();

		String query = "Construct {?s ?p ?o . } From <" + graphName
				+ "> { ?s ?p ?o . Filter(?s In (<"
				+ StringUtil.implode(">,<", ways) + ">) && ?p In (<"
				+ GeoRSS.point.toString() + "> || <" + GeoRSS.polygon
				+ ">)) . }";
		Model result = graphDAO.executeConstruct(query);

		return result;
	}

	public static Model selectWayNodesByNodes(ISparqlExecutor graphDAO,
			String graphName, Collection<Resource> nodes) throws Exception
	{
		if (nodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {
			String query = "Construct { ?wn ?p ?n } From <" + graphName
					+ "> { ?wn ?p ?n . Filter(?n In (<"
					+ StringUtil.implode(">,<", chunk) + ">)) . }";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}

		return result;
	}

	/**
	 * Note: This is just looking up triples by their certain objects. So this
	 * method could be generalized
	 */
	public static Model selectNodesByWayNodes(ISparqlExecutor graphDAO,
			String graphName, Collection<Resource> wayNodes) throws Exception
	{
		if (wayNodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(wayNodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {
			String query = "Construct { ?wn ?p ?n } From <" + graphName
					+ "> { ?wn ?p ?n . Filter(?wn In (<"
					+ StringUtil.implode(">,<", chunk) + ">)) . }";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}

		return result;
	}

	/**
	 * Select untagged nodes belonging to the given wayNodes. Maybe a better
	 * solution would be to retrieve all wayNodes that reference a given node
	 * using the 'selectWayNodesByNodes' method. As the information may be more
	 * suitable for caching.
	 * 
	 * 
	 * @param graphDAO
	 * @param graphName
	 * @param wayNodes
	 * @return
	 * @throws Exception
	 */
	public static Model selectReferencedNodes(ISparqlExecutor graphDAO,
			String graphName, Collection<Resource> nodes) throws Exception
	{
		if (nodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {
			// String query = "Select ?n From <" + graphName +
			// "> { ?n ?p1 ?o1 . Optional { ?n ?p2 ?o2 . Filter !(?p1 = ?p2 && ?o1 = ?o2)) . } . Filter(?n In (<"
			// + StringUtil.implode(">,<", chunk) + ">) .  }";
			String query = "Construct {?wn ?i ?n . } From <" + graphName
					+ "> { ?wn ?i ?n . Filter(?n In (<"
					+ StringUtil.implode(">,<", chunk) + ">)) . }";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}

		return result;
	}
}

/*
 * Following code can be removed as soon as the plugin is working - because then
 * its definitely not needed anymore private static String
 * constructBySubject(String iri, String graphName) { String fromPart =
 * (graphName != null) ? "From <" + graphName + "> " : "";
 * 
 * String result = "Construct { ?s ?p ?o . } " + fromPart +
 * "{ ?s ?p ?o . Filter(?s = <" + iri + ">) . }";
 * 
 * return result; }
 * 
 * List<QuerySolution> rs = graphDAO.executeSelect(query); for(QuerySolution q :
 * rs) { Resource wayNode = q.getResource("wn");
 * 
 * String str = wayNode.getURI().toString(); if(!str.endsWith("\nodes")) { throw
 * new RuntimeException("A way node did not end with /nodes; uri = " + str); }
 * 
 * str = str.substring(0, str.length() - 6);
 * 
 * 
 * Resource way = ResourceFactory.createResource(str);
 * 
 * result.add(way); }
 */

/*
 * private static String constructNodeModelQuery(ILGDVocab vocab, long nodeId,
 * String graphName) { String nodeIRI = vocab.createNIRNodeURI(nodeId);
 * 
 * String fromPart = (graphName != null) ? "From <" + graphName + "> " : "";
 * 
 * String result = "Construct { ?s ?p ?o . } " + fromPart +
 * "{ ?s ?p ?o . Filter(?s = <" + nodeIRI + ">) . }";
 * 
 * return result; }
 */

/*
 * private static String constructQuery(final ILGDVocab vocab, Iterable<Long>
 * nodeIds, Iterable<Long> wayIds, String graphName) {
 * if(!wayIds.iterator().hasNext()) return "";
 * 
 * String resources = "";
 * 
 * resources += StringUtil.implode(",", new TransformIterable<Long, String>(
 * nodeIds, new Transformer<Long, String>() {
 * 
 * @Override public String transform(Long nodeId) { return
 * vocab.createNIRNodeURI(nodeId); } }));
 * 
 * resources += StringUtil.implode(",", new TransformIterable<Long, String>(
 * nodeIds, new Transformer<Long, String>() {
 * 
 * @Override public String transform(Long wayId) { return "<" +
 * vocab.createNIRWayURI(wayId) + ">,<" +
 * vocab.getHasNodesResource(wayId).toString() + ">"; } }));
 * 
 * 
 * String fromPart = (graphName != null) ? "From <" + graphName + "> " : "";
 * 
 * String result = "Construct { ?s ?p ?o . } " + fromPart +
 * "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
 * 
 * return result; }
 */

/*
 * private static Model getBySubject(ISparqlExecutor graphDAO, String iri,
 * String graphName) throws Exception { String query = constructBySubject(iri,
 * graphName);
 * 
 * //logger.info("Created query: " + query);
 * 
 * Model model = graphDAO.executeConstruct(query);
 * 
 * return model; }
 */

/*
 * if(timeStamp == null) timeStamp = new Date();
 * 
 * Date now = new Date();
 */

/*
 * if(timeStamp == null) timeStamp = entity.getTimestamp();
 * 
 * Date now = entity.getTimestamp();
 * 
 * if(timeStamp.getTime() > now.getTime()) {
 * logger.warn("Warning: Entities arriving out of order: " + timeStamp + " > " +
 * now); }
 */

/*
 * if(timeStamp == null) timeStamp = new Date();
 * 
 * Date now = new Date();
 * 
 * if(now.getTime() - timeStamp.getTime() > entityDiffTimeSpan) {
 * process(timelyDiff);
 * 
 * timeStamp = now;
 * 
 * entities.clear(); }
 * 
 * 
 * 
 * 
 * // Process all affected ways in order to update their linestrings/polygons
 * 
 * // Process affected ways // Whenever the shape of a way changes, it is very
 * likely that all // nodes are already in the same changeset. However, if the
 * way // is connected to already existing nodes, these nodes won't appear in //
 * the changeset. String query =
 * "Select ?w ?i ?p {:w ldo:hasNodes ?wn . ?wn ?i ?n . Optional { ?n georss:point ?p }. }"
 * ;
 * 
 * 
 * Model newMainModel = ModelFactory.createDefaultModel(); List<Way> ways =
 * CollectionUtils.filterByType(inDiff.getAdded(), Way.class); for(Way way :
 * ways) { Resource res = vocab.getHasNodesResource(way.getId()); StmtIterator
 * itWayNode = newMainModel.listStatements(res, vocab.getHasNodesPred(),
 * (RDFNode)null); try { Statement stmt = itWayNode.next();
 * 
 * StmtIterator itNode = newMainModel.listLiteralStatements(stmt.getObject(), ,
 * object)
 * 
 * } finally { itWayNode.close(); }
 * 
 * 
 * //outDiff.getAdded()
 * 
 * }
 * 
 * 
 * 
 * // Each node may affect zero or more ways // Retrieve the ids of those ways
 * that have not already been updated. List<Node> nodes =
 * CollectionUtils.filterByType(inDiff.getAdded(), Node.class); String query =
 * "Select ?w ?i ?n {?w ldo:hasNodes ?wn . ?wn ?i ?n . ?n georss:point ?p . }";
 * 
 * 
 * 
 * //for(Entity inDiff.getAdded()
 * 
 * // Update the node-way and node-position map
 */
