<?php
include('inc.php');


// $tmpZoom currently not used, as the zoom is calculated from left, top,
// right and bottom
function generateValuesHTML($lonMin, $latMax, $lonMax, $latMin, $tmpZoom, $property, $value)
{
	global $db;
	
	$t=microtime(1);



$latDelta = $latMax - $latMin;
$lonDelta = $lonMax - $lonMin;

$tileSizeLat = 180 / 65536.0;
$tileSizeLon = 360 / 65536.0;

for($zoom = 16; $zoom >= 0; --$zoom) {
	$numTilesLat = ceil($latDelta / $tileSizeLat);
	$numTilesLon = ceil($lonDelta / $tileSizeLon);

	$numTilesTotal = $numTilesLat * $numTilesLon;
	if($numTilesTotal <= 64)
		break;

	// Try next zoom level with double size
	$tileSizeLat *= 2;
	$tileSizeLon *= 2;
}

// If there is no tile table/index for that zoom, use next greater zoom level
if($zoom % 2 == 1)
	--$zoom;

/*
$latMin = $_GET['bottom'];
$latMax = $_GET['top'];
$lonMin  = $_GET['left'];
$lonMax = $_GET['right'];
*/

	$exactBox="ST_SetSRID(ST_MakeBox2D(ST_MakePoint($lonMin, $latMin), ST_MakePoint($lonMax, $latMax)), 4326)";
	
/*
	$exactBox='latitude BETWEEN '.$db->escape_string(round($bottom*10000000)).' AND '.$db->escape_string(round($top*10000000)).
		' AND longitude BETWEEN '.$db->escape_string(round($left*10000000)).' AND '.$db->escape_string(round($right*10000000));
*/	

	// Note: We let sqlForArea determine the zoom
	//$tileBox = sqlForArea($bottom, $left, $top, $right, $zoom);
	//$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $zoom);

	
	
	//$propertyPart = $db->escape_string($property);
	$propertyPart = $property;	
	 
	if($zoom >= 14) {
		$tmp = 16;
		$tileBox = sqlForArea($bottom, $left, $top, $right, $tmp);
		$box = "$tileBox AND $exactBox";
		
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
			
			/*
				k IN (
					SELECT
						id
					FROM
						resources
					WHERE
						$exactBox AND
						label = '$propertyPart'
					GROUP BY
						v)";
*/
	}
	else {
		$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $zoom, "tile_id");

		$s =
			"SELECT
				v AS value,
				SUM(usage_count) AS c
			FROM
				lgd_stats_node_tags_tilekv_$zoom
			WHERE
				k = '$propertyPart' AND $tileBox
			GROUP BY
				k, v
			ORDER BY
				v";
	}
	// SUM(c) > 0
	//echo $s;
	
	$result = "";
	foreach($db->query($s) as $r) {
		$result .= '<a class="value" onclick="$(\'.value,.property\').removeClass(\'highlight\'); $(this).addClass(\'highlight\');" href="javascript:{property=\''.$_GET['property'].'\'; value=\''.htmlspecialchars(utf8_encode($r['value'])).'\'; mapEvent();}">'.utf8_encode($r['value']).'&nbsp;('.$r['c'].')</a>&nbsp;| ';
	}

	return $result;
}


echo generateValuesHTML(
	$_GET['left'], $_GET['top'], $_GET['right'], $_GET['bottom'],  $_GET['zoom'], 
	$_GET['property'], utf8_decode($_GET['value']));

	
#echo $s;
#exit;

	
/*
if($zoom>16)
	$s='SELECT vl.label value, count(*) c FROM elements n INNER JOIN tags t USING(id,type) INNER JOIN resources vl ON(v=vl.id)
		WHERE k IN (SELECT id FROM resources WHERE label="'.$db->escape_string($_GET['property']).'") AND '.$box.' GROUP BY v';
else
	$s='SELECT vl.label value, SUM(c) c FROM tilesv'.$zoom.' INNER JOIN resources kl ON(k=kl.id) INNER JOIN resources vl ON(v=vl.id)
		WHERE c>0 AND kl.label="'.$db->escape_string($_GET['property']).'" AND '.$b.
		' GROUP BY k,v';
*/
	
#echo microtime(1)-$t;
?>
