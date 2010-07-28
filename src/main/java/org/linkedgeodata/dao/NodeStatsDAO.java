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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ini4j.Ini;
import org.linkedgeodata.access.TagFilterUtils;
import org.linkedgeodata.core.IniUtils;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.tiles.SubTileIdCollection;
import org.linkedgeodata.util.tiles.TileUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * TODO Implements this class
 * What I'm trying to do here:
 * When querying for certain instances, the result is a set of tile-data of 
 * certain types: either there are actual items in the area, or there are
 * just some stats for that tile instead.
 * 
 * This data could be used to render clickable tiles on the map when there is
 * too much data, where a zoom-in action is activated when clicked.
 *  
 */ 
class TileResult
{
	private RectangularShape rect;
	private int zoom;
	private long tileId;
	private long subTileCount;
	private long estimatedNodeCount;

	private List<Long> nodeIds;
	
	public List<Long> getNodeIds()
	{
		return nodeIds;
	}
	
	/**
	 * Returns the rectangle in the form latmin-latmax,longmin-longmax
	 * 
	 * 
	 * @param rect
	 * @return
	 */
	private static String toLatLong(RectangularShape rect)
	{
		return rect.getMinY() + "-" + rect.getMaxY() + "," + rect.getMinX() + "-" + rect.getMaxX();
	}
	
	public void toRDF(Model model) {
		Resource subject = model.createResource("http://linkedgeodata.org/area/rect" + toLatLong(rect));
		//Resource subject = model.createResource("http://linkedgeodata.org/area/rect" + toLatLong(rect));
		
		model.add(subject, RDF.type, model.createResource(LGDVocab.ONTOLOGY_NS + "TileArea"));
		model.addLiteral(subject, model.createProperty(LGDVocab.ONTOLOGY_NS + "tileId"), tileId);
		model.addLiteral(subject, model.createProperty(LGDVocab.ONTOLOGY_NS + "zoomLevel"), zoom);
		model.addLiteral(subject, model.createProperty(LGDVocab.ONTOLOGY_NS + "subTileCount"), subTileCount);
		model.addLiteral(subject, model.createProperty(LGDVocab.ONTOLOGY_NS + "estimatedNodeCount"), estimatedNodeCount);
	}
}


