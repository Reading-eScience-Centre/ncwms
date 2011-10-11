/**
 * Class: Godiva2.Layer
 *
 */

 Godiva2.Layer = OpenLayers.Class({

    /**
     * Property: details
     * Details of the layer's metadata
     */
    details: null,

    /**
     * Points to the currently-active overlay that is coming from this ncWMS
     * Will point to either tiledOverlay or untiledOverlay.
     */
    overlay: null,
    
    /**
     * We shall maintain two separate overlays, one tiled (for scalar
     * quantities) and one untiled (for vector quantities)
     */
    tiledOverlay: null,
    untiledOverlay: null,
    animationOverlay: null,

    /**
     * Constructor: Godiva2.Layer
     * Create a Godiva2 Layer.  For example:
     *
     * > var layer = new Godiva2.Layer();

     * Parameters:
     * blah - description of blah
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
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

    setUnits: function () {
        // Units are ncWMS-specific
        if (this.isNcWMS()) {
            $('units').innerHTML = '<b>Units: </b>' + this.details.units;
        } else {
            $('units').innerHTML = '';
        }
    },
    
    isNcWMS: function () {
        if (typeof this.details.units != 'undefined') {
            return true;
        } else {
            return false;
        }
    },

    /**
     * Method: setDataOpacity
     * Sets the opacity of the ncwms layer if it exists
     */
    setDataOpacity: function () {
        var value = document.getElementById('opacityValue').value;
        if (this.overlay != null) this.overlay.setOpacity(value);
    },

    CLASS_NAME: "Godiva2.Layer"
 });