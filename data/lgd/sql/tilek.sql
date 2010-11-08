DROP TABLE lgd_stats_node_tags_tilek_16;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_16
		FROM
			lgd_stats_node_tags_tilekv_16 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_16_tile_id_k ON lgd_stats_node_tags_tilek_16 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_14;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_14
		FROM
			lgd_stats_node_tags_tilekv_14 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_14_tile_id_k ON lgd_stats_node_tags_tilek_14 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_12;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_12
		FROM
			lgd_stats_node_tags_tilekv_12 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_12_tile_id_k ON lgd_stats_node_tags_tilek_12 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_10;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_10
		FROM
			lgd_stats_node_tags_tilekv_10 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_10_tile_id_k ON lgd_stats_node_tags_tilek_10 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_8;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_8
		FROM
			lgd_stats_node_tags_tilekv_8 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_8_tile_id_k ON lgd_stats_node_tags_tilek_8 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_6;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_6
		FROM
			lgd_stats_node_tags_tilekv_6 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_6_tile_id_k ON lgd_stats_node_tags_tilek_6 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_4;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_4
		FROM
			lgd_stats_node_tags_tilekv_4 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_4_tile_id_k ON lgd_stats_node_tags_tilek_4 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_2;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_2
		FROM
			lgd_stats_node_tags_tilekv_2 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_2_tile_id_k ON lgd_stats_node_tags_tilek_2 (tile_id, k);
	

DROP TABLE lgd_stats_node_tags_tilek_0;

		SELECT
			t.tile_id AS tile_id, t.k, SUM(t.usage_count)::INT AS usage_count, COUNT(*)::INT AS distinct_value_count
		INTO
			lgd_stats_node_tags_tilek_0
		FROM
			lgd_stats_node_tags_tilekv_0 t
		GROUP BY
			tile_id, k;

		CREATE INDEX idx_lgd_stats_node_tags_tilek_0_tile_id_k ON lgd_stats_node_tags_tilek_0 (tile_id, k);
	

