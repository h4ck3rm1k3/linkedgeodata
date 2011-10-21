function jumpTo(lon, lat, zoom) {
    var x = Lon2Merc(lon);
    var y = Lat2Merc(lat);
    map.setCenter(new OpenLayers.LonLat(x, y), zoom);
    return false;
}
 
function Lon2Merc(lon) {
    return 20037508.34 * lon / 180;
}
function Merc2Lon(lon) {
    return lon / 20037508.34 * 180;
}
 
function Lat2Merc(lat) {
    lat = Math.log(Math.tan( (90 + lat) * Math.PI / 360)) / (Math.PI / 180);
    return 20037508.34 * lat / 180;
}
function Merc2Lat(lat) {
	var rad = lat * 180.0/(Math.PI);
	var sinrad = Math.sin(rad);

	return (6378137.0 * Math.log(Math.tan((rad + Math.PI/2) / 2) * Math.pow(((1 - 0.0818191913108718138 * sinrad) / (1 + 0.0818191913108718138 * sinrad)), (0.0818191913108718138/2))));
}
 
function addMarker(layer, lon, lat, popupContentHTML) {
 
    var ll = new OpenLayers.LonLat(Lon2Merc(lon), Lat2Merc(lat));
    var feature = new OpenLayers.Feature(layer, ll); 
    feature.closeBox = true;
    feature.popupClass = OpenLayers.Class(OpenLayers.Popup.FramedCloud, {minSize: new OpenLayers.Size(300, 180) } );
    feature.data.popupContentHTML = popupContentHTML;
    feature.data.overflow = "hidden";
 
    var marker = new OpenLayers.Marker(ll);
    marker.feature = feature;
 
    var markerClick = function(evt) {
        if (this.popup == null) {
            this.popup = this.createPopup(this.closeBox);
            map.addPopup(this.popup);
            this.popup.show();
        } else {
            this.popup.toggle();
        }
        OpenLayers.Event.stop(evt);
    };
    marker.events.register("mousedown", feature, markerClick);
 
    layer.addMarker(marker);
    map.addPopup(feature.createPopup(feature.closeBox));
}
 
function getCycleTileURL(bounds) {
   var res = this.map.getResolution();
   var x = Math.round((bounds.left - this.maxExtent.left) / (res * this.tileSize.w));
   var y = Math.round((this.maxExtent.top - bounds.top) / (res * this.tileSize.h));
   var z = this.map.getZoom();
   var limit = Math.pow(2, z);
 
   if (y < 0 || y >= limit)
   {
     return null;
   }
   else
   {
     x = ((x % limit) + limit) % limit;
 
     return this.url + z + "/" + x + "/" + y + "." + this.type;
   }
}