<ul style="margin-left:-1em; list-style-type:none;">
<?php
include('inc.php');

$showAll=false;


$t=microtime(1);


//$b=sql_for_area($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right'],$zoom);

//get the zoom
$tileBox = sqlForArea($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right'],$zoom);

$namePart = implode("', '", $properties);
$filterPart = $showAll
	? ""
	: "AND label IN ('$namePart')";


if($zoom >= 14) {
	// If we are zoomed in close, use the finest grained tile box
	$exactBox='latitude BETWEEN '.$db->escape_string(round($_GET['bottom']*10000000)).' AND '.$db->escape_string(round($_GET['top']*10000000)).
		' AND longitude BETWEEN '.$db->escape_string(round($_GET['left']*10000000)).' AND '.$db->escape_string(round($_GET['right']*10000000));
	
	$tmp = 16;
	$tileBox = sqlForArea($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right'], $tmp);


	$box = "$tileBox AND $exactBox";

//			COUNT(DISTINCT v) v,
		
	$sql =
		"SELECT
			r.label           property,
			COUNT(*)          c,
			p.type
		FROM
			elements n
			INNER JOIN tags       t ON (t.id = n.id AND t.type = n.type)
			INNER JOIN resources  r ON (r.id = t.k)
			INNER JOIN properties p ON (p.id = t.k)
		WHERE
			$box
			AND n.type = 'node'
			$filterPart
		GROUP BY
			t.k
		ORDER BY
			p.type, r.label";
			

			//INNER JOIN tags       t USING (type, id)
			
		
	#$sql='SELECT k property,COUNT(*) c,COUNT(DISTINCT v) v FROM tag_nodes n INNER JOIN node_tags USING(id)
	#	WHERE '.$box.' AND '.($showAll?'':'k IN ('.join(',',array_keys($pids)).')').' GROUP BY k';
}
else {
/*	$sql="SELECT label property, SUM(c) c FROM tiles$zoom INNER JOIN resources r ON(k=id) WHERE ".
		(!$showAll?'label IN ("'.join('","',$properties).'") AND ':'').$b.
		' GROUP BY k'; */	
	$tileBox = sqlForArea($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right'], $zoom);
	
	$sql =
		"SELECT
			r.label property,
			SUM(c) c,
			p.type
		FROM
			tiles$zoom t
			INNER JOIN resources  r ON (r.id = t.k)
			INNER JOIN properties p ON (p.id = t.k)
		WHERE
			$tileBox
			$filterPart
		GROUP BY
			t.k
		ORDER BY
			p.type, r.label";
}
//echo $sql;	
//echo "Zoom level was $zoom <br />";
#echo $sql;
#echo microtime(1)-$t;
$res=$db->query($sql);
print_r($db->error);

$totalPropertyCount = 0;


$currentType = "";

$selectedProperty = $_GET['property'];

while($row=$res->fetch_assoc()) {
	$count = $row['c'];
	
	$totalPropertyCount += $count;
	
	
	$p    = $row['property'];
	$c    = $row['c'];
	//$v    = $row['v'];
	$v = 10;
	$type = $row['type'];
 
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