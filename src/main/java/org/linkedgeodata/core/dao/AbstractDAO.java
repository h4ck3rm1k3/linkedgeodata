package org.linkedgeodata.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.linkedgeodata.util.SQLUtil;

public class AbstractDAO
{
	protected Connection conn;
	protected Map<IQuery, PreparedStatement> queryToStmt = new HashMap<IQuery, PreparedStatement>();

	private Collection<? extends IQuery> queries;
	
	protected AbstractDAO(Collection<? extends IQuery> queries)
	{
		this.queries = queries;
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
		
		for(IQuery item : queries) {
			PreparedStatement stmt = conn.prepareStatement(item.getSQL());
		
			queryToStmt.put(item, stmt);
		}
		
		this.conn = conn;
	}
	
	public <T> T execute(IQuery query, Class<T> clazz, Object ...args)
		throws Exception
	{
		PreparedStatement stmt = queryToStmt.get(query);
		for(int i = 0; i < args.length; ++i) {
			stmt.setObject(i + i, args[i]);
		}

		T result = null;
		if(clazz == null || Void.class.equals(clazz)) {
			stmt.execute();
		}
		else {
			ResultSet rs = stmt.executeQuery();		
			result = SQLUtil.single(rs, clazz);
			rs.close();
		}
		
		return result;
	}
}
