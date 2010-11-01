/**
 * 
 * @param id Id of the node
 * @param lon longitude
 * @param lat latitude
 * @param tags A map (a set of key value pairs)
 * @return
 */
function oldrenderNode(id, lon, lat, tags) {
	
	var autoc='';
	
	var ret = "";
	
	ret += '<b>View '+(tags['name']||'')+"</b>";
	
	ret += "<br />";

        ret += "<a href='http://linkedgeodata.org/triplify/node" + id + ".ttl'><img class='noborder' src='icon/rdf-export2.png'> <br />";

	var depiction = tags['foaf:depiction'];
	if(depiction != null)
		ret += "<img style='float:left;' src = '" + depiction + "' />";
	
	var abstractEn = tags['dbpedia:property/abstract@en'];
	if(abstractEn != null)
		ret += abstractEn;
	
	ret += "<p style='clear:left;' >";
	
	ret +='<form class="nodeform"><input type="hidden" name="id" value="'+(id||'')+'" /><input type="hidden" name="lon" value="'+lon+'" /><input type="hidden" name="lat" value="'+lat+'" />\
		<table><tr><td>Name</td><td><input disabled="true" type="text" name="t[name]" value="'+(tags['name']||'')+'" /></td></tr>\
		<tr><td>Description</td><td><textarea disabled="true" name="t[description]">'+(tags['description']||'')+'</textarea></td></tr>\
		<tr><td>Image</td><td><input disabled="true" type="text" name="t[image]" value="'+(tags['image']||'')+'" /></td></tr>\
		<tr><td>Source_ref</td><td><input disabled="true" type="text" name="t[source_ref]" value="'+(tags['source_ref']||'')+'" /></td>';
	for(t in tags)
		if(t!='name' && t!='description' && t!='image' && t!='source_ref') {
			ret+='<tr><td>'+t+'</td><td><input disabled="true" class="autocomplete" type="text" name="t['+t+']" id="'+t+'" value="'+tags[t]+'" />';//<a onclick="$(this).parent().parent().remove()">[-]</a></td></tr>';
			//autoc+='$(\'#'+t+'\').autocomplete(\'autocomplete.php\',{extraParams:{p:\''+t+'\'}});'
		}
	//ret+='</tr></table>\
	//	<a onclick="appendrow($(this));">[+]</a><br />';
	if(loggedin)
		ret+=(id!=null?'<input type="button" onclick="if(confirm(\'Really delete?\')) $(this.form).load(\'delete.php?\'+$(form).serialize());" value="Delete" />&nbsp;':'')+'<input type="button" onclick="submitForm(this.form)" value="Save" />';
	ret+='</form>';
	
	ret += "</p>";
	
	ret+='<script type="text/javascript">'+autoc+'</script>';

	return ret;
}

function sparqlFailure()
{
	$("#dbpedia").html("Error loading data");
}

function namespaceUri(uri)
{
	var knownPrefixes = {
			'http://dbpedia.org/': 'dbpedia',
			'http://xmlns.com/foaf/0.1/' : 'foaf',
			'http://www.w3.org/2000/01/rdf-schema#' : 'rdfs'
		};

	for (var prefix in knownPrefixes) {
		var namespace = knownPrefixes[prefix];
		
		// FIXME replace with startsWith
		if(uri.substr(0, prefix.length)==prefix) {
			return namespace + ':' + uri.substr(prefix.length);
			break;
		}					
	}
	return null;
}

function extractTags(json)
{
	var result = {};
	for each(var item in json.results.bindings) {
		var key = item.p.value;
		
		// Check if the key is prefixed with a known namespace
		// In that case replace it
		var namespacedKey = namespaceUri(key);
		if(namespacedKey != null)
			key = namespacedKey;
		
		if(item.o['xml:lang'] != null)
			key = key += "@" + item.o['xml:lang'];
		
		result[key] = item.o.value;
	}
	
	return result;
}


function printKeys(item)
{
	var ret = "";
	for(var key in item)
		ret += key + " " + item[key] + "<br />";

	return ret;
}

