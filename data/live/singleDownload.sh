#!/bin/bash
source config.ini

source $osmReplicationConfigPath/configuration.txt
source $osmReplicationConfigPath/state.txt


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



oscPath()
{
        fragment=`./id_to_path.sh $1`
        fileName=`./id_to_file.sh $1`
	echo "$baseUrl/$fragment/$fileName.osc.gz"
}

statePath()
{
        fragment=`./id_to_path.sh $1`
        fileName=`./id_to_file.sh $1`
	echo "$baseUrl/$fragment/$fileName.state.txt"
}

mkdir -p $tmpPath

echo "Creating backup of current state"
if ! cp $osmReplicationConfigPath/state.txt $osmReplicationConfigPath/state.txt.bak; then
	m_error "Could not create backup of state file"
fi

echo "Downloading Osmosis Changeset"
if ! wget -q `oscPath $sequenceNumber` -O"$tmpPath/diff.osc.gz"; then
	m_error "Could not download changeset"
fi


echo "Unpacking Osmosis Changeset"
if ! gzip -df "$tmpPath/diff.osc.gz"; then
	m_error "Could not unpack changeset"
fi

echo "Starting osmosis task for updating the DB"
#TODO

echo "Starting osmosis task for writing out the diff"

if ! $osmosisPath/osmosis --read-xml-change file="$tmpPath/diff.osc" $entityFilter $tagFilter --liveRDFPluginFactory hostName="$rdfStore_hostName" graphName="$rdfStore_graphName" userName="$rdfStore_userName" passWord="$rdfStore_passWord" outFileBase="$tmpPath/diff"; then
	m_error "Osmosis task failed"
fi

echo "Applying the diff"
if ! ./applyDiff.sh "$tmpPath/diff"; then
	m_error "Could not apply diff"
fi

echo "Publishing the diff"
fragment=`./id_to_path.sh $sequenceNumber`
fileName=`./id_to_file.sh $sequenceNumber`
publishPath="$publishDiffRepoPath/$fragment"

mkdir -p $publishPath
publishFilePath=$publishPath/$fileName

cp "$tmpPath/diff.added.nt" "$publishFilePath.added.nt"
cp "$tmpPath/diff.removed.nt" "$publishFilePath.removed.nt"
cp $osmReplicationConfigPath/state.txt "$publishFilePath.state.txt"

gzip -f "$publishFilePath.added.nt"
gzip -f "$publishFilePath.removed.nt"

echo "Downloading new state"
let newSequenceNumber=$sequenceNumber+1

wget -q `statePath $newSequenceNumber` -O "$osmReplicationConfigPath/state.txt"

