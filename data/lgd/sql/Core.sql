DROP TYPE IF EXISTS OSMEntityType;
CREATE TYPE OSMEntityType AS ENUM ('node', 'way', 'relation');

/**
 * A view for uniform access to all sorts of tags
 * 
 */
CREATE VIEW lgd_tags AS
	SELECT
		t.osm_entity_type, t.osm_entity_id, t.k, t.v
	FROM
		((SELECT OSMEntityType('node') AS osm_entity_type, node_id AS osm_entity_id, k, v FROM node_tags) UNION ALL
		 (SELECT OSMEntityType('way') AS osm_entity_type, way_id AS osm_entity_id, k, v FROM way_tags) UNION ALL
		 (SELECT OSMEntityType('relation') AS osm_entity_type, relation_id AS osm_entity_id, k, v FROM relation_tags)) AS t;

