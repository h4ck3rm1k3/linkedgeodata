<?php

//define("MYSQLSTRING",'postgresql://localhost/osm');
//define("USER",'osm');
//define("PW",'osm');

/**
 * TODO Add a configuration section for the table names.
 */
$prefix = "";
$tblTags = $prefix . "tags";



try {
	$triplify['db'] = new PDO('pgsql:host=localhost;dbname=lgd', 'postgres', 'postgres');
} catch(Exception $e) {
    echo $e;
    die('db server down');
}

$triplify['namespaces'] = array(
	'vocabulary'=>'http://linkedgeodata.org/vocabulary#',
	'base'=>'http://linkedgeodata.org/triplify/',
	'rdf'=>'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
	'rdfs'=>'http://www.w3.org/2000/01/rdf-schema#',
	'owl'=>'http://www.w3.org/2002/07/owl#',
	'foaf'=>'http://xmlns.com/foaf/0.1/',
	'sioc'=>'http://rdfs.org/sioc/ns#',
	'sioctypes'=>'http://rdfs.org/sioc/types#',
	'dc'=>'http://purl.org/dc/elements/1.1/',
	'dcterms'=>'http://purl.org/dc/terms/',
	'skos'=>'http://www.w3.org/2004/02/skos/core#',
	'tag'=>'http://www.holygoat.co.uk/owl/redwood/0.1/tags/',
	'xsd'=>'http://www.w3.org/2001/XMLSchema#',
	'wgs84_pos'=>'http://www.w3.org/2003/01/geo/wgs84_pos#',
	'update'=>'http://triplify.org/vocabulary/update#',
	'dbp'=>'http://dbpedia.org/resource/',
    'georss'=>'http://www.georss.org/georss/'
);

$distance = distancePostGISSphere();



/**
 *
 *
 */
function predicateBBox(
	$distance,
	$latArg = '$1',
	$lonArg = '$2')
{
/*
$box='longitude between CEIL(($2-($3/1000)/abs(cos(radians($1))*111))*10000000) and CEIL(($2+($3/1000)/abs(cos(radians($1))*111))*10000000)
		AND latitude between CEIL(($1-($3/1000/111))*10000000) and CEIL(($1+($3/1000/111))*10000000)';
*/
	return "
		ST_SetSRID(
			ST_MakeBox2D(
				ST_MakePoint(
					$lonArg - $distance / 1000.0 / ABS(COS(RADIANS($latArg)) * 111.0),
					$latArg - $distance / 1000.0 / 111.0
				),
				ST_MakePoint(
					$lonArg + $distance / 1000.0 / ABS(COS(RADIANS($latArg)) * 111.0),
					$latArg + $distance / 1000.0 / 111.0
				)
			),
			4326
		)
		";
}



function distancePostGISSphere($geomCol = 'n.geom', $latArg = '$1', $lonArg = '$2')
{
	return "
		ST_Distance_Sphere($geomCol, ST_SetSRID(ST_MakePoint($2, $1), 4326))
	";
}


/**
 * This is the old distance function used with the MYSQL version of LGD.
 * Used only as a reference. use postgis' ST_Distance_Sphere/Spheroid instead.
 *
 */
function distanceOld($latCol = 'Y(n.geom)', $lonCol = 'X(n.geom)', $latArg = '$1', $lonArg = '$2')
{
/*
$distance='ROUND(1000*1.609 * 3956 * 2 * ASIN(SQRT(  POWER(SIN(($1 - latitude/10000000) * pi()/180 / 2), 2) +
		COS($1 * pi()/180) *  COS(latitude/10000000 * pi()/180) *  POWER(SIN(($2 - longitude/10000000) * pi()
		/180 / 2), 2) ) )) AS distance';
*/

	return "
		ROUND(1000 * 1.609 * 3956 * 2 *
			ASIN(
				SQRT(
					POWER(SIN(RADIANS($latArg - $latCol) / 2), 2) +
					COS(RADIANS($latArg)) *  COS(RADIANS($latCol)) *
					POWER(SIN(RADIANS($lonArg - $lonCol) / 2), 2)
				)
			)
		)
	";
}


/**
 * Common projection for queries dealing with tags
 *
 * 		Y(n.geom) AS \"wgs84_pos:lat^^xsd:decimal\",
 *		X(n.geom) AS \"wgs84_pos:long^^xsd:decimal\",
 */
