#!/bin/bash

cacheDir=./cache

#Input date format: yymmdd



osmDate=$1
date=$osmDate


tmpDate=$((`date -d "$osmDate" +%s`-42000))


planetFile="planet-$osmDate.osm.bz2"   


cacheFile="$cacheDir/$planetFile"

#Check if data for date exists
#TODO Perform MD5 check
#echo "File is: $cacheFile"

if [ ! -f "$cacheFile" ]; then
    url="http://planet.openstreetmap.org/$planetFile"

#    echo "Downloading $url"
    wget -P $cacheDir $url
fi

date=$1

source config-base.ini


mkdir $date
cd $date


#Write the config

nodePositionDbName="lgd_nodes"
nodePositionTableName="node_position_$date"
osmData="$date/osmdata"

rm -f config.ini
echo "#Do not edit this file. Changes might become overridden." >> config.ini
echo "source ./config-base.ini" >> config.ini
echo "osmReplicationConfigPath=$osmData" >> config.ini
echo "osmDb_dataBaseName=$nodePositionDbName" >> config.ini
echo "publishDiffRepoPath=/var/www/downloads.linkedgeodata.org/dumps/$date/changesets/minutely" >> config.ini
echo "rdfStore_graphName=http://linkedgeodata.org/$osmDate" >> config.ini
echo "nodePositionTableName=$nodePositionTableName" >> config.ini

#Create the database
#echo "Setting up database: createdb -U$osmDb_userName -W$osmDb_passWord $nodePositionDbName"
#createdb -U"$osmDb_userName" -W "$nodePositionDbName"


echo "Date is: $tmpDate"

#Download the statefile
yy=`date -d "@$tmpDate" +%y`
Y="20$yy"
m=`date -d "@$tmpDate" +%m`
d=`date -d "@$tmpDate" +%d`
H=`date -d "@$tmpDate" +%H`
i=`date -d "@$tmpDate" +%M`
s=`date -d "@$tmpDate" +%S`


mkdir osmdata
curl "http://toolserver.org/~mazder/replicate-sequences/?Y=$Y&m=$m&d=$d&H=$H&i=$i&s=$s#" > osmdata/state.txt

