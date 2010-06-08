package org.linkedgeodata.dao;

/**
 * Currently not used. May be removed
 * 
 * @author raven
 *
 */
public class Query
	implements IQuery
{
	private String sql;
	
	public Query(String sql)
	{
		this.sql = sql;
	}

	@Override
	public String getSQL()
	{
		return sql;
	}	
}
