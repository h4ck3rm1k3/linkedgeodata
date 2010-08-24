#!/bin/bash

targetPath='/tmp/lgdlive'

hostName="localhost"
dataBaseName="unittest_lgd"
userName="postgres"
passWord="postres"

sleepInterval=60

echo "This script is not done yet, however it already contains the neccessary statements commented out."
echo "Starting an infinite loop of fetching data and patching the database..."


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
	./osmosis --rri workingDirectory=$targetPath --write-xml-change "$targetPath/diff.osc"

	# Synching with a triple store
	# TODO Add the database connection params
	./osmosis	-v --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory

	#./osmosis	-v --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory host=$hostName database=$dataBaseName user=$userName password=$passWord

	# TODO Decide whether the old diffs should be kept or removed
	rm "$targetPath/diff.osc"

	echo "Going to sleep for $sleepInterval seconds..."
	sleep $sleepInterval
done


# osmosis --read-xml-change file="diff.osc" --simplify-change --write-pgsql-change host="localhost" #database="unittest_lgd" user="postgres" password="postgres"

