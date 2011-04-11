#!/bin/bash

# The existence of this script is due to the fact that I couldn't figure out how to
# to "dump" the data in a graph based on a sparql query.
# Therefore this script splits a monolithic dump file into multiple smaller ones

filename=$1

filename_extension=${filename##*.}
filename_target=${filename%.*}

nodeFile=$filename_target.Nodes.$filename_extension
wayNodeFile=$filename_target.WayNodes.$filename_extension
wayFile=$filename_target.Ways.$filename_extension

rm "$nodeFile"
rm "$wayNodeFile"
rm "$wayFile"

grep $filename -Ee "^<http://linkedgeodata.org/triplify/node" >> "$nodeFile"
grep $filename -Ee "^<http://linkedgeodata.org/triplify/[^/>]+/nodes" >> "$wayNodeFile"
grep $filename -Ee "^<http://linkedgeodata.org/triplify/way[^/>]*>" >> "$wayFile"

