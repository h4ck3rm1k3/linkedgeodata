<?php
error_reporting(E_ALL ^ E_NOTICE);

// TODO: read values from config.ini
//$db=new mysqli('..');
$db=new PDO('pgsql:host=localhost;dbname=lgd', 'postgres', '8Dd7CLCWkA');

$properties=array('place', 'aeroway','aerialway','amenity','cuisine','denomination','highway','historic','leisure','man_made','military','natural','power','railway','religion','shop','sport','tourism','waterway');


function tileToXY($tile)
{
	$bin = decbin($tile);

	$bits = strrev($bin);
	
	$x = "";
	$y = "";
	
	for($i = 0; $i < strlen($bits); ++$i) {
		$bit = substr($bits, $i, 1);
		
		if($i % 2 == 0)
			$y = $bit . $y;
		else
			$x = $bit . $x;
	}

	$x = bindec($x);
	$y = bindec($y);
	
	return array('x' => $x, 'y' => $y);
}


/**
 * Normalizes a longitude value in the range [-180 .. 180] to [0 .. 1] 
 *
 * @param unknown_type $lon
 * @return unknown
 */
function lonNormX($lon)
{
	return ($lon + 180.0) / 360.0;
}

/**
 * Normalizes a latitude value in the range [-90 .. 90] to [0 .. 1]
 *
 * @param unknown_type $lon
 * @return unknown
 */
function latNormY($lat)
{
	return ($lat + 90.0) / 180.0;
}


function lonInt($lon)
{
	return (int)(10000000 * $lon);
}

function latInt($lat)
{
	return (int)(10000000 * $lat);
}


/**
 * Returns the tile id (an integer value) for the given tile indices.
 * 
 *
 * @param unknown_type $x
 * @param unknown_type $y
 * @param unknown_type $zoom
 * @return unknown
 */
function xyToTile($x, $y, $zoom = 16) {
	$bx = strrev(decbin($x));
	$by = strrev(decbin($y));
	$r = "";
	
	$l = max(array(strlen($bx), strlen($by)));
	
	for($i = 0; $i < $l; $i++) {
		$vx = substr($bx, $i, 1) ? 1 : 0;
		$vy = substr($by, $i, 1) ? 1 : 0;

		//$r = $vy . $vx . $r;
		$r = $vx . $vy . $r;
	}
	return bindec($r);
}

function tilesForArea($latMin, $latMax, $lonMin, $lonMax, $zoom = 16)
{
	$f = pow(2, $zoom); // - 1; removed the -1 -- claus
	
	//echo "$f\n";
	/**
	 * Transform the given geo-coordinates into tile coordinates.
	 */
	$min = llToXY($lonMin, $latMin, $zoom);
	$max = llToXY($lonMax, $latMax, $zoom);
	
	$minX = $min['x'];
	$minY = $min['y'];
	
	$maxX = $max['x'];
	$maxY = $max['y'];
	
	//echo "$minLon $minLat $maxLon $maxLat\n";
	//echo "$minX $maxX $minY $maxY\n";
	$tiles = Array();

	for($x = $minX; $x <= $maxX; $x++) {
		for($y = $minY; $y <= $maxY; $y++) {
			$tiles[] = xyToTile($x, $y);
		}
	}
#print_r($tiles);
	return $tiles;
}

function sqlForArea($latMin, $latMax, $lonMin, $lonMax, &$zoom, $colName = "tile") {
	// changed to 65536 - claus
	$zoom=$zoom?$zoom:2+max(
		(16-max(0,min(2*round(0.5*log(($latMax+90-($latMin+90))*65536/180,2)),16))),
		(16-max(0,min(2*round(0.5*log(($lonMax+180-($lonMin+180))*65536/360,2)),16)))
	);

	$tiles=tilesForArea($latMin, $latMax, $lonMin, $lonMax, $zoom);
	#return "tile IN(".join(',',$tiles).")";
	sort($tiles);
	foreach($tiles as $tile) {
		if(!$start) {
			$start=$last=$tile;
			continue;
		}
		if((substr($tile,0,-1)!=substr($last,0,-1) || substr($tile,-1)!=substr($last,-1)+1)
			&& (substr($tile,0,-2)!=substr($last,0,-2) || substr($tile,-2)!=substr($last,-2)+1)
			&& (substr($tile,0,-3)!=substr($last,0,-3) || substr($tile,-3)!=substr($last,-3)+1)
			&& (substr($tile,0,-4)!=substr($last,0,-4) || substr($tile,-4)!=substr($last,-4)+1)
			&& (substr($tile,0,-5)!=substr($last,0,-5) || substr($tile,-5)!=substr($last,-5)+1)
			&& (substr($tile,0,-6)!=substr($last,0,-6) || substr($tile,-6)!=substr($last,-6)+1)
		) {
			$sql.=($start==$last?" OR $colName=".$last:" OR $colName BETWEEN $start AND $last");
			$start=$tile;
		}
		$last=$tile;
	}
	$sql.=($start==$last?" OR $colName=".$last:" OR $colName BETWEEN $start AND $last");
	return '(FALSE'.$sql.')';
}

