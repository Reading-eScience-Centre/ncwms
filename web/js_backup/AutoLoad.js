/**
 * Class: Godiva2.Animation
 *
 */

 Godiva2.AutoLoad = OpenLayers.Class({

    /**
     * Property: calendar
     * {<OpenLayers.Map>} The DHTML calendar for normal (365- or 366-day) years
     */
    layer: null,
    zValue: null,
    isoTValue: null,
    bbox: null,
    scaleMin: null,
    scaleMax: null,
    menu: null,
    debugMode: null,

    /**
     * Constructor: Godiva2.AutoLoad
     * Create a Godiva2 AutoLoad.  For example:
     *
     * > var autoLoad = new Godiva2.AutoLoad();

     * Parameters:
     * layer - details of the layer which the TimeDimension represents
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
        this.menu = '';
        this.debugMode = false;
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

    /**
     * Method: populate
     * Populates the autoLoad object from the given window location object
     */
    populate: function (windowLocation) {
        var queryString = windowLocation.search.split('?')[1];
        if (queryString != null) {
            var kvps = queryString.split('&');
            for (var i = 0; i < kvps.length; i++) {
                var keyAndVal = kvps[i].split('=');
                if (keyAndVal.length > 1) {
                    var key = keyAndVal[0].toLowerCase();
                    if (key == 'layer') {
                        this.layer = keyAndVal[1];
                    } else if (key == 'elevation') {
                        this.zValue = keyAndVal[1];
                    } else if (key == 'time') {
                        this.isoTValue = keyAndVal[1];
                    } else if (key == 'bbox') {
                        this.bbox = keyAndVal[1];
                    } else if (key == 'scale') {
                        this.scaleMin = keyAndVal[1].split(',')[0];
                        this.scaleMax = keyAndVal[1].split(',')[1];
                    } else if (key == 'menu') {
                        // we load a specific menu instead of the default
                        this.menu = keyAndVal[1];
                    } else if (key == 'debugmode') {
                        this.debugMode = keyAndVal[1] == 'true';
                    }
                }
            }
        }
    },

    reset: function () {
        this.layer = null;
        this.zValue = null;
        this.isoTValue = null;
        this.bbox = null;
        this.scaleMin = null;
        this.scaleMax = null;
    },

    CLASS_NAME: "Godiva2.AutoLoad"
 });