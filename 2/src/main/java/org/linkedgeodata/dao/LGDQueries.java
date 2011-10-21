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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;

import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.tiles.TileInfo;
import org.linkedgeodata.util.tiles.TileUtil;

import com.google.common.base.Joiner;


/**
 * Helper class for building the LGD queries.
 * This class should only do string manips - so no database stuff
 * should go here.
 * 
 * Database stuff should go to LGDDAO
 * 
 * @author raven
 *
 */
public class LGDQueries
{
	public static double log(double value, int base) {
		return Math.log(value) / Math.log(base);
	}
	
	public static String sqlForArea(RectangularShape rect, Integer zoom, String colName)
	{
		if(zoom == null) {
			zoom = (int)(2 + Math.max(
					16 - Math.max(0, Math.min(2 * Math.round(0.5*log((rect.getMaxY()+90-(rect.getMinY()+90))*65536/180,2)),16)),
					16 - Math.max(0, Math.min(2 * Math.round(0.5*log((rect.getMaxX()+180-(rect.getMinX()+180))*65536/360,2)),16))));
		}

		NavigableSet<TileInfo> tiles = TileUtil.tilesForArea(rect, zoom);

		String result = "FALSE";
		
		TileInfo rangeStart = null;
		TileInfo prev = null;
		List<Long> individuals = new ArrayList<Long>();
		for(TileInfo tile : tiles) {
			if(rangeStart == null) {
				rangeStart = tile;
				prev = tile;
				continue;
			}
			
			if(tile.getZipped() == prev.getZipped() + 1) {
				continue;
			} else {
				
				if(rangeStart == prev) {
					individuals.add(prev.getZipped());
				} else {
					result += " OR " + colName + " BETWEEN " + rangeStart.getZipped() + " AND " + prev.getZipped();
				}

				rangeStart = tile;
				prev = tile;
			}
			
		}

		if(rangeStart == prev) {
			individuals.add(prev.getZipped());
		} else {
			result += " OR " + colName + " BETWEEN " + rangeStart.getZipped() + " AND " + prev.getZipped();
		}

		if(!individuals.isEmpty()) {
			result += " OR " + colName + " IN (" + Joiner.on(",").join(individuals) + ")";
		}
		
		return result;
	}
	
	public static String buildAreaStatsQueryExact(RectangularShape rect, Collection<String> keys)
	{
		List<String> ks = SQLUtil.escapeCollectionPostgres(keys);
		String filterPart = "AND k IN (" + Joiner.on(",").join(ks) + ")";

		return
			"SELECT\n" +
			"	t.k           k,\n" +
			"	COUNT(*)          c\n" +
			"FROM\n" +
			"	node_tags t\n" +
			"WHERE\n" +
			"	t.geom && " + BBox(rect) + "\n" +
				filterPart +
			"GROUP BY\n" +
			"	t.k\n" +
			"ORDER BY\n" +
			"	t.k";
		
		/*
		return
			"SELECT\n" +
			"	t.k           property,\n" +
			"	p.owl_entity_type AS type,\n" +
			"	COUNT(*)          c\n" +
			"FROM\n" +
			"	node_tags t\n" +
			//"	INNER JOIN lgd_tag_ontology_k p ON (p.k = t.k)\n" +
			"WHERE\n" +
			"	t.geom && " + BBox(rect) + "\n" +
				filterPart +
			"GROUP BY\n" +
			"	t.k, p.owl_entity_type\n" +
			"ORDER BY\n" +
			"	p.owl_entity_type, t.k";
		*/
	}
	
	public static String buildAreaStatsQueryApprox(RectangularShape rect, int zoom)
	{
		String tileBox = sqlForArea(rect, zoom, "tile");
		return
			"SELECT\n" +
			"	t.k property,\n" +
			"	p.owl_entity_type AS type,\n" +
			"	SUM(t.usage_count) c\n" +
			"FROM\n" +
			"	lgd_stats_node_tags_tilek_$zoom t\n" +
			"	INNER JOIN lgd_tag_ontology_k p ON (p.k = t.k)\n" +
			"WHERE\n" +
			"	" + tileBox + "\n" +
			"GROUP BY\n" +
			"t.k, p.owl_entity_type\n" +
			"ORDER BY\n" +
			"	p.owl_entity_type, t.k\n";
	}
	

	
	
	
	
	
	
