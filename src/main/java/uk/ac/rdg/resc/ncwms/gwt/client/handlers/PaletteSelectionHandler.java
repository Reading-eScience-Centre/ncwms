package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

public interface PaletteSelectionHandler {
    public void paletteChanged(String layerId, String paletteName, int nColorBands);
    public void scaleRangeChanged(String layerId, String scaleRange);
    public void logScaleChanged(String layerId, boolean newIsLogScale);
    public void autoAdjustPalette(String layerId);
}
