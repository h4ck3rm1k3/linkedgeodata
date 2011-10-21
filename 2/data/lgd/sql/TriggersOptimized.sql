DROP TABLE lgd_node_tags_actions
CREATE TABLE lgd_node_tags_actions
(
	id INT NOT NULL PRIMARY KEY,
	action_type INT, /* Insert or Delete */
	node_id INT,
	k text,
	v text,
	
	/* temporary fields */
	tile_id INT8
);

DROP FUNCTION lgd_process_table() CASCADE;
CREATE FUNCTION lgd_process_table() RETURNS trigger AS $$
DECLARE
BEGIN	
	
END;
$$
	LANGUAGE plpgsql;

	

DROP FUNCTION lgd_update_lgd_stats_kv() CASCADE;
CREATE FUNCTION lgd_update_lgd_stats_kv() RETURNS trigger AS $$
DECLARE
	old_uc INT; /* usage count */
	new_uc INT; /* usage count */
BEGIN	
	IF(TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
		INSERT INTO lgd_node_tags_actions(action_type, latitude, longitude, k, v) VALUES ("insert", Y(NEW.geom), X(NEW.geom), NEW.k, NEW.v);	
	END IF;

	IF (TG_OP = 'DELETE' OR TG_OP = 'UPDATE') THEN
		INSERT INTO lgd_node_tags_actions(action_type, latitude, longitude, k, v) VALUES ("delete", Y(NEW.geom), X(NEW.geom), OLD.k, OLD.v);	
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
	FOR EACH ROW EXECUTE PROCEDURE lgd_update_node_tags_stats();


	
	
	




DROP FUNCTION LGD_GetTileForNode(INT, bigint);
CREATE FUNCTION LGD_GetTileForNode(zoom INT, node_id bigint) RETURNS INT8 AS $$
DECLARE
	geog geography;
BEGIN
	/* get the tile id for the entity_id */
	SELECT geom INTO geog FROM nodes WHERE id = node_id;
	RETURN LGD_ToTile(geog, zoom);
END;
$$
	LANGUAGE plpgsql;


/* tile based updates */
DROP FUNCTION LGD_InsertNodeTag(INT, INT8, text, text);
CREATE FUNCTION LGD_InsertNodeTag(zoom INT, _tile_id INT8, _k text, _v text) RETURNS VOID AS $$
DECLARE
	rec record;
	table_name_kv text;
	table_name_k text;
	uc_kv INT;
	uc_k INT;
BEGIN
	table_name_kv ='lgd_stats_node_tags_tilekv_' || zoom;
	table_name_k = 'lgd_stats_node_tags_tilek_' || zoom;
	
	FOR rec IN
			EXECUTE 'SELECT usage_count FROM ' || table_name_kv ||
				' WHERE (tile_id, k, v) = ($1, $2, $3)'
			USING _tile_id, _k, _v LOOP

		uc_kv = rec.usage_count;
	END LOOP;
	
	IF(uc_kv IS NULL) THEN
		EXECUTE 'INSERT INTO ' || table_name_kv || '(tile_id, k, v, usage_count) ' ||
			'VALUES($1, $2, $3, 1)' USING _tile_id, _k, _v;

		FOR rec IN
			EXECUTE 'SELECT usage_count FROM ' || table_name_k ||
				' WHERE (tile_id, k) = ($1, $2)'
			USING _tile_id, _k LOOP

			uc_k = rec.usage_count;
		END LOOP;

		IF(uc_k IS NULL) THEN
			EXECUTE 'INSERT INTO ' || table_name_k ||'(tile_id, k, usage_count, distinct_value_count) ' ||
				'VALUES($1, $2, 1, 1)' USING _tile_id, _k;
		ELSE			
			EXECUTE 'UPDATE ' || table_name_k
				|| ' SET usage_count = usage_count + 1,'
				|| ' distinct_value_count = distinct_value_count + 1'
				|| ' WHERE k = $1' USING _k;
		END IF;
	ELSE
		EXECUTE 'UPDATE ' || table_name_kv ||
			' SET usage_count = usage_count + 1 WHERE (k, v) = ($1, $2)' USING _k, _v;		

		EXECUTE 'UPDATE ' || table_name_k
			|| ' SET usage_count = usage_count + 1'
			|| ' WHERE k = $1' USING _k;
	END IF;
END;
$$
	LANGUAGE plpgsql;

/** corresponding test */
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilekv_0 WHERE (k, v) = ('lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilek_0 WHERE k = 'lgdtest';
	
	






DROP FUNCTION LGD_DeleteNodeTag(INT, INT8, text, text);
CREATE FUNCTION LGD_DeleteNodeTag(zoom INT, _tile_id INT8, _k text, _v text) RETURNS VOID AS $$
DECLARE
	rec record;
	table_name_kv text;
	table_name_k text;
	uc_kv INT;
BEGIN
	table_name_kv ='lgd_stats_node_tags_tilekv_' || zoom;
	table_name_k = 'lgd_stats_node_tags_tilek_' || zoom;
	
	FOR rec IN
			EXECUTE 'SELECT usage_count FROM ' || table_name_kv ||
				' WHERE (tile_id, k, v) = ($1, $2, $3)'
			USING _tile_id, _k, _v LOOP

		uc_kv = rec.usage_count;
	END LOOP;
	
	IF(uc_kv > 1) THEN
		EXECUTE 'UPDATE ' || table_name_kv || ' SET usage_count = usage_count - 1 WHERE (k, v) = ($1, $2)' USING _k, _v;		

		EXECUTE 'UPDATE ' || table_name_k
			|| ' SET usage_count = usage_count - 1'
			|| ' WHERE k = $1' USING _k;
	ELSEIF (uc_kv <= 1) THEN
		EXECUTE 'DELETE FROM ' || table_name_kv ||
			' WHERE (tile_id, k, v) = ($1, $2, $3)' USING _tile_id, _k, _v;		
			
		EXECUTE 'UPDATE ' || table_name_k
			|| ' SET usage_count = usage_count - 1,'
			|| ' distinct_value_count = distinct_value_count - 1'
			|| ' WHERE k = $1' USING _k;
	
		/* delete entry if distinct_value_count reached zero */
		EXECUTE 'DELETE FROM ' || table_name_k
			|| ' WHERE (tile_id, k) = ($1, $2) AND distinct_value_count < 1' USING _tile_id, _k;			
	END IF;
END;
$$
	LANGUAGE plpgsql;

SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilekv_0 WHERE (k, v) = ('lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilek_0 WHERE k = 'lgdtest';



SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilekv_0 WHERE (k, v) = ('lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilek_0 WHERE k = 'lgdtest';
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_InsertNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilekv_0 WHERE (k, v) = ('lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilek_0 WHERE k = 'lgdtest';
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT LGD_DeleteNodeTag(0, 0, 'lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilekv_0 WHERE (k, v) = ('lgdtest', 'lgdtest');
SELECT * FROM lgd_stats_node_tags_tilek_0 WHERE k = 'lgdtest';

	
/** tile based updates
 *
 * Whenever a tag is inserted, update the statistic for the lgd_tiles_kv_xx
 * table 
 * 
 */
DROP FUNCTION LGD_TrigFN_NodeTags() CASCADE;
CREATE FUNCTION LGD_TrigFN_NodeTags() RETURNS TRIGGER AS $$
DECLARE
	rec record;
	point Geometry;
	tile_id INT8;
BEGIN
	
	FOR rec IN
		SELECT table_name, substr(table_name, 28)::int AS zoom FROM information_schema.tables
			WHERE table_catalog = current_database() AND table_name LIKE 'lgd_stats_node_tags_tilekv_%'
			LOOP
		
		IF(TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
			SELECT LGD_ToTile(geom::geometry, rec.zoom) INTO tile_id FROM nodes WHERE id = NEW.node_id; 			
			PERFORM LGD_InsertNodeTag(rec.zoom, tile_id, NEW.k, NEW.v);
		END IF;

		IF (TG_OP = 'DELETE' OR TG_OP = 'UPDATE') THEN
			SELECT LGD_ToTile(geom::geometry, rec.zoom) INTO tile_id FROM nodes WHERE id = OLD.node_id; 			
			PERFORM LGD_DeleteNodeTag(rec.zoom, tile_id, OLD.k, OLD.v);
		END IF;
	END LOOP;

	IF (TG_OP = 'DELETE') THEN
		RETURN OLD;
	ELSE
		RETURN NEW;
	END IF;
END;
$$
	LANGUAGE plpgsql;



DROP TRIGGER trg_lgd_stats_node_tags ON node_tags;
CREATE TRIGGER trg_lgd_stats_node_tags BEFORE INSERT OR UPDATE OR DELETE ON node_tags
	FOR EACH ROW EXECUTE PROCEDURE LGD_TrigFN_NodeTags();
	

	
INSERT INTO nodes(id, version, user_id, tstamp, changeset_id, geom) VALUES(0, 0, 0, now(), 0, ST_SetSRID(ST_MakePoint(50.0, 50.0), 4326));
INSERT INTO node_tags(node_id, k, v) VALUES(0, 'a', 'b');
SELECT * FROM lgd_stats_node_tags_tilekv_16 WHERE (k, v) = ('a', 'b');
SELECT * FROM lgd_stats_node_tags_tilek_16 WHERE k = 'a';

	
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





/** junk **/


DROP FUNCTION lgd_test(text);
CREATE FUNCTION lgd_test(_k text) RETURNS int AS $$
DECLARE
BEGIN
	EXECUTE 'PERFORM * FROM ' || 'node_tags' || ' WHERE k = $1' USING _k;

	IF(FOUND) THEN
		RETURN 100;
	ELSE
		RETURN 50;
	END IF;
END;
$$
	LANGUAGE plpgsql;

	
DROP FUNCTION lgd_test(text, INT, INT, text, text);
CREATE FUNCTION lgd_test(base_table_name text, zoom INT, _tile_id INT, _k text, _v text) RETURNS int AS $$
DECLARE
	table_name text;
	uc INT;
	rec record;
BEGIN
	table_name = base_table_name || zoom;
	
	FOR rec IN
			EXECUTE 'SELECT count FROM ' || table_name ||
				' WHERE tile_id = $1 AND k = $2 AND v = $3'
			USING _tile_id, _k, _v LOOP

		RETURN rec.count;

	END LOOP;
		
	RETURN 0;
END;
$$
	LANGUAGE plpgsql;

	
+DROP FUNCTION lgd_test(table_name text);
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