	public static String distancePostGISSphere(
			String geomCol,
			String latArg,
			String lonArg)
	{
		String result =
			"ST_Distance_Sphere($geomCol, ST_SetSRID(ST_MakePoint($lonArg, $latArg), 4326))";
		
		result = result.replace("$geomCol", geomCol);
		result = result.replace("$latArg", latArg);
		result = result.replace("$lonArg", lonArg);

		return result;
	}

	
	public static String buildPoint(
			Object latArg,
			Object lonArg)
	{
		String result =
			"ST_SetSRID(ST_MakePoint(" + lonArg + ", " + latArg + "), 4326)";

		return result;
	}

	public static String BBox(RectangularShape rect)
	{
		return BBox(rect.getMinY(), rect.getMaxY(), rect.getMinX(), rect.getMaxX());		
	}
	

	
	public static String BBox(
			Object minLatArg,
			Object maxLatArg,
			Object minLonArg,
			Object maxLonArg)
	{
		String result =
			"ST_SetSRID(ST_MakeBox2D(" +
				buildPoint(minLatArg, minLonArg) + ", " +
				buildPoint(maxLatArg, maxLonArg) +
			"), 4326)\n";
		
		return result;
		
	}
	
	public static String predicateBBox(
			String distance,
			String latArg,
			String lonArg)
	{
		String result =
			"ST_SetSRID(\n" +
			"	ST_MakeBox2D(\n" +
			"		ST_MakePoint(\n" +
			"			$lonArg - $distance / 1000.0 / ABS(COS(RADIANS($latArg)) * 111.0),\n" +
			"			$latArg - $distance / 1000.0 / 111.0\n" +
			"		),\n" +
			"		ST_MakePoint(\n" +
			"			$lonArg + $distance / 1000.0 / ABS(COS(RADIANS($latArg)) * 111.0),\n" +
			"			$latArg + $distance / 1000.0 / 111.0\n" +
			"		)\n" +
			"	),\n" +
			"	4326\n" +
			")";
		
		result = result.replace("$distance", distance);
		result = result.replace("$latArg", latArg);
		result = result.replace("$lonArg", lonArg);
		
		return result;
	}

	public static String createPredicate(String relName, String k, String v, boolean bOr)
	{	
		relName = (relName == null || relName.isEmpty())
			? ""
			: relName + ".";
		
		String kvPred = "";
		
		String kPart = k != null ? relName + "k = '" + k + "'" : "";
		String vPart = v != null ? relName + "v = '" + v + "'" : "";
		
		String opPart = "";
		if(k != null && v != null) {
			opPart = bOr == true ? " OR " : " AND ";
		}
			
		kvPred = kPart + opPart + vPart;

		if(k != null && v != null) {
			kvPred = "(" + kvPred + ")";
		}
		
		return kvPred;
	}

	public static String buildFindNodesQueryOld(String distance_m, String k, String v, boolean bOr)
	{
		String kvPred = createPredicate("snt", k, v, bOr);
		if(!kvPred.isEmpty())
			kvPred += " AND ";

		String result =
			"SELECT\n" +
			"    n.id AS id\n" +
			"FROM\n" +
			"    nodes n\n" +
			"    $join\n" +
			"WHERE\n" +
			"    $kvPred\n" +
			"    n.geom && $predicateBBox\n" +
			"    AND $distanceFn < $distance_m\n" +
			"LIMIT" +
			"    1000";
		
		result = result.replace("$kvPred", kvPred);
		result = result.replace("$join", kvPred.isEmpty() ? "" : "INNER JOIN node_tags snt ON (snt.node_id = n.id)");
		result = result.replace("$predicateBBox", predicateBBox(distance_m, "$1", "$2") );
		result = result.replace("$distanceFn", distancePostGISSphere("n.geom", "$1", "$2"));
		result = result.replace("$distance_m", distance_m);
		
		return result;
	}
	

	/**
	 * Finds all tagged nodes within a given bounding box.
	 * This query only returns the ids of such nodes.
	 * 
	 * 
	 * @param latMin
	 * @param latMax
	 * @param lonMin
	 * @param lonMax
	 * @param k
	 * @param v
	 * @param bOr
	 * @return
	 */
	public static String buildFindTaggedNodesQuery(RectangularShape rect, Integer limit, String entityFilter)
	{		
		String limitStr = limit == null
			? ""
			: "LIMIT " + limit + "\n"; 
		
		String result =
			"SELECT\n" +
			"    osm_entity_type, osm_entity_id\n" +
			"FROM\n" +
			"    lgd_tags lt\n" +
			"WHERE\n" +
			"	 osm_entity_type = 'node'\n" +
			"    geom && " + BBox(rect) + "\n" +
			"    " + entityFilter + "\n" +
			limitStr;

		return result;
	}

	
	public static String buildFindNodesQuery(String distance_m, String k, String v, boolean bOr)
	{
		String kvPred = createPredicate("snt", k, v, bOr);
		if(!kvPred.isEmpty())
			kvPred += " AND ";

		String result =
			"SELECT\n" +
			"    n.id AS id\n" +
			"FROM\n" +
			"    nodes n\n" +
			"    $join\n" +
			"WHERE\n" +
			"    $kvPred\n" +
			"    ST_DWithin(n.geom, $point, $distance_m, true)\n" +
			"LIMIT" +
			"    1000";
		
		result = result.replace("$kvPred", kvPred);
		result = result.replace("$join", kvPred.isEmpty() ? "" : "INNER JOIN node_tags snt ON (snt.node_id = n.id)");
		result = result.replace("$point", buildPoint("$1", "$2") + "::geography");
		result = result.replace("$distance_m", distance_m);
		
		return result;
	}

