package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

public class GodivaWidgets {
    private ElevationSelectorIF elevationSelector;
    private TimeSelectorIF timeSelector;
    private PaletteSelectorIF paletteSelector;
    private UnitsInfoIF unitsInfo;
    private CopyrightInfoIF copyrightInfo;

    public GodivaWidgets(ElevationSelectorIF elevationSelector, TimeSelectorIF timeSelector,
            PaletteSelectorIF paletteSelector, UnitsInfoIF unitsInfo, CopyrightInfoIF copyrightInfo) {
        super();
        this.elevationSelector = elevationSelector;
        this.timeSelector = timeSelector;
        this.paletteSelector = paletteSelector;
        this.unitsInfo = unitsInfo;
        this.copyrightInfo = copyrightInfo;
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
    
    public CopyrightInfoIF getCopyrightInfo() {
        return copyrightInfo;
    }
}
