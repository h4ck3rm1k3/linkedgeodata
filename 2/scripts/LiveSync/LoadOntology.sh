#!/bin/bash

virtLoad="/opt/virtuoso/ose/6.1.2/bin/virtload.sh"

date=$1
source $date/config.ini

ontologyFile="$date/lgd-ontology-$date.owl.nt"


curl -LH "Accept:text/plain" http://linkedgeodata.org/ontology > $ontologyFile

$virtLoad $ontologyFile $rdfStore_graphName $rdfStore_hostName $rdfStore_userName $rdfStore_passWord

