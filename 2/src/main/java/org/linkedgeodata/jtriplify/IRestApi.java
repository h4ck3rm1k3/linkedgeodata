package org.linkedgeodata.jtriplify;

import com.hp.hpl.jena.rdf.model.Model;

public interface IRestApi
{
	public abstract Model getNode(Long idStr) throws Exception;

	public abstract Model getWayNode(Long idStr) throws Exception;

	public abstract Model getWay(Long idStr) throws Exception;

	public abstract Model publicGetEntitiesWithinRadius(Double lat, Double lon,
			Double radius, String className, String language, String matchMode,
			String label, Long offset, Long limit) throws Exception;

	public abstract Model publicGetEntitiesWithinRect(Double latMin,
			Double latMax, Double lonMin, Double lonMax, String className,
			String language, String matchMode, String label, Long offset,
			Long limit) throws Exception;

	public abstract Model publicDescribe(String uri) throws Exception;

}