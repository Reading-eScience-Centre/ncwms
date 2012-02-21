package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gwtopenmaps.openlayers.client.LonLat;
import org.gwtopenmaps.openlayers.client.OpenLayers;
import org.gwtopenmaps.openlayers.client.event.MapMoveListener.MapMoveEvent;
import org.gwtopenmaps.openlayers.client.event.MapZoomListener.MapZoomEvent;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.ElevationSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.GodivaActionsHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.PaletteSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ConnectionException;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ErrorHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerDetails;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestCallback;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.AnimationButton;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MapArea;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.WidgetCollection;
import uk.ac.rdg.resc.ncwms.gwt.shared.CaseInsensitiveParameterMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.impl.StringBuilderImpl;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class BaseWmsClient implements EntryPoint, ErrorHandler, GodivaActionsHandler, LayerSelectionHandler, ElevationSelectionHandler, TimeDateSelectionHandler, PaletteSelectionHandler {

    private int mapHeight;
    private int mapWidth;
    private String proxyUrl;
    protected String wmsUrl;
    private String docHref;
    private String currentProjection;
    
    private String currentTime;
    private String currentElevation;
    
    protected MapArea mapArea;
    /*
     * A count of how many items we are currently waiting to load.
     */
    private int loadingCount;    
    
    /*
     * These 3 booleans are used so that we only update the map when all
     * required data have been loaded
     */
    private boolean layerDetailsLoaded;
    private boolean dateTimeDetailsLoaded;
    private boolean minMaxDetailsLoaded;
    
    /*
     * Whether or not the current call to the service contains all of the state
     * information in the url or not (for permalinking)
     */
    private boolean permalinking;
    private CaseInsensitiveParameterMap permalinkParamsMap;
    
    @Override
    public void onModuleLoad() {
        RequestBuilder getConfig = new RequestBuilder(RequestBuilder.GET, "getconfig");
        getConfig.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                try{
                    JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                    JSONObject parentObj = jsonMap.isObject();
                    proxyUrl = parentObj.get("proxy").isString().stringValue();
                    docHref = parentObj.get("docLocation").isString().stringValue();
                    mapHeight = Integer.parseInt(parentObj.get("mapHeight").isString().stringValue());
                    mapWidth = Integer.parseInt(parentObj.get("mapWidth").isString().stringValue());
                    initBaseWms();
                } catch(Exception e){
                    initWithDefaults();
                }
            }
            
            @Override
            public void onError(Request request, Throwable exception) {
                initWithDefaults();
            }
        });
        try {
            getConfig.send();
        } catch (RequestException e) {
            initWithDefaults();
        }
    }
    
    /**
     * Initializes the WMS client with some default settings.
     * 
     * Subclasses can override this to define new defaults
     */
    protected void initWithDefaults() {
        mapHeight = 400;
        mapWidth = 512;
        proxyUrl = "proxy?";
        proxyUrl = "";
        docHref = "http://www.resc.rdg.ac.uk/trac/ncWMS/wiki/GodivaTwoUserGuide";
        initBaseWms();
    }
    
    private void initBaseWms(){
        wmsUrl = "wms";
        loadingCount = 0;
        OpenLayers.setProxyHost(proxyUrl);
        mapArea = new MapArea(wmsUrl, mapWidth, mapHeight, this);
        String permalinkString = Window.Location.getParameter("permalinking");
        if (permalinkString != null && Boolean.parseBoolean(permalinkString)) {
            permalinking = true;
            permalinkParamsMap = CaseInsensitiveParameterMap.getMapFromList(Window.Location
                    .getParameterMap());
        }
        kmzLink = new Anchor("Open in Google Earth");
        kmzLink.setStylePrimaryName("labelStyle");
        kmzLink.setTitle("Open the current view in Google Earth");

        permalink = new Anchor("Permalink");
        permalink.setStylePrimaryName("labelStyle");
        permalink.setTarget("_blank");
        permalink.setTitle("Permanent link to the current view");

        email = new Anchor("Email Link");
        email.setStylePrimaryName("labelStyle");
        email.setTitle("Email a link to the current view");

        screenshot = new Anchor("Export to PNG");
        screenshot.setHref("/screenshot?");
        screenshot.setStylePrimaryName("labelStyle");
        screenshot.setTarget("_blank");
        screenshot.setTitle("Open a downloadable image in a new window - may be slow to load");
        
/*
 * TODO GOES SOMEWHERE ELSE
 */
        Anchor docLink = new Anchor("Documentation", docHref);
        docLink.setTarget("_blank");
        docLink.setTitle("Open documentation in a new window");
        
        init();
        
        requestMenu();
    }
    
    protected String getWmsUrl(String request, Map<String, String> parameters){
        StringBuilderImpl url = new StringBuilderImpl();
        url.append("?request="+request);
        for(String key : parameters.keySet()){
            url.append("&"+key+"="+parameters.get(key));
        }
        return getUrl(url.toString());
    }
    
    private String getUrl(String url){
        return URL.encode(proxyUrl + wmsUrl + url);
    }
    
    protected int getMapHeight(){
        return mapHeight;
    }
    
    protected int getMapWidth(){
        return mapWidth;
    }
    
    private LayerMenuItem populateLayers(JSONObject json) {
        String nodeLabel = json.get("label").isString().stringValue();
        JSONValue children = json.get("children");
        LayerMenuItem rootItem = new LayerMenuItem(nodeLabel, "rootId");
        JSONArray childrenArray = children.isArray();
        for (int i = 0; i < childrenArray.size(); i++) {
            addNode(childrenArray.get(i).isObject(), rootItem);
        }
        return rootItem;
    }
    
    private void addNode(JSONObject json, LayerMenuItem parentItem) {
        final String label = json.get("label").isString().stringValue();
        final String id = json.get("id").isString().stringValue();
        LayerMenuItem newChild = new LayerMenuItem(label, id);
        parentItem.addChildItem(newChild);
        
        // The JSONObject is an array of leaf nodes
        JSONValue children = json.get("children");
        if (children != null) {
            /*
             * We have a branch node
             */
            JSONArray childrenArray = children.isArray();
            for (int i = 0; i < childrenArray.size(); i++) {
                addNode(childrenArray.get(i).isObject(), newChild);
            }
        }
    }
    
    protected void requestMenu() {
        RequestBuilder getMenuRequest = new RequestBuilder(RequestBuilder.GET, getUrl("?request=GetMetadata&item=menu"));
        getMenuRequest.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request req, Response response) {
                try {
                    if(response.getStatusCode() != Response.SC_OK){
                        throw new ConnectionException("Error contacting server");
                    }
                    JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                    JSONObject parentObj = jsonMap.isObject();
                    LayerMenuItem menuTree = populateLayers(parentObj);

                    menuLoaded(menuTree);
                } catch (Exception e) {
                    invalidJson(e);
                } finally {
                    setLoading(false);
                }
            }

            @Override
            public void onError(Request request, Throwable e) {
                setLoading(false);
                handleError(e);
            }
        });

        try {
            setLoading(true);
            getMenuRequest.send();
        } catch (RequestException e) {
            handleError(e);
        }
    }
    
    /**
     * @param layerId
     * @param currentTime can be null
     * @param autoUpdate
     */
    protected void requestLayerDetails(String layerId, final String currentElevation, final String currentTime, final boolean autoUpdate){
        if (layerId == null) {
            // We have no variables defined in the selected layer
            // Return here. We are already dealing with the case where there are
            // no layers present
            return;
        }
        layerDetailsLoaded = false;
        dateTimeDetailsLoaded = false;
        minMaxDetailsLoaded = false;
        
        // TODO PERMALINKING
//        if(permalinking){
//            currentTime = permalinkParamsMap.get("time");
//        }

        LayerRequestBuilder getLayerDetailsRequest = new LayerRequestBuilder(layerId, proxyUrl+wmsUrl, currentTime);
        
        getLayerDetailsRequest.setCallback(new LayerRequestCallback(layerId, this) {
            @Override
            public void onResponseReceived(Request req, Response response) {
                try {
                    super.onResponseReceived(req, response);
                    if(response.getStatusCode() != Response.SC_OK){
                        throw new ConnectionException("Error contacting server");
                    }
                    
                    layerDetailsLoaded(getLayerDetails(), autoUpdate);
                    
                    dateSelected(null,getLayerDetails().getNearestDate());

                    if (autoUpdate) {
                        try {
                            mapArea.zoomToExtents(extents);
                            // extentsUpdated = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        requestAutoRange(getLayerDetails().getId(), currentElevation, currentTime, false);
                    } else {
                        minMaxDetailsLoaded = true;
                    }
                    layerDetailsLoaded = true;
                    updateMap();
                } catch (Exception e) {
                    invalidJson(e);
                } finally {
                    setLoading(false);
                }
            }

            @Override
            public void onError(Request request, Throwable e) {
                setLoading(false);
                // TODO DEAL WITH THESE
//                layerDetailsLoaded = true;
//                updateMap();
                handleError(e);
            }
        });

        try {
            setLoading(true);
            getLayerDetailsRequest.send();
        } catch (RequestException e) {
            handleError(e);
        }
    }
    
    protected void requestAutoRange(String layer, String elevation, String time, boolean force){
        Map<String, String> parameters = new HashMap<String, String>();
        /*
         * We use 1.1.1 here, because if getMap().getProjection() returns EPSG:4326, 
         * getMap().getExtent().toBBox(4) will still return in long-lat co-ords
         */
        parameters.put("item", "minmax");
        parameters.put("layers", layer);
        parameters.put("srs", currentProjection);
        parameters.put("height", "50");
        parameters.put("width", "50");
        parameters.put("version", "1.1.1");
        parameters.put("bbox", mapArea.getMap().getExtent().toBBox(4));
        if(elevation != null){
            parameters.put("elevation", elevation);
        }
        if(time != null){
            parameters.put("time", time);
        }

        RequestBuilder getMinMaxRequest = new RequestBuilder(RequestBuilder.GET, getWmsUrl("GetMetadata", parameters));
        getMinMaxRequest.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request req, Response response) {
                if(response.getText() != null && !response.getText().isEmpty()){
                    try{
                        JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                        JSONObject parentObj = jsonMap.isObject();
                        double min = parentObj.get("min").isNumber().doubleValue();
                        double max = parentObj.get("max").isNumber().doubleValue();
                        rangeLoaded(min, max);
                    } catch (Exception e){
                        minMaxDetailsLoaded = true;
                        updateMap();
                    }
                }
                setLoading(false);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                // We have failed, but we still want to update the map
                setLoading(false);
                minMaxDetailsLoaded = true;
                updateMap();
                handleError(exception);
            }
        });
        try {
            setLoading(true);
            getMinMaxRequest.send();
        } catch (RequestException e) {
            handleError(e);
        }
    }
    
    public void setLoading(boolean loading) {
        if (loading) {
            loadingCount++;
            if (loadingCount == 1) {
                loadingStarted();
            }
        } else {
            loadingCount--;
            if (loadingCount == 0) {
                loadingFinished();
            }
        }
    }
        
    protected void invalidJson(Exception e){
        final DialogBox popup = new DialogBox();
        VerticalPanel v = new VerticalPanel();
        if(e instanceof ConnectionException){
            v.add(new Label(e.getMessage()));
        } else {
            v.add(new Label("Invalid JSON returned from server"));
        }
        popup.setText("Error");
        Button b = new Button();
        b.setText("Close");
        b.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                popup.hide();
            }
        });
        v.add(b);
        v.setCellHorizontalAlignment(b, HasHorizontalAlignment.ALIGN_CENTER);
        popup.setWidget(v);
        popup.center();
    }

    public void handleError(Throwable e) {
        // TODO general method for handling errors
        e.printStackTrace();
    }
    
    protected void handleCatastrophicError(String message){
        HTML errorMessage = new HTML();
        Window.setTitle("Unrecoverable Error");
        errorMessage.setHTML(message);
        RootLayoutPanel mainWindow = RootLayoutPanel.get();
        for (int i = 0; i < mainWindow.getWidgetCount(); i++) {
            mainWindow.remove(0);
        }
        mainWindow.add(errorMessage);
    }
    
    private void updateLinksEtc() {
        anim.updateDetails(currentLayer, currentElevation, currentPalette, currentStyle, scaleRange, nColorBands, logScale);
        
        String baseurl = "http://" + Window.Location.getHost() + Window.Location.getPath() + "?permalinking=true&";

        String urlParams = "dataset=" + wmsUrl +
                           "&numColorBands=" + nColorBands + 
                           "&logScale=" + logScale + 
                           "&zoom=" + zoom +
                           "&centre=" + centre.lon() + "," + centre.lat() ;
                           
//+"&gwt.codesvr=127.0.0.1:9997";
        
        if (currentLayer != null) {
            urlParams += "&layer=" + currentLayer;
        }
        if (currentTime != null) {
            urlParams += "&time=" + currentTime;
        }
        if (currentElevation != null) {
            urlParams += "&elevation=" + currentElevation;
        }
        if (currentPalette != null) {
            urlParams += "&palette=" + currentPalette;
        }
        if (scaleRange != null) {
            urlParams += "&scaleRange=" + scaleRange;
        }
        permalink.setHref(URL.encode(baseurl+urlParams));
        email.setHref("mailto:?subject=MyOcean Data Link&body="+URL.encodeQueryString(baseurl+urlParams));
        
        // Screenshot-only stuff
        urlParams += "&bbox=" + mapArea.getMap().getExtent().toBBox(6);
        if(layerIdToTitle != null)
            urlParams += "&layerName=" + layerIdToTitle.get(currentLayer);
        urlParams += "&datasetName=" + datasetTitle;
        urlParams += "&crs=EPSG:4326";
        urlParams += "&mapHeight="+mapHeight;
        urlParams += "&mapWidth="+mapWidth;
        urlParams += "&style="+currentStyle;
//        urlParams += "&baseUrl="+mapArea.getBaseLayerUrl();
        if(zUnits != null)
            urlParams += "&zUnits="+zUnits;
        if(units != null)
            urlParams += "&units="+units;
        screenshot.setHref("screenshot?"+urlParams);
        
        kmzLink.setHref(mapArea.getKMZUrl());
    }
    
    protected void populateWidgets(LayerDetails layerDetails, WidgetCollection widgetCollection, boolean autoUpdate){
        widgetCollection.getUnitsInfo().setUnits(layerDetails.getUnits());
        
        String extents = layerDetails.getExtents();

        supportedStyles = layerDetails.getSupportedStyles();
        if(supportedStyles.size() > 0){
            currentStyle = supportedStyles.get(0);
        }

        widgetCollection.getElevationSelector().setUnitsAndDirection(layerDetails.getZUnits(), layerDetails.isZPositive());
        // TODO change the null to an elevation (current?  current for this layer?  ????)
        widgetCollection.getElevationSelector().populateVariables(layerDetails.getAvailableZs(), null);

        widgetCollection.getPaletteSelector().populatePalettes(layerDetails.getAvailablePalettes());

        String nearestDate;
        widgetCollection.getTimeSelector().populateDates(layerDetails.getAvailableDates());
        if (layerDetails.getNearestTime() != null) {
//            nearestTime = layerDetails.getNearestTime()error;
            nearestDate = layerDetails.getNearestDate();
            widgetCollection.getTimeSelector().selectDate(nearestDate);
        } else {
            nearestDate = timeSelector.getSelectedDate();
        }
        
        if (!widgetCollection.getPaletteSelector().isLocked()) {
            widgetCollection.getPaletteSelector().setScaleRange(layerDetails.getScaleRange());
            widgetCollection.getPaletteSelector().setNumColorBands(layerDetails.getNumColorBands());
            widgetCollection.getPaletteSelector().setLogScale(layerDetails.isLogScale());
        }
        
        if (autoUpdate) {
            widgetCollection.getPaletteSelector().selectPalette(layerDetails.getSelectedPalette());
        }
    }
    
    private void updateMap() {
        if (layerDetailsLoaded && dateTimeDetailsLoaded && minMaxDetailsLoaded) {
            unitsInfo.setUnits(units);
            if(permalinking) {
                String centre = permalinkParamsMap.get("centre");
                if(centre != null) {
                    String zoom = permalinkParamsMap.get("zoom");
                    if(zoom != null) {
                        mapArea.getMap().setCenter(new LonLat(Double.parseDouble(centre.split(",")[0]),
                                Double.parseDouble(centre.split(",")[1])), Integer.parseInt(zoom));
                    } else {
                        mapArea.getMap().setCenter(new LonLat(Double.parseDouble(centre.split(",")[0]),
                                Double.parseDouble(centre.split(",")[1])));
                    }
                }
                permalinking = false;
            }
            currentStyle = "boxfill";
            if(supportedStyles.size() > 0){
                currentStyle = supportedStyles.get(0);
            }
            mapArea.changeLayer(currentLayer, currentTime, currentElevation, currentStyle, currentPalette, scaleRange, nColorBands,
                    logScale);
            updateLinksEtc();
        }
    }
    
    /**
     * This gets called once the page has loaded. Subclasses should use this in
     * as they would use a constructor.
     */
    public abstract void init();
