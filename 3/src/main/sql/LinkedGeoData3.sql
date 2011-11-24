/****************************************************************************
 *                                                                          *
 * LinkedGeoData 3 Utility Functions                                        *
 *                                                                          *
 ****************************************************************************/

DROP FUNCTION lgd_tryparse_boolean(v TEXT);
CREATE FUNCTION lgd_tryparse_boolean(v TEXT) RETURNS BOOL AS
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
    

    
DROP FUNCTION lgd_tryparse_int(str TEXT);
CREATE FUNCTION lgd_tryparse_int(str TEXT) RETURNS INT8 AS
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

    
DROP FUNCTION lgd_tryparse_float(str TEXT);
CREATE FUNCTION lgd_tryparse_float(str TEXT) RETURNS FLOAT AS
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




/****************************************************************************
 *                                                                          *
 * LinkedGeoData 3 specific database objects                                *
 *     Indexes are a bit overused here - many tables are so small that the  *
 *     DB does a scan anyway                                                *
 *                                                                          * 
 *                                                                          *
 ****************************************************************************/

DROP TYPE IF EXISTS OSMEntityType;
CREATE TYPE OSMEntityType AS ENUM ('node', 'way', 'relation');

/**
 * A view for uniform access to all sorts of tags
 * 
 */
DROP VIEW IF EXISTS lgd_tags; 
CREATE VIEW lgd_tags AS
	SELECT
		t.osm_entity_type, t.osm_entity_id, t.k, t.v
	FROM
		((SELECT OSMEntityType('node') AS osm_entity_type, node_id AS osm_entity_id, k, v FROM node_tags) UNION ALL
		 (SELECT OSMEntityType('way') AS osm_entity_type, way_id AS osm_entity_id, k, v FROM way_tags) UNION ALL
		 (SELECT OSMEntityType('relation') AS osm_entity_type, relation_id AS osm_entity_id, k, v FROM relation_tags)) AS t;


    
DROP TYPE IF EXISTS lgd_datatype;
CREATE TYPE lgd_datatype AS ENUM ('boolean', 'int', 'float');


DROP TABLE IF EXISTS lgd_stat_datatype;
CREATE TABLE lgd_stat_datatype (
	k             text   PRIMARY KEY NOT NULL,
	count_total   bigint NOT NULL,
	count_int     bigint NOT NULL,
	count_float   bigint NOT NULL,
	count_boolean bigint NOT NULL
);



DROP TABLE IF EXISTS lgd_map_datatype;
CREATE TABLE lgd_map_datatype (
    k text PRIMARY KEY NOT NULL,
    datatype lgd_datatype NOT NULL
);

DROP INDEX IF EXISTS idx_lgd_map_datatype_k;
CREATE INDEX idx_lgd_map_datatype_k ON lgd_map_datatype_k(k);

DROP INDEX IF EXISTS idx_lgd_map_datatype_datatype_k;
CREATE INDEX idx_lgd_map_datatype_datatype_k ON lgd_map_datatype_k(datatype, k);



/* A helper view on absolute and relative errors */
DROP VIEW IF EXISTS  lgd_stat_datatype_error;
CREATE VIEW lgd_stat_datatype_error AS
SELECT
	a.*,
	count_total - count_boolean AS a_error_boolean, (1.0 - count_boolean / count_total::float) AS r_error_boolean,
	count_total - count_int     AS a_error_int,     (1.0 - count_int     / count_total::float) AS r_error_int,
	count_total - count_float   AS a_error_float,   (1.0 - count_float   / count_total::float) AS r_error_float
FROM
	lgd_stat_datatype a;



DROP TABLE IF EXISTS lgd_map_literal;
CREATE TABLE lgd_map_literal (
	k text NOT NULL,
	property text NOT NULL, 
	language text NOT NULL,

	/* Avoid duplicates */
	UNIQUE(k, property, language)
);

CREATE INDEX idx_lgd_map_literal_k ON lgd_map_literal(k);
CREATE INDEX idx_lgd_map_literal_property_language ON lgd_map_literal(property, language);
CREATE INDEX idx_lgd_map_literal_language_property ON lgd_map_literal(language, property);



DROP TABLE IF EXISTS lgd_map_label_kv;
CREATE TABLE lgd_map_label_kv (
	k TEXT NOT NULL,
	v TEXT NOT NULL, 
	language VARCHAR(16) NOT NULL,
	label TEXT NOT NULL,

	/* Avoid duplicates */
	UNIQUE(k, v, language, label)
);

