/**
 * Class: Godiva2.Screenshot
 *
 */

 Godiva2.Screenshot = OpenLayers.Class({

    /**
     * Property: screenshotPanel
     * panel to display a screenshot
     */
    screenshotPanel: null,

    /**
     * Constructor: Godiva2.Screenshot
     * Create a Godiva2 Screenshot.  For example:
     *
     * > var screenshot = new Godiva2.Screenshot();

     * Parameters:
     * blah - description of blah
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
        
        // Set up the "Screenshot loading" panel
        this.screenshotPanel = new YAHOO.widget.Panel("screenshotPanel", {
            height:"600px",
            width:"700px",
            constraintoviewport: true,
            fixedcenter: true,
            underlay:"shadow",
            close:true,
            visible:false,
            draggable:true,
            modal:true
        });
    },

    /**
     * Method: destroy
     * The destroy method is used to perform any clean up before the component
     * is dereferenced.  Typically this is where event listeners are removed
     * to prevent memory leaks.
     */
    destroy: function () {
        if (this.calendar) {
            this.calendar = null;
        }
    },

    // Loads a screenshot
    loadScreenshot: function () {
        if (layer.overlay == null) {
            alert('Data not yet loaded');
        } else {
            $('screenshotMessage').innerHTML = "Screenshot loading, please wait...";
            $('screenshotImage').src = "images/ajax-loader.gif";
            this.screenshotPanel.render(document.body);
            this.screenshotPanel.show();
            // See code in server.js
            var bounds = map.map.getExtent();
            var params = {
                urlBG: map.map.baseLayer.getURL(bounds),
                urlFG: layer.overlay.getURL(bounds),
                urlPalette: $('scaleBar').src,
                title: $('layerPath').innerHTML,
                upperValue: $('scaleMax').value,
                twoThirds: $('scaleTwoThirds').innerHTML,
                oneThird: $('scaleOneThird').innerHTML,
                lowerValue: $('scaleMin').value,
                latLon: map.map.baseLayer.projection.getCode() == 'EPSG:4326'
            };
            // Add the elevation, time and units if this layer has them
            var zAxis = layer.details.zaxis;
            if (zAxis != null) {
                var axisLabel = zAxis.positive ? 'Elevation' : 'Depth';
                var zValue = $('zValues').options[$('zValues').selectedIndex].text;
                params.elevation = axisLabel + ': ' + zValue + ' ' + zAxis.units;
            }
            if (layer.details.datesWithData != null) {
                params.time = time.isoTValue;
            }
            if (typeof layer.details.units != 'undefined') {
                params.units = layer.details.units;
            }

            getScreenshotLink(layer.details.server, {
                callback: this.gotScreenshotLink.bind(this),
                error: this.screenshotError.bind(this),
                urlparams: params
            });
        }
    },

    // Called when the server has generated a screenshot
    gotScreenshotLink: function (url) {
        $('screenshotMessage').innerHTML = 'To save the screenshot, right-click on the image and select "Save Image As"';
        $('screenshotImage').src = url;
    },

    // Called when there is an error getting the screenshot
    screenshotError: function (exception) {
        this.screenshotPanel.hide();
        alert("Error getting screenshot: " + exception.className +
            ", Message: " + exception.message);
    },

    CLASS_NAME: "Godiva2.Screenshot"
 });