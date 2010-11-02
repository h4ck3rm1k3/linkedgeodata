package org.linkedgeodata.util.sparql;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.commons.jena.ModelSetView;
import org.aksw.commons.util.collections.FlatMapView;
import org.apache.commons.collections15.map.LRUMap;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.osm.osmosis.plugins.LgdSparqlTasks;
import org.linkedgeodata.osm.osmosis.plugins.RDFDiff;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.collections.CacheSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;





/**
 * This file is a playground for my thoughts of what a
 * generic cache implementation
 * could look like.
 * 
 * @author raven
 *
 */



class QueryUtils
{
	/**
	 * Triples that do not appear in optional blocks
	 * (Variables of those triples need to be bound first)
	 * 
	 */
	public static Set<Triple> getRequiredTriples()
	{
		return null;
	}
	
	
	/**
	 * Triples that appear in optional blocks
	 * @return
	 */
	public static Set<Triple> getOptionalTriples()
	{
		return null;
	}
	
	
}


/**
 * This class acts as a filter and a delegate to the different indexes that may
 * be attached to it. 
 *  
 * @author raven
 *
 */
interface ITripleCache
{
	void add(Model model);
	void remove(Model model);
	
	//void addIndex(ITripleCacheIndex index);
	//List<ITripleCacheIndex> getIndexes();
	Collection<ITripleCacheIndex> getIndexes();
	
	Model construct(List<List<Object>> keys, int[] indexColumns)
		throws Exception;
}


/**
 * A cache that only operates on single triples (rather than complete
 * graph patterns which may be composed of multiple triples)
 * 
 * The 
 * 
 * @author raven
 *
 */
interface ITripleCacheIndex
{
	ITripleCache getParent();
	
	void add(Model model);
	void remove(Model model);
}





interface ICacheIndex
{
	//put(List<Object> varBindings, List<Object> values);
}



/**
 * Represents a cache entry for a certain index key.
 * The complete flag indicates whether the store may contain additional data
 * that is not in the index.
 * 
 * Note: A "real" implementation would take orderings into account
 * 
 * @author raven
 */
class IndexTable {
	private boolean isComplete = false;
	private Set<List<Object>> rows = new HashSet<List<Object>>();
	
	public IndexTable() {
	}

	public IndexTable(boolean isComplete) {
		this.isComplete = isComplete;
	}
	
	public boolean isComplete()
	{
		return isComplete;
	}
	
	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
	
	/*
	public void add(List<Object> row) {
		rows.add(row);
	}
	
	public void remove(List<Object> row) {
		rows.remove(row);
	}
	*/
	
	public Set<List<Object>> getRows() {
		return rows;
	}
}



interface ITripleFilter
	extends Selector
{
	/**
	 * Express the predicate implemented by the filter as a sparql filter
	 * (over the variables ?s ?p ?o)
	 * @return
	 */
	String asSparqlFilter();
}

/**
 * This class represents a filter over a single triple.
 * This means, that the filter condition may only be expressed over the
 * variables ?s ?p and ?o.
 * 
 * @author raven
 *
 */
class TripleFilter
	implements ITripleFilter
{	
	
	public TripleFilter(String filterExpr) {
		//System.out.println(filterExpr);
		Query query = QueryFactory.create("Construct {?s ?p ?o .} {?s ?p ?o . Filter(" + filterExpr + ") .}");

		Model model = ModelFactory.createDefaultModel();
		
		model.add(ResourceFactory.createResource("http://test.org"), RDF.type, OWL.Class);
		
		
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		
		long start = System.nanoTime();
		for(int i = 0; i < 1000000; ++i) {
			Model m = qe.execConstruct();
			qe.close();
		}
		
		System.out.println((System.nanoTime() - start) / 1000000000.0);
		
		
		
		ElementGroup group = (ElementGroup)query.getQueryPattern();
		ElementFilter filter = (ElementFilter)group.getElements().get(1);

		System.out.println(filter.getExpr());
	}
	
	
	/**
	 * Must translate the filter expression as implemented by this class
	 * to a sparql filter expression.
	 * e.g. if the filter only matches statements with foo:bar as a predicate
	 * and where the object ends on "woo", then it must return
	 * 
	 * ?p = foo:bar & Regex(?o, ".*woo$")
	 * 
	 * 
	 * 
	 * 
	 * @return
	 */
	public String asSparqlFilter()
	{
		return "";
	}
	
	
	@Override
	public boolean test(Statement s)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSimple()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Resource getSubject()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Property getPredicate()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFNode getObject()
	{
		// TODO Auto-generated method stub
		return null;
	}
}





