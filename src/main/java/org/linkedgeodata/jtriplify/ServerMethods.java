package org.linkedgeodata.jtriplify;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.linkedgeodata.access.TagFilterUtils;
import org.linkedgeodata.dao.LGDQueries;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.dao.NodeStatsDAO;
import org.linkedgeodata.util.StringUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * TODO All methods consisting of more than 1 line should be put into the 
 * LGDRDFDAO
 * 
 * TODO Use Jersey for providing the REST-interface
 * http://docs.sun.com/app/docs/doc/820-4867/ggnxo?l=en&a=view
 * @author raven
 *
 */
public class ServerMethods
{
	private LGDRDFDAO dao;
	
	//private ExecutorService executor = Executors.newFixedThreadPool(2);
	//private ExecutorService executor = Executors.newCachedThreadPool();
	
	private Map<String, String> prefixMap = null;
	
	
	private Model createModel()
	{
		Model result = ModelFactory.createDefaultModel();
		result.setNsPrefixes(prefixMap);
		
		return result;
	}
	
	public ServerMethods(LGDRDFDAO dao, Map<String, String> prefixMap)
	{
		this.dao = dao;
		this.prefixMap = prefixMap;
	}
	
	
	public Model getNode(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);

		Model result = createModel();
		dao.resolveNodes(result, Collections.singleton(id), false, null);
	
		return result;
	}
	
	public Model getWay(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);
		
		Model result = createModel();
		dao.resolveWays(result, Collections.singleton(id), false, null);
	
		return result;
	}
	
	public Model publicGetEntitiesWithinRadius(Double lat, Double lon, Double radius, String k, String v, Boolean bOr)
		throws Exception
	{
		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
		if(tagFilter.isEmpty())
			tagFilter = null;

		Model result = createModel();
		dao.getNodesWithinRadius(result, new Point2D.Double(lon, lat), radius, false, tagFilter, null, null);
		
		//System.out.println(ModelUtil.toString(result));
		
		dao.getWaysWithinRadius(result, new Point2D.Double(lon, lat), radius, false, tagFilter, null, null);
		//System.out.println(ModelUtil.toString(result));
		
		return result;
	}
	

	public Model publicGetEntitiesWithinRectOld(Double latMin, Double latMax, Double lonMin, Double lonMax, String k, String v, Boolean bOr)
		throws Exception
	{
		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
		if(tagFilter.isEmpty())
			tagFilter = null;

		Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax - lonMin, latMax - latMin);
		
		Model result = createModel();
		dao.getNodesWithinRect(result, rect, false, tagFilter, null, null);
		dao.getWaysWithinRect(result, rect, false, tagFilter, null, null);
		
		return result;
	}
	
	
	public Model publicGetEntitiesWithinRect(Double latMin, Double latMax, Double lonMin, Double lonMax, String className, String label, String language, String matchMode)
		throws Exception
	{
		if(language != null && language.equalsIgnoreCase("any"))
			language = null;
		
		TagFilterUtils.MatchMode mm = TagFilterUtils.MatchMode.EQUALS;
		if(matchMode.equalsIgnoreCase("contains")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		}
		else if(matchMode.equalsIgnoreCase("startsWith")) {
			mm = TagFilterUtils.MatchMode.ILIKE;
			label = label.replace("%", "\\%") + "%";
		}
		if(matchMode.equalsIgnoreCase("ccontains")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = "%" + label.replace("%", "\\%") + "%";
		}
		else if(matchMode.equalsIgnoreCase("cstartsWith")) {
			mm = TagFilterUtils.MatchMode.LIKE;
			label = label.replace("%", "\\%") + "%";
		}

		
		// FIXME Add this to some kind of facade
		TagFilterUtils filterUtil = new TagFilterUtils(dao.getOntologyDAO());

		List<String> entityTagConditions = new ArrayList<String>();
		
		if(className != null)
			entityTagConditions.add(filterUtil.restrictByObject(RDF.type.toString(), "http://linkedgeodata.org/ontology/" + className, "$$"));

		if(label != null)
			entityTagConditions.add(filterUtil.restrictByText(RDFS.label.toString(), label, language, mm, "$$"));
		
	
		Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax - lonMin, latMax - latMin);
		
		Model result = createModel();
		NodeStatsDAO nodeStatsDAO = new NodeStatsDAO(dao.getSQLDAO().getConnection());
		
		Collection<Long> tileIds = null; //NodeStatsDAO.getTileIds(rect, 16);
		Collection<Long> nodeIds = nodeStatsDAO.getNodeIds(tileIds, 16, rect, entityTagConditions);
		
		dao.resolveNodes(result, nodeIds, false, null);
		//dao.getNodesWithinRect(result, rect, false, tagFilter, null, null);
		//dao.getWaysWithinRect(result, rect, false, tagFilter, null, null);
		
		return result;
	}
	
	public Model publicGetOntology()
		throws SQLException
	{
		Model model = dao.getOntologyDAO().getOntology(null);
		
		return model;
	}
		
	public Model publicDescribe(String uri)
		throws Exception
	{
		// Replace the prefix with the appropriate namespace
		// The reason not to use the full original URI is because in testing
		// the domain and port may be differ.
		uri = uri.replaceFirst("^ontology/", dao.getVocabulary().getOntologyNS());

		Model model = dao.getOntologyDAO().describe(uri, null);
		
		return model;
	}
}