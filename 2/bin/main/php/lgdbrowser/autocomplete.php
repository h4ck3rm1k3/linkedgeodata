<?php
include('inc.php');

if($_GET['p']=='property')
	$sql='SELECT label FROM tags INNER JOIN resources r ON(k=r.id AND label LIKE "'.$db->escape_string($_GET['q']).'%") GROUP BY label ORDER BY count(*) DESC LIMIT 10';
else
	$sql='SELECT vr.label FROM tags t INNER JOIN resources kr ON(k=kr.id AND kr.label="'.$db->escape_string($_GET['p']).'")
		INNER JOIN resources vr ON(v=vr.id AND vr.label LIKE "'.$db->escape_string($_GET['q']).'%")
		GROUP BY label ORDER BY count(*) DESC LIMIT 10';
#echo $sql;
$res=$db->query($sql);
while($row=$res->fetch_assoc())
	echo($row['label']."\n");
?>