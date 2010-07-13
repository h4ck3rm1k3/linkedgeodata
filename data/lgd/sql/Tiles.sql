DROP FUNCTION LGD_WidenBits(val INT4, n INT4);
CREATE FUNCTION LGD_WidenBits(val INT4, n INT4) RETURNS INT8 AS
$$
DECLARE
    m INT4;
	tmp INT8;
	result INT8 := 0;
BEGIN
    m = n - 1;

    FOR i IN 0..m LOOP
        tmp := (val >> i) & 1;
        tmp := tmp << (i * 2);
        result := result | tmp;
    END LOOP;
    RETURN result;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;


DROP FUNCTION LGD_ShrinkBits(INT8, INT4);
CREATE FUNCTION LGD_ShrinkBits(val INT8, n INT4) RETURNS INT4 AS
$$
DECLARE
    m   INT4;
	tmp INT4;
	result INT4 := 0;
BEGIN
    m = n - 1;

    FOR i IN 0..m LOOP
        tmp := (val >> (i * 2)) & 1;
        tmp := tmp << i;
        result := result | tmp;
    END LOOP;
    RETURN result;
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;




DROP FUNCTION LGD_BitZip(lon INT4, lat INT4, n INT4);
CREATE FUNCTION LGD_BitZip(lon INT4, lat INT4, n INT4) RETURNS INT8 AS
$$
BEGIN    
    RETURN (LGD_WidenBits(lon, n) << 1) | LGD_WidenBits(lat, n); 
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;


DROP FUNCTION lgd_to_tile(geom Geometry, zoom INT);
CREATE FUNCTION lgd_to_tile(geom Geometry, zoom INT) RETURNS INT8 AS
$$
DECLARE
	f   INT4;
	lat INT4;
	lon INT4;
	tmpLat INT4;
	tmpLon INT4;
BEGIN
    f   := POW(2, zoom);
    lat := TRUNC((Y(geom) + 90.0) / 180.0 * f)::INT4;
	lon := TRUNC((X(geom) + 180.0) / 360.0 * f)::INT4;
    
    IF lat >= f THEN tmpLat := f - 1; ELSE tmpLat := lat; END IF;
    IF lon >= f THEN tmpLon := f - 1; ELSE tmpLon := lon; END IF;

    RETURN LGD_BitZip(tmpLon, tmpLat, zoom); 
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;



DROP FUNCTION lgd_to_tile(geog Geography, zoom INT);
CREATE FUNCTION lgd_to_tile(geog Geography, zoom INT) RETURNS INT8 AS
$$
BEGIN
    RETURN lgd_to_tile(geog::geometry, zoom); 
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

    
DROP FUNCTION LGD_ToBBox(tile INT8, zoom INT);
CREATE FUNCTION LGD_ToBBox(tile INT8, zoom INT) RETURNS Geometry AS
$$
DECLARE
    f      FLOAT;
    lat    INT4;
    lon    INT4;
	latMin FLOAT;
	latMax FLOAT;
	lonMin FLOAT;
	lonMax FLOAT;
BEGIN
    f := POW(2, zoom);
    lat := LGD_ShrinkBits(tile, zoom);
    lon := LGD_ShrinkBits(tile >> 1, zoom);
    
    latMin := lat / f * 180.0 - 90.0;
	latMax := (lat + 1) / f * 180.0 - 90.0;

	lonMin := lon / f * 360.0 - 180.0;
	lonMax := (lon + 1) / f * 360.0 - 180.0;


    RETURN ST_SetSRID(ST_MakeBox2D(ST_MakePoint(lonMin, latMin), ST_MakePoint(lonMax, latMax)), 4326); 
END;
$$
    LANGUAGE 'plpgsql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;
