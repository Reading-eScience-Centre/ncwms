package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;

import com.google.gwt.user.client.ui.IsWidget;

public interface LayerSelectorIF extends IsWidget {
    public void populateLayers(LayerMenuItem topItem);
    public List<String> getSelectedIds();
    public void setSelectedLayer(String id);
    public void setEnabled(boolean enabled);
    public List<String> getTitleElements();
}