//			" . distance('$1', '$2', '$3', 'Y(e.geom)', 'X(e.geom)') . " AS distance
function tagHead($osmEntityType, $osmEntityId, $propertyTypeCol = 'p.ontology_entity_type', $kCol = 't.k', $vCol = 't.v')
{
	return "
			('base:' || $osmEntityType || '/' || $osmEntityId || '#id') AS id,
			('vocabulary:' || $osmEntityType) AS \"rdf:type\",
			(
				CASE WHEN $propertyTypeCol = 'DatatypeProperty' THEN '' ELSE 'vocabulary:' END ||
				CASE WHEN $vCol ~* 'yes' AND $propertyTypeCol != 'DatatypeProperty'
				THEN REPLACE($kCol, ':', '%25')
				ELSE $vCol
				END
			) AS \"t:unc\",
			(
				CASE WHEN $propertyTypeCol = 'Class' OR $propertyTypeCol != 'DatatypeProperty' AND $vCol ~* 'yes'
				THEN 'rdf:type'
				ELSE
					REPLACE($kCol, ':', '%25') ||
					CASE WHEN $propertyTypeCol = 'DatatypeProperty' THEN '' ELSE '->' END
				END 
			) AS a
	";
}


$nodeTagsQuery = "
	SELECT
		" . tagHead('t.osm_entity_type', 't.osm_entity_id', 'p.ontology_entity_type', 't.k', 't.v') . "
	FROM
		tags t
		LEFT JOIN properties p ON (p.k = t.k)
	WHERE
		t.osm_entity_type = 'node' AND
		t.osm_entity_id IN ($1)
";


$wayTagsQuery = "
	SELECT
		" . tagHead('t.osm_entity_type', 't.osm_entity_id', 'p.ontology_entity_type', 't.k', 't.v') . "
	FROM
		tags t
		LEFT JOIN properties p ON (p.k = t.k)
	WHERE
		t.osm_entity_type = 'way' AND
		t.osm_entity_id IN ($1)
";



/**
 * 
 * This select statement actually consits of 2 queries:
 * The first subselect finds the starting and end node of a way and the
 * total number of nodes on that way.
 * 
 * The second subselect combines all node positions of that path
 * into a single string.
 * 
 */
// to bind: wn.way_id
//			WHERE
//				wn.way_id = $1
$wayGeoRSSQuery = "
	SELECT
		('base:way/' || w.id || '#id') AS id,
		
		REPLACE(REPLACE(REPLACE(
				ST_AsText(ST_Affine(w.linestring, 0, 1, 1, 0, 0, 0))
		,'LINESTRING(', ''), ',', ' '), ')', '') AS \"t:unc\",
		
		(
			'georss:' ||
			CASE WHEN ST_IsClosed(w.linestring) THEN 'polygon' ELSE 'line' END		
		) AS a
	FROM
		ways w
	WHERE
		w.id IN ($1)
";

/*
$georssWayTypeSubQuery = "
		SELECT
			mm.way_id,
			CASE WHEN wn_min.node_id = wn_max.node_id THEN
				CASE WHEN mm.node_count >= 4 THEN 'polygon' ELSE 'error' END
			ELSE
				CASE WHEN mm.node_count >= 2 THEN 'line' ELSE 'error' END
			END
			AS way_type
		FROM (
			SELECT
				wn.way_id,
				MIN(wn.sequence_id) AS min_seq_id,
				MAX(wn.sequence_id) AS max_seq_id,
				COUNT(*) AS node_count
			FROM
				way_nodes wn
			GROUP BY
				wn.way_id
		) mm
			INNER JOIN way_nodes wn_min ON ((wn_min.way_id, wn_min.sequence_id) = (mm.way_id, mm.min_seq_id))
			INNER JOIN way_nodes wn_max ON ((wn_max.way_id, wn_max.sequence_id) = (mm.way_id, mm.max_seq_id))
	";


// to bind: wn.way_id 
//			WHERE
//				wn.way_id = $1
$georssWayDataSubQuery = "
	SELECT
	    w.id AS way_id,
		array_to_string(array(
			SELECT
				Y(n.geom) || ' ' || X(n.geom)
			FROM
				way_nodes wn
				INNER JOIN nodes n ON (n.id = wn.node_id)
			WHERE
				wn.way_id = w.id
			ORDER BY
				wn.sequence_id
		), ' ') AS val
	FROM
		ways w
	";



$wayGeoRSSQuery = "
	SELECT
		('base:way/' || wt.way_id || '#id') AS id,
		wd.val AS \"t:unc\",
		('georss:' || wt.way_type)
	FROM
		($georssWayTypeSubQuery) wt
		INNER JOIN ($georssWayDataSubQuery) wd ON (wt.way_id = wd.way_id)
	WHERE
		wt.way_id IN ($1)
	";
*/



