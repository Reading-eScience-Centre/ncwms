/**
 * Class: Godiva2.PortalWindow
 *
 */

 Godiva2.PortalWindow = OpenLayers.Class({

    /**
     * Property: blah
     * description of blah
     */
    blah: null,

    /**
     * Constructor: Godiva2.PortalWindow
     * Create a Godiva2 PortalWindow.  For example:
     *
     * > var portalWindow = new Godiva2.PortalWindow();

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

    setLayerInfoAndCopyright: function () {
        // Set the link to more details about this dataset
        if (typeof layer.details.moreInfo != 'undefined' &&
                layer.details.moreInfo != '') {
            $('moreInfo').innerHTML = '<a target="_blank" href="' + layer.details.moreInfo +
                '">More information</a>';
        } else {
            $('moreInfo').innerHTML = '';
        }

        // Set up the copyright statement
        $('copyright').innerHTML = layer.details.copyright;
    },

    // Gets the value of the element with the given name from the given XML document,
    // or null if the given element doesn't exist
    getElementValue: function (xml, elName) {
        var el = xml.getElementsByTagName(elName);
        if (!el || !el[0] || !el[0].firstChild) return null;
        return el[0].firstChild.nodeValue;
    },

    popUp: function (url, width, height) {
        var day = new Date();
        var id = day.getTime();
        window.open(url, id, 'toolbar=0,scrollbars=0,location=0,statusbar=0,menubar=0,resizable=1,width='
            + width + ',height=' + height + ',left = 300,top = 300');
    },

    // Sets the permalink, i.e. the link back to this view of the page
    setPermalinkURL: function () {
        if (layer != null && layer.details != null) {
            // Note that we must use window.top to get the containing page, in case
            // the Godiva2 page is embedded in an iframe
            // Get the window location, minus any query string or hash
            var loc = window.top.location;
            var url = loc.protocol + '//' + loc.host + loc.pathname;
            url +=
                '?menu=' + autoLoad.menu +
                '&layer=' + layer.details.id +
                '&elevation=' + depth.getZValue() +
                '&time=' + time.isoTValue +
                '&scale=' + style.scaleMinVal + ',' + style.scaleMaxVal +
                '&bbox=' + map.map.getExtent().toBBOX();
            $('permalink').innerHTML = '<a target="_blank" href="' + url +
                '">Permalink</a>&nbsp;|&nbsp;<a href="mailto:?subject=Godiva2%20link&body='
                + escape(url) + '">email</a>';
            $('permalink').style.visibility = 'visible';
        }
    },

    // Sets the URL for "Open in Google Earth"
    // TODO: does this screw up if we're looking in polar stereographic coords?
    setGEarthURL: function () {
        if (layer != null && layer.overlay != null) {
            if (time.timeSeriesSelected()) {
                // We need to call a Javascript function to generate the timeseries URL
                $('googleEarth').innerHTML = '<a href="#">Open animation in Google Earth</a>';
                document.getElementById('googleEarth').onclick = animation.createGoogleEarthAnimation.bind(animation);
            } else {
                $('googleEarth').innerHTML = '<a href="#">Open in Google Earth</a>';
                document.getElementById('googleEarth').onclick = this.loadGEarthURL.bind(this);
            }
        }
    },

    // Generates a URL for generating a GetMap request that returns KMZ for opening
    // in Google Earth.
    loadGEarthURL: function () {
        var gEarthURL = null;
        if (layer.overlay != null) {
            // Get a URL for a WMS request that covers the current map extent
            var mapBounds = map.map.getExtent();
            var urlEls = layer.overlay.getURL(mapBounds).split('&');
            gEarthURL = urlEls[0];
            for (var i = 1; i < urlEls.length; i++) {
                if (urlEls[i].startsWith('FORMAT')) {
                    // Make sure the FORMAT is set correctly
                    gEarthURL += '&FORMAT=application/vnd.google-earth.kmz';
                } else if (urlEls[i].startsWith('BBOX')) {
                    // Set the bounding box so that there are no transparent pixels around
                    // the edge of the image: i.e. find the intersection of the layer BBOX
                    // and the viewport BBOX
                    gEarthURL += '&BBOX=' + map.getIntersectionBBOX();
                } else if (urlEls[i].startsWith('WIDTH')) {
                    gEarthURL += '&WIDTH=' + map.map.size.w;
                } else if (urlEls[i].startsWith('HEIGHT')) {
                    gEarthURL += '&HEIGHT=' + map.map.size.h;
                } else if (time.timeSeriesSelected() && urlEls[i].startsWith('TIME')) {
                    gEarthURL += '&TIME=' + animation.animationEls.animationResolutionEl.value
                } else {
                    gEarthURL += '&' + urlEls[i];
                }
            }
        }
        //return gEarthURL;
        window.location = gEarthURL;
    },

    CLASS_NAME: "Godiva2.PortalWindow"
 });