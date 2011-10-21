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

$query = "
SELECT
	LGD_ToTile(n.geom, $init) AS tile_id, k, v, COUNT(*)::INT AS usage_count
INTO
	{$prefix}{$table}_tilekv_{$init}
FROM
	{$table} nt JOIN nodes n ON (nt.node_id = n.id)
GROUP BY
	tile_id, k, v;

CREATE INDEX idx_{$prefix}{$table}_tilekv_{$init}_tile_id_k_v ON {$prefix}{$table}_tilekv_{$init} (tile_id, k, v);
";

echo "DROP TABLE {$prefix}{$table}_tilekv_{$init};\n";
echo "$query\n\n";

$shift = 2 * $step;
for($child = ($init - $step); $child >= 0; $child -= $step) {
	$parent = $child + $step;	

	$query = "
		SELECT
			t.tile_id >> {$shift} AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			{$prefix}{$table}_tilekv_{$child}
		FROM
			{$prefix}{$table}_tilekv_{$parent} t
		GROUP BY
			tile_id >> {$shift}, k, v;

		CREATE INDEX idx_{$prefix}{$table}_tilekv_{$child}_tile_id_k_v ON {$prefix}{$table}_tilekv_{$child} (tile_id, k, v);
	";

	echo "DROP TABLE {$prefix}{$table}_tilekv_{$child};\n";
	echo "$query\n\n";
}
	
