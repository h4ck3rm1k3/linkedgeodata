<script type="text/javascript">
var mk=new Array();
</script>
<?php

// NOTE: BUG DETECTED: when searching for e.g. Leipzig, the form sets a parameter
// and the query will contain a where v="Leipzig"

//echo "Under maintainance - since Mon, May 3";
//die; 

include('inc.php');

$t=microtime(1);



function generateInstanceHTML($latMin, $latMax, $lonMin, $lonMax, $zoom, $property, $value)
{

	global $db;
	global $properties;

	// Currently instances will not be shown if the zoom level is too low
	$minRequiredZoom = 14;
	

	if($zoom < $minRequiredZoom) {
		return
			"Currently no instance information can be provided for zoom levels smaller than $minRequiredZoom.
			This is because usually too many exist.
			In the future the estimated count of instances in an area will be taken into account in order
			to be able to display instances which do not clutter the whole display.
			Current zoom level is $zoom.
			";
	}
	
	$result = "";
	
	//background-color:#b0b0b0;
	$result .= "<ol style='margin-left:-1em;'>";
	
	// Alternating background colors
	$colorClasses = array("color0", "color1");
		
	/**
	 * Build the SQL query
	 * /
	$exactBox='latitude BETWEEN '.$db->escape_string(round($bottom*10000000)).' AND '.$db->escape_string(round($top*10000000)).
		' AND longitude BETWEEN '.$db->escape_string(round($left*10000000)).' AND '.$db->escape_string(round($right*10000000));
	*/

	$b="tile = (CONV(BIN(FLOOR(0.5 + 65535*($left+180)/360)), 4, 10)<<1)
		| CONV(BIN(FLOOR(0.5 + 65535*($bottom+90)/180)), 4, 10)+4
		OR tile = (CONV(BIN(FLOOR(0.5 + 65535*($right+180)/360)), 4, 10)<<1)
		| CONV(BIN(FLOOR(0.5 + 65535*($top+90)/180)), 4, 10)";
	
	//$box = sql_for_area($bottom, $left, $top, $right, $zoom);
	//$result .= $box;
	
	//$result .= "<br /> <br />";
	$tmp = 16;
	$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $tmp, "LGD_ToTile(t.geom, 16)");	
	$zoom = $tmp; // if tmp is not set, sqlForArea will set it
	if($zoom > 16) {
		$zoom = 16;
		$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $zoom);
	}
	//$result .= $box;
	
	$box = "$tileBox AND $exactBox";
	//$box = $exactBox; 
	
	$exactBox = "ST_SetSRID(ST_MakeBox2D(ST_MakePoint($lonMin, $latMin), ST_MakePoint($lonMax, $latMax)), 4326)::geography";
	
	// On close zooms, show everything.
	$limitPart = "LIMIT 300";
	//if($zoom > $minRequiredZoom)
	//	$limitPart = "";
	
		
	//$elements = "instance_tile_k_$zoom";
	$elements = "node_tags";
	
	$propertyPart = "";
	$conditionPart = "";
	
	$valuePart = "";
	if($value) {
		$v = $db->quote($value);
		$conditionPart .= " AND t.v IN($v) ";
	}

	if($property)
		$p = $db->quote($property);
	else
		$p = "'" . implode("', '", $properties) . "'";
			
	$conditionPart .= " AND t.k IN($p)";
	//}

	// IMPORTANT: The USE INDEX(tile) is actually needed because otherwise
	// the query optimizer will think its better to join with tags first
	// (it thinks there are only like 17 records to fetch while there are
	// actually like 25000 when looking for 'place:suburb'- so either the stats
	// need to be updated or on average there are really only 17 records to fetch)
	
	// The DISTINCT is needed because the same instance may appear in
	// multiple classifications
	// update - DISTINCT is no longer needed - we retrieve all nodes and tags at once.
	// however we do a sorting
	//	FROM	$propertyPart
	//			LEFT JOIN `lgd-dbp` dbp ON ((dbp.type, dbp.id) = (e.type, e.id))
