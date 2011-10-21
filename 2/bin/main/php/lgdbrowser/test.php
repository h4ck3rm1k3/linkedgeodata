<?php

include('inc.php');


// Note: last 2 least significant bits are: xy
// so it ends on y which is the latitude
function tileToLL($tile, $zoom)
{
	$xy = tileToXY($tile);
	$x = $xy['x'];
	$y = $xy['y'];
	
	$f = pow(2, $zoom) - 1;
	
	$x = $x / $f * 360 - 180;
	$y = $y / $f * 180 - 90;
	
	return array('x' => $x, 'y' => $y);
}

print_r(tileToLL(3502434551, 16));
echo "\n";

$zoom = 6;
for($i = 0; $i < 18; ++$i) {
	echo "i = $i\n";
	print_r(llToTile(13.9629011, 51.0185816, $i));
	echo "\n";
	print_r(llToTile(51.0185816, 13.9629011, $i));
	echo "\n";
	echo "\n";
}
//print_r(tileToLL(xyToTile(520, 704), 10));
print_r(tileToLL(llToTile(-19, 34, 10), 10));
//echo decbin(1023);

//$t = tilesForArea(12.078174003482, 50.4141319666, 13.078174004482, 53.4141320666, $zoom);

//$t = tilesForArea(0, 0, 1, 1, $zoom);
//print_r($t);
/*
foreach($t as $item) {
	print_r(tileToLL($item, $zoom));
}
*/
//echo "test = " . toValue(bindec("1101")) . "\n";


//$tile = bindec("1110111");


/*
$x= tiles_for_area(12.078174003482, 50.4141319666, 13.078174004482, 50.4141320666, 16);
print_r($x);

$y= tilesForArea(12.078174003482, 50.4141319666, 13.078174004482, 50.4141320666, 9);
print_r($y);
*/
?>