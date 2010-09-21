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
import java.util.Comparator;
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

import javassist.bytecode.Descriptor.Iterator;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.map.LRUMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.TransformIterable;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.sort.v0_6.EntityByTypeThenIdComparator;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;


class SetMultiHashMap<K, V>
	extends MultiHashMap<K, V>
{
	@Override
	protected Set<V> createCollection(Collection<? extends V> col) {
		return (col == null)
			? new HashSet<V>()
			: new HashSet<V>(col);
	}
}


class NodeWayCache
{
	private SetMultiHashMap<Resource, Resource> nodeToWays = new SetMultiHashMap<Resource, Resource>();
	private MultiHashMap<Resource, Resource> wayToNodes = new MultiHashMap<Resource, Resource>();
	private Set<Resource> negCache;
	
	
	/*
	public Collection<Resource> lookupWaysByNode(Iterable<Resource> nodes) {
		for(Resource node : nodes) {

		}
	}*/
}

class LGDCache
{
	enum State
	{
		Complete, // Every statement about a resource is in the cache
		Partial,  // Not all statements about a resoruce are in the cache
		None,     // The resource doesn't exist
		Unknown   // Cache miss
	}
	
	private Model posCache = ModelFactory.createDefaultModel();
	private LRUMap<Resource, State> resourceToState = new LRUMap<Resource, State>();
	
	private Set<Resource> negCache;
	private String graphName;
	private ISparulExecutor graphDAO;

	
	//private LRUMap<Long, Point2D> nodeToPosition = new LRUMap<Long, Point2D>();
	
	
	//private LinkedList<Resource> lru = new LinkedList<Resource>();

	private int maxSize = 1000000;
	
	public LGDCache()
	{
	}
	
	public void insert(Model model)
	{
		while(Math.max(0, model.size() - (maxSize - posCache.size())) > 0) {
			
		
		}

		//if(cache.size() + model.size() > maxSize) {
			
		//}
		StmtIterator it = model.listStatements();
		try {
			while(it.hasNext()) {
				
				
			}
		}
		finally {
			it.close();
		}
	}
	
	
	public void remove(Model model)
	{
	}
	
	public String createSubjectQuery(Iterable<Resource> subjects)
	{
		//String resources = "<" + StringUtil.implode(">,<", uris) + ">";
		String resources = StringUtil.implode(",", subjects);
		
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
	
		//return Collections.singletonList(result);
		return result;
	}
	
	
	public void lookup(Model out, Iterable<Resource> subjects)
	{
		List<Resource> cacheMisses = coreLookup(out, subjects);
		
		String query = createSubjectQuery(cacheMisses);
		//Model model = graphDAO.executeConstruct(query);
		
		//model.listSubjects().toSet()
		
		
		
		for(Resource miss : cacheMisses) {
			
		}
		
	}
	
