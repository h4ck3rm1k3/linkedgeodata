/****************************************************************************
 *                                                                          *
 * OpenStreetMap Schema Modifications                                       *
 *                                                                          *
 ****************************************************************************/

CREATE INDEX idx_node_tags_k on node_tags(k);
CREATE INDEX idx_node_tags_k_v on node_tags(k, v);
CREATE INDEX idx_node_tags_v_k on node_tags(v, k);
CREATE INDEX idx_node_tags_k_boolean ON node_tags(k, lgd_tryparse_boolean(v)) WHERE lgd_tryparse_boolean(v) IS NOT NULL;
-- CREATE INDEX idx_node_tags_k_int ON node_tags(k, lgd_tryparse_int(v)) WHERE lgd_tryparse_int(v) IS NOT NULL;
CREATE INDEX idx_node_tags_k_float ON node_tags(k, lgd_tryparse_float(v)) WHERE lgd_tryparse_float(v) IS NOT NULL;


CREATE INDEX idx_way_tags_k on way_tags(k);
CREATE INDEX idx_way_tags_k_v ON way_tags(k, v);
CREATE INDEX idx_way_tags_v_k on way_tags(v, k);
CREATE INDEX idx_way_tags_k_boolean ON way_tags(k, lgd_tryparse_boolean(v)) WHERE lgd_tryparse_boolean(v) IS NOT NULL;
-- CREATE INDEX idx_way_tags_k_int ON way_tags(k, lgd_tryparse_int(v)) WHERE lgd_tryparse_int(v) IS NOT NULL;
CREATE INDEX idx_way_tags_k_float ON way_tags(k, lgd_tryparse_float(v)) WHERE lgd_tryparse_float(v) IS NOT NULL;

CREATE INDEX idx_relation_tags_k on relation_tags(k);
CREATE INDEX idx_relation_tags_k_v ON relation_tags(k, v);
CREATE INDEX idx_relation_tags_v on relation_tags(v);
CREATE INDEX idx_relation_tags_k_boolean ON relation_tags(k, lgd_tryparse_boolean(v)) WHERE lgd_tryparse_boolean(v) IS NOT NULL;
-- CREATE INDEX idx_relation_tags_k_int ON relation_tags(k, lgd_tryparse_int(v)) WHERE lgd_tryparse_int(v) IS NOT NULL;
CREATE INDEX idx_relation_tags_k_float ON relation_tags(k, lgd_tryparse_float(v)) WHERE lgd_tryparse_float(v) IS NOT NULL;


CREATE INDEX idx_nodes_tstamp on nodes(tstamp);
CREATE INDEX idx_ways_tstamp on ways(tstamp);
CREATE INDEX idx_relations_tstamp on relations(tstamp);




/**
 * 
 * Optionally: We could create an index:
 *
 * CREATE INDEX idx_node_tags_k_string ON node_tags(k, v) WHERE lgd_tryparse_boolean(v) IS NULL AND lgd_tryparse_float(v) IS NULL;
 * 
 * Then we could get rid of the  idx_node_tags_k_v
 */



/* Relations */
CREATE INDEX idx_relation_tags_k on relation_tags(k);
CREATE INDEX idx_relation_tags_k_v on relation_tags(k, v);
CREATE INDEX idx_relation_tags_v_k on relation_tags(v, k);


CREATE INDEX idx_relation_members_relation_id on relation_members(relation_id);
CREATE INDEX idx_relation_members_member_id on relation_members(member_id);
CREATE INDEX idx_relation_members_member_role on relation_members(member_role);

/* Efficient scans on members of a certain type */
CREATE INDEX idx_relation_members_member_type_relation_id on relation_members(member_type, relation_id);


