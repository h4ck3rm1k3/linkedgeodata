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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.config.PropertyGetter.PropertyCallback;
import org.linkedgeodata.osm.mapping.DBTagMapper;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.tiles.SubTileIdCollection;
import org.linkedgeodata.util.tiles.TileUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;





public class NodeStatsDAO
{
	private static final Logger logger = Logger.getLogger(NodeStatsDAO.class);
	
	/**
	 * Some ideas for options:
	 * MaxInstances: The maximum number of entity-ids that may be returned.
	 * MaxTiles: That maximum number of tiles that may be investigated for looking up entities
	 * BreakOnMaxTiles: Whether to quit with an error if that maximum is reached.
	 * 
	 * 
	 * 
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		Connection conn = PostGISUtil.connectPostGIS("localhost", "unittest_lgd", "postgres", "postgres");

		
		DBTagMapper tagMapper = new DBTagMapper();
		Model model = ModelFactory.createDefaultModel();
		tagMapper.map("http://test", new Tag("amenity", "park"), model);
		
		System.out.println("RESULT:");
		System.out.println("-------------------------------");
		System.out.println( ModelUtil.toString(model));
		
		if(true)
			System.exit(0);
		
		
		NodeStatsDAO dao = new NodeStatsDAO();
		dao.setConnection(conn);
		
		//NodeDAO ndao = new NodeDAO(conn);
		LGDDAO ldao = new LGDDAO(conn);
		NodeDAO ndao = ldao.getNodeDAO();
		
		Rectangle2D maxRect = ndao.getNodeExtents();
		System.out.println("MaxRect: " + maxRect);

		String k = "amenity";
		String v = "pub";
		int maxZoom = 16;
		/*
		int zoom = 4;
		
		
		int limit = 500;
		
		
		if(zoom > maxZoom)
			zoom = maxZoom;
		
		int delta = 2;
		List<Long> candidateTiles = getTileIds(maxRect, zoom);
		while(zoom != maxZoom) {
			System.out.println("Entering level " + zoom);
		
			List<Long> tileIds = candidateTiles;
			System.out.println("TileIds before clipping: " + tileIds.size() + "; " + tileIds);
		
			// TODO Clip the tiles against the rect
			Iterator<Long> it = tileIds.iterator();
			while(it.hasNext()) {
				Rectangle2D tileRect = TileUtil.getRectangle(it.next(), zoom);
		
				//System.out.println(tileRect + " vs " + maxRect);
				if(!(maxRect.intersects(tileRect) || maxRect.contains(tileRect)))
					it.remove();
			}
			System.out.println("TileIds after clipping: " + tileIds.size() + "; " + tileIds);
			
			
			Map<Long, Long> counts = dao.getCounts(tileIds, zoom, k, v);
			System.out.println("Counts: " + counts);
			
			candidateTiles = new ArrayList<Long>();
			for(Map.Entry<Long, Long> entry : counts.entrySet()) {
				long itemCount = entry.getValue();
				if(itemCount == 0 || itemCount > limit) {
					continue;
				}
				
				// add all children of the current tile
				Collection<Long> subTileIds =
					new SubTileIdCollection(entry.getKey(), delta);
				
				for(long subTileId : subTileIds)  
					candidateTiles.add(subTileId);
			}
			
			zoom += delta;
		}
		*/
		
		List<Long> candidateTiles = dao.getCandidateTiles(maxRect, k, v);
		
		System.out.println("Result:" + candidateTiles);
		
			
		// Loading node-ids from the remaining tiles
		List<Long> nodeIds = dao.getNodeIds(candidateTiles, maxZoom, maxRect, "k = '" + k + "' AND v = '" + v + "'");
		
