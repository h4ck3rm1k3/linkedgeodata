/* Following query took 260 minutes on the server */
DROP TABLE IF EXISTS lgd_stats_tags_datatypes;
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


/* A helper view on absolute and relative errors */
DROP VIEW IF EXISTS  lgd_stats_tags_datatypes_errors;
CREATE VIEW lgd_stats_tags_datatypes_errors AS
SELECT
	a.*,
	count_total - count_boolean AS a_error_boolean, (1.0 - count_boolean / count_total::float) AS r_error_boolean,
	count_total - count_int     AS a_error_int,     (1.0 - count_int     / count_total::float) AS r_error_int,
	count_total - count_float   AS a_error_float,   (1.0 - count_float   / count_total::float) AS r_error_float
FROM
	lgd_stats_tags_datatypes a;


DROP TYPE IF EXISTS LGDDatatype;
CREATE TYPE LGDDatatype AS ENUM ('boolean', 'int', 'float');


/* Based on the stats, determine the best matching datatyp */
DROP TABLE IF EXISTS lgd_tags_datatypes;
SELECT
	k, datatype
INTO
	lgd_tags_datatypes
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

DROP INDEX IF EXISTS idx_lgd_tags_datatypes_k_datatype;
CREATE INDEX idx_lgd_tags_datatypes_k ON lgd_tags_datatypes(k, datadatype);

DROP INDEX IF EXISTS idx_lgd_tags_datatypes_d_k;
CREATE INDEX idx_lgd_tags_datatypes_d_k ON lgd_tags_datatypes(datatype, k);


DROP VIEW IF EXISTS node_tags_boolean;
CREATE VIEW node_tags_boolean AS SELECT a.node_id, a.k, LGD_TryParseBoolean(a.v) v FROM node_tags a JOIN lgd_tags_datatypes b ON (a.k = b.k) WHERE LGD_TryParseBoolean(a.v) IS NOT NULL AND b.datatype = LGDDatatype('boolean');

DROP VIEW IF EXISTS node_tags_int;
CREATE VIEW node_tags_int AS SELECT a.node_id, a.k, LGD_TryParseInt(a.v) v FROM node_tags a JOIN lgd_tags_datatypes b ON (a.k = b.k) WHERE LGD_TryParseInt(a.v) IS NOT NULL AND b.datatype = LGDDatatype('int');

DROP VIEW IF EXISTS node_tags_float;
CREATE VIEW node_tags_float AS SELECT a.node_id, a.k, LGD_TryParseFloat(a.v) v FROM node_tags a JOIN lgd_tags_datatypes b ON (a.k = b.k) WHERE LGD_TryParseFloat(a.v) IS NOT NULL AND b.datatype = LGDDatatype('float');

/**
 * Everything that is neither mapped to a datatype nor to a class/object property
 * becomes a datatype property
 */
DROP VIEW IF EXISTS node_tags_text;
CREATE VIEW node_tags_text AS
	SELECT a.node_id, a.k, a.v FROM node_tags a WHERE
		NOT EXISTS (SELECT b.k FROM lgd_tags_datatypes b WHERE b.k = a.k) AND 
		NOT EXISTS (SELECT c.k FROM generic_resource_map c WHERE c.k = a.k) AND 
		NOT EXISTS (SELECT d.k FROM specific_resource_map d WHERE (d.k, d.v) = (a.k, a.v)); 


/*
These view definitions are not per key, and would therefore cause troubles
in the ontology because of potential multiple datatypes per key

DROP VIEW IF EXISTS node_tags_boolean;
CREATE VIEW node_tags_boolean AS SELECT node_id, k, LGD_TryParseBoolean(v) v FROM node_tags WHERE LGD_TryParseBoolean(v) IS NOT NULL;

DROP VIEW IF EXISTS node_tags_int;
CREATE VIEW node_tags_int AS SELECT node_id, k, LGD_TryParseInt(v) v FROM node_tags WHERE LGD_TryParseInt(v) IS NOT NULL;

DROP VIEW IF EXISTS node_tags_float;
CREATE VIEW node_tags_float AS SELECT node_id, k, LGD_TryParseFloat(v) v FROM node_tags WHERE LGD_TryParseFloat(v) IS NOT NULL;
*/