	public List<Resource> coreLookup(Model out, Iterable<Resource> subjects)
	{
		List<Resource> cacheMisses = new ArrayList<Resource>();
		
		for(Resource subject : subjects) {
			if(negCache.contains(subject))
				continue;
			
			StmtIterator it = posCache.listStatements(subject, null, (RDFNode)null);
			
			if(!it.hasNext()) {
				cacheMisses.add(subject);
			} else {
				while(it.hasNext()) {
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
 * Entities are being passed to this Change sink.
 * Entities then have their tags filtered, and are then themselves classified as
 * either "accept", "reject", or "position accept".
 * 
 *
 * 
 * 
 * Each entity is transformed to RDF, however not all RDF data can be generated at once.
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
class ChunkIterator<T>
	implements Iterator<Collection<T>>
{
	private Collection<T> source;
	private int batchSize;

	public CollectionChunker(Collection<T> source, int batchSize)
	{
		this.source = source;
		this.batchSize = batchSize;
	}

	@Override
	public boolean hasNext()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<T> next()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}


class CollectionChunker<T>
	extends AbstractCollection<Collection<T>>
{
	private Collection<T> source;
	private int batchSize;
	
	public CollectionChunker(Collection<T> source, int batchSize)
	{
		this.source = source;
		this.batchSize = batchSize;
	}

	@Override
	public Iterator<Collection<T>> iterator()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size()
	{
		return source.size() / batchSize; 
	}
}
*/


class SetDiff<T>
	extends CollectionDiff<T, Set<T>>
{
	/*
	public SetDiff(Comparator<T> comparator)
	{
	}
	*/

	public SetDiff(Comparator<T> comparator)
	{
		super(
				new TreeSet<T>(comparator),
				new TreeSet<T>(comparator),
				new TreeSet<T>(comparator));
	}	
}

abstract class CollectionDiff<T, C extends Collection<T>>
	extends Diff<C>
{	
	public CollectionDiff(C added, C removed, C retained)
	{
		super(added, removed, retained);
	}

	public void add(T item) {
		getRemoved().remove(item);
		getAdded().add(item);
	}
	
	public void remove(T item) {
		getAdded().remove(item);
		getRemoved().add(item);
	}
	
	public void clear() {
		getAdded().clear();
		getRemoved().clear();
	}
	
	
	public int size()
	{
		return getAdded().size() + getRemoved().size();
	}
}

/**
 * Update Strategy which ignores the information of deleted tags - and in consequence triples -
 * from modified elements.
 * Therefore this strategy always performs a diff.
 * 
 * @author raven
 *
 * FIXME Somehow separate the store update code from the timely diff code
 *
 */
public class IgnoreModifyDeleteDiffUpdateStrategy
	implements IUpdateStrategy
{
	private static final Logger logger = Logger.getLogger(IUpdateStrategy.class);
	
	private ILGDVocab vocab; 
	private ITransformer<Entity, Model> entityTransformer;
	private ISparqlExecutor graphDAO;	
	private String mainGraphName;
	private String nodeGraphName;

	private ITransformer<Model, Model> postProcessTransformer = new VirtuosoStatementNormalizer();
	
	
	private RDFDiff mainGraphDiff;
	
	private Predicate<Entity> entityFilter;
	private Predicate<Tag> tagFilter;
	
	
	
	//private LRUMap<Long, Point2D> nodeToPosition = new LRUMap<Long, Point2D>();
	//private LRUMap<Long, Long> nodeToWay = new LRUMap<Long, Long>();
	
	

	// The nodes which are removed/inserted into the NodeStore
	// This graph only contains triples of the form <node> geo:geometry "value"^^virtrdf:geo
	private RDFDiff nodeGraphDiff;
	//private IDiff<Map<Long, Point2D>> nodeDiff = new Diff<Map<Long, Point2D>>(new HashMap<Long, Point2D>(), new HashMap<Long, Point2D>(), null);
	
	
	// Unfortunately we need to make the entityFilter a property of this class.
	// As nodes that will be filtered out need to be treated in a special way:
	// Whenever nodes are filtered out, they are written to a separate graph
	// (or table - doesn't matter). This way it is always possible to retrieve
	// the positions of nodes. This is necessairy for computing the polygons
	// of ways.
	//private Predicate<Tag> tagFilter;
	//private Predicate<Entity> entityFilter;
	
	
	
	//private Set<Entity> entities = new HashSet<Entity>();
	
	SetDiff<EntityContainer> entityDiff = new SetDiff<EntityContainer>(new EntityByTypeThenIdComparator());
	
	// Number of entities that should be processed as a batch
	private int maxEntityBatchSize = 500;
	
	/*
	long entityDiffTimeSpan = 60000;	
	private Date timeStamp = null;
	*/
	
	public IgnoreModifyDeleteDiffUpdateStrategy(
			ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			ISparqlExecutor graphDAO,
			String mainGraphName)
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.graphDAO = graphDAO;
		this.mainGraphName = mainGraphName;
		this.nodeGraphName = nodeGraphName;
	}
	
	/**
	 * NOTE Does not set retained triples
	 * 
	 * @param o
	 * @param n
	 * @return
	 */
	private static IDiff<Model> diff(Model o, Model n)
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
		if(c.getAction().equals(ChangeAction.Delete)) {
			entityDiff.remove(c.getEntityContainer());
		}		
		else {
			entityDiff.add(c.getEntityContainer());
		}
	}

	
	private static Model executeConstruct(ISparqlExecutor graphDAO, Collection<String> queries)
		throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		int i = 1;
		for(String query : queries) {
			logger.info("Executing query " + (i++) + "/" + queries.size());
			
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
		}
		catch(Exception e1) {
			return null;
		}
	}
	
	
	private static Pattern rdfSeqPattern = Pattern.compile(RDF.getURI() + "_(\\d+)$");
	
	public static Integer tryParseSeqPredicate(Resource res)
	{
		String pred = res.toString();
		Matcher m = rdfSeqPattern.matcher(pred);
		if(m.find()) {
			String indexStr = m.group(1);
			Integer index = Integer.parseInt(indexStr);
			
			return index;
		}
		
		return null;
	}
	
	
	/**
	 * Scans the given models for triples having a rdf:_n predicate and
	 * returns a map of subject-> index -> object
	 * 
	 * @param model
	 * @return
	 */
	/*
	public Map<Resource, SortedMap<Integer, RDFNode>> processSeq(Model model) {
		Map<Resource, SortedMap<Integer, RDFNode>> result = new HashMap<Resource, SortedMap<Integer, RDFNode>>();
		
		
		for(Resource subject : model.listSubjects().toSet()) {
			SortedMap<Integer, RDFNode> part = processSeq(subject, model);
			result.put(subject, part);
		}
		
		return result;
	}*/
	// FIXME: Clearify semantics: If a resource does not have a seq associated
	// should it appear in the result?
	// Currently the answer is "no" (therefore only resources with seqs are
	// returned.
	// However, actually all resources that are "a rdf:Seq" should be in the map
	// (i guess)
	public void processSeq(Map<Resource, TreeMap<Integer, RDFNode>> result, Model model) {
		//Map<Resource, SortedMap<Integer, RDFNode>> result = new HashMap<Resource, SortedMap<Integer, RDFNode>>();
		
		
		for(Resource subject : model.listSubjects().toSet()) {
			TreeMap<Integer, RDFNode> part = processSeq(subject, model);
			if(!part.isEmpty())
				result.put(subject, part);
		}
		
		//return result;
	}

	
	
	private TreeMap<Integer, RDFNode> processSeq(Resource res, Model model)
	{
		TreeMap<Integer, RDFNode> indexToObject = new TreeMap<Integer, RDFNode>();
		StmtIterator it =  model.listStatements(res, null, (RDFNode)null);
		try {
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				String pred = stmt.getPredicate().toString();
				Matcher m = rdfSeqPattern.matcher(pred);
				if(m.find()) {
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
		for(int i = 0; i < (parts.length / 2); ++i) {
			result.add(parts[2 * i] + " " + parts[2 * i + 1]);
		}

		return result;
	}

	
	private static Pattern pointPattern = Pattern.compile("POINT\\s+\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
	
	private static String tryParsePoint(String value)
	{
		Matcher m = pointPattern.matcher(value);

		return m.find()
			? m.group(1)
			: null;
	}
	
	private void populatePointPosMappingVirtuoso(Model model, Map<Resource, String> nodeToPos)
	{
		StmtIterator it = model.listStatements(null, GeoRSS.geo, (RDFNode)null);
		try {
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				String value = stmt.getLiteral().getValue().toString();
				String pointStr = tryParsePoint(value);
				if(pointStr == null)
					continue;
				
				
				nodeToPos.put(stmt.getSubject(), pointStr);
			}
		} finally {
			it.close();
		}
	}
	
	private void populatePointPosMappingGeoRSS(Model model, Map<Resource, String> nodeToPos)
	{
		StmtIterator it = model.listStatements(null, GeoRSS.point, (RDFNode)null);
		try {
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				String value = stmt.getLiteral().getValue().toString();
				nodeToPos.put(stmt.getSubject(), value);
			}
		} finally {
			it.close();
		}
	}
	
	
	// Map<Resource, List<Resource>> wayToNodes
	private void populatePointPosMapping(StmtIterator it, Model lookupModel, Map<Resource, String> nodeToPos)
	{
		try {
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				String value = stmt.getObject().as(Literal.class).getValue().toString();
				
				List<String> positions = tryParseGeoRRSPointList(value);

				
				Resource wayNodeRes = ResourceFactory.createResource(stmt.getSubject().toString() + "/nodes");
				SortedMap<Integer, RDFNode> seq = processSeq(wayNodeRes, lookupModel);
				
				//List<Resource> wayNodes = new ArrayList<Resource>();
				//wayToNodes.put(wayNodeRes, wayNodes);
				
				int i = 1;
				for(Map.Entry<Integer, RDFNode> entry : seq.entrySet()) {
					if(entry.getKey() != i) {
						logger.warn("WayNodes out of sync");
					}
										
					Resource wayNode = entry.getValue().as(Resource.class);
					
					nodeToPos.put(wayNode, positions.get(i));
					//wayNodes.add(wayNode);
					
					
					++i;
				}
				
			}
		} finally {
			it.close();
		}
	}
	
	/**
	 * transforms a waynode resource to the corresponding way resource
	 * 
	 * @param res
	 * @return
	 */
	private static Resource wayNodeToWay(Resource res)
	{
		String str = res.getURI().toString();
		String suffix = "/nodes";
		if(!str.endsWith(suffix))
			return null;
		
		str = str.substring(0, str.length() - suffix.length());
		
		return ResourceFactory.createResource(str);
	}

	private static Resource wayToWayNode(Resource res)
	{
		return ResourceFactory.createResource(res.getURI().toString() + "/nodes");
	}
		
	
	/**
	 * Some issues i recently noticed (as of 20 sept 2010):
	 * .) The nodeToPos map is only populated from the diff,
	 *    However, all positions that are avaiable in the new set should be placed there.
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
		logger.info("Processing entities. Added/removed = " + inDiff.getAdded().size() + "/" + inDiff.getRemoved().size());
		long start = System.nanoTime();
		
		List<List<EntityContainer>> parts;

		Transformer<EntityContainer, Entity> entityExtractor = new Transformer<EntityContainer, Entity>() {
			@Override
			public Entity transform(EntityContainer input)
			{
				return input.getEntity();
			}
		};
		
		String graphName = mainGraphName;


		
		List<String> mainGraphQueries = GraphDAORDFEntityDAO.constructQuery(
				TransformIterable.transformedView(inDiff.getRemoved(), entityExtractor),
				vocab,
				graphName,
				1024);
		
		// Fetch all data for current entities
		Model oldModel = executeConstruct(graphDAO, mainGraphQueries);		

		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for deleted entities, " + oldModel.size() + " triples fetched");
		
		
		List<String> mainGraphQueries2 = GraphDAORDFEntityDAO.constructQuery(
				TransformIterable.transformedView(inDiff.getAdded(), entityExtractor),
				vocab,
				graphName,
				1024);
		
		// Fetch all data for current entities
		Model oldModel2 = executeConstruct(graphDAO, mainGraphQueries2);		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for added entities, " + oldModel2.size() + " triples fetched");

		oldModel.add(oldModel2);
		
		Model newModel = ModelFactory.createDefaultModel();
		
		//processBatch(TransformIterable.transformedView(inDiff.getRemoved(), entityExtractor), ChangeAction.Delete, mainGraphName, oldModel, newModel);
		transformToModel(TransformIterable.transformedView(inDiff.getAdded(), entityExtractor), mainGraphName, newModel);		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed RDF transformation of entities");
		
		
		
		
		// Based on the old model, it is possible to assign positions to nodes
		// as follows:
		// For each way (in the old model) retrieve the georss:line / polygon triples
		// Also retrieve all node memberships (they are also always completly part of the old model)
		// We parse the string, and map all points to the corresponding nodes
		
		
		// Afterwards, we scan the diff, whether any of the positions of the nodes were changed
		// and update our node-position mapping accordingly
		Map<Resource, String> nodeToPos = new HashMap<Resource, String>();
		//Map<Resource, List<Resource>> wayToNodes = new HashMap<Resource, List<Resource>>();
		
		// Deduce PointPos mappings based on georss:(line|polygon)s from the old model
		
		// GAH! Do we need to the following two calls?
		// Yes: We need them in order to create way polygons for ways that don't
		// have this triple in the old model.
		// In that case we can't patch an existing point list, but we rather
		// have to create it anew
		//populatePointPosMapping(oldModel.listStatements(null, GeoRSS.line, (RDFNode)null), oldModel, nodeToPos);
		//populatePointPosMapping(oldModel.listStatements(null, GeoRSS.polygon, (RDFNode)null), oldModel, nodeToPos);
				
		IDiff<Model> diff = diff(oldModel, newModel);		
		
		outDiff.remove(diff.getRemoved());
		outDiff.add(diff.getAdded());


		// Update PointPos mappings based on the diff to the new model
		// FIXME Should we scan the "remove"-model first and remove corresponding
		// point-pos mappings, so we can detect dangling references? Otherwise
		// the old positions would still be referencable.
		populatePointPosMappingGeoRSS(diff.getAdded(), nodeToPos);
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed populating point-position mapping based on old model, indexed " +  nodeToPos.size() + " mappings");
			
		
		
		// for each repositioned node, determine the ways it belongs to
		// FIXME: The following statement is probably true:
		// Nodes that were created (rather than modified) can only belong
		// to ways that are part of the changeset.
		// Therefore, there is no need to check for way-memberships of these
		// nodes.
		// HOWEVER: With this osmosis pipe architecture, we probably don't
		// know where a changeset starts and ends.
		// But STILL: Probably no node is referenced before it is created.
		//logger.info(nodeToPos.keySet() + " nodes were created or repositioned");

		// This model is basically a map containing the changes that need to be injected into the
		// ways polygon: way -> index -> update
		Model changeSet = selectWayNodesByNodes(graphDAO, graphName, nodeToPos.keySet());
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching ways for repositioned nodes, " + changeSet.size() + " waynodes affected");

		// Now that we know which nodes changed positions,
		// index the positions of all nodes
		populatePointPosMappingGeoRSS(newModel, nodeToPos);

		
		// Mixin the changes from diff.added
		StmtIterator it = diff.getAdded().listStatements();
		try {
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				String subject = stmt.getSubject().toString();
				if(subject.endsWith("/nodes"))
					changeSet.add(stmt);
			}
		} finally {
			it.close();
		}
		
		// From this model retrieve the set of wayNode resources
		Set<Resource> wayNodes = changeSet.listSubjects().toSet();
		Set<Resource> ways = new HashSet<Resource>();
				
		for(Resource wayNode : wayNodes) {
			ways.add(wayNodeToWay(wayNode));
		}
		
		// Retrieve the positions of those nodes of which we don't have it yet

		
		
		// Patch ways
		// Index all newly updated ways - we use that in order to determine
		// whether a way is a line or a polygon
		Map<Resource, TreeMap<Integer, RDFNode>> ws = new HashMap<Resource, TreeMap<Integer, RDFNode>>();
		processSeq(ws, newModel);

		// Determine nodes for which we yet have to fetch the positions
		Set<Resource> unindexedNodes = new HashSet<Resource>(); 
		for(SortedMap<Integer, RDFNode> indexToNode : ws.values()) {
			for(RDFNode node : indexToNode.values()) {
				if(!(node instanceof Resource)) {
					logger.error("Not a node: " + node);
				}
				
				if(!nodeToPos.containsKey(node))
					unindexedNodes.add((Resource)node);
			}
		}
		Map<Resource, RDFNode> mappings = fetchNodePositions(graphDAO, graphName, unindexedNodes);
		
		for(Map.Entry<Resource, RDFNode> mapping : mappings.entrySet()) {
			nodeToPos.put(mapping.getKey(), mapping.getValue().toString());
		}
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching positions for " + mappings.size() + " additional nodes");

		
		
		// Load all georss:(line|polygon) strings of the old model into a new one
		Model georss = ModelFactory.createDefaultModel();
		georss.add(oldModel.listStatements(null, GeoRSS.line, (RDFNode)null));
		georss.add(oldModel.listStatements(null, GeoRSS.polygon, (RDFNode)null));
		
		// Load all georss:(line|polygon) strings of ways with repositioned nodes
		// Some of that data may already be part of the old model 
		Set<Resource> lookupWays = new HashSet<Resource>(ways);
		Set<Resource> alreadyHaveGeoRSS = georss.listSubjects().toSet(); 
		lookupWays.removeAll(alreadyHaveGeoRSS);
		
		// FIXME: If the positions of all nodes of a way are already known,
		// there is no need to fetch its georss, as it can be computed
		// directly.
		Model georssOfWaysWithRepositionedPoints = constructGeoRSSLinePolygon(graphDAO, graphName, lookupWays);
		georss.add(georssOfWaysWithRepositionedPoints);
		
		
		// Note We need to take the removed triples into account when patching
		// the georss point list for that case that n nodes are removed
		// from the end of a way.
		for(Resource way : ways) {
			Resource wayNode = wayToWayNode(way);
			
			for(Statement base : georss.listStatements(way, null, (RDFNode) null).toList()) {
				String geoStr = base.getLiteral().getValue().toString();
				ArrayList<String> positions = tryParseGeoRRSPointList(geoStr);
				
				int highestUpdateIndex = -1;
				
				SortedMap<Integer, RDFNode> fixes = processSeq(wayNode, changeSet);
				//for(Statement fix : changeSet.listStatements(wayNode, null, (RDFNode)null).toList()) {
				for(Map.Entry<Integer, RDFNode> fix : fixes.entrySet()) {
					int index = fix.getKey() - 1;
					//Integer index = tryParseSeqPredicate(fix.getPredicate());
					//if(index == null)
						//continue;
					
					highestUpdateIndex = Math.max(highestUpdateIndex, index);
					
					//String value = fix.getLiteral().getValue().toString().trim();
					String value = nodeToPos.get(fix.getValue());
					if(value == null) {
						logger.warn("Cannot patch way " + way + " because its point list references node " + fix.getValue() + " for which no position was found");
						positions = null;
						break;
					}
					
					if(index >= positions.size()) {
						while(index > positions.size()) {
							logger.warn("Adding dummy node at index " + positions.size() + " for wayNode " + wayNode);
							positions.add("-180.0 -90.0");
						}
						
						positions.add(value);
						
					}
					else {					
						positions.set(index, value);
					}
				}
				
				
				if(positions != null) {
					// Check against diff.removed whether we need to remove some positions
					
					for(Statement check : diff.getRemoved().listStatements(wayNode, null, (RDFNode)null).toList()) {
						Integer index = tryParseSeqPredicate(check.getPredicate());
						if(index == null)
							continue;
						
						// Remove all indexes that are above highestUpdateIndex
						if(index > highestUpdateIndex)
							positions.remove(index); // FIXME This does array shifting
					}

					Property predicate = base.getPredicate(); 
					
					// FIXME: A line may have become a polygon
					Resource wayNode2 = wayToWayNode(base.getSubject());
					TreeMap<Integer, RDFNode> indexToNode = ws.get(wayNode2);
					if(indexToNode != null && !indexToNode.isEmpty()) {
						predicate = indexToNode.firstEntry().getValue().equals(indexToNode.lastEntry().getValue())
								? GeoRSS.polygon
								: GeoRSS.line;
					}
					
					
					// Check whether the way (and therefore everything of it)
					// is part of the newModel
					
					
					String newValue = StringUtil.implode(" ", positions);

					outDiff.getRemoved().add(base); 
					outDiff.getAdded().add(base.getSubject(), predicate, newValue);
					newModel.add(base.getSubject(), predicate, newValue);
				}
				
			}
			
		}

		// If there are ways left that did not have georss (line|polygon)
		// triple, generate that for them.
		// Note: We are not creating a copy of the ways set because this
		// is the last time we need that set
		Set<Resource> waysWithoutGeoRSS = ways;
		waysWithoutGeoRSS.removeAll(georss.listSubjects().toSet());
		
		if(!waysWithoutGeoRSS.isEmpty()) {
			
			List<Resource> wayNodes2 = new ArrayList<Resource>();
			for(Resource res : waysWithoutGeoRSS) {
				 wayNodes2.add(wayToWayNode(res));
			}
			
			logger.info("Creating GeoRSS for " + waysWithoutGeoRSS.size() + " ways");
			Model wayNodesToNodes = selectNodesByWayNodes(graphDAO, graphName, wayNodes2);
			
			// Create the set of nodes of which we do not have positions
			//Set<Resource> 
			unindexedNodes = new HashSet<Resource>();
			for(RDFNode node : wayNodesToNodes.listObjects().toSet()) {
				if(node.isResource()
						&& node.toString().startsWith("http://linkedgeodata.org/node")
						&& !nodeToPos.containsKey(node)) {
					
					unindexedNodes.add((Resource)node);
				}
			}
			
			populatePointPosMappingGeoRSS(wayNodesToNodes, nodeToPos);

			// finally fetch the positions
			//Map<Resource, RDFNode>
			mappings = fetchNodePositions(graphDAO, graphName, unindexedNodes);
			
			for(Map.Entry<Resource, RDFNode> mapping : mappings.entrySet()) {
				nodeToPos.put(mapping.getKey(), mapping.getValue().toString());
			}
			
			
			//Map<Resource, SortedMap<Integer, RDFNode>> ws = new HashMap<Resource, SortedMap<Integer, RDFNode>>();
			processSeq(ws, wayNodesToNodes);
			//processSeq(ws, newModel);
			
			// Finally, for each way generate the georss
			for(Map.Entry<Resource, TreeMap<Integer, RDFNode>> w : ws.entrySet()) {
				List<String> geoRSSParts = new ArrayList<String>();
				
				int i = 1;
				for(Map.Entry<Integer, RDFNode> indexToNode : w.getValue().entrySet()) {
					if(indexToNode.getKey() != (i++)) {
						logger.warn("Index out of sync: " + w);
					}
					
					geoRSSParts.add(nodeToPos.get(indexToNode.getValue()));
				}
				
				String geoRSS = StringUtil.implode(" ", geoRSSParts);
				
				if(w.getValue().isEmpty())
					continue;
				
				Property predicate = w.getValue().firstEntry().getValue().equals(w.getValue().lastEntry().getValue())
					? GeoRSS.polygon
					: GeoRSS.line;
				
				outDiff.getAdded().add(wayNodeToWay(w.getKey()), predicate, geoRSS); 
			}
			
			
			//Map<Resource, SortedMap<Integer, RDFNode>> index = processSeq(wayNodesToNodes);
		}
		
		
		// Finally: In the diff: Remove the georss:line/polygon triples
		// from the "remove" set if the resource exists in the new set but
		// doesn't have that triple
		ExtendedIterator<Statement> x =
			outDiff.getRemoved().listStatements(null, GeoRSS.line, (RDFNode)null).andThen(
				outDiff.getRemoved().listStatements(null, GeoRSS.polygon, (RDFNode)null));
		
		while(x.hasNext()) {
			Statement stmt = x.next();
			
			if(newModel.contains(stmt.getSubject(), null) &&
					!(	newModel.contains(stmt.getSubject(), GeoRSS.line)
							|| newModel.contains(stmt.getSubject(), GeoRSS.polygon))) {
				x.remove();
			}
		}
		
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed processing of entities");
	}

	
	/*
	public static boolean isTaglessNode(Entity entity)
	{
		if(!(entity instanceof Node))
			return false;
		
		return entity.getTags().isEmpty();
	}
	*/

	
	private void transformToModel(Iterable<Entity> entityBatch, String graphName, Model outModel)
		throws Exception
	{

		/*
		Model newModel = ModelFactory.createDefaultModel();
		for(Entity entity : entityBatch) {
			if(!isTaglessNode(entity))
				entityTransformer.transform(newModel, entity);
		} 
		*/
		
		Model newModel = ModelFactory.createDefaultModel();
		for(Entity entity : entityBatch) {
			// Tagless nodes are put into a separate graph
			// As these nodes are not very informative, but are required
			// in order to lookup ways

			if(entity instanceof Node) {
				// Only generate a virtuoso specific triple in order to
				// to relate the node's uri to a position.
				Node node = (Node)entity;
				Resource subject = vocab.createResource(entity);


				// FIXME Meh, the Open Source version of virtuoso has no special
				// geo support, and yields errors when attempting to insert
				// geoms (rather then silently accept them)
				// Add a switch to toggle special virtuoso stuff on and off 
				//SimpleNodeToRDFTransformer.generateVirtusoPosition(newNodeModel, subject, node);
				SimpleNodeToRDFTransformer.generateGeoRSS(newModel, subject, node);
				
				if(!node.getTags().isEmpty()) {
					entityTransformer.transform(newModel, entity);
				}
				
			} else {
				entityTransformer.transform(newModel, entity);
			}
		}
		
		
		// Virtuoso-specific transforms for the triples that were added
		postProcessTransformer.transform(outModel, newModel);
		//System.out.println(ModelUtil.toString(added));
	}
		
	
	@Override
	public void complete()
	{
		//logger.info(this.getClass() + " completed");
		try {
			mainGraphDiff = new RDFDiff();
			process(entityDiff, mainGraphDiff, maxEntityBatchSize);
			entityDiff.clear();
		} catch(Exception e) {
			logger.error("An error occurred at the completion phase of a task", e);
		}
	}
	
	
	public RDFDiff getMainGraphDiff()
	{
		return mainGraphDiff;
	}
	
	public RDFDiff getNodeGraphDiff()
	{
		return nodeGraphDiff;
	}
	

	@Override
	public void release()
	{
		mainGraphDiff = null;
	}
	
	
	public static Map<Resource, RDFNode> fetchNodePositions(ISparqlExecutor graphDAO, String graphName, Set<Resource> nodes)
		throws Exception
	{
		Map<Resource, RDFNode> result = new HashMap<Resource, RDFNode>();
		
		if(nodes.isEmpty())
			return result;
		
		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		for(List<Resource> chunk : chunks) {
			String query = "Select ?n ?o From <" + graphName + "> { ?n <" + GeoRSS.point + "> ?o . Filter(?n In (<" + StringUtil.implode(">,<", chunk) + ">)) . }";
			
			// FIXME Make executeConstruct accept the output model as an parameter 
			//Model tmp = graphDAO.executeConstruct(query);
			List<QuerySolution> qs = graphDAO.executeSelect(query);
			for(QuerySolution q : qs) {
				result.put(q.getResource("n"), q.get("o"));
			}
		}
		
		return result;		
	}
	
	
	public static Model constructGeoRSSLinePolygon(ISparqlExecutor graphDAO, String graphName, Set<Resource> ways)
		throws Exception
	{
		if(ways.isEmpty())
			return ModelFactory.createDefaultModel();
		
		String query = "Construct {?s ?p ?o . } From <" + graphName + "> { ?s ?p ?o . Filter(?s In (<" + StringUtil.implode(">,<", ways) + ">) && ?p In (<" + GeoRSS.point.toString() + "> || <" + GeoRSS.polygon + ">)) . }";
		Model result = graphDAO.executeConstruct(query);
		
		return result;
	}
	
	public static Model selectWayNodesByNodes(ISparqlExecutor graphDAO, String graphName, Collection<Resource> nodes)
		throws Exception
	{
		if(nodes.isEmpty())
			return ModelFactory.createDefaultModel();
		
		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for(List<Resource> chunk : chunks) {
			String query = "Construct { ?wn ?p ?n } From <" + graphName + "> { ?wn ?p ?n . Filter(?n In (<" + StringUtil.implode(">,<", chunk) + ">)) . }";
			
			// FIXME Make executeConstruct accept the output model as an parameter 
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}
		
		return result;
	}

	/**
	 * Note: This is just looking up triples by their certain objects.
	 * So this method could be generalized
	 */
	public static Model selectNodesByWayNodes(ISparqlExecutor graphDAO, String graphName, Collection<Resource> wayNodes)
		throws Exception
	{
		if(wayNodes.isEmpty())
			return ModelFactory.createDefaultModel();
		
		List<List<Resource>> chunks = CollectionUtils.chunk(wayNodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for(List<Resource> chunk : chunks) {
			String query = "Construct { ?wn ?p ?n } From <" + graphName + "> { ?wn ?p ?n . Filter(?wn In (<" + StringUtil.implode(">,<", chunk) + ">)) . }";
			
			// FIXME Make executeConstruct accept the output model as an parameter 
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}
		
		return result;
	}

}




/*
 * Following code can be removed as soon as the plugin is working - 
 * because then its definitely not needed anymore
private static String constructBySubject(String iri, String graphName)
{
	String fromPart = (graphName != null)
		? "From <" + graphName + "> "
		: "";

	String result =
		"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + iri + ">) . }";
	
	return result;
}
		
		List<QuerySolution> rs = graphDAO.executeSelect(query);
		for(QuerySolution q : rs) {
			Resource wayNode = q.getResource("wn");
		
			String str = wayNode.getURI().toString();
			if(!str.endsWith("\nodes")) {
				throw new RuntimeException("A way node did not end with /nodes; uri = " + str);
			}
			
			str = str.substring(0, str.length() - 6);
			
			
			Resource way = ResourceFactory.createResource(str);
			
			result.add(way);
		}

*/

/*
private static String constructNodeModelQuery(ILGDVocab vocab, long nodeId, String graphName)
{
	String nodeIRI = vocab.createNIRNodeURI(nodeId);
	
	String fromPart = (graphName != null)
		? "From <" + graphName + "> "
				: "";

	String result =
		"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + nodeIRI + ">) . }";

return result;
}
*/	

/*
private static String constructQuery(final ILGDVocab vocab, Iterable<Long> nodeIds, Iterable<Long> wayIds, String graphName)
{
	if(!wayIds.iterator().hasNext())
		return "";

	String resources = "";
	
	resources += StringUtil.implode(",",
			new TransformIterable<Long, String>(
					nodeIds,
					new Transformer<Long, String>() {
						@Override
						public String transform(Long nodeId)
						{
							return vocab.createNIRNodeURI(nodeId);
						}
					}));

	resources += StringUtil.implode(",",
			new TransformIterable<Long, String>(
					nodeIds,
					new Transformer<Long, String>() {
						@Override
						public String transform(Long wayId)
						{
							return
								"<" + vocab.createNIRWayURI(wayId) + ">,<" +
								vocab.getHasNodesResource(wayId).toString() + ">";
						}
					}));
	
		
	String fromPart = (graphName != null)
		? "From <" + graphName + "> "
		: "";

	String result =
		"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";

	return result;
}
*/

/*
private static Model getBySubject(ISparqlExecutor graphDAO, String iri, String graphName)
	throws Exception
{
	String query = constructBySubject(iri, graphName);
	
	//logger.info("Created query: " + query);
	
	Model model = graphDAO.executeConstruct(query);
	
	return model;
}*/

/*
if(timeStamp == null)
	timeStamp = new Date();

Date now = new Date();
*/

/*
if(timeStamp == null)
	timeStamp = entity.getTimestamp(); 

Date now = entity.getTimestamp();

if(timeStamp.getTime() > now.getTime()) {
	logger.warn("Warning: Entities arriving out of order: " + timeStamp + " > " + now);
}*/


/*
if(timeStamp == null)
	timeStamp = new Date();

Date now = new Date();

if(now.getTime() - timeStamp.getTime() > entityDiffTimeSpan) {
	process(timelyDiff);
	
	timeStamp = now;
	
	entities.clear();
}

*
		
		
		// Process all affected ways in order to update their linestrings/polygons

		// Process affected ways
		// Whenever the shape of a way changes, it is very likely that all
		// nodes are already in the same changeset. However, if the way
		// is connected to already existing nodes, these nodes won't appear in
		// the changeset.
		String query = "Select ?w ?i ?p {:w ldo:hasNodes ?wn . ?wn ?i ?n . Optional { ?n georss:point ?p }. }";
		
		
		Model newMainModel = ModelFactory.createDefaultModel();
		List<Way> ways = CollectionUtils.filterByType(inDiff.getAdded(), Way.class);
		for(Way way : ways) {
			Resource res = vocab.getHasNodesResource(way.getId());
			StmtIterator itWayNode = newMainModel.listStatements(res, vocab.getHasNodesPred(), (RDFNode)null);
			try {
				Statement stmt = itWayNode.next();			
				
				StmtIterator itNode = newMainModel.listLiteralStatements(stmt.getObject(), , object)
				
			} finally {
				itWayNode.close();
			}
			
			
			//outDiff.getAdded()
			
		}
		
		
		
		// Each node may affect zero or more ways
		// Retrieve the ids of those ways that have not already been updated.
		List<Node> nodes = CollectionUtils.filterByType(inDiff.getAdded(), Node.class);
		String query = "Select ?w ?i ?n {?w ldo:hasNodes ?wn . ?wn ?i ?n . ?n georss:point ?p . }";
		
		
		
		//for(Entity inDiff.getAdded()
		
		// Update the node-way and node-position map
				
*/
