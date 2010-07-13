DROP FUNCTION lgd_get_tile_for_node(INT, bigint);
CREATE FUNCTION lgd_get_tile_for_node(zoom INT, node_id bigint) RETURNS INT8 AS $$
DECLARE
	geog geography;
BEGIN
	/* get the tile id for the entity_id */
	SELECT geom INTO geog FROM nodes WHERE id = node_id;
	RETURN lgd_to_tile(geog, zoom);
END;
$$
	LANGUAGE plpgsql;

	

	


/** tile based updates
 *
 * Whenever a tag is inserted, update the statistic for the lgd_tiles_kv_xx
 * table 
 * 
 */
DROP FUNCTION lgd_helper_insert_tag_tile_kv(text, text, INT);
CREATE FUNCTION lgd_helper_insert_tag_tile_kv(table_name text, level INT, _id _k text, _v text, uc INT) RETURNS VOID AS $$
BEGIN
	/* get the tile id for the entity_id */
	FOR rec IN EXECUTE 'SELECT k FROM ' || table_name || ' LIMIT 1' LOOP
		RETURN rec.k;
	END LOOP;

	
	IF(uc IS NULL) THEN
			INSERT INTO lgd_stats_kv(k, v, usage_count) VALUES(_k, _v, 1);
		ELSE
			UPDATE lgd_stats_kv SET usage_count = uc + 1 WHERE k = _k AND v = _v;		
		END IF;
END;
$$
	LANGUAGE plpgsql;

	
DROP FUNCTION lgd_test(table_name text);
CREATE FUNCTION lgd_test(table_name text) RETURNS text AS $$
DECLARE
	rec record;
	result text;
BEGIN
	FOR rec IN EXECUTE 'SELECT k FROM ' || table_name || ' LIMIT 1' LOOP
		RETURN rec.k;
	END LOOP;
 
	RETURN NULL;
END;
$$
	LANGUAGE plpgsql;


	
/** global updates **/


DROP FUNCTION lgd_helper_insert_tag(text, text, INT);
CREATE FUNCTION lgd_helper_insert_tag(_k text, _v text, uc INT) RETURNS VOID AS $$
BEGIN
		IF(uc IS NULL) THEN
			INSERT INTO lgd_stats_kv(k, v, usage_count) VALUES(_k, _v, 1);
		ELSE
			UPDATE lgd_stats_kv SET usage_count = uc + 1 WHERE k = _k AND v = _v;		
		END IF;
END;
$$
	LANGUAGE plpgsql;


DROP FUNCTION lgd_helper_delete_tag(text, text, INT);
CREATE FUNCTION lgd_helper_delete_tag(_k text, _v text, uc INT) RETURNS VOID AS $$
BEGIN
		IF(uc IS NULL) THEN
			/* nothing to do here */
		ELSE
			IF(uc <= 1) THEN
				DELETE FROM lgd_stats_kv WHERE k = _k AND v = _v;
			ELSE
				UPDATE lgd_stats_kv SET usage_count = uc - 1 WHERE k = _k AND v = _v;
			END IF;
		END IF;
END;
$$
	LANGUAGE plpgsql;

	

	
/**
 * The following updates are performed whenever a new tag is inserted/deleted:
 * .) lgs_stats_kv(usage_count)
 * .) lgd_stats_k(usage_count, distinct_value_count)
 * .) lgd_stats_k(distinct_value_count)
 */
DROP FUNCTION lgd_update_lgd_stats_kv() CASCADE;
CREATE FUNCTION lgd_update_lgd_stats_kv() RETURNS trigger AS $$
DECLARE
	old_uc INT; /* usage count */
	new_uc INT; /* usage count */
BEGIN	
	IF(TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
		SELECT usage_count INTO new_uc FROM lgd_stats_kv WHERE k = NEW.k AND v = NEW.v;
		PERFORM lgd_helper_insert_tag(NEW.k, NEW.v, new_uc);		
	END IF;

	IF (TG_OP = 'DELETE' OR TG_OP = 'UPDATE') THEN
		SELECT usage_count INTO old_uc FROM lgd_stats_kv WHERE k = OLD.k AND v = OLD.v;
		PERFORM lgd_helper_delete_tag(OLD.k, OLD.v, old_uc);
	END IF;
	
	IF (TG_OP = 'DELETE') THEN
		RETURN OLD;
	ELSE
		RETURN NEW;
	END IF;
END;
$$
	LANGUAGE plpgsql;


DROP TRIGGER trg_node_tags ON node_tags;
CREATE TRIGGER trg_node_tags BEFORE INSERT OR UPDATE OR DELETE ON node_tags
	FOR EACH ROW EXECUTE PROCEDURE lgd_update_lgd_stats_kv();
	

	

/** --- part 2 ------------------------------------------------------------**/
	
DROP FUNCTION lgd_helper_insert_tag_k(text, INT, INT);
CREATE FUNCTION lgd_helper_insert_tag_k(_k text, uc INT, dvt INT) RETURNS VOID AS $$
DECLARE
	old_uc  INT;
	new_uc  INT;
	old_dvt INT;
	new_dvt INT;
BEGIN
		SELECT usage_count, distinct_value_count INTO new_uc, new_dvt FROM lgd_stats_k WHERE k = _k;
		
		IF(new_dvt IS NULL) THEN
			INSERT INTO lgd_stats_k(k, usage_count, distinct_value_count) VALUES (NEW.k, 0, 1);
		ELSE
			UPDATE lgd_stats_k SET distinct_value_count = distinct_value_count + 1;
		END IF;
		
		UPDATE lgd_stats_k SET usage_count = usage_count + 1;
END;
$$
	LANGUAGE plpgsql;



CREATE FUNCTION lgd_update_lgd_stats_k() RETURNS trigger AS $$
DECLARE
	old_uc  INT;
	new_uc  INT;
	old_dvt INT;
	new_dvt INT;
BEGIN
	IF(TG_OP = 'INSERT') THEN
	END IF;
	
	
	
	IF(TG_OP = 'DELETE') THEN
		SELECT usage_count, distinct_value_count INTO old_uc, old_dvt FROM lgd_stats_k WHERE k = OLD.k;

		IF(usage_count <= 1) THEN
			DELETE FROM lgd_stats_k WHERE k = OLD.k;
		ELSE 
			UPDATE lgd_stats_k SET (usage_count, distinct_value_count) = (usage_count - old_uc, distinct_value_count - 1);
			/* note: here we subtract the remaining usage_count, which should usually (always?) be 1 */
		END IF;
		
	END IF;
	
END;
$$
	LANGUAGE plpgsql;

/** testing --- ***/

SELECT * FROM node_tags WHERE node_id = 1000000000000;
	
INSERT INTO node_tags(node_id, k, v) VALUES (1000000000000, 'amenity', 'parking');
SELECT * FROM lgd_stats_kv WHERE k = 'amenity' AND v = 'parking';

DELETE FROM node_tags WHERE node_id = 1000000000000;
SELECT * FROM lgd_stats_kv WHERE k = 'amenity' AND v = 'parking';


INSERT INTO node_tags(node_id, k, v) VALUES (1000000000000, 'should not', 'exist!');
SELECT * FROM lgd_stats_kv WHERE k = 'should not' AND v = 'exist!';

DELETE FROM node_tags WHERE node_id = 1000000000000;
SELECT * FROM lgd_stats_kv WHERE k = 'should not' AND v = 'exist!';

/** test succeeded so far **/

/** now for updating the lgd_stats_k table **/



UPDATE lgd_stats_kv SET usage_count = 709 WHERE k = 'amenity' AND v = 'parking';