/**
 * A triple cache only indexes single triple in accordance with a specified
 * filter.
 * 
 * 
 * Please take note of the following points:
 * 
 * . The cache acts merely as a filter and a proxy to its indexes.
 *   So the indexes only receive triples that pass the filter.
 *   You must attach at least one CacheIndex 
 * 
 * . The cache is incapable of caching graph patterns
 *   which would require a join of triple patterns.
 *   However, in future the triple cache may be serve as a base for a
 *   TriplePatternCache.
 * 
 * . The triple cache is based on sparql over jena models.
 *   This means, that testing individual triples against the filter is very slow.
 *   Therefore, always try to use batch inserts/removals.  
 * 
 * . The indexes maintain a map of key-values internally.
 *   These key-value mappings are transformed into a model on demand.
 * 
 * 
 * ------
 * Following point is outdated
 * . Statements may have references to the model that created them.
 *   This means that if the cache contains triples of multiple temporary models
 *   these models could not be cleaned up.
 *   Therefore the design decision was for each
 *   TripleCache instance to maintain its own model.
 *   
 *   
 * 
 * @author raven
 *
 */
class TripleCache
	implements ITripleCache
{
	private Query filterQuery;
	private String filter;
	private Set<ITripleCacheIndex> indexes = new HashSet<ITripleCacheIndex>();
	
	
	private ISparqlExecutor sparqlEndpoint;
	private Set<String> graphNames;
	
	/**
	 * A sparql filter expression in terms of ?s ?p ?o.
	 * Maybe null or empty. 
	 * 
	 * @param filter
	 */
	public TripleCache(ISparqlExecutor sparqlEndpoint, Set<String> graphNames, String filter)
	{
		this.sparqlEndpoint = sparqlEndpoint;
		this.graphNames = graphNames;
		this.filterQuery = QueryFactory.create("Construct {?s ?p ?o .} {?s ?p ?o . Filter(" + filter + ") .}");
		this.filter = filter;
	}
	
	private Model createFilteredModel(Model source) {

		QueryExecution qe = QueryExecutionFactory.create(filterQuery, source);
		
		try {
			Model result;
			result = qe.execConstruct();
			return result;
		}
		finally {
			qe.close();
		}
	}
	
	
	public void add(Model model)
	{
		Model filteredModel = createFilteredModel(model);
		
		for(ITripleCacheIndex index : indexes) {
			index.add(filteredModel);
		}
	}
	
	
	public void remove(Model model)
	{
		Model filteredModel = createFilteredModel(model);

		for(ITripleCacheIndex index : indexes) {
			index.remove(filteredModel);
		}
	}

	@Override
	public Collection<ITripleCacheIndex> getIndexes()
	{
		return indexes;
	}
	
	

	private List<String> compileFilter(List<List<Object>> keys, List<String> columnNames)
	{

		List<String> result = new ArrayList<String>();

		// Special handling for a set of resources
		if(columnNames.size() == 1) {

			String columnName = columnNames.get(0);
			Set<Object> remaining = new HashSet<Object>(new FlatMapView<Object>(keys));
			
			Set<Resource> resources = new HashSet<Resource>();
			
			Iterator<Object> it = remaining.iterator();
			while(it.hasNext()) {
				Object current = it.next();
				if(current instanceof Resource) {
					resources.add((Resource)current);
				}
				
				it.remove();
			}
			
			String inPart = Joiner.on(">,<").join(resources);
			
			if(!inPart.isEmpty()) {
				result.add(columnName + " In (<" + inPart + ">)");
			}
			
			
			for(Resource resource : resources)
				remaining.remove(resource);

			for(Object o : remaining) {
				result.add(compileFilter(o, columnName));
			}
			
		} else {

			for(List<Object> key : keys) {
				String part = "";
				for(int i = 0; i < key.size(); ++i) {
					if(!part.isEmpty())
						part += " &&";
					
					part += compileFilter(key.get(i), columnNames.get(i));
				}
				
				result.add(part);
			}
		}
		
		return result;
	}
	
	
	private String compileFilter(Object o, String columnName) {
		if(o instanceof Resource) {
			return columnName + " = <" + o + ">";
		} else if(o instanceof Literal) {
			Literal l = (Literal)o;

			String result = "str(" + columnName + ") = \"" + l.getLexicalForm() + "\"";
			
			if(!l.getLanguage().isEmpty()) {
				result += "&& langMatches(lang(" + columnName + "), " + l.getLanguage() + ")"; 
			} else if(l.getDatatype() == null) {
				result += "&& datatype(" + columnName + ") = <" + l.getDatatypeURI() + ">";
			}
			
			return result;
		} else {
			throw new RuntimeException("Should never come here - maybe a blank node of evilness?");
		}
	}
	
	
	public Model construct(List<List<Object>> keys, int[] indexColumns)
		throws Exception
	{
		String[] names = {"?s", "?p", "?o"};
		
		List<String> columnNames = new ArrayList<String>();
		for(int index : indexColumns) {
			columnNames.add(names[index]);
		}
		
		return construct(keys, columnNames);
	}
	

	public Model construct(List<List<Object>> keys, List<String> columnNames)
		throws Exception
	{
		List<String> filters = compileFilter(keys, columnNames);
		
		String partitionFilter = Joiner.on("),(").join(filters);
		
		if(!partitionFilter.isEmpty()) {
			partitionFilter = "\tFilter(" + partitionFilter + ") .\n";
		}
		
		String masterFilter = (filter.isEmpty())
			? ""
			: "Filter(" + filter + ") .\n";
		
		String query =
			"Construct {?s ?p ?o .} " + LgdSparqlTasks.createFromClause(graphNames) + "{\n" +
				"\t?s ?p ?o .\n" +
				masterFilter +
				partitionFilter +
			"}";

		Model result = sparqlEndpoint.executeConstruct(query);
		
		
		// publish the result to all indexes so they can create incomplete 
		// partitions.
		add(result);
		
		return result;
	}
}



