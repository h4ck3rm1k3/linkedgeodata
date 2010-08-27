#!/bin/bash

targetPath='/tmp/lgdlive'

hostName="localhost"
dataBaseName="unittest_lgd"
userName="postgres"
passWord="postres"

sleepInterval=60

echo "Starting LinkedGeoData live update (based on osmosis)..."

mkdir -p $targetPath

# Check if we need to initialize the working directory
if [ ! -f "$targetPath/configuration.txt" ]
then
	./osmosis --rrii workingDirectory=$targetPath

	if [ $? == 0 ]
	then	
		echo "Osmosis target directory has been initialized. Please check configuration in $targetPath, then run this script again"
	else
		echo "Osmosis exited with error code ($?)"
	fi
	exit 0
fi


while [ 1 ]
do
        # TODO Decide whether the old diffs should be kept or removed
	# Remove possibly existing diff.osc file
        rm "$targetPath/diff.osc"

	./osmosis --rri workingDirectory=$targetPath --write-xml-change "$targetPath/diff.osc"

	# Synching with a triple store
	# TODO Add the database connection params
	./osmosis -v 1 --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory

	#./osmosis -v --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory host=$hostName database=$dataBaseName user=$userName password=$passWord

	echo "Going to sleep for $sleepInterval seconds..."
	sleep $sleepInterval


done


# osmosis --read-xml-change file="diff.osc" --simplify-change --write-pgsql-change host="localhost" #database="unittest_lgd" user="postgres" password="postgres"

