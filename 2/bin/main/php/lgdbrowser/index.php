<?
error_reporting(E_ALL ^ E_NOTICE);
session_start();
if($_POST['user']) {
	/**
	 * Login works by attempting to retrieve the user profile using the
	 * given credentials.
	 */
	//echo "http://{$_POST['user']}:{$_POST['pass']}@api.openstreetmap.org/api/0.6/user/preferences";
	//http://api.openstreetmap.org/login?referer=&user[email]=test&user[password]=test8&commit=Login
	$s=@file_get_contents('http://'.$_POST['user'].':'.$_POST['pass'].'@api.openstreetmap.org/api/0.6/user/preferences');
	if($s) {
		$_SESSION['user']=$_POST['user'];
		$_SESSION['pass']=$_POST['pass'];
	} else {
		echo('<p>Wrong username/password combination. Please retry, <a href="http://www.openstreetmap.org/user/forgot-password">recover your password</a> or <a href="http://www.openstreetmap.org/user/new">register as a new user</a>!</p>');
	}
	echo $s;
}
if($_REQUEST['logout']) {
	session_destroy();
	unset($_SESSION);
}

$prop = $_GET['prop'];
$val = $_GET['val'];

?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta http-equiv="content-type" content="text/html; charset=UTF-8">

  <title>LinkedGeoData Browser</title>
<script type="text/javascript" src="jquery-1.2.6.min.js"></script>
<link rel="stylesheet" href="jquery-autocomplete/jquery.autocomplete.css" type="text/css" />
<script type="text/javascript" src="jquery-autocomplete/lib/jquery.bgiframe.min.js"></script>
<script type="text/javascript" src="jquery-autocomplete/lib/jquery.dimensions.js"></script>
<script type="text/javascript" src="jquery-autocomplete/jquery.autocomplete.js"></script>

<script type="text/javascript" src="http://openlayers.org/api/OpenLayers.js"></script>
<script type="text/javascript" src="http://www.openstreetmap.org/openlayers/OpenStreetMap.js"></script>

<script type="text/javascript" src="jquery-qtip-1.0.0-rc3151959/jquery.qtip-1.0.0-rc3.min.js"></script> 

<script type="text/javascript" src="sparql.js"></script>
<script type="text/javascript" src="script.js"></script>

<!-- xml to json -->
<script type="text/javascript" src="x2j.js"></script>


<link rel="stylesheet" type="text/css" href="styles.css" />

</head>
<body>

<div id='header' class='header'>

	<table>
		<tr>
			<td>
	
				<img style='float:left;' src='lgd-logo-big.png' />
			
				<!--  
				<span style="font-size: 250%; afont-weight:bold; color:#999; margin-left:40px">Linked<span style="font-weight:bold">GeoData</span>.org</span>
			  	<div id="tagline" style="font-weight:bold;margin-left:40px;">Adding a spatial dimension to the Web of Data.</div>
				-->
				<p>This <b>faceted Linked Geo Data browser</b> is based on data obtained 
				from the <a href="http://www.openstreetmap.org">OpenStreetMap project</a> (released under <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>) and was developed by <a href="http://aksw.org">AKSW research group</a>.</p>
			</td>
			

		</tr>
	</table>
</div>


<div id='area' style='position:relative; width:30%; height:80%; float:left;'>
	<div id='search-area' style='position:absolute; top:0px; left:0px; width:100%; height:100%; float:left;'>
		<div style='width:100%; height:100%; float:left;'>
			<div class='map-bar'>
				<table style='width:100%'>
					<tr>
						<td style='text-align:left'><b>Search results</b></td>
						<td style='text-align:right'><a id='search-toggle' href='#' onclick="$('#search-area').hide(); $('#facet-area').show(); return false;"><img class='noborder' src='facet-icon.png'></a></td>
					</tr>
				</table>
			</div>
			<div id="search-result" style='height:100%; overflow:auto;'>
			</div>
		</div>
	</div>

	<div id='facet-area' style='position:absolute; top:0px; left:0px; width:100%; height:100%; float:left'>
	
		<div style='width:50%; height:100%; float:left;'>
			<div class='map-bar'>
				<b>Facets<b/>
			</div>
			<div id="prop" style='height:100%; overflow:auto;'></div>
		</div>
		
		<div style='width:50%; height:100%; float:left;'>
			<div class='map-bar'>
				<table style='width:100%'>
					<tr>
						<td style='text-align:left'><b>Instances</b></td>
						<td style='text-align:right'><a id='facet-toggle' href='#' onclick="$('#facet-area').hide(); $('#search-area').show(); return false;"><img class='noborder' src='search-icon.png'></a></td>
					</tr>
				</table>
			</div>
			<div id='inst' style='height:100%; overflow:auto;'></div>
		</div>
		
	</div>
</div>

