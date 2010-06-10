package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.linkedgeodata.util.PrefetchIterator;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;


public class WayTagIteratorDenorm1
	extends PrefetchIterator<Way>
{
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;
	
	public WayTagIteratorDenorm1(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIteratorDenorm1(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	@Override
	protected Iterator<Way> prefetch()
		throws Exception
	{
		String sql = "SELECT DISTINCT way_id FROM way_tags ";
		if(offset != null)
			sql += "WHERE way_id > " + offset + " ";
		
		sql += "ORDER BY way_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";
	
		String s = "SELECT way_id, k, v, linestring::geometry FROM way_tags WHERE way_id IN (" + sql + ")";
	
		//System.out.println(s);
		ResultSet rs = conn.createStatement().executeQuery(s);
	
		Map<Long, Way> idToWay = new HashMap<Long, Way>();
		
		int counter = 0;
		while(rs.next()) {
			++counter;
	
			long wayId = rs.getLong("way_id");		
			String k = rs.getString("k");
			String v = rs.getString("v");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");
			
			offset = offset == null ? wayId : Math.max(offset, wayId);
			
			Way way = idToWay.get(wayId);
			if(way == null) {
				way = new Way(wayId, 0, (Date)null, null, -1);
				idToWay.put(wayId, way);
			}
	
			Tag tag = new Tag(k, v);
			way.getTags().add(tag);
	
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
				
				tag = new Tag(key, value);
				//System.out.println(tag);
				
				if(!way.getTags().contains(tag)) {
					way.getTags().add(tag);
				}
			}
		}
		
		if(counter == 0)
			return null;
		else
			return idToWay.values().iterator();
	}
}
