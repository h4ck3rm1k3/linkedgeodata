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

import java.awt.geom.RectangularShape;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.log4j.Logger;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.postgis.LineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

public class WayDAO
{
	private static final Logger logger = Logger.getLogger(WayDAO.class);

	private Connection conn;
	
	public WayDAO()
	{
	}
	
	public WayDAO(Connection conn)
	{
		this.conn = conn;
	}
	
	
	public void setConnection(Connection conn)
	{
		this.conn = conn;
	}

	public MultiMap<Long, Long> getNodeMemberships(Collection<Long> wayIds)
		throws SQLException
	{
		MultiMap<Long, Long> result = new MultiHashMap<Long, Long>();
		if(wayIds.isEmpty())
			return result;
		
		String sql =
			"SELECT way_id, node_id FROM way_nodes WHERE way_id IN (" + StringUtil.implode(",", wayIds) + ") ORDER BY way_id, sequence_id";
		
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long wayId = rs.getLong(1);
			long nodeId = rs.getLong(2);
			
			result.put(wayId, nodeId);
		}
		
		return result;
	}
	
	public Collection<Way> getWays(Collection<Long> ids, boolean skipUntagged, String tagFilterStr)
		throws SQLException
	{
		Collection<Way> result = getWaysRaw(ids, skipUntagged, tagFilterStr);
		
		Map<Long, Way> idToWay = new HashMap<Long, Way>();
		for(Way way : result)
			idToWay.put(way.getId(), way);
		
		MultiMap<Long, Long> wayToNodes = getNodeMemberships(idToWay.keySet());
		
		for(Map.Entry<Long, Collection<Long>> entry : wayToNodes.entrySet()) {
			Way way = idToWay.get(entry.getKey());
			
			for(Long nodeId : entry.getValue())
				way.getWayNodes().add(new WayNode(nodeId));
		}
		
		return result;
	}
		
	public Collection<Way> getWaysRaw(Collection<Long> ids, boolean skipUntagged, String tagFilterStr)
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
		
		Collection<Way> result = new ArrayList<Way>();
		if(subIds.isEmpty()) {
			logger.warn("Empty id set");
			return result;
		}

		
		String sql = "SELECT id, user_id, linestring::geometry FROM ways WHERE id IN (" + StringUtil.implode(",", subIds) + ") ";
		logger.trace(sql);
		
		ResultSet rs = conn.createStatement().executeQuery(sql);
		while(rs.next()) {
			long id = rs.getLong("id");
			PGgeometry g = (PGgeometry)rs.getObject("linestring");

			int userId = rs.getInt("user_id");
		
			if(userId < 0) {
				userId = 0;
			}
			
			Way way = new Way(id, -1, (Date)null, new OsmUser(userId, "<not set>"), -1);
			result.add(way);
			
			Collection<Tag> tags = idToTags.get(id);
			if(tags != null) {
				way.getTags().addAll(tags);
			}
			
			
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
		
		
		return result;
	}

	
	public MultiMap<Long, Tag> getTags(Collection<Long> ids, String tagFilterStr)
		throws SQLException
	{
		return LGDDAO.getTags(conn, "way", ids, tagFilterStr);
	}
	
	
	public List<Long> getWayIds(RectangularShape filter, Collection<String> tagConditions, Long offset, Long limit)
			throws SQLException
		{
			// Limit and offset
			String offsetPart = (offset == null)
				? ""
				: " OFFSET " + offset + " ";
			
			String limitPart = (limit == null)
				? ""
				: " LIMIT " + limit + " ";
			
		
			// BBox part
			String bbox = NodeStatsDAO.createGeographyFilter("w.linestring", filter);
			
			if(!bbox.isEmpty())
				bbox = "AND " + bbox;
			
		
			String query
					= "SELECT w.id FROM "
					+ NodeStatsDAO.createJoin("ways w", "w.id=wt0.w_id", "way_tags", "wt", "way_id", tagConditions) + " "
					+ bbox
					+ limitPart
					+ offsetPart;
		
			/*
			String query
				= "SELECT node_id FROM node_tags "
				+ "WHERE "
				+ "LGD_ToTile(geom, " + zoom + ") IN (" + StringUtil.implode(",", tileIds) + ") "
				+ bbox
				+ strTagFilter;
			*/
			System.out.println(query);
		
			ResultSet rs = conn.createStatement().executeQuery(query);
			List<Long> result = SQLUtil.list(rs, Long.class);
			
			return result;
		}

	
	
	/*
	public MultiMap<Long, Tag> getWayTags(Collection<Long> wayIds, String tagFilterStr)
		throws SQLException
	{
		String sql = "SELECT way_id, k, v FROM way_tags WHERE way_id IN (" + StringUtil.implode(",", wayIds) + ") ";
			
		if(tagFilterStr != null) {
			sql += "AND " + tagFilterStr + " ";
		}
		
		ResultSet rs = conn.createStatement().executeQuery(sql);	
		
		MultiMap<Long, Tag> idToTags = new MultiHashMap<Long, Tag>();
		int counter = 0;
		while(rs.next()) {
			++counter;
	
			long wayId = rs.getLong("way_id");
			String k = rs.getString("k");
			String v = rs.getString("v");
		
			Tag tag = new Tag(k, v);
			idToTags.put(wayId, tag);
		}

		return idToTags;
	}
	*/
}
