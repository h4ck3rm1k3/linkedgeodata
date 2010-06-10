package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.linkedgeodata.util.PrefetchIterator;
import org.linkedgeodata.util.StringUtil;
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
	
	public WayTagIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public WayTagIterator(Connection conn, int batchSize)
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
