package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.ErrorHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerRequestCallback;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestBuilder;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.TimeRequestCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AnimationButton extends ToggleButton {
    
    private MapArea map;
    private DialogBox popup;
    private String animLayer = null;
    private String jsonProxyUrl;
    private String currentElevation;
    private String palette;
    private String scaleRange;
    private int nColorBands;
    private boolean logScale;
    private Button nextButton;
    private Button goButton;
    private Button cancelButton;
    private TimeSelector startTimeSelector;
    private TimeSelector endTimeSelector;
    private List<String> availableDates;
    private Map<String, List<String>> availableTimes;
    
    private VerticalPanel timeSelectionPanel;
    private Label loadingLabel = new Label("Loading");
    private VerticalPanel granularitySelectionPanel;
    private ListBox granularitySelector;
    
    private boolean completed;
    private String style;
    
    public AnimationButton(final MapArea map, final String jsonProxyUrl) {
        super(new Image("img/film.png"), new Image("img/stop.png"));
        super.setTitle("Open the animation wizard");

        this.map = map;
        this.jsonProxyUrl = jsonProxyUrl;
        
        this.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if(AnimationButton.this.isDown()){
                    completed = false;
                    popup = getWizard();
                    popup.center();
                    AnimationButton.this.setTitle("Stop the animation");
                } else {
                    AnimationButton.this.setTitle("Open the animation wizard");
                    map.stopAnimation();
                }
            }
        });
    }
    
    public void updateDetails(String layer, String currentElevation, String palette, String style, String scaleRange, int nColorBands, boolean logScale){
        this.animLayer = layer;
        this.currentElevation = currentElevation;
        this.palette = palette;
        this.style = style;
        this.scaleRange = scaleRange;
        this.nColorBands = nColorBands;
        this.logScale = logScale;
    }
    
    private void startAnimation(String times) {
        map.addAnimationLayer(animLayer, times, currentElevation, palette, style, scaleRange, nColorBands, logScale);
        popup.removeFromParent();
        timeSelectionPanel = null;
        granularitySelectionPanel = null;
        nextButton = null;
        startTimeSelector = null;
        endTimeSelector = null;
        granularitySelector = null;
        goButton = null;
        this.setTitle("Stop animation");
    }
    
    private DialogBox getWizard(){
        if(popup == null){
            popup = new DialogBox();
            popup.setAutoHideEnabled(true);
            popup.setModal(true);
            popup.setAnimationEnabled(true);
            popup.setGlassEnabled(true);
            popup.addCloseHandler(new CloseHandler<PopupPanel>() {
                @Override
                public void onClose(CloseEvent<PopupPanel> event) {
                    if(!completed)
                        AnimationButton.this.setDown(false);
                }
            });
        }

        if(animLayer == null){
           setNoAnimationPossible("Please select a layer before trying to create an animation");
           return popup;
        }
        
        setLoading();
        
        LayerRequestBuilder getLayerDetails = new LayerRequestBuilder(animLayer, jsonProxyUrl, null);
        getLayerDetails.setCallback(new LayerRequestCallback(animLayer, new ErrorHandler() {
            @Override
            public void handleError(Throwable e) {
                e.printStackTrace();
            }
        }) {
            
            public void onResponseReceived(Request request, Response response) {
                super.onResponseReceived(request, response);
                availableTimes = new HashMap<String, List<String>>();
                availableDates = getLayerDetails().getAvailableDates();
                if(availableDates == null || availableDates.size() < 2){
                    setNoAnimationPossible("We do not have multiple time data to create an animation.  "
                            + "Please choose a variable with multiple time values");
                    return;
                }
                getStartTimeSelector().populateDates(availableDates);
                getEndTimeSelector().populateDates(availableDates);
                for(final String date : availableDates){
                    TimeRequestBuilder getTimeRequest = new TimeRequestBuilder(animLayer, date, jsonProxyUrl);
                    getTimeRequest.setCallback(new TimeRequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            super.onResponseReceived(request, response);
                            availableTimes.put(date, getAvailableTimesteps());
                            if(date.equals(startTimeSelector.getSelectedDate())){
                                startTimeSelector.populateTimes(getAvailableTimesteps());
                            }
                            if(date.equals(endTimeSelector.getSelectedDate())){
                                endTimeSelector.populateTimes(getAvailableTimesteps());
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
        

        try{
            getLayerDetails.send();
        } catch (RequestException e) {
            // TODO handle error
        }
        
        return popup;
    }
    
    protected TimeSelector getStartTimeSelector(){
        if(startTimeSelector == null){
            startTimeSelector = new TimeSelector("Start time", new TimeDateSelectionHandler() {
                @Override
                public void timeSelected(String id, String selectedTime) {
                    /*
                     * Do nothing here - we only check the time when the "Next" button is clicked
                     */
                }
                
                @Override
                public void dateSelected(String id, String selectedDate) {
                    startTimeSelector.populateTimes(availableTimes.get(startTimeSelector.getSelectedDate()));
                }
            });
        }
        return startTimeSelector;
    }

    protected TimeSelector getEndTimeSelector(){
        if(endTimeSelector == null){
            endTimeSelector = new TimeSelector("End time", new TimeDateSelectionHandler() {
                @Override
                public void timeSelected(String id, String selectedTime) {
                    /*
                     * Do nothing here - we only check the time when the "Next" button is clicked
                     */
                }
                
                @Override
                public void dateSelected(String id, String selectedDate) {
                    endTimeSelector.populateTimes(availableTimes.get(endTimeSelector.getSelectedDate()));
                }
            });
        }
        return endTimeSelector;
    }
    
    protected Button getNextButton(){
        if(nextButton == null){
            nextButton = new Button("Next >");
            nextButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    setLoading();
                    String url = URL.encode(jsonProxyUrl + "?request=GetMetadata&item=animationTimesteps&layerName="
                            + animLayer + "&start=" + startTimeSelector.getSelectedDateTime() + "&end="
                            + endTimeSelector.getSelectedDateTime());
                    RequestBuilder getAnimTimestepsRequest = new RequestBuilder(RequestBuilder.GET, url);
                    getAnimTimestepsRequest.setCallback(new RequestCallback() {
                        @Override
                        public void onResponseReceived(Request request, Response response) {
                            JSONValue jsonMap = JSONParser.parseLenient(response.getText());
                            JSONObject parentObj = jsonMap.isObject();

                            JSONValue timesJson = parentObj.get("timeStrings");
                            
                            granularitySelector = new ListBox();
                            if (timesJson != null) {
                                JSONArray timesArr = timesJson.isArray();
                                for(int i=0; i < timesArr.size(); i++){
                                    JSONObject timeObj = timesArr.get(i).isObject();
                                    String title = timeObj.get("title").isString().stringValue();
                                    String value = timeObj.get("timeString").isString().stringValue();
                                    granularitySelector.addItem(title,value);
                                }
                            }
                            setGranularitySelector();
                        }
                        
                        @Override
                        public void onError(Request request, Throwable exception) {
                            // TODO Auto-generated method stub
                            
                        }
                    });
                    try {
                        getAnimTimestepsRequest.send();
                    } catch (RequestException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }
        return nextButton;
    }
    
    protected Button getGoButton(){
        if(goButton == null){
            goButton = new Button("Go");
            goButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    completed = true;
                    startAnimation(granularitySelector.getValue(granularitySelector.getSelectedIndex()));
                }
            });
        }
        return goButton;
    }
    
    protected Button getCancelButton(){
        if(cancelButton == null){
            cancelButton = new Button("Cancel");
            cancelButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    popup.hide();
                }
            });
        }
        return cancelButton;
    }
    
    private void setLoading(){
        if(loadingLabel == null){
            loadingLabel = new Label("Loading...");
        }
        loadingLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_JUSTIFY);
        loadingLabel.setSize("350px", "150px");
        popup.setText("Loading details");
        popup.setWidget(loadingLabel);
    }
    
    private void setNoAnimationPossible(String message){
        popup.setText("Cannot create animation");
        Label errorLabel = new Label(message);
        errorLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_JUSTIFY);
        errorLabel.setSize("350px", "150px");
        Button closeButton = new Button("Close");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                completed = false;
                popup.hide();
                popup.removeFromParent();
            }
        });
        VerticalPanel vP = new VerticalPanel();
        vP.add(errorLabel);
        vP.add(closeButton);
        vP.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
        popup.setWidget(vP);
    }
    
    private void setTimeSelector(){
        timeSelectionPanel = new VerticalPanel();
        timeSelectionPanel.add(getStartTimeSelector());
        timeSelectionPanel.add(getEndTimeSelector());
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.add(getCancelButton());
        buttonPanel.add(getNextButton());
        timeSelectionPanel.add(buttonPanel);
        timeSelectionPanel.setCellHorizontalAlignment(buttonPanel,  HasHorizontalAlignment.ALIGN_CENTER);
        popup.setText("Select start and end times");
        timeSelectionPanel.setSize("350px", "150px");
        popup.setWidget(timeSelectionPanel);
    }
    
    private void setGranularitySelector(){
        granularitySelectionPanel = new VerticalPanel();
        Label infoLabel = new Label("The more frames you choose the longer your animation will take to load."
                + " Please choose the smallest number you think you need!");
        granularitySelectionPanel.add(infoLabel);
        granularitySelectionPanel.add(granularitySelector);
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.add(getCancelButton());
        buttonPanel.add(getGoButton());
        granularitySelectionPanel.add(buttonPanel);
        granularitySelectionPanel.setCellHorizontalAlignment(infoLabel, HasHorizontalAlignment.ALIGN_CENTER);
        granularitySelectionPanel.setCellHorizontalAlignment(granularitySelector, HasHorizontalAlignment.ALIGN_CENTER);
        granularitySelectionPanel.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_CENTER);
        granularitySelectionPanel.setSize("350px", "150px");
        popup.setText("Select the time resolution");
        popup.setWidget(granularitySelectionPanel);
    }
}