	public static String buildFindWaysQueryOld(String distance_m, String k, String v, boolean bOr)
	{
		String kvPred = createPredicate("swt", k, v, bOr);
		if(!kvPred.isEmpty())
			kvPred += " AND ";

		String result =
			"SELECT\n" +
			"	w.id\n" +
			"FROM\n" +
			"	ways w\n" +
			"	$join\n" +
			"WHERE\n" +
			"	$kvPred" +
			" 	w.linestring && $predicateBBox\n" +
			"	AND $distanceFn < $distance_m\n" +
			"LIMIT\n" +
			"	1000\n";


		result = result.replace("$kvPred", kvPred);
		result = result.replace("$join", kvPred.isEmpty() ? "" : "INNER JOIN way_tags swt ON(swt.way_id = w.id)");
		result = result.replace("$predicateBBox", predicateBBox(distance_m, "$1", "$2") );
		result = result.replace("$distanceFn", distancePostGISSphere("w.linestring", "$1", "$2"));
		result = result.replace("$distance_m", distance_m);

		return result;
	}

	public static String buildFindWaysQuery(String distance_m, String k, String v, boolean bOr)
	{
		String kvPred = createPredicate("swt", k, v, bOr);
		if(!kvPred.isEmpty())
			kvPred += " AND ";

		String result =
			"SELECT\n" +
			"	w.id\n" +
			"FROM\n" +
			"	ways w\n" +
			"	$join\n" +
			"WHERE\n" +
			"    $kvPred\n" +
			"    ST_DWithin(w.linestring, $point, $distance_m, true)\n" +
			"LIMIT\n" +
			"	1000\n";


		result = result.replace("$kvPred", kvPred);
		result = result.replace("$join", kvPred.isEmpty() ? "" : "INNER JOIN way_tags swt ON(swt.way_id = w.id)");
		result = result.replace("$point", buildPoint("$1", "$2") + "::geography");
		result = result.replace("$distance_m", distance_m);

		return result;
	}

	public static String tagHead(
			String osmEntityType,
			String osmEntityId,
			String propertyTypeCol,
			String kCol,
			String vCol)
	{
		String result = 
				"('base:' || $osmEntityType || '/' || $osmEntityId || '#id') AS id,\n" +
				"('vocabulary:' || $osmEntityType) AS \"rdf:type->\",\n" +
				"(\n" +
				"	CASE WHEN COALESCE($propertyTypeCol, 'DatatypeProperty') = 'DatatypeProperty' THEN '' ELSE 'vocabulary:' END ||\n" +
				"	CASE WHEN $vCol ~* 'yes' AND COALESCE($propertyTypeCol, 'DatatypeProperty') != 'DatatypeProperty'\n" +
				"	THEN REPLACE(REPLACE(REPLACE($kCol, ':', '%25'), '+', '%2B'), ' ', '+')\n" +
				//"	ELSE $vCol\n" +
				"	ELSE REPLACE(REPLACE(REPLACE($vCol, ':', '%25'), '+', '%2B'), ' ', '+')\n" +
				"	END\n" +
				") AS \"t:unc\",\n" +
				"(\n" +
				"	CASE WHEN $propertyTypeCol = 'Class' OR COALESCE($propertyTypeCol, 'DatatypeProperty') != 'DatatypeProperty' AND $vCol ~* 'yes'\n" +
				"	THEN 'rdf:type->'\n" +
				"	ELSE\n" +
				"		CASE WHEN COALESCE($propertyTypeCol, 'DatatypeProperty') = 'DatatypeProperty' THEN '' ELSE 'vocabulary:' END ||\n" +
				"		REPLACE(REPLACE(REPLACE($kCol, ':', '%25'), '+', '%2B'), ' ', '+') ||\n" +
				"		CASE WHEN COALESCE($propertyTypeCol, 'DatatypeProperty') = 'DatatypeProperty' THEN '' ELSE '->' END\n" +
				"	END\n"  +
				") AS a\n";

		result = result.replace("$osmEntityType", osmEntityType);
		result = result.replace("$osmEntityId", osmEntityId);
		result = result.replace("$propertyTypeCol", propertyTypeCol);
		result = result.replace("$kCol", kCol);
		result = result.replace("$vCol", vCol);

		return result;
	}
	
	
	