		System.out.println("Nodes (" + nodeIds.size() + "): " + nodeIds);
	}
	
	
	private static final String TABLE_PREFIX_K = "lgd_stats_node_tags_tilek_";
	private static final String TABLE_PREFIX_KV = "lgd_stats_node_tags_tilekv_";

	private Connection conn;
	
	
	public void setConnection(Connection conn)
	{
		this.conn = conn;
	}
	
	
	private static String getTableNameKForZoom(int zoom)
	{
		return TABLE_PREFIX_K + zoom;
	}

	private static String getTableNameKVForZoom(int zoom)
	{
		return TABLE_PREFIX_KV + zoom;
	}

	
	
	public static int getZoom(RectangularShape rect)
	{
		int zoom = getRawZoom(rect, 64);
		if(zoom % 2 == 1)
			--zoom;

		return zoom;
	}

	public static int getRawZoom(RectangularShape rect, int maxTileCount)
	{
		double tileSizeLat = 180 / 65536.0;
		double tileSizeLon = 360 / 65536.0;
		
		for(int zoom = 16; zoom >= 0; --zoom) {
			int numTilesLat = (int)Math.ceil(rect.getHeight() / tileSizeLat);
			int numTilesLon = (int)Math.ceil(rect.getWidth() / tileSizeLon);

			int numTilesTotal = numTilesLat * numTilesLon;
			if(numTilesTotal <= maxTileCount)
				return zoom;

			// Try next zoom level with double size
			tileSizeLat *= 2;
			tileSizeLon *= 2;
		}

		return 0;
	}
	

	/**
	 * Requires denormed schema
	 * 
	 * @param tileIds
	 * @param zoom
	 * @param filter
	 * @param tagFilter
	 * @return
	 * @throws SQLException
	 */
	public List<Long> getNodeIds(Collection<Long> tileIds, int zoom, RectangularShape filter, String tagFilter)
		throws SQLException
	{
		String strTagFilter = (tagFilter == null || tagFilter.trim().isEmpty())
			? ""
			: "AND " + tagFilter + " ";
		
		String bbox = (filter == null)
			? ""
			: "AND geom::geometry && " + LGDQueries.BBox(filter) + " ";
		
		String query
			= "SELECT node_id FROM node_tags "
			+ "WHERE "
			+ "LGD_ToTile(geom, " + zoom + ") IN (" + StringUtil.implode(",", tileIds) + ") "
			+ bbox
			+ strTagFilter;
		
		System.out.println(query);

		ResultSet rs = conn.createStatement().executeQuery(query);
		List<Long> result = SQLUtil.list(rs, Long.class);
		
		return result;
	}
	
	Map<Long, Long> getCounts(Collection<Long> tileIds, int zoom, String k, String v)
		throws SQLException
	{
		Map<Long, Long> result = new HashMap<Long, Long>();
		
		if(tileIds.size() == 0)
			return result;

		
		String tableName = (v == null)
			? getTableNameKForZoom(zoom)
			: getTableNameKVForZoom(zoom);
			
		
		//String query = "SELECT tile_id, usage_count FROM " + getTableNameForZoom(zoom) +
		//" WHERE k = ? AND V = ? AND tile_id IN (" + StringUtil.implode(",", tileIds) + ")";
		
		/*
		 * Safe variant: uncomment if statistics contain duplicates
		 */
		String filterPart = (v == null)
			? "k = ?"
			: "k = ? AND v = ?";
			
		String query = "SELECT tile_id, SUM(usage_count) FROM " + tableName +
		" WHERE " + filterPart + " AND tile_id IN (" + StringUtil.implode(",", tileIds) + ") GROUP BY tile_id";
		
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
		
			ResultSet rs = (v == null)
				? SQLUtil.execute(stmt, k)
				: SQLUtil.execute(stmt, k, v);
			
			while(rs.next()) {
				result.put(rs.getLong(1), rs.getLong(2));
			}
			rs.close();
			
			return result;
		}
		finally {
			if(stmt != null) stmt.close();
		}
	}
	
	
	public static double lonNormX(double lon)
	{
		return (lon + 180.0) / 360.0;
	}
	
	public static double latNormY(double lat)
	{
		return (lat + 90.0) / 180.0;
	}
	
	public static Point llToXY(double lon, double lat, int zoom)
	{
		double f = Math.pow(2, zoom) - 1;

		int x = (int)Math.round(lonNormX(lon) * f);
		int y = (int)Math.round(latNormY(lat) * f);
		
		return new Point(x, y);
	}
	
	//public static Rectangle transform(Rect

	
	public static List<Long> getTileIds(Rectangle2D rect, int zoom)
	{
		//double f = Math.pow(2, zoom); // - 1; removed the -1 -- claus
		
		/**
		 * Transform the given geo-coordinates into tile coordinates.
		 */
		Point min = llToXY(rect.getMinX(), rect.getMinY(), zoom);
		Point max = llToXY(rect.getMaxX(), rect.getMaxY(), zoom);

		List<Long> result = new ArrayList<Long>();
		//echo "$minLon $minLat $maxLon $maxLat\n";
		//echo "$minX $maxX $minY $maxY\n";

		for(int x = min.x; x <= max.x; x++) {
			for(int y = min.y; y <= max.y; y++) {
				result.add(TileUtil.zip(x, y));
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieve the set of tiles in the given area, that actually contain nodes
	 * with the given key/value pair.
	 * 
	 * @param rect
	 * @param k
	 * @param v
	 * @return
	 * @throws SQLException 
	 */
	public List<Long> getCandidateTiles(Rectangle2D rect, String k, String v)
		throws SQLException
	{
		int zoom = getZoom(rect);
		logger.debug("Chose zoom level " + zoom + " for " + rect);
		List<Long> tileIds = getTileIds(rect, zoom);

		List<Long> result = getCandidateTiles(tileIds, zoom, rect, k, v);
		
		return result;
	}

	
	public List<Long> getStatsNodeTagsK(Rectangle rect, List<String> kList, boolean asBlackList)
	{
		int maxZoom = 16;
		
		int zoom = getZoom(rect);
		
		// TODO SQL Injection vulnerability
		String filterPart = (kList == null)
			? ""
			: " AND t.k IN (" + StringUtil.implode(",", kList) + ")";

		if(zoom > maxZoom)
			zoom = maxZoom;
		
		if(zoom == maxZoom) {
			String query
				= "SELECT "
				+ 	"t.k property, "
				+ 	"COUNT(*) c "
				+ "FROM "
				+ 	"node_tags t "
				+ 	"INNER JOIN lgd_properties p ON (p.k = t.k) "
				+ "WHERE "
				+ 	"t.geom && " + LGDQueries.BBox(rect) + " "
				+ 	filterPart + " "
				+ "GROUP BY "
				+ 	"t.k "
				+ "ORDER BY "
				+ 	"p.ontology_entity_type, t.k "
				;
		}
		else {
			List<Long> tileIds = getTileIds(rect, zoom);		
			
			String query
				= "SELECT " 
				+ 	"t.k property, SUM(usage_count) c "
				+ "FROM "
				+ 	"lgd_stats_node_tags_tilek_" + zoom + " t "
				+ "WHERE "
				+ "LGD_ToTile(t.geom, 16) IN (" + StringUtil.implode(",", tileIds) + ") AND "
				+ 	filterPart + " "
				+ "GROUP BY "
				+ 	"t.k "
				+ "ORDER BY "
				+ "t.k"
				;
		}

		return null;
	}
	
	
	public List<Long> getStatsNodeTagsKV(Rectangle rect, String k)
	{
		/*
		if(zoom == maxZoom) {
			$s =
				"SELECT
					v AS value,
					count(*) AS c
				FROM
					node_tags nt JOIN
					nodes n ON (n.id = nt.node_id)
				WHERE
					n.geom && $exactBox AND
					nt.k = '$propertyPart'
				GROUP BY
					k, v
				ORDER BY
					k, v
				";

		}
		else {
			$s =
				"SELECT
					v AS value,
					SUM(count) AS c
				FROM
					node_tags_tiles_kv_$zoom
				WHERE
					k = '$propertyPart' AND $tileBox
				GROUP BY
					k, v
				ORDER BY
					v";			
		}
		
		//String query = 
		 */
		return null;
	}
	
	
	public List<Long> getCandidateTiles(List<Long> tileIds, int zoom, Rectangle2D maxRect, String k, String v)
		throws SQLException
	{
		logger.debug("Retrieving candidate tiles: " + StringUtil.implode(", ",
						"zoom: " + zoom, 
						"k: " + k,
						"v: " + v,
						"rect: " + maxRect,
						"tileIds: " + tileIds));
		
		int limit = 500;
		int maxZoom = 16;
		
		if(zoom > maxZoom)
			zoom = maxZoom;
		
		int delta = 2;
		List<Long> candidateTiles = getTileIds(maxRect, zoom);
		while(zoom != maxZoom) {
			logger.trace("Entering level " + zoom);
		
			tileIds = candidateTiles;
			logger.trace("TileIds before clipping: " + tileIds.size() + "; " + tileIds);
		
			// TODO Clip the tiles against the rect
			Iterator<Long> it = tileIds.iterator();
			while(it.hasNext()) {
				Rectangle2D tileRect = TileUtil.getRectangle(it.next(), zoom);

				//System.out.println(tileRect + " vs " + maxRect);
				if(!(maxRect.intersects(tileRect) || maxRect.contains(tileRect)))
					it.remove();
			}
			logger.trace("TileIds after clipping: " + tileIds.size() + "; " + tileIds);
			
			
			Map<Long, Long> counts = getCounts(tileIds, zoom, k, v);
			logger.trace("Counts: " + counts);
			
			candidateTiles = new ArrayList<Long>();
			for(Map.Entry<Long, Long> entry : counts.entrySet()) {
				long itemCount = entry.getValue();
				if(itemCount == 0 || itemCount > limit) {
					continue;
				}
				
				// add all children of the current tile
				Collection<Long> subTileIds =
					new SubTileIdCollection(entry.getKey(), delta);
				
				for(long subTileId : subTileIds)  
					candidateTiles.add(subTileId);
			}
			
			zoom += delta;
		}
		
		logger.debug(candidateTiles.size() + " tiles found");
		
		return candidateTiles;
	}
}