/* Index for searching by label */ 
CREATE INDEX idx_lgd_map_label_kv_label_language ON lgd_map_label_kv(label, language);

/* Index for searching by language */
CREATE INDEX idx_lgd_map_label_kv_language_label ON lgd_map_label_kv(language, label);

/* Index for joins on (k, v) */
CREATE INDEX idx_lgd_map_label_kv_k_v ON lgd_map_label_kv(v, k);


DROP TABLE IF EXISTS lgd_map_resource_k; 
CREATE TABLE lgd_map_resource_k (
    k text NOT NULL,
    property text NOT NULL,
    object text NOT NULL
);

CREATE INDEX idx_lgd_map_resource_k_k ON lgd_map_resource_k USING btree (k);
CREATE INDEX idx_lgd_map_resource_k_property_object ON lgd_map_resource_k USING btree (property, object);
CREATE INDEX idx_lgd_map_resource_k_object_property ON lgd_map_resource_k USING btree (object, property);



DROP TABLE IF EXISTS lgd_map_resource_kv;
CREATE TABLE lgd_map_resource_kv (
    k text NOT NULL,
    v text NOT NULL,
    property text NOT NULL,
    object text NOT NULL
);

CREATE INDEX idx_lgd_map_resource_kv_k ON lgd_map_resource_kv USING btree (k);
CREATE INDEX idx_lgd_map_resource_kv_v_k ON lgd_map_resource_kv USING btree (v, k);
CREATE INDEX idx_lgd_map_resource_kv_property_object ON lgd_map_resource_kv USING btree (property, object);
CREATE INDEX idx_lgd_map_resource_kv_object_property ON lgd_map_resource_kv USING btree (object, property);



CREATE TABLE lgd_map_resource_prefix (
    k text NOT NULL,
    property text NOT NULL,
    object_prefix prefix_range NOT NULL,
    post_processing text NOT NULL
);

CREATE INDEX idx_lgd_map_resource_prefix_prefix ON lgd_map_resource_prefix USING gist (object_prefix);
CREATE INDEX idx_lgd_map_resource_prefix_k ON lgd_map_resource_prefix USING btree (k);


CREATE TABLE lgd_map_property (
    k text NOT NULL,
    property text NOT NULL
);

CREATE INDEX idx_lgd_map_property_k ON lgd_map_property USING btree (k);
CREATE INDEX idx_lgd_map_property_property ON lgd_map_property USING btree (property);



CREATE VIEW lgd_resource_label AS
 SELECT b.object AS resource, a.label, a.language
   FROM lgd_map_label_kv a
   JOIN lgd_map_resource_kv b USING (k, v);


/****************************************************************************
 * node_tags                                                                *
 ****************************************************************************/
   
DROP VIEW IF EXISTS lgd_node_tags_boolean;
CREATE VIEW lgd_node_tags_boolean AS
  SELECT a.node_id, a.k, lgd_tryparse_boolean(a.v) AS v
   FROM node_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_boolean(a.v) IS NOT NULL AND b.datatype = 'boolean'::lgd_datatype;


DROP VIEW IF EXISTS lgd_node_tags_int;
CREATE VIEW lgd_node_tags_int AS
  SELECT a.node_id, a.k, lgd_tryparse_int(a.v) AS v
   FROM node_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_int(a.v) IS NOT NULL AND b.datatype = 'int'::lgd_datatype;
  

DROP VIEW IF EXISTS lgd_node_tags_float;
CREATE VIEW lgd_node_tags_float AS
  SELECT a.node_id, a.k, lgd_tryparse_float(a.v) AS v
   FROM node_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_float(a.v) IS NOT NULL AND b.datatype = 'float'::lgd_datatype;

  
/**
 * Everything that is neither mapped to a datatype nor to a class/object property
 * becomes a datatype property
 */
