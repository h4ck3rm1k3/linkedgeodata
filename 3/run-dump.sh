#!/bin/bash
java -cp target/lgd3-0.0.1-SNAPSHOT-jar-with-dependencies.jar "org.linkedgeodata.dump.LgdDumper" -h localhost -u postgres -p postgres -d osm_bremen_snapshot

