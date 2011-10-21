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
$prefix = "lgd_stats_";

/*
$query = "
SELECT
	LGD_ToTile(n.geom, $init) AS tile_id, k, COUNT(*) AS usage_count, SUM(s.count) AS distinct_value_count
INTO
	{$prefix}{$table}_tilek_{$init} s
FROM
	{$prefix}{$table}_tilekv_{$init}
GROUP BY
	tile_id, k;

CREATE INDEX idx_{$prefix}{$table}_tilek_{$init}_tile_id_k ON {$prefix}{$table}_tilek_{$init} (tile_id, k);
";

echo "DROP TABLE {$prefix}{$table}_tilek_{$init};\n";
echo "$query\n\n";
*/

$shift = 2 * $step;
for($child = $init; $child >= 0; $child -= $step) {
	//$parent = $child + $step;	

	$query = "
		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			{$prefix}{$table}_tilek_{$child}
		FROM
			{$prefix}{$table}_tilekv_{$child} t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_{$prefix}{$table}_tilek_{$child}_tile_id_k ON {$prefix}{$table}_tilek_{$child} (tile_id, k);
	";

	echo "DROP TABLE {$prefix}{$table}_tilek_{$child};\n";
	echo "$query\n\n";
}
	
