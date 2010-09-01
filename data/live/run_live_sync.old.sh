#!/bin/bash

configFileName="./config.ini"

m_info()
{
#        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1" >> "$RUNLOG"
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1"
}

m_error()
{
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ [error] $1"
#        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ [error] $1" >> "$RUNLOG"
	exit 1
}

m_ok()
{
#        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1" >> "$RUNLOG"
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1"
}


echo "Starting LinkedGeoData live update (based on osmosis)..."


if [ ! -f $configFileName ]; then
	m_info "No config found, attempting to copy '$configFileName.dist' to '$configFileName'"
	cp "$configFileName.dist" $configFileName
fi

if ! source $configFileName; then
	m_error "Could not load '$configFileName'"
fi

mkdir -p $targetPath

# Check if we need to initialize the working directory
if [ ! -f "$targetPath/configuration.txt" ]
then
	$osmosisPath/osmosis --read-replication-interval-init workingDirectory=$targetPath

	if [ $? == 0 ]
	then	
		m_info "Osmosis target directory has been initialized. Please check configuration in $targetPath, then run this script again"
	else
		m_error "Osmosis exited with error code ($?)"
	fi
	exit 0
fi


while [ 1 ]
do
	# TODO Decide whether the old diffs should be kept or removed
	# Remove possibly existing diff.osc file
	rm "$targetPath/diff.osc"

	$osmosisPath/osmosis --read-replication-interval workingDirectory=$targetPath --simplify-change --write-xml-change "$targetPath/diff.osc"

	# Synching with a triple store
	# TODO Add the database connection params
	$osmosisPath/osmosis -v 1 --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory

	#./osmosis -v --read-xml-change file="$targetPath/diff.osc" --liveRDFPluginFactory host=$hostName database=$dataBaseName user=$userName password=$passWord

	echo "Going to sleep for $sleepInterval seconds..."
	sleep $sleepInterval
done


# osmosis --read-xml-change file="diff.osc" --simplify-change --write-pgsql-change host="localhost" #database="unittest_lgd" user="postgres" password="postgres"