/**
 * An index for a TripleCache.
 * 
 * The index registers itself automatically at the supplied cache.
 * 
 * The TripleCache defines a filter over a given dataset, whereas the IndexCache
 * partitions the filtered dataset.
 *
 * Partitions can then be accessed by their corresponding key.
 * whenever a partition is accessed, it is first checked whether it exists
 * in memory, and if this is not the case, it is loaded from the underlying
 * store.
 * 
 *  
 * @author raven
 *
 */
class TripleCacheIndexImpl
	implements ITripleCacheIndex
{	
	private ITripleCache cache;
	
	private Model model = ModelFactory.createDefaultModel();
	
	private LRUMap<List<Object>, IndexTable> keyToValues = new LRUMap<List<Object>, IndexTable>();
	private CacheSet<List<Object>> noDataCache = new CacheSet<List<Object>>();

	
	private int[] indexColumns;
	private int[] valueColumns;
	
	/**
	 * Index columns:
	 * 0: subject
	 * 1: predicate
	 * 2: object
	 * 
	 * @param filter
	 * @param indexColumns
	 * @throws Exception
	 */
	public TripleCacheIndexImpl(ITripleCache cache, int ...indexColumns)
		throws Exception
	{
		this.cache = cache;
		this.indexColumns = indexColumns;
		
		valueColumns = new int[3 - indexColumns.length];
		
		int j = 0;
		for(int i = 0; i < valueColumns.length; ++i) {
			if(Arrays.asList(indexColumns).contains(j)) {
				continue;
			}
			
			valueColumns[j++] = i;
		}		
		
		cache.getIndexes().add(this);
	}
	
	
	public static RDFNode getItemAt(Statement stmt, int index)
	{
		switch(index) {
		case 0: return stmt.getSubject();
		case 1: return stmt.getPredicate();
		case 2: return stmt.getObject();
		default: throw new IndexOutOfBoundsException();
		}
	}
	
	/*
	public static void (Statement stmt, int index, Object value) {
		switch(index) {
		case 0: { stmt.
		case 1: return stmt.getPredicate();
		case 2: return stmt.getObject();
		default: throw new IndexOutOfBoundsException();
		}
	}*/
	
	
	public List<Object> extractKey(Statement stmt)
	{
		Object[] keyTmp = new Object[indexColumns.length];
		for(int i = 0; i < indexColumns.length; ++i) {
			keyTmp[i] = getItemAt(stmt, indexColumns[i]);
		}
		return Arrays.asList(keyTmp);
	}
	
	
	/*
	public Map<List<Object>> lookup()
	{
		return null;
	}
	*/
	
	
	public void add(Model model) {
		
	}
	
	
	public static IndexTable getOrCreate(Map<List<?>, IndexTable> map, List<?> key) {
		IndexTable result = map.get(key);
		if(result == null) {
			result = new IndexTable();
			map.put(key, result);
		}
		return result;
	}
	
	

	public Model getModel(List<List<Object>> keys)
		throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		
		Map<List<Object>, IndexTable> map = get(keys);
		
		for(Map.Entry<List<Object>, IndexTable> entry : map.entrySet()) {
			
			Object[] objects = new Object[3];
			for(int i = 0; i < indexColumns.length; ++i) {
				objects[indexColumns[i]] = entry.getKey().get(i);
			}
			
			for(List<Object> row : entry.getValue().getRows()) {
				for(int i = 0; i < valueColumns.length; ++i) {
					objects[valueColumns[i]] = row.get(i);
				}
				
				
				result.add((Resource)objects[0], (Property)objects[1], (RDFNode)objects[2]);
			}
			
			
		}
		
		return result;
	}
	
	
	public Map<List<Object>, IndexTable> get(List<List<Object>> keys)
		throws Exception
	{

		Map<List<?>, IndexTable> result = new HashMap<List<?>, IndexTable>();
		
		List<List<?>> unresolveds = new ArrayList<List<?>>(); 
		for(List<?> key : keys) {
		
			IndexTable table = keyToValues.get(key);
			if(table == null || !table.isComplete()) {
				// Potential cache miss
				if(noDataCache.contains(key)) {
					continue;
				}
				
				unresolveds.add(key);
			} else {
				// Cache hit
				
				IndexTable resultTable = getOrCreate(result, key);
				resultTable.getRows().addAll(table.getRows());
			}
		}
		
		
		Model model = cache.construct(keys, indexColumns);
		
		index(model, true);
		
		// Perform a lookup for all unresolved resources
		//listStatements(map(key);
		return null;
	}

	
	private void index(Model model, boolean isComplete)
	{
		for(Statement stmt : new ModelSetView(model)) {
			List<Object> key = new ArrayList<Object>();
			
			List<Object> value = new ArrayList<Object>();
			
			for(int index : indexColumns) {
				key.add(getItemAt(stmt, index));
			}
			
			for(int index : valueColumns) {
				value.add(getItemAt(stmt, index));
			}
			
			//keyToValues.get(key);
			//IndexTable table = getOrCreate(keyToValues, key);
			
			IndexTable table = keyToValues.get(key);
			if(table == null) {
				table = new IndexTable(isComplete);
				keyToValues.put(key, table);
			}
			
			table.getRows().add(value);
		}
	}


	@Override
	public ITripleCache getParent()
	{
		return cache;
	}


	@Override
	public void remove(Model model)
	{
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * Using ask it is possible to check for the existence of entries
	 * In this case incomplete cache entries do not matter 
	 * 
	 * @param keys
	 * @return
	public Map<List<Object>, Boolean> ask(Collection<List<Object>> keys)
	{
	}
	 */
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

	/*
	public static void main(String[] args)
		throws Exception
	{
		Query query = QueryFactory.create("Select * {?s ?p ?o .}");
		
		Dataset dataset = DatasetFactory.create("http://hanne.aksw.org:8892/sparql");
		///ResourceFactory.createResource("http://dbpedia.org"))
		Model model = dataset.getNamedModel("http://dbpedia.org");
		StmtIterator it = model.listStatements(null, RDF.type, (RDFNode)null);
		while(it.hasNext()) {
			System.out.println(it.next());
		}
		
		dataset.close();
		
		//QueryEngineHTTP x = new QueryEngineHTTP();
		//CacheIndexImpl index = new CacheIndexImpl(query, "?s");
		//System.out.println(query.getQueryPattern().varsMentioned().toArray()[0]);
		
		//GraphFactory.
		//ModelFactory.create
	}
	*/
	
	
	public static void mainThis(String[] args)
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

	
	

	public static void main(String[] args)
		throws Exception
	{
		//TripleFilter filter = new TripleFilter("?s = <http://test.org>");
		
		ISparqlExecutor sparqlEndpoint = new SparqlEndpointExecutor("http://localhost:8890/sparql", "http://test.org");
		
		/*

 http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/5
http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/11/14
http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/11/12
http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/3
 
 
 
		 */
		String[] resourceStrs = {
				"http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/5",
				"http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/11/14",
				"http://nke/Exp3Random/Actors_from_Tennessee/fold/1/phase/1/3" };
		
		
		List<List<Object>> resources = new ArrayList<List<Object>>();
		for(String resourceStr : resourceStrs) {
			Resource resource = ResourceFactory.createResource(resourceStr); 
			resources.add(Collections.singletonList((Object)resource));
		}
		
		List<QuerySolution> rs = sparqlEndpoint.executeSelect("Select Distinct ?s From <http://Exp3Random.log> {?s ?p ?o .} Limit 20");
		for(QuerySolution qs : rs) {
			System.out.println(qs.get("s"));
		}
		//System.exit(1);
		
		
		//sparqlEndpoint.
		TripleCache cache = new TripleCache(sparqlEndpoint, Collections.singleton("http://test.org"), "?p = <" + RDF.type + ">");
		
		TripleCacheIndexImpl sIndex = new TripleCacheIndexImpl(cache, 0);
	
		
		sIndex.get(resources);
		
		Model model = sIndex.getModel(resources);
		
		
		model.write(System.out, "N3");
		
	}
	
}