<div style='position:relative; width:70%; height:100%; float:left;'>
		<div id="map-bar" class='map-bar'> <!--  style="height:100px;"> -->
			<table style="width:100%;">
				<tr>
					<td style='text-align:left; width:50%;'>
						<div><form style='float:left' action='javascript: doSearch();'><label for="search"><b>Search:</b></label>
						<input type="text" id="search-field" name="search-field" value="<?=$_GET['search']?>" autocomplete="off" aonchange="this.form.submit();" /></form></div>
						powered by <a class='link' href='http://gazetteer.openstreetmap.org/namefinder/'><img class='noborder' src='osm-logo-small.png' />Namefinder</a>
					</td>
					<td style='text-align:right; width:50%;'>
						<a id='map-link' class="link" href="#"><img class='noborder' src='icon/permalink.png' />Link</a>
						<span style='color:#a0a0a0'>|</span>
						<a id='rdf-export' class="link" href="#"><img class='noborder' src='icon/rdf-export.png' />RDF-Export</a>
					</td>
				</tr>
			</table>
		</div>
	
		<div style='position:relative; width:100%; height:100%;'>
			<div style="position:absolute; top:0px; left:0px; z-index:2; width:100%; height:4px; background-image: url(shadowx.png);"></div>	
			<div style="position:absolute; top:0px; left:0px; z-index:1; width:4px; height:100%; background-image: url(shadow2x.png);"></div>	
			<div style="position:absolute; top:0px; left:0px; z-index:0; width:100%; height:80%;" id="map"></div>
		</div>
</div>



<script type="text/javascript" defer="defer">
/* <![CDATA[ */

             
$('#search-area').hide();
             
$('#map-link').qtip({
      content: {
         text: "This link contains your current map settings"
      },
      position: {
          corner: {
             tooltip: 'bottomRight', // Use the corner...
             target: 'topLeft' // ...and opposite corner
          }
       },
       style: 'cream' // Give it some style
   });

$('#rdf-export').qtip({
    content: {
       text: "Create an RDF export of all visible markers"
    },
    position: {
        corner: {
           tooltip:'bottomRight', // Use the corner...
           target:'topLeft' // ...and opposite corner
        }
     },
    style: 'cream' // Give it some style
 });

$('#facet-toggle').qtip({
    content: {
       text: "Switch view to search results"
    },
    style: 'cream' // Give it some style
 });

$('#search-toggle').qtip({
    content: {
       text: "Switch view to facets"
    },
    style: 'cream' // Give it some style
 });


function doSearch(form)
{	
	$('#facet-area').hide();
	$('#search-area').show();


	value = encodeURI($('#search-field').val());

	$('#search-result').html("<img src='loading.gif' />");
	$('#search-result').load("search_proxy.php?find=" + value);	
}

$('#search').autocomplete('autocomplete.php',{extraParams:{p:'name'}});

var property="<?=$prop?>",value="<?=$val?>";

var loggedin=<?=($_SESSION['user']?'true':'false')?>;

map = new OpenLayers.Map('map', {
	maxExtent: new OpenLayers.Bounds(-20037508.34,-20037508.34,20037508.34,20037508.34),
	controls: [
		new OpenLayers.Control.MouseDefaults(),
//		new OpenLayers.Control.LayerSwitcher(),
		new OpenLayers.Control.PanZoomBar(),
		new OpenLayers.Control.MousePosition(),
//		new OpenLayers.Control.OverviewMap(),
		new OpenLayers.Control.ScaleLine(),
	],
	numZoomLevels: 19,
	maxResolution: 156543.0399,
	units: 'm',
	projection: new OpenLayers.Projection("EPSG:900913"),
	displayProjection: new OpenLayers.Projection("EPSG:4326"),
	eventListeners: {"moveend": mapEvent,"zoomend": mapEvent,"click":addNode}
	});

var layerMapnik = new OpenLayers.Layer.OSM.Mapnik("Mapnik");
//var layerTah = new OpenLayers.Layer.OSM.Osmarender("Tiles@Home");
var layerMarkers = new OpenLayers.Layer.Markers("Address", { projection: new OpenLayers.Projection("EPSG:4326"), visibility: true, displayInLayerSwitcher: false });

map.addLayers([layerMapnik,layerMarkers]);

<?php

/*
if($_GET['search']) {
	include('inc.php');
	$sql='SELECT * FROM tags INNER JOIN elements USING(id) WHERE
		k=(SELECT id FROM resources WHERE label="name" LIMIT 1) AND v=(SELECT id FROM resources WHERE label="'.$db->escape_string($_GET['search']).'" LIMIT 1) LIMIT 1';
	#echo $sql;
	$res=$db->query($sql);
}
if($_GET['search'] && $row=$res->fetch_assoc()) {
	$lon=$row['longitude']/10000000;
	$lat=$row['latitude']/10000000;
} else {
	$lon='13.733333';
	$lat='51.033333'; 
}
*/
	$lon='13.733333';
	$lat='51.033333'; 
	$zoom = 12;

$xlat  = $_GET['lat'];
$xlon  = $_GET['lon'];
$xzoom = $_GET['zoom'];

if(isset($xlat))
	$lat = $xlat;
	
if(isset($xlon))
	$lon = $xlon;
 
if(isset($xzoom))
	$zoom = $xzoom;

 //"$lat $lon $zoom";
?>
var center=new OpenLayers.LonLat(<?=$lon?>,<?=$lat?>).transform(map.displayProjection,map.projection);
map.setCenter(center,<?=$zoom?>);

var size = new OpenLayers.Size(21,25);
var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
var icon = new OpenLayers.Icon('http://www.openstreetmap.org/openlayers/img/marker.png',size,offset);
//addMarker(center,'<?=$_GET['search']?>')
/* ]]> */
</script>
<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
try {
var pageTracker = _gat._getTracker("UA-1095975-8");
pageTracker._trackPageview();
} catch(err) {}
</script>
</body>
</html>