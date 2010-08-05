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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.linkedgeodata.dao.NodeStatsDAO.Query;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class TagDAO
	extends AbstractDAO
	implements ITagDAO 
{
	private static final String TABLE_PREFIX_K = "lgd_stats_node_tags_tilek_";
	private static final String TABLE_PREFIX_KV = "lgd_stats_node_tags_tilekv_";

	enum Query
		implements IQuery
	{
		DOES_TAG_EXIST("SELECT tile_id FROM " + TABLE_PREFIX_KV + "0 WHERE (tile_id, k, v) = (0, ?, ?) LIMIT 1"),
		GET_KEYS_BY_PATTERN("SELECT DISTINCT k FROM " + TABLE_PREFIX_KV + "0 WHERE tile_id = 0 AND k ~* ?"),
		;
	
		private String sql;
	
		Query(String sql) { this.sql = sql; }
		public String getSQL() { return sql; }
	}

	public TagDAO()
	{
		super(Arrays.asList(Query.values()));
	}
	
	@Override
	public boolean doesTagExist(Tag tag)
		throws SQLException
	{
		 Long tileId = execute(Query.DOES_TAG_EXIST, Long.class, tag.getKey(), tag.getValue());
		 
		 return tileId != null;
	}

	@Override
	public List<String> findKeys(Pattern pattern)
		throws SQLException
	{
		List<String> result = executeList(Query.GET_KEYS_BY_PATTERN, String.class, pattern.toString());
		return result;
	}

	@Override
	public Connection getConnection()
	{
		return this.conn;
	}

}
