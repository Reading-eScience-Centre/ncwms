package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.StartEndTimeHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ErrorHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestCallback;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A popup which allows a user to select start and end times. The start time is
 * guaranteed to be earlier or in some exceptional cases, equal to, the end
 * time.
 * 
 * Clients should use the
 * {@link StartEndTimePopup#setTimeSelectionHandler(StartEndTimeHandler)} method
 * to set a callback for when the times have been selected
 * 
 * @author Guy Griffiths
 * 
 */
public class StartEndTimePopup extends DialogBoxWithCloseButton {

    private final String layer;
    private String jsonProxyUrl;
    private String errorMessage;
    private String buttonLabel;
    private Button nextButton;
    private TimeSelector startTimeSelector;
    private TimeSelector endTimeSelector;
    private List<String> availableDates;
    private Map<String, List<String>> availableTimes;
    private StartEndTimeHandler timesHandler;

    private VerticalPanel timeSelectionPanel;
    private Label loadingLabel = new Label("Loading");
    
    public StartEndTimePopup(String layer, String baseUrl, final TimeSelectorIF currentTimeSelector) {
        this.jsonProxyUrl = baseUrl;
        this.layer = layer;

        setAutoHideEnabled(true);
        setModal(true);
        setAnimationEnabled(true);
        setGlassEnabled(true);

        errorMessage = "You need multiple time values to do this";

        setLoading();

        LayerRequestBuilder getLayerDetails = new LayerRequestBuilder(layer, jsonProxyUrl, null);
        getLayerDetails.setCallback(new LayerRequestCallback(layer, new ErrorHandler() {
            @Override
            public void handleError(Throwable e) {
                e.printStackTrace();
            }
        }) {

            @Override
            public void onResponseReceived(Request request, Response response) {
                super.onResponseReceived(request, response);
                availableTimes = new LinkedHashMap<String, List<String>>();
                availableDates = getLayerDetails().getAvailableDates();
                if (availableDates == null || availableDates.size() == 0) {
                    handleNoMultipleTimes();
                    return;
                }
                /*
                 * These need to be in date order. They should already be, but
                 * sort to be safe
                 */
                Collections.sort(availableDates);
                getStartTimeSelector().populateDates(availableDates);
                if (currentTimeSelector != null) {
                    getStartTimeSelector().selectDate(currentTimeSelector.getSelectedDate());
                }
                getEndTimeSelector().populateDates(availableDates);
                getEndTimeSelector().selectDate(availableDates.get(availableDates.size() - 1));
                for (final String date : availableDates) {
                    TimeRequestBuilder getTimeRequest = new TimeRequestBuilder(
                            StartEndTimePopup.this.layer, date, jsonProxyUrl);
                    getTimeRequest.setCallback(new TimeRequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            super.onResponseReceived(request, response);
                            availableTimes.put(date, getAvailableTimesteps());
                            if (date.equals(startTimeSelector.getSelectedDate())) {
                                if (date.equals(availableDates.get(availableDates.size() - 1))
                                        && getAvailableTimesteps().size() > 1) {
                                    startTimeSelector.populateTimes(getAvailableTimesteps()
                                            .subList(0, getAvailableTimesteps().size() - 2));
                                } else {
                                    startTimeSelector.populateTimes(getAvailableTimesteps());
                                }
                            }
                            if (date.equals(endTimeSelector.getSelectedDate())) {
                                endTimeSelector.populateTimes(getAvailableTimesteps());
                                endTimeSelector.selectDateTime(getAvailableTimesteps().get(
                                        getAvailableTimesteps().size() - 1));
                            }
                            if (currentTimeSelector != null) {
                                getStartTimeSelector().selectDateTime(
                                        currentTimeSelector.getSelectedDateTime());
                            }
                        }

                        @Override
                        public void onError(Request request, Throwable exception) {
                            // TODO handle error
                        }
                    });

                    try {
                        getTimeRequest.send();
                    } catch (RequestException e) {
                        // TODO handle error
                    }
                }
                setTimeSelector();
            }
        });

        try {
            getLayerDetails.send();
        } catch (RequestException e) {
            // TODO handle error
        }
    }

    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setTimeSelectionHandler(StartEndTimeHandler timesHandler) {
        this.timesHandler = timesHandler;
    }

    private void handleNoMultipleTimes() {
        setText("Multiple times not found");
        Label errorLabel = new Label(errorMessage);
        errorLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_JUSTIFY);
        errorLabel.setSize("350px", "150px");
        Button closeButton = new Button("Close");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                StartEndTimePopup.this.hide();
                StartEndTimePopup.this.removeFromParent();
            }
        });
        VerticalPanel vP = new VerticalPanel();
        vP.add(errorLabel);
        vP.add(closeButton);
        vP.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
        setWidget(vP);
    }

    private void setTimeSelector() {
        timeSelectionPanel = new VerticalPanel();
        timeSelectionPanel.add(getStartTimeSelector());
        timeSelectionPanel.add(getEndTimeSelector());
        timeSelectionPanel.add(getNextButton());
        timeSelectionPanel.setCellHorizontalAlignment(nextButton,
                HasHorizontalAlignment.ALIGN_CENTER);
        setText("Select start and end times");
        timeSelectionPanel.setSize("350px", "150px");
        setWidget(timeSelectionPanel);
    }

    private TimeSelector getStartTimeSelector() {
        if (startTimeSelector == null) {
            startTimeSelector = new TimeSelector("start_time", "Start time",
                    new TimeDateSelectionHandler() {
                        @Override
                        public void dateSelected(String id, String selectedDate) {
                            startTimeSelector.populateTimes(availableTimes.get(startTimeSelector
                                    .getSelectedDate()));
                            /*
                             * Store the currently selected end date
                             */
                            String selectedEndDate = endTimeSelector.getSelectedDate();
                            /*
                             * Re-populate the end dates with those equal to or
                             * later than the selected start date
                             */
                            endTimeSelector.populateDates(getDatesLaterOrEqualTo(startTimeSelector
                                    .getSelectedDate()));
                            /*
                             * Now try and select the previously selected end
                             * date.
                             * 
                             * If this fails, the first available date will be
                             * selected. This is fine, because it means that
                             * previously we had a date selected which was
                             * before the newly-selected start date
                             */
                            endTimeSelector.selectDate(selectedEndDate);
                        }

                        @Override
                        public void timeSelected(String id, String selectedStartDateTime) {
                            /*
                             * If both start and end times occur on the same
                             * day, remove any later times from the end selector
                             */
                            if (endTimeSelector.getSelectedDate().equals(
                                    startTimeSelector.getSelectedDate())) {
                                String selectedStartTime = startTimeSelector.getSelectedTime();
                                String selectedEndDateTime = endTimeSelector.getSelectedDateTime();
                                List<String> laterTimes = getTimesLaterTo(selectedStartTime,
                                        startTimeSelector.getSelectedDate());
                                if (laterTimes.size() > 0) {
                                    endTimeSelector.populateTimes(laterTimes);
                                    endTimeSelector.selectDateTime(selectedEndDateTime);
                                } else {
                                    /*
                                     * No times are later - i.e. the final time
                                     * for the day has been selected.
                                     * 
                                     * Get dates later or equals to the selected
                                     * date
                                     */
                                    List<String> datesLaterOrEqualTo = getDatesLaterOrEqualTo(endTimeSelector
                                            .getSelectedDate());
                                    /*
                                     * Sort them and remove the first one. This
                                     * should mean that the next day gets
                                     * selected.
                                     */
                                    Collections.sort(datesLaterOrEqualTo);
                                    datesLaterOrEqualTo.remove(0);
                                    endTimeSelector.populateDates(datesLaterOrEqualTo);
                                    endTimeSelector.selectDateTime(datesLaterOrEqualTo.get(0) + "T"
                                            + availableTimes.get(datesLaterOrEqualTo.get(0)).get(0));
                                }
                            }
                        }

                    });
        }
        return startTimeSelector;
    }

    private List<String> getDatesLaterOrEqualTo(String selectedDate) {
        List<String> laterDates = new ArrayList<String>();
        for (String date : availableDates) {
            if (date.compareTo(selectedDate) >= 0) {
                laterDates.add(date);
            }
        }
        return laterDates;
    }

    private List<String> getTimesLaterTo(String selectedTime, String selectedDate) {
        List<String> laterTimes = new ArrayList<String>();
        for (String time : availableTimes.get(selectedDate)) {
            if (time.compareTo(selectedTime) > 0) {
                laterTimes.add(time);
            }
        }
        return laterTimes;
    }

    private TimeSelector getEndTimeSelector() {
        if (endTimeSelector == null) {
            endTimeSelector = new TimeSelector("end_time", "End time",
                    new TimeDateSelectionHandler() {
                        @Override
                        public void timeSelected(String id, String selectedTime) {
                            /*
                             * Do nothing here - we only check the time when the
                             * "Next" button is clicked
                             */
                        }

                        @Override
                        public void dateSelected(String id, String selectedDate) {
                            endTimeSelector.populateTimes(availableTimes.get(endTimeSelector
                                    .getSelectedDate()));
                        }
                    });
        }
        return endTimeSelector;
    }

    private void setLoading() {
        if (loadingLabel == null) {
            loadingLabel = new Label("Loading...");
        }
        loadingLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_JUSTIFY);
        loadingLabel.setSize("350px", "150px");
        setText("Loading details");
        setWidget(loadingLabel);
    }

    private Button getNextButton() {
        if (nextButton == null) {
            if (buttonLabel != null)
                nextButton = new Button(buttonLabel);
            else
                nextButton = new Button("OK");
            nextButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (timesHandler != null) {
                        timesHandler.timesReceived(startTimeSelector.getSelectedDateTime(),
                                endTimeSelector.getSelectedDateTime());
                    }
                }
            });
        }
        return nextButton;
    }
}
