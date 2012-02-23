package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gwtopenmaps.openlayers.client.LonLat;
import org.gwtopenmaps.openlayers.client.OpenLayers;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.ElevationSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.GodivaActionsHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.PaletteSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ConnectionException;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ErrorHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerDetails;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestCallback;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestCallback;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.AnimationButton;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MapArea;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;
import uk.ac.rdg.resc.ncwms.gwt.shared.CaseInsensitiveParameterMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
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
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */

public class Godiva implements EntryPoint, ErrorHandler, LayerSelectionHandler, ElevationSelectionHandler, PaletteSelectionHandler, TimeDateSelectionHandler, GodivaActionsHandler  {

    private String baseUrl;

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
//    private OpacitySelector opacitySelector;
    // The main map widget
    private MapArea mapArea;
    // The current units (for info)
    private UnitsInfo unitsInfo;
    // Animation wizard button
    private AnimationButton anim;
    // The link to the Google Earth KMZ
    private Anchor kmzLink;
    // Link to the current state
    private Anchor permalink;
    // Email a link to the current state
    private Anchor email;
    // Link to a screenshot of the current state
    private Anchor screenshot;
    // The logo
    private Image logo;
    // The loading indicator
    private Image loadingImage;
    
    /*
     * Essential metadata
     */
    // The title of this dataset (which holds all of the variables)
    private String datasetTitle;
    // Layer ID to title is used to populate the variable list, and for screenshots
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
    // The current time
    private String currentTime;
    // The currently selected variable
    private String currentLayer;
    // The current elevation/depth
    private String currentElevation;
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
    // Whether or not the current call to the service contains all of the state
    // information in the url or not (for permalinking)
    private boolean permalinking;
    // Zoom level and central point for permalink
    private int zoom = 1;
    private LonLat centre = new LonLat(0.0, 0.0);
    private String zUnits = null;
    
    /*
     * These 3 booleans are used so that we only update the map when
     * all required data have been loaded
     */
    private boolean layerDetailsLoaded;
    private boolean dateTimeDetailsLoaded;
    private boolean minMaxDetailsLoaded;

    // A count of how many items we are currently waiting to load.
    // If this is > 0, display a loading indicator.  When it reaches zero,
    // remove the indicator
    private int loadingCount;
    
    // We need this because the call to layerDetails (where we receive this time) is separate to
    // the call where we discover what actual times (as opposed to dates) are available
    private String nearestTime;
    
    private int mapHeight;
    private int mapWidth;
    private String proxyUrl;
    private String docHref;

    private CaseInsensitiveParameterMap permalinkParamsMap;

    /**
     * This is the entry point method.
     */
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
                    init();
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
//        initWithDefaults();
    }
    
    private void initWithDefaults(){
        mapHeight = 400;
        mapWidth = 512;
        proxyUrl = "proxy?";
        proxyUrl = "";
        docHref = "http://www.resc.rdg.ac.uk/trac/ncWMS/wiki/GodivaTwoUserGuide";
        init();
    }
     
    private void init() {
        loadingCount = 0;
        OpenLayers.setProxyHost(proxyUrl);
        
        baseUrl = "wms";//Window.Location.getHost()+"/wms";
        if (baseUrl == null) {
            String message = "<h1>No dataset specified</h1>"+
                             "You must specify the target dataset in the url, using the HTTP parameter \"dataset\"<br><br>"+
                             "Either modify the url, or enter the target dataset in the text box below and hit \"Go\"";
            HTML errorMessage = new HTML();
            Window.setTitle("Error");
            errorMessage.setHTML(message);
            RootLayoutPanel mainWindow = RootLayoutPanel.get();
            for (int i = 0; i < mainWindow.getWidgetCount(); i++) {
                mainWindow.remove(0);
            }
            final TextBox urlEnter = new TextBox();
            urlEnter.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    Window.Location.replace(Window.Location.getPath()+"?dataset="+urlEnter.getValue());
                }
            });
            Button goButton = new Button("Go");
            goButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Window.Location.replace(Window.Location.getPath()+"?dataset="+urlEnter.getValue());
                }
            });
            
            VerticalPanel errorPanel = new VerticalPanel();
            errorPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            errorPanel.add(errorMessage);
            urlEnter.setWidth("100%");
            errorPanel.add(urlEnter);
            errorPanel.add(goButton);
            errorPanel.setCellHeight(errorMessage, "10%");
            errorPanel.setCellHeight(urlEnter, "10%");
            errorPanel.setCellVerticalAlignment(urlEnter, HasVerticalAlignment.ALIGN_MIDDLE);
            errorPanel.setCellHeight(goButton, "90%");
            errorPanel.setCellVerticalAlignment(goButton, HasVerticalAlignment.ALIGN_TOP);
            mainWindow.add(errorPanel);
            return;
        } else {
            String permalinkString = Window.Location.getParameter("permalinking");
            if(permalinkString != null && Boolean.parseBoolean(permalinkString)){
                permalinking = true;
                permalinkParamsMap = CaseInsensitiveParameterMap.getMapFromList(Window.Location.getParameterMap());
            }
        }

        layerSelectorCombo = new LayerSelectorCombo(this);

        elevationSelector = new ElevationSelector("mainLayer", "Depth", this);
        timeSelector = new TimeSelector("mainLayer", this);
        paletteSelector = new PaletteSelector("mainLayer", mapHeight, this, baseUrl);
        unitsInfo = new UnitsInfo();
        
        
        Anchor docLink = new Anchor("Documentation", docHref);
        docLink.setTarget("_blank");
        docLink.setTitle("Open documentation in a new window");

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
        
        logo = new Image("img/resc_logo.png");

        loadingImage = new Image("img/loading.gif");
        loadingImage.setVisible(false);
        loadingImage.setStylePrimaryName("loadingImage");
        
        mapArea = new MapArea(baseUrl, mapWidth, mapHeight, this);
        
        anim = new AnimationButton(mapArea, proxyUrl+baseUrl);
        
        RootLayoutPanel mainWindow = RootLayoutPanel.get();
        
        
        mainWindow.add(LayoutManager.getGodivaLayout(layerSelectorCombo,
                                                              unitsInfo, 
                                                              timeSelector, 
                                                              elevationSelector, 
                                                              paletteSelector,
                                                              anim,
                                                              kmzLink, 
                                                              permalink,
                                                              email,
                                                              docLink,
                                                              screenshot,
                                                              logo, 
                                                              mapArea,
                                                              loadingImage));
        
        timeSelector.setEnabled(false);
        elevationSelector.setEnabled(false);
        paletteSelector.setEnabled(false);
        unitsInfo.setEnabled(false);

        populateLayerInfo();
    }

    private void populateLayerInfo() {
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
                    layerSelectorCombo.populateLayers(parentObj);
                    String dataProductTitle = parentObj.get("label").isString().stringValue();

                    JSONArray childArray = parentObj.get("children").isArray();
                    JSONObject childObj = childArray.get(0).isObject();
                    datasetTitle = childObj.get("label").isString().stringValue();

                    JSONArray variableArray = childObj.get("children").isArray();
                    if (variableArray.size() == 0) {
                        String message = "<h2>No Georeferencing Data Found</h2><br>"
                                + "No georeferencing data could be found in the dataset \"" + datasetTitle + "\"";
                        handleCatastrophicError(message);
                        return;
                    }

                    layerIdToTitle = new LinkedHashMap<String, String>();
                    for (int i = 0; i < variableArray.size(); i++) {
                        JSONObject variable = variableArray.get(i).isObject();
                        layerIdToTitle.put(variable.get("id").isString().stringValue(), variable.get("label")
                                .isString().stringValue());
                    }

                    Window.setTitle(dataProductTitle);
                    if (!permalinking) {
                        dealWithLayerSelection(layerSelectorCombo.getSelectedId(), true);
                    } else {
                        System.out.println("Permalink");
                        String currentLayer = permalinkParamsMap.get("layer");
                        if (currentLayer != null) {
                            layerSelectorCombo.setSelectedLayer(currentLayer);
                            dealWithLayerSelection(currentLayer, false);
                        }
                    }
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

    public final String getUrl(String url) {
        return URL.encode(proxyUrl + baseUrl + url);
    }

    private void dealWithLayerSelection(String selectedId, final boolean autoUpdate) {
        if (selectedId == null) {
            // We have no variables defined in the selected layer
            // Return here. We are already dealing with the case where there are
            // no layers present
            return;
        }
        layerDetailsLoaded = false;
        dateTimeDetailsLoaded = false;
        minMaxDetailsLoaded = false;
        
        if(permalinking){
            currentTime = permalinkParamsMap.get("time");
        }
        
        this.currentLayer = selectedId;
        
        LayerRequestBuilder getLayerDetailsRequest = new LayerRequestBuilder(selectedId, proxyUrl+baseUrl, currentTime);
        
        getLayerDetailsRequest.setCallback(new LayerRequestCallback(selectedId, this) {
            @Override
            public void onResponseReceived(Request req, Response response) {
                try {
                    super.onResponseReceived(req, response);
                    if(response.getStatusCode() != Response.SC_OK){
                        throw new ConnectionException("Error contacting server");
                    }

                    LayerDetails layerDetails = getLayerDetails();
                    
                    units = layerDetails.getUnits();
                    extents = layerDetails.getExtents();

                    supportedStyles = layerDetails.getSupportedStyles();
                    if(supportedStyles.size() > 0){
                        currentStyle = supportedStyles.get(0);
                    }

                    zUnits = layerDetails.getZUnits();
                    elevationSelector.setUnitsAndDirection(zUnits, layerDetails.isZPositive());
                    elevationSelector.populateVariables(layerDetails.getAvailableZs(), currentElevation);

                    paletteSelector.populatePalettes(layerDetails.getAvailablePalettes());

                    String nearestDate;
                    timeSelector.populateDates(layerDetails.getAvailableDates());
                    if (layerDetails.getNearestTime() != null) {
                        nearestTime = layerDetails.getNearestTime();
                        nearestDate = layerDetails.getNearestDate();
                        timeSelector.selectDate(nearestDate);
                    } else {
                        nearestDate = timeSelector.getSelectedDate();
                    }

                    if (permalinking) {
                        String scaleRange = permalinkParamsMap.get("scaleRange");
                        if (scaleRange != null) {
                            Godiva.this.scaleRange = scaleRange;
                            paletteSelector.setScaleRange(scaleRange);
                        }

                        String numColorBands = permalinkParamsMap.get("numColorBands");
                        if (numColorBands != null) {
                            Godiva.this.nColorBands = Integer.parseInt(numColorBands);
                            paletteSelector.setNumColorBands(Godiva.this.nColorBands);
                        }

                        String logScale = permalinkParamsMap.get("logScale");
                        if (logScale != null) {
                            Godiva.this.logScale = Boolean.parseBoolean(logScale);
                            paletteSelector.setLogScale(Godiva.this.logScale);
                        }

                        String currentElevation = permalinkParamsMap.get("elevation");
                        if (currentElevation != null) {
                            Godiva.this.currentElevation = currentElevation;
                            elevationSelector.setSelectedElevation(Godiva.this.currentElevation);
                        }

                        String currentPalette = permalinkParamsMap.get("palette");
                        if (currentPalette != null) {
                            Godiva.this.currentPalette = currentPalette;
                            paletteSelector.selectPalette(currentPalette);
                        }
                    } else {
                        if (!paletteSelector.isLocked()) {
                            scaleRange = layerDetails.getScaleRange();
                            paletteSelector.setScaleRange(scaleRange);

                            nColorBands = layerDetails.getNumColorBands();
                            paletteSelector.setNumColorBands(nColorBands);

                            logScale = layerDetails.isLogScale();
                            paletteSelector.setLogScale(logScale);
                        }
                        if (autoUpdate) {
                            currentPalette = layerDetails.getSelectedPalette();
                            paletteSelector.selectPalette(currentPalette);
                        }
                        currentElevation = elevationSelector.getSelectedElevation();
                    }

                    // Not currently used.
                    // moreInfo = getMoreInfo();
                    // copyright = getCopyright();

                    dateSelected(null,nearestDate);

                    if (autoUpdate) {
                        try {
                            mapArea.zoomToExtents(extents);
                            // extentsUpdated = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        getAutoRange(false);
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
                layerDetailsLoaded = true;
                updateMap();
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

    private void getAutoRange(boolean force) {
        minMaxDetailsLoaded = false;
        if(paletteSelector.isLocked()){
            minMaxDetailsLoaded = true;
            return;
        }
        String[] scaleRangeSplit = scaleRange.split(",");
        if (!force && (Double.parseDouble(scaleRangeSplit[0]) != -50 || Double.parseDouble(scaleRangeSplit[1]) != 50)) {
            minMaxDetailsLoaded = true;
            return;
        }
        
        /*
         * We use 1.1.1 here, because if getMap().getProjection() returns EPSG:4326, 
         * getMap().getExtent().toBBox(4) will still return in long-lat co-ords
         */
        String url = "?request=GetMetadata" 
                    + "&item=minmax" 
                    + "&layers=" + currentLayer 
                    + "&srs=" + mapArea.getMap().getProjection() 
                    + "&height=50&width=50"
                    + "&version=1.1.1";
        url += "&bbox=" + mapArea.getMap().getExtent().toBBox(4);
        if (currentElevation != null)
            url = url + "&elevation=" + currentElevation;
        if (currentTime != null)
            url = url + "&time=" + currentTime;

        RequestBuilder getMinMaxRequest = new RequestBuilder(RequestBuilder.GET, getUrl(url));
        getMinMaxRequest.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request req, Response response) {
                if(response.getText() != null && !response.getText().isEmpty()){
                    try{
                        JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                        JSONObject parentObj = jsonMap.isObject();
                        double min = parentObj.get("min").isNumber().doubleValue();
                        double max = parentObj.get("max").isNumber().doubleValue();
                        String scaleRange = min + "," + max;
                        if(paletteSelector.setScaleRange(scaleRange)){
                            Godiva.this.scaleRange = scaleRange;
                            updateMap();
                        }
                    } catch (Exception e){
                        updateMap();
                    }
                }
                minMaxDetailsLoaded = true;
                setLoading(false);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                // We have failed, but we still want to update the map
                System.out.println("Range finding failed");
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

    @Override
    public void dateSelected(String layerId, String selectedDate) {
        if(selectedDate == null){
            dateTimeDetailsLoaded = true;
            updateMap();
            return;
        }
        dateTimeDetailsLoaded = false;
        TimeRequestBuilder getTimeRequest = new TimeRequestBuilder(currentLayer, selectedDate, proxyUrl+baseUrl);
        getTimeRequest.setCallback(new TimeRequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                try{
                    super.onResponseReceived(request, response);
                    if(response.getStatusCode() != Response.SC_OK){
                        throw new ConnectionException("Error contacting server");
                    }
                    timeSelector.populateTimes(getAvailableTimesteps());
                    if (nearestTime != null){
                        timeSelector.selectTime(nearestTime); 
                    }
                    timeSelected(null, timeSelector.getSelectedDateTime());
                    dateTimeDetailsLoaded = true;
                    updateMap();
                } catch (Exception e){
                    invalidJson(e);
                } finally {
                    setLoading(false);
                }
            }
            
            @Override
            public void onError(Request request, Throwable exception) {
                setLoading(false);
                dateTimeDetailsLoaded = true;
                updateMap();
                handleError(exception);
            }
        });
        
        try {
            setLoading(true);
            getTimeRequest.send();
        } catch (RequestException e) {
            handleError(e);
        }
    }
    
    @Override
    public void timeSelected(String layerId, String selectedTime) {
        currentTime = selectedTime;
        nearestTime = null;
        updateMap();
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
    
    private void updateLinksEtc() {
        anim.updateDetails(currentLayer, currentElevation, currentPalette, currentStyle, scaleRange, nColorBands, logScale);
        
        String baseurl = "http://" + Window.Location.getHost() + Window.Location.getPath() + "?permalinking=true&";

        String urlParams = "dataset=" + baseUrl +
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

    @Override
    public void setLoading(boolean loading){
        if(loading){
            loadingCount++;
            if(loadingCount == 1)
                loadingImage.setVisible(true);
        } else {
            loadingCount--;
            if(loadingCount == 0)
                loadingImage.setVisible(false);
        }
    }

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
    
    private void invalidJson(Exception e){
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
    
    private void handleCatastrophicError(String message){
        HTML errorMessage = new HTML();
        Window.setTitle("Unrecoverable Error");
        errorMessage.setHTML(message);
        RootLayoutPanel mainWindow = RootLayoutPanel.get();
        for (int i = 0; i < mainWindow.getWidgetCount(); i++) {
            mainWindow.remove(0);
        }
        mainWindow.add(errorMessage);
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
        dealWithLayerSelection(layerName, true);
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
        getAutoRange(true);
    }

    @Override
    public void refreshLayerList() {
        populateLayerInfo();
    }
}
