package org.linkedgeodata.util.sparql.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.linkedgeodata.osm.osmosis.plugins.LgdSparqlTasks;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.util.graph.GraphFactory;


interface IFilteredGraph
	extends IGraph
{
	IFilteredGraph getParent();
	String getFilter();
	
	Multimap<List<Object>, List<Object>> bulkFind(Collection<List<Object>> keys, List<String> columnNames, String filterStr);
}


/*
class FilteredSubGraph
{
}*/



public class SparqlEndpointFilteredGraph
		extends BaseIndexedGraph
		implements IGraphListener
{
	private Query			filterQuery;
	private String			filter;

	private ISparulExecutor	sparqlEndpoint;
	private Set<String>		graphNames;
	
	// Note: Update graph name must be in gaphNames
	private String 			updateGraphName;

	private IFilterCompiler	filterCompiler;
	
	private SparqlEndpointFilteredGraph parentGraph; 
	
	
	public List<String> collectFilters()
	{
		List<String> result = new ArrayList<String>();
		if(parentGraph != null) {
			result.addAll(parentGraph.collectFilters());
		}
		
		if(filter != null && !filter.isEmpty()) {
			result.add(filter);
		}
		
		return result;
	}
	
	/*
	private static String createFilter(List<String> filters)
	{
		String result = "";
		for(String filter : filters) {
			result += "Filter(" + filter + ") .\n";
		}
		
		return result;
	}
	*/

	public SparqlEndpointFilteredGraph(ISparulExecutor sparqlEndpoint)
	{
		this.sparqlEndpoint = sparqlEndpoint;
		this.filter = null;
		this.filterCompiler = new DefaultFilterCompiler();
		this.graphNames = new HashSet<String>();
		this.updateGraphName = null;

		this.filterQuery = QueryFactory
		.create("Construct {?s ?p ?o .} {?s ?p ?o .}");
		
	}
	
	/**
	 * A sparql filter expression in terms of ?s ?p ?o. Maybe null or empty.
	 * 
	 * @param filter
	 */
	public SparqlEndpointFilteredGraph(ISparulExecutor sparqlEndpoint,
			Set<String> graphNames, String filter,
			IFilterCompiler filterCompiler)
	{
		this.sparqlEndpoint = sparqlEndpoint;
		this.graphNames = graphNames;
		this.filterQuery = QueryFactory
				.create("Construct {?s ?p ?o .} {?s ?p ?o . Filter(" + filter
						+ ") .}");
		this.filter = filter;
		this.filterCompiler = filterCompiler;
	}

	public SparqlEndpointFilteredGraph(SparqlEndpointFilteredGraph parentGraph, String filter)
	{
		this.parentGraph = parentGraph;
		this.sparqlEndpoint = parentGraph.sparqlEndpoint;
		this.graphNames = parentGraph.graphNames;
		this.filterCompiler = parentGraph.filterCompiler;
		
		this.filter = filter;
		

		/*
		this.filterQuery = QueryFactory
				.create("Construct {?s ?p ?o .} {?s ?p ?o . Filter(" + filter
						+ ") .}");
		*/
	}
	
	
	/**
	 * Creates a graph with an additional filter.
	 * Any inserts on the parent graph are delegated to the sub graphs.
	 * Therefore, indexes on the subgraphs will be updated automatically.
	 * 
	 * 
	 * @param filter
	 * @return
	 */
	public SparqlEndpointFilteredGraph createSubGraph(String filter) {
		SparqlEndpointFilteredGraph result =  new SparqlEndpointFilteredGraph(this, filter);
		this.getGraphListeners().add(result);
		
		return result;
	}
	
	
	private Collection<Triple> doFiltering(Collection<Triple> triples)
	{
		if(filter == null || filter.isEmpty())
			return triples;
		
		Graph graph = GraphFactory.createDefaultGraph();

		for (Triple triple : triples) {
			graph.add(triple);
		}

		Model model = ModelFactory.createModelForGraph(graph);

		QueryExecution qe = QueryExecutionFactory.create(filterQuery, model);

		try {
			Model result;
			result = qe.execConstruct();
			return new GraphTripleCollectionView(result.getGraph());
		} finally {
			qe.close();
		}
		
	}

	@Override
	public Collection<Triple> bulkFind(Collection<List<Object>> keys, int[] indexColumns)
	{
		return cacheProvider.bulkFind(keys, indexColumns);
	}


	@Override
	public Collection<Triple> uncachedBulkFind(Collection<List<Object>> keys, int[] indexColumns)
	{
		if(keys.isEmpty())
			return Collections.emptySet();
		
		String[] names = { "?s", "?p", "?o" };

		List<String> columnNames = new ArrayList<String>();
		for (int index : indexColumns) {
			columnNames.add(names[index]);
		}

		return uncachedBulkFind(keys, columnNames);
	}

	
	//public static boolean matches(Triple triple, List<Object> keys, 
	
	//private Multimap<List<Object>, List<Object>> uncachedBulkFind(Collection<List<Object>> keys, List<String> columnNames)
	private Collection<Triple> uncachedBulkFind(Collection<List<Object>> keys, List<String> columnNames)
	{
		List<String> filters = filterCompiler.compileFilter(keys, columnNames);

		String partitionFilter = Joiner.on("),(").join(filters);

		if (!partitionFilter.isEmpty()) {
			partitionFilter = "\tFilter(" + partitionFilter + ") .\n";
		}

		String masterFilter = (filter == null) ? "" : "Filter(" + filter
				+ ") .\n";

		String query = "Construct {?s ?p ?o .} "
				+ LgdSparqlTasks.createFromClause(graphNames) + "{\n"
				+ "\t?s ?p ?o .\n" + masterFilter + partitionFilter + "}";

		Model result;
		try {
			result = sparqlEndpoint.executeConstruct(query);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Implement proper exception handling");
		}

		Collection<Triple> triples = new GraphTripleCollectionView(
				result.getGraph());


		return triples;
	}


	@Override
	public void add(Collection<Triple> triples)
	{
		if(parentGraph == null) {
			try {
				sparqlEndpoint.insert(triples, updateGraphName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			super.add(triples); // Notify listeners
		} else {
			parentGraph.add(triples);
		}
	}


	@Override
	public void remove(Collection<Triple> triples)
	{
		if(parentGraph == null) {
			try {
				sparqlEndpoint.remove(triples, updateGraphName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			super.remove(triples); // Notify listeners
		} else {
			parentGraph.remove(triples);
		}
	}


	@Override
	public void onAdd(IGraph g, Collection<Triple> triples)
	{
		if(g == parentGraph) {
			getCacheProvider().addSeen(doFiltering(triples));
		}
	}

	@Override
	public void onRemove(IGraph g, Collection<Triple> triples)
	{
		if(g == parentGraph) {
			getCacheProvider().removeSeen(doFiltering(triples));
		}
	}
}
