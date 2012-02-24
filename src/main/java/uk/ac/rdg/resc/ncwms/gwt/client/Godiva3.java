package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerDetails;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.GodivaWidgets;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MapArea;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfoIF;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Godiva3 extends BaseWmsClient {
    private static final String WMS_LAYER_ID = "singleLayer";
    
    private LayerSelectorIF layerSelector;
    private GodivaWidgets widgetCollection;
    private String style;
    
    private Image logo;
    private Image loadingImage;
    
    @Override
    public void init() {
        mapArea.setTransectLayerId(WMS_LAYER_ID);
        
        layerSelector = new LayerSelectorCombo(this);

        ElevationSelectorIF elevationSelector = new ElevationSelector("mainLayer", "Depth", this);
        TimeSelectorIF timeSelector = new TimeSelector("mainLayer", this);
        PaletteSelectorIF paletteSelector = new PaletteSelector("mainLayer", getMapHeight(), this, wmsUrl);
        UnitsInfoIF unitsInfo = new UnitsInfo();

        widgetCollection = new GodivaWidgets(elevationSelector, timeSelector, paletteSelector, unitsInfo);

        logo = new Image("img/resc_logo.png");

        loadingImage = new Image("img/loading.gif");
        loadingImage.setVisible(false);
        loadingImage.setStylePrimaryName("loadingImage");

//        anim = new AnimationButton(mapArea, proxyUrl + wmsUrl);

        RootLayoutPanel mainWindow = RootLayoutPanel.get();

        mainWindow.add(LayoutManager.getGodiva3Layout(layerSelector, unitsInfo, timeSelector,
                elevationSelector, paletteSelector, kmzLink, permalink, email,
                screenshot, logo, mapArea, loadingImage));

        timeSelector.setEnabled(false);
        elevationSelector.setEnabled(false);
        paletteSelector.setEnabled(false);
        unitsInfo.setEnabled(false);
    }
    
    @Override
    public void menuLoaded(LayerMenuItem menuTree) {
        layerSelector.populateLayers(menuTree);
        
        if (menuTree.isLeaf()) {
            String message = "<h2>No Georeferencing Data Found</h2><br>"
                    + "No georeferencing data could be found in the dataset \"" + menuTree.getTitle() + "\"";
            handleCatastrophicError(message);
            return;
        }

        Window.setTitle(menuTree.getTitle());
    }
    
    @Override
    public void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate) {
        if(layerDetails.getSupportedStyles().size() > 0){
            style = layerDetails.getSupportedStyles().get(0);
        }
        /*
         * We populate our widgets here, but in a multi-layer system, we may
         * want to create new widgets here
         */
        populateWidgets(layerDetails, widgetCollection);
        if(autoUpdate){
            widgetCollection.getPaletteSelector().selectPalette(layerDetails.getSelectedPalette());
        }
    }
    
    @Override
    public void loadingStarted() {
        loadingImage.setVisible(true);
    }

    @Override
    public void loadingFinished() {
        loadingImage.setVisible(false);
    }
    
    @Override
    public void enableWidgets() {
        layerSelector.setEnabled(true);
        widgetCollection.getTimeSelector().setEnabled(true);
        widgetCollection.getElevationSelector().setEnabled(true);
        widgetCollection.getPaletteSelector().setEnabled(true);
        widgetCollection.getUnitsInfo().setEnabled(true);
    }
    
    @Override
    public void disableWidgets() {
        layerSelector.setEnabled(false);
        widgetCollection.getTimeSelector().setEnabled(false);
        widgetCollection.getElevationSelector().setEnabled(false);
        widgetCollection.getPaletteSelector().setEnabled(false);
        widgetCollection.getUnitsInfo().setEnabled(false);
    }

    @Override
    public void availableTimesLoaded(String layerId, List<String> availableTimes, String nearestTime) {
        widgetCollection.getTimeSelector().populateTimes(availableTimes);
        if (nearestTime != null){
            widgetCollection.getTimeSelector().selectTime(nearestTime); 
        }
    }
    
    @Override
    public void rangeLoaded(double min, double max) {
        widgetCollection.getPaletteSelector().setScaleRange(min + "," + max);
    }

    @Override
    public GodivaWidgets getWidgetCollection(String layerId){
        return widgetCollection;
    }

    @Override
    public void updateMap(MapArea mapArea) {
        String currentTime = widgetCollection.getTimeSelector().getSelectedDateTime();
        String currentElevation = widgetCollection.getElevationSelector().getSelectedElevation();
        String currentPalette = widgetCollection.getPaletteSelector().getSelectedPalette();
        String currentScaleRange = widgetCollection.getPaletteSelector().getScaleRange();
        int nColourBands = widgetCollection.getPaletteSelector().getNumColorBands();
        boolean logScale = widgetCollection.getPaletteSelector().isLogScale();
        mapArea.addLayer(WMS_LAYER_ID, layerSelector.getSelectedId(), currentTime, currentElevation, style,
                currentPalette, currentScaleRange, nColourBands, logScale);
    }
}
