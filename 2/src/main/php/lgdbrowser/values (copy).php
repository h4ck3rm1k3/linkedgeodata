<?php
include('inc.php');


// $tmpZoom currently not used, as the zoom is calculated from left, top,
// right and bottom
function generateValuesHTML($left, $top, $right, $bottom, $tmpZoom, $property, $value)
{
	global $db;
	
	$t=microtime(1);
	
	$exactBox='latitude BETWEEN '.$db->escape_string(round($bottom*10000000)).' AND '.$db->escape_string(round($top*10000000)).
		' AND longitude BETWEEN '.$db->escape_string(round($left*10000000)).' AND '.$db->escape_string(round($right*10000000));
	

	// Note: We let sqlForArea determine the zoom
	$tileBox = sqlForArea($bottom, $left, $top, $right, $zoom);
	
	
	$propertyPart = $db->escape_string($property);
		
	 
	if($zoom >= 14) {
		$tmp = 16;
		$tileBox = sqlForArea($bottom, $left, $top, $right, $tmp);
		$box = "$tileBox AND $exactBox";
		
		$s =
			"SELECT
				vl.label value,
				count(*) c
			FROM
				elements n
				INNER JOIN tags       t USING (id, type)
				INNER JOIN resources vl ON (vl.id = t.v)
				INNER JOIN resources  r ON (r.id = t.k)
			WHERE
				$box AND
				r.label = '$propertyPart' AND
				n.type ='node'
			GROUP BY
				t.v
			ORDER BY
				vl.label
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
	else
		$s =
			"SELECT
				vl.label value,
				SUM(c) c
			FROM
				tilesv$zoom
				INNER JOIN resources kl ON (k = kl.id)
				INNER JOIN resources vl ON (v = vl.id)
			WHERE
				c > 0 AND kl.label = '$propertyPart' AND $tileBox
			GROUP BY
				k, v
			ORDER BY
				vl.label";
	
	//echo $s;
	//return "";

	$p=$db->query($s);
	print_r($db->error);
	
	$result = "";
	while($r=$p->fetch_assoc())
		$result .= '<a class="value" onclick="$(\'.value,.property\').removeClass(\'highlight\'); $(this).addClass(\'highlight\');" href="javascript:{property=\''.$_GET['property'].'\'; value=\''.htmlspecialchars(utf8_encode($r['value'])).'\'; mapEvent();}">'.utf8_encode($r['value']).'&nbsp;('.$r['c'].')</a>&nbsp;| ';

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