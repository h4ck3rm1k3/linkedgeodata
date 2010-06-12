SELECT k, v, COUNT(*) AS usage_count INTO lgd_stats_kv FROM lgd_tags GROUP BY k, v;
CREATE INDEX idx_lgd_stats_kv_k_v ON lgd_stats_kv(k, v);
