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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.aksw.commons.jena.ModelSetView;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.Predicate;
import org.apache.log4j.Logger;
import org.jboss.cache.util.DeltaBulkMap;
import org.jboss.cache.util.SetDiff;
import org.jboss.cache.util.SetMultiHashMap;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.core.vocab.WGS84Pos;
import org.linkedgeodata.dao.nodestore.RDFNodePositionDAO;
import org.linkedgeodata.osm.mapping.impl.SimpleNodeToRDFTransformer;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.sparql.cache.DeltaGraph;
import org.linkedgeodata.util.sparql.cache.IGraph;
import org.linkedgeodata.util.sparql.cache.SparqlEndpointFilteredGraph;
import org.linkedgeodata.util.sparql.cache.TripleCacheIndexImpl;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;



public class OptimizedDiffUpdateStrategy
		implements IUpdateStrategy
{
	private static final Logger			logger					= Logger.getLogger(IUpdateStrategy.class);

	private ILGDVocab					vocab;
	private ITransformer<Entity, Model>	entityTransformer;
	//private ISparqlExecutor				sparqlEndpoint;
	
	private DeltaGraph deltaGraph;
	
	
	private IGraph linePolygonGraph;
	private IGraph pointGraph;
	
	

	private DeltaBulkMap<Long, Point2D>			nodePositionDAO;
	private ITransformer<Model, Model>	postProcessTransformer	= new VirtuosoStatementNormalizer();

	// The purpose of the following two attributes is to hold the step-result
	private RDFDiff						mainGraphDiff;
	private TreeSetDiff<Node> nodeDiff = new TreeSetDiff<Node>();
	
	
	private Predicate<Tag> tagRelevanceFilter;
	
	// Number of entities that should be processed as a batch
	//private int	maxEntityBatchSize	= 500;

	
	/**
	 * Entities pending for becoming processed
	 * 
	 */
	private Set<Node> createdNodes = new HashSet<Node>();
	private Set<Node> modifiedNodes = new HashSet<Node>();
	private Set<Node> deletedNodes = new HashSet<Node>();
	
	private Set<Way> createdWays = new HashSet<Way>();
	private Set<Way> modifiedWays = new HashSet<Way>();
	private Set<Way> deletedWays = new HashSet<Way>();
	
	Set<? extends Entity> createdEntities = Sets.union(createdNodes, createdWays);
	Set<? extends Entity> modifiedEntities = Sets.union(modifiedNodes, modifiedWays);
	Set<? extends Entity> deletedEntities = Sets.union(deletedNodes, deletedWays);
	
	/**
	 * Caches
	 * 
	 * We keep track of:
	 * .) dependencies      Multimap<Node, Way>
	 * .) nodePositions     Map<Node, Point2D>
	 * 
	 */
	
	

	/**
	 * Constructor
	 * @throws Exception 
	 * 
	 */
	public OptimizedDiffUpdateStrategy(
			ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			DeltaGraph deltaGraph,
			DeltaBulkMap<Long, Point2D> nodePositionDAO,
			Predicate<Tag> tagRelevanceFilter) throws Exception
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.deltaGraph = deltaGraph;
		//this.sparqlEndpoint = sparqlEndpoint;
		this.nodePositionDAO = nodePositionDAO;
		// this.nodeGraphName = nodeGraphName;
		this.tagRelevanceFilter = tagRelevanceFilter;
		
		this.linePolygonGraph = ((SparqlEndpointFilteredGraph)deltaGraph.getBaseGraph()).createSubGraph(LgdSparqlTasks.toString(GeoRSS.line,  GeoRSS.polygon));
		this.pointGraph = ((SparqlEndpointFilteredGraph)deltaGraph.getBaseGraph()).createSubGraph(LgdSparqlTasks.toString(GeoRSS.point));

		TripleCacheIndexImpl.create(linePolygonGraph, 100000, 10000, 10000, new int[]{0}); 
		TripleCacheIndexImpl.create(pointGraph, 100000, 10000, 10000, new int[]{0}); 
	
	}


	public void clear()
	{
		createdNodes.clear();
		modifiedNodes.clear();
		deletedNodes.clear();
		
		createdWays.clear();
		modifiedWays.clear();
		deletedWays.clear();
		/*
		createdEntities.clear();
		modifiedEntities.clear();
		deletedEntities.clear();
		*/
	}

	
	public static <T extends Entity> void addEntity(ChangeAction ca, T entity, 
			Set<T> created, Set<T> modified, Set<T> deleted)
	{
		// If an equal entity was already created, we can treat it as non 
		// existent (as nothing was persisted yet)
		created.remove(entity);

		// If an entity became deleted but the same becomes recreated (shouldn't
		// happen?)
		// it must be treated as modified - on other words:
		// Subsequent recreates must be treated as modified
		boolean didRemove = modified.remove(entity)
				|| deleted.remove(entity);

		switch (ca) {
		case Create:
			if (didRemove)
				modified.add(entity);
			else
				created.add(entity);
			break;
		case Modify:
			modified.add(entity);
			break;
		case Delete:
			deleted.add(entity);
			break;
		}
	}

	@Override
	public void process(ChangeContainer c)
	{
		Entity entity = c.getEntityContainer().getEntity();
		
		if(entity instanceof Node) {
			addEntity(c.getAction(), (Node)entity, createdNodes, modifiedNodes, deletedNodes);
		}
		else if(entity instanceof Way) {
			addEntity(c.getAction(), (Way)entity, createdWays, modifiedWays, deletedWays);
		}		
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


	private static void createMinimalStatements(Iterable<? extends Entity> entities,
			Model model, ILGDVocab vocab)
	{		
		for (Entity entity : entities) {
			if (entity instanceof Node) {
				Node node = (Node) entity;
				Resource subject = vocab.createResource(node);
				SimpleNodeToRDFTransformer.generateGeoRSS(model, subject, node);
			}
		}
	}


	/**
	 * Advances the state of this object; ie processes all pending entities.  
	 * Afterwards, the diffs can be taken using getMainDiff() and getNodeDiff().
	 * 
	 * @param inDiff
	 * @param outDiff
	 * @param batchSize
	 * @throws Exception
	 */
	private void step()
		throws Exception
	{
		logger.info("Processing entities. Created/Modified/Deleted = " + createdEntities.size() + "/" + modifiedEntities.size() + "/" + deletedEntities.size());
		long start = System.nanoTime();

		//Set<Resource> createdNodesResources = GraphDAORDFEntityDAO.getInvolvedResources(createdNodes, vocab);
		
		// Clean up entities
		// Entities that were created but are irrelevant are simply ignored
		Iterator<Way> wayIt = createdWays.iterator();
		while(wayIt.hasNext()) {
			Way e = wayIt.next();
			
			if(reject(e, tagRelevanceFilter)) {
				wayIt.remove();
			}
		}
		
		// Ways that are modified but irrelevant become deleted
		wayIt = modifiedWays.iterator();
		while(wayIt.hasNext()) {
			Way e = wayIt.next();
			
			if(reject(e, tagRelevanceFilter)) {
				wayIt.remove();
				deletedWays.add((Way)e);
			}
		}
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed cleaning entities. Created/Modified/Deleted = " + createdEntities.size() + "/" + modifiedEntities.size() + "/" + deletedEntities.size()); 
		
		
		
		Set<Resource> createdResources = GraphDAORDFEntityDAO.getInvolvedResources(createdEntities, vocab);
		Set<Resource> modifiedResources = GraphDAORDFEntityDAO.getInvolvedResources(modifiedEntities, vocab);
		Set<Resource> deletedResources = GraphDAORDFEntityDAO.getInvolvedResources(deletedEntities, vocab);
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed converting entities to resources."); 
		
		Set<Resource> deletedOrModifiedResources = Sets.union(deletedResources, modifiedResources);
		Set<Node> createdOrModifiedNodes = Sets.union(createdNodes, modifiedNodes);
		Set<Entity> createdOrModifiedEntities = Sets.union(createdEntities, modifiedEntities);
		
		
		// new: Model oldMainModel = GraphUtils.findBySubject(mainGraph, deletedOrModifiedResources);		
		Model oldMainModel = LgdSparqlTasks.fetchStatementsBySubject(deltaGraph, deletedOrModifiedResources, 512);				
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for " + deletedOrModifiedResources.size() + " modified or deleted entities, " + oldMainModel.size() + " triples fetched");

		Model newMainModel = ModelFactory.createDefaultModel();
		Set<Entity> danglingEntities = transformToModel(createdOrModifiedEntities, newMainModel, tagRelevanceFilter);
		//Set<Resource> danglingResources = GraphDAORDFEntityDAO.getInvolvedResources(danglingEntities, vocab);
		Set<Resource> danglingResources = new HashSet<Resource>();
		for(Entity entity : danglingEntities) {
			Resource resource = vocab.createResource(entity);
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
		

		// Determine the set set of nodes that have been repositioned
		// These are nodes that have a position-related triple in the diff
		Map<Resource, String> repositionedNodes = LgdRdfUtils.createNodePosMapFromNodesGeoRSS(LgdRdfUtils.createModelFromStatements(mainDiff.getAdded()));
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed populating " +  repositionedNodes.size() + " repositioned nodes");
			
		// NOTE: Add an option to exclude newly created nodes here - as they can't affect
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
		
		
		//Model wayPatchSet = LgdSparqlTasks.selectWayNodesByNodes(sparqlEndpoint, mainDefaultGraphNames, repositionedNodes.keySet());
		Model wayPatchSet = LgdSparqlTasks.fetchStatementsByObject(deltaGraph, repositionedNodes.keySet(), 1024);
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching ways for " + repositionedNodes.keySet().size() + " repositioned nodes, " + wayPatchSet.size() + " waynodes affected");

		Map<Resource, TreeMap<Integer, RDFNode>> affectedWays = LgdRdfUtils.indexRdfMemberships(wayPatchSet);
		Map<Resource, TreeMap<Integer, RDFNode>> newWays = LgdRdfUtils.indexRdfMemberships(newMainModel);

		// If an affected way is also a new way, only treat it as a new one
		// (As only new ways undergo structural changes)
		for(Resource newWay : newWays.keySet())
			affectedWays.remove(newWay);
		
		// Create inferred node-pos mappings based on georss data in the old model
		Map<Resource, String> nodeToPos = LgdRdfUtils.createNodePosMapFromWays(oldMainModel, vocab);
		
		
		// Index the positions of all newly created/modified nodes
		// Note that we overwrite the inferred data with data from the new model
		Map<Resource, String> nodeToPosTmp = LgdRdfUtils.createNodePosMapFromEntities(createdOrModifiedEntities, vocab);
		nodeToPos.putAll(nodeToPosTmp);


		
		// Whenever a reference from a way to a node gets removed, the node
		// may become dangling,
		// unless it either
		//   a) is already a subject in the newMain graph
		//   b) is referenced by another way (this will be checked for in the dependencies section)
		MultiMap<Resource, Resource> lostNodeLinks = LgdRdfUtils.extractDependencies(LgdRdfUtils.createModelFromStatements(mainDiff.getRemoved()), vocab);
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
		dependencies.putAll(LgdRdfUtils.extractDependencies(newMainModel, vocab));
		
		// FIXME Do we have to remove dependencies based on the old model? I'd say no
		
		//undangledResources.addAll(dependencies.keySet());
		danglingResources.removeAll(dependencies.keySet());

		
		
		// UNb)
		// Note: We are selecting node references from the database,
		// Note: We do not have to lookup dangling resources that were created in this batch
		// however some of the references may just about to become deleted
		// FIXME Replace dangling resources with dangling nodes - as We might undangle ways here, although only nodes are undangled here
		Set<Resource> danglingResourceReferenceLookup = Sets.difference(danglingResources, createdResources);
		//Model resourceDependencyModel = LgdSparqlTasks.selectReferencedNodes(sparqlEndpoint, mainDefaultGraphNames, danglingResourceReferenceLookup);
		Model resourceDependencyModel = LgdSparqlTasks.fetchStatementsByObject(deltaGraph, danglingResourceReferenceLookup, 1024);
		resourceDependencyModel.remove(Lists.newArrayList(mainDiff.getRemoved()));
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching references for " + danglingResourceReferenceLookup.size() + " dangling resources");
		
		
		
		MultiMap<Resource, Resource> dependenciesTmp = LgdRdfUtils.extractDependencies(resourceDependencyModel, vocab); 
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
		for(Node node : deletedNodes)
			nodeDiff.getRemoved().add(node);
				
		// We need to retrieve everything of all modified items that are
		// dangling and add them to the old model, so that they get deleted
		// properly
		Set<Resource> danglingResourceDataLookup = Sets.intersection(danglingResources, modifiedResources);
		//Model danglingModel = LgdSparqlTasks.fetchStatementsBySubject(sparqlEndpoint, mainDefaultGraphNames, danglingResourceDataLookup, 1024);
		Model danglingModel = LgdSparqlTasks.fetchStatementsBySubject(deltaGraph, danglingResourceDataLookup, 1024);
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching data for " + danglingResourceDataLookup.size() + " modified dangling resources, " + danglingModel.size() + " triples fetched");
		oldMainModel.add(danglingModel);
		
		
		
		// The positions of all undangled nodes need to go into the main graph
		Model minimalStatementModel = ModelFactory.createDefaultModel();
		for(Entity entity : createdOrModifiedEntities) {
			Resource resource = vocab.createResource(entity);
			if(dependencies.keySet().contains(resource) && !newMainSubjects.contains(resource)) {
				
				createMinimalStatements(Collections.singleton(entity), minimalStatementModel, vocab);
			}
		}
		newMainModel.add(minimalStatementModel);


		// FIXME whenever a modified node goes into the main graph, it should be removed from the NodePositionGraph
		// However, actually this is only optional!

		

		// FIXME: Add all nodes to the node diff that did not go into the main graph
		// (Hm, but these are exactly all the dangling nodes, aren't they?)
		for(Node node : createdOrModifiedNodes) {
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
		//Map<Resource, RDFNode> mappings = LgdSparqlTasks.fetchNodePositions(sparqlEndpoint, mainDefaultGraphNames, unindexedNodes);
		//TODO Analyse: Actually we could always fetch node positions from nodeDao - can't we?
		Map<Resource, RDFNode> mappings = LgdSparqlTasks.fetchNodePositions(pointGraph, unindexedNodes, 1024);
		
		for(Map.Entry<Resource, RDFNode> mapping : mappings.entrySet()) {
			nodeToPos.put(mapping.getKey(), mapping.getValue().toString());
		}
		
		
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed fetching positions for " + mappings.size() + " additional nodes");


		unindexedNodes.remove(mappings.keySet());
		
		
		// Check the node store for additional node positions
		// For all unindexed nodes that do not have a position yet, try
		// to retrieve them from the nodePositionDAO
		Map<Resource, Point2D> nodePositionDAOLookups = RDFNodePositionDAO.getAll(nodePositionDAO, vocab, unindexedNodes);
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
		//Model georssOfAffectedWays = LgdSparqlTasks.constructGeoRSSLinePolygon(sparqlEndpoint, mainDefaultGraphNames, affectedWays.keySet());
		Model georssOfAffectedWays = LgdSparqlTasks.fetchStatementsBySubject(linePolygonGraph, affectedWays.keySet(), 1024);
			
		
		
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
			ArrayList<String> positions = LgdRdfUtils.tryParseGeoRRSPointList(geoStr);
							
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
				
				for(Statement check : LgdRdfUtils.createModelFromStatements(mainDiff.getRemoved()).listStatements(wayNode, null, (RDFNode)null).toList()) {
					Integer index = LgdRdfUtils.tryParseSeqPredicate(check.getPredicate());
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
		//outDiff.
		
		
		//outDiff.getAdded().add(Lists.newArrayList(mainDiff.getAdded()));
		//outDiff.getRemoved().add(Lists.newArrayList(mainDiff.getRemoved()));
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + " Completed processing of entities");
	}

	public DeltaGraph getDeltaGraph()
	{
		return deltaGraph;
	}
	
	
	
	public boolean reject(Entity entity, Predicate<Tag> relevanceFilter)
	{		
		// Skip irrelevant ways or ways with too many nodes 
		if(entity instanceof Way) {
			Way way = (Way)entity;

			return
				(way.getWayNodes().size() > 20) ||
				(!isRelevant(way, relevanceFilter));
		}
		
		return false;
	}
	
	public static boolean isRelevant(Entity entity, Predicate<Tag> relevanceFilter)
	{
		for(Tag tag : entity.getTags()) {
			if(relevanceFilter.evaluate(tag)) {
				return true;
			}
		}
		
		return false;
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
	 * 
	 * Entities without any relevant tag are ignored.
	 */
	private Set<Entity> transformToModel(
			Iterable<? extends Entity> entities, Model mainModel, Predicate<Tag> relevanceFilter) throws Exception
	{
		Set<Entity> result = new HashSet<Entity>();

		Model newModel = ModelFactory.createDefaultModel();
		for (Entity entity : entities) {

			// FIXME Doesn't work this way, as ways have at least two resources
			// So the hack is to simple remove any tagless items
			if (entity.getTags().isEmpty()) {
				result.add(entity);
				continue;
			}
			
			// Skip ways with too many nodes
			if(entity instanceof Way) {
				Way way = (Way)entity;
				if(way.getWayNodes().size() > 20) {
					//result.add(entity);
					continue;
				}
			}
			

			if(!isRelevant(entity, relevanceFilter)) {
				if(entity instanceof Node) {
					result.add(entity);
				}
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
			//process(null, mainGraphDiff, maxEntityBatchSize);
			step();
			
			System.out.println("LinePolygonGraph:");
			System.out.println(linePolygonGraph);
			System.out.println("PointGraph:");
			System.out.println(pointGraph);
			
			
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

	@Override
	public void release()
	{
		mainGraphDiff = null;
		nodeDiff = null;
	}
}

