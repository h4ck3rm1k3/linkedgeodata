package org.linkedgeodata.jtriplify;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.dao.AbstractDAO;
import org.linkedgeodata.util.SQLUtil;

public class LGDQueries
{
	private static final Logger logger = Logger.getLogger(LGDQueries.class);

	private int n;

	enum Queries {
		NODE_FIND,
		NODE_FIND_K,
		NODE_FIND_K_AND_V,
		NODE_FIND_K_OR_V,
		
		WAY_FIND,
		WAY_FIND_K,
		WAY_FIND_K_AND_V,
		WAY_FIND_K_OR_V
	}

	private static String distancePostGISSphere(
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

	private static String predicateBBox(
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

	private String createPredicate(String relName, String k, String v, boolean bOr)
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

	private String findNodesQuery(String k, String v, boolean bOr, String distance_m)
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

	
	

}