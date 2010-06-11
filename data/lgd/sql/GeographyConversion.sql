
/*
 * Statements for converting an existing database to geography type
 * (there is probably a better way by just changing the schema type - rather
 * than actually converting the data (I think geography and geometry are binary compatible)
 * 
SELECT id, version, user_id, tstamp, changeset_id, geom::geography INTO geognodes FROM nodes;
ALTER TABLE nodes RENAME TO nodes_orig;
ALTER INDEX idx_nodes_geom RENAME TO idx_nodes_orig_geom;
ALTER TABLE geognodes RENAME to nodes;
ALTER TABLE nodes ADD PRIMARY KEY(id);
CREATE INDEX idx_nodes_geom ON nodes USING GIST(geom);

Same for ways:
SELECT id, version, user_id, tstamp, changeset_id, linestring::geography INTO geogways FROM ways;
ALTER TABLE ways RENAME TO ways_orig;
ALTER INDEX idx_ways_linestring RENAME TO idx_ways_orig_linestring;
ALTER TABLE geogways RENAME to ways;
ALTER TABLE ways ADD PRIMARY KEY(id);
CREATE INDEX idx_ways_linestring ON ways USING GIST(linestring);
*/
 