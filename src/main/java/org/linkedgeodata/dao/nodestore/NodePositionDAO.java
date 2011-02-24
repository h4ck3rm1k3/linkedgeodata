package org.linkedgeodata.dao.nodestore;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.cache.util.AbstractBulkMap;
import org.jboss.cache.util.IBulkMap;
import org.linkedgeodata.dao.ISQLDAO;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.sparql.cache.TripleIndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;






/*
class KeySetView
	extends AbstractSet<T>
{

	@Override
	public Iterator<T> iterator()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
}
*/

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
 * @author raven
 *
 */
public class NodePositionDAO
	extends AbstractBulkMap<Long, Point2D>
	implements ISQLDAO
	//extends AbstractDAO
	//implements IBulkMap<Long, Point2D>
{
	private Connection conn;
	
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
		this.conn = conn;
		
		try {
			createTable();
		} catch(Exception e) {
			logger.warn("Error creating table", e);
		}
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.linkedgeodata.dao.nodestore.INodePositionDao#lookup(java.util.Collection)
	 */
	@Override
	public Map<Long, Point2D> getAll(Collection<?> ids)
	{
		Map<Long, Point2D> result = new HashMap<Long, Point2D>();
		if(ids.isEmpty())
			return result;
		
		List<Object> lids = new ArrayList<Object>(ids);
		
		Iterable<List<Object>> partitions = Lists.partition(lids, 512);
		for(List<Object> partition : partitions) {
			String sql = "SELECT node_id, longitude, latitude FROM " + tableName + " WHERE node_id IN (" + StringUtil.implode(",", partition)+ ")"; 
			logger.trace(sql);
		
			ResultSet rs = null;
			try {
				rs = SQLUtil.executeCore(conn, sql);
				while(rs.next()) {
					result.put(rs.getLong(1), new Point2D.Double(rs.getDouble(2), rs.getDouble(3)));
				}
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
			finally {
				try {
					rs.close();
				}
				catch(Exception e1){
				}
			}
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.linkedgeodata.dao.nodestore.INodePositionDao#updateOrInsert(java.util.Map)
	 */
	@Override
	public void putAll(Map<? extends Long, ? extends Point2D> idToPosition)
	{
		Map<Long, Point2D> lookups = getAll(idToPosition.keySet());
		
		Map<Long, Point2D> inserts = new HashMap<Long, Point2D>();
		Map<Long, Point2D> updates = new HashMap<Long, Point2D>();
		
		for(Map.Entry<? extends Long, ? extends Point2D> entry : idToPosition.entrySet()) {
			Point2D p = lookups.get(entry.getKey());
			
			if(p == null) {
				inserts.put(entry.getKey(), entry.getValue());
			} else if(!entry.getValue().equals(p)) {
				updates.put(entry.getKey(), entry.getValue());
			}
		}
		// NOTE: VIRTUOSO SUPPORTS "INSERT REPLACING..."		
		Iterable<List<Map.Entry<Long, Point2D>>> insertPartitions = Iterables.partition(inserts.entrySet(), 512);

		for(List<Map.Entry<Long, Point2D>> partition : insertPartitions) {
			String sql = "INSERT INTO " + tableName + "(node_id, longitude, latitude) VALUES " + mapToSQL(partition);
			logger.trace(sql);
			//	System.out.println(sql);
			try {
				SQLUtil.execute(conn, sql, Void.class);
			} catch(Exception e) {
				logger.error("Error during insert", e);
			}
		}

		/*
		 * UPDATE tbl_1 SET col1 = t.col1 FROM (VALUES
        (25, 3)
        (26, 5)
) AS t(id, col1)
WHERE tbl_1.id = t.id; 
		 */
		
		Iterable<List<Map.Entry<Long, Point2D>>> updatePartitions = Iterables.partition(updates.entrySet(), 512);
		for(List<Map.Entry<Long, Point2D>> partition : updatePartitions) {
			String sql = "UPDATE " + tableName + " SET longitude = t.longitude, latitude = t.latitude FROM (VALUES" + mapToSQL(partition) + ") AS t(node_id, longitude, latitude) WHERE " + tableName + ".node_id = t.node_id";    		
			logger.trace(sql);
			try {
				SQLUtil.execute(conn, sql, Void.class);
			} catch(Exception e) {
				logger.error("Error during update", e);
			}
		}
		
		//String sql = "INSERT INTO node_position_tmp(node_id, latitude, longitude) VALUES (" + mapToSQL(idToPosition) + ")";
		//SQLUtil.execute(conn, sql, Void.class);

		// TODO This loop does individual updates
		// Maybe there is a way to optimize this
		// (I am not sure whether derby supports selects in updates)
		/*
		for(Map.Entry<Long, Point2D> entry : updates.entrySet()) {
			//Iterable<List<Map.Entry<Long, Point2D>>> partitions = Iterables.partition(inserts.entrySet(), 512);

			String sql = "UPDATE " + tableName + " SET latitude = " + entry.getValue().getY() + ", longitude = " + entry.getValue().getX() + " WHERE node_id = " + entry.getKey();   
			logger.trace(sql);
			SQLUtil.execute(conn, sql, Void.class);
		}*/
		
		
	}
	
	//private String mapToSQL(Map<Long, Point2D> map) {
	//}
	private String mapToSQL(Collection<Map.Entry<Long, Point2D>> entries) {
		List<String> parts = new ArrayList<String>();
		for(Map.Entry<Long, Point2D> entry : entries) {
			parts.add("(" + entry.getKey() + "," + entry.getValue().getX() + "," + entry.getValue().getY() + ")");
			//parts.add("('" + entry.getKey() + "','" + entry.getValue().getY() + "','" + entry.getValue().getX() + "')");
		}

		//return StringUtil.implode(",", SQLUtil.quotePostgres(ids));
		
		return StringUtil.implode(",", parts);
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.linkedgeodata.dao.nodestore.INodePositionDao#remove(java.util.Collection)
	 */
	@Override
	public void removeAll(Collection<?> ids)
	{
		if(ids.isEmpty())
			return;
		
		//String idStr = StringUtil.implode(",", SQLUtil.quotePostgres(ids));
		String idStr = StringUtil.implode(",", ids);
		
		String sql = "DELETE FROM " + tableName + " WHERE node_id IN (" + idStr + ")"; 
		logger.trace(sql);
		try {
			SQLUtil.execute(conn, sql, Void.class);
		} catch(Exception e) {
			logger.error("Error during removal", e);
		}
	}


	@Override
	public Connection getConnection()
	{
		return conn;
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
