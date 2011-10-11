// Javascript for GODIVA2 page.

// Enable us to use bind for any function, thus propogating the 'this' scope
// through various callbacks etc. See http://www.digital-web.com/articles/scope_in_javascript/
Function.prototype.bind = function(obj) {
  var method = this,
   temp = function() {
    return method.apply(obj, arguments);
   };
  return temp;
 }

// Namespace declaration:
Godiva2 = {};

// NEW GLOBAL VARS
var portalWindow = null;
var map = null;
var time = null;
var menu = null;
var style = null;
var autoLoad = null;
var animation = null;
var screenshot = null;
var depth = null;
var layer = null;

// Called when the page has loaded
window.onload = function()
{
    // Initialise Global Vars
    portalWindow = new Godiva2.PortalWindow();
    map = new Godiva2.Map({div: "map"});
    autoLoad = new Godiva2.AutoLoad();
    time = new Godiva2.Time();
    menu = new Godiva2.Menu();
    style = new Godiva2.Style();
    animation = new Godiva2.Animation();
    screenshot = new Godiva2.Screenshot();
    depth = new Godiva2.Depth();
    layer = new Godiva2.Layer();

    // Set up event handlers (ones that need to pass on an argument can't be handled
    // like this. We use the referral methods at the end of this file
    document.getElementById('refreshLayers').onclick = menu.setup.bind(menu);
    document.getElementById('zValues').onchange = map.update.bind(map);
    document.getElementById('autoZoom').onclick = map.zoomToLayerBBOX.bind(map);
    document.getElementById('setFirstFrame').onclick = animation.setFirstAnimationFrame.bind(animation);
    document.getElementById('setLastFrame').onclick = animation.setLastAnimationFrame.bind(animation);
    document.getElementById('createAnimation').onclick = animation.createOpenLayersAnimation.bind(animation);
    document.getElementById('hideAnimation').onclick = animation.hideAnimation.bind(animation);
    document.getElementById('mapOverlay').onload = animation.animationLoaded.bind(animation);
    document.getElementById('scaleBar').onclick = style.showPaletteSelector.bind(style);
    document.getElementById('scaleMax').onblur = style.validateScaleAndUpdateMap.bind(style);
    document.getElementById('scaleSpacing').onchange = style.validateScaleAndUpdateMap.bind(style);
    document.getElementById('scaleMin').onblur = style.validateScaleAndUpdateMap.bind(style);
    document.getElementById('autoScale').onclick = style.autoScale.bind(style);
    document.getElementById('lockScale').onclick = style.toggleLockScale.bind(style);
    document.getElementById('screenshot').onclick = screenshot.loadScreenshot.bind(screenshot);
    document.getElementById('opacityValue').onchange = layer.setDataOpacity.bind(layer);
    document.getElementById('numColorBands').onchange = style.updatePaletteSelector.bind(style);
    document.getElementById('loadAnimation').onclick = animation.loadAnimation.bind(animation);

    // Set up the autoload object
    // Note that we must get the query string from the top-level frame
    // strip off the leading question mark
    autoLoad.populate(window.location);
    if (window.top.location != window.location) {
        // We're in an iframe so we must also use the query string from the top frame
        autoLoad.populate(window.top.location);
    }

    // Set up the left-hand menu of layers
    menu.setup();
}

// Called when the user clicks on the name of a displayable layer in the left-hand menu
// Gets the details (units, grid etc) of the given layer.
function layerSelected(layerDetails)
{
    map.clearPopups();
    layer.details = layerDetails;
    style.gotScaleRange = false;
    animation.resetAnimation();
    layer.setUnits();
    depth.configure();
    style.setVisibility(); 
    style.configure(layerDetails);
    map.prepare();
    portalWindow.setLayerInfoAndCopyright();

    time.configure();
    if (time.layerHasNoTime()) {
        // No time dimension for this layer, just update the map
        map.update();
    }
    else { // layer has a time dimension
        if (time.layerHasNormalYear()) { // layer has regular 365-,366-day years
            time.setupCalendar();
            // Pretend we clicked the calendar
            time.getSelectedDateAndLoadTimesteps(time.calendar, true);
        }
        else { // layer has 360-day years
            time.setupBasicDateSelector();
            var nearestDate = time.getNearestTime();
            time.loadTimesteps(nearestDate);
        }
    }
}

function gotTimesteps() {
    if (style.weAreAutoLoading()) {
        style.autoLoadScale();
        style.validateScaleAndUpdateMap();
    } else if (style.hasNoScaleRange()) {
        style.autoScale();
    } else {
        map.update();
    }
}

// Handle Events which send an argument from the user interface
// We simply forward the call the the method in the relevant class
function setColourScaleExtreme(minOrMax, value) {style.setColourScaleExtreme(minOrMax, value);}
function paletteSelected(thePalette) {style.paletteSelected(thePalette);}
function popUp(url, width, height) {portalWindow.popUp(url, width, height);}