/**
 * Export nodes as georss
 *
 */
$nodeGeoRSSQuery = "
	SELECT
		('base:node/' || n.id || '#id') AS id,
		(Y(n.geom) || ' ' || X(n.geom)) AS \"t:unc\" ,
		'georss:point' AS a
	FROM
		nodes n
	WHERE
		n.id IN ($1)
	";


function createPredicate($relName, $k, $v, $bOr)
{
	$kvPred = "";
	
	$kPart = isset($k) ? "$relName.k = '" . $k . "'" : "";
	$vPart = isset($v) ? "$relName.v = '" . $v . "'" : "";
	$opPart = "";
	if(isset($k) && isset($v)) {
		$opPart = $bOr == true ? " OR " : " AND ";

		$kvPred = "($kPart $opPart $vPart)";
	}
	else {
		$kvPred = $kPart . $opPart . $vPart;
	}	

	return $kvPred;
}


/**
 * Find all entities near a given point(lat, lon) within a given radius.
 * Optionally: filter by tags
 *
 * This query is intended to be used as a subquery for retrieving node-ids.
 *
 */
function findNodesQuery($k, $v, $bOr, $distance_m='$3')
{
	$kvPred = createPredicate('snt', $k, $v, $bOr);
	if($kvPred != "")
		$kvPred .= " AND ";


	$predicateBBox =  predicateBBox($distance_m);
	$distance = distancePostGISSphere('n.geom');

	return "
		SELECT
			n.id AS id
		FROM
			nodes n
			" . ($kvPred != "" ? "INNER JOIN node_tags snt ON (snt.node_id = n.id)" : "") . "
		WHERE
			$kvPred
		 	n.geom && $predicateBBox
			AND $distance < $distance_m
		LIMIT
			1000
	";
}



function findWaysQuery($k, $v, $bOr, $distance_m='$3')
{
	$kvPred = createPredicate('swt', $k, $v, $bOr);
	if($kvPred != "")
		$kvPred .= " AND ";

	$predicateBBox =  predicateBBox($distance_m);
	$distance = distancePostGISSphere('w.linestring');

	return "
		SELECT
			w.id
		FROM
			ways w
			" . ($kvPred != "" ? "INNER JOIN way_tags swt ON(swt.way_id = w.id)" : "") . "
		WHERE
			$kvPred
		 	w.linestring && $predicateBBox
			AND $distance < $distance_m
		LIMIT
			1000
	";
}

/*
function findWaysQuery($k, $v, $bOr, $distance_m='$3')
{
	global $distance;

	$kvPred = createPredicate('swt', $k, $v, $bOr);
	if($kvPred != "")
		$kvPred .= " AND ";

	return "
		SELECT
			wn.way_id AS id
		FROM
			nodes n
			INNER JOIN way_nodes wn ON(wn.node_id = n.id)
			" . ($kvPred != "" ? "INNER JOIN way_tags swt ON(swt.way_id = wn.way_id)" : "") . "
		WHERE
			$kvPred
		 	" . predicateBBox($distance_m) . "
			AND $distance < $distance_m
	";
}
*/