function renderNode(id, lon, lat, tags)
{
	//var ret = renderNode(id, lon, lat, tags);
	var ret = "";
	
	

	/**
	 * Load DBpedia resources if the tags contain the key dbpedia-resource
	 */
	if(tags.dbpedia != null) {
		// Reserve a div for dbpedia data
		//ret += "<div id='dbpedia'>Fetching data from DBpedia <img src='loading.gif' /> </div>";

		var sparqler = new SPARQL.Service("dbpedia_sparql_proxy.php");
		sparqler.addDefaultGraph("http://dbpedia.org");

		var query = sparqler.createQuery();
		
		$("#dbpedia").html("Fetching data from DBpedia <img src='loading.gif' />"); 

		var queryString = "SELECT * {<" + tags.dbpedia + "> ?p ?o} Limit 10";

		query.query(queryString,
				{ failure: sparqlFailure, success: sparqlSuccess });

		ret += query.queryUrl();
	}
	else
		ret += "Resource = " + tags;  
	
	return ret;
}


function submitForm(form) {
	$(form).load('save.php?'+$(form).serialize());
}

function chRow(li) {
	$('.pautoc').parent().next().html('<input class="autocomplete" type="text" id="'+li+'" name="t['+li+']" value="" /><a onclick="$(this).parent().parent().remove()">[-]</a>');
	$('#'+li).autocomplete('autocomplete.php',{extraParams:{p:li}});
	$('.pautoc').replaceWith(li.toString());
}

function appendrow(t) {
	var row='<tr><td><input class="pautoc" type="text" id="" value="" /></td><td></td></tr>';
	t.prev().append(row);
	$('.pautoc').autocomplete('autocomplete.php',{extraParams:{p:'property'}}).result(function(event, item) {chRow(item); });
	for(var i=map.popups.length-1; i>=0; --i) map.popups[i].updateSize();
}

function addNode(event) {
	if(!event.ctrlKey)
		return;
	var lonlat = map.getLonLatFromViewPortPx(event.xy);
	var ll=map.getLonLatFromViewPortPx(event.xy).transform(map.projection,map.displayProjection);
//	alert(renderNode(null,ll.lon,ll.lat,new Array()));
	var m=addMarker(lonlat,renderNode(null,ll.lon,ll.lat,new Array()));
}


/**
 * Called on any map event.
 * Clears all markers from the map and reloads data.
 * Note that new markers are placed on the map by the instances.php script.
 * 
 * 
 */
var bottom,top,left,right,prop,val;
var instanceData = {};

function mapEvent(event) {
	
	
	var b=map.getExtent().transform(map.projection,map.displayProjection);
	if(bottom==b.bottom && top==b.top && left==b.left && right==b.right && prop==property && val==value)
		return;
	bottom=b.bottom; top=b.top; left=b.left; right=b.right; prop=property; val=value;
	layerMarkers.clearMarkers();
	for (var i = map.popups.length - 1; i >= 0; --i)
		map.removePopup(map.popups[i]);

	instanceData = {};
	
	/**
	 * This section creates a 'perma-link' to the current map location.
	 */
	var center = map.getCenter();
	center.transform(map.projection, map.displayProjection);
	var base = document.URL.split("?", 2)[0];
	var zoom = map.getZoom();
	var link =
			base + "?" + 
			"lat="  + center.lat          + "&" +
			"lon="  + center.lon          + "&" +
			"zoom=" + zoom                + "&" +
			"prop=" + encodeURI(property) + "&" +
			"val="  + encodeURI(value);
	//$("#map-link").html("<a href='" + link + "'>Perma-link</a>");

	$("#map-link").attr("href", link);
	
	//if(event) {
		//property="";
		//value="";
		$("#prop").html('<img src="loading.gif" />');
		$("#prop").load("properties.php?left="+b.left+"&bottom="+b.bottom+"&right="+b.right+"&top="+b.top+"&zoom="+zoom+(property!=""?"&property="+encodeURI(property):'')+(value!=""?"&value="+encodeURI(value):''));
	//}
	//$("#dbpedia").html("Loaded with: " + link);
		
	$("#inst").html('<img src="loading.gif" />');
	$("#inst").load( "instances.php?left="+b.left+"&bottom="+b.bottom+"&right="+b.right+"&top="+b.top+"&zoom="+zoom+(property!=""?"&property="+encodeURI(property):'')+(value!=""?"&value="+encodeURI(value):''));
	
}


