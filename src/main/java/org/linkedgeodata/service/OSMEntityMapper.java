package org.linkedgeodata.service;

import java.net.URI;
import java.sql.Connection;
import java.util.Arrays;

import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.core.dao.IQuery;

public class OSMEntityMapper
	extends AbstractDAO
	implements IOSMEntityMapper
{
	enum Query
		implements IQuery
	{
		RESOLVE("SELECT t.k, t.v FROM node_tags t WHERE t.k = ? AND t.v = ?");
		;

		private String sql;
		
		Query(String sql)
		{
			this.sql = sql;
		}
		
		@Override
		public String getSQL()
		{
			return sql;
		}
	}
	
	public OSMEntityMapper()
	{
		super(Arrays.asList(Query.values()));
	}
	

	@Override
	public URI resolve(String key, String value)
	{
		//execute(RESOLVE, List<String>.class, key, value);
		return null;
	}

	@Override
	public OntologyType getOntologyType(String key, String value)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
