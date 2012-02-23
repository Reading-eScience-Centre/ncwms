package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.PaletteSelectionHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class PaletteSelector extends HorizontalPanel implements PaletteSelectorIF {
	private TextBox minScale;
	private TextBox maxScale;
	private ListBox nColorBands;
	private ListBox logScale;
	private PushButton autoButton;
	private ToggleButton lockButton;
	private Label mhLabel;
	private Label mlLabel;
	private final NumberFormat format = NumberFormat.getFormat("#0.000");
	
	private List<String> availablePalettes;
	private String currentPalette;
	private int mapHeight;
	
	private Image paletteImage;
	private String baseUrl;
	private final PaletteSelectionHandler paletteHandler;
	
	private DialogBox popup;
	private HorizontalPanel palettesPanel;
	
	private boolean enabled;
	
	private String id;
	
	public PaletteSelector(String id, int mapHeight, 
	                       final PaletteSelectionHandler handler,
	                       String baseUrl) {
	    this.id = id;
		this.baseUrl = baseUrl;
		this.mapHeight = mapHeight;
		this.paletteHandler = handler;
		
		enabled = true;
		
        nColorBands = new ListBox();
        nColorBands.addItem("10");
        nColorBands.addItem("20");
        nColorBands.addItem("50");
        nColorBands.addItem("100");
        nColorBands.addItem("254");
        nColorBands.setTitle("Select the number of colour bands to use for this data");
        
        paletteImage = new Image();
        paletteImage.setWidth("30px");
        paletteImage.setHeight(mapHeight+"px");
        paletteImage.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if(enabled && !isLocked())
                    popupPaletteSelector();
            }
        });
        paletteImage.setTitle("Click to choose palette and number of colour bands");
        
        add(paletteImage);
        
        VerticalPanel vp = new VerticalPanel();
        vp.setHeight(mapHeight+"px");
        vp.setWidth("70px");
        
        ChangeHandler scaleChangeHandler = new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handler.scaleRangeChanged(PaletteSelector.this.id, getScaleRange());
                setScaleLabels();
            }
        };
        
		maxScale = new TextBox();
		maxScale.setWidth("60px");
		maxScale.addChangeHandler(scaleChangeHandler);
		maxScale.setTitle("The maximum value of the colour range");
		maxScale.setMaxLength(8);
		
		logScale = new ListBox();
		logScale.addItem("linear");
		logScale.addItem("log");
		logScale.setWidth("60px");
		logScale.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                if(isLogScale() && Float.parseFloat(minScale.getValue()) <= 0) {
                    setLogScale(false);
                    ErrorBox.popupErrorMessage("Cannot use a negative or zero value for logarithmic scale");
                } else {
                    setScaleLabels();
                    handler.logScaleChanged(PaletteSelector.this.id, isLogScale());
                }
            }
        });
		logScale.setTitle("Choose between a linear and a logarithmic scale");

		minScale = new TextBox();
		minScale.setWidth("60px");
		minScale.addChangeHandler(scaleChangeHandler);
		minScale.setTitle("The minimum value of the colour range");
		minScale.setMaxLength(8);

		HorizontalPanel buttonsPanel = new HorizontalPanel();
		autoButton = new PushButton(new Image("img/color_wheel.png"));
		autoButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handler.autoAdjustPalette(PaletteSelector.this.id);
            }
        });
		autoButton.setTitle("Auto-adjust the colour range");
		lockButton = new ToggleButton(new Image("img/lock_open.png"),new Image("img/lock.png"));
		lockButton.setTitle("Lock the colour range");
		lockButton.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                boolean unlocked = !event.getValue();
                logScale.setEnabled(unlocked);
                autoButton.setEnabled(unlocked);
                maxScale.setEnabled(unlocked);
                minScale.setEnabled(unlocked);
                lockButton.setTitle(unlocked ? "Lock the colour range" : "Unlock the colour range");
            }
        });
		
		mhLabel = new Label();
		mhLabel.setStylePrimaryName("tickmark");
		mlLabel = new Label();
		mlLabel.setStylePrimaryName("tickmark");
		
		buttonsPanel.add(autoButton);
		buttonsPanel.add(lockButton);
		
		vp.add(maxScale);
		vp.setCellHeight(maxScale, "30px");
		vp.setCellVerticalAlignment(maxScale, ALIGN_TOP);
		vp.add(mhLabel);
		vp.setCellVerticalAlignment(mhLabel, ALIGN_BOTTOM);
		
		vp.add(logScale);
		vp.setCellHeight(logScale, "60px");
		vp.setCellVerticalAlignment(logScale, ALIGN_BOTTOM);
		vp.add(buttonsPanel);
		vp.setCellHeight(buttonsPanel, "60px");
		vp.setCellVerticalAlignment(buttonsPanel, ALIGN_TOP);

		vp.add(mlLabel);
		vp.setCellVerticalAlignment(mlLabel, ALIGN_TOP);
		
		vp.add(minScale);
		vp.setCellHeight(minScale, "30px");
		vp.setCellVerticalAlignment(minScale, ALIGN_BOTTOM);
		
		add(vp);
	}
	
	private void popupPaletteSelector() {
	    if(popup == null){
	        popup = new DialogBox();
	    
	        nColorBands.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    populatePaletteSelector();
                }
            });
	        
	        populatePaletteSelector();
	    }
        VerticalPanel popupPanel = new VerticalPanel();
        HorizontalPanel nCBPanel = new HorizontalPanel();
        nCBPanel.add(new Label("Colour bands:"));
        nCBPanel.add(nColorBands);
        popupPanel.add(nCBPanel);
        popupPanel.setCellHorizontalAlignment(nCBPanel, ALIGN_CENTER);
        popupPanel.add(palettesPanel);
        
        popup.setAutoHideEnabled(true);
        popup.setText("Click to choose a colour palette");
        popup.setModal(true);
        popup.setWidget(popupPanel);
        popup.setAnimationEnabled(true);
        popup.setGlassEnabled(true);
        popup.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {
                selectPalette(currentPalette);
                paletteHandler.paletteChanged(id, currentPalette, getNumColorBands());
            }
        });
        popup.center();
    }
	
	private void populatePaletteSelector() {
	    if(palettesPanel == null){
	        palettesPanel = new HorizontalPanel();
	    }
	    palettesPanel.clear();
	    for(final String palette : availablePalettes){
	        Image pImage = new Image(getImageUrl(palette, 200));
	        pImage.setHeight("200px");
	        pImage.setWidth("30px");
	        pImage.addClickHandler(new ClickHandler() {
	            @Override
	            public void onClick(ClickEvent event) {
	                selectPalette(palette);
	                popup.hide();
	            }
	        });
	        pImage.setTitle(palette);
	        palettesPanel.add(pImage);
	    }
    }

    private String getImageUrl(String paletteName, int height){
	    String url = "?request=GetLegendGraphic"
	        +"&height="+height
	        +"&width=1"
	        +"&numcolorbands="+getNumColorBands()
	        +"&colorbaronly=true"
	        +"&palette="+paletteName;
	    return URL.encode(baseUrl+url);
	}

    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    public void populatePalettes(List<String> availablePalettes){
	    this.availablePalettes = availablePalettes;
	    setEnabled(true);
	}
	
	public String getSelectedPalette(){
	    return currentPalette;
	}
	
	public void selectPalette(String paletteString){
	    currentPalette = paletteString;
        paletteImage.setUrl(getImageUrl(paletteString, mapHeight));
	}
	
	public boolean setScaleRange(String scaleRange) {
	    String[] vals = scaleRange.split(",");
	    float minVal = Float.parseFloat(vals[0]);
	    if(isLogScale() && minVal <= 0){
	        ErrorBox.popupErrorMessage("Cannot use a negative or zero value for logarithmic scale");
	        return false;
	    }
	    minScale.setValue(format.format(minVal));
	    maxScale.setValue(format.format(Float.parseFloat(vals[1])));
	    setScaleLabels();
	    return true;
	}
	
	public void setScaleLabels(){
	    boolean log = logScale.getSelectedIndex() == 1;
	    double min = log ? Math.log(Double.parseDouble(minScale.getValue())) : Double.parseDouble(minScale.getValue());
	    double max = log ? Math.log(Double.parseDouble(maxScale.getValue())) : Double.parseDouble(maxScale.getValue());
	    double third = (max-min)/3;
	    double sOneThird = log ? Math.exp(min + third) : min + third;
	    double sTwoThird = log ? Math.exp(min + 2*third) : min + 2*third;
	    mlLabel.setText(format.format(sOneThird));
	    mhLabel.setText(format.format(sTwoThird));
	}

    public String getScaleRange() {
        return minScale.getValue()+","+maxScale.getValue();
    }

    public int getNumColorBands() {
        return Integer.parseInt(nColorBands.getValue(nColorBands.getSelectedIndex()));
    }
    
    public void setNumColorBands(int nBands){
        int diff = 254*254;
        int minIndex = 0;
        for(int i=0; i< nColorBands.getItemCount(); i++){
            int value = Integer.parseInt(nColorBands.getValue(i));
            if(value == nBands){
                nColorBands.setSelectedIndex(i);
                return;
            } else {
                int abs = (value-nBands)*(value-nBands);
                if(abs < diff){
                    diff = abs;
                    minIndex = i;
                }
            }
        }
        nColorBands.setSelectedIndex(minIndex);
    }

    public void setLogScale(boolean logScale) {
        this.logScale.setSelectedIndex(logScale ? 1 : 0);
    }

    public boolean isLogScale() {
        if (logScale.getSelectedIndex() == 0) {
            // Linear scale
            return false;
        } else {
            // Log scale
            return true;
        }
    }
    
    public boolean isLocked() {
        return lockButton.getValue();
    }

    public void setEnabled(boolean enabled) {
        minScale.setEnabled(enabled);
        maxScale.setEnabled(enabled);
        autoButton.setEnabled(enabled);
        lockButton.setEnabled(enabled);
        logScale.setEnabled(enabled);
        this.enabled = enabled;
    }
}
