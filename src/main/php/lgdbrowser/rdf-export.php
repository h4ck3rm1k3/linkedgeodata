<?php

if(!isset($_REQUEST['nodeIds'])) {
	echo "RDF export proxy";
	die;
}

$nodeIds = split(",", $_REQUEST['nodeIds']);



header("Content-type: text/rdf+n3");
header("Content-Disposition: attachment; filename=\"export.n3\"");

$result = fetchMultipleData($nodeIds, "http://linkedgeodata.org/data/node");
if($result == false) {
	echo "Export failed";
	die;
}

echo $result;
	


function fetchData($id, $base)
{
	$url = $base . $id;
	
	$c = curl_init();
	curl_setopt($c, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($c, CURLOPT_URL, $url);
	
	$contents = curl_exec($c);
	curl_close($c);

	return $contents;
}

function fetchMultipleData($ids, $base)
{
	$result = "";
	foreach($ids as $id) {
		$tmp = fetchData($id, $base);
		
		if($tmp === false)
			return false;
		
		$result .= $tmp . "\n"; 
	}
	
	return $result;
}


?>
