package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.IsWidget;

import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;

public interface LayerSelectorIF extends IsWidget {
    public void populateLayers(LayerMenuItem topItem);
    public String getSelectedId();
    public void setSelectedLayer(String id);
    public void setEnabled(boolean enabled);
}
