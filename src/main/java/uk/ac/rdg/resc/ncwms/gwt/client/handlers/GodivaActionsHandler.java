package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

import org.gwtopenmaps.openlayers.client.event.MapMoveListener;
import org.gwtopenmaps.openlayers.client.event.MapZoomListener;

public interface GodivaActionsHandler extends MapMoveListener, MapZoomListener {
    /**
     * Should be called when a component starts or stops loading. This allows
     * implementing classes to keep a count of the number of elements loading
     * and take appropriate action when something starts loading, or when
     * everything has finished loading
     * 
     * @param loading
     *            true if the element has started loading, false if it has
     *            finished loading
     */
    public void setLoading(boolean loading);

    /**
     * Called to enable widgets. This allows control of which widgets are
     * available when e.g. animation is happening
     */
    public void enableWidgets();

    /**
     * Called to disable widgets. This allows control of which widgets are
     * available when e.g. animation is happening
     */
    public void disableWidgets();
}
