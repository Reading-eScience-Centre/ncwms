package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

public interface LayerSelectionHandler {
    public void layerSelected(String layerName, boolean autoZoomAndPalette);
    public void layerDeselected(String layerName);
    public void refreshLayerList();
}
