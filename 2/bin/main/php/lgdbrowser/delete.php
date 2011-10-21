<?php
session_start();
include('inc.php');

$c=curl_init('http://api.openstreetmap.org/api/0.5/node/'.$_GET['id']); 
$fp=fopen($file,'r');
curl_setopt_array($c,array(
	CURLOPT_CUSTOMREQUEST=>'DELETE',
	CURLOPT_RETURNTRANSFER=>1,
	CURLOPT_HTTPHEADER=>array('Expect:'),
	CURLOPT_HTTPAUTH=>CURLAUTH_BASIC,
	CURLOPT_USERPWD=>$_SESSION['user'].':'.$_SESSION['pass']
));
$ret=curl_exec($c);
$info=curl_getinfo($c);
curl_close($c);
if($info['http_code']!=200) {
	echo("<div class=\"failure\">Update failed! Response was:</div><pre>"); print_r($info); echo("</pre>");
} else {
	echo('<div class="success">Node successfully updated!</div>');

	if($_GET['id']) {
		$sql='SELECT property,value FROM tags WHERE id="'.$db->escape_string($_GET['id']).'"';
		$res=$db->query($sql);
		$tags=array();
		while($row=$res->fetch_assoc())
			$tags[$row['property']]=$row['value'];
		foreach($tags as $t=>$v) {
			for($z=16;$z>=0;$z-=2) {
				$tile=tile_for_point($_GET['lat'],$_GET['lon'],$z);
				$db->query("UPDATE tiles$z SET count=count-1 WHERE tile=$tile AND property='".$db->escape_string($t)."'");
				if(in_array($t,$properties))
					$db->query("UPDATE tilesv$z SET count=count-1 WHERE tile=$tile AND property='".$db->escape_string($t)."' AND value='".$db->escape_string($v)."'");
			}
		}

		$db->query('DELETE FROM tag_nodes WHERE id='.$db->escape_string($_GET['id']));
		$db->query('DELETE FROM tags WHERE id='.$db->escape_string($_GET['id']));
	}
}
?>