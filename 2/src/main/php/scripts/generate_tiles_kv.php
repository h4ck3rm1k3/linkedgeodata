<?php
/**
 * This script generates SQL statements for gathering per-tile
 * tag-key statistics. (So how often a key is used per tile).
 *
 * Part of the LinkedGeoData project.
 *
 * @Author Claus Stadler
 */

$init = 16;
$step = 2;

$table = "node_tags";


$query = "
SELECT
	LGD_ToTile(t.geom, $init) AS tile_id, k, v, COUNT(*) AS count
INTO
	{$table}_tiles_kv_{$init}
FROM
	{$table} t
GROUP BY
	tile_id, k, v;

CREATE INDEX idx_{$table}_tiles_kv_{$init}_tile_id_k_v ON {$table}_tiles_kv_{$init} (tile_id, k, v);
";

echo "$query\n\n";

$shift = 2 * $step;
for($child = ($init - $step); $child >= 0; $child -= $step) {
	$parent = $child + $step;	

	$query = "
		SELECT
			t.tile_id >> {$shift} AS tile_id, t.k, t.v, SUM(t.count) AS count
		INTO
			{$table}_tiles_kv_{$child}
		FROM
			{$table}_tiles_kv_{$parent} t
		GROUP BY
			tile_id, k, v;

		CREATE INDEX idx_{$table}_tiles_kv_{$child}_tile_id_k_v ON {$table}_tiles_kv_{$child} (tile_id, k, v);
	";

	echo "$query\n\n";
}
	
