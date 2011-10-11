/**
 * Class: Godiva2.Depth
 *
 */

 Godiva2.Depth = OpenLayers.Class({

    /**
     * Property: blah
     * description of blah
     */
    blah: null,

    /**
     * Constructor: Godiva2.Depth
     * Create a Godiva2 Depth.  For example:
     *
     * > var depth = new Godiva2.Depth();

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

    configure: function () {
        // Set the range selector objects
        var zValue = autoLoad.zValue == null
            ? this.getZValue()
            : parseFloat(autoLoad.zValue);

        // clear the list of z values
        $('zValues').options.length = 0;

        var zAxis = layer.details.zaxis;
        if (zAxis == null) {
            $('zAxis').innerHTML = ''
            $('zValues').style.visibility = 'hidden';
        } else {
            var axisLabel = zAxis.positive ? 'Elevation' : 'Depth';
            $('zAxis').innerHTML = '<b>' + axisLabel + ' (' + zAxis.units + '): </b>';
            // Populate the drop-down list of z values
            // Make z range selector invisible if there are no z values
            var zValues = zAxis.values;
            var zDiff = 1e10; // Set to some ridiculously-high value
            var nearestIndex = 0;
            for (var j = 0; j < zValues.length; j++) {
                // Create an item in the drop-down list for this z level
                var zLabel = zAxis.positive ? zValues[j] : -zValues[j];
                $('zValues').options[j] = new Option(zLabel, zValues[j]);
                // Find the nearest value to the currently-selected
                // depth level
                var diff = Math.abs(parseFloat(zValues[j]) - zValue);
                if (diff < zDiff) {
                    zDiff = diff;
                    nearestIndex = j;
                }
            }
            $('zValues').selectedIndex = nearestIndex;
        }
    },

    // Gets the Z value set by the user
    getZValue: function () {
        // If we have no depth information, assume we're at the surface.  This
        // will be ignored by the map server
        return $('zValues').options.length == 0 ? 0 : $('zValues').value;
    },

    CLASS_NAME: "Godiva2.Depth"
 });