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
package org.linkedgeodata.dump;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.jtriplify.LGDOSMEntityBuilder;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.SinglePrefetchIterator;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * An iterator for the modified node_tags table.
 * Modified means, that the geom column was added to this table.
 * 
 * Assumes the following columns:
 * id   : long
 * k    : String
 * v    : String
 * geom : geometry
 * 
 * @author Claus Stadler
 *
 */
public class NodeIdIterator
	extends SinglePrefetchIterator<Collection<Long>>
{
	private static final Logger logger = Logger.getLogger(NodeIdIterator.class);
	
	private Connection conn;
	private Long offset = null;
	
	private Integer batchSize = 1000;

	private String entityFilterStr;
	//private String tagFilterStr;
	
	private PreparedStatement prepStmt = null;
	
	public NodeIdIterator(Connection conn)
	{
		this.conn = conn;
	}
	
	public NodeIdIterator(Connection conn, int batchSize)
	{
		this.conn = conn;
		this.batchSize = batchSize;
	}
	
	public NodeIdIterator(Connection conn, int batchSize, String entityFilterStr)
	{
		this.conn = conn;
		this.batchSize = batchSize;
		this.entityFilterStr = entityFilterStr;
	}
	
	
	public String buildQuery()
	{
		String sql = "SELECT DISTINCT nt.node_id FROM node_tags nt ";
		if(offset != null)
			sql += "WHERE nt.node_id > ? ";
				

		if(entityFilterStr != null) {
			
			sql += (offset == null)
				? "WHERE "
				: "AND ";
			
			sql += "nt.node_id NOT IN (SELECT filter.node_id FROM node_tags filter WHERE filter.node_id = nt.node_id AND " + entityFilterStr + ") ";
		}

		sql += " ORDER BY node_id ASC ";
		
		if(batchSize != null)
			sql += "LIMIT " + batchSize + " ";


		/*
		String s = "SELECT node_id, k, v, n.geom::geometry FROM node_tags JOIN nodes n ON (node_id = id) WHERE node_id IN (" + sql + ") ";
	
		
		if(tagFilterStr != null) {
			s += "AND " + tagFilterStr + " ";
		}
		*/
		
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
		
		logger.trace("Current offset: " + offset);
		List<Long> result = SQLUtil.list(rs, Long.class);

		if(!result.isEmpty())
			offset = result.get(result.size() - 1);
		
		if(result.size() < batchSize)
			finish();
		
		return result;
	}	
}