function llToXYother($lon, $lat, $zoom)
{
	$f = pow(2, $zoom) - 1;
	
	$x = floor(lonNormX($lon) * $f);
	$y = floor(latNormY($lat) * $f);
	
	return array('x' => $x, 'y' => $y);
}


function llToXY($lon, $lat, $zoom)
{
	$f = pow(2, $zoom) - 1;

	$x = round(lonNormX($lon) * $f);
	$y = round(latNormY($lat) * $f);
	
	return array('x' => $x, 'y' => $y);
}

function llToTile($lon, $lat, $zoom)
{
	$xy = llToXY($lon, $lat, $zoom);
	return xyToTile($xy['x'], $xy['y']);
}


function tile_for_xy($x,$y,$zoom=16) {
	$bx=decbin($x);
	$by=decbin($y);
	for($i=0;$i<$zoom;$i++)
		$r.=substr($bx,$i,1).substr($by,$i,1);
	return bindec($r);
}

function tile_for_point($lat,$lon,$zoom=16) {
	$f=pow(2,$zoom); //removed -1 --claus
	return tile_for_xy(round(($lon+180)*$f/360.0),round(($lat+90)*$f/180.0),$zoom);
}

function tiles_for_area($minlat, $minlon, $maxlat, $maxlon, $zoom=16) {
	$f=pow(2,$zoom); //removed - 1
	$minx=round(($minlon + 180) * $f / 360.0);
	$maxx=round(($maxlon + 180) * $f / 360.0);
	$miny=round(($minlat + 90 ) * $f / 180.0);
	$maxy=round(($maxlat + 90 ) * $f / 180.0);
#print "$minx $maxx $miny $maxy";
	$tiles=Array();

	for ($x=$minx+1; $x<$maxx; $x++) {
		for ($y=$miny+1; $y<$maxy; $y++) {
			$tiles[]=tile_for_xy($x,$y);
		}
	}
#print_r($tiles);
	return $tiles;
}

/*
function getDbZoom($zoom)
{
	$zoom=$zoom?$zoom:2+max(
		(16-max(0,min(2*round(0.5*log(($maxlat+90-($minlat+90))*65535/180,2)),16))),
		(16-max(0,min(2*round(0.5*log(($maxlon+180-($minlon+180))*65535/360,2)),16)))
	);
	
	return $zoom;
}*/

function sql_for_area($latMin, $latMax, $lonMin, $lonMax, &$zoom) {

	$zoom=$zoom?$zoom:2+max(
		(16-max(0,min(2*round(0.5*log(($latMax+90-($latMin+90))*65536/180,2)),16))),
		(16-max(0,min(2*round(0.5*log(($lonMax+180-($lonMin+180))*65536/360,2)),16)))
	);

	$tiles=tiles_for_area($minlat, $minlon, $maxlat, $maxlon,$zoom);
	#return "tile IN(".join(',',$tiles).")";
	sort($tiles);
	foreach($tiles as $tile) {
		if(!$start) {
			$start=$last=$tile;
			continue;
		}
		if((substr($tile,0,-1)!=substr($last,0,-1) || substr($tile,-1)!=substr($last,-1)+1)
			&& (substr($tile,0,-2)!=substr($last,0,-2) || substr($tile,-2)!=substr($last,-2)+1)
			&& (substr($tile,0,-3)!=substr($last,0,-3) || substr($tile,-3)!=substr($last,-3)+1)
			&& (substr($tile,0,-4)!=substr($last,0,-4) || substr($tile,-4)!=substr($last,-4)+1)
			&& (substr($tile,0,-5)!=substr($last,0,-5) || substr($tile,-5)!=substr($last,-5)+1)
			&& (substr($tile,0,-6)!=substr($last,0,-6) || substr($tile,-6)!=substr($last,-6)+1)
		) {
			$sql.=($start==$last?" OR tile=".$last:" OR tile BETWEEN $start AND $last");
			$start=$tile;
		}
		$last=$tile;
	}
	$sql.=($start==$last?" OR tile=".$last:" OR tile BETWEEN $start AND $last");
	return '(0'.$sql.')';
}



function Lon2X($Long){
	return ($Long + 180) / 360;
}
function Lat2Y($Lat){
  $LimitY = ProjectF(85.0511);
  $Y = ProjectF($Lat);
  $PY = ($LimitY - $Y) / (2 * $LimitY);
  return($PY);
}
function ProjectF($Lat){
  $Lat = deg2rad($Lat);
  $Y = log(tan($Lat) + (1/cos($Lat)));
  return($Y);
}
?>