public class NodeStatsDAO
	extends AbstractDAO
{
	private static final Logger logger = Logger.getLogger(NodeStatsDAO.class);
	
	private static final String TABLE_PREFIX_K = "lgd_stats_node_tags_tilek_";
	private static final String TABLE_PREFIX_KV = "lgd_stats_node_tags_tilekv_";

	enum Query
		implements IQuery
	{
		DOES_TAG_EXIST("SELECT tile_id FROM " + TABLE_PREFIX_KV + "0 WHERE (tile_id, k, v) = (0, ?, ?) LIMIT 1"),
		;
		
		private String sql;
		
		Query(String sql) { this.sql = sql; }
		public String getSQL() { return sql; }
	}
	
	
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
		Ini ini = new Ini(new File("config.ini"));
		PropertyConfigurator.configure(ini.get("Main").get("log4jConfigFile"));
		ConnectionConfig connConfig = IniUtils.getConnectionConfig(ini.get("OSMDataBase"));

		Connection conn = PostGISUtil.connectPostGIS(connConfig);
		
		NodeStatsDAO nodeStatsDAO = new NodeStatsDAO();
		nodeStatsDAO.setConnection(conn);
		
		TagMapperDAO tagMapper = new TagMapperDAO();
		LGDDAO lgdDAO = new LGDDAO(conn);
		LGDRDFDAO dao = new LGDRDFDAO(lgdDAO, tagMapper, new LGDVocab());

		OntologyDAO ontologyDAO = new OntologyDAO(tagMapper, conn);
		TagFilterUtils filterUtil = new TagFilterUtils(ontologyDAO);
		
		
		//Rectangle2D maxRect = dao.getSQLDAO().getNodeDAO().getNodeExtents();
		Rectangle2D maxRect = new Rectangle2D.Double(-110.8018691, -38.0312804, 50.2977917, 50.235065);
		System.out.println("MaxRect: " + maxRect);

		String k = "amenity";
		//String v = "pub";
		String v = null;
		int maxZoom = 16;

		//System.out.println(tagMapper.map("http://x.org", new Tag("name", "Technisches Bildungszentrum Mitte"), null));
		//if(true) return;
		
		
		/**
		 * Note $$ is a special table alias, which will get replaced
		 * when the actual query is built 
		 */
		List<String> entityTagConditions = Arrays.asList(
				StringUtil.implode(" OR ",
						filterUtil.restrictByObject(RDF.type.toString(), "http://linkedgeodata.org/ontology/amenity_school", "$$"),
						filterUtil.restrictByObject(RDF.type.toString(), "http://linkedgeodata.org/ontology/amenity_university", "$$")
				)
				//filterUtil.restrictByText(RDFS.label.toString(), "%weil%", "", TagFilterUtils.MatchMode.LIKE, "$$")
		);
		
		
		System.out.println("\n\n\nENTITY TAG CONDITIONS: " + entityTagConditions + "\n\n\n");
		/*
		String tagFilter = "k = '" + k + "'";
		
		if(v != null)
			tagFilter += " AND v = '" + v + "'";
		*/
		
		List<Long> candidateTiles = nodeStatsDAO.getCandidateTiles(maxRect, entityTagConditions);
		List<Long> nodeIds = nodeStatsDAO.getNodeIds(candidateTiles, maxZoom, maxRect, entityTagConditions, 1000, null);

		
		Map<String, Long> statsK = nodeStatsDAO.getStatsNodeTagsK(maxRect, null, false);
		System.out.println(statsK);
		
		Map<String, Map<String, Long>> statsKV = nodeStatsDAO.getStatsNodeTagsKV(maxRect, Arrays.asList("amenity"));
		System.out.println(statsKV);
		
		
		Model model = ModelFactory.createDefaultModel();
		dao.resolveNodes(model, Collections.singleton(nodeIds.iterator().next()), false, null);

		System.out.println("Sleep");
		Thread.sleep(1000);
		System.out.println("Running");
		
		//dao.resolveNodes(model, nodeIds, false, null);

		//tagMapper.map("http://test", new Tag("amenity", "park"), model);

		
		System.out.println("RESULT:");
		System.out.println("-------------------------------");
		System.out.println( ModelUtil.toString(model));
		
		//System.out.println("Count: " + nodeIds.size());
	}	
	

	public NodeStatsDAO()
	{
		super(Arrays.asList(Query.values()));
	}
	
	public NodeStatsDAO(Connection conn)
		throws Exception
	{
		super(Arrays.asList(Query.values()));

		setConnection(conn);
	}
	
	/*
	public void setConnection(Connection conn)
	{
		this.conn = conn;
	}*/
	
	
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
	public List<Long> getNodeIds(Collection<Long> tileIds, int zoom, RectangularShape filter, Collection<String> tagConditions, Integer limit, Integer offset)
		throws SQLException
	{
		String offsetPart = (offset == null)
			? ""
			: " OFFSET " + offset + " ";
		
		String limitPart = (limit == null)
			? ""
			: " LIMIT " + limit + " ";
		
		if(tileIds != null && tileIds.size() == 0)
			return new ArrayList<Long>();
		
		String tileFilter = (tileIds == null)
			? ""
			: " AND LGD_ToTile(nt0.geom, " + zoom + ") IN (" + StringUtil.implode(",", tileIds) + ") ";

		
		String bbox = (filter == null)
			? ""
			: "AND nt0.geom::geometry && " + LGDQueries.BBox(filter) + " ";
		

		String query
			=  "SELECT nt0.node_id FROM "
				+ createJoin("node_tags", "nt", "node_id", tagConditions) + " "
				+ tileFilter
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
	public List<Long> getNodeIds(Collection<Long> tileIds, int zoom, RectangularShape filter, String tagFilter)
		throws SQLException
	{
		if(tileIds.size() == 0)
			return new ArrayList<Long>();
		
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
	*/

	public static String createJoin(String tableName, String aliasPrefix, String joinColumn, Collection<String> tagFilters)
	{
		String joinPart = "";
		String wherePart = "";
		
		if(tagFilters.isEmpty())
			joinPart += tableName + " " + aliasPrefix + "0";
		
		int aliasCounter = 0;
		for(String filter : tagFilters) {
			String currentAlias = aliasPrefix + aliasCounter;

			if(!joinPart.isEmpty()) {
				String oldAlias = aliasPrefix + (aliasCounter - 1);
				joinPart += " INNER JOIN " + tableName + " " + currentAlias + " ON (" + currentAlias + "." + joinColumn + " = " + oldAlias + "." + joinColumn + ")";
			}
			else {
				joinPart += tableName + " " + currentAlias;
			}
			
			if(!wherePart.isEmpty())
				wherePart += " AND ";
			
			wherePart += "(" + filter.replace("$$", currentAlias) + ")"; 
			
			++aliasCounter;
		}
		
		if(wherePart.isEmpty())
			wherePart += " TRUE ";
		
		return joinPart + " WHERE " + wherePart;
	}

	Map<Long, Long> getCounts(Collection<Long> tileIds, int zoom, Collection<String> tagConditions)
		throws SQLException
	{
		Map<Long, Long> result = new HashMap<Long, Long>();
		
		if(tileIds.size() == 0)
			return result;

		/*
		String tableName = (v == null)
			? getTableNameKForZoom(zoom)
			: getTableNameKVForZoom(zoom);
		*/
		String tableName = getTableNameKVForZoom(zoom);
		

		// TODO Create the query based on the filters
		// TODO Add an optimization if the tagFilter is independent of v
		// With the assumption that table aliases are special constants, this
		// is easy to determine.
		// Proposed pattern for special table aliases: $$v1$$
		
		
		
		//String query = "SELECT tile_id, usage_count FROM " + getTableNameForZoom(zoom) +
		//" WHERE k = ? AND V = ? AND tile_id IN (" + StringUtil.implode(",", tileIds) + ")";
		
		/*
		 * Safe variant: uncomment if statistics contain duplicates
		 */
		/*
		String filterPart = (v == null)
			? "k = ?"
			: "k = ? AND v = ?";
		*/

		/*
		String filterPart = tagFilter;
			
		String query = "SELECT tile_id, SUM(usage_count) FROM " + tableName +
		" WHERE " + filterPart + " AND tile_id IN (" + StringUtil.implode(",", tileIds) + ") GROUP BY tile_id";
		*/
		
		String query
			=  "SELECT alias0.tile_id, SUM(alias0.usage_count) FROM "
			+ createJoin(tableName, "alias", "tile_id", tagConditions) + " "
			+ " AND alias0.tile_id IN (" + StringUtil.implode(",", tileIds) + ") GROUP BY alias0.tile_id";
			
		//" WHERE " + filterPart + " AND tile_id IN (" + StringUtil.implode(",", tileIds) + ") GROUP BY tile_id";
		
		System.out.println(query);
		
		//PreparedStatement stmt = null;
		//try {
			//stmt = conn.prepareStatement(query);
		
			/*
			ResultSet rs = (v == null)
				? SQLUtil.execute(stmt, k)
				: SQLUtil.execute(stmt, k, v);
			*/
			ResultSet rs = SQLUtil.executeCore(conn, query);
			
			while(rs.next()) {
				result.put(rs.getLong(1), rs.getLong(2));
			}
			rs.close();
			
			return result;
		//}
		//finally {
			//if(stmt != null) stmt.close();
		//}
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

	
	public static List<Long> getTileIds(RectangularShape rect, int zoom)
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
	public List<Long> getCandidateTiles(Rectangle2D rect, Collection<String> tagConditions)
		throws SQLException
	{
		int zoom = getZoom(rect);
		logger.debug("Chose zoom level " + zoom + " for " + rect);
		List<Long> tileIds = getTileIds(rect, zoom);

		List<Long> result = getCandidateTiles(tileIds, zoom, rect, tagConditions);
		
		return result;
	}

	
	/**
	 * 
	 * 
	 * FIXME Maybe enhance to Map<Map<String, Lon>>
	 * 
	 * @param rect
	 * @param kList
	 * @param asBlackList
	 * @return
	 * @throws SQLException
	 */
	public Map<String, Long> getStatsNodeTagsK(RectangularShape rect, List<String> kList, boolean asBlackList)
		throws SQLException
	{
		int maxZoom = 16;
		
		int zoom = getZoom(rect);
		
		//int limit 100;
		
		
		// TODO SQL Injection vulnerability
		String filterPart = (kList == null || kList.isEmpty())
			? ""
			: " AND t.k IN (" + StringUtil.implode(",", kList) + ")";

		/*
		String limitPart = (limit == null)
			? ""
			: " LIMIT " + limit;
		*/

		if(zoom > maxZoom)
			zoom = maxZoom;

		String query;
		if(zoom == maxZoom) {
			query
				= "SELECT "
				+ 	"t.k k, "
				+ 	"COUNT(*) usage_count "
				+ "FROM "
				+ 	"node_tags t "
				//+ 	"INNER JOIN lgd_tag_mapping_simple_base sb ON (p.k = t.k) "
				//+   "INNER JOIN lgd_tag_mapping_simple_class sc ON (sc.id = db.id) "
				+ "WHERE "
				+ 	"t.geom && " + LGDQueries.BBox(rect) + " "
				+ 	filterPart + " "
				+ "GROUP BY "
				+ 	"t.k "
				//+ "ORDER BY "
				//+ 	"t.k "
				;
		}
		else {
			List<Long> tileIds = getTileIds(rect, zoom);		
			
			query
				= "SELECT " 
				+ 	"t.k k, SUM(usage_count) usage_count "
				+ "FROM "
				+ 	"lgd_stats_node_tags_tilek_" + zoom + " t "
				+ "WHERE "
				+ 	"tile_id IN (" + StringUtil.implode(",", tileIds) + ") "
				+ 	filterPart + " "
				+ "GROUP BY "
				+ 	"t.k "
				//+ "ORDER BY "
				//+ "t.k"
				;
		}

		// Use a tree map here, as its nicer for display
		Map<String, Long> result = new TreeMap<String, Long>();
		ResultSet rs = SQLUtil.executeCore(conn, query);
		while(rs.next()) {
			String k = rs.getString(1);
			long usageCount = rs.getLong(2);
			
			result.put(k, usageCount);
		}
		
		return result;
	}
	
	
	
	public Map<String, Map<String, Long>> getStatsNodeTagsKV(RectangularShape rect, Iterable<String> ks)
		throws SQLException
	{
		int maxZoom = 16;
		
		int zoom = getZoom(rect);
		
		//Collection<String> kList = new Transformed(k, new QuoteTransformer());
		
		//int limit 100;
		/*
		// TODO SQL Injection vulnerability
		String filterPart = (kList == null || kList.isEmpty())
			? ""
			: " AND t.k IN (" + StringUtil.implode(",", kList) + ")";
		*/
		
		String query;
		if(zoom == maxZoom) {
			query
			= "SELECT "
			+ 	"t.k k, t.v v, COUNT(*) usage_count "
			+ "FROM "
			+ 	"node_tags t "
			//+ 	"INNER JOIN lgd_tag_mapping_simple_base sb ON (p.k = t.k) "
			//+   "INNER JOIN lgd_tag_mapping_simple_class sc ON (sc.id = db.id) "
			+ "WHERE "
			+ 	"t.geom && " + LGDQueries.BBox(rect) + " "
			+   "t.k = (" + StringUtil.implode(",", SQLUtil.quotePostgres(ks)) + ") "
			//+ 	filterPart + " "
			+ "GROUP BY "
			+ 	"t.k, t.v "
			//+ "ORDER BY "
			//+ 	"t.k "
			;
		}
		else {
			List<Long> tileIds = getTileIds(rect, zoom);	
			
			query
				= "SELECT " 
				+ 	"t.v v, SUM(usage_count) usage_count "
				+ "FROM "
				+ 	"lgd_stats_node_tags_tilekv_" + zoom + " t "
				+ "WHERE "
				+ 	"tile_id IN (" + StringUtil.implode(",", tileIds) + ") "
				+	"AND k IN (" +  StringUtil.implode(",", SQLUtil.quotePostgres(ks)) + ") " 
				//+ 	filterPart + " "
				+ "GROUP BY "
				+ 	"t.k, t.v "
				//+ "ORDER BY "
				//+ "t.k"
				;
		}
		
		// Use a tree map here, as its nicer for display
		Map<String, Map<String, Long>> result = new TreeMap<String, Map<String, Long>>();
		ResultSet rs = SQLUtil.executeCore(conn, query);
		while(rs.next()) {
			String k = rs.getString(1);
			String v = rs.getString(2);
			long usageCount = rs.getLong(2);
			
			Map<String, Long> vMap = result.get(k);
			if(vMap == null) {
				vMap = new TreeMap<String, Long>();
				result.put(k, vMap);
			}
			
			vMap.put(v, usageCount);
		}
		
		return result;
	}
	
	
	/**
	 * Retrieves the all available classes in a given area 
	 * 
	 * @param tiles
	 * @return
	 */
	public List<Long> getClassesInAreaEstimated(Iterable<Long> tileIds, int zoom)
	{
		/*
		String query
			= "SELECT"
			+ 	"a.k, a.usage_count, b. "
			+ "FROM "
			+ 	"lgd_stats_node_tags_tilek_" + zoom + " a"
			+   "INNER JOIN lgd_tag_mapping_simple_class b ON (b.k, b.v) = (a.k, a.v)" 
			+ "WHERE "
			+ 	"a.tile_id IN (" + StringUtil.implode(",", tileIds) + ")"
			+ "GROUP BY "
			+   "a.k"
			;
		*/
		return null;
	}
	
	
	
	public List<Long> getCandidateTiles(List<Long> tileIds, int zoom, Rectangle2D maxRect, Collection<String> tagConditions)
		throws SQLException
	{
		logger.debug("Retrieving candidate tiles: " + StringUtil.implode(", ",
						"zoom: " + zoom, 
						"tagConditions: " + tagConditions,
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
			
			
			Map<Long, Long> counts = getCounts(tileIds, zoom, tagConditions);
			logger.trace("Counts: " + counts);
			
			candidateTiles = new ArrayList<Long>();
			for(Map.Entry<Long, Long> entry : counts.entrySet()) {
				long itemCount = entry.getValue();
				if(itemCount == 0)
					continue;

				if(itemCount > limit) {
					logger.trace("Skipping tile because ItemCount " + itemCount + " exceeds limit " + limit);
					break;
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
