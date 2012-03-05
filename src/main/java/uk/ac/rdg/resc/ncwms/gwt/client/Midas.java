package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerDetails;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.GodivaWidgets;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MapArea;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MultiLayerSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfoIF;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * TODO:
 * opacity
 * redo palette selector (but get current one working first)
 * DON'T get too involved.  Make it work, don't make it pretty.  Make it pretty once gFI is working.
 * 
 * @author Guy Griffiths
 *
 */
public class Midas extends BaseWmsClient {
    
//    private TabLayoutPanel tabPanel;
    private TabPanel tabPanel;
    private Map<String, GodivaWidgets> idToWidgets;
    private Map<String, Widget> idToTabPane;
    private MultiLayerSelector layerSelector;
    
    @Override
    public void init() {
        idToWidgets = new HashMap<String, GodivaWidgets>();
        idToTabPane = new HashMap<String, Widget>();
        layerSelector = new MultiLayerSelector(this);
//        tabPanel = new TabLayoutPanel(1.5, Unit.EM);
        tabPanel = new TabPanel();

        ScrollPanel layerScroll = new ScrollPanel(layerSelector);
        layerScroll.setHeight(getMapHeight()+"px");
        layerScroll.setWidth("420px");
        tabPanel.setHeight(getMapHeight()+"px");
        tabPanel.setWidth("420px");
        
        RootLayoutPanel mainWindow = RootLayoutPanel.get();
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.add(layerScroll);
        hPanel.add(mapArea);
        hPanel.add(tabPanel);
        mainWindow.add(hPanel);
    }

    @Override
    public void enableWidgets() {
        // TODO Auto-generated method stub

    }

    @Override
    public void disableWidgets() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateMap(MapArea mapArea, String layerUpdated) {
        GodivaWidgets widgetCollection = idToWidgets.get(layerUpdated);
        String currentTime = widgetCollection.getTimeSelector().getSelectedDateTime();
        String currentElevation = widgetCollection.getElevationSelector().getSelectedElevation();
        String currentPalette = widgetCollection.getPaletteSelector().getSelectedPalette();
        String currentScaleRange = widgetCollection.getPaletteSelector().getScaleRange();
        int nColourBands = widgetCollection.getPaletteSelector().getNumColorBands();
        boolean logScale = widgetCollection.getPaletteSelector().isLogScale();
        mapArea.addLayer(layerUpdated, layerUpdated, currentTime, currentElevation, "boxfill",
                currentPalette, currentScaleRange, nColourBands, logScale);
    }

    @Override
    public void menuLoaded(LayerMenuItem menuTree) {
        layerSelector.populateLayers(menuTree);
    }

    @Override
    public void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate) {
        String id = layerDetails.getId();
        ElevationSelectorIF elevationSelector = new ElevationSelector(id, "Elevation", this);
        TimeSelectorIF timeSelector = new TimeSelector(id, "Time", this);
        PaletteSelectorIF paletteSelector = new PaletteSelector(id, 30, 400, this, getBaseWmsUrl(), false);
        UnitsInfoIF unitsInfo = new UnitsInfo();
        GodivaWidgets widgets = new GodivaWidgets(elevationSelector, timeSelector, paletteSelector, unitsInfo);
        populateWidgets(layerDetails, widgets);
        if(autoUpdate){
            paletteSelector.selectPalette(layerDetails.getSelectedPalette());
        }
        idToWidgets.put(id, widgets);
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(timeSelector);
        vPanel.add(elevationSelector);
        vPanel.add(paletteSelector);
        vPanel.add(unitsInfo);
        
        tabPanel.add(vPanel, id);
        idToTabPane.put(id, vPanel);
        int index = tabPanel.getWidgetIndex(vPanel);
//        tabPanel.setTabHTML(index, "<FONT COLOR=\"BLACK\">"+id+"</FONT>");
        tabPanel.selectTab(index);
    }

    @Override
    public void availableTimesLoaded(String layerId, List<String> availableTimes, String nearestTime) {
        idToWidgets.get(layerId).getTimeSelector().populateTimes(availableTimes);
    }

    @Override
    public void rangeLoaded(String layerId, double min, double max) {
        idToWidgets.get(layerId).getPaletteSelector().setScaleRange(min+","+max);
    }

    @Override
    public void loadingStarted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadingFinished() {
        // TODO Auto-generated method stub

    }

    @Override
    public GodivaWidgets getWidgetCollection(String layerId) {
        return idToWidgets.get(layerId);
    }

    @Override
    public String getCurrentTime() {
        // TODO We have no time here, but *maybe* we want one
        return null;
    }
    
    @Override
    public void layerDeselected(String layerId) {
        super.layerDeselected(layerId);
        tabPanel.remove(idToTabPane.get(layerId));
        idToWidgets.remove(layerId);
    }

}