/**
 *
 *
 * /
$entityClassQuery = "
	SELECT
		$tagHead
	FROM
    	elements e
    	INNER JOIN tags       st ON ((st.type, st.id) = (e.type, e.id))
   		INNER JOIN properties sp ON (sp.id = st.k)

    	INNER JOIN tags       t  ON ((t.type, t.id) = (e.type, e.id))
    	INNER JOIN properties p  ON (p.id  = t.k)
	WHERE
    	sp.type   = 'classification' AND
		(sp.k = '$4' OR sp.v = '$4')
		AND " . predicateBBox('1000') . "
	HAVING
		distance < $3
	LIMIT 1000'
";






function wayTagQuery($wayIdVar = '$1')
{
/*
'SELECT CONCAT("base:way/",t.id,"#id") AS id,
				CONCAT(IF(p.type="datatype","","vocabulary:"),IF(v.label LIKE "yes" AND p.type!="datatype",REPLACE(k.label,":","%25"),v.label)) AS "t:unc",
				IF(p.type="classification" OR (p.type!="datatype" AND v.label LIKE "yes"),"rdf:type",
					CONCAT(REPLACE(k.label,":","%25"),IF(p.type="datatype","","->"))
				) a
			FROM tags t INNER JOIN properties p ON(t.k=p.id)
				INNER JOIN resources v ON(v.id=t.v) INNER JOIN resources k ON(k.id=t.k)
			WHERE t.type="way" AND t.id = $1'
* /
	return "
		SELECT
			'base:way/' || t.id || '#id' AS id,
			(
				CASE WHEN p.type = 'datatype' THEN '' ELSE 'vocabulary:' END ||
				CASE WHEN v.label ~* 'yes' AND p.type != 'datatype'
				THEN REPLACE(rk.label, ':', '%25')
				ELSE v.label
				END
			) AS 't:unc',
			(
				CASE WHEN p.type = 'classification' OR p.type != 'datatype' AND rv.label ~* 'yes'
				THEN 'rdf:type'
				ELSE
					REPLACE(rk.label, ':', '%25') ||
					CASE WHEN p.type = 'datatype' THEN '' ELSE '->' END
				END 
			) AS a,
		FROM
			way_tags wt
		WHERE
			wt.way_id = $wayIdVar
	";
}



/*
$distance='ROUND(1000*1.609 * 3956 * 2 * ASIN(SQRT(  POWER(SIN(($1 - latitude/10000000) * pi()/180 / 2), 2) +
		COS($1 * pi()/180) *  COS(latitude/10000000 * pi()/180) *  POWER(SIN(($2 - longitude/10000000) * pi()
		/180 / 2), 2) ) )) AS distance';

$box='longitude between CEIL(($2-($3/1000)/abs(cos(radians($1))*111))*10000000) and CEIL(($2+($3/1000)/abs(cos(radians($1))*111))*10000000)
		AND latitude between CEIL(($1-($3/1000/111))*10000000) and CEIL(($1+($3/1000/111))*10000000)';
$latlon='longitude/10000000 AS "wgs84_pos:long^^xsd:decimal",latitude/10000000 AS "wgs84_pos:lat^^xsd:decimal"';
*/



$nodeWGSQuery = "
		SELECT
			'base:node/' || n.id || '#id' AS id,
			Y(n.geom) AS \"wgs84_pos:lat^^xsd:decimal\",
			X(n.geom) AS \"wgs84_pos:long^^xsd:decimal\"
		FROM
			nodes n
		WHERE n.id IN ($1)
";


$nodeWayMemberQuery = "
		SELECT
			('base:node/' || wn.node_id || '#id') AS id,
			(wn.way_id || '#id') AS \"memberOfWay->way\",
			sequence_id
		FROM
			way_nodes wn
		WHERE
			wn.node_id IN ($1)

";

$wayNodesQuery = "
		SELECT
			('base:way/' || wn.way_id || '#id') AS id,
			(wn.node_id || '#id') AS \"hasNode->node\"
		FROM
			way_nodes wn
		WHERE
			wn.way_id IN ($1)
";	