//    
//    /**
//     * This gets called once the layers have been loaded. Subclasses should use
//     * this to populate layer lists
//     * 
//     * @param menuTree
//     *            a {@link LayerMenuItem} representing the root of the menu tree
//     */
//    public abstract void menuLoaded(LayerMenuItem menuTree);
//
//    /**
//     * This gets called once the layer details have been loaded. Subclasses
//     * should use this to populate appropriate widgets
//     * 
//     * @param menuTree
//     *            a {@link LayerMenuItem} representing the root of the menu tree
//     * @param autoUpdate
//     *            true if the map should be updated once the layer is loaded
//     */
//    public abstract void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate);
//    
//    /**
//     * This gets called once the range has been loaded. Subclasses should use
//     * this to adjust the palette appropriately
//     * 
//     * @param min
//     *            the minimum value of the scale range
//     * @param max
//     *            the maximum value of the scale range
//     */
//    public abstract void rangeLoaded(double min, double max);
//    
//    /**
//     * This is called when an item starts loading.  It can be used to implement loading logos etc
//     */
//    public abstract void loadingStarted();
//    
//    /**
//     * This is called when an item finished loading.  It can be used to implement loading logos etc
//     */
//    public abstract void loadingFinished();
    
    
    
    /*
     * Widgets
     */
    // Selects the variable to display in the WMS layer
    private LayerSelectorCombo layerSelectorCombo;
    // Selects the time and date
    private TimeSelector timeSelector;
    // Selects the elevation/depth
    private ElevationSelector elevationSelector;
    // Selects the palette
    private PaletteSelector paletteSelector;
    // Selects the opacity
    // private OpacitySelector opacitySelector;
    // The current units (for info)
    private UnitsInfo unitsInfo;
    // Animation wizard button
    private AnimationButton anim;
    // The link to the Google Earth KMZ
    protected Anchor kmzLink;
    // Link to the current state
    protected Anchor permalink;
    // Email a link to the current state
    protected Anchor email;
    // Link to a screenshot of the current state
    protected Anchor screenshot;

    /*
     * Essential metadata
     */
    // The title of this dataset (which holds all of the variables)
    private String datasetTitle;
    // Layer ID to title is used to populate the variable list, and for
    // screenshots
    private Map<String, String> layerIdToTitle;

    /*
     * Non-essential metadata
     */
    private String units;
    @SuppressWarnings("unused")
    private String moreInfo;
    @SuppressWarnings("unused")
    private String copyright;

    /*
     * State information
     */
    // The bounds of the data in the current layer
    private String extents;
    // The colour scale range
    private String scaleRange;
    // The currently selected variable
    private String currentLayer;
    // The current style name
    private String currentStyle;
    // The current palette name
    private String currentPalette;
    // The current number of colour bands
    private int nColorBands;
    // Whether we are viewing a logarithmic scale
    private boolean logScale;
    // A list of supported styles (currently only the first one will be used)
    private List<String> supportedStyles;

    // Zoom level and central point for permalink
    private int zoom = 1;
    private LonLat centre = new LonLat(0.0, 0.0);
    private String zUnits = null;



    // A count of how many items we are currently waiting to load.
    // If this is > 0, display a loading indicator. When it reaches zero,
    // remove the indicator

    // We need this because the call to layerDetails (where we receive this
    // time) is separate to
    // the call where we discover what actual times (as opposed to dates) are
    // available
    private String nearestTime;


    public abstract void menuLoaded(LayerMenuItem menuTree);
    public abstract void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate);
    public abstract void loadingStarted();
    public abstract void loadingFinished();

//    {
//        units = layerDetails.getUnits();
//        extents = layerDetails.getExtents();
//
//        supportedStyles = layerDetails.getSupportedStyles();
//        if(supportedStyles.size() > 0){
//            currentStyle = supportedStyles.get(0);
//        }
//
//        zUnits = layerDetails.getZUnits();
//        elevationSelector.setUnitsAndDirection(zUnits, layerDetails.isZPositive());
//        elevationSelector.populateVariables(layerDetails.getAvailableZs(), currentElevation);
//
//        paletteSelector.populatePalettes(layerDetails.getAvailablePalettes());
//
//        String nearestDate;
//        timeSelector.populateDates(layerDetails.getAvailableDates());
//        if (layerDetails.getNearestTime() != null) {
//            nearestTime = layerDetails.getNearestTime();
//            nearestDate = layerDetails.getNearestDate();
//            timeSelector.selectDate(nearestDate);
//        } else {
//            nearestDate = timeSelector.getSelectedDate();
//        }
//
//        if (permalinking) {
//            String scaleRange = permalinkParamsMap.get("scaleRange");
//            if (scaleRange != null) {
//                this.scaleRange = scaleRange;
//                paletteSelector.setScaleRange(scaleRange);
//            }
//
//            String numColorBands = permalinkParamsMap.get("numColorBands");
//            if (numColorBands != null) {
//                this.nColorBands = Integer.parseInt(numColorBands);
//                paletteSelector.setNumColorBands(this.nColorBands);
//            }
//
//            String logScale = permalinkParamsMap.get("logScale");
//            if (logScale != null) {
//                this.logScale = Boolean.parseBoolean(logScale);
//                paletteSelector.setLogScale(this.logScale);
//            }
//
//            String currentElevation = permalinkParamsMap.get("elevation");
//            if (currentElevation != null) {
//                this.currentElevation = currentElevation;
//                elevationSelector.setSelectedElevation(this.currentElevation);
//            }
//
//            String currentPalette = permalinkParamsMap.get("palette");
//            if (currentPalette != null) {
//                this.currentPalette = currentPalette;
//                paletteSelector.selectPalette(currentPalette);
//            }
//        } else {
//            if (!paletteSelector.isLocked()) {
//                scaleRange = layerDetails.getScaleRange();
//                paletteSelector.setScaleRange(scaleRange);
//
//                nColorBands = layerDetails.getNumColorBands();
//                paletteSelector.setNumColorBands(nColorBands);
//
//                logScale = layerDetails.isLogScale();
//                paletteSelector.setLogScale(logScale);
//            }
//            if (autoUpdate) {
//                currentPalette = layerDetails.getSelectedPalette();
//                paletteSelector.selectPalette(currentPalette);
//            }
//            currentElevation = elevationSelector.getSelectedElevation();
//        }
//
//        // Not currently used.
//        // moreInfo = getMoreInfo();
//        // copyright = getCopyright();
//
//        dateSelected(null,nearestDate);
//
//        if (autoUpdate) {
//            try {
//                mapArea.zoomToExtents(extents);
//                // extentsUpdated = false;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            getAutoRange(false);
//        } else {
//            minMaxDetailsLoaded = true;
//        }
//
//        layerDetailsLoaded = true;
//        updateMap();
//    }

    @Override
    public void disableWidgets() {
        layerSelectorCombo.setEnabled(false);
        timeSelector.setEnabled(false);
        elevationSelector.setEnabled(false);
        paletteSelector.setEnabled(false);
        unitsInfo.setEnabled(false);
    }

    @Override
    public void enableWidgets() {
        layerSelectorCombo.setEnabled(true);
        timeSelector.setEnabled(true);
        elevationSelector.setEnabled(true);
        paletteSelector.setEnabled(true);
        unitsInfo.setEnabled(true);
    }

    @Override
    public void dateSelected(String layerId, String selectedDate) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void timeSelected(String layerId, String selectedTime) {
        currentTime = selectedTime;
        nearestTime = null;
        updateMap();
    }

    @Override
    public void onMapMove(MapMoveEvent eventObject) {
        centre = mapArea.getMap().getCenter();
        updateLinksEtc();
    }

    @Override
    public void onMapZoom(MapZoomEvent eventObject) {
        zoom = mapArea.getMap().getZoom();
        updateLinksEtc();
    }

    @Override
    public void layerSelected(String layerName) {
        requestLayerDetails(layerName, currentElevation, currentTime, true);
        updateMap();
    }

    @Override
    public void elevationSelected(String layerId, String elevation) {
        currentElevation = elevation;
        updateMap();
    }

    @Override
    public void paletteChanged(String layerId, String paletteName, int nColorBands) {
        currentPalette = paletteName;
        this.nColorBands = nColorBands;
        updateMap();
    }

    @Override
    public void scaleRangeChanged(String layerId, String scaleRange) {
        this.scaleRange = scaleRange;
        updateMap();
    }

    @Override
    public void logScaleChanged(String layerId, boolean newIsLogScale) {
        logScale = newIsLogScale;
        updateMap();
    }

    @Override
    public void autoAdjustPalette(String layerId) {
        requestAutoRange(layerId, currentElevation, currentTime, true);
    }

    @Override
    public void refreshLayerList() {
        requestMenu();
    }

    public void rangeLoaded(double min, double max) {
        // TODO Auto-generated method stub
        
    }
}
