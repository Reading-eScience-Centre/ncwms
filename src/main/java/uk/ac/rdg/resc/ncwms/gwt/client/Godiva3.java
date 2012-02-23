package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.LinkedHashMap;
import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerDetails;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.AnimationButton;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelectorIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfoIF;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.WidgetCollection;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Godiva3 extends BaseWmsClient {
    private LayerSelectorIF layerSelector;
    private WidgetCollection widgetCollection;
    private LayerDetails layerDetails = null;
    private LayerState layerState;
    
    private Image logo;
    private Image loadingImage;
    
    public void init() {
        /*
         * Create a new layer state with default values
         */
        layerState = new LayerState();
        layerSelector = new LayerSelectorCombo(this);

        ElevationSelectorIF elevationSelector = new ElevationSelector("mainLayer", "Depth", this);
        TimeSelectorIF timeSelector = new TimeSelector("mainLayer", this);
        PaletteSelectorIF paletteSelector = new PaletteSelector("mainLayer", getMapHeight(), this, wmsUrl);
        UnitsInfoIF unitsInfo = new UnitsInfo();

        widgetCollection = new WidgetCollection(elevationSelector, timeSelector, paletteSelector, unitsInfo);

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
    
    public void menuLoaded(LayerMenuItem menuTree) {
        layerSelector.populateLayers(menuTree);
        
        if (menuTree.isLeaf()) {
            String message = "<h2>No Georeferencing Data Found</h2><br>"
                    + "No georeferencing data could be found in the dataset \"" + menuTree.getTitle() + "\"";
            handleCatastrophicError(message);
            return;
        }

        Window.setTitle(menuTree.getTitle());
        
        requestLayerDetails(layerSelector.getSelectedId(), null, true);
    }
    
    public void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate) {
        this.layerDetails = layerDetails;
        if(layerDetails.getSupportedStyles().size() > 0){
            layerState.setStyle(layerDetails.getSupportedStyles().get(0));
        }
        populateWidgets(layerDetails, widgetCollection, autoUpdate);
    }
    
    public void loadingStarted() {
        loadingImage.setVisible(true);
    }

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
//        System.out.println("nearestTime (Godiva3 122):"+nearestTime);
        if (nearestTime != null){
            widgetCollection.getTimeSelector().selectTime(nearestTime); 
        }
    }
    
    public void rangeLoaded(double min, double max) {
        // TODO Auto-generated method stub
        String scaleRange = min + "," + max;
        layerState.setScaleRange(scaleRange);
        widgetCollection.getPaletteSelector().setScaleRange(scaleRange);
    }

    @Override
    public LayerState getLayerState(String layerId) {
        /*
         * TODO this isn't very nice...
         */
        layerState.setCurrentTime(widgetCollection.getTimeSelector().getSelectedDateTime());
        layerState.setCurrentElevation(widgetCollection.getElevationSelector().getSelectedElevation());
        layerState.setLogScale(widgetCollection.getPaletteSelector().isLogScale());
        layerState.setNColorBands(widgetCollection.getPaletteSelector().getNumColorBands());
        layerState.setPalette(widgetCollection.getPaletteSelector().getSelectedPalette());
        layerState.setScaleRange(widgetCollection.getPaletteSelector().getScaleRange());
        return layerState;
    }

    @Override
    public String getLayerId() {
        return layerSelector.getSelectedId();
    }
}
