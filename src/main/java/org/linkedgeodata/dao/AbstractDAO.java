/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
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
	///*
	//private Collection<? extends IQuery> queries;
	
	protected AbstractDAO()
	{
	}
	
	
	protected AbstractDAO(Collection<? extends IQuery> queries)
	{
		//this.queries = queries;

		for(IQuery item : queries) {
			idToQuery.put(item, item.getSQL());
		}
	}
	//*/

	protected void setPreparedStatement(Object id, String query)
	{
		Logger.getLogger(this.getClass()).trace("Preparing statement [" + id + "]: " + query);

		idToQuery.put(id, query);
	}

	
	private void close()
		throws SQLException
	{
		for(PreparedStatement item : queryToStmt.values()) {
			if(item != null)
				item.close();
		}
		
		queryToStmt.clear();
	}

	
	public void setConnection(Connection conn)
		throws SQLException
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
		throws SQLException
	{
		PreparedStatement stmt = getPreparedStatement(id);
		
		T result = SQLUtil.execute(stmt, clazz, args);
		
		return result;
	}
	
	public <T> List<T> executeList(Object id, Class<T> clazz, Object ...args)
		throws SQLException
	{
		PreparedStatement stmt = getPreparedStatement(id);
		
		List<T> result = SQLUtil.executeList(stmt, clazz, args);
		
		return result;
	}
	
	public ResultSet executeQuery(Object id, Object ...args)
		throws SQLException
	{
		PreparedStatement stmt = getPreparedStatement(id);
		
		return SQLUtil.execute(stmt, args);
	}
}
