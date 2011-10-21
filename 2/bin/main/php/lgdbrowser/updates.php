<b>Updates</b>
<script type="text/javascript">
var mk=new Array();
layerMarkers.clearMarkers();
</script>
<ol style="margin-left:-1em">
<?php
include('inc.php');
$t=microtime(1);

$box='latitude BETWEEN '.$db->escape_string(round($_GET['bottom']*10000000)).' AND '.$db->escape_string(round($_GET['top']*10000000)).
	' AND longitude BETWEEN '.$db->escape_string(round($_GET['left']*10000000)).' AND '.$db->escape_string(round($_GET['right']*10000000));

$b="tile = (CONV(BIN(FLOOR(0.5 + 65535*({$_GET['left']}+180)/360)), 4, 10)<<1)
	| CONV(BIN(FLOOR(0.5 + 65535*({$_GET['bottom']}+90)/180)), 4, 10)+4
	OR tile = (CONV(BIN(FLOOR(0.5 + 65535*({$_GET['right']}+180)/360)), 4, 10)<<1)
	| CONV(BIN(FLOOR(0.5 + 65535*({$_GET['top']}+90)/180)), 4, 10)";

#$box=sql_for_area($_GET['bottom'],$_GET['left'],$_GET['top'],$_GET['right']);


$sql='SELECT tn.id,longitude,latitude,tn.type,u.user,u.timestamp FROM elements tn '.
	($_GET['property']?'INNER JOIN tags t USING(type,id) INNER JOIN resources kr ON(k=kr.id AND kr.label="'.$db->escape_string($_GET['property']).'")'.
	($_GET['value']?' INNER JOIN resources vr ON(v=vr.id AND vr.label="'.$db->escape_string(utf8_decode($_GET['value'])).'")':''):'').
	'INNER JOIN updates u ON(tn.type=u.type AND tn.id=u.id) WHERE '.$box.' LIMIT 20';
#echo $sql;
$res=$db->query($sql);
print_r($db->error);
while($row=$res->fetch_assoc()) {
	unset($pr,$desc,$popdesc,$popform,$name,$ta);
	$desc=$row['timestamp'].' '.$row['user'];
	$p=$db->query('SELECT rk.label property, rv.label value FROM tags t INNER JOIN resources rk ON(k=rk.id) INNER JOIN resources rv ON(v=rv.id) WHERE t.type="'.$row['type'].'" AND t.id='.$row['id']);
	while($r=$p->fetch_assoc()) {
		$pr[$r['property']]=utf8_encode($r['value']);
		$ta.='"'.$r['property'].'":"'.htmlspecialchars(utf8_encode($r['value'])).'",';
	}
	if($pr['name'])
		$desc=$popdesc='<b>'.$pr['name'].'</b><br />';
	if($pr)
		foreach($pr as $p=>$v) if($p!='name') {
				if($p!='description' && $p!='image' && $p!='source_ref') {
					$autoc.='$(\'#'.$p.'\').autocomplete(\'autocomplete.php\',{extraParams:{p:\''.$p.'\'}});';
				}
				if(in_array($p,$properties))
					$desc.=$p.': '.$v.'<br />';
			}

	echo('<li id="p'.(++$id).'" onmouseout="mk['.$id.'].events.triggerEvent(\'mouseout\'); $(this).removeClass(\'highlight\');" onmouseover="mk['.$id.'].events.triggerEvent(\'mouseover\'); $(this).addClass(\'highlight\');">'.$desc.'</li>');
	echo('<script type="text/javascript">
mk['.$id.']=addMarker(new OpenLayers.LonLat('.($row['longitude']/10000000).','.($row['latitude']/10000000).').transform(map.displayProjection,map.projection),renderNode(\''.$row['id'].'\',\''.($row['longitude']/10000000).'\',\''.($row['latitude']/10000000).'\',{'.$ta.'},\''.$row['type'].'\'));
</script>');
}
?>
</ol>
<? microtime(1)-$t?>