$triplify['queries'] = array(

	/**
	 *Anything within a certain radius
	 *
	 * near/$lat,$lon/$dist
	 */
	'/^near\/(-?[0-9\.]+),(-?[0-9\.]+)\/([0-9]+)\/?$/'=>
		array(
			str_replace('$1', findNodesQuery(null, null, false), $nodeGeoRSSQuery),
			str_replace('$1', findNodesQuery(null, null, false), $nodeWGSQuery),
			str_replace('$1', findNodesQuery(null, null, false), $nodeTagsQuery),
			str_replace('$1', findNodesQuery(null, null, false), $nodeWayMemberQuery),

			str_replace('$1', findWaysQuery(null, null, false), $wayGeoRSSQuery),
			str_replace('$1', findWaysQuery(null, null, false), $wayTagsQuery),
			str_replace('$1', findWaysQuery(null, null, false), $wayNodesQuery)
		),

	/**
	 * Anything within a certain radius and constrained to a certain key
	 *
	 * near/$lat,$lon/$dist/$prop 
	 */
	'/^near\/(-?[0-9\.]+),(-?[0-9\.]+)\/([0-9]+)\/([^\/=]+)\/?$/'=> 
		array(
			str_replace('$1', findNodesQuery('$4', null, false), $nodeGeoRSSQuery),
			str_replace('$1', findNodesQuery('$4', null, false), $nodeWGSQuery),
			str_replace('$1', findNodesQuery('$4', null, false), $nodeTagsQuery),
			str_replace('$1', findNodesQuery('$4', null, false), $nodeWayMemberQuery),

			str_replace('$1', findWaysQuery('$4', null, false), $wayGeoRSSQuery),
			str_replace('$1', findWaysQuery('$4', null, false), $wayTagsQuery),
			str_replace('$1', findWaysQuery('$4', null, false), $wayNodesQuery)
		),

	/**
	 * Anything within a certain radius and constrained to a certain key-value
	 *
	 * near/$lat,$lon/$dist/$prop=$value
	 */
	'/^near\/(-?[0-9\.]+),(-?[0-9\.]+)\/([0-9]+)\/([^\/=]+)=(.+)\/?$/'=>
		array(
			str_replace('$1', findNodesQuery('$4', '$5', false), $nodeGeoRSSQuery),
			str_replace('$1', findNodesQuery('$4', '$5', false), $nodeWGSQuery),
			str_replace('$1', findNodesQuery('$4', '$5', false), $nodeTagsQuery),
			str_replace('$1', findNodesQuery('$4', '$5', false), $nodeWayMemberQuery),

			str_replace('$1', findWaysQuery('$4', '$5', false), $wayGeoRSSQuery),
			str_replace('$1', findWaysQuery('$4', '$5', false), $wayTagsQuery),
			str_replace('$1', findWaysQuery('$4', '$5', false), $wayNodesQuery)
		),


	/**
	 * Pattern for near/$lon,$lat/$dist/class/$className
	 * (with optional / at end)
	 *
	 */
	'/^near\/(-?[0-9\.]+),(-?[0-9\.]+)\/([0-9]+)\/class\/(.+)\/?$/'=>
		array(
			str_replace('$1', findNodesQuery('$4', '$4', true), $nodeGeoRSSQuery),
			str_replace('$1', findNodesQuery('$4', '$4', true), $nodeWGSQuery),
			str_replace('$1', findNodesQuery('$4', '$4', true), $nodeTagsQuery),
			str_replace('$1', findNodesQuery('$4', '$4', true), $nodeWayMemberQuery),

			str_replace('$1', findWaysQuery('$4', '$4', true), $wayGeoRSSQuery),
			str_replace('$1', findWaysQuery('$4', '$4', true), $wayTagsQuery),
			str_replace('$1', findWaysQuery('$4', '$4', true), $wayNodesQuery)
		),

	'/^node\/(.+)\/?$/'=>array(		
		$nodeGeoRSSQuery,
		$nodeWGSQuery,
		$nodeTagsQuery,
		$nodeWayMemberQuery
	),

	'/^way\/(.+)\/?$/'=>array(
		$wayGeoRSSQuery,
		$wayNodesQuery,
		$wayTagsQuery
	),
);



$triplify['objectProperties']=array(
	'sioc:has_creator'=>'user',
	'rdf:type'=>'',
	'dc:publisher'=>'',
);

$triplify['classMap']=array(
	'user'=>'foaf:person'
);

$triplify['license']='http://creativecommons.org/licenses/by-sa/2.0/';

$triplify['metadata']=array(
	'vocabulary:attribution'=>'This data is derived from information collected by the OpenStreetMap project (http://www.openstreetmap.org).',
	'dc:title'=>'',
	'dc:publisher'=>'AKSW research group (http://aksw.org)'
);

#$triplify['register']=true;

//$triplify['TTL']=36000;
$triplify['TTL']=1;

$triplify['cachedir']='cache/';

$triplify['LinkedDataDepth']='3';

$triplify['CallbackFunctions']=array(
);
?>
