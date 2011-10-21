<ul style="margin-left:-1em; list-style-type:none;">
<?php
//echo "Under maintainance";
//die; 

include('inc.php');

$showAll=false;


$t=microtime(1);


$b=sql_for_area($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right'],$zoom);

//get the zoom
$latMin = $_GET['bottom'];
$latMax = $_GET['top'];
$lonMin  = $_GET['left'];
$lonMax = $_GET['right'];

// AutoZoom: Use the first zoomlevel that does not have more than n tiles
//$viewportWidth = 640
//$viewportHeight = 480

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

//echo "Zoomlevel: $zoom\nx: $numTilesLat\ny: $numTilesLon\ntotal: $numTilesTotal\n";

$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $zoom);




$namePart = implode("', '", $properties);
$filterPart = $showAll
	? ""
	: "AND t.k IN ('$namePart')";


if($zoom >= 14) {
	// If we are zoomed in close, use the finest grained tile box
	//$exactBox='latitude BETWEEN '.$db->escape_string(round($_GET['bottom']*10000000)).' AND '.$db->escape_string(round($_GET['top']*10000000)).
	//	' AND longitude BETWEEN '.$db->escape_string(round($_GET['left']*10000000)).' AND '.$db->escape_string(round($_GET['right']*10000000));
	
	$exactBox="ST_SetSRID(ST_MakeBox2D(ST_MakePoint($lonMin, $latMin), ST_MakePoint($lonMax, $latMax)), 4326)";
	
	$tmp = 16;
	$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $tmp, "LGD_ToTile(t.geom, 16)");


	$box = "$tileBox AND $exactBox";

//			COUNT(DISTINCT v) v,
//			p.ontology_entity_type = 'node' AND 
		
		
	$sql =
		"SELECT
			t.k           property,
			p.owl_entity_type AS type,
			COUNT(*)          c
		FROM
			node_tags t
			INNER JOIN lgd_tag_ontology_k p ON (p.k = t.k)
		WHERE
			t.geom && $exactBox
			$filterPart
		GROUP BY
			t.k, p.owl_entity_type
		ORDER BY
			p.owl_entity_type, t.k";

// Ooops, the subselect belongs to the instance query
/*
	$sql =
		"SELECT
			t2.k           property,
			p.ontology_entity_type AS type,
			COUNT(*)          c
		FROM
			node_tags t2
			INNER JOIN lgd_properties p ON (p.k = t2.k)
		WHERE
			t2.node_id IN (
				SELECT
					t.node_id
				FROM
					node_tags t
				WHERE
					t.geom && $exactBox
					$filterPart
			)
		GROUP BY
			t2.k, p.ontology_entity_type
		ORDER BY
			p.ontology_entity_type, t2.k";
*/


			
//			AND p.ontology_entity_type != NULL

			//INNER JOIN tags       t USING (type, id)
			
		
	#$sql='SELECT k property,COUNT(*) c,COUNT(DISTINCT v) v FROM tag_nodes n INNER JOIN node_tags USING(id)
	#	WHERE '.$box.' AND '.($showAll?'':'k IN ('.join(',',array_keys($pids)).')').' GROUP BY k';
}
else {

/*	$sql="SELECT label property, SUM(c) c FROM tiles$zoom INNER JOIN resources r ON(k=id) WHERE ".
		(!$showAll?'label IN ("'.join('","',$properties).'") AND ':'').$b.
		' GROUP BY k'; */	
	$tileBox = sqlForArea($latMin, $latMax, $lonMin, $lonMax, $zoom, "t.tile_id");
	
	$sql =
		"SELECT
			t.k property,
			p.owl_entity_type AS type,
			SUM(t.usage_count) c
		FROM
			lgd_stats_node_tags_tilek_$zoom t
			INNER JOIN lgd_tag_ontology_k p ON (p.k = t.k)
		WHERE
			$tileBox
			$filterPart
		GROUP BY
			t.k, p.owl_entity_type
		ORDER BY
			p.owl_entity_type, t.k";
}
//echo $sql;	
//echo "Zoom level was $zoom <br />";
#echo $sql;
#echo microtime(1)-$t;

$totalPropertyCount = 0;
$currentType = "";

$selectedProperty = $_GET['property'];

//echo $zoom;
//echo "Debug output";
//echo $sql;
//$res=$db->query($sql);
//print_r($db->error);
//die;

