DROP VIEW IF EXISTS lgd_node_tags_prefix;
CREATE VIEW lgd_node_tags_prefix AS
	SELECT
		a.node_id, b.property, b.object_prefix, a.v
	FROM
		node_tags a JOIN
		lgd_resource_map_object_prefix b ON (b.k = a.k);

/**
 * De-Prefix a uri against the prefix table.
 *
 * Example:
 * If the prefix table contains 'lgd:' and the input is 'lgd:amenity'
 * the result is 'amenity.
 * 
 */
DROP FUNCTION LGD_DePrefix(uri TEXT);
CREATE FUNCTION LGD_DePrefix(uri TEXT) RETURNS SETOF text AS
$$
BEGIN
	RETURN QUERY
		SELECT
			substring(uri from char_length(x.prefix::text) + 1)::text remainder
		FROM (
			SELECT DISTINCT
				object_prefix prefix
			FROM
				lgd_resource_map_object_prefix
			WHERE
				object_prefix @> uri
			) x;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

      
    
DROP FUNCTION LGD_DePrefixTest(uri TEXT);
CREATE FUNCTION LGD_DePrefixTest(uri TEXT) RETURNS SETOF text AS
$$
BEGIN
	RETURN QUERY
		SELECT
			substring(uri from char_length(x.prefix::text) + 1)::text remainder
		FROM (
			SELECT DISTINCT
				prefix
			FROM
				prefixes
			WHERE
				prefix @> uri
			) x;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

    
/**
 * Example query
 * TODO Move to an exapmles section
 */
SELECT
	substring('http://linkedgeodata.org/ontology/wheelchair' from char_length(x.prefix::text) + 1) remainder
FROM (
	SELECT DISTINCT
		object_prefix prefix
	FROM
		lgd_resource_map_object_prefix
	WHERE
		object_prefix @> 'http://linkedgeodata.org/ontology/wheelchair'
	) x;
