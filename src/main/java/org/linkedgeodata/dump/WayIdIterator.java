package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.SinglePrefetchIterator;



public class WayIdIterator
	extends SinglePrefetchIterator<Collection<Long>>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	private String entityFilterStr;
	
	private PreparedStatement prepStmt = null;
	
	public WayIdIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayIdIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	public WayIdIterator(Connection conn, int batchSize, String entityFilterStr)
	{
		this.conn = conn;
		this.batchSize = batchSize;
		this.entityFilterStr = entityFilterStr;
	}
	
	
	public String buildQuery()
	{
		String sql = "SELECT DISTINCT wt2.way_id FROM way_tags wt2 ";
		if(offset != null)
			sql += "WHERE wt2.way_id > ? ";
				
	
		if(entityFilterStr != null) {
			
			sql += (offset == null)
				? "WHERE "
				: "AND ";
			
			sql += "NOT EXISTS (SELECT filter.way_id FROM way_tags filter WHERE filter.way_id = wt2.way_id AND " + entityFilterStr + ") ";
		}
	
		sql += " ORDER BY way_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";
	
		
		return sql;
	}
	
	@Override
	protected Collection<Long> prefetch()
		throws Exception
	{	
		ResultSet rs;		
		if(offset == null) {
			String sql = buildQuery();
			System.out.println(sql);
			rs = conn.createStatement().executeQuery(sql);
		}
		else  {
			if(prepStmt == null) {
				String sql = buildQuery();
				System.out.println(sql);
				prepStmt = conn.prepareStatement(sql);
			}
			
			prepStmt.setLong(1, offset);
			rs = prepStmt.executeQuery();
		}
		
		List<Long> result = SQLUtil.list(rs, Long.class);

		if(!result.isEmpty())
			offset = result.get(result.size() - 1);
		
		if(result.size() < batchSize)
			finish();
		
		return result;
	}	
}


