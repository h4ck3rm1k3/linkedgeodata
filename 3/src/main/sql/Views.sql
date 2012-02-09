   
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
CREATE VIEW lgd_tags_int AS
  SELECT a.node_id, a.k, lgd_tryparse_int(a.v) AS v
   FROM node_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_int(a.v) IS NOT NULL AND b.datatype = 'int'::lgd_datatype;
  

DROP VIEW IF EXISTS lgd_node_tags_float;
CREATE VIEW lgd_node_tags_float AS
  SELECT a.node_id, a.k, lgd_tryparse_float(a.v) AS v
   FROMnode_tags a
   JOIN lgd_map_datatype b ON a.k = b.k
  WHERE lgd_tryparse_float(a.v) IS NOT NULL AND b.datatype = 'float'::lgd_datatype;

  
/**
 * Everything that is neither mapped to a datatype nor to a class/object property
 * becomes a datatype property
 */
DROP VIEW IF EXISTS lgd_node_tags_string;
CREATE VIEW lgd_node_tags_string AS
	SELECT a.node, a.k, a.v FROM node_tags a WHERE
		NOT EXISTS (SELECT b.k FROM lgd_map_datatype  b WHERE b.k = a.k) AND 
		NOT EXISTS (SELECT c.k FROM lgd_map_resource_k  c WHERE c.k = a.k) AND 
		NOT EXISTS (SELECT d.k FROM lgd_map_resource_kv d WHERE (d.k, d.v) = (a.k, a.v)) AND 
		NOT EXISTS (SELECT e.k FROM lgd_map_literal e WHERE e.k = a.k) AND
		NOT EXISTS (SELECT f.k FROM lgd_map_property f WHERE f.k = a.k) AND 
		NOT EXISTS (SELECT g.k FROM lgd_map_resource_prefix g WHERE g.k = a.k); 

		
DROP VIEW IF EXISTS lgd_node_tags_text;
CREATE VIEW lgd_tags_text AS
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
   JOIN lgd_map_resource_k b USING(k)
 WHERE
  NOT EXISTS (SELECT c.k FROM lgd_map_datatype c WHERE c.k = b.k); 

  
DROP VIEW IF EXISTS lgd_node_tags_resource_kv;
CREATE VIEW lgd_node_tags_resource_kv AS   
  SELECT a.osm_node_id, b.property, b.object
   FROM node_tags a
   JOIN lgd_map_resource_kv b USING(k, v)
 WHERE
  NOT EXISTS (SELECT c.k FROM lgd_map_datatype c WHERE c.k = b.k); 


DROP VIEW IF EXISTS lgd_node_tags_resource_prefix;
CREATE VIEW lgd_node_tags_resource_prefix AS   
  SELECT a.node_id, b.property, b.object_prefix, b.v, b.post_processing
   FROM node_tags a
   JOIN lgd_map_resource_prefix b USING(k)
 WHERE
  NOT EXISTS (SELECT c.k FROM lgd_map_datatype c WHERE c.k = b.k); 

DROP VIEW IF EXISTS lgd_node_tags_property;
CREATE VIEW lgd_node_tags_property AS   
  SELECT a.node_id, b.property, b.v "object"
   FROM node_tags a
   JOIN lgd_map_property b USING(k)
 WHERE
  NOT EXISTS (SELECT c.k FROM lgd_map_datatype c WHERE c.k = b.k) AND 
  NOT EXISTS (SELECT d.k FROM lgd_map_resource_k d WHERE d.k = b.k) AND 
  NOT EXISTS (SELECT e.k FROM lgd_map_resource_kv e WHERE e.k = b.k AND 
  NOT EXISTS (SELECT f.k FROM lgd_map_literal f WHERE f.k = b.k) AND
  NOT EXISTS (SELECT h.k FROM lgd_map_resource_prefix h WHERE h.k = b.k)); 
