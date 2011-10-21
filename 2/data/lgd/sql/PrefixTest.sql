CREATE TABLE prefixes (
	prefix prefix_range PRIMARY KEY NOT NULL
);

INSERT INTO prefixes(prefix) VALUES ('http://example.org/');

INSERT INTO prefixes(prefix) VALUES ('http://linkedgeodata.org/');
INSERT INTO prefixes(prefix) VALUES ('http://linkedgeodata.org/resource/');
INSERT INTO prefixes(prefix) VALUES ('http://linkedgeodata.org/resource/node/');

