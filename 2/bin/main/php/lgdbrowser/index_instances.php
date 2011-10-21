<?php
ini_set("memory_limit", "1024M");

include("inc.php");


/*
 * 
 * 
 *select * from tags t JOIN elements e ON ((e.id, e.type) = (t.id, t.type)) WHERE e.type = 'node' and e.id = 305639346  
 * 
 *select count(*) FROM elements WHERE tile BETWEEN (1161499 << 4) AND (1161499 << 4 | 15) 
 *select count(*) FROM tiles14 WHERE tile = 1161499 and k = 5
 */

echo "Starting simple index\n";
simpleIndexAll();
echo "Finished indexing\n";

die;

function simpleIndexAll()
{
	$deltaZoom = 2;
	for($zoom = 12; $zoom >= 2; $zoom -= $deltaZoom) {
		
		echo "Entering zoom level: $zoom\n";
		
		$sourceTable = "instance_tile_k_$zoom";
		if($zoom == 16)
			$sourceTable = "elements";
		
		$targetTable = "instance_tile_k_" . ($zoom - $deltaZoom);
		

		createTargetTable($targetTable);
		
		simpleIndexInstances($zoom, $deltaZoom, $sourceTable, $targetTable);
	}
}


function simpleIndexInstances($sourceZoom, $deltaZoom, $sourceTable, $targetTable)
{
	global $db;
	global $properties;
	
	$propTable = "tiles". ($sourceZoom - $deltaZoom);
	
	echo "sourceZoom = $sourceZoom\ndeltaZoom = $deltaZoom\nsourceTable=$sourceTable\nvalueTable=$valueTable\ntargetTable=$targetTable\n";
	
	echo "Creating mapping table\n";
	
	
	//$mappingTable = "mapping_table";
	//$mappingTable = createMappingTable($sourceTable, $deltaZoom);
	$mappingTable = fillMappingTable($sourceTable, $deltaZoom);
	
	echo "Done\n";
	$props = implode("', '", $properties); 
	
	
	$sql = "
		SELECT
			m.target_tile, s.type, s.id, s.latitude, s.longitude
		FROM
			$sourceTable s
			INNER JOIN $mappingTable m ON(s.tile = m.source_tile)
		WHERE
			s.type = 'node' AND
			EXISTS (
				SELECT
					*
				FROM
					tags t
					INNER JOIN resources r ON (r.id = t.k)			
					INNER JOIN $propTable p ON(p.k = t.k)					
				WHERE
					(t.type, t.id) = (s.type, s.id) AND
					r.label IN ('$props') AND				
					p.tile = m.target_tile AND
					p.c <= 200
			)
			";

	$insert = 
		"INSERT INTO $targetTable(tile, type, id, latitude, longitude)
			$sql";
	
	echo "\n$insert\n\n";
	$rs = $db->query($insert);
	print_r($db->error);
	
	echo "Hooray";
}


function createTargetTable($tableName)
{	
	global $db;
	// Maybe it makes sense to include
	echo "Creating table $tableName\n";
	$sql = "
		CREATE TABLE IF NOT EXISTS $tableName (
			tile INT(10) UNSIGNED NOT NULL,			
			
			latitude INT(11) NOT NULL,
			longitude INT(11) NOT NULL,

			type ENUM('node','way') COLLATE utf8_general_ci,
			id   INT(11) UNSIGNED NOT NULL,
			
			PRIMARY KEY (type, id),
			INDEX USING BTREE (tile),
			INDEX USING BTREE (latitude),
			INDEX USING BTREE (longitude),
			INDEX USING BTREE (id)
			)";
	echo "\n\n$sql\n\n";
		
	$rs = $db->query($sql);
	print_r($db->error);

	echo "done.";
}




//echo "db = $db\n";

//indexByProperty($sourceTable, $targetTable, 0, 0, 65536, 65536, 15, 16, 1000);
//analyzeTileMappings("elements", "instance_tile_kv_14", 16, 14);

