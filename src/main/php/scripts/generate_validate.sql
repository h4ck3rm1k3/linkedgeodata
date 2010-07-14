/* Both queries should yield no results */
SELECT tile_id, k, COUNT(*) FROM lgd_stats_node_tags_tilek_0 GROUP BY tile_id, k HAVING COUNT(*) > 1;
SELECT tile_id, k, COUNT(*) FROM lgd_stats_node_tags_tilekv_0 GROUP BY tile_id, k, v HAVING COUNT(*) > 1;