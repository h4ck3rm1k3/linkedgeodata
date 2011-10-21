<?php


function convert($numbers, $denominator, $precision, $readable)
{
   $index=0;
   while($numbers >= $denominator){
      $numbers /= $denominator;
      $index++;
   }
   return("".round($numbers, $precision)."".$readable [$index]);
}

function convertSize($numbers)
{
	return convert($numbers, 1024, 0, array("B", "KB", "MB", "GB", "TB"));
}

function convertCount($numbers)
{
	return convert($numbers, 1024, 0, array("", "K", "Mio", "Bil"));
}


//echo convert($argv[1]) . "\n";

