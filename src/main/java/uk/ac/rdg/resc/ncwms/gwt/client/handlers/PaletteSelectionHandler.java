package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

public interface PaletteSelectionHandler {
    public void paletteChanged(String paletteName, int nColorBands);
    public void scaleRangeChanged(String scaleRange);
    public void logScaleChanged(boolean newIsLogScale);
    public void autoAdjustPalette();
}
