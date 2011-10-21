#!/bin/bash
#Arguments: latMin, latMax, lonMin, lonMax

rm -f /tmp/lgd.data.rest.nt /tmp/lgd.data.sparql.nt /tmp/lgd.data.rest.sorted.nt /tmp/lgd.data.sparql.sorted.nt

curl -LH "Accept: text/plain" "http://linkedgeodata.org/triplify/near/$1-$2,$3-$4" > /tmp/lgd.data.rest.nt
sort -u /tmp/lgd.data.rest.nt > /tmp/lgd.data.rest.sorted.nt

url="http://linkedgeodata.org/sparql/?default-graph-uri=http%3A%2F%2Flinkedgeodata.org&query=BASE+<http%3A%2F%2Flinkedgeodata.org>%0D%0APREFIX+lgdo%3A+<http%3A%2F%2Flinkedgeodata.org%2Fontology%2F>%0D%0APREFIX+geo%3A+<http%3A%2F%2Fwww.w3.org%2F2003%2F01%2Fgeo%2Fwgs84_pos%23>%0D%0APREFIX+rdfs%3A+<http%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23>%0D%0APREFIX+owl%3A+<http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23>%0D%0ACONSTRUCT+%7B+%3Fpoi+%3Fp+%3Fo+%7D+FROM+<http%3A%2F%2Flinkedgeodata.org>%0D%0AWHERE+%7B%0D%0A%3Fpoi++++++++geo%3Ageometry+%3Fpoigeo+.%0D%0A%3Fpoi++++++++geo%3Alat++++++%3Fpoilat+.%0D%0A%3Fpoi++++++++geo%3Along+++++%3Fpoilong+.%0D%0A%3Fpoi++++++++%3Fp+++++++++++%3Fo.%0D%0AFILTER(%0D%0A%3Fpoilat+>%3D+\"$1\"^^<http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23double>+%26%26%0D%0A%3Fpoilat+<%3D+\"$2\"^^<http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23double>+%26%26%0D%0A%3Fpoilong+>%3D+\"$3\"^^<http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23double>+%26%26%0D%0A%3Fpoilong+<%3D+\"$4\"^^<http%3A%2F%2Fwww.w3.org%2F2001%2FXMLSchema%23double>%0D%0A)+.%0D%0A%7D+&format=text%2Fplain&debug=on&timeout="

echo $url

curl -L $url > /tmp/lgd.data.sparql.nt
#curl -LH "Accept: Accept: text/plain" $url > /tmp/rdf.diff.b.nt

sort -u /tmp/lgd.data.sparql.nt > /tmp/lgd.data.sparql.sorted.nt

meld /tmp/lgd.data.rest.sorted.nt /tmp/lgd.data.sparql.sorted.nt

