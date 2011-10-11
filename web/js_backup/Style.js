/**
 * Class: Godiva2.Style
 *
 */

 Godiva2.Style = OpenLayers.Class({

    /**
     * Property: calendar
     * {<OpenLayers.Map>} The DHTML calendar for normal (365- or 366-day) years
     */
    styleEls: null,
    gotScaleRange: null,
    scaleLocked: null,
    scaleMinVal: null,
    scaleMaxVal: null,
    paletteName: null,
    paletteSelector: null,

    /**
     * Constructor: Godiva2.Style
     * Create a Godiva2 Style.  For example:
     *
     * > var style = new Godiva2.Style();

     * Parameters:
     * blah - description of blah
     */
    initialize: function () { 
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)

        this.styleEls = {};
        this.styleEls.scaleMinEl = document.getElementById('scaleMin');
        this.styleEls.scaleMaxEl = document.getElementById('scaleMax');
        this.styleEls.scaleBarEl = document.getElementById('scaleBar');
        this.styleEls.scaleControlsEl = document.getElementById('scaleControls');
        this.styleEls.autoScaleEl = document.getElementById('autoScale');
        this.styleEls.scaleSpacingEl = document.getElementById('scaleSpacing');
        this.styleEls.numColorBandsEl = document.getElementById('numColorBands');
        this.styleEls.lockScaleEl = document.getElementById('lockScale');
        this.styleEls.paletteDivEl = document.getElementById('paletteDiv');

        // reset the scale markers
        this.styleEls.scaleMinEl.value = '';
        this.styleEls.scaleMaxEl.value = '';

        this.gotScaleRange = false;
        this.scaleLocked = false;

        // Set up the palette selector pop-up
        this.paletteSelector = new YAHOO.widget.Panel("paletteSelector", {
            width:"400px",
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

    setVisibility: function () {
        // Only show the scale bar if the data are coming from an ncWMS server
        var scaleVisibility = layer.isNcWMS() ? 'visible' : 'hidden';
        this.styleEls.scaleBarEl.style.visibility = scaleVisibility;
        this.styleEls.scaleMinEl.style.visibility = scaleVisibility;
        this.styleEls.scaleMaxEl.style.visibility = scaleVisibility;
        this.styleEls.scaleControlsEl.style.visibility = scaleVisibility;
        this.styleEls.autoScaleEl.style.visibility = this.scaleLocked ? 'hidden' : scaleVisibility;
    },

    configure: function () {
        this.setValues();
        this.setPalette();
        this.setSpacing();
    },

    setValues: function () {
        // Set the scale value if this is present in the metadata
        if (typeof layer.details.scaleRange != 'undefined' &&
                layer.details.scaleRange != null &&
                layer.details.scaleRange.length > 1 &&
                layer.details.scaleRange[0] != layer.details.scaleRange[1] &&
                !this.scaleLocked) {
            this.scaleMinVal = parseFloat(layer.details.scaleRange[0]);
            this.scaleMaxVal = parseFloat(layer.details.scaleRange[1]);
            this.styleEls.scaleMinEl.value = this.scaleMinVal.toPrecision(4);
            this.styleEls.scaleMaxEl.value = this.scaleMaxVal.toPrecision(4);
            this.gotScaleRange = true;
        }
    },

    setPalette: function () {
        // Set the palette for this variable
        if (this.paletteName == null || !this.scaleLocked) {
            if (typeof layer.details.defaultPalette != 'undefined') {
                this.paletteName = layer.details.defaultPalette;
            }
            this.updateScaleBar();
        }
    },

    setSpacing: function () {
        if (!this.scaleLocked && typeof layer.details.logScaling != 'undefined') {
            this.styleEls.scaleSpacingEl.value = layer.details.logScaling ? 'logarithmic' : 'linear';
        }
    },

    /**
     * Method: updateScaleBar
     * Updates the colour scale bar URL
     */
    updateScaleBar: function () {
        this.styleEls.scaleBarEl.src = 'wms?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=398'
            + '&PALETTE=' + this.paletteName + '&NUMCOLORBANDS=' + this.styleEls.numColorBandsEl.value;
    },

    weAreAutoLoading: function () {
        return autoLoad.scaleMin != null && autoLoad.scaleMax != null;
    },

    hasNoScaleRange: function () {
        return !this.gotScaleRange && !this.scaleLocked;
    },

    setColourScaleExtreme: function (minOrMax, value) {
        var el = minOrMax == 'min' ? this.styleEls.scaleMinEl : this.styleEls.scaleMaxEl;
        el.value = value;
        this.validateScaleAndUpdateMap();
    },

    /**
     * Method: setColourScaleMin
     * Sets the minimum value of the colour scale
     */
    setColourScaleMin: function (scaleMin) {
        // Get the colour value from the DOM and trim the string to get rid of whitespace
        //var scaleMin = document.getElementById('colourValue').innerHTML.replace(/^\s+|\s+$/g, '');
        this.styleEls.scaleMinEl.value = scaleMin;
        this.validateScaleAndUpdateMap();
    },

    /**
     * Method: setColourScaleMax
     * Sets the minimum value of the colour scale
     */
    setColourScaleMax: function (scaleMax) {
        // Get the colour value from the DOM and trim the string to get rid of whitespace
        //var scaleMax = document.getElementById('colourValue').innerHTML.replace(/^\s+|\s+$/g, '');
        this.styleEls.scaleMaxEl.value = scaleMax;
        this.validateScaleAndUpdateMap();
    },

    autoLoadScale: function () {
        this.styleEls.scaleMinEl.value = autoLoad.scaleMin;
        this.styleEls.scaleMaxEl.value = autoLoad.scaleMax;
    },

    /**
     * Method: validateScaleAndUpdateMap
     * Validates the entries for the scale bar
     */
    validateScaleAndUpdateMap: function () {
        var fMin = parseFloat(this.styleEls.scaleMinEl.value);
        var fMax = parseFloat(this.styleEls.scaleMaxEl.value);
        if (isNaN(fMin)) {
            alert('Scale limits must be set to valid numbers');
            // Reset to the old value
            this.styleEls.scaleMinEl.value = this.scaleMinVal;
        } else if (isNaN(fMax)) {
            alert('Scale limits must be set to valid numbers');
            // Reset to the old value
            this.styleEls.scaleMaxEl.value = this.scaleMaxVal;
        } else if (fMin > fMax) {
            alert('Minimum scale value must be less than the maximum');
            // Reset to the old values
            this.styleEls.scaleMinEl.value = this.scaleMinVal;
            this.styleEls.scaleMaxEl.value = this.scaleMaxVal;
        } else if (fMin <= 0 && this.styleEls.scaleSpacingEl.value == 'logarithmic') {
            alert('Cannot use a logarithmic scale with negative or zero values');
            this.styleEls.scaleSpacingEl.value = 'linear';
        } else {
            this.styleEls.scaleMinEl.value = fMin;
            this.styleEls.scaleMaxEl.value = fMax;
            this.scaleMinVal = fMin;
            this.scaleMaxVal = fMax;
            map.update();
        }
    },

    /**
     * Method: autoScale
     * Calls the WMS to find the min and max data values, then rescales.
     * If this is a newly-selected variable the method gets the min and max values
     * for the whole layer.  If not, this gets the min and max values for the viewport.
     */
    autoScale: function(event) {
        var newVariable;
        if (event) {
            // we've been called from the user interface and hence don't have a new variable
            newVariable = false;
        } else {
            // we've been called from gotTimesteps() with style.hasNoScaleRange == true.
            newVariable = true;
        }

        var dataBounds;
        if ($('tValues')) {
            time.isoTValue = $('tValues').value;
        }
        if (newVariable) {
            // We use the bounding box of the whole layer
            dataBounds = map.bbox[0] + ',' + map.bbox[1] + ',' + map.bbox[2] + ',' + map.bbox[3];
        } else {
            // Use the intersection of the viewport and the layer's bounding box
            dataBounds = map.getIntersectionBBOX();
        }
        getMinMax(layer.details.server, {
            callback: this.gotMinMax.bind(this),
            layerName: layer.details.id,
            bbox: dataBounds,
            crs: map.map.baseLayer.projection.toString(), // (projection is a Projection object)
            elevation: depth.getZValue(),
            time: time.isoTValue
        });
    },

    /**
     * Method: gotMinMax
     * This function is called when we have received the min and max values from the server
     */
    gotMinMax: function (minmax) {
        this.styleEls.scaleMinEl.value = minmax.min.toPrecision(4);
        this.styleEls.scaleMaxEl.value = minmax.max.toPrecision(4);
        this.validateScaleAndUpdateMap(); 
    },

    /**
     * Method: toggleLockScale
     * When the scale is locked, the user cannot change the colour scale either
     * by editing manually or clicking "auto".  Furthermore the scale will not change
     * when a new layer is loaded
     */
    toggleLockScale: function () {
        if (this.scaleLocked) {
            // We need to unlock the scale
            this.scaleLocked = false;
            this.styleEls.lockScaleEl.innerHTML = 'lock';
            this.styleEls.autoScaleEl.style.visibility = 'visible';
            this.styleEls.scaleSpacingEl.disabled = false;
            this.styleEls.scaleMinEl.disabled = false;
            this.styleEls.scaleMaxEl.disabled = false;
        } else {
            // We need to lock the scale
            this.scaleLocked = true;
            this.styleEls.lockScaleEl.innerHTML = 'unlock';
            this.styleEls.autoScaleEl.style.visibility = 'hidden';
            this.styleEls.scaleSpacingEl.disabled = true;
            this.styleEls.scaleMinEl.disabled = true;
            this.styleEls.scaleMaxEl.disabled = true;
        }
    },

    /**
     * Method: showPaletteSelector
     * Shows a pop-up window with the available palettes for the user to select
     * This is called when the user clicks the colour scale bar
     */
    showPaletteSelector: function () {
        this.updatePaletteSelector();
        this.paletteSelector.render(document.body);
        this.paletteSelector.show();
    },

    /**
     * Method: updatePaletteSelector
     * Updates the contents of the palette selection table
     */
    updatePaletteSelector: function () {
        // Populate the palette selector dialog box
        // TODO: revert to default palette if layer doesn't support this one
        var palettes = layer.details.palettes;
        if (palettes == null || palettes.length == 0) {
            this.styleEls.paletteDivEl.innerHTML = 'There are no alternative palettes for this layer';
            return;
        }

        // TODO test if coming from a different server
        var width = 50;
        var height = 200;
        var server = layer.details.server;
        if (server == '') server = 'wms';
        var paletteUrl = server + '?REQUEST=GetLegendGraphic' +
            '&LAYER=' + layer.details.id +
            '&COLORBARONLY=true' +
            '&WIDTH=1' +
            '&HEIGHT=' + height +
            '&NUMCOLORBANDS=' + this.styleEls.numColorBandsEl.value;
        var palStr = '<div style="overflow: auto">'; // ensures scroll bars appear if necessary
        palStr += '<table border="1"><tr>';
        for (var i = 0; i < palettes.length; i++) {
            palStr += '<td><img src="' + paletteUrl + '&PALETTE=' + palettes[i] +
                '" width="' + width + '" height="' + height + '" title="' + palettes[i] +
                '" onclick="paletteSelected(\'' + palettes[i] + '\')"' +
                '/></td>';
        }
        palStr += '</tr></table></div>';
        this.styleEls.paletteDivEl.innerHTML = palStr;
    },

    /**
     * Method: paletteSelected
     * Called when the user selects a new palette in the palette selector
     */
    paletteSelected: function (thePalette) {
        this.paletteName = thePalette;
        this.paletteSelector.hide();
        // Change the colour scale bar on the main page
        this.updateScaleBar();
        map.update();
    },

    CLASS_NAME: "Godiva2.style"
 });