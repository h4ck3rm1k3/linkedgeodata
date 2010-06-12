SELECT k, COUNT(k) AS usage_count, COUNT(DISTINCT v) AS distinct_value_count INTO lgd_stats_k FROM lgd_tags GROUP BY k;
CREATE INDEX idx_lgd_stats_k_k ON lgd_stats_k(k);
