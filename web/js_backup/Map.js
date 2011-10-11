/**
 * Class: Godiva2.Map
 * Wraps an OpenLayers map, providing higher-level functions for controlling
 * changes to projection, base map, adding new overlays etc.
 */
 
 Godiva2.Map = OpenLayers.Class({
	
    /**
     * Property: map 
     * {<OpenLayers.Map>} The OpenLayers map being wrapped
     */
    map: null,

    /** 
     * Property: div in which the map will be displayed
     * {DOMElement} 
     */
    div: null,

    projectionCode: null,
    bbox: null,
    popups: null,
    editingToolbar: null,
    layerSwitcher: null,
    featureInfoUrl: null,
    layersLoading: null,
    drawingLayer: null,
	
    /**
     * Constructor: Godiva2.Map
     * Create a Godiva2 map.  For example:
     * 
     * > var map = new Godiva2.Map({div: myDiv});
	 
     * Parameters:
     * div - id of the div into which the map is to be placed
     */
    initialize: function (div) {
		// (There is some stuff in OpenLayers classes about
		// creating a displayClassName.  Perhaps this should be
		// reinstated, but I don't really know what it does.)

		this.map = new OpenLayers.Map(div);

                this.popups = [];
                this.layersLoading = 0;

                // Stop the pink tiles appearing on error
                OpenLayers.Util.onImageLoadError = function() {
                    this.style.display = "";
                    this.src="./images/blank.png";
                }

                // Create a layer on which users can draw transects (i.e. lines on the map)
                this.drawinglayer = new OpenLayers.Layer.Vector( "Drawing" );
                this.drawinglayer.displayInLayerSwitcher = false;
                
                // Set up a control for drawing on the map
                // We use CSS to hide the controls we're not using
                this.editingToolbar = new OpenLayers.Control.EditingToolbar(this.drawinglayer);

                // Set up the history navigator
                var nav = new OpenLayers.Control.NavigationHistory();
                var navHistoryPanel = new OpenLayers.Control.Panel();
                navHistoryPanel.addControls([nav.next, nav.previous]);

		this.map.addControl(nav);
                this.map.addControl(navHistoryPanel);
                this.map.addControl(this.editingToolbar);
                this.editingToolbar.div.style.visibility = 'hidden';

                this.map.fractionalZoom = true;

                var ol_wms = new OpenLayers.Layer.WMS1_1_1( "OpenLayers WMS",
                    "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'basic'});
                //var bluemarble_wms = new OpenLayers.Layer.WMS1_1_1( "Blue Marble",
                //  "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite'});
                var bluemarble_demis_wms = new OpenLayers.Layer.WMS1_1_1( "Demis Blue Marble",
                    "http://www2.demis.nl/wms/wms.ashx?WMS=BlueMarble" , {layers: 'Earth Image'});
                var osm_wms = new OpenLayers.Layer.WMS1_1_1( "Openstreetmap",
                    "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'osm-map'});
                var human_wms = new OpenLayers.Layer.WMS1_1_1( "Human Footprint",
                    "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'hfoot'});
                var demis_wms = new OpenLayers.Layer.WMS1_1_1( "Demis WMS",
                    "http://www2.demis.nl/wms/wms.ashx?WMS=WorldMap",
                    {layers:'Countries,Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports',
                    format: 'image/png'});
                var plurel = new OpenLayers.Layer.WMS("PLUREL WMS",
                    "http://plurel.jrc.ec.europa.eu/ArcGIS/services/worldwithEGM/mapserver/wmsserver?", {layers: '0,2,3,4,5,8,9,10,40'});
                var weather = new OpenLayers.Layer.WMS("Latest Clouds",
                    "http://maps.customweather.com/image?client=ucl_test&client_password=t3mp", {layers: 'base,global_ir_satellite_10km,radar_precip_mode'}, {wrapDateLine: true});
                var srtm_dem = new OpenLayers.Layer.WMS( "SRTM DEM",
                    "http://iceds.ge.ucl.ac.uk/cgi-bin/icedswms?", {layers: 'bluemarble,srtm30'}, {wrapDateLine: true});
                var marine_geo = new OpenLayers.Layer.WMS( 'Marine Geo', 'http://emii3.its.utas.edu.au/cgi-bin/tilecache_proxied.cgi?', {styles: '', layers: 'marine_geo', format: 'image/png', transparent: 'FALSE', version: '1.1.0'}, {isBaseLayer: true, opacity: 1.0, queryable: false, buffer: 1, gutter: 0, wrapDateLine: true} );

                // Now for the polar stereographic layers, one for each pole. We do this
                // as an Untiled layer because, for some reason, if we use a tiled layer
                // this results in lots of spurious tiles being requested when switching
                // from a lat-lon base layer to polar stereographic.
                // The full extent of a polar stereographic projection is (-10700000, -10700000,
                // 14700000, 14700000) but we don't use all of this range because we're only
                // really interested in the stuff near the poles. Therefore we set maxExtent
                // so the user only sees a quarter of this range and maxResolution so that
                // we can't zoom out too far.
                var polarMaxExtent = new OpenLayers.Bounds(-10700000, -10700000, 14700000, 14700000);
                var halfSideLength = (polarMaxExtent.top - polarMaxExtent.bottom) / (4 * 2);
                var centre = ((polarMaxExtent.top - polarMaxExtent.bottom) / 2) + polarMaxExtent.bottom;
                var low = centre - halfSideLength;
                var high = centre + halfSideLength;
                var polarMaxResolution = (high - low) / 256;
                var windowLow = centre - 2 * halfSideLength;
                var windowHigh = centre + 2 * halfSideLength;
                var polarWindow = new OpenLayers.Bounds(windowLow, windowLow, windowHigh, windowHigh);
                var northPoleBaseLayer = new OpenLayers.Layer.WMS1_1_1(
                    "North polar stereographic",
                    "http://wms-basemaps.appspot.com/wms", {
                        layers: 'bluemarble_file',
                        format: 'image/jpeg'
                    },{
                        wrapDateLine: false,
                        transitionEffect: 'resize',
                        projection: 'EPSG:32661',
                        maxExtent: polarWindow,
                        maxResolution: polarMaxResolution
                    }
                );
                var southPoleBaseLayer = new OpenLayers.Layer.WMS1_1_1(
                    "South polar stereographic",
                    "http://wms-basemaps.appspot.com/wms", {
                        layers: 'bluemarble_file',
                        format: 'image/jpeg'
                    },{
                        wrapDateLine: false,
                        transitionEffect: 'resize',
                        projection: 'EPSG:32761',
                        maxExtent: polarWindow,
                        maxResolution: polarMaxResolution
                    }
                );
                this.map.addLayers([bluemarble_demis_wms, demis_wms, ol_wms, osm_wms, human_wms, plurel, weather, srtm_dem, marine_geo, northPoleBaseLayer, southPoleBaseLayer, this.drawinglayer]);
                this.map.setBaseLayer(bluemarble_demis_wms);

                this.projectionCode = this.map.baseLayer.projection.getCode();

                // HANDLE MAP EVENTS:
                // Set up the throbber (acts as progress indicator)
                // Adapted from LoadingPanel.js
                this.map.events.register('preaddlayer', this.map, this.loadingPleaseWait.bind(this));
                // Make sure the Google Earth and Permalink links are kept up to date when
                // the map is moved or zoomed
                this.map.events.register('moveend', this.map, portalWindow.setGEarthURL.bind(portalWindow));
                this.map.events.register('moveend', this.map, portalWindow.setPermalinkURL.bind(portalWindow));
                // Register an event for when the base layer of the map is changed
                this.map.events.register('changebaselayer', this.map, this.baseLayerChanged.bind(this));
                // Add a listener for GetFeatureInfo
                this.map.events.register('click', this.map, this.getFeatureInfo.bind(this));
                this.drawinglayer.events.register('featureadded', this.drawinglayer, this.transectEventHandler.bind(this));

                // If we have loaded Google Maps and the browser is compatible, add it as a base layer
                if (typeof GBrowserIsCompatible == 'function' && GBrowserIsCompatible()) {
                    var gmapLayer = new OpenLayers.Layer.Google("Google Maps (satellite)", {type: G_SATELLITE_MAP});
                    var gmapLayer2 = new OpenLayers.Layer.Google("Google Maps (political)", {type: G_NORMAL_MAP});
                    this.map.addLayers([gmapLayer, gmapLayer2]);
                }

                this.layerSwitcher = new OpenLayers.Control.LayerSwitcher()
                this.map.addControl(this.layerSwitcher);

                //map.addControl(new OpenLayers.Control.MousePosition({prefix: 'Lon: ', separator: ' Lat:'}));
                this.map.zoomTo(1);
    },
	
    /**
     * Method: destroy
     * The destroy method is used to perform any clean up before the component
     * is dereferenced.  Typically this is where event listeners are removed
     * to prevent memory leaks.
     */
    destroy: function () {
        if (this.map) {
            this.map = null;
        }
    },

    loadingPleaseWait: function (evt) {
        if (evt.layer) {
            evt.layer.events.register('loadstart', this, function() {
                this.layersLoading++;
                if (this.layersLoading > 0) {
                    $('throbber').style.visibility = 'visible';
                }
            });
            evt.layer.events.register('loadend', this, function() {
                if (this.layersLoading > 0) {
                    this.layersLoading--;
                }
                if (this.layersLoading == 0) {
                    $('throbber').style.visibility = 'hidden';
                }
            });
        }
    },

    transectEventHandler: function (event) {
        // Destroy previously-added line string
        if (this.drawinglayer.features.length > 1) {
            this.drawinglayer.destroyFeatures(this.drawinglayer.features[0]);
        }
        // Get the linestring specification
        var line = event.feature.geometry.toString();
        // we strip off the "LINESTRING(" and the trailing ")"
        line = line.substring(11, line.length - 1);
        // Load an image of the transect
        var server = layer.details.server == '' ? 'wms' : layer.details.server;
        var transectUrl = server + '?REQUEST=GetTransect' +
            '&LAYER=' + layer.details.id +
            '&CRS=' + this.map.baseLayer.projection.toString() +
            '&ELEVATION=' + depth.getZValue() +
            '&TIME=' + time.isoTValue +
            '&LINESTRING=' + line +
            '&FORMAT=image/png';
        portalWindow.popUp(transectUrl, 450, 350);
    },

    prepare: function () {
        // Set the auto-zoom box
        this.bbox = layer.details.bbox;
        $('autoZoom').innerHTML = '<a href="#">Fit layer to window</a>';

        // Automatically zoom to the max extent of the layer if selected
        if ($('zoomOnSelect').checked) {
            this.zoomToLayerBBOX();
        }

        // Make the editing toolbar visible
        this.editingToolbar.div.style.visibility = 'visible';
    },

    zoomToLayerBBOX: function () {
        this.map.zoomToExtent(new OpenLayers.Bounds(this.bbox[0], this.bbox[1], this.bbox[2], this.bbox[3]));
    },

    /**
     * Method: getExtent
     * Gets the current map extent, checking for out-of-range values
     */
    getExtent: function () {
        var bounds = this.map.getExtent();
        var maxBounds = this.map.maxExtent;
        var top = Math.min(bounds.top, maxBounds.top);
        var bottom = Math.max(bounds.bottom, maxBounds.bottom);
        var left = Math.max(bounds.left, maxBounds.left);
        var right = Math.min(bounds.right, maxBounds.right);
        return new OpenLayers.Bounds(left, bottom, right, top);
    },

    /**
     * Method: getIntersectionBBOX
     * Returns a bounding box as a string in format "minlon,minlat,maxlon,maxlat"
     * that represents the intersection of the currently-visible map layer's
     * bounding box and the viewport's bounding box.
     */
    getIntersectionBBOX: function () {
        if (this.map.baseLayer.projection.getCode() == 'EPSG:4326') {
            // We compute the intersection of the bounding box and the currently-
            // visible map extent
            var mapBboxEls = this.map.getExtent().toArray();
            // bbox is the bounding box of the currently-visible layer
            var newBBOX = Math.max(mapBboxEls[0], this.bbox[0]) + ',';
            newBBOX += Math.max(mapBboxEls[1], this.bbox[1]) + ',';
            newBBOX += Math.min(mapBboxEls[2], this.bbox[2]) + ',';
            newBBOX += Math.min(mapBboxEls[3], this.bbox[3]);
            return newBBOX;
        } else {
            return this.map.getExtent().toBBOX();
        }
    },

    /**
     * Method: baseLayerChanged
     * Called when the user changes the base layer
     */
    baseLayerChanged: function (event) {
        this.clearPopups();
        // Change the parameters of the map based on the new base layer
        this.map.setOptions({
           //projection: projCode,
           maxExtent: this.map.baseLayer.maxExtent,
           maxResolution: this.map.baseLayer.maxResolution
        });
        if (this.projectionCode != this.map.baseLayer.projection.getCode()) {
            // We've changed the projection of the base layer
            this.projectionCode = this.map.baseLayer.projection.getCode();
            this.map.zoomToMaxExtent();
        }
        if (layer.overlay != null) {
            layer.tiledOverlay.maxExtent = this.map.baseLayer.maxExtent;
            layer.tiledOverlay.maxResolution = this.map.baseLayer.maxResolution;
            layer.tiledOverlay.minResolution = this.map.baseLayer.minResolution;
            layer.tiledOverlay.resolutions = this.map.baseLayer.resolutions;
            // We only wrap the datelinein EPSG:4326
            layer.tiledOverlay.wrapDateLine = this.map.baseLayer.projection == 'EPSG:4326';
            layer.untiledOverlay.maxExtent = this.map.baseLayer.maxExtent;
            layer.untiledOverlay.maxResolution = this.map.baseLayer.maxResolution;
            layer.untiledOverlay.minResolution = this.map.baseLayer.minResolution;
            layer.untiledOverlay.resolutions = this.map.baseLayer.resolutions;
            layer.untiledOverlay.wrapDateLine = this.map.baseLayer.projection == 'EPSG:4326';
            this.update();
        }
    },

    /**
     * Method: clearPopups
     * Clear the popups
     */
    clearPopups: function () {
        for (var i = 0; i < this.popups.length; i++) {
            this.map.removePopup(this.popups[i]);
        }
        this.popups.clear();
    },

    /**
     * Method: getFeatureInfo
     * Event handler for when a user clicks on a map
     */
    getFeatureInfo: function (e) {
        var lonLat = this.map.getLonLatFromPixel(e.xy);
        // Check we haven't clicked off-map
        // Could also check the bbox of the layer but this would only work in lat-lon
        // projection...
        if (layer.overlay != null && layer.overlay.maxExtent.containsLonLat(lonLat))
        {
            // Immediately load popup saying "loading"
            var tempPopup = new OpenLayers.Popup (
                "temp", // TODO: does this need to be unique?
                lonLat,
                new OpenLayers.Size(100, 50),
                "Loading...",
                true, // Means "add a close box"
                null  // Do nothing when popup is closed.
            );
            tempPopup.autoSize = true;
            this.map.addPopup(tempPopup);

            var params = {
                REQUEST: "GetFeatureInfo",
                BBOX: this.map.getExtent().toBBOX(),
                I: e.xy.x,
                J: e.xy.y,
                INFO_FORMAT: 'text/xml',
                QUERY_LAYERS: layer.overlay.params.LAYERS,
                WIDTH: this.map.size.w,
                HEIGHT: this.map.size.h
            };
            if (layer.details.server != '') {
                // This is the signal to the server to load the data from elsewhere
                params.url = layer.details.server;
            }
            this.featureInfoUrl = layer.overlay.getFullRequestString(
                params,
                'wms' // We must always load from the home server
            );
            // Now make the call to GetFeatureInfo
            OpenLayers.loadURL(this.featureInfoUrl, '', this, function(response) {
                var xmldoc = response.responseXML;
                var lon = parseFloat(portalWindow.getElementValue(xmldoc, 'longitude'));
                var lat = parseFloat(portalWindow.getElementValue(xmldoc, 'latitude'));
                var iIndex = parseInt(portalWindow.getElementValue(xmldoc, 'iIndex'));
                var jIndex = parseInt(portalWindow.getElementValue(xmldoc, 'jIndex'));
                var gridCentreLon = parseFloat(portalWindow.getElementValue(xmldoc, 'gridCentreLon'));
                var gridCentreLat = parseFloat(portalWindow.getElementValue(xmldoc, 'gridCentreLat'));
                var val = parseFloat(portalWindow.getElementValue(xmldoc, 'value'));
                var html = "";
                if (lon) {
                    // We have a successful result
                    var truncVal = val.toPrecision(4);
                    html = "<b>Lon:</b> " + lon.toFixed(6) + "<br /><b>Lat:</b> " +
                        lat.toFixed(6) + "<br /><b>Value:</b><span id='colourValue'> " + truncVal + "</span><br />"
                    if (iIndex && autoLoad.debugMode) {
                        // Add extra information about the grid
                        html += "<i>(Grid indices: i=" + iIndex + ", j=" + jIndex + ")</i><br />";
                        html += "<i>(Grid centre: lon=" + gridCentreLon.toFixed(6) + ", lat="
                            + gridCentreLat.toFixed(6) + ")</i><br />";
                    }
                    if (!isNaN(truncVal)) {
                        // Add links to alter colour scale min/max
                        html += "<a href='#' onclick=setColourScaleExtreme('min'," + val + ") " +
                            "title='Sets the minimum of the colour scale to " + truncVal + "'>" +
                            "Set colour min</a><br />";
                        html += "<a href='#' onclick=setColourScaleExtreme('max'," + val + ") " +
                            "title='Sets the maximum of the colour scale to " + truncVal + "'>" +
                            "Set colour max</a>";  
                    }
                    if (time.timeSeriesSelected()) {
                        // Construct a GetFeatureInfo request for the timeseries plot
                        var serverAndParams = this.featureInfoUrl.split('?');
                        var server = layer.details.server == '' ? 'wms' : layer.details.server;
                        var urlEls = serverAndParams[1].split('&');
                        // Replace the parameters as needed: we need to add the
                        // time range and change the format to PNG
                        var newURL = server + '?';
                        for (var i = 0; i < urlEls.length; i++) {
                            if (urlEls[i].startsWith('TIME=')) {
                                newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
                            } else if (urlEls[i].startsWith('INFO_FORMAT')) {
                                newURL += '&INFO_FORMAT=image/png';
                            } else {
                                newURL += '&' + urlEls[i];
                            }
                        }
                        // Image will be 400x300, need to allow a little elbow room
                        html += "<br /><a href='#' onclick=popUp('"
                            + newURL + "',450,350) title='Creates a plot of the value"
                            + " at this point over the selected time range'>Create timeseries plot</a>";
                    }
                } else {
                    html = "Can't get feature info data<br /> for this layer <a href=\"javascript:popUp('whynot.html', 200, 200)\">(why not?)</a>";
                }
                // Remove the "Loading..." popup
                this.map.removePopup(tempPopup);
                // Show the result in a popup
                var popup = new OpenLayers.Popup (
                    "id", // TODO: does this need to be unique?
                    lonLat,
                    new OpenLayers.Size(100, 50),
                    html,
                    true, // Means "add a close box"
                    null  // Do nothing when popup is closed.
                );
                popup.autoSize = true;
                this.popups.push(popup);
                this.map.addPopup(popup);
            });
            Event.stop(e);
        }
    },

    setOverlayVisibility: function (theOverlay, visible) {
        if (theOverlay != null) {
            theOverlay.setVisibility(visible);
            theOverlay.displayInLayerSwitcher = visible;
        }
    },

    /**
     * Method: setVisibleOverlay
     * Decides whether to display the animation, or the tiled or untiled
     * version of the ncwms layer overlay
     */
    setVisibleOverlay: function (isAnimation) {
        // TODO: repeats code above
        var style = typeof layer.details.supportedStyles == 'undefined' ? 'boxfill' : layer.details.supportedStyles[0];
        if (isAnimation) {
            this.setOverlayVisibility(layer.animationOverlay, true);
            this.setOverlayVisibility(layer.tiledOverlay, false);
            this.setOverlayVisibility(layer.untiledOverlay, false);
        } else if (style.toLowerCase() == 'vector') {
            this.setOverlayVisibility(layer.animationOverlay, false);
            this.setOverlayVisibility(layer.tiledOverlay, false);
            this.setOverlayVisibility(layer.untiledOverlay, true);
            layer.overlay = layer.untiledOverlay;
        } else {
            this.setOverlayVisibility(layer.animationOverlay, false);
            this.setOverlayVisibility(layer.tiledOverlay, true);
            this.setOverlayVisibility(layer.untiledOverlay, false);
            layer.overlay = layer.tiledOverlay;
        }
        this.layerSwitcher.layerStates = []; // forces redraw
        this.layerSwitcher.redraw();
    },

    update: function() {
        // Hide the z values selector if it contains no values.  We do this here
        // because it seems that the calendar.show() method can change the visibility
        // unexpectedly
        $('zValues').style.visibility = $('zValues').options.length == 0 ? 'hidden' : 'visible';

        var logscale = $('scaleSpacing').value == 'logarithmic';

        // Update the intermediate scale markers
        var min = logscale ? Math.log(parseFloat(style.scaleMinVal)) : parseFloat(style.scaleMinVal);
        var max = logscale ? Math.log(parseFloat(style.scaleMaxVal)) : parseFloat(style.scaleMaxVal);
        var third = (max - min) / 3;
        var scaleOneThird = logscale ? Math.exp(min + third) : min + third;
        var scaleTwoThirds = logscale ? Math.exp(min + 2 * third) : min + 2 * third;
        $('scaleOneThird').innerHTML = scaleOneThird.toPrecision(4);
        $('scaleTwoThirds').innerHTML = scaleTwoThirds.toPrecision(4);

        if ($('tValues')) {
            time.isoTValue = $('tValues').value;
        }

        // Set the map bounds automatically
        if (autoLoad.bbox != null) {
            var bboxEls = autoLoad.bbox.split(",");
            var bounds = new OpenLayers.Bounds(parseFloat(bboxEls[0]), parseFloat(bboxEls[1]),
                parseFloat(bboxEls[2]), parseFloat(bboxEls[3]));
            this.map.zoomToExtent(bounds);
        }

        // Make sure the autoLoad object is cleared
        autoLoad.reset();

        // Get the default style for this layer.  There is some defensive programming here to
        // take old servers into account that don't advertise the supported styles
        var styleString = typeof layer.details.supportedStyles == 'undefined' ?
            'boxfill' : layer.details.supportedStyles[0];
        if (style.paletteName != null) {
            styleString += '/' + style.paletteName;
        }

        // Notify the OpenLayers widget
        // TODO get the map projection from the base layer
        // TODO use a more informative title
        var params = {
            layers: layer.details.id,
            elevation: depth.getZValue(),
            time: time.isoTValue,
            transparent: 'true',
            styles: styleString,
            crs: this.map.baseLayer.projection,
            colorscalerange: style.scaleMinVal + ',' + style.scaleMaxVal,
            numcolorbands: $('numColorBands').value,
            logscale: logscale
        };

        if (layer.overlay == null) {
            // Buffer is set to 1 to avoid loading a large halo of tiles outside the
            // current viewport
            layer.tiledOverlay = new OpenLayers.Layer.WMS1_3("ncWMS",
                layer.details.server == '' ? 'wms' : layer.details.server,
                params,
                {buffer: 1, wrapDateLine: this.map.baseLayer.projection == 'EPSG:4326'}
            );
            layer.untiledOverlay = new OpenLayers.Layer.WMS1_3("ncWMS",
                layer.details.server == '' ? 'wms' : layer.details.server,
                params,
                {buffer: 1, ratio: 1.5, singleTile: true, wrapDateLine: this.map.baseLayer.projection == 'EPSG:4326'}
            );
            this.setVisibleOverlay(false);
            this.map.addLayers([layer.tiledOverlay, layer.untiledOverlay]);
            // Create a layer for coastlines
            // TOOD: only works at low res (zoomed out)
            //var coastline_wms = new OpenLayers.Layer.WMS( "Coastlines",
            //    "http://labs.metacarta.com/wms/vmap0?", {layers: 'coastline_01', transparent: 'true' } );
            //map.addLayers([layer.overlay, coastline_wms]);
            //map.addLayers([layer.tiledOverlay, layer.untiledOverlay]);
        } else {
            this.setVisibleOverlay(false);
            layer.overlay.url = layer.details.server == '' ? 'wms' : layer.details.server;
            layer.overlay.mergeNewParams(params);
        }

        var imageURL = layer.overlay.getURL(new OpenLayers.Bounds(this.bbox[0], this.bbox[1], this.bbox[2], this.bbox[3]));
        $('testImage').innerHTML = '<a target="_blank" href="' + imageURL + '">test image</a>';
        $('screenshot').style.visibility = 'visible'; // TODO: enable this when working properly
        portalWindow.setGEarthURL();
        portalWindow.setPermalinkURL();
    },

    CLASS_NAME: "Godiva2.Map"
 });