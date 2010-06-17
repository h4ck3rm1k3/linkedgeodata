package org.linkedgeodata.jtriplify;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;

import org.linkedgeodata.dao.LGDQueries;
import org.linkedgeodata.dao.LGDRDFDAO;
import org.linkedgeodata.util.ModelUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * TODO: All methods consisting of more than 1 line should be put into the 
 * LGDRDFDAO
 * 
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
	

	public Model publicGetEntitiesWithinRect(Double latMin, Double latMax, Double lonMin, Double lonMax, String k, String v, Boolean bOr)
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
}