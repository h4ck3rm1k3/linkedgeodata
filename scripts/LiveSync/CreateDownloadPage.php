<?php

include "convert.php";

function endsWith( $str, $sub ) {
   return ( substr( $str, strlen( $str ) - strlen( $sub ) ) === $sub );
}

$hostName = "http://downloads.linkedgeodata.org/";
$basePath = "/var/www/downloads.linkedgeodata.org/";
$releaseDir = $basePath . "releases/";

$it = new DirectoryIterator($releaseDir);

// Determine an ordering on the directories
$order=array();
while($it->valid()) {
    $filename = $it->getPathname();

    if(preg_match("/(\d{2})(\d{2})(\d{2})/", $filename, $matches)) {
        $order[$matches[0]] = $filename;
    }
   
    $it->next();
}


$releases = array();
// Process the directories
foreach($order as $index=>$dirName) {
	echo "Current dir $dirName\n";

	$release = array();
	$releases[$index] = &$release;

    // Check if the dir contains release-notes
    $releaseNotesWiki = file_get_contents("$dirName/ReleaseNotesWiki.txt");

	$release["releaseNotesWiki"] = $releaseNotesWiki;

	$sortKeyToInis = array();
	$release["files"] = &$sortKeyToInis;

    // Read all meta files
    // Determine an ordering
    $it = new DirectoryIterator("$dirName");

    while($it->valid()) {
        $fileName = $it->getPathname();
        //echo "$fileName\n";

		if(endsWith($fileName, ".meta")) {
			$ini = parse_ini_file($fileName);

			$ini["absoluteFileName"] = $it->getPath() . "/" . $ini["fileName"];
			
			$sortKey = array_key_exists("sortKey", $ini) ? $ini["sortKey"] : 0;
			//echo "$sortKey\n";
			
			if(!array_key_exists($sortKey, $sortKeyToInis)) {
				 $sortKeyToInis[$sortKey] = array();
			}
			$arr = &$sortKeyToInis[$sortKey];
			$arr[] = $ini; 

			
			//print_r($sortKeyToInis);
		}

		$it->next();
    }

}

//print_r($releases);

$humanReadableDate="2011-Feb-23";
echo "#||\n";
foreach($releases as $release) {

	echo "|| **$humanReadableDate** | **Dataset** | **#Triples** | **Download Size** | **Uncompressed** ||\n";


	$sortKeyToInis = &$release["files"];
	
	
	foreach($sortKeyToInis as $sortKey=>$inis) {
		foreach($inis as $ini) {
			$fileName = $ini["absoluteFileName"];
			$cutName = substr($fileName, strlen($basePath));
			
			$url = $hostName . $cutName;
			
			$displayName = $ini["displayName"];

			$previewName = str_replace("/", "_sl_", $cutName);

			$previewLink = "http://downloads.linkedgeodata.org/preview.php?file=$previewName";

			$numTriples = convertCount($ini["lineCount"]);
			$originalSize = convertSize($ini["originalSize"]); 
			$compressedSize = convertSize($ini["compressedSize"]); 
			
			echo "|| | <# <a href='$url'>$displayName</a> #> (<#<a href='$previewLink'>preview</a>#>) | $numTriples | $compressedSize | $originalSize ||\n";
		}
	}

	$releaseNotesWiki = &$release["releaseNotesWiki"];
	if(FALSE != $releaseNotesWiki) {
		echo "|| | " . trim($releaseNotesWiki) . " ||\n";
	}
	
}

//echo "|| | $notes ||\n";
echo "||#\n";

