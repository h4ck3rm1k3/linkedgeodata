package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.linkedgeodata.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





/**
 * A Simple DAO for associating an id with latitude/longitude
 *
 * This class is needed because of the following problem:
 * Whenever the list of nodes of certain way becomes modified, the corresponding
 * linestring/polygon needs to be updated.
 * 
 * However, of course whenever a node becomes modified, every
 * ways that refer to it must also be updated.
 * 
 * Fortunately, this data can be queried from the triple store.
 * 
 * 
 * 
 * 
 * 
 * @author raven
 *
 */
public class NodePositionDAO
	extends AbstractDAO
{
	private static final Logger logger = LoggerFactory.getLogger(NodePositionDAO.class);
	
	private String tableName;
	private boolean isTableExisting;
	
	public NodePositionDAO(String tableName)
	{
		this.tableName = tableName;
	}
	
	
	private void createTable()
		throws Exception
	{
		String sql = "CREATE TABLE " + tableName + " (node_id BIGINT PRIMARY KEY, latitude DOUBLE PRECISION NOT NULL, longitude DOUBLE PRECISION NOT NULL)"; 
		SQLUtil.execute(conn, sql, Void.class);		
	}
	
	@Override
	public void setConnection(Connection conn)
		throws SQLException
	{
		super.setConnection(conn);
		
		try {
			createTable();
		} catch(Exception e) {
			logger.warn("Error creating table", e);
		}
		
	}
	
	
	
	public Map<Long, Point2D> lookup(Collection<Long> ids)
		throws SQLException
	{
		Map<Long, Point2D> result = new HashMap<Long, Point2D>();
		if(ids.isEmpty())
			return result;
		
		String sql = "SELECT node_id, latitude, longitude FROM " + tableName + " WHERE node_id IN (" + StringUtil.implode(",", ids)+ ")"; 
		logger.trace(sql);
		
		ResultSet rs = SQLUtil.executeCore(conn, sql);
		try {
			while(rs.next()) {
				result.put(rs.getLong(1), new Point2D.Double(rs.getLong(3), rs.getLong(2)));
			}
		} finally {
			rs.close();
		}
		
		return result;
	}
	
	public void updateOrInsert(Map<Long, Point2D> idToPosition)
		throws SQLException
	{
		Map<Long, Point2D> lookups = lookup(idToPosition.keySet());
		
		Map<Long, Point2D> inserts = new HashMap<Long, Point2D>();
		Map<Long, Point2D> updates = new HashMap<Long, Point2D>();
		
		for(Map.Entry<Long, Point2D> entry : idToPosition.entrySet()) {
			Point2D p = lookups.get(entry.getKey());
			
			if(p == null) {
				inserts.put(entry.getKey(), entry.getValue());
			} else if(!entry.getValue().equals(p)) {
				updates.put(entry.getKey(), entry.getValue());
			}
		}
				
		if(!inserts.isEmpty()) {
			String sql = "INSERT INTO " + tableName + "(node_id, latitude, longitude) VALUES " + mapToSQL(inserts);
			//	System.out.println(sql);
			SQLUtil.execute(conn, sql, Void.class);
		}
		
		//String sql = "INSERT INTO node_position_tmp(node_id, latitude, longitude) VALUES (" + mapToSQL(idToPosition) + ")";
		//SQLUtil.execute(conn, sql, Void.class);

		// TODO This loop does individual updates
		// Maybe there is a way to optimize this
		// (I am not sure whether derby supports selects in updates)
		for(Map.Entry<Long, Point2D> entry : updates.entrySet()) {
			String sql = "UPDATE " + tableName + " SET latitude = " + entry.getValue().getY() + ", longitude = " + entry.getValue().getX() + " WHERE node_id = " + entry.getKey();   
			SQLUtil.execute(conn, sql, Void.class);
		}
		
		
	}
	
	private String mapToSQL(Map<Long, Point2D> map) {
		List<String> parts = new ArrayList<String>();
		for(Map.Entry<Long, Point2D> entry : map.entrySet()) {
			parts.add("(" + entry.getKey() + "," + entry.getValue().getY() + "," + entry.getValue().getX() + ")");
			//parts.add("('" + entry.getKey() + "','" + entry.getValue().getY() + "','" + entry.getValue().getX() + "')");
		}

		//return StringUtil.implode(",", SQLUtil.quotePostgres(ids));
		
		return StringUtil.implode(",", parts);
	}
	
	
	
	public void remove(Iterable<Long> ids)
		throws SQLException
	{
		//String idStr = StringUtil.implode(",", SQLUtil.quotePostgres(ids));
		String idStr = StringUtil.implode(",", ids);
		
		String sql = "DELETE FROM " + tableName + " WHERE node_id IN (" + idStr + ")"; 
		SQLUtil.execute(conn, sql, Void.class);
	}

	/*
	public static void main(String[] args)
		throws Exception
	{
		//Class c = org.apache.derby.jdbc.EmbeddedDriver.class;
		
		String driver = "org.apache.derby.jdbc.EmbeddedDriver";
		Class.forName(driver).newInstance();
		
		String protocol = "jdbc:derby:";
		Connection conn = DriverManager.getConnection(protocol + "derbyDB;create=true");

		
		System.out.println("Current content of database:");
		ResultSet rs = SQLUtil.executeCore(conn, "SELECT node_id, latitude, longitude FROM node_position");
		
		while(rs.next()) {
			System.out.println(StringUtil.implode(", ", rs.getLong(1), rs.getDouble(2), rs.getDouble(2)));
		}
		
		
		
		try {
			//SQLUtil.execute(conn, "DROP TABLE node_position", Void.class);
			
			String sql = "CREATE TABLE node_position (node_id BIGINT PRIMARY KEY, latitude DOUBLE, longitude DOUBLE)"; 
			SQLUtil.execute(conn, sql, Void.class);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		
		try {
			long start = System.nanoTime();
			
			NodePositionDAO dao = new NodePositionDAO("node_position");
			dao.setConnection(conn);
			Random rand = new Random();
			
			long batchSize = 1000;
			for(long i = 0; i < 10000; ++i) {
				Map<Long, Point2D> map = new HashMap<Long, Point2D>();
				for(long j = 0; j < batchSize ; ++j) {
					map.put(i * batchSize + j, new Point2D.Double(rand.nextDouble(), rand.nextDouble()));
				}
				dao.updateOrInsert(map);
				
				long now = System.nanoTime();
				float deltaT = (now - start) / 1000000000.0f;
				long n = (i * batchSize);
				
				System.out.println("Status: n = " + n + ", t = " + deltaT + " ration = " + (n / deltaT));
				
				map.clear();
			}
			

		} finally {
			conn.close();
		}
		
		
		DriverManager.getConnection("jdbc:derby:;shutdown=true");
	}*/

}
