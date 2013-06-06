<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#import "/imports/entities.ftl" as entities>
<#macro view>

<div class="entitylistings">
<#if it.hasOccurrences()>
<h3>Extracted entities</h3>
</#if>
<#-- First print the predefined types -->
<div class="entitylisting">
<#if it.personOccurrences?size != 0>
<h3>People</h3>
<@entities.listing entities=it.personOccurrences /> 
</#if>
</div>

<div class="entitylisting">
<#if it.organizationOccurrences?size != 0>
<h3>Organizations</h3>
<@entities.listing entities=it.organizationOccurrences /> 
</#if>
</div>

<div class="entitylisting">
<#if it.placeOccurrences?size != 0>
<h3>Places</h3>
<@entities.listing entities=it.placeOccurrences /> 
</#if>
</div>

<div class="entitylisting">
<#if it.conceptOccurrences?size != 0>
<h3>Concepts</h3>
<@entities.listing entities=it.conceptOccurrences /> 
</#if>
</div>

<#-- add Occurrences with other types -->
<#list it.otherOccurrencyTypes as type>
  <div class="entitylisting">
  <h3>${it.extractLabel(type)}</h3>
  <@entities.listing entities=it.getOccurrences(type) />
  </div>
</#list>

<#-- add Occurrences with no type -->
<div class="entitylisting">
<#if it.otherOccurrences?size != 0>
<h3>Others</h3>
<@entities.listing entities=it.otherOccurrences /> 
</#if>
</div>
<#-- add Lanugage Annotations -->
<div class="entitylisting">
<#if it.languageOccurrences?size != 0>
<h3>Language</h3>
<@entities.listing entities=it.languageOccurrences /> 
</#if>
</div>

</div>
<div style="clear: both"></div>

<script>
$("th").click(function () {
  $(this).parents("table").toggleClass("collapsed");
});
</script>

<#if it.placeOccurrences?size != 0>
<div class="mapContainer">
  <div id="map" class="olMap"
   style="border: 1px solid #ccc; height: 400px; width: 800px;"></div> 
<script>
$(document).ready(function() {
  var extent = new OpenLayers.Bounds(-180, -90, 180, 90);

  var map = new OpenLayers.Map('map',
    {controls: [
      new OpenLayers.Control.Navigation({documentDrag: true}),
      new OpenLayers.Control.PanZoom(),
      new OpenLayers.Control.ArgParser(),
      new OpenLayers.Control.Attribution()
     ],
     restrictedExtent: extent});

  var options = {
    numZoomLevels: 2,
  };
  var graphic = new OpenLayers.Layer.Image(
    'Default World Map',
    '${it.staticRootUrl}/home/images/world_map_1024_512.png',
    extent,
    new OpenLayers.Size(1024, 512),
    options
  );
  map.addLayer(graphic);

  map.setCenter(new OpenLayers.LonLat(0, 20), 0);
  map.addControl(new OpenLayers.Control.LayerSwitcher());
  map.zoomToMaxExtent();


  var markers = new OpenLayers.Layer.Markers("Markers");
  map.addLayer(markers);

  var entities = ${it.placesAsJSON};
  
  var id = 0;
  for (var entity_id in entities) {
    id += 1;
    var entity = entities[entity_id];
    var label = entity['http://www.w3.org/2000/01/rdf-schema#label'][0]['value'];
    var lat;
    var long;
    var georsspoint = entity['http://www.georss.org/georss/point'];
    if (georsspoint != undefined) {
      var parts = georsspoint[0]['value'].split(' ');
      lat = parseFloat(parts[0]);
      long = parseFloat(parts[1]);
    }
	var latitutes = entity['http://www.w3.org/2003/01/geo/wgs84_pos#lat'];
    var longitutes = entity['http://www.w3.org/2003/01/geo/wgs84_pos#long'];
    if (latitutes != undefined && longitutes != undefined) {
      lat = parseFloat(latitutes[0]['value']);
      long = parseFloat(longitutes[0]['value']);
    }
    
    if (lat == undefined || long == undefined) {
      continue;
    }
    
    (function(lat, long) {
      /* closure leak isolation for the popup instance scoped in the event handle */
      var position = new OpenLayers.LonLat(long, lat);
	  var iconSize = new OpenLayers.Size(32, 32);
      var offset = new OpenLayers.Pixel(-(iconSize.w/2), -iconSize.h);
      var markerIcon = new OpenLayers.Icon('${it.staticRootUrl}/home/images/pin_map_32.png', iconSize, offset);
	  var popupSize = new OpenLayers.Size(200, 20);
      var marker = new OpenLayers.Marker(position, markerIcon);
      var popup = new OpenLayers.Popup.Anchored("popup-" + id, position, popupSize, label, markerIcon, false);
      map.addPopup(popup);
      popup.hide();
      marker.events.register('mouseover', marker,
        function(evt) { popup.show(); OpenLayers.Event.stop(evt); });
      marker.events.register('mouseout', marker,
        function(evt) { popup.hide(); OpenLayers.Event.stop(evt); });
      markers.addMarker(marker);
    })(lat, long);
  }

});
</script>

</div>
</#if>

</#macro>