foreach($db->query($sql) as $row) {
	$count = $row['c'];
	
	$totalPropertyCount += $count;
	
	
	$p    = $row['property'];
	$c    = $row['c'];
	//$v    = $row['v'];
	$v = 10;
	$type = $row['type'];
	if(!isset($type))
		$type = "datatype";
 
	if($type != $currentType) {
		if($currentType != "")
			echo "<br />";
		
		switch($type) {
			case "classification":
				echo "<b>Class hierarchy</b> <br />";
				break;
			case "datatype":
				echo "<b>Properties</b> <br />";
				break;
			default:
				echo "<b>$type</b> <br />";
		}

		$currentType = $type;
	}
		
	
	//if(!isset($propData[$type]))
		//$propData[$type] = "<table> <tr> <th> property </th> <th> count </th> ";

	//if(!isset($propData[$type][$p]))

	$values = "values.php?{$_SERVER['QUERY_STRING']}&property=$p";
	
	$toggleIcon = "";
	if(1 || $c > 10 && $c / $v > 2)
		$toggleIcon = "
			<a  style   = 'position:relative; margin-left:-1.5em; width:1.5em;'
				onclick = \"
					$(this).toggleClass('highlight');
					if($('#pd-$p').html())
						$('#pd-$p').slideToggle();
					else {
						$('#pd-$p').html('<img src=loading.gif />');
						$('#pd-$p').load('$values');} \"
			\>[+]</a>&nbsp;"; 
	
	$link = "
		<a class='property'
			onclick=\"
				$('.value,.property').removeClass('highlight');
				$(this).toggleClass('highlight');\"
			href=\"javascript:{property='$p'; value=''; mapEvent();}\">
		$p</a>($c)<br />
		";
	
	$div = "<div id='pd-$p' style='font-size:70%'></div></li>";
	
	
	if($p == $selectedProperty)
		echo "$toggleIcon <b>$link</b> $div ";
	else
		echo "$toggleIcon $link $div ";


	
	
	//$p .= "Test";
	/*
	$u='values.php?'.$_SERVER['QUERY_STRING'].'&property='.$row['property'];
	echo('<li>'.(1 || $row['c']>10 && $row['c']/$row['v']>2?'<a style="position:relative; margin-left:-1.5em; width:1.5em;"
		onclick="$(this).toggleClass(\'highlight\'); if($(\'#pd-'.$p.'\').html()) $(\'#pd-'.$p.'\').slideToggle(); else { $(\'#pd-'.$p.'\').html(\'<img src=loading.gif />\'); $(\'#pd-'.$p.'\').load(\''.$u.'\');}">[+]</a>&nbsp;':'').
		'<a class="property" onclick="$(\'.value,.property\').removeClass(\'highlight\'); $(this).toggleClass(\'highlight\');" href="javascript:{property=\''.$p.'\'; value=\'\'; mapEvent();}">'.$p.'</a> ('.$row['c'].')<br />');
	echo('<div id="pd-'.$p.'" style="font-size:70%"></div></li>');
	* /
	
	$u = "values.php?{$_SERVER['QUERY_STRING']}&property=$p";
	
	//$item =
	//	"<li>"
	
	echo('<li>'.(1 || $row['c']>10 && $row['c']/$row['v']>2?'<a style="position:relative; margin-left:-1.5em; width:1.5em;"
		onclick="$(this).toggleClass(\'highlight\'); if($(\'#pd-'.$p.'\').html()) $(\'#pd-'.$p.'\').slideToggle(); else { $(\'#pd-'.$p.'\').html(\'<img src=loading.gif />\'); $(\'#pd-'.$p.'\').load(\''.$u.'\');}">[+]</a>&nbsp;':'').
		'<a class="property" onclick="$(\'.value,.property\').removeClass(\'highlight\'); $(this).toggleClass(\'highlight\');" href="javascript:{property=\''.$p.'\'; value=\'\'; mapEvent();}">'.$p.'</a> ('.$row['c'].')<br />');
	echo('<div id="pd-'.$p.'" style="font-size:70%"></div></li>');
	*/
	
}

if(isset($selectedProperty) && $selectedProperty != '')
	echo "<br />
			<a class='property'
				onclick=\"
					$('.value,.property').removeClass('highlight');
					$(this).toggleClass('highlight');\"
				href=\"javascript:{property=''; value=''; mapEvent();}\"
			>
				deselect
			</a><br />
		";


/*
	if($totalPropertyCount > 200)
		echo "<script type='text/javascript'>$('#inst').html('Too many estimated visible markers. Please either zoom in or change the facet')</script>";
	else
		echo "<script type='text/javascript'>$('#inst').html('Blubb')</script>";
*/
?>
</ul>
<?echo "Query took " . round((microtime(1)-$t) * 1000) . "ms."?>
