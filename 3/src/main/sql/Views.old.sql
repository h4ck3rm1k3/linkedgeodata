
  
  
-- IGNORE BELOW
  
  
  
  
  
  
  
  
  
  
  
  
  
/****************************************************************************
 * node_tags                                               *
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
*/
   
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
   JOIN lgd_map_resource_prefix b USING(k)
 WHERE
  NOT EXISTS (SELECT c.k FROM lgd_map_datatype c WHERE c.k = b.k); 


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
   
   
DROP VIEW IF EXISTS lgd_way_tags_boolean;
CREATE VIEW lgd_way_tags_boolean AS
  SELECT a.way_id, a.k, lgd_tryparse_boolean(a.v) AS v
   FROM way_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_boolean(a.v) IS NOT NULL AND b.datatype = 'boolean'::lgd_datatype;


DROP VIEW IF EXISTS lgd_way_tags_int;
CREATE VIEW lgd_way_tags_int AS
  SELECT a.way_id, a.k, lgd_tryparse_int(a.v) AS v
   FROM way_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_int(a.v) IS NOT NULL AND b.datatype = 'int'::lgd_datatype;
  

DROP VIEW IF EXISTS lgd_way_tags_float;
CREATE VIEW lgd_way_tags_float AS
  SELECT a.way_id, a.k, lgd_tryparse_float(a.v) AS v
   FROM way_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_float(a.v) IS NOT NULL AND b.datatype = 'float'::lgd_datatype;

  
/**
 * Everything that is neither mapped to a datatype nor to a class/object property
 * becomes a datatype property
 */
DROP VIEW IF EXISTS lgd_way_tags_string;
CREATE VIEW lgd_way_tags_string AS
	SELECT a.way_id, a.k, a.v FROM way_tags a WHERE
		NOT EXISTS (SELECT b.k FROM lgd_map_datatype  b WHERE b.k = a.k) AND 
		NOT EXISTS (SELECT c.k FROM lgd_map_resource_k  c WHERE c.k = a.k) AND 
		NOT EXISTS (SELECT d.k FROM lgd_map_resource_kv d WHERE (d.k, d.v) = (a.k, a.v)) AND 
		NOT EXISTS (SELECT e.k FROM lgd_map_literal e WHERE e.k = a.k) AND
		NOT EXISTS (SELECT f.k FROM lgd_map_property f WHERE f.k = a.k) AND 
		NOT EXISTS (SELECT g.k FROM lgd_map_resource_prefix g WHERE g.k = a.k); 

		
DROP VIEW IF EXISTS lgd_way_tags_text;
CREATE VIEW lgd_way_tags_text AS
 SELECT a.way_id, b.property, a.v, b.language
   FROM way_tags a
   JOIN lgd_map_literal b ON b.k = a.k;

		
/*
DROP VIEW IF EXISTS lgd_way_tags_text;
CREATE VIEW lgd_way_tags_text AS
 SELECT a.way_id, b.property, a.v, b.language
   FROM lgd_way_tags_string a
   JOIN lgd_map_literal b ON b.k = a.k;
*/
   
DROP VIEW IF EXISTS lgd_way_tags_resource_k;
CREATE VIEW lgd_way_tags_resource_k AS
 SELECT a.way_id, b.property, b.object
   FROM way_tags a
   JOIN lgd_map_resource_k b USING(k);

  
DROP VIEW IF EXISTS lgd_way_tags_resource_kv;
CREATE VIEW lgd_way_tags_resource_kv AS   
  SELECT a.way_id, b.property, b.object
   FROM way_tags a
   JOIN lgd_map_resource_kv b USING(k, v);

/****************************************************************************
 * relation                                                                 *
 ****************************************************************************/



CREATE VIEW lgd_relation_resource_kv AS 
  SELECT a.relation_id, b.property, b.object
   FROM relation_tags a
   JOIN lgd_map_resource_kv b ON a.k = b.k::text AND a.v = b.v::text;

