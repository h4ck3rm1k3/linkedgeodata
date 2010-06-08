package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.linkedgeodata.util.SQLUtil;

public class AbstractDAO
{
	protected Connection conn;
	protected Map<Object, PreparedStatement> queryToStmt = new HashMap<Object, PreparedStatement>();

	protected Map<Object, String> idToQuery = new HashMap<Object, String>();
	/*
	private Collection<? extends IQuery> queries;
	
	protected AbstractDAO(Collection<? extends IQuery> queries)
	{
		this.queries = queries;
	}
	*/
	
	protected void setPreparedStatement(Object id, String query)
	{
		Logger.getLogger(this.getClass()).trace("Preparing statement [" + id + "]: " + query);

		idToQuery.put(id, query);
	}
	
	private void close()
		throws Exception
	{
		for(PreparedStatement item : queryToStmt.values()) {
			if(item != null)
				item.close();
		}
		
		queryToStmt.clear();
	}

	
	public void setConnection(Connection conn)
		throws Exception
	{
		close();
		
		for(Map.Entry<Object, String> entry : idToQuery.entrySet()) {
			PreparedStatement stmt = conn.prepareStatement(entry.getValue());
		
			queryToStmt.put(entry.getKey(), stmt);
		}
		
		this.conn = conn;
	}
	
	private PreparedStatement getPreparedStatement(Object id)
	{
		PreparedStatement stmt = queryToStmt.get(id);
		if(stmt == null)
			throw new RuntimeException("No such query with id " + id);
		
		return stmt;
	}
	
	public <T> T execute(Object id, Class<T> clazz, Object ...args)
		throws Exception
	{
		PreparedStatement stmt = getPreparedStatement(id);
		
		T result = SQLUtil.execute(stmt, clazz, args);
		
		return result;
	}
	
	public <T> List<T> executeList(Object id, Class<T> clazz, Object ...args)
		throws Exception
	{
		PreparedStatement stmt = getPreparedStatement(id);
		
		List<T> result = SQLUtil.executeList(stmt, clazz, args);
		
		return result;
	}	
}
