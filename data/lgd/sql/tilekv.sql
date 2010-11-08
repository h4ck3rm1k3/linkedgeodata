DROP TABLE lgd_stats_node_tags_tilekv_16;

SELECT
	LGD_ToTile(n.geom, 16) AS tile_id, k, v, COUNT(*)::INT AS usage_count
INTO
	lgd_stats_node_tags_tilekv_16
FROM
	node_tags nt JOIN nodes n ON (nt.node_id = n.id)
GROUP BY
	tile_id, k, v;

CREATE INDEX idx_lgd_stats_node_tags_tilekv_16_tile_id_k_v ON lgd_stats_node_tags_tilekv_16 (tile_id, k, v);


DROP TABLE lgd_stats_node_tags_tilekv_14;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_14
		FROM
			lgd_stats_node_tags_tilekv_16 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_14_tile_id_k_v ON lgd_stats_node_tags_tilekv_14 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_12;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_12
		FROM
			lgd_stats_node_tags_tilekv_14 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_12_tile_id_k_v ON lgd_stats_node_tags_tilekv_12 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_10;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_10
		FROM
			lgd_stats_node_tags_tilekv_12 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_10_tile_id_k_v ON lgd_stats_node_tags_tilekv_10 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_8;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_8
		FROM
			lgd_stats_node_tags_tilekv_10 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_8_tile_id_k_v ON lgd_stats_node_tags_tilekv_8 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_6;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_6
		FROM
			lgd_stats_node_tags_tilekv_8 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_6_tile_id_k_v ON lgd_stats_node_tags_tilekv_6 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_4;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_4
		FROM
			lgd_stats_node_tags_tilekv_6 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_4_tile_id_k_v ON lgd_stats_node_tags_tilekv_4 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_2;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_2
		FROM
			lgd_stats_node_tags_tilekv_4 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_2_tile_id_k_v ON lgd_stats_node_tags_tilekv_2 (tile_id, k, v);
	

DROP TABLE lgd_stats_node_tags_tilekv_0;

		SELECT
			t.tile_id >> 4 AS tile_id, t.k, t.v, SUM(t.usage_count)::INT AS usage_count
		INTO
			lgd_stats_node_tags_tilekv_0
		FROM
			lgd_stats_node_tags_tilekv_2 t
		GROUP BY
			tile_id >> 4, k, v;

		CREATE INDEX idx_lgd_stats_node_tags_tilekv_0_tile_id_k_v ON lgd_stats_node_tags_tilekv_0 (tile_id, k, v);
	

