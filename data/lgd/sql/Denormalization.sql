/* Add the geom column to the node_tags column */
ALTER TABLE node_tags ADD COLUMN geom Geography;
UPDATE node_tags nt SET geom = (SELECT n.geom FROM nodes n WHERE n.id = nt.node_id);
CREATE INDEX idx_node_tags_geom ON node_tags USING GIST(geom); 
