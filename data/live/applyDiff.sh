#!/bin/bash
source config.ini

java -jar JenaCLIUtils.jar -h"$rdfStore_hostName" -d"$rdfStore_graphName" -u"$rdfStore_userName" -w"$rdfStore_passWord" -f"$1.removed.nt" -r
java -jar JenaCLIUtils.jar -h"$rdfStore_hostName" -d"$rdfStore_graphName" -u"$rdfStore_userName" -w"$rdfStore_passWord" -f"$1.added.nt"


