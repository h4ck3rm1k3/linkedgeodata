<?php

$str = "
	SELECT
		CONCAT('base:node/', n.id, '#id') AS id,
		CONCAT(
			ROUND(n.latitude  / 10000000, 7),
			' ',
			ROUND(n.longitude / 10000000, 7)
		) AS 't:unc',
		'georss:point' AS a
	FROM
		nodes n
	WHERE
		n.id = $1
	";
		


$str = str_replace('\t', '', $str);
$str = preg_replace('/\s+/', ' ', $str);
$str = str_replace('$1', '35', $str);

echo "$str\n";


?>