DROP VIEW IF EXISTS lgd_node_tags_string;
CREATE VIEW lgd_node_tags_string AS
	SELECT a.node_id, a.k, a.v FROM node_tags a WHERE
		NOT EXISTS (SELECT b.k FROM lgd_map_datatype  b WHERE b.k = a.k) AND 
		NOT EXISTS (SELECT c.k FROM lgd_map_resource_k  c WHERE c.k = a.k) AND 
		NOT EXISTS (SELECT d.k FROM lgd_map_resource_kv d WHERE (d.k, d.v) = (a.k, a.v)) AND 
		NOT EXISTS (SELECT e.k FROM lgd_map_literal e WHERE e.k = a.k) AND
		NOT EXISTS (SELECT f.k FROM lgd_map_property f WHERE f.k = a.k) AND 
		NOT EXISTS (SELECT g.k FROM lgd_map_resource_prefix g WHERE g.k = a.k); 

		
DROP VIEW IF EXISTS lgd_node_tags_text;
CREATE VIEW lgd_node_tags_text AS
 SELECT a.node_id, b.property, a.v, b.language
   FROM node_tags a
   JOIN lgd_map_literal b ON b.k = a.k;

		
/*
DROP VIEW IF EXISTS lgd_node_tags_text;
CREATE VIEW lgd_node_tags_text AS
 SELECT a.node_id, b.property, a.v, b.language
   FROM lgd_node_tags_string a
   JOIN lgd_map_literal b ON b.k = a.k;
*/
   
DROP VIEW IF EXISTS lgd_node_tags_resource_k;
CREATE VIEW lgd_node_tags_resource_k AS
 SELECT a.node_id, b.property, b.object
   FROM node_tags a
   JOIN lgd_map_resource_k b USING(k);

  
DROP VIEW IF EXISTS lgd_node_tags_resource_kv;
CREATE VIEW lgd_node_tags_resource_kv AS   
  SELECT a.node_id, b.property, b.object
   FROM node_tags a
   JOIN lgd_map_resource_kv b USING(k, v);

   
/*
DROP VIEW IF EXISTS lgd_node_tags_resource_prefix;
CREATE VIEW lgd_node_tags_resource_prefix AS      
  SELECT a.node_id, b.property, b.object_prefix, a.v
   FROM node_tags a
   JOIN lgd_map_resource_prefix b USING(k);
*/

/****************************************************************************
 * generic tags (adapted the node_tag stuff to lgd_tags                     *
 ****************************************************************************/
   
DROP VIEW IF EXISTS lgd_tags_boolean;
CREATE VIEW lgd_tags_boolean AS
  SELECT a.osm_entity_type, a.osm_entity_id, a.k, lgd_tryparse_boolean(a.v) AS v
   FROM lgd_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_boolean(a.v) IS NOT NULL AND b.datatype = 'boolean'::lgd_datatype;


DROP VIEW IF EXISTS lgd_tags_int;
CREATE VIEW lgd_tags_int AS
  SELECT a.osm_entity_type, a.osm_entity_id, a.k, lgd_tryparse_int(a.v) AS v
   FROM lgd_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_int(a.v) IS NOT NULL AND b.datatype = 'int'::lgd_datatype;
  

DROP VIEW IF EXISTS lgd_tags_float;
CREATE VIEW lgd_tags_float AS
  SELECT a.osm_entity_type, a.osm_entity_id, a.k, lgd_tryparse_float(a.v) AS v
   FROM lgd_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_float(a.v) IS NOT NULL AND b.datatype = 'float'::lgd_datatype;

  
/**
 * Everything that is neither mapped to a datatype nor to a class/object property
 * becomes a datatype property
 */
DROP VIEW IF EXISTS lgd_tags_string;
CREATE VIEW lgd_tags_string AS
	SELECT a.osm_entity_type, a.osm_entity_id, a.k, a.v FROM lgd_tags a WHERE
		NOT EXISTS (SELECT b.k FROM lgd_map_datatype  b WHERE b.k = a.k) AND 
		NOT EXISTS (SELECT c.k FROM lgd_map_resource_k  c WHERE c.k = a.k) AND 
		NOT EXISTS (SELECT d.k FROM lgd_map_resource_kv d WHERE (d.k, d.v) = (a.k, a.v)) AND 
		NOT EXISTS (SELECT e.k FROM lgd_map_literal e WHERE e.k = a.k) AND
		NOT EXISTS (SELECT f.k FROM lgd_map_property f WHERE f.k = a.k) AND
		NOT EXISTS (SELECT g.k FROM lgd_map_resource_prefix g WHERE g.k = a.k); 


		
