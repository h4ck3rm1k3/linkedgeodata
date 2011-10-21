#!/bin/bash

date=$1
configFileName="./$date/config.ini"

m_info()
{
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1"
}

m_error()
{
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ [error] $1"
	exit 1
}

m_ok()
{
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] $$ $1"
}


if ! source $configFileName; then
	m_error "Could not load '$configFileName'"
fi


# Check if we need to initialize the working directory
if [ ! -f "$osmReplicationConfigPath/configuration.txt" ]
then
	mkdir -p $osmReplicationConfigPath
	$osmosisPath/osmosis --read-replication-interval-init workingDirectory=$osmReplicationConfigPath

	if [ $? == 0 ]
	then	
		m_ok "Osmosis target directory has been initialized. Please check configuration in $osmReplicationConfigPath, then run this script again"
	else
		m_error "Osmosis exited with error code ($?)"
	fi
	exit 0
fi


java -Xmx2048m -cp ../lgd.jar RunLiveSync -c "$configFileName"

