#!/bin/bash
source config.ini

$osmosisPath/osmosis -v 1 --read-xml-change file="diff.osc" --liveRDFPluginFactory

