/**
 * Lets see if we can (ab)use function indexes as the basis for views
 */
DROP INDEX IF EXISTS idx_node_tags_k_boolean;
CREATE INDEX idx_node_tags_k_boolean ON node_tags(k, LGD_TryParseBoolean(v)) WHERE LGD_TryParseBoolean(v) IS NOT NULL;


/* Int */
DROP INDEX IF EXISTS idx_node_tags_k_int;
CREATE INDEX idx_node_tags_k_int ON node_tags(k, LGD_TryParseInt(v)) WHERE LGD_TryParseInt(v) IS NOT NULL;


/* Float */
DROP INDEX IF EXISTS idx_node_tags_k_float;
CREATE INDEX idx_node_tags_k_float ON node_tags(k, LGD_TryParseFloat(v)) WHERE LGD_TryParseFloat(v) IS NOT NULL;