//echo "$conditionPart\n";
	$sql =
		"SELECT
			'node' AS type,
			t.node_id AS id,
			Y(t.geom::geometry) AS latitude,
			X(t.geom::geometry) AS longitude,
			t.k AS property,
			t.v AS value,
			NULL AS article
		FROM
			$elements t
		WHERE
			$tileBox 
		$limitPart
		";

	$sql =
		"SELECT
			'node' AS type,
			t2.node_id AS id,
			Y(t2.geom::geometry) AS latitude,
			X(t2.geom::geometry) AS longitude,
			t2.k AS property,
			t2.v AS value,
			NULL AS article
		FROM
			node_tags t2
		WHERE
			t2.node_id IN (
				SELECT
					t.node_id
				FROM
					node_tags t
				WHERE
					t.geom && $exactBox
					$conditionPart
				$limitPart
			)
		";

//					$tileBox 



			//dbp.article
			//ON (e.id = dbp.id AND dbp.type = 'node')
			//ON (dbp.id = e.id AND dbp.type = e.type)

/*

			t.geom && $box
			$conditionPart
*/
//echo "$zoom <br />";
		
//echo "$sql <br />";
//die;
	//$res=$db->query($sql);
	//print_r($db->error);
	//while($row=$res->fetch_assoc()) {
	//echo $sql;
	//echo $box;
	$idToTags = array();
	foreach($db->query($sql) as $row) {
		//$p=$db->query('SELECT rk.label property, rv.label value FROM tags t INNER JOIN resources rk ON(k=rk.id) INNER JOIN resources rv ON(v=rv.id) WHERE t.type="'.$row['type'].'" AND t.id='.$row['id']);
		//print_r($db->error);
		
		$id = $row['id'];
		
		$idToPos[$id]['latitude'] = $row['latitude'];
		$idToPos[$id]['longitude'] = $row['longitude'];
		
		$idToTags[$id][$row['property']] = $row['value'];
	}

	$markerId = 0;
	foreach($idToTags as $nodeId => $pr) {		
		unset($desc,$popdesc,$popform,$name,$ta);

		foreach($pr as $p => $v) {
			$ta.='"'.$p.'":"'.htmlspecialchars(utf8_encode($v)).'",';
		}
	

		if($pr['name']) {
			$desc=$popdesc='<b>'.$pr['name'].'</b><br />';
		}

		if($pr) {
			foreach($pr as $p=>$v) {
				if($p!='name') {
					if($p!='description' && $p!='image' && $p!='source_ref') {
						$autoc.='$(\'#'.$p.'\').autocomplete(\'autocomplete.php\',{extraParams:{p:\''.$p.'\'}});';
					}
				}	
				
				if(in_array($p,$properties))
					$desc.=$p.': '.$v.'<br />';
			}
		}

		$dbpediaPrefix   = "http://dbpedia.org/resource/";
		
		$lonD    = $idToPos[$nodeId]['longitude'];
		$latD    = $idToPos[$nodeId]['latitude'];
	
	//echo "[$latD  $lonD]\n";
	
		$article = null; // TODO $row['article'];
		
		$logo = "";
		
		if(isset($article)) {
			$dbpediaResource = $dbpediaPrefix.$article;
			$ta .= '"dbpedia":"'.htmlspecialchars(utf8_encode($dbpediaResource)).'"';
			
			$logo = "<img src='icon/dbpedia.png' />";
		}	

		$colorClass = $colorClasses[$markerId % 2];
		//$result .= "<li style='background-color:$color;".'id="p'.(++$id).'" onmouseout="mk['.$id.'].events.triggerEvent(\'mouseout\'); $(this).removeClass(\'highlight\');" onmouseover="mk['.$id.'].events.triggerEvent(\'mouseover\'); $(this).addClass(\'highlight\');">'.$logo . " " .$desc.'</li>';

		//style='background-color:$color;'
		// style='background-color:$color;'
		++$markerId;
		$result .=
			"
				<li id='p$markerId' class='$colorClass'
					onmouseout=\"
						mk['$markerId'].events.triggerEvent('mouseout');
						$(this).removeClass('highlight');
					\"
					onclick=\"
						mk['$markerId'].events.triggerEvent('click');
					\"
					onmouseover=\"
						$(this).addClass('highlight');
					\"
				>$logo $desc</li>
			";

		
		$fragment = 
			"<script type = 'text/javascript'>
				mk[$markerId] =
					addMarker(
						new OpenLayers.LonLat($lonD, $latD)
							.transform(map.displayProjection, map.projection),
						$nodeId,
						{{$ta}}
					);
			</script>";

		$result .= $fragment;
	}
	
	$result .= "</ol>";
	
	if($markerId >= 300)
		$result = "Currently at most $markerId instances will be shown. Some results have been omitted. <br />" . $result;
	
	return $result;
}


