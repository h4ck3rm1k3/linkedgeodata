package org.linkedgeodata.jtriplify;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.linkedgeodata.core.OSMEntityType;
import org.linkedgeodata.dao.LinkedGeoDataDAO;
import org.linkedgeodata.util.ModelUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/*
public class ServerMethodsOld
{
	private LinkedGeoDataDAO dao;

	//private ExecutorService executor = Executors.newFixedThreadPool(2);
	private ExecutorService executor = Executors.newCachedThreadPool();
	
	public ServerMethodsOld(LinkedGeoDataDAO dao)
	{
		this.dao = dao;
	}
	
	
	public Model publicNear(Double lat, Double lon, Double distance, String k, String v, Boolean bOr)
		throws Exception
	{
		List<Model> models = getNearModels(lat, lon, distance, k, v, bOr);
		
		Model result = ModelUtil.combine(models);
		
		return result;
	}
	

	// TODO Write this method
	public Model publicFindEntitiesByBBox(Double latMin, Double latMax, Double lonMin, Double lonMax, String k, String v, Boolean bOr)
		throws Exception
	{
		Callable<Model> callable = dao.getEntitiesWithinBBox(OSMEntityType.NODE, latMin, latMax, lonMin, lonMax, 1000, k, v, bOr);

		Future<Model> model = executor.submit(callable);
	
		Model result = model.get();
		
		return result;
	}

	
	
	public List<Model> getNearModels(final double lat, final double lon, final double distance, final String k, final String v, final boolean bOr)
		throws Exception
	{
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


	public Model getNode(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);
		
		final List<Long> ids = Arrays.asList(id);
	
		List<Callable<Model>> callables = getNodeModelQueries(ids);
		List<Model> models = executeAll(executor, callables);
	
		Model result = ModelUtil.combine(models);
		
		return result;
	}

	public Model getWay(String idStr)
		throws Exception
	{
		Long id = Long.parseLong(idStr);
		
		final List<Long> ids = Arrays.asList(id);

		List<Callable<Model>> callables = getWayModelQueries(ids);
		List<Model> models = executeAll(executor, callables);
		
		Model result = ModelUtil.combine(models);
		
		return result;
	}


	
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

}
*/
