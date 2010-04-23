package org.linkedgeodata.jtriplify;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;

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
		if(k == null && v == null)
			return null;
		
		String kvPred = "";
		
		String kPart = k != null ? relName + ".k = '" + k + "'" : "";
		String vPart = v != null ? relName + ".v = '" + v + "'" : "";
		
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

	public static String buildFindNodesQuery(String k, String v, boolean bOr, String distance_m)
	{
		String kvPred = createPredicate("snt", k, v, bOr);
		if(kvPred != null)
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
		result = result.replace("$join", kvPred != "" ? "INNER JOIN node_tags snt ON (snt.node_id = n.id)" : "");
		result = result.replace("$predicateBBox", predicateBBox(distance_m, "$1", "$2") );
		result = result.replace("$distanceFn", distancePostGISSphere("n.geom", "$1", "$2"));
		result = result.replace("$distanceFn", distance_m);
		
		return result;
	}

	
	public static String wayGeoRSSQuery =
		"SELECT\n" +
		"	('base:way/' || w.id || '#id') AS id,\n"  +
		"	REPLACE(REPLACE(REPLACE(\n"  +
		"			ST_AsText(ST_Affine(w.linestring, 0, 1, 1, 0, 0, 0))\n"  +
		"	,'LINESTRING(', ''), ',', ' '), ')', '') AS \"t:unc\",\n"  +

		"	(\n" +
		"		'georss:' ||\n" +
		"		CASE WHEN ST_IsClosed(w.linestring) THEN 'polygon' ELSE 'line' END\n" +		
		"	) AS a\n" +
		"FROM\n" +
		"	ways w\n" +
		"WHERE\n" +
		"	w.id IN ($1)\n";
	
	

}