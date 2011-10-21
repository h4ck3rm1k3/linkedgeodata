<?php
session_start();
include('inc.php');

$sql='SELECT property,value FROM tags WHERE id="'.$db->escape_string($_GET['id']).'"';
$res=$db->query($sql);
$tags=array();
while($row=$res->fetch_assoc())
	$tags[$row['property']]=$row['value'];
#print_r($_GET);

$xml="<?xml version='1.0' encoding='UTF-8'?>
<osm version='0.5' generator='linkedgeodata.org'>
<node id='".$_GET['id']."' lat='".$_GET['lat']."' lon='".$_GET['lon']."'>";
foreach($_GET['t'] as $t=>$v)
	$xml.="<tag k='".$t."' v='".str_replace('\'','&#39;',$v)."' />\n";
$xml.="</node></osm>";

$file='/tmp/'.uniqid().'.osm';
file_put_contents($file,$xml);
$c=curl_init('http://api.openstreetmap.org/api/0.5/node/'.($_GET['id']?$_GET['id']:'create')); 
$fp=fopen($file,'r');
curl_setopt_array($c,array(
	CURLOPT_PUT=>1,
	CURLOPT_RETURNTRANSFER=>1,
	CURLOPT_INFILE=>$fp,
	CURLOPT_INFILESIZE=>filesize($file),
	CURLOPT_HTTPHEADER=>array('Expect:'),
	CURLOPT_HTTPAUTH=>CURLAUTH_BASIC,
	CURLOPT_USERPWD=>$_SESSION['user'].':'.$_SESSION['pass']
));
$ret=curl_exec($c);
$info=curl_getinfo($c);
curl_close($c);
unlink($file);
if($info['http_code']!=200) {
	echo("<div class=\"failure\">Update failed! Response was:</div><pre>".$ret); print_r($info); echo("</pre>");
#echo ('<pre>'.htmlspecialchars($xml).'</pre>');
} else {
	echo('<div class="success">Node successfully <a href="http://openstreetmap.org/browse/node/'.($_GET['id']?$_GET['id']:$ret).'" target="_blank">updated</a>!</div>');

	if(!$_GET['id'] && $ret) { // new node
		$db->query('INSERT INTO tag_nodes VALUES ('.$db->escape_string($ret).','.round($_GET['lat']*10000000).','.round($_GET['lon']*10000000).','.tile_for_point($_GET['lat'],$_GET['lon']).')');
		$_GET['id']=$ret;
	}

	if($_GET['id']) { // update tags
		foreach(array_diff_assoc($_GET['t'],$tags) as $t=>$v) if($v) {
			$db->query('INSERT INTO tags VALUES ('.$db->escape_string($_GET['id']).',"'.$db->escape_string($t).'","'.$db->escape_string($v).'")');
			for($z=16;$z>=0;$z-=2) {
				$tile=tile_for_point($_GET['lat'],$_GET['lon'],$z);
				$sql="UPDATE tiles$z SET count=count+1 WHERE tile=$tile AND property='".$db->escape_string($t)."'";
				$res=$db->query($sql);
				if(!$db->affected_rows)
					$res=$db->query("INSERT tiles$z VALUES ($tile,'".$db->escape_string($t)."',1)");
				$res=$db->query("UPDATE tilesv$z SET count=count+1 WHERE tile=$tile AND property='".$db->escape_string($t)."' AND value='".$db->escape_string($v)."'");
				if(!$db->affected_rows)
					$res=$db->query("INSERT tilesv$z VALUES ($tile,'".$db->escape_string($t)."','".$db->escape_string($v)."',1)");
			}
		}
		foreach(array_diff_assoc($tags,$_GET['t']) as $t=>$v) {
			$db->query('DELETE FROM tags WHERE id='.$db->escape_string($_GET['id']).' AND property="'.$db->escape_string($t).'" AND value="'.$db->escape_string($v).'"');
			for($z=16;$z>=0;$z-=2) {
				$tile=tile_for_point($_GET['lat'],$_GET['lon'],$z);
				$db->query("UPDATE tiles$z SET count=count-1 WHERE tile=$tile AND property='".$db->escape_string($t)."'");
#echo $z.'-'.$tile."-".$db->affected_rows.$db->error.":";
				if(in_array($t,$properties))
					$db->query("UPDATE tilesv$z SET count=count-1 WHERE tile=$tile AND property='".$db->escape_string($t)."' AND value='".$db->escape_string($v)."'");
if($db->affected_rows!=1)
	echo $z.'-'.$tile."-".$db->affected_rows.$db->error.":";
			}
		}
		echo $db->error;
	}
}
?>