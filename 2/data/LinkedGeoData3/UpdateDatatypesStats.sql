/* Following query took 260 minutes on the server */
DELETE FROM lgd_stats_tags_datatypes;
SELECT
	k,
	SUM(1) AS count_total,
	SUM(CASE WHEN lgd_tryparse_int(v) IS NOT NULL THEN 1 ELSE 0 END) AS count_int,
	SUM(CASE WHEN lgd_tryparse_float(v) IS NOT NULL THEN 1 ELSE 0 END) AS count_float,
	SUM(CASE WHEN lgd_tryparse_boolean(v) IS NOT NULL THEN 1 ELSE 0 END) as count_boolean
INTO
	lgd_stats_tags_datatypes
FROM
	lgd_tags
GROUP BY
	k;

	
	

/* Based on the stats, determine the best matching datatyp */
/* DROP TABLE IF EXISTS lgd_tags_datatypes; */
DELETE FROM lgd_map_datatype;
SELECT
	k, datatype
INTO
	lgd_stats_tags_datatypes
FROM (
	SELECT
		k,
		(CASE WHEN a_error_boolean < 5000 AND r_error_boolean < 0.01 THEN LGDDatatype('boolean') ELSE
			(CASE WHEN a_error_int < 5000 AND r_error_int < 0.01 THEN LGDDatatype('int') ELSE
				(CASE WHEN a_error_float < 5000 AND r_error_float < 0.01 THEN LGDDatatype('float') END)
			END)
		END) datatype
	FROM
		lgd_stats_tags_datatypes_errors) x
WHERE
	datatype IS NOT NULL;