echo generateInstanceHTML(
	$_GET['bottom'], $_GET['top'], $_GET['left'], $_GET['right'],
	$_GET['zoom'], 
	$_GET['property'], utf8_decode($_GET['value']));

// Once all instances/markers have been added, update the export button
echo 
	"<script type = 'text/javascript'>
		updateExport();
	</script>";

/*
$box = sql_for_area(
	$_GET['bottom'],
	$_GET['left'],
	$_GET['top'],
	$_GET['right'],
	$zoom);
*/
//echo $box;
/*
Original query with dbpedia 
$sql='SELECT e.id,longitude,latitude,e.type, dbp.article FROM elements e LEFT JOIN `lgd-dbp` dbp USING (id)'.
	($_GET['property']?'INNER JOIN tags t USING(type,id) INNER JOIN resources kr ON(k=kr.id AND kr.label="'.$db->escape_string($_GET['property']).'")'.
	($_GET['value']?' INNER JOIN resources vr ON(v=vr.id AND vr.label="'.$db->escape_string(utf8_decode($_GET['value'])).'")':''):'').
	'WHERE '.$box.' LIMIT 20';

Original query
$sql='SELECT e.id,longitude,latitude,e.type, dbp.article FROM elements e '.
	($_GET['property']?'INNER JOIN tags t USING(type,id) INNER JOIN resources kr ON(k=kr.id AND kr.label="'.$db->escape_string($_GET['property']).'")'.
	($_GET['value']?' INNER JOIN resources vr ON(v=vr.id AND vr.label="'.$db->escape_string(utf8_decode($_GET['value'])).'")':''):'').
	'LEFT JOIN `lgd-dbp` dbp ON (e.id = dbp.id) '.
	'WHERE '.$box.' LIMIT 20';
* /


$propertyPart = "";
$conditionPart = "";
if($_GET['property']) {

	$valuePart = "";
	if($_GET['value']) {
		$v = $db->escape_string(utf8_decode($_GET['value']));
		$valuePart = "
			INNER JOIN resources vr ON (v = vr.id)";

		$conditionPart .= " AND vr.label = '$v'";
	}

	$p = $db->escape_string($_GET['property']);
	
	$propertyPart = "
		INNER JOIN tags t USING(type, id)
		INNER JOIN resources kr ON (k = kr.id) 
		$valuePart";
	
	$conditionPart .= " AND kr.label = '$p'";
}

$sql =
	"SELECT
		e.id,
		e.longitude,
		e.latitude,
		e.type,
		dbp.article
	FROM
		elements e
		$propertyPart
		LEFT JOIN `lgd-dbp` dbp ON (e.id = dbp.id)
	WHERE
		$box
		$conditionPart
	LIMIT
		20";
/*
$sql =
	"SELECT
		e.id,
		e.longitude,
		e.latitude,
		e.type
	FROM
		elements e
		$propertyPart
	WHERE
		$box
		$conditionPart
	LIMIT
		20";

$sql='SELECT e.id,longitude,latitude,e.type, dbp.article FROM elements e '.
	($_GET['property']?'INNER JOIN tags t USING(type,id) INNER JOIN resources kr ON(k=kr.id AND kr.label="'.$db->escape_string($_GET['property']).'")'.
	($_GET['value']?' INNER JOIN resources vr ON(v=vr.id AND vr.label="'.$db->escape_string(utf8_decode($_GET['value'])).'")':''):'').
	'LEFT JOIN `lgd-dbp` dbp ON (e.id = dbp.id) '.
	'WHERE '.$box.' LIMIT 20';
*/		
/*
$sql =
	"SELECT
		e.id,
		e.longitude,
		e.latitude,
		e.type
	FROM
		elements e
		$propertyPart
	WHERE
		$box
		$conditionPart
	LIMIT
		20";
* /

		
//echo $sql;
$res=$db->query($sql);
print_r($db->error);
while($row=$res->fetch_assoc()) {
	unset($pr,$desc,$popdesc,$popform,$name,$ta);
	$p=$db->query('SELECT rk.label property, rv.label value FROM tags t INNER JOIN resources rk ON(k=rk.id) INNER JOIN resources rv ON(v=rv.id) WHERE t.type="'.$row['type'].'" AND t.id='.$row['id']);
	print_r($db->error);
	while($r=$p->fetch_assoc()) {
		$pr[$r['property']]=utf8_encode($r['value']);
		$ta.='"'.$r['property'].'":"'.htmlspecialchars(utf8_encode($r['value'])).'",';
	}
	if($pr['name'])
		$desc=$popdesc='<b>'.$pr['name'].'</b><br />';
	if($pr)
		foreach($pr as $p=>$v) if($p!='name') {
			if($p!='description' && $p!='image' && $p!='source_ref') {
				$autoc.='$(\'#'.$p.'\').autocomplete(\'autocomplete.php\',{extraParams:{p:\''.$p.'\'}});';
			}
			if(in_array($p,$properties))
				$desc.=$p.': '.$v.'<br />';
		}

		//echo("$sql<br />");

	/*
	echo('<script type="text/javascript">
mk['.$id.']=addMarker(new OpenLayers.LonLat('.($row['longitude']/10000000).','.($row['latitude']/10000000).').transform(map.displayProjection,map.projection),renderNode(\''.$row['id'].'\',\''.($row['longitude']/10000000).'\',\''.($row['latitude']/10000000).'\',{'.$ta.'},\''.$row['type'].'\'));
</script>');
* /
	/*
	$x = '<script type="text/javascript">
mk['.$id.']=addMarker(new OpenLayers.LonLat('.($row['longitude']/10000000).','.($row['latitude']/10000000).').transform(map.displayProjection,map.projection),renderNode(\''.$row['id'].'\',\''.($row['longitude']/10000000).'\',\''.($row['latitude']/10000000).'\',{'.$ta.'},\''.$row['type'].'\'));
</script>';
	
	echo htmlentities($x);
* /
	
	$dbpediaPrefix   = "http://dbpedia.org/resource/";
	
	$nodeId  = $row['id'];
	$latD    = $row['longitude'] / 10000000;
	$lonD    = $row['latitude']  / 10000000;

	$article = $row['article'];
	
	$logo = "";
	
	if(isset($article)) {
		$dbpediaResource = $dbpediaPrefix.$article;
		$ta .= '"dbpedia":"'.htmlspecialchars(utf8_encode($dbpediaResource)).'"';
		
		$logo = "<img src='icon/dbpedia.png' />";
	}	
	
	echo('<li id="p'.(++$id).'" onmouseout="mk['.$id.'].events.triggerEvent(\'mouseout\'); $(this).removeClass(\'highlight\');" onmouseover="mk['.$id.'].events.triggerEvent(\'mouseover\'); $(this).addClass(\'highlight\');">'.$logo . " " .$desc.'</li>');

	
	//$listItem =
		//"<li id='p$id' onmouseout="mk[$id].events.triggerEvent(\'mouseout\');"
	
	
	//Jesewitz
	//GaststÃ¤tte "ReuÃischer Hof"
	/*
	$fragment = 
		"<script type = 'text/javascript'>
			mk[$id] =
				addMarker(
					new OpenLayers.LonLat($latD, $lonD)
						.transform(map.displayProjection, map.projection),
					renderNode('$nodeId', '$latD', '$lonD', {{$ta}})
				);
		</script>";
* /
	$fragment = 
		"<script type = 'text/javascript'>
			mk[$id] =
				addMarker(
					new OpenLayers.LonLat($latD, $lonD)
						.transform(map.displayProjection, map.projection),
					$nodeId,
					{{$ta}}
				);
		</script>";
	
	//echo htmlentities($fragment);
	echo $fragment;
}
*/
/*
$dbpediaSparql = new SPARQLEndpoint("http://dbpedia.org/sparql", "http://dbpedia.org");
$queryResult = $dbpediaSparql->executeQuery("Select * {?s ?p ?o} Limit 10");
print_r($queryResult);
*/
?>
<?echo "Query took " . round((microtime(1)-$t) * 1000) , "ms."?>