	public static final String nodeGeoRSSQuery =
		"SELECT\n" +
		"	('base:node/' || n.id || '#id') AS id,\n" +
		"	(Y(n.geom::geometry) || ' ' || X(n.geom::geometry)) AS \"t:unc\" ,\n" +
		"	'georss:point' AS a\n" +
		"FROM\n" +
		"	nodes n\n" +
		"WHERE\n" +
		"	n.id IN ($1)";

		
	public static final String nodeWGSQuery =
			"SELECT\n" +
			"	'base:node/' || n.id || '#id' AS id,\n" +
			"	Y(n.geom::geometry) AS \"wgs84_pos:lat^^xsd:decimal\",\n" +
			"	X(n.geom::geometry) AS \"wgs84_pos:long^^xsd:decimal\"\n" +
			"FROM\n" +
			"	nodes n\n" +
			"WHERE n.id IN ($1)";


	public static final String nodeWayMemberQuery =
			"SELECT\n" +
			"	('base:node/' || wn.node_id || '#id') AS id,\n" +
			"	(wn.way_id || '#id') AS \"memberOfWay->way\",\n" +
			"	sequence_id\n" +
			"FROM\n" +
			"	way_nodes wn\n" +
			"WHERE\n" +
			"	wn.node_id IN ($1)\n";
	
	public static final String nodeTagsQuery =
		"SELECT\n" +
		"	nt.node_id AS id, nt.k, nt.v\n" +
		"FROM\n" +
		"	node_tags nt\n" +
		"WHERE\n" +
		"	nt.node_id IN ($1)";

	/*
	public static final String nodeTagsQuery =
		"SELECT\n" +
		"	" + tagHead("t.osm_entity_type", "t.osm_entity_id", "p.ontology_entity_type", "t.k", "t.v") + "\n" +
		"FROM\n" +
		"	tags t\n" +
		"	LEFT JOIN properties p ON (p.k = t.k)\n" +
		"WHERE\n" +
		"	t.osm_entity_type = 'node' AND\n" +
		"	t.osm_entity_id IN ($1)";
	*/


	public static final String wayGeoRSSQuery =
		"SELECT\n" +
		"	('base:way/' || w.id || '#id') AS id,\n"  +
		"	REPLACE(REPLACE(REPLACE(\n"  +
		"			ST_AsText(ST_Affine(w.linestring::geometry, 0, 1, 1, 0, 0, 0))\n"  +
		"	,'LINESTRING(', ''), ',', ' '), ')', '') AS \"t:unc\",\n"  +

		"	(\n" +
		"		'georss:' ||\n" +
		"		CASE WHEN ST_IsClosed(w.linestring::geometry) THEN 'polygon' ELSE 'line' END\n" +		
		"	) AS a\n" +
		"FROM\n" +
		"	ways w\n" +
		"WHERE\n" +
		"	w.id IN ($1)\n";
	
	public static final String wayTagsQuery =
		"SELECT\n" +
		"	t.way_id AS id, t.k, t.v\n" +
		"FROM\n" +
		"	way_tags t\n" +
		"WHERE\n" +
		"	t.way_id IN ($1)\n";

	
	/*
	public static final String wayTagsQuery =
		"SELECT\n" +
		"	 " +  tagHead("t.osm_entity_type", "t.osm_entity_id", "p.ontology_entity_type", "t.k", "t.v") + "\n" +
		"FROM\n" +
		"	tags t\n" +
		"	LEFT JOIN properties p ON (p.k = t.k)\n" +
		"WHERE\n" +
		"	t.osm_entity_type = 'way' AND\n" +
		"	t.osm_entity_id IN ($1)\n";
*/
	public static final String wayNodeQuery =
		"SELECT\n" +
		"	('base:way/' || wn.way_id || '#id') AS id,\n" +
		"	(wn.node_id || '#id') AS \"hasNode->node\"\n" +
		"FROM\n" +
		"	way_nodes wn\n" +
		"WHERE\n" +
		"	wn.way_id IN ($1)";

	

}