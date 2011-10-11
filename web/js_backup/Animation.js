/**
 * Class: Godiva2.Animation
 *
 */

 Godiva2.Animation = OpenLayers.Class({

    /**
     * Property: blah
     * Description of blah
     */
    animationEls: null,
    isGoogleEarthAnim: null,
    animationSelector: null,

    /**
     * Constructor: Godiva2.Animation
     * Create a Godiva2 Animation.  For example:
     *
     * > var animation = new Godiva2.Animation();

     * Parameters:
     * blah - description of blah
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
        this.animationEls = {};
        this.animationEls.firstFrameEl = document.getElementById('firstFrame');
        this.animationEls.lastFrameEl = document.getElementById('lastFrame');
        this.animationEls.setFramesEl = document.getElementById('setFrames');
        this.animationEls.animationEl = document.getElementById('animation');
        this.animationEls.animationResolutionEl = document.getElementById('animationResolution');
        this.animationEls.hideAnimationEl = document.getElementById('hideAnimation');

        this.isGoogleEarthAnim = false;

        // Set up the animation selector pop-up
        this.animationSelector = new YAHOO.widget.Panel("animationSelector", {
            width:"300px",
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

    resetAnimation: function () {
        this.hideAnimation();
        this.animationEls.setFramesEl.style.visibility = 'hidden';
        this.animationEls.animationEl.style.visibility = 'hidden';
        this.animationEls.firstFrameEl.innerHTML = '';
        this.animationEls.lastFrameEl.innerHTML = '';
    },

    setFirstAnimationFrame: function () {
        this.animationEls.firstFrameEl.innerHTML = $('tValues').value;
        this.animationEls.animationEl.style.visibility = 'visible';
        portalWindow.setGEarthURL();
    },

    setLastAnimationFrame: function () {
        this.animationEls.lastFrameEl.innerHTML = $('tValues').value;
        this.animationEls.animationEl.style.visibility = 'visible';
        portalWindow.setGEarthURL();
    },

    createGoogleEarthAnimation: function () {
        this.isGoogleEarthAnim = true;
        this.createAnimation();
    },

    createOpenLayersAnimation: function () {
        this.isGoogleEarthAnim = false;
        this.createAnimation();
    },

    createAnimation: function () {
        if (!time.timeSeriesSelected()) {
            alert("Must select a first and last frame for the animation");
            return;
        }

        getAnimationTimesteps(layer.details.server, {
            callback: this.gotAnimationTimesteps.bind(this),
            layerName: layer.details.id,
            startTime: this.animationEls.firstFrameEl.innerHTML,
            endTime: this.animationEls.lastFrameEl.innerHTML
        });
    },

    gotAnimationTimesteps: function (timesteps) {
        // Look in the timesteps object and generate the list of options
        this.animationEls.animationResolutionEl.options.length = 0;
        for (var i = 0; i < timesteps.length; i++) {
            var timestep = timesteps[i];
            this.animationEls.animationResolutionEl.options[i] = new Option(timestep.title, timestep.timeString);
        }
        // Select the last item by default (this has the smallest number of frames,
        // so if the user clicks "OK" by mistake the load on the server will be
        // minimized)
        this.animationEls.animationResolutionEl.selectedIndex = this.animationEls.animationResolutionEl.options.length - 1;
        this.animationSelector.render(document.body);
        this.animationSelector.show();
    },

    // Called when the user clicks "OK" in the animation selector
    loadAnimation: function () {
        this.animationSelector.hide();
        var i;
        var urlEls;
        var newURL;
        if (this.isGoogleEarthAnim) {
            /*
            urlEls = portalWindow.getGEarthURL().split('&');
            newURL = urlEls[0];
            for (i = 1; i < urlEls.length; i++) {
                if (urlEls[i].startsWith('TIME=')) {
                    newURL += '&TIME=' + this.animationEls.animationResolutionEl.value
                } else {
                    newURL += '&' + urlEls[i];
                }
            }
            // Load the KMZ file
            window.location = newURL;
            */
           portalWindow.loadGEarthURL();
        } else {
            // Get a URL for a WMS request that covers the current map extent
            urlEls = layer.overlay.getURL(map.map.getExtent()).split('&');
            // Replace the parameters as needed.
            var width = $('map').clientWidth;// / 2;
            var height = $('map').clientHeight;// / 2;
            newURL = urlEls[0];
            for (i = 1; i < urlEls.length; i++) {
                if (urlEls[i].startsWith('TIME=')) {
                    newURL += '&TIME=' + this.animationEls.animationResolutionEl.value
                } else if (urlEls[i].startsWith('FORMAT')) {
                    newURL += '&FORMAT=image/gif';
                } else if (urlEls[i].startsWith('WIDTH')) {
                    newURL += '&WIDTH=' + width;
                } else if (urlEls[i].startsWith('HEIGHT')) {
                    newURL += '&HEIGHT=' + height;
                } else {
                    newURL += '&' + urlEls[i];
                }
            }
            // The animation will be displayed on the map
            $('autoZoom').style.visibility = 'hidden';
            this.animationEls.hideAnimationEl.style.visibility = 'visible';
            // We show the "please wait" image then immediately load the animation
            $('throbber').style.visibility = 'visible'; // This will be hidden by animationLoaded()

            // When the mapOverlay has been loaded we call animationLoaded() and place the image correctly
            // on the map
            $('mapOverlay').src = newURL;
            $('mapOverlay').width = width;
            $('mapOverlay').height = height;
        }
    },

    animationLoaded: function () {
        $('throbber').style.visibility = 'hidden';
        //$('mapOverlayDiv').style.visibility = 'visible';
        // Load the image into a new layer on the map
        layer.animationOverlay = new OpenLayers.Layer.Image(
            "ncWMS", // Name for the layer
            $('mapOverlay').src, // URL to the image
            map.map.getExtent(), // Image bounds
            new OpenLayers.Size($('mapOverlay').width, $('mapOverlay').height), // Size of image
            { // Other options
                isBaseLayer : false,
                maxResolution: map.map.baseLayer.maxResolution,
                minResolution: map.map.baseLayer.minResolution,
                resolutions: map.map.baseLayer.resolutions
            }
        );
        map.setVisibleOverlay(true);
        map.map.addLayers([layer.animationOverlay]);
    },

    hideAnimation: function () {
        map.setVisibleOverlay(false);
        $('autoZoom').style.visibility = 'visible';
        this.animationEls.hideAnimationEl.style.visibility = 'hidden';
        $('mapOverlayDiv').style.visibility = 'hidden';
    },

    CLASS_NAME: "Godiva2.Animation"
 });