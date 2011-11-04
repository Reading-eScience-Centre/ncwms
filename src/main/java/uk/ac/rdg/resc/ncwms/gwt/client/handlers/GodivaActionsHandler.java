package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

import org.gwtopenmaps.openlayers.client.event.MapMoveListener;
import org.gwtopenmaps.openlayers.client.event.MapZoomListener;

public interface GodivaActionsHandler extends MapMoveListener, MapZoomListener {
    public void setLoading(boolean loading);
    public void disableWidgets();
    public void enableWidgets();
}
