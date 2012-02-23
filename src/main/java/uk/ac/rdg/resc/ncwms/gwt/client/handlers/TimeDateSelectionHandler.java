package uk.ac.rdg.resc.ncwms.gwt.client.handlers;

public interface TimeDateSelectionHandler {
    public void dateSelected(String layerId, String selectedDate);
    public void timeSelected(String layerId, String selectedTime);
}
