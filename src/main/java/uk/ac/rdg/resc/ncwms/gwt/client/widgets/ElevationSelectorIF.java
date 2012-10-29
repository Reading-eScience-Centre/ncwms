package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

public interface ElevationSelectorIF extends IsWidget {
    public void setId(String id);
    public void populateElevations(List<String> availableElevations);
    public void setUnitsAndDirection(String units, boolean positive);
    public String getSelectedElevation();
    public String getSelectedElevationRange();
    public void setSelectedElevation(String currentElevation);
    public void setEnabled(boolean enabled);
    public int getNElevations();
    public String getVerticalUnits();
    
    public void setContinuous(boolean continuous);
    public boolean isContinuous();
}