/**
 * min/max-X/Y are in target-tile-space (so this is the set of target tiles
 * that should be indexed)
 *
 * @param unknown_type $sourceTable
 * @param unknown_type $targetTable
 * @param unknown_type $minX
 * @param unknown_type $minY
 * @param unknown_type $maxX
 * @param unknown_type $maxY
 */

function createMappingTable()
{	
	global $db;
	
	$tableName = "mapping_table";
	
	$sql = "DROP TABLE IF EXISTS $tableName";
	$rs = $db->query($sql);
	print_r($db->error);
	
	// Maybe it makes sense to include
	echo "Creating target table";
	$sql = "
		CREATE TABLE IF NOT EXISTS $tableName (
			target_tile INT(10) UNSIGNED NOT NULL,
			source_tile INT(10) UNSIGNED NOT NULL,
			
			INDEX USING BTREE (target_tile),
			INDEX USING BTREE (source_tile)
		)";
	
	//echo "\n\n$sql\n\n";
	$rs = $db->query($sql);
	print_r($db->error);

	echo "done.";
	
	return $tableName;
}



function fillMappingTable($sourceTable, $deltaZoom)
{
	global $db;
	
	$tableName = createMappingTable();
	
	$sourceQuery = "
		SELECT
			DISTINCT st.tile
		FROM
			$sourceTable st
		WHERE
			st.type = 'node'
			";

	// our goal: target_tile, k, (type, id)

	$targetQuery = "
		SELECT
			s.tile >> (2 * $deltaZoom) AS target_tile,
			s.tile AS source_tile
		FROM
			($sourceQuery) s
		";
	
	$sql = "
		INSERT INTO $tableName(target_tile, source_tile)
			$targetQuery";
	
	echo $sql;
	$rs = $db->query($sql);
	print_r($db->error);
			
	return $tableName;
}



function createPropertyTable()
{
	global $db;
	$tableName = "tmp_property";
	
	
	echo "Creating property table\n";
	$sql = "
		CREATE TABLE IF NOT EXISTS $tableName (
			target_tile INT(10) UNSIGNED NOT NULL,
			k    INT(10) UNSIGNED NOT NULL,
			v    INT(10) UNSIGNED NOT NULL,
						
			INDEX USING BTREE (target_tile, k, v)
		)";
	
	echo "\n\n$sql\n\n";
	$rs = $db->query($sql);
	print_r($db->error);

	echo "done.";
	
	return $tableName;
}

function calcPropertyStats($sourceTable, $mappingTable, $limit)
{	
	$tableName = createPropertyTable();
	
	$propertyQuery = "
		SELECT
			tq.target_tile,
			t.k,
			t.v
		FROM
			$mappingTable tq
			INNER JOIN $sourceTable st2 ON (st2.tile = tq.source_tile)
			INNER JOIN tags t ON ((t.type, t.id) = (st2.type, st2.id))
		WHERE
			st2.type = 'node'
		GROUP BY
			tq.target_tile, t.k, t.v
		HAVING
			COUNT(t.v) <= $limit
		";

	$sql = "
		INSERT INTO $tableName(target_tile, k, v)
			$propertyQuery";		
	
	echo "$sql\n";

	$rs = $db->query($sql);
	print_r($db->error);
	
	
	return $tableName;
}


/**
 * Analyze instance data and return the set of target tiles which need
 * to be considered.
 *
 * @param unknown_type $sourceTable
 * @param unknown_type $targetZoom
 */
