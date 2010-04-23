package org.linkedgeodata.jtriplify;


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
				"	CASE WHEN $propertyTypeCol = 'DatatypeProperty' THEN '' ELSE 'vocabulary:' END ||\n" +
				"	CASE WHEN $vCol ~* 'yes' AND $propertyTypeCol != 'DatatypeProperty'\n" +
				"		THEN REPLACE($kCol, ':', '%25')\n" +
				"		ELSE $vCol\n" +
				"	END\n" +
				") AS \"t:unc\",\n" +
				"(\n" +
				"	CASE WHEN $propertyTypeCol = 'Class' OR $propertyTypeCol != 'DatatypeProperty' AND $vCol ~* 'yes'\n" +
				"	THEN 'rdf:type->'\n" +
				"	ELSE\n" +
				"		CASE WHEN $propertyTypeCol = 'DatatypeProperty' THEN '' ELSE 'vocabulary:' END ||\n" +
				"		REPLACE($kCol, ':', '%25') ||\n" +
				"		CASE WHEN $propertyTypeCol = 'DatatypeProperty' THEN '' ELSE '->' END\n" +
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
		"	(Y(n.geom) || ' ' || X(n.geom)) AS \"t:unc\" ,\n" +
		"	'georss:point' AS a\n" +
		"FROM\n" +
		"	nodes n\n" +
		"WHERE\n" +
		"	n.id IN ($1)";

		
	public static final String nodeWGSQuery =
			"SELECT\n" +
			"	'base:node/' || n.id || '#id' AS id,\n" +
			"	Y(n.geom) AS \"wgs84_pos:lat^^xsd:decimal\",\n" +
			"	X(n.geom) AS \"wgs84_pos:long^^xsd:decimal\"\n" +
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
		"	" + tagHead("t.osm_entity_type", "t.osm_entity_id", "p.ontology_entity_type", "t.k", "t.v") + "\n" +
		"FROM\n" +
		"	tags t\n" +
		"	LEFT JOIN properties p ON (p.k = t.k)\n" +
		"WHERE\n" +
		"	t.osm_entity_type = 'node' AND\n" +
		"	t.osm_entity_id IN ($1)";



	public static final String wayGeoRSSQuery =
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
	
	public static final String wayTagsQuery =
		"SELECT\n" +
		"	 " +  tagHead("t.osm_entity_type", "t.osm_entity_id", "p.ontology_entity_type", "t.k", "t.v") + "\n" +
		"FROM\n" +
		"	tags t\n" +
		"	LEFT JOIN properties p ON (p.k = t.k)\n" +
		"WHERE\n" +
		"	t.osm_entity_type = 'way' AND\n" +
		"	t.osm_entity_id IN ($1)\n";

	public static final String wayNodeQuery =
		"SELECT\n" +
		"	('base:way/' || wn.way_id || '#id') AS id,\n" +
		"	(wn.node_id || '#id') AS \"hasNode->node\"\n" +
		"FROM\n" +
		"	way_nodes wn\n" +
		"WHERE\n" +
		"	wn.way_id IN ($1)";

	

}