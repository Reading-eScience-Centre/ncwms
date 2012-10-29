package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

public interface TimeSelectorIF extends IsWidget {
    public void setId(String id);
    public void populateDates(List<String> availableDates);
    public List<String> getAvailableDates();
    public void populateTimes(List<String> availableTimes);
    public List<String> getAvailableTimes();
    public String getSelectedDate();
    public String getSelectedDateTime();
    public String getSelectedTime();
    public String getSelectedDateTimeRange();
    public boolean selectDate(String dateString);
    public boolean selectDateTime(String timeString);
    public void setEnabled(boolean enabled);
    public boolean hasMultipleTimes();
    
    public void setContinuous(boolean continuous);
    public boolean isContinuous();
    public void selectRange(String currentRange);
    public String getRange();
}
