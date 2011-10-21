#!/bin/bash
WORKING_DIR=`pwd`

rm -rf /tmp/lgd_tmp
mkdir /tmp/lgd_tmp
cp -r /home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/target/classes/* /tmp/lgd_tmp/
cp ./plugin.xml /tmp/lgd_tmp
cp ./TagMappingDB.postgres.cfg.xml /tmp/lgd_tmp

cd /tmp/lgd_tmp
zip -r RDFDeltaOSMPlugin.zip *
cd $WORKING_DIR

cp /tmp/lgd_tmp/RDFDeltaOSMPlugin.zip .

# NOTE This path is a standard path according to http://wiki.openstreetmap.org/wiki/Osmosis/Detailed_Usage#Plugin_Tasks
cp RDFDeltaOSMPlugin.zip ~/.openstreetmap/osmosis/plugins
