package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;


public class LinkedGeoDataDAO2
{
	private Connection conn;
	
	public LinkedGeoDataDAO2()
	{
	}
	
	public LinkedGeoDataDAO2(Connection conn)
	{
		this.conn = conn;
	}
	
	
	public void setConnection(Connection conn)
	{
		this.conn = conn;
	}
	
	public MultiMap<Long, Long> getWayMemberships(Collection<Long> nodeIds)
		throws SQLException
	{
		String sql =
			"SELECT node_id, way_id FROM way_nodes WHERE node_id IN (" + StringUtil.implode(",", nodeIds) + ")";
		
		MultiMap<Long, Long> result = new MultiHashMap<Long, Long>();
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long nodeId = rs.getLong(1);
			long wayId = rs.getLong(2);
			
			result.put(nodeId, wayId);
		}
		
		return result;
	}
	
	
	public MultiMap<Long, Long> getNodeMemberships(Collection<Long> wayIds)
		throws SQLException
	{
		String sql =
			"SELECT way_id, node_id FROM way_nodes WHERE way_id IN (" + StringUtil.implode(",", wayIds) + ") ORDER BY way_id, sequence_id";
		
		MultiMap<Long, Long> result = new MultiHashMap<Long, Long>();
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long wayId = rs.getLong(1);
			long nodeId = rs.getLong(2);
			
			result.put(nodeId, wayId);
		}
		
		return result;
	}
}
