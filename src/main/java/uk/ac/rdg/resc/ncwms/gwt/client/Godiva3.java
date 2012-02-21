package uk.ac.rdg.resc.ncwms.gwt.client;

import java.util.LinkedHashMap;

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
    
    private Image logo;
    private Image loadingImage;
    
    public void init() {
        layerSelector = new LayerSelectorCombo(this);

        ElevationSelectorIF elevationSelector = new ElevationSelector("mainLayer", "Depth", this);
        TimeSelectorIF timeSelector = new TimeSelector("mainLayer", this);
        PaletteSelectorIF paletteSelector = new PaletteSelector("mainLayer", getMapHeight(), this, wmsUrl);
        UnitsInfoIF unitsInfo = new UnitsInfo();

        widgetCollection = new WidgetCollection(layerSelector, elevationSelector, timeSelector, paletteSelector, unitsInfo);

        logo = new Image("img/resc_logo.png");

        loadingImage = new Image("img/loading.gif");
        loadingImage.setVisible(false);
        loadingImage.setStylePrimaryName("loadingImage");

//        anim = new AnimationButton(mapArea, proxyUrl + wmsUrl);

        RootLayoutPanel mainWindow = RootLayoutPanel.get();

//        mainWindow.add(LayoutManager.getGodivaLayout(layerSelector, unitsInfo, timeSelector,
//                elevationSelector, paletteSelector, /*anim*/null, kmzLink, permalink, email, null,//docLink,
//                screenshot, logo, mapArea, loadingImage));

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
        
        requestLayerDetails(layerSelector.getSelectedId(), null, null, true);
//        if (permalinking) {
//            String currentLayer = permalinkParamsMap.get("layer");
//            if (currentLayer != null) {
//                layerSelectorCombo.setSelectedLayer(currentLayer);
//                requestLayerDetails(layerSelectorCombo.getSelectedId(), currentTime, false);
//            }
//        }
    }
    
    public void layerDetailsLoaded(LayerDetails layerDetails, boolean autoUpdate) {
        populateWidgets(layerDetails, widgetCollection, autoUpdate);

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
        

//        currentElevation = elevationSelector.getSelectedElevation();
//        }

        // Not currently used.
        // moreInfo = getMoreInfo();
        // copyright = getCopyright();


    }
    
    public void loadingStarted() {
        loadingImage.setVisible(true);
    }

    public void loadingFinished() {
        loadingImage.setVisible(false);
    }
}
