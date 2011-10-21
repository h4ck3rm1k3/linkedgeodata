#!/bin/bash

date=$1
configFile="./$date/config.ini"
planetFile="./cache/planet-$date.osm.bz2"

echo "Config file: $configFile"
echo "Planet file: $planetFile"

#java -Xmx2048m -cp ../lgd.jar RunLiveDump "$@" 
java -Xmx2048m -cp ../lgd.jar RunLiveDump -c $configFile -f $planetFile

