package uk.ac.rdg.resc.ncwms.xmlview.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.text.StrBuilder;

import uk.ac.rdg.resc.godiva.client.handlers.ElevationSelectionHandler;
import uk.ac.rdg.resc.godiva.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.godiva.client.handlers.TimeDateSelectionHandler;
import uk.ac.rdg.resc.godiva.client.requests.ConnectionException;
import uk.ac.rdg.resc.godiva.client.requests.ErrorHandler;
import uk.ac.rdg.resc.godiva.client.requests.LayerDetails;
import uk.ac.rdg.resc.godiva.client.requests.LayerRequestBuilder;
import uk.ac.rdg.resc.godiva.client.requests.LayerRequestCallback;
import uk.ac.rdg.resc.godiva.client.requests.LayerTreeJSONParser;
import uk.ac.rdg.resc.godiva.client.requests.TimeRequestBuilder;
import uk.ac.rdg.resc.godiva.client.requests.TimeRequestCallback;
import uk.ac.rdg.resc.godiva.client.state.ElevationSelectorIF;
import uk.ac.rdg.resc.godiva.client.state.LayerSelectorIF;
import uk.ac.rdg.resc.godiva.client.state.TimeSelectorIF;
import uk.ac.rdg.resc.godiva.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.godiva.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.godiva.client.widgets.TimeSelector;
import uk.ac.rdg.resc.godiva.shared.LayerMenuItem;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class XMLView implements EntryPoint, LayerSelectionHandler, ErrorHandler, TimeDateSelectionHandler, ElevationSelectionHandler {

    private Image image;
    private Label sizeLabel;
    private TextBox width;
    private Label xLabel;
    private TextBox height;
    private TextArea xmlEntry;
    private Label idList;
    private PushButton button;
    private LayerSelectorIF layerSelector;
    private TimeSelectorIF timeSelector;
    private ElevationSelectorIF elevationSelector;
    
    private String bbox = null;
    
    @Override
    public void onModuleLoad() {
//        curl -v -F "REQUEST=WMS" -F "XML_STYLE=@raster_plus_arrows.xml" http://honeybee:8080/edal-ncwms/wms?
        VerticalPanel mainPanel = new VerticalPanel();
        
        VerticalPanel widgetPanel = new VerticalPanel();
        
        HorizontalPanel xmlMapPanel = new HorizontalPanel();
        xmlEntry = new TextArea();
        xmlEntry.setName("XML_STYLE");
        xmlEntry.setSize("600px", "400px");
        
        image = new Image();
        
        xmlMapPanel.add(xmlEntry);
        xmlMapPanel.add(image);
        
        button = new PushButton("Process XML");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                System.out.println(getUrl());
                image.setUrl(getUrl());
            }

        });
        
        layerSelector = new LayerSelectorCombo(this);
        timeSelector = new TimeSelector("timeselector", this);
        elevationSelector = new ElevationSelector("elevationselector", "Depth", this);
        idList = new Label("ID: ");
        idList.setStylePrimaryName("labelStyle");
        
        widgetPanel.add(layerSelector);
        widgetPanel.add(timeSelector);
        widgetPanel.add(elevationSelector);
        widgetPanel.add(idList);
        
        HorizontalPanel sizePanel = new HorizontalPanel();
        sizeLabel = new Label("Size: ");
        width = new TextBox();
        width.setText("800");
        xLabel = new Label(" x ");
        height = new TextBox();
        height.setText("400");
        
        sizePanel.add(sizeLabel);
        sizePanel.add(width);
        sizePanel.add(xLabel);
        sizePanel.add(height);
        
        widgetPanel.add(sizePanel);
        
        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.add(widgetPanel);
        topPanel.add(new HTML(
                "First select one of the layers you wish to plot.<br/>"
                        + "This will give the available times and depths,"
                        +" and will set the bounding box to the full extents.<br/>"
                        +"It will also show the layer ID for use in the XML<br/>"
                        +"Select the desired time/depth values (or leave the defaults).<br/>"
                        +"Change the image size if you want<br/>"
                        +"Then enter some valid XML in the box and click \"Process XML\"<br/>"
                        +"If your XML was valid, you'll get an image<br/>"
                        +"This is a minimal client for developer testing<br/>"
                        ));
        
        mainPanel.add(topPanel);
        mainPanel.add(xmlMapPanel);
        mainPanel.add(button);
        
        
        RootLayoutPanel rootLayoutPanel = RootLayoutPanel.get();
        rootLayoutPanel.add(mainPanel);
        
        requestAndPopulateMenu();
    }
    
    private String getUrl() {
        StringBuilder url = new StringBuilder();
        url.append(URL.encode("wms?REQUEST=TestStyle&VERSION=1.3.0&XML_STYLE=") + URL.encodeQueryString(xmlEntry.getValue())
                + "&BBOX=" + URL.encodeQueryString(bbox)
                + "&WIDTH=" + URL.encodeQueryString(width.getText())
                + "&HEIGHT=" + URL.encodeQueryString(height.getText()));
        if(timeSelector.getSelectedDateTime() != null) {
            url.append("&TIME="+timeSelector.getSelectedDateTime());
        }
        if(timeSelector.getSelectedDateTimeRange() != null) {
            url.append("&COLORBY/TIME="+timeSelector.getSelectedDateTimeRange());
        }
        if(elevationSelector.getSelectedElevation() != null) {
            url.append("&ELEVATION="+elevationSelector.getSelectedElevation());
        }
        if(elevationSelector.getSelectedElevationRange() != null) {
            url.append("&COLORBY/DEPTH="+elevationSelector.getSelectedElevationRange());
        }
        return url.toString();
    }
    
    /**
     * Requests the layer menu from the server. When the menu is returned,
     * menuLoaded will be called
     */
    private void requestAndPopulateMenu() {
        /*
         * This is where we define the fact that we are working with a single
         * local server
         */
        final String wmsUrl = "wms";
        final RequestBuilder getMenuRequest = new RequestBuilder(RequestBuilder.GET, URL.encode("wms?request=GetMetadata&item=menu"));
        getMenuRequest.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request req, Response response) {
                try {
                    if (response.getStatusCode() != Response.SC_OK) {
                        throw new ConnectionException("Error contacting server");
                    }
                    /*
                     * Once the menu has been received, parse it, and call
                     * menuLoaded()
                     * 
                     * This is a separate method so that subclasses can change
                     * the behaviour once the menu is loaded
                     */
                    JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                    JSONObject parentObj = jsonMap.isObject();
                    LayerMenuItem menuTree = LayerTreeJSONParser.getTreeFromJson(wmsUrl, parentObj);

                    menuLoaded(menuTree);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Request request, Throwable e) {
                e.printStackTrace();
            }
        });

        try {
            getMenuRequest.send();
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }
    
    protected void menuLoaded(LayerMenuItem menuTree) {
        if (menuTree.isLeaf()) {
            menuTree.addChildItem(new LayerMenuItem("No georeferencing data found!", null, null,
                    false, null));
        }
        layerSelector.populateLayers(menuTree);
    }

    @Override
    public void layerSelected(String wmsUrl, String layerId, boolean autoZoomAndPalette) {
        requestLayerDetails(wmsUrl, layerId, autoZoomAndPalette);        
        idList.setText("ID: "+layerId);
    }

    @Override
    public void layerDeselected(String layerId) {
    }

    @Override
    public void refreshLayerList() {
        requestAndPopulateMenu();
    }
    
    protected void requestLayerDetails(final String wmsUrl, final String layerId, final boolean autoZoomAndPalette) {
        if (layerId == null) {
            /*
             * We have no variables defined in the selected layer
             * 
             * Return here. We are already dealing with the case where there are
             * no layers present.
             */
            return;
        }

        final LayerRequestBuilder getLayerDetailsRequest = new LayerRequestBuilder(layerId,
                wmsUrl, null);

        getLayerDetailsRequest.setCallback(new LayerRequestCallback(layerId, this) {
            @Override
            public void onResponseReceived(Request req, Response response) {
                try {
                    super.onResponseReceived(req, response);
                    if (response.getStatusCode() != Response.SC_OK) {
                        throw new ConnectionException("Error contacting server");
                    }
                    /*
                     * This will make a call to populateWidgets, and may create
                     * extra widgets if needed (e.g. for multi-layer clients)
                     */
                    layerDetailsLoaded(getLayerDetails(), autoZoomAndPalette);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Request request, Throwable e) {
                /*
                 * We have an error. We set the state variables correctly and
                 * update the map, then handle the error that occurred
                 */
                handleError(e);
            }
        });

        try {
            /*
             * Register that we are loading something, then send the request
             */
            getLayerDetailsRequest.send();
        } catch (RequestException e) {
            /*
             * If this fails, set the loading state
             */
            handleError(e);
        }
    }

    protected void layerDetailsLoaded(LayerDetails layerDetails, boolean autoZoomAndPalette) {
        bbox = layerDetails.getExtents();
        
        elevationSelector.setId(layerDetails.getId());
        timeSelector.setId(layerDetails.getId());

        elevationSelector.setUnitsAndDirection(layerDetails.getZUnits(),
                layerDetails.isZPositive());

        timeSelector.setContinuous(layerDetails.isMultiFeature());
        elevationSelector.setContinuous(layerDetails.isMultiFeature());

        if (layerDetails.isMultiFeature()) {
            /*
             * Set all options which depend on this being a multi-feature layer
             */
            if (layerDetails.getStartTime().equals(layerDetails.getEndTime())) {
                timeSelector.populateDates(null);
            } else {
                List<String> startEndDates = new ArrayList<String>();
                startEndDates.add(layerDetails.getStartTime());
                startEndDates.add(layerDetails.getEndTime());
                timeSelector.populateDates(startEndDates);
            }

            if (layerDetails.getStartZ().equals(layerDetails.getEndZ())) {
                elevationSelector.populateElevations(null);
            } else {
                List<String> startEndZs = new ArrayList<String>();
                startEndZs.add(layerDetails.getStartZ());
                startEndZs.add(layerDetails.getEndZ());
                elevationSelector.populateElevations(startEndZs);
            }
            if (layerDetails.getNearestDateTime() != null) {
                timeSelector
                        .selectDateTime(layerDetails.getNearestDateTime());
            }
        } else {
            /*
             * Set all options which depend on this being a single-feature layer
             */
            timeSelector.populateDates(layerDetails.getAvailableDates());
            elevationSelector.populateElevations(
                    layerDetails.getAvailableZs());
            if (layerDetails.getNearestDateTime() != null) {
                timeSelector.selectDate(layerDetails.getNearestDate());
            }
        }
    }

    @Override
    public void handleError(Throwable e) {
        e.printStackTrace();
    }

    @Override
    public void dateSelected(final String layerId, String selectedDate) {
        if (selectedDate == null) {
            return;
        }
        final TimeRequestBuilder getTimeRequest = new TimeRequestBuilder(layerId, selectedDate,
                layerSelector.getWmsUrl());
        getTimeRequest.setCallback(new TimeRequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                try {
                    super.onResponseReceived(request, response);
                    if (response.getStatusCode() != Response.SC_OK) {
                        throw new ConnectionException("Error contacting server");
                    }
                    timeSelector.populateTimes(getAvailableTimesteps());
                    
                    datetimeSelected(layerId, timeSelector.getSelectedDateTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                handleError(exception);
            }
        });

        try {
            getTimeRequest.send();
        } catch (RequestException e) {
            handleError(e);
        }
    }

    @Override
    public void datetimeSelected(String layerId, String selectedDatetime) {
    }

    @Override
    public void elevationSelected(String layerId, String elevation) {
    }
}
