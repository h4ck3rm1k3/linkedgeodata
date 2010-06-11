package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;


public class NodeDAO
{
	private static final Logger logger = Logger.getLogger(NodeDAO.class);
	
	private Connection conn;
	
	public NodeDAO()
	{
	}
	
	public NodeDAO(Connection conn)
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
		MultiMap<Long, Long> result = new MultiHashMap<Long, Long>();
		
		if(nodeIds.isEmpty())
			return result;
		
		String sql =
			"SELECT node_id, way_id FROM way_nodes WHERE node_id IN (" + StringUtil.implode(",", nodeIds) + ")";
		
		logger.trace(sql);
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long nodeId = rs.getLong(1);
			long wayId = rs.getLong(2);
			
			result.put(nodeId, wayId);
		}
		
		return result;
	}

	
	public MultiMap<Long, Tag> getTags(Collection<Long> nodeIds, String tagFilterStr)
		throws SQLException
	{
		return LGDDAO.getTags(conn, "node", nodeIds, tagFilterStr);
	}

	

	public Collection<Node> getNodes(Collection<Long> ids, boolean skipUntagged, String tagFilterStr)
		throws SQLException
	{
		// First fetch way-tags - so we can skip ways that do not have any tags
		MultiMap<Long, Tag> idToTags = getTags(ids, tagFilterStr);
	
		Collection<Long> subIds;
		if(skipUntagged) {
			subIds = new ArrayList<Long>();
			for(Map.Entry<Long, Collection<Tag>> entry : idToTags.entrySet()) {
				if(!entry.getValue().isEmpty())
					subIds.add(entry.getKey());
			}
		}
		else {
			subIds = ids;
		}
		
		Collection<Node> result = new ArrayList<Node>();
		if(subIds.isEmpty()) {
			logger.warn("Empty id set");
			return result;
		}
		
		String sql = "SELECT id, geom::geometry FROM nodes WHERE id IN (" + StringUtil.implode(",", subIds) + ") ";
	
		//System.out.println(sql);
		logger.trace("getNodes: " + sql);
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long id = rs.getLong(1);
			PGgeometry g = (PGgeometry)rs.getObject("geom");

			double lat = 0.0;
			double lon = 0.0;
			
			if(g != null) {
				Point p = new Point(g.toString());
				lat = p.getY();
				lon = p.getX();
			}

			
			Node node = new Node(id, -1, (Date)null, (OsmUser)null, -1, lat, lon);
			result.add(node);
			
			Collection<Tag> tags = idToTags.get(id);
			if(tags != null) {
				node.getTags().addAll(tags);
			}
		}
		
		
		return result;
	}
}