function analyzeTileMappings($sourceTable, $targetTable, $sourceZoom, $targetZoom)
{
	global $db;
	$limit = 250;
	$deltaZoom = $sourceZoom - $targetZoom;
	
	
	
	createIndexTable($targetTable);
	$mappingTable = createMappingTable();
	//$mappingTable = fillMappingTable($sourceTable, $deltaZoom);
	
	
	$propertyTable = calcPropertyStats($sourceTable, $mappingTable, $limit);
	// the delta zoom level determines how many bits we need to cut off
	// for the parent tile
	// if a tile has the bits 11[2 more bits], then the parent would be 11
	// (so we simply cut off 2 bits per zoom level)
	// 'cut off' can be realized by a simple bitshift right
	

	//$mappingTable = "(Select * from $mappingTable LIMIT 200)";

	/*	$targetQuery = "
		SELECT
			DISTINCT s.tile AS source_tile,
			s.tile >> (2 * $deltaZoom) AS target_tile
		FROM
			$sourceTable s
		";
*/	
	
	// for each target tile determine candidate properties
	// we then have: target_tile, tq.k, 
/*
	$propertyQuery = "
		SELECT
			tq.target_tile,
			t.k,
			t.v
		FROM
			$mappingTable tq
			INNER JOIN $sourceTable st2 ON (st2.tile = tq.source_tile)
			INNER JOIN tags t ON ((t.type, t.id) = (st2.type, st2.id))
		WHERE
			st2.type = 'node'
		GROUP BY
			tq.target_tile, t.k, t.v
		HAVING
			COUNT(t.v) <= $limit
		";
*/
			
	// target_tile < source_tile < k,
	$sql = "
		SELECT
			tq2.target_tile,
			pq.k,
			pq.v,
			st3.type,
			st3.id,
			st3.latitude,
			st3.longitude
		FROM
			($propertyQuery) pq
			INNER JOIN $mappingTable tq2 ON (tq2.target_tile = pq.target_tile)
			INNER JOIN tags t ON (t.k = pq.k AND t.v = pq.v)
			INNER JOIN $sourceTable st3 ON ((st3.type, st3.id) = (t.type, t.id))
		WHERE
			st3.tile = tq2.source_tile
			AND st3.type = 'node'
		";
//			 CONV(st3.tile, 10, 2) AS source_tile
echo $sql;
die;
	$insert = "
		INSERT INTO
			$targetTable(tile, k, v, type, id, latitude, longitude)
			($sql)
		";
	
	echo "\n\n$insert\n\n";
	$rs = $db->query($insert);
	print $db->error;

	/*
	while($row = $rs->fetch_assoc()) {
		$targetTile = $row['target_tile'];
		$sourceTile = $row['source_tile'];
		$k = $row['k'];
		$type = $row['type'];
		$id = $row['id'];
		
		echo "$targetTile $sourceTile $k $type $id\n";
	}
*/
	/* 
	echo "\n\n$sql\n\n";
	$rs = $db->query($sql);
	print $db->error;
	$tileMap = array();
	

	$count = 0;
	while($row = $rs->fetch_assoc()) {
		$sourceTile = $row['tile'];		
		
		// Determine the target tile
		$sourceXY = tileToXY($sourceTile);
		$sourceX = $sourceXY['x'];
		$sourceY = $sourceXY['y'];
		
		$targetX = round($sourceX / $f);
		$targetY = round($sourceY / $f);
		
		$targetTile = xyToTile($targetX, $targetY);
		
		if(!isset($tileMap[$targetTile]))
			$tileMap[$targetTile] = array();

		$tileMap[$targetTile][] = $sourceTile;
		
		++$count;
		echo "Processed $count items.\n";
	}
*/
	echo "Done\n";
}



/**
 * Source table must have the structure:
 * tile, k, type, id
 *
 * @param unknown_type $targetTable
 * @param unknown_type $targetTile
 * @param unknown_type $sourceTable
 * @param unknown_type $sourceTiles
 */
function updateTileInit($targetTable, $targetTile, $sourceTable, $sourceTiles, $limit)
{
	global $db;
	
	$tiles = implode("', '", $sourceTiles); 
	
	echo "target = $targetTile, sources = '$tiles'\n";
	
	//Find all properties in the given tiles which do not appear to often
	$subQuery = "
		SELECT
			s.k
		FROM
			$sourceTable s
		WHERE
			s.tile IN ('$tiles')
		GROUP BY
			s.k
		HAVING
			COUNT(s.k) <= $limit
		";


	// This query determines all properties we could take for display
	$subQuery = "
		SELECT
			tt.k,
			COUNT(tt.k) c
		FROM
			elements ee
			INNER JOIN tags tt ON ((tt.type, tt.id) = (ee.type, ee.id))
		WHERE
			ee.tile IN ('$tiles')
			AND ee.type = 'node'
		GROUP BY
			tt.k
		HAVING
			COUNT(tt.k) <= $limit
	";
	
	// This query finds all instances for the interesting properties
	$query = "
		SELECT
			e.tile,
			s.k,
			e.type,
			e.id
		FROM
			($subQuery) s
			INNER JOIN tags t ON (t.k = s.k)
			INNER JOIN elements e ON ((e.type, e.id) = (t.type, t.id))
		WHERE
			e.tile IN ('$tiles')
			AND e.type = 'node'
	";


			/*
	echo "SubQuery\n\n";
	$rs = $db->query($subQuery);
	print $db->error;

	while($row = $rs->fetch_assoc()) {
		$k = $row['k'];
		$c = $row['c'];
		
		echo "$k($c)\n";
	}
	*/


	//echo "$query\n\n";
	$rs = $db->query($query);
	print $db->error;

	while($row = $rs->fetch_assoc()) {
		$tile = $row['tile'];
		$k    = $row['k'];
		$type = $row['type'];
		$id   = $row['id'];
		
		//echo "$tile $k $type $id\n";
	}

	//echo "done";
}

/*
function indexByProperty($sourceTable, $targetTable,
	$minX, $minY, $sizeX, $sizeY,
	$targetZoomLevel, $sourceZoomLevel, $limit)
{
*/
	/*
	for($q = 0; $q < $sizeY; ++$q) {
		for($p = 0; $p < $sizeX; ++$p) {
			$x = $minX + $p;
			$y = $minY + $q;
		*/


/**
 * This function takes a set of target tiles to build.
 * The set of target tiles to build should be optained by analysing
 * the source tiles first. 
 * (So first check which source tiles are available and to which target
 * tiles do they correspond)
 *
 * @param unknown_type $sourceTable
 * @param unknown_type $targetTable
 * @param unknown_type $targetTiles
 * @param unknown_type $targetZoomLevel
 * @param unknown_type $sourceZoomLevel
 * @param unknown_type $limit
 */
function indexByProperty($sourceTable, $targetTable,
	$tileMappings,
	$limit)
{
	//createIndexTable($targeTable);
	$deltaZoom = $sourceZoomLevel - $targetZoomLevel;
	
	if($deltaZoom < 1) {
		echo "ERROR: difference between target and source zoom level must be greater than 1";
		return;
	}

	$f = pow(2, $deltaZoom) - 1;
	
	$processed = 0;
	foreach($tileMappings as $targetTile => $sourceTiles) {

		$startTime = microtime(1);
		
		updateTileInit($targetTable, $targetTile, $sourceTable, $sourceTiles, $limit);

		++$processed;

		$total = sizeof($tileMappings);
		$elapsedTime = microtime(1) - $startTime;
		$ratio = $processed / $elapsedTime;
		
		// sizeX * sizeY / ratio = sizeX / ratio * sizeY
		$estimate = (total / $ratio) * $sizeY;
		$estimate /= 60; // minutes
		
		$progress = round(($q + ($p / $sizeX)) / $sizeY, 5);
		echo "Progress: $progress%  - Processed: $processed - Ratio: $ratio - Estimate: $estimate\n";
	}

//while($row=$res->fetch_assoc()) {
		
}


?>