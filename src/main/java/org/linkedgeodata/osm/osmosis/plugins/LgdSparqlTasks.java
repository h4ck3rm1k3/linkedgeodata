package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.linkedgeodata.core.vocab.GeoRSS;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.cache.IGraph;
import org.linkedgeodata.util.sparql.cache.TripleIndexUtils;
import org.linkedgeodata.util.sparql.cache.TripleUtils;

import scala.actors.threadpool.Arrays;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class LgdSparqlTasks
{
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
			Set<String> graphNames, Collection<Resource> nodes) throws Exception
	{
		if (nodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {

			String query =
				"Construct {?wn ?i ?n . } " + createFromClause(graphNames) + "{\n" +
					"\t?wn ?i ?n .\n" +
					"\tFilter(?n In (" + toString(chunk) +")) .\n" +
				"}";

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
			Set<String> graphNames, Collection<Resource> wayNodes) throws Exception
	{
		if (wayNodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(wayNodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {
			String query =
				"Construct { ?wn ?p ?n } " + createFromClause(graphNames) + "{\n" +
					"\t?wn ?p ?n .\n" +
					"\tFilter(?wn In (" + toString(chunk) + ")) .\n" +
				"}";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}

		return result;
	}
	

	public static Map<Resource, RDFNode> fetchNodePositions(
			ISparqlExecutor graphDAO, Set<String> graphNames, Set<Resource> nodes)
			throws Exception
	{
		Map<Resource, RDFNode> result = new HashMap<Resource, RDFNode>();

		if (nodes.isEmpty())
			return result;
		
		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		for (List<Resource> chunk : chunks) {
			String query =
				"Select ?n ?o " + createFromClause(graphNames) + "> {\n" +
					"\t?n " + toString(GeoRSS.point) + " ?o .\n" +
					"\tFilter(?n In (" + toString(chunk) + ")) .\n" +
				"}";

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
			Set<String> graphNames, Set<Resource> ways) throws Exception
	{
		if (ways.isEmpty())
			return ModelFactory.createDefaultModel();

		String query =
			"Construct {?s ?p ?o . } " + createFromClause(graphNames) + "{" +
				"\t?s ?p ?o . " +
				"\tFilter(?s In (" + toString(ways) + ") && " +
				"?p In (" + toString(GeoRSS.line,  GeoRSS.polygon) + ")) .\n" +
			"}";
		Model result = graphDAO.executeConstruct(query);

		return result;
	}

	public static Model selectWayNodesByNodes(ISparqlExecutor graphDAO,
			Set<String> graphNames, Collection<Resource> nodes) throws Exception
	{
		if (nodes.isEmpty())
			return ModelFactory.createDefaultModel();

		List<List<Resource>> chunks = CollectionUtils.chunk(nodes, 1024);
		Model result = ModelFactory.createDefaultModel();
		for (List<Resource> chunk : chunks) {
			String query =
				"Construct { ?wn ?p ?n } " + createFromClause(graphNames) + "{\n" +
					"\t?wn ?p ?n .\n" +
					"\tFilter(?n In (" + toString(chunk) + ")) .\n" +
				"}";

			// FIXME Make executeConstruct accept the output model as an
			// parameter
			Model tmp = graphDAO.executeConstruct(query);
			result.add(tmp);
		}

		return result;
	}

	public static String constructBySubject(Collection<Resource> subjects, Set<String> graphNames)
	{
		return
			"Construct { ?s ?p ?o . } " + createFromClause(graphNames) + "{\n" +
				"\t?s ?p ?o .\n" +
				"\tFilter(?s In (" + toString(subjects) + ")) .\n" +
			"}";
	}

	
	@SuppressWarnings("unchecked")
	public static String toString(Resource ...resources) {
		return toString(Arrays.asList(resources));
	}
	
	public static String toString(Collection<Resource> resources) {
		if(resources.isEmpty())
			return "";
		
		return "<" + Joiner.on(">,<").join(resources) + ">";
	}
	
	public static String createFromClause(Collection<String> graphNames)
    {
        String result = "";
        for(String graphName : graphNames) {
            result += "From <" + graphName + "> ";
        }

        return result;
    }

	
	public static Model fetchStatementsBySubject(ISparqlExecutor sparqlEndpoint, Set<String> graphNames,
			Iterable<Resource> subjects, int chunkSize) throws Exception
	{
		Model result = ModelFactory.createDefaultModel();

		for (List<Resource> item : Iterables.partition(subjects, chunkSize)) {
			String query = constructBySubject(item, graphNames);
			
			Model part = sparqlEndpoint.executeConstruct(query);
			result.add(part);
		}

		return result;
	}

	
	public static Model fetchStatementsBySubject(IGraph graph,
			Iterable<Resource> resources, int chunkSize) throws Exception
	{
		return fetchStatements(graph, resources, 0, chunkSize);
	}
	
	public static Model fetchStatementsByObject(IGraph graph,
			Iterable<Resource> resources, int chunkSize) throws Exception
	{
		return fetchStatements(graph, resources, 0, chunkSize);
	}
	
	public static Model fetchStatements(IGraph graph,
			Iterable<Resource> resources, int keyColumn, int chunkSize) throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		
		for (List<Resource> item : Iterables.partition(resources, chunkSize)) {

			Set<List<Object>> keys = TripleIndexUtils.toKeys(item);
			
			Set<Triple> triples = graph.bulkFind(keys, new int[]{keyColumn});
			
			TripleUtils.toModel(triples, result);
		}

		return result;
	}
	
	public static Map<Resource, RDFNode> fetchNodePositions(
			IGraph graph, Set<Resource> nodes, int chunkSize)
			throws Exception
	{
		Map<Resource, RDFNode> result = new HashMap<Resource, RDFNode>();

		if (nodes.isEmpty())
			return result;
		
		Model model = fetchStatementsBySubject(graph, nodes, chunkSize);
		
		StmtIterator it = model.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			result.put(stmt.getSubject(), stmt.getObject());
		}

		return result;
	}

}
