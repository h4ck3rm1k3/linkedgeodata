
/**
 * Returns a set (array) of integers which correspond to tile indices.
 * 
 *
 * @param unknown_type $minLat
 * @param unknown_type $minLon
 * @param unknown_type $maxLat
 * @param unknown_type $maxLon
 * @param unknown_type $zoom
 * @return unknown
 */
function tilesForArea($minLat, $minLon, $maxLat, $maxLon, $zoom = 16)
{
	$f = pow(2, $zoom) - 1;
	
	/**
	 * Transform the given geo-coordinates into tile coordinates.
	 */
	$minX = (int)(lonNormX($minLon) * $f);
	$maxX = (int)(lonNormX($maxLon) * $f);
	$minY = (int)(latNormY($minLat) * $f);
	$maxY = (int)(latNormY($maxLat) * $f);

	#print "$minx $maxx $miny $maxy";
	$tiles=Array();

	for($x = $minX; $x <= $maxX; $x++) {
		for($y = $minY; $y <= $maxY; $y++) {
			$tiles[] = tileForXY($x, $y);
		}
	}
#print_r($tiles);
	return $tiles;
}

class Rectangle
{
	public function __construct($minX, $minY, $maxX, $maxY)
	{
		$this->minX = $minX;
		$this->minY = $minY;
		$this->maxX = $maxX;
		$this->maxY = $maxY;
	}
	
	public $minX;
	public $minY;
	public $maxX;
	public $maxY;
}

class Range
{
	public function __construct($min, $max)
	{
		$this->min = $min;
		$this->max = $max;
	}
	
	public $min;
	public $max;
}



/**
 * 
 * 
 *
 * @param unknown_type $rectangle
 * @param unknown_type $raster
 * @param unknown_type $range
 */
function ranges($rectangle, $raster, $range)
{
	// Check if the

	
}