function loadData(popup, nodeId, tags)
{
	//popup.setContentHTML(printKeys(tags));
	var content = oldrenderNode(nodeId, popup.lonlat.lon, popup.lonlat.lat, tags);
	
	/**
	 * Load DBpedia resources if the tags contain the key dbpedia-resource
	 */
	if(tags.dbpedia != null) {
		// Reserve a div for dbpedia data
		//ret += "<div id='dbpedia'>Fetching data from DBpedia <img src='loading.gif' /> </div>";

		var sparqler = new SPARQL.Service("dbpedia_sparql_proxy.php");
		sparqler.addDefaultGraph("http://dbpedia.org");

		var query = sparqler.createQuery();
		
		content = "Fetching data from DBpedia <img src='loading.gif' /> <br />" + content; 

		var queryString = "SELECT * {<" + tags.dbpedia + "> ?p ?o}";

		query.query(queryString,
				{ failure: sparqlFailure, success: function(json) {
					newTags = extractTags(json);
					for (key in newTags) { tags[key] = newTags[key]; }
					//var mergedTags = tags.merge(newTags);
					//mergedTags.loadedDBpedia = true;
					
					popup.setContentHTML(oldrenderNode(nodeId, popup.lonlat.lon, popup.lonlat.lat, tags));
					}	
				});
	}
	
	popup.setContentHTML(content);
}


/**
 * This function should be called once all instances are gathered.
 * The function call is generated by instances.php.
 * 
 * @return
 */
function updateExport()
{
	var ids = "";
	var first = true;
	var count = 0;
	for(var key in instanceData) {
		if(!first)
			ids += ",";

		ids += key;
		first = false;
		
		++count;
	}
	
	//if(count > 100)
//		$('#rdf-export').html("Export disabled - too many visible items");
//	else
	//$('#rdf-export').html("<a href='rdf-export.php?nodeIds=" + ids + "'>RDF-export</a>");
	
	$('#rdf-export').attr('href', 'rdf-export.php?nodeIds=' + ids);
}


function addMarker(ll, nodeId, tags) {
	var feature = new OpenLayers.Feature(layerMarkers, ll); 
	feature.closeBox = true;
	feature.popupClass = OpenLayers.Class(OpenLayers.Popup.FramedCloud,{'panMapIfOutOfView':false, 'autoSize': true});
	feature.data.popupContentHTML = "No content loaded yet";
	feature.data.overflow = "auto";

	
	instanceData[nodeId]         = {};
	instanceData[nodeId]['ll']   = ll;
	instanceData[nodeId]['tags'] = tags;
	
	var marker = feature.createMarker();

	var markerClick = function (evt) {
		
		for (var i = map.popups.length - 1; i >= 0; --i) {
			map.popups[i].hide();
		}
		if (this.popup == null) {
			this.popup = this.createPopup(this.closeBox);
			map.addPopup(this.popup);
			this.popup.show();
		} else {
			this.popup.toggle();
		}
		currentPopup = this.popup;
		
		loadData(currentPopup, nodeId, tags);
		
		OpenLayers.Event.stop(evt);
	};
	//marker.events.register("mouseover", feature, markerClick);
	//marker.events.register("mouseout", feature, markerClick);
	marker.events.register("click", feature, markerClick);

	layerMarkers.addMarker(marker);
	return marker;
}


function oldaddMarker(ll, popupContentHTML) {
	var feature = new OpenLayers.Feature(layerMarkers, ll); 
	feature.closeBox = true;
	feature.popupClass = OpenLayers.Class(OpenLayers.Popup.FramedCloud,{'panMapIfOutOfView':false, 'autoSize': true});
	feature.data.popupContentHTML = popupContentHTML;
	feature.data.overflow = "auto";

	var marker = feature.createMarker();

	var markerClick = function (evt) {
		$('#dbpedia').html("X" + $('#dbpedia').html());
		for (var i = map.popups.length - 1; i >= 0; --i) {
			map.popups[i].hide();
		}
		if (this.popup == null) {
			this.popup = this.createPopup(this.closeBox);
			map.addPopup(this.popup);
			this.popup.show();
		} else {
			this.popup.toggle();
		}
		currentPopup = this.popup;
		OpenLayers.Event.stop(evt);
	};
	marker.events.register("mouseover", feature, markerClick);
	//marker.events.register("mouseout", feature, markerClick);

	layerMarkers.addMarker(marker);
	return marker;
}
