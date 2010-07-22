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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.postgis.PGgeometry;
import org.postgis.Point;


public class NodeDAO
{
	private static final Logger logger = Logger.getLogger(NodeDAO.class);
	
	private Connection conn;
	
	
	private static final String strGetNodeExtents = "SELECT MIN(X(geom::geometry)), MIN(Y(geom::geometry)), MAX(X(geom::geometry)), MAX(Y(geom::geometry)) FROM nodes";
	
	private PreparedStatement stmtGetNodeExtents;
	
	
	public NodeDAO()
	{
	}
	
	public NodeDAO(Connection conn)
		throws SQLException
	{
		setConnection(conn);
	}
	
	
	public void setConnection(Connection conn)
		throws SQLException
	{
		if(stmtGetNodeExtents != null)
			stmtGetNodeExtents.close();
		
		this.conn = conn;

		stmtGetNodeExtents = conn.prepareStatement(strGetNodeExtents);
	}
	
	public Rectangle2D getNodeExtents()
		throws SQLException
	{
		ResultSet rs = stmtGetNodeExtents.executeQuery();
		try {
			while(rs.next()) {
				Rectangle2D result = new Rectangle2D.Double(
						rs.getDouble(1),
						rs.getDouble(2),
						rs.getDouble(3) - rs.getDouble(1),
						rs.getDouble(4) - rs.getDouble(2));
				
				return result;
			}
		}
		finally {
			rs.close();
		}
		return new Rectangle2D.Double();
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
		if(ids == null)
				throw new NullPointerException();
		
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
