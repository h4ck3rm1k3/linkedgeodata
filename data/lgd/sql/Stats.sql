/**
 * This file contains functions for trying to parse strings (e.g. from tags)
 * as values of specific types (float, integer and bool).
 * 
 * This is used for automatically deriving the ranges of certain properties
 * 
 * FIXME Add stats for positive/negative ints/floats
 */

DROP FUNCTION LGD_TryParseBoolean(v TEXT);
CREATE FUNCTION LGD_TryParseBoolean(v TEXT) RETURNS BOOL AS
$$
DECLARE
BEGIN
    RETURN
    	CASE
    		WHEN (v ~* 'true'  OR v ~* 'yes' OR v = '1') THEN TRUE
    		WHEN (v ~* 'false' OR v ~* 'no'  OR v = '0') THEN FALSE
    		ELSE NULL
    	END;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;
    

    
DROP FUNCTION LGD_TryParseInt(str TEXT);
CREATE FUNCTION LGD_TryParseInt(str TEXT) RETURNS INT8 AS
$$
DECLARE
BEGIN
    RETURN str::int8;
EXCEPTION
	WHEN OTHERS THEN
		RETURN NULL;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

    
DROP FUNCTION LGD_TryParseFloat(str TEXT);
CREATE FUNCTION LGD_TryParseFloat(str TEXT) RETURNS FLOAT AS
$$
DECLARE
BEGIN
    RETURN str::float;
EXCEPTION
	WHEN OTHERS THEN
		RETURN NULL;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;


/* Following query took 260 minutes on the server */
SELECT
	k,
	SUM(1) AS count_total,
	SUM(CASE WHEN LGD_TryParseInt(v) IS NOT NULL THEN 1 ELSE 0 END) AS count_int,
	SUM(CASE WHEN LGD_TryParseFloat(v) IS NOT NULL THEN 1 ELSE 0 END) AS count_float,
	SUM(CASE WHEN LGD_TryParseBoolean(v) IS NOT NULL THEN 1 ELSE 0 END) as count_boolean
INTO
	lgd_stats_tags_datatypes
FROM
	lgd_tags
GROUP BY
	k;