DROP VIEW IF EXISTS lgd_tags_text;
CREATE VIEW lgd_tags_text AS
 SELECT a.osm_entity_type, a.osm_entity_id, b.property, a.v, b.language
   FROM lgd_tags a
   JOIN lgd_map_literal b ON b.k = a.k;

		
/*
DROP VIEW IF EXISTS lgd_node_tags_text;
CREATE VIEW lgd_node_tags_text AS
 SELECT a.node_id, b.property, a.v, b.language
   FROM lgd_node_tags_string a
   JOIN lgd_map_literal b ON b.k = a.k;
*/
   
DROP VIEW IF EXISTS lgd_tags_resource_k;
CREATE VIEW lgd_tags_resource_k AS
 SELECT a.osm_entity_type, a.osm_entity_id, b.property, b.object
   FROM lgd_tags a
   JOIN lgd_map_resource_k b USING(k);

  
DROP VIEW IF EXISTS lgd_tags_resource_kv;
CREATE VIEW lgd_tags_resource_kv AS   
  SELECT a.osm_entity_type, a.osm_entity_id, b.property, b.object
   FROM lgd_tags a
   JOIN lgd_map_resource_kv b USING(k, v);

   
DROP VIEW IF EXISTS lgd_tags_resource_prefix;
CREATE VIEW lgd_tags_resource_prefix AS   
  SELECT osm_entity_type, osm_entity_id, property, object_prefix, v, post_processing
   FROM lgd_tags a
   JOIN lgd_map_resource_prefix b USING(k);


/* My attemp to push all the postprocessing into the DB
 * Don't use ;) (Some things are easier to do in e.g. Java)
DROP VIEW IF EXISTS lgd_tags_resource_prefix;
CREATE VIEW lgd_tags_resource_prefix AS   
  SELECT osm_entity_type, osm_entity_id, property,
  	CASE WHEN post_processing = 'ucamelize&urlencode' THEN object_prefix || urlencode(lgd_ucamelize(lgd_ucamelize(v, ' '), '_'), 'utf8')
  	ELSE object_prefix || v
  END "object"
   FROM lgd_tags a
   JOIN lgd_map_resource_prefix b USING(k)
*/
   
   
DROP VIEW IF EXISTS lgd_tags_property;
CREATE VIEW lgd_tags_property AS   
  SELECT osm_entity_type, osm_entity_id, property, v "object"
   FROM lgd_tags a
   JOIN lgd_map_property b USING(k);

   
/****************************************************************************
 * way_tags                                                                 *
 ****************************************************************************/

CREATE VIEW lgd_way_nodes AS
 SELECT a.way_id, a.sequence_id AS first_sequence_id, b.sequence_id AS rest_sequence_id, a.node_id AS first_node_id, b.node_id AS rest_node_id
   FROM way_nodes a
   LEFT JOIN way_nodes b ON b.way_id = a.way_id AND b.sequence_id = (a.sequence_id + 1);
   

DROP VIEW IF EXISTS lgd_ways_endpoints;
CREATE VIEW lgd_ways_endpoints AS
   SELECT a.way_id, a.min_sequence_id, b.node_id AS min_node_id, a.max_sequence_id, c.node_id AS max_node_id
   FROM ( SELECT way_nodes.way_id, min(way_nodes.sequence_id) AS min_sequence_id, max(way_nodes.sequence_id) AS max_sequence_id
           FROM way_nodes
          GROUP BY way_nodes.way_id) a
   JOIN way_nodes b ON b.way_id = a.way_id AND b.sequence_id = a.min_sequence_id
   JOIN way_nodes c ON c.way_id = a.way_id AND c.sequence_id = a.max_sequence_id;

   
DROP VIEW IF EXISTS lgd_ways_open;
CREATE VIEW lgd_ways_open AS
   SELECT way_id
   FROM lgd_ways_endpoints
  WHERE min_node_id <> max_node_id;


DROP VIEW IF EXISTS lgd_ways_closed;
CREATE VIEW lgd_ways_closed AS
   SELECT way_id
   FROM lgd_ways_endpoints
  WHERE min_node_id = max_node_id;
   
 
/****************************************************************************
 * relation                                                                 *
 ****************************************************************************/



CREATE VIEW lgd_relation_resource_kv AS 
  SELECT a.relation_id, b.property, b.object
   FROM relation_tags a
   JOIN lgd_map_resource_kv b ON a.k = b.k::text AND a.v = b.v::text;


   
