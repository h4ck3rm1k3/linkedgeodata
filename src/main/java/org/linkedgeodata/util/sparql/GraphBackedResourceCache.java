package org.linkedgeodata.util.sparql;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.osm.osmosis.plugins.RDFDiff;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.collections.CacheSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


class ResourceComparator
	implements Comparator<Resource>
{
	@Override
	public int compare(Resource a, Resource b)
	{
		return a.toString().compareTo(b.toString());
	}
}


/**
 * This class is not finished yet.
 * 
 * Some thoughts: It would be cool to have a "construct cache:"
 * 
 * E.g. Construct { ?p :hasName ?name . ?p :hasAddress  ?a . }
 * The graph pattern can be decomposed into clauses:
 * (And(.)
 * 
 * Our cache function is vars(Query) -> Model
 * (e.g. (?p ?a) -> Model) (Hm actually it doesn't matter whether its a model or not)
 * 
 * 
 * Whenever a triple is inserted, that satisfied one of the clauses,  we
 * need to invalidate a corresponding cache entry.
 * 
 * 
 * 
 * @author raven
 *
 */
public class GraphBackedResourceCache
{
	private static final Logger logger = LoggerFactory.getLogger(GraphBackedResourceCache.class);
	
	private String graphName;
	private ISparulExecutor graphDAO;
	private int batchSize = 1024;
	
	
	// Updates pending for the database
	private RDFDiff pendingUpdates = new RDFDiff();

	private Model cacheData = ModelFactory.createDefaultModel();
	
	private CacheSet<Resource> posCache = new CacheSet<Resource>();
	private CacheSet<Resource> negCache = new CacheSet<Resource>();

	
	public static void main(String[] args)
		throws Throwable
	{
		PropertyConfigurator.configure("log4j.properties");
		
		System.out.println("Test");
		
		String graphName = "http://test.org";
		Connection conn = VirtuosoUtils.connect("localhost", "dba", "dba");
		
		ISparulExecutor graphDAO = new VirtuosoJdbcSparulExecutor(conn, graphName);
		
		GraphBackedResourceCache cache = new GraphBackedResourceCache(graphDAO);

		List<Resource> resources = Arrays.asList(new Resource[]{
			ResourceFactory.createResource("http://s.org"),
			ResourceFactory.createResource("http://linkedgeodata.org/triplify/way54888992/nodes")
		});
		
		
		Model m;
		
		m = cache.lookup(resources);
		m = cache.lookup(resources);
		System.out.println(ModelUtil.toString(m));
	}
	
	
	public GraphBackedResourceCache(ISparulExecutor graphDAO)
	{
		this.graphDAO = graphDAO;
	}
	
	public Model lookup(Collection<Resource> resources)
		throws Exception
	{		
		Model result = ModelFactory.createDefaultModel();
		
		Set<Resource> ress = new HashSet<Resource>(resources);

		
		int negCacheHits = 0;
		int posCacheHits = 0;
		
		
		// Check for negative hit
		Iterator<Resource> it = ress.iterator();
		while(it.hasNext()) {
			Resource resource = it.next();
			
			if(negCache.contains(resource)) {
				it.remove();
				++negCacheHits;
				continue;
			}

			if(posCache.contains(resource)) {
				result.add(cacheData.listStatements(resource, null, (RDFNode)null));
				posCache.renew(ress);
				it.remove();
				
				++posCacheHits;
			}
		}
		
		Model lookup = lookupBySubject(graphDAO, ress, graphName, batchSize);
		// Check for which resources we
		
		Set<Resource> subjects = lookup.listSubjects().toSet();
		
		// FIXME If in unlikely case that too many resources are lookup,
		// just purge the cache and fill with as much as possible,
		// rather then updating the cache for each individual resource.
		for(Resource resource : ress) {
			if(subjects.contains(resource)) {
				Resource removed = posCache.addAndGetRemoved(resource);
				if(removed != null) {
					cacheData.remove(removed, null, (RDFNode)null);
				}
				
				cacheData.add(lookup.listStatements(resource, null, (RDFNode)null));
			} else {
				negCache.add(resource);
			}
		}
		
		result.add(lookup);
		
		
		logger.debug("Cache statistics for lookup on " + resources.size() + " resources: posHit/negHit/retrieve = " + posCacheHits + "/" + negCacheHits + "/" + ress.size());
	
		return result;
	}
	
	
	public void insert(Model model)
		throws Exception
	{	
		Model added = ModelFactory.createDefaultModel();
		added.add(model);
		
		// Fetch data for all inserted resources
		Model oldModel = lookup(model.listSubjects().toSet());

		// Remove all triples that already existed
		added.remove(oldModel);
	
		pendingUpdates.add(added);

		for(Resource resource : model.listSubjects().toSet()) {
			if(negCache.contains(resource)) {
				negCache.remove(resource);
				
				Resource removed = posCache.addAndGetRemoved(resource);
				cacheData.remove(removed, null, (RDFNode)null);
			}
			
			if(posCache.contains(resource)) {
				cacheData.add(added.listStatements(resource, null, (RDFNode)null));
			}			
		}
	}

	public void remove(Model model)
		throws Exception
	{	
		Model removed = ModelFactory.createDefaultModel();
		removed.add(model);
		
		// Fetch data for all inserted resources
		Model oldModel = lookup(model.listSubjects().toSet());
	
		// Remove all triples that already existed
		removed.remove(oldModel);
	
		pendingUpdates.remove(removed);
	
		for(Resource resource : model.listSubjects().toSet()) {
			if(negCache.contains(resource)) {
				negCache.remove(resource);
				
				Resource rem = posCache.addAndGetRemoved(resource);
				cacheData.remove(rem, null, (RDFNode)null);
			}
			
			if(posCache.contains(resource)) {
				cacheData.add(removed.listStatements(resource, null, (RDFNode)null));
			}			
		}
	}
	
	
	
	public void applyChanges()
		throws Exception
	{
		graphDAO.remove(pendingUpdates.getRemoved(), graphName);
		graphDAO.insert(pendingUpdates.getAdded(), graphName);
	}
	
	private static Model lookupBySubject(ISparulExecutor graphDAO, Collection<Resource> subjects, String graphName, int batchSize)
		throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		
		
		List<List<Resource>> chunks = CollectionUtils.chunk(subjects, batchSize);
		
		for(List<Resource> chunk : chunks) {
			String resources = "<" + StringUtil.implode(">,<", chunk) + ">";
			
			String fromPart = (graphName != null)
				? "From <" + graphName + "> "
				: "";
	
			String query =
				"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
	
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}
		
		return result;
	}
	
}
