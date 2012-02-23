package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

public interface ElevationSelectorIF extends IsWidget {
    public void setId(String id);
    public void populateVariables(List<String> availableElevations, String currentElevation);
    public void setUnitsAndDirection(String units, boolean positive);
    public String getSelectedElevation();
    public void setSelectedElevation(String currentElevation);
    public void setEnabled(boolean enabled);
}
