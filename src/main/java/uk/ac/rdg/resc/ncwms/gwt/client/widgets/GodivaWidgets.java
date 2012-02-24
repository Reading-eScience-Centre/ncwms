package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

public class GodivaWidgets {
    private ElevationSelectorIF elevationSelector;
    private TimeSelectorIF timeSelector;
    private PaletteSelectorIF paletteSelector;
    private UnitsInfoIF unitsInfo;

    public GodivaWidgets(ElevationSelectorIF elevationSelector,
            TimeSelectorIF timeSelector, PaletteSelectorIF paletteSelector, UnitsInfoIF unitsInfo) {
        super();
        this.elevationSelector = elevationSelector;
        this.timeSelector = timeSelector;
        this.paletteSelector = paletteSelector;
        this.unitsInfo = unitsInfo;
    }

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
