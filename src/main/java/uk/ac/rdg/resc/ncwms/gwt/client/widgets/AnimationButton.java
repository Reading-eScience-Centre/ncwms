package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.AviExportHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.handlers.StartEndTimeHandler;

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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AnimationButton extends ToggleButton {

    private MapArea map;
    private StartEndTimePopup popup;
    private String animLayer = null;
    private String jsonProxyUrl;
    private String currentElevation;
    private String palette;
    private String scaleRange;
    private int nColorBands;
    private boolean logScale;
    private Button goButton;
    private Button cancelButton;
    
    private final TimeSelectorIF currentTimeSelector;

    private VerticalPanel detailsSelectionPanel;
    private ListBox granularitySelector;
    private ListBox fpsSelector;
    private VerticalPanel formatSelector;
    private RadioButton overlayRadioButton;
    private RadioButton aviRadioButton;

    private boolean completed;
    private String style;
    
    private final AviExportHandler aviExporter;

    public AnimationButton(final MapArea map, final String jsonProxyUrl, final TimeSelectorIF currentTimeSelector, AviExportHandler aviExporter) {
        super(new Image("img/film.png"), new Image("img/stop.png"));
        super.setTitle("Open the animation wizard");

        this.map = map;
        this.jsonProxyUrl = jsonProxyUrl;
        this.currentTimeSelector = currentTimeSelector;
        this.aviExporter = aviExporter;

        this.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (AnimationButton.this.isDown()) {
                    completed = false;
                    popup = getWizard();
                    popup.center();
                    AnimationButton.this.setTitle("Stop the animation");
                } else {
                    AnimationButton.this.setTitle("Open the animation wizard");
                    map.stopAnimation();
                    AnimationButton.this.aviExporter.animationStopped();
                }
            }
        });
        
        formatSelector = new VerticalPanel();
        overlayRadioButton = new RadioButton("formatSelector", "Overlay");
        overlayRadioButton.setValue(true);
        aviRadioButton = new RadioButton("formatSelector", "Export to AVI");
        formatSelector.add(overlayRadioButton);
        formatSelector.add(aviRadioButton);
        
        fpsSelector = new ListBox();
        fpsSelector.addItem("1fps", "1");
        fpsSelector.addItem("2fps", "2");
        fpsSelector.addItem("5fps", "5");
        fpsSelector.addItem("10fps", "10");
        fpsSelector.addItem("15fps", "15");
        fpsSelector.addItem("24fps", "24");
        fpsSelector.addItem("30fps", "30");
        fpsSelector.setSelectedIndex(3);
        
        /*
         * We disable the animation button until we have a layer selected
         */
        setEnabled(false);
    }

    public void updateDetails(String layer, String currentElevation, String palette, String style,
            String scaleRange, int nColorBands, boolean logScale) {
        if(layer == null)
            this.setEnabled(false);
        else
            this.setEnabled(true);
        this.animLayer = layer;
        this.currentElevation = currentElevation;
        this.palette = palette;
        this.style = style;
        this.scaleRange = scaleRange;
        this.nColorBands = nColorBands;
        this.logScale = logScale;
    }

    private void startAnimation(String times, String frameRate, boolean overlay) {
        if(overlay){
            map.addAnimationLayer(animLayer, times, currentElevation, palette, style, scaleRange,
                    nColorBands, logScale, frameRate);
            this.setTitle("Stop animation");
        } else {
            Window.open(aviExporter.getAviUrl(times, frameRate), null, null);
            this.setDown(false);
        }
        popup.removeFromParent();
        popup = null;
        detailsSelectionPanel = null;
        granularitySelector = null;
        goButton = null;
        aviExporter.animationStarted(times, frameRate);
    }

    private StartEndTimePopup getWizard() {
        popup = new StartEndTimePopup(animLayer, jsonProxyUrl, currentTimeSelector);
        popup.setErrorMessage("Can only create animation where there are multiple times");
        popup.setButtonLabel("Next >");
        popup.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                if (!completed)
                    AnimationButton.this.setDown(false);
            }
        });

        if (animLayer == null) {
            setNoAnimationPossible("Please select a layer before trying to create an animation");
            return popup;
        }

        popup.setTimeSelectionHandler(new StartEndTimeHandler() {
            @Override
            public void timesReceived(String startDateTime, String endDateTime) {
                String url = URL.encode(jsonProxyUrl
                        + "?request=GetMetadata&item=animationTimesteps&layerName=" + animLayer
                        + "&start=" + startDateTime + "&end=" + endDateTime);
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
                            for (int i = 0; i < timesArr.size(); i++) {
                                JSONObject timeObj = timesArr.get(i).isObject();
                                String title = timeObj.get("title").isString().stringValue();
                                String value = timeObj.get("timeString").isString().stringValue();
                                granularitySelector.addItem(title, value);
                            }
                        }
                        
                        setDetailsSelector();
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

        return popup;
    }

    protected Button getGoButton() {
        if (goButton == null) {
            goButton = new Button("Go");
            goButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    completed = true;
                    String times = granularitySelector.getValue(granularitySelector.getSelectedIndex());
                    String frameRate = fpsSelector.getValue(fpsSelector.getSelectedIndex());
                    startAnimation(times, frameRate, overlayRadioButton.getValue());
                }
            });
        }
        return goButton;
    }

    protected Button getCancelButton() {
        if (cancelButton == null) {
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

    private void setNoAnimationPossible(String message) {
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

    private void setDetailsSelector() {
        detailsSelectionPanel = new VerticalPanel();
        Label infoLabel = new Label(
                "The more frames you choose the longer your animation will take to load."
                        + " Please choose the smallest number you think you need!");
        detailsSelectionPanel.add(infoLabel);
        
        HorizontalPanel granPan = new HorizontalPanel();
        Label granLabel = new Label("Granularity:");
        granPan.add(granLabel);
        granPan.add(granularitySelector);
        granPan.setCellWidth(granLabel, "40%");
        granPan.setCellHorizontalAlignment(granLabel, HasHorizontalAlignment.ALIGN_RIGHT);
        granPan.setCellHorizontalAlignment(granularitySelector, HasHorizontalAlignment.ALIGN_LEFT);
        granPan.setWidth("100%");
        detailsSelectionPanel.add(granPan);
        
        HorizontalPanel formatPan = new HorizontalPanel();
        Label formatLabel = new Label("Type of animation:");
        formatPan.add(formatLabel);
        formatPan.add(formatSelector);
        formatPan.setCellWidth(formatLabel, "40%");
        formatPan.setCellVerticalAlignment(formatLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        formatPan.setCellHorizontalAlignment(formatLabel, HasHorizontalAlignment.ALIGN_RIGHT);
        formatPan.setCellHorizontalAlignment(formatSelector, HasHorizontalAlignment.ALIGN_LEFT);
        formatPan.setWidth("100%");
        detailsSelectionPanel.add(formatPan);
        
        HorizontalPanel fpsPan = new HorizontalPanel();
        Label fpsLabel = new Label("Frame Rate:");
        fpsPan.add(fpsLabel);
        fpsPan.add(fpsSelector);
        fpsPan.setCellWidth(fpsLabel, "40%");
        fpsPan.setCellHorizontalAlignment(fpsLabel, HasHorizontalAlignment.ALIGN_RIGHT);
        fpsPan.setCellHorizontalAlignment(fpsSelector, HasHorizontalAlignment.ALIGN_LEFT);
        fpsPan.setWidth("100%");
        detailsSelectionPanel.add(fpsPan);
        
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.add(getCancelButton());
        buttonPanel.add(getGoButton());
        detailsSelectionPanel.add(buttonPanel);
        detailsSelectionPanel.setCellHorizontalAlignment(buttonPanel,
                HasHorizontalAlignment.ALIGN_CENTER);
        detailsSelectionPanel.setSize("350px", "150px");
        popup.setText("Select the time resolution");
        popup.setWidget(detailsSelectionPanel);
    }
}
