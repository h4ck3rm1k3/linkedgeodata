package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.linkedgeodata.jtriplify.LGDOSMEntityBuilder;
import org.linkedgeodata.util.PrefetchIterator;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;



public class WayTagIterator
	extends PrefetchIterator<Way>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	private String entityFilterStr;
	private String tagFilterStr;
	
	private PreparedStatement prepStmt = null;
	
	public WayTagIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	public WayTagIterator(Connection conn, int batchSize, String entityFilterStr, String tagFilterStr)
	{
		this.conn = conn;
		this.batchSize = batchSize;
		this.entityFilterStr = entityFilterStr;
		this.tagFilterStr = tagFilterStr;
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
	
		String s = "SELECT way_id, k, v, w.linestring::geometry FROM way_tags JOIN ways w ON (way_id = id) WHERE way_id IN (" + sql + ") ";
	
		
		if(tagFilterStr != null) {
			s += "AND " + tagFilterStr + " ";
		}
		
		return s;
	}
	
	@Override
	protected Iterator<Way> prefetch()
		throws Exception
	{	
		ResultSet rs;		
		if(offset == null) {
			String s = buildQuery();
			System.out.println(s);
			rs = conn.createStatement().executeQuery(s);
		}
		else  {
			if(prepStmt == null) {
				String s = buildQuery();
				System.out.println(s);
				prepStmt = conn.prepareStatement(s);
			}
			
			prepStmt.setLong(1, offset);
			rs = prepStmt.executeQuery();
		}
		
		Map<Long, Way> idToWay = new HashMap<Long, Way>();
		while(rs.next()) {
			long id = rs.getLong("way_id");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");
	
			offset = offset == null ? id : Math.max(offset, id);
	
			Way way = new Way(id, 0, (Date)null, null, -1);
			idToWay.put(id, way);
	
			if(g != null) {
				LineString ls = new LineString(g.getValue());
				String value = "";
				for(Point point : ls.getPoints()) {
					if(!value.isEmpty())
						value += " ";
	
					value += point.getY() + " " + point.getX();
				}
	
				String key =
					(ls.getFirstPoint().equals(ls.getLastPoint()) && ls.getPoints().length > 2)
					? "@@geoRSSLine"
					: "@@geoRSSPolygon";
				
				Tag tag = new Tag(key, value);
	
				way.getTags().add(tag);
			}
			
		}
		
		if(idToWay.isEmpty()) {
			prepStmt.close();
			return null;
		}
	
		String s = "SELECT way_id, k, v FROM way_tags WHERE way_id IN (" + 
		StringUtil.implode(", ", idToWay.keySet()) + ")";
	
		//System.out.println(s);
		rs = conn.createStatement().executeQuery(s);
	
		
		int counter = 0;
		while(rs.next()) {
			++counter;
	
			long wayId = rs.getLong("way_id");
			String k = rs.getString("k");
			String v = rs.getString("v");
			
			Way way = idToWay.get(wayId);	
			Tag tag = new Tag(k, v);
			way.getTags().add(tag);
		}
		
		return idToWay.values().iterator();
	}	
}





/**
 * To be removed
 * 
 * @author raven
 *
 */
class WayTagIteratorOld
	extends PrefetchIterator<Way>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	public WayTagIteratorOld(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIteratorOld(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Way> prefetch()
		throws Exception
	{
		Map<Long, Way> idToWay = new HashMap<Long, Way>();
	
		String sql = "SELECT id, linestring::geometry FROM ways ";
		if(offset != null)
			sql += "WHERE id > " + offset + " ";
		
		sql += "ORDER BY id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";
	
	
		ResultSet rs = conn.createStatement().executeQuery(sql);
		
		while(rs.next()) {
			long id = rs.getLong("id");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");
	
			offset = offset == null ? id : Math.max(offset, id);
	
			Way way = new Way(id, 0, (Date)null, null, -1);
			idToWay.put(id, way);
	
			if(g != null) {
				LineString ls = new LineString(g.getValue());
				String value = "";
				for(Point point : ls.getPoints()) {
					if(!value.isEmpty())
						value += " ";
	
					value += point.getY() + " " + point.getX();
				}
	
				String key =
					(ls.getFirstPoint().equals(ls.getLastPoint()) && ls.getPoints().length > 2)
					? "@@geoRSSLine"
					: "@@geoRSSPolygon";
				
				Tag tag = new Tag(key, value);
	
				way.getTags().add(tag);
			}
			
		}
		
		if(idToWay.isEmpty())
			return null;
	
		String s = "SELECT way_id, k, v FROM way_tags WHERE way_id IN (" + 
		StringUtil.implode(", ", idToWay.keySet()) + ")";
	
		//System.out.println(s);
		rs = conn.createStatement().executeQuery(s);
	
		
		int counter = 0;
		while(rs.next()) {
			++counter;
	
			long wayId = rs.getLong("way_id");
			String k = rs.getString("k");
			String v = rs.getString("v");
			
			Way way = idToWay.get(wayId);	
			Tag tag = new Tag(k, v);
			way.getTags().add(tag);
		}
		
		return idToWay.values().iterator();
	}
}
