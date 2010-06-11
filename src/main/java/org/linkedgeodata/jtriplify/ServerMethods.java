package org.linkedgeodata.jtriplify;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.linkedgeodata.core.OSMEntityType;
import org.linkedgeodata.dao.LGDQueries;
import org.linkedgeodata.dao.LGDRDFDAO;

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
	
	public ServerMethods(LGDRDFDAO dao)
	{
		this.dao = dao;
	}
	
	
	public Model getNode(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);

		Model result = ModelFactory.createDefaultModel();
		dao.resolveNodes(result, Collections.singleton(id), false, null);
	
		return result;
	}
	
	public Model getWay(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);
		
		Model result = ModelFactory.createDefaultModel();
		dao.resolveWays(result, Collections.singleton(id), false, null);
	
		return result;
	}
	
	public Model publicGetEntitiesWithinRadius(Double lat, Double lon, Double radius, String k, String v, Boolean bOr)
		throws Exception
	{
		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
		if(tagFilter.isEmpty())
			tagFilter = null;

		Model result = ModelFactory.createDefaultModel();
		dao.getNodesWithinRadius(result, new Point2D.Double(lon, lat), radius, false, tagFilter, null, null);
		dao.getWaysWithinRadius(result, new Point2D.Double(lon, lat), radius, false, tagFilter, null, null);
		
		return result;
	}
	

	public Model publicGetEntitiesWithinRect(Double latMin, Double latMax, Double lonMin, Double lonMax, String k, String v, Boolean bOr)
		throws Exception
	{
		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);
		if(tagFilter.isEmpty())
			tagFilter = null;

		Rectangle2D rect = new Rectangle2D.Double(lonMin, latMin, lonMax - lonMin, latMax - latMin);
		
		Model result = ModelFactory.createDefaultModel();
		dao.getNodesWithinRect(result, rect, false, tagFilter, null, null);
		dao.getWaysWithinRect(result, rect, false, tagFilter, null, null);
		
		return result;
	}

	
	/*
	public List<Model> publicGetEntitiesWithinRadius(final double lat, final double lon, final double distance, final String k, final String v, final boolean bOr)
		throws Exception
	{
		String tagFilter = LGDQueries.createPredicate("", k, v, bOr);

		List<Callable<List<Model>>> callables = new ArrayList<Callable<List<Model>>>();
		
		callables.add(new Callable<List<Model>>() {
			@Override
			public List<Model> call() throws Exception
			{
				List<Long> ids = dao.getEntitiesWithinDistance(OSMEntityType.NODE, lat, lon, distance, k, v, bOr, 1000);
			
				List<Callable<Model>> callables = getNodeModelQueries(ids);
				
				List<Model> result = executeAll(executor, callables);
				
				return result;
			}
		});

		
		callables.add(new Callable<List<Model>>() {
			@Override
			public List<Model> call() throws Exception
			{
				List<Long> ids = dao.getEntitiesWithinDistance(OSMEntityType.WAY, lat, lon, distance, k, v, bOr, 1000);
			
				List<Callable<Model>> callables = getWayModelQueries(ids);
				
				List<Model> result = executeAll(executor, callables);
				
				return result;
			}
		});

		List<List<Model>> modelsList = executeAll(executor, callables);

		Iterator<List<Model>> it = modelsList.iterator();
		
		if(!it.hasNext()) {
			return new ArrayList<Model>();
		}
		
		List<Model> result = it.next();
		
		while(it.hasNext()) {
			List<Model> tmp = it.next();
			
			result.addAll(tmp);
		}
		
		return result;
	}





/*
	public List<Callable<Model>> getNodeModelQueries(final List<Long> ids)
		throws Exception
	{		
		List<Callable<Model>> result = new ArrayList<Callable<Model>>();
		result.add(dao.getNodeGeoRSS(ids));
		result.add(dao.getNodeWGSQuery(ids));		
		result.add(dao.getNodeTagsQuery(ids));
		result.add(dao.getNodeWayMemberQuery(ids));
			
		return result;
	}

	
	public List<Callable<Model>> getWayModelQueries(final List<Long> ids)
		throws Exception
	{
		List<Callable<Model>> result = new ArrayList<Callable<Model>>();
		result.add(dao.getWayGeoRSS(ids));
		result.add(dao.getWayTags(ids));
		result.add(dao.getWayNodes(ids));
	
		
		return result;
	}
	* /

	// TODO Add timeouts. Also add some features to abort queries
	public static <T> List<T> executeAll(ExecutorService executor, Collection<Callable<T>> callables)
		throws InterruptedException, ExecutionException
	{
		List<Future<T>> futures = new ArrayList<Future<T>>();
		for(Callable<T> callable : callables) {
			futures.add(executor.submit(callable));
		}
		
		List<T> result = new ArrayList<T>();
		for(Future<T> future : futures) {
			T value = future.get();
			
			result.add(value);
		}
		
		return result;
	}
	*/
}