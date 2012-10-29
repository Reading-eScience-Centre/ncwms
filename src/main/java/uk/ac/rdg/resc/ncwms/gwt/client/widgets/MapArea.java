package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.gwtopenmaps.openlayers.client.Bounds;
import org.gwtopenmaps.openlayers.client.LonLat;
import org.gwtopenmaps.openlayers.client.Map;
import org.gwtopenmaps.openlayers.client.MapOptions;
import org.gwtopenmaps.openlayers.client.MapWidget;
import org.gwtopenmaps.openlayers.client.Pixel;
import org.gwtopenmaps.openlayers.client.Projection;
import org.gwtopenmaps.openlayers.client.control.EditingToolbar;
import org.gwtopenmaps.openlayers.client.control.LayerSwitcher;
import org.gwtopenmaps.openlayers.client.control.MousePosition;
import org.gwtopenmaps.openlayers.client.control.WMSGetFeatureInfo;
import org.gwtopenmaps.openlayers.client.control.WMSGetFeatureInfoOptions;
import org.gwtopenmaps.openlayers.client.event.EventHandler;
import org.gwtopenmaps.openlayers.client.event.EventObject;
import org.gwtopenmaps.openlayers.client.event.GetFeatureInfoListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadCancelListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadEndListener;
import org.gwtopenmaps.openlayers.client.event.LayerLoadStartListener;
import org.gwtopenmaps.openlayers.client.event.MapBaseLayerChangedListener;
import org.gwtopenmaps.openlayers.client.feature.VectorFeature;
import org.gwtopenmaps.openlayers.client.geometry.LineString;
import org.gwtopenmaps.openlayers.client.geometry.Point;
import org.gwtopenmaps.openlayers.client.layer.Image;
import org.gwtopenmaps.openlayers.client.layer.ImageOptions;
import org.gwtopenmaps.openlayers.client.layer.TransitionEffect;
import org.gwtopenmaps.openlayers.client.layer.Vector;
import org.gwtopenmaps.openlayers.client.layer.WMS;
import org.gwtopenmaps.openlayers.client.layer.WMSOptions;
import org.gwtopenmaps.openlayers.client.layer.WMSParams;
import org.gwtopenmaps.openlayers.client.util.JSObject;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.GodivaActionsHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.StartEndTimeHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

public class MapArea extends MapWidget {

    private static final Projection EPSG4326 = new Projection("EPSG:4326");
    private static final NumberFormat FORMATTER = NumberFormat.getFormat("###.#####"); 

    private final class WmsDetails {
        private final WMS wms;
        private final WMSParams params;
        private final boolean multipleElevations;
        private final boolean multipleTimes;

        public WmsDetails(WMS wms, WMSParams wmsParameters, boolean multipleElevations,
                boolean multipleTimes) {
            if (wms == null || wmsParameters == null)
                throw new IllegalArgumentException("Cannot provide null parameters");
            this.wms = wms;
            this.params = wmsParameters;
            this.multipleElevations = multipleElevations;
            this.multipleTimes = multipleTimes;
        }
    }

    private Map map;
    private java.util.Map<String, WmsDetails> wmsLayers;
    private Image animLayer;
    private String currentProjection;
    private String baseUrl;

    private String transectLayer = null;

    private WMSOptions wmsPolarOptions;
    private WMSOptions wmsStandardOptions;

    private LayerLoadStartListener loadStartListener;
    private LayerLoadCancelListener loadCancelListener;
    private LayerLoadEndListener loadEndListener;

    private GodivaActionsHandler widgetDisabler;

    private String baseUrlForExport;
    private String layersForExport;

    private WMSGetFeatureInfo getFeatureInfo;
    private EditingToolbar editingToolbar;

    public MapArea(String baseUrl, int width, int height, final GodivaActionsHandler godivaListener) {
        super(width + "px", height + "px", getDefaultMapOptions());

        wmsLayers = new LinkedHashMap<String, WmsDetails>();

        this.baseUrl = baseUrl;
        loadStartListener = new LayerLoadStartListener() {
            @Override
            public void onLoadStart(LoadStartEvent eventObject) {
                godivaListener.setLoading(true);
            }
        };
        loadCancelListener = new LayerLoadCancelListener() {
            @Override
            public void onLoadCancel(LoadCancelEvent eventObject) {
                godivaListener.setLoading(false);
            }
        };
        loadEndListener = new LayerLoadEndListener() {
            @Override
            public void onLoadEnd(LoadEndEvent eventObject) {
                godivaListener.setLoading(false);
            }
        };
        this.widgetDisabler = godivaListener;
        init();
        map.addMapMoveListener(godivaListener);
        map.addMapZoomListener(godivaListener);

        wmsStandardOptions = new WMSOptions();
        wmsStandardOptions.setWrapDateLine(true);
    }

    public void addAnimationLayer(String layerId, String timeList, String currentElevation,
            String palette, String style, String scaleRange, int nColorBands, boolean logScale, String frameRate) {
        StringBuilder url = new StringBuilder(baseUrl + "?service=WMS&request=GetMap&version=1.1.1");
        url.append("&format=image/gif" + "&transparent=true" + "&styles=" + style + "/" + palette
                + "&layers=" + layerId + "&time=" + timeList + "&logscale=" + logScale + "&srs="
                + currentProjection + "&bbox=" + map.getExtent().toBBox(6) + "&width="
                + ((int) map.getSize().getWidth()) + "&height=" + ((int) map.getSize().getHeight())
                + "&animation=true");
        if (scaleRange != null)
            url.append("&colorscalerange=" + scaleRange);
        if (currentElevation != null)
            url.append("&elevation=" + currentElevation.toString());
        if (nColorBands > 0)
            url.append("&numcolorbands=" + nColorBands);
        if (frameRate != null)
            url.append("&frameRate=" + frameRate);
        ImageOptions opts = new ImageOptions();
        opts.setAlwaysInRange(true);
        animLayer = new Image("Animation Layer", url.toString(), map.getExtent(), map.getSize(),
                opts);
        animLayer.addLayerLoadStartListener(loadStartListener);
        animLayer.addLayerLoadCancelListener(new LayerLoadCancelListener() {
            @Override
            public void onLoadCancel(LoadCancelEvent eventObject) {
                stopAnimation();
                loadCancelListener.onLoadCancel(eventObject);
            }
        });
        animLayer.addLayerLoadEndListener(loadEndListener);
        animLayer.setIsBaseLayer(false);
        animLayer.setDisplayInLayerSwitcher(false);
        
        /*
         * Out of all visible layers, we choose the most transparent and set the
         * animation layer transparency to that.
         */
        float opacity = 1.0f;
        for(WmsDetails wmsDetails : wmsLayers.values()){
            float currentOpacity = wmsDetails.wms.getOpacity();
            wmsDetails.wms.setIsVisible(false);
            if(currentOpacity < opacity)
                opacity = currentOpacity;
        }
        animLayer.setOpacity(opacity);
        
        map.addLayer(animLayer);
        widgetDisabler.disableWidgets();
    }

    public void stopAnimation() {
        // This stops and removes the animation. We may want a pause method...
        widgetDisabler.enableWidgets();
        if (animLayer != null) {
            map.removeLayer(animLayer);
            animLayer = null;
        }
        for(WmsDetails wmsDetails : wmsLayers.values()){
            wmsDetails.wms.setIsVisible(true);
        }
    }

    public void addLayer(String internalLayerId, String wmsLayerName, String time,
            String colorbyTime, String elevation, String colorbyElevation, String style,
            String palette, String scaleRange, int nColourBands, boolean logScale,
            boolean multipleElevations, boolean multipleTimes) {
//        JSObject vendorParams = JSObject.createJSObject();

        WMSParams params = new WMSParams();
        params.setFormat("image/png");
        params.setTransparent(true);
        params.setStyles(style + "/" + palette);
        params.setLayers(wmsLayerName);
        if (time != null) {
            params.setParameter("TIME", time);
        }
        if(colorbyTime != null){
            params.setParameter("COLORBY/TIME", colorbyTime);
        }
        if (elevation != null) {
            params.setParameter("ELEVATION", elevation);
        }
        if (colorbyElevation != null) {
            params.setParameter("COLORBY/DEPTH", colorbyElevation);
        }
        if (scaleRange != null)
            params.setParameter("COLORSCALERANGE", scaleRange);
        if (nColourBands > 0)
            params.setParameter("NUMCOLORBANDS", nColourBands + "");
        params.setParameter("LOGSCALE", logScale + "");

        WMSOptions options = getOptionsForCurrentProjection();
        
        if(wmsLayerName.endsWith("*")){
            /*
             * We are querying a parent layer. We don't want to add links for
             * profile/timeseries plots
             */
            multipleElevations = false;
            multipleTimes = false;
        }   

        doAddingOfLayer(internalLayerId, params, options, multipleElevations, multipleTimes);
    }

    private void doAddingOfLayer(String internalLayerId, WMSParams params, WMSOptions options,
            boolean multipleElevations, boolean multipleTimes) {
        WmsDetails wmsAndParams = wmsLayers.get(internalLayerId);
        WMS wmsLayer;
        if (wmsAndParams == null) {
            wmsLayer = new WMS("WMS Layer", baseUrl, params, options);
            wmsLayer.addLayerLoadStartListener(loadStartListener);
            wmsLayer.addLayerLoadCancelListener(loadCancelListener);
            wmsLayer.addLayerLoadEndListener(loadEndListener);
            map.addLayer(wmsLayer);
        } else {
            wmsLayer = wmsLayers.get(internalLayerId).wms;
            wmsLayer.getParams().setParameter("ELEVATION", "");
            wmsLayer.getParams().setParameter("TIME", "");
            wmsLayer.mergeNewParams(params);
            wmsLayer.addOptions(options);
            wmsLayer.redraw();
        }
        WmsDetails newWmsAndParams = new WmsDetails(wmsLayer, params, multipleElevations,
                multipleTimes);
        wmsLayers.put(internalLayerId, newWmsAndParams);
        setGetFeatureInfoDetails(multipleElevations, multipleTimes, internalLayerId);
        if (animLayer != null)
            animLayer.setIsVisible(false);
    }

    public void removeLayer(String layerId) {
        if (wmsLayers.containsKey(layerId)) {
            map.removeLayer(wmsLayers.get(layerId).wms);
            wmsLayers.remove(layerId);
        }
    }

    private void setGetFeatureInfoDetails(final boolean multipleElevations,
            final boolean multipleTimes, final String layerId) {
        WMSGetFeatureInfoOptions getFeatureInfoOptions = new WMSGetFeatureInfoOptions();
        getFeatureInfoOptions.setQueryVisible(true);
        getFeatureInfoOptions.setInfoFormat("text/xml");
        getFeatureInfoOptions.setMaxFeaturess(5);
        
        WMS[] layers = new WMS[wmsLayers.size()];
        Iterator<WmsDetails> it = wmsLayers.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            layers[i] = it.next().wms;
            i++;
        }
        getFeatureInfoOptions.setLayers(layers);

        JSObject vendorParams = JSObject.createJSObject();
        final String timeStr = wmsLayers.get(layerId).params.getJSObject().getPropertyAsString("TIME");
        if(timeStr != null){
            vendorParams.setProperty("TIME", timeStr);
        }
        final String colorbyTimeStr = wmsLayers.get(layerId).params.getJSObject().getPropertyAsString("COLORBY/TIME");
        if(colorbyTimeStr != null){
            vendorParams.setProperty("COLORBY/TIME", colorbyTimeStr);
        }
        final String elevationStr = wmsLayers.get(layerId).params.getJSObject().getPropertyAsString("ELEVATION");
        if(elevationStr != null){
            vendorParams.setProperty("ELEVATION", elevationStr);
        }
        final String colorbyElevationStr = wmsLayers.get(layerId).params.getJSObject().getPropertyAsString("COLORBY/DEPTH");
        if(colorbyElevationStr != null){
            vendorParams.setProperty("COLORBY/DEPTH", colorbyElevationStr);
        }
        
        if (getFeatureInfo != null) {
            getFeatureInfo.deactivate();
            map.removeControl(getFeatureInfo);
            getFeatureInfo = null;
        }
        getFeatureInfo = new WMSGetFeatureInfo(getFeatureInfoOptions);

        getFeatureInfo.addGetFeatureListener(new GetFeatureInfoListener() {
            @Override
            public void onGetFeatureInfo(GetFeatureInfoEvent eventObject) {
                String pixels[] = eventObject.getJSObject().getProperty("xy").toString().split(",");
                final LonLat lonLat = MapArea.this.map.getLonLatFromPixel(new Pixel(Integer
                        .parseInt(pixels[0].substring(2)), Integer.parseInt(pixels[1].substring(2))));
                FeatureInfoMessageAndFeatureIds featureInfo = processFeatureInfo(eventObject.getText());
                
                String message = featureInfo.message;

                int x = Integer.parseInt(pixels[0].substring(2)) + MapArea.this.getAbsoluteLeft();
                int y = Integer.parseInt(pixels[1].substring(2)) + MapArea.this.getAbsoluteTop();
                final DialogBox pop = new DialogBoxWithCloseButton();

                pop.setHTML("Feature Info");

                VerticalPanel panel = new VerticalPanel();

                HTML html = new HTML("<div class=\"getFeatureInfo\">" + message + "</div>");
                panel.add(html);

                StringBuilder layerNames = new StringBuilder();
                for(String layerName : featureInfo.featureIds){
                    layerNames.append(layerName+",");
                }
                // Remove the final comma
                layerNames.deleteCharAt(layerNames.length()-1);
                final String layer = layerNames.toString();
                
                if (multipleElevations) {
                    final String link = baseUrl + "?REQUEST=GetVerticalProfile" + "&LAYER=" + layer
                            + "&CRS=CRS:84" + ((timeStr != null) ? ("&TIME=" + timeStr) : "")
                            + "&POINT=" + lonLat.lon() + "%20" + lonLat.lat() + "&FORMAT=image/png";
                    Anchor profilePlot = new Anchor("Vertical Profile Plot");
                    profilePlot.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            displayImagePopup(link, "Vertical Profile");
                            pop.hide();
                        }
                    });
                    panel.add(profilePlot);
                }

                if (multipleTimes) {
                    Anchor timeseriesPlot = new Anchor("Time Series Plot");
                    timeseriesPlot.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            String wmsLayer = wmsLayers.get(layerId).wms.getParams().getLayers().split(",")[0];
                            final StartEndTimePopup timeSelector = new StartEndTimePopup(wmsLayer,
                                    baseUrl, null);
                            timeSelector.setButtonLabel("Plot");
                            timeSelector
                                    .setErrorMessage("You can only plot a time series when you have multiple times available");
                            timeSelector.setHTML("Select range for time series");
                            timeSelector.setTimeSelectionHandler(new StartEndTimeHandler() {
                                @Override
                                public void timesReceived(String startDateTime, String endDateTime) {
                                    String eS = elevationStr;
                                    if(colorbyElevationStr != null){
                                        eS = colorbyElevationStr;
                                    }
                                    String link = baseUrl
                                            + "?REQUEST=GetTimeseries"
                                            + "&LAYER="
                                            + layer
                                            + "&CRS=CRS:84"
                                            + "&TIME="
                                            + startDateTime
                                            + "/"
                                            + endDateTime
                                            + "&POINT="
                                            + lonLat.lon()
                                            + "%20"
                                            + lonLat.lat()
                                            + "&FORMAT=image/png"
                                            + (eS == null ? "" : "&ELEVATION=" + eS);
                                    displayImagePopup(link, "Time series");
                                    timeSelector.hide();
                                }
                            });
                            pop.hide();
                            timeSelector.center();
                            timeSelector.show();
                        }
                    });
                    panel.add(timeseriesPlot);
                }
                pop.add(panel);
                pop.setPopupPosition(x, y);
                pop.setAutoHideEnabled(true);
                pop.show();
            }
        });
        getFeatureInfo.setAutoActivate(true);
        map.addControl(getFeatureInfo);

        getFeatureInfo.getJSObject().setProperty("vendorParams", vendorParams);
    }

    private void displayImagePopup(String url, String title) {
        final DialogBoxWithCloseButton popup = new DialogBoxWithCloseButton();
        final com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(url);
        image.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent event) {
                popup.center();
            }
        });
        /*
         * TODO this doesn't seem to appear on Chromium...
         */
        image.setAltText("Image loading...");
        if(title != null){
            popup.setHTML(title);
        }
        popup.add(image);
        popup.center();
    }
    
    public void zoomToExtents(String extents) throws Exception {
        if (currentProjection.equalsIgnoreCase("EPSG:32661")
                || currentProjection.equalsIgnoreCase("EPSG:32761")) {
            /*
             * If we have a polar projection, the extents will be wrong. In this
             * case, just ignore the zoom to extents.
             * 
             * This is acceptable behaviour, since if we are looking at polar
             * data, we never want to zoom to the extents of the data
             */
            return;
        }
        try {
            String[] bboxStr = extents.split(",");
            double lowerLeftX = Double.parseDouble(bboxStr[0]);
            double lowerLeftY = Double.parseDouble(bboxStr[1]);
            double upperRightX = Double.parseDouble(bboxStr[2]);
            double upperRightY = Double.parseDouble(bboxStr[3]);
            map.zoomToExtent(new Bounds(lowerLeftX, lowerLeftY, upperRightX, upperRightY));
        } catch (Exception e) {
            throw e;
        }
    }

    public String getKMZUrl() {
        
        String url;
        WmsDetails wmsAndParams = wmsLayers.get(getTransectLayerId());
        if (wmsAndParams != null) {
            if(animLayer != null){
                url = animLayer.getUrl();
                url = url.replaceAll("image/gif", "application/vnd.google-earth.kmz");
                url = url.replaceAll("image%2Fgif", "application%2Fvnd.google-earth.kmz");
            } else {
                url = wmsAndParams.wms.getFullRequestString(wmsAndParams.params, null);
                url = url + "&height=" + (int) map.getSize().getHeight() + "&width="
                        + (int) map.getSize().getWidth() + "&bbox=" + map.getExtent().toBBox(6);
                url = url.replaceAll("image/png", "application/vnd.google-earth.kmz");
                url = url.replaceAll("image%2Fpng", "application%2Fvnd.google-earth.kmz");
            }
        } else {
            url = null;
        }
        return url;
    }

    private static MapOptions getDefaultMapOptions() {
        MapOptions mapOptions = new MapOptions();
        mapOptions.setProjection("EPSG:4326");
        mapOptions.setDisplayProjection(EPSG4326);
        return mapOptions;
    }

    private void init() {
        this.setStylePrimaryName("mapStyle");
        map = this.getMap();

        addBaseLayers();

        currentProjection = map.getProjection();
        map.addControl(new LayerSwitcher());
        addDrawingLayer();
        map.addControl(new MousePosition());
        // map.addControl(new Graticule());
        
        map.setCenter(new LonLat(0.0, 0.0), 2);
        map.setMaxExtent(new Bounds(-180, -360, 180, 360));
    }

    public void setOpacity(String layerId, float opacity) {
        if (wmsLayers.containsKey(layerId)) {
            wmsLayers.get(layerId).wms.setOpacity(opacity);
        }
        if(animLayer != null){
            animLayer.setOpacity(opacity);
        }
    }

    public String getBaseLayerUrl() {
        return baseUrlForExport;
    }
    
    public String getBaseLayerLayers() {
        return layersForExport;
    }

    private void addBaseLayers() {
        WMS openLayers;
        WMS bluemarbleDemis;
        WMS demis;
        WMS plurel;
        WMS weather;
        WMS srtmDem;
        WMS northPole;
        WMS southPole;

        WMSParams wmsParams;
        WMSOptions wmsOptions;
        wmsOptions = new WMSOptions();
        wmsOptions.setWrapDateLine(true);
        wmsParams = new WMSParams();
        wmsParams.setLayers("basic");
        openLayers = new WMS("OpenLayers WMS", "http://labs.metacarta.com/wms-c/Basic.py?",
                wmsParams, wmsOptions);
        openLayers.addLayerLoadStartListener(loadStartListener);
        openLayers.addLayerLoadEndListener(loadEndListener);
        openLayers.setIsBaseLayer(true);
        wmsParams = new WMSParams();
        wmsParams.setLayers("Earth Image");
        bluemarbleDemis = new WMS("Demis Blue Marble",
                "http://www2.demis.nl/wms/wms.ashx?WMS=BlueMarble", wmsParams, wmsOptions);
        bluemarbleDemis.setIsBaseLayer(true);
        bluemarbleDemis.addLayerLoadStartListener(loadStartListener);
        bluemarbleDemis.addLayerLoadEndListener(loadEndListener);
        wmsParams = new WMSParams();
        wmsParams
                .setLayers("Countries,Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,"
                        + "Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports");
        wmsParams.setFormat("image/png");
        demis = new WMS("Demis WMS", "http://www2.demis.nl/wms/wms.ashx?WMS=WorldMap", wmsParams,
                wmsOptions);
        demis.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("0,2,3,4,5,8,9,10,40");
        plurel = new WMS("PLUREL_WMS",
                "http://plurel.jrc.ec.europa.eu/ArcGIS/services/worldwithEGM/mapserver/wmsserver?",
                wmsParams, wmsOptions);
        plurel.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("base,global_ir_satellite_10km,radar_precip_mode");
        weather = new WMS("Latest Clouds",
                "http://maps.customweather.com/image?client=ucl_test&client_password=t3mp",
                wmsParams, wmsOptions);
        weather.setIsBaseLayer(true);

        wmsParams = new WMSParams();
        wmsParams.setLayers("bluemarble,srtm30");
        srtmDem = new WMS("SRTM DEM", "http://iceds.ge.ucl.ac.uk/cgi-bin/icedswms?", wmsParams,
                wmsOptions);
        srtmDem.setIsBaseLayer(true);

        Bounds polarMaxExtent = new Bounds(-10700000, -10700000, 14700000, 14700000);
        double halfSideLength = (polarMaxExtent.getUpperRightY() - polarMaxExtent.getLowerLeftY())
                / (4 * 2);
        double centre = ((polarMaxExtent.getUpperRightY() - polarMaxExtent.getLowerLeftY()) / 2)
                + polarMaxExtent.getLowerLeftY();
        double low = centre - halfSideLength;
        double high = centre + halfSideLength;
        float polarMaxResolution = (float) ((high - low) / 256.0);
        double windowLow = centre - 2 * halfSideLength;
        double windowHigh = centre + 2 * halfSideLength;
        Bounds polarBounds = new Bounds(windowLow, windowLow, windowHigh, windowHigh);

        wmsParams = new WMSParams();
        wmsParams.setLayers("bluemarble_file");
        wmsParams.setFormat("image/jpeg");

        wmsPolarOptions = new WMSOptions();
        wmsPolarOptions.setProjection("EPSG:32661");
        wmsPolarOptions.setMaxExtent(polarBounds);
        wmsPolarOptions.setMaxResolution(polarMaxResolution);
        wmsPolarOptions.setTransitionEffect(TransitionEffect.RESIZE);
        wmsPolarOptions.setWrapDateLine(false);
        northPole = new WMS("North polar stereographic", "http://wms-basemaps.appspot.com/wms",
                wmsParams, wmsPolarOptions);

        wmsPolarOptions.setProjection("EPSG:32761");
        southPole = new WMS("South polar stereographic", "http://wms-basemaps.appspot.com/wms",
                wmsParams, wmsPolarOptions);

        map.addLayer(openLayers);
        map.addLayer(bluemarbleDemis);
        map.addLayer(demis);
        map.addLayer(plurel);
        map.addLayer(weather);
        map.addLayer(srtmDem);
        map.addLayer(northPole);
        map.addLayer(southPole);
        currentProjection = map.getProjection();
        map.addMapBaseLayerChangedListener(new MapBaseLayerChangedListener() {
            @Override
            public void onBaseLayerChanged(MapBaseLayerChangedEvent eventObject) {
                String url = eventObject.getLayer().getJSObject().getPropertyAsString("url");
                String layers = eventObject.getLayer().getJSObject().getPropertyAsArray("params")[0]
                        .getPropertyAsString("LAYERS");
                baseUrlForExport = url + (url.contains("?") ? "&" : "?");
                layersForExport = layers;
                if (!map.getProjection().equals(currentProjection)) {
                    currentProjection = map.getProjection();
                    for (String internalLayerId : wmsLayers.keySet()) {
                        WmsDetails wmsAndParams = wmsLayers.get(internalLayerId);
                        if (wmsAndParams != null) {
                            removeLayer(internalLayerId);
                            doAddingOfLayer(internalLayerId, wmsAndParams.params,
                                    getOptionsForCurrentProjection(),
                                    wmsAndParams.multipleElevations, wmsAndParams.multipleTimes);
                        }
                    }
                    map.zoomToMaxExtent();
                }
            }
        });
        map.setBaseLayer(demis);
    }

    private WMSOptions getOptionsForCurrentProjection() {
        if (currentProjection.equalsIgnoreCase("EPSG:32661")
                || currentProjection.equalsIgnoreCase("EPSG:32761")) {
            return wmsPolarOptions;
        } else {
            return wmsStandardOptions;
        }
    }

    private class FeatureInfoMessageAndFeatureIds {
        private String message;
        private String[] featureIds;
        public FeatureInfoMessageAndFeatureIds(String message, String[] featureIds) {
            super();
            this.message = message;
            this.featureIds = featureIds;
        }
    }
    
    private FeatureInfoMessageAndFeatureIds processFeatureInfo(String text) {
        Document featureInfo = XMLParser.parse(text);
        double lon = Double.parseDouble(featureInfo.getElementsByTagName("longitude").item(0)
                .getChildNodes().item(0).getNodeValue());
        double lat = Double.parseDouble(featureInfo.getElementsByTagName("latitude").item(0)
                .getChildNodes().item(0).getNodeValue());
        
        StringBuffer html = new StringBuffer("<table>");
        html.append("<tr><td><b>Clicked:</b></td></tr>");
        html.append("<tr><td><b>Longitude:</b></td><td>"+FORMATTER.format(lon)+"</td></tr>");
        html.append("<tr><td><b>Latitude:</b></td><td>"+FORMATTER.format(lat)+"</td></tr>");
        html.append("<tr><td>&nbsp;</td></tr>");
        
        NodeList feature = featureInfo.getElementsByTagName("Feature");
        int length = feature.getLength();
        String[] ids = new String[length];
        for(int i=0;i<length;i++){
            /*
             * For each feature...
             */
            Node item = feature.item(i);
            NodeList childNodes = item.getChildNodes();
            
            String id = null;
            Double actualX = null;
            Double actualY = null;
            NodeList featureInfoNode = null;
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node child = childNodes.item(j);
                if(child.getNodeName().equalsIgnoreCase("id")){
                    id = child.getFirstChild().getNodeValue();
                } else if(child.getNodeName().equalsIgnoreCase("actualX")){
                    actualX = Double.parseDouble(child.getFirstChild().getNodeValue());
                } else if(child.getNodeName().equalsIgnoreCase("actualY")){
                    actualY = Double.parseDouble(child.getFirstChild().getNodeValue());
                } else if(child.getNodeName().equalsIgnoreCase("FeatureInfo")){
                    featureInfoNode = child.getChildNodes();
                }
            }
            
            if(id != null) {
                ids[i] = id;
                html.append("<tr><td><b>Feature:</b></td><td>"+id);
            }
            if(actualX != null && actualY != null)
                html.append(" ("+FORMATTER.format(actualX)+","+FORMATTER.format(actualY)+")");
            html.append("</td></tr>");
            if(featureInfoNode != null){
                String time = null;
                String value = null;
                for (int j = 0; j < featureInfoNode.getLength(); j++) {
                    Node child = featureInfoNode.item(j);
                    if(child.getNodeName().equalsIgnoreCase("time")){
                        time = child.getFirstChild().getNodeValue();
                    } else if(child.getNodeName().equalsIgnoreCase("value")){
                        value = child.getFirstChild().getNodeValue();
                    }
                }
                if(value != null){
                    if(time != null){
                        html.append("<tr><td><b>Time:</b></td><td>"+time+"</td></tr>");
                    }
                    value = value.replaceAll(";", "<br/>");
                    html.append("<tr><td><b>Value:</b></td><td>"+value+"</td></tr>");
                }
            }
        }
        html.append("</table>");
        return new FeatureInfoMessageAndFeatureIds(html.toString(), ids);
    }

    private void addDrawingLayer() {
        Vector drawingLayer = new Vector("Drawing");
        drawingLayer.getEvents().register("featureadded", drawingLayer, new EventHandler() {
            @Override
            public void onHandle(EventObject eventObject) {
                WmsDetails wmsAndParams = wmsLayers.get(getTransectLayerId());
                if (wmsAndParams != null) {
                    WMS wmsLayer = wmsAndParams.wms;
                    JSObject featureJs = eventObject.getJSObject().getProperty("feature");
                    JSObject lineStringJs = VectorFeature.narrowToVectorFeature(featureJs)
                            .getGeometry().getJSObject();
                    LineString line = LineString.narrowToLineString(lineStringJs);
                    Point[] points = line.getComponents();
                    StringBuilder lineStringBuilder = new StringBuilder();
                    for (int i = 0; i < points.length - 1; i++) {
                        lineStringBuilder.append(points[i].getX() + " " + points[i].getY() + ", ");
                    }
                    lineStringBuilder.append(points[points.length - 1].getX() + " "
                            + points[points.length - 1].getY());
                    String transectUrl = "wms" + "?REQUEST=GetTransect" + "&LAYER="
                            + wmsLayer.getParams().getLayers() + "&CRS=" + currentProjection
                            + "&LINESTRING=" + lineStringBuilder + "&FORMAT=image/png";
                    String elevation = wmsLayer.getParams().getJSObject()
                            .getPropertyAsString("ELEVATION");
                    String time = wmsLayer.getParams().getJSObject().getPropertyAsString("TIME");
                    if (elevation != null && !elevation.equals("")) {
                        transectUrl += "&ELEVATION=" + elevation;
                    }
                    if (time != null && !time.equals("")) {
                        transectUrl += "&TIME=" + time;
                    }
                    /*
                     * Yes, this is peculiar. Yes, it is also necessary.
                     * 
                     * Without this, the GetFeatureInfo functionality stops
                     * working after a transect graph has been plotted.
                     * 
                     * Please feel free to play with it for hours trying to get
                     * it to work another way - and Good Luck!
                     */
                    if (getFeatureInfo != null) {
                        getFeatureInfo.deactivate();
                        getFeatureInfo.activate();
                    }
                    displayImagePopup(transectUrl, "Transect");
                }
            }
        });
        editingToolbar = new EditingToolbar(drawingLayer); 
        map.addControl(editingToolbar);
    }

    /*
     * Gets the ID of the layer to be used for transects + KML
     * 
     * If it hasn't been set, pick a random layer. Failing that, return null
     */
    private String getTransectLayerId() {
        if (transectLayer != null) {
            return transectLayer;
        } else {
            return wmsLayers.keySet().isEmpty() ? null : wmsLayers.keySet().iterator().next();
        }
    }

    public void setTransectLayerId(String transectLayer) {
        this.transectLayer = transectLayer;
    }
    
    public void setMultiFeature(boolean multiFeature){
        if(multiFeature){
            editingToolbar.deactivate();
        } else {
            editingToolbar.activate();
        }
    }

}