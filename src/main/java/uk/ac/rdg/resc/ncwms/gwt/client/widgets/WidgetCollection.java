package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

public class WidgetCollection {
//    private LayerSelectorIF layerSelector;
    private ElevationSelectorIF elevationSelector;
    private TimeSelectorIF timeSelector;
    private PaletteSelectorIF paletteSelector;
    private UnitsInfoIF unitsInfo;

    public WidgetCollection(/*LayerSelectorIF layerSelector, */ElevationSelectorIF elevationSelector,
            TimeSelectorIF timeSelector, PaletteSelectorIF paletteSelector, UnitsInfoIF unitsInfo) {
        super();
//        this.layerSelector = layerSelector;
        this.elevationSelector = elevationSelector;
        this.timeSelector = timeSelector;
        this.paletteSelector = paletteSelector;
        this.unitsInfo = unitsInfo;
    }

//    public LayerSelectorIF getLayerSelector() {
//        return layerSelector;
//    }

    public ElevationSelectorIF getElevationSelector() {
        return elevationSelector;
    }

    public TimeSelectorIF getTimeSelector() {
        return timeSelector;
    }

    public PaletteSelectorIF getPaletteSelector() {
        return paletteSelector;
    }

    public UnitsInfoIF getUnitsInfo() {
        return unitsInfo;
    }
}
