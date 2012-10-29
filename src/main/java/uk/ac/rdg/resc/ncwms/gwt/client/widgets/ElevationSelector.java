package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.ElevationSelectionHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.ListBox;

public class ElevationSelector extends BaseSelector implements ElevationSelectorIF {
	private ListBox elevations;
    private final NumberFormat format = NumberFormat.getFormat("#0.##");
    private Map<String, String> formattedValuesToRealValues;
    private String id;
    private String units;
    private boolean continuous;
//    private String continuousRange = null;
    
	public ElevationSelector(String id, String title, final ElevationSelectionHandler handler) {
		super(title);
		this.id = id;
		
		elevations = new ListBox();
		elevations.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handler.elevationSelected(ElevationSelector.this.id, getSelectedElevation());
            }
        });
		add(elevations);
		
	}
	
	@Override
    public void setId(String id) {
	    this.id = id;
    }

    @Override
    public void populateElevations(List<String> availableElevations){
        String currentElevation = getSelectedElevation();
		elevations.clear();
		formattedValuesToRealValues = new HashMap<String, String>();
		if(availableElevations == null || availableElevations.size()==0){
			label.setStylePrimaryName("inactiveLabelStyle");
			elevations.setEnabled(false);
		} else {
		    if(continuous){
		        if(availableElevations.size() != 2){
                    throw new IllegalArgumentException(
                            "For a continuous elevation axis, you must provide exactly 2 elevation");
                }
		        double firstVal = Double.parseDouble(availableElevations.get(0));
		        double secondVal = Double.parseDouble(availableElevations.get(1));
		        double dZ = getOptimumDz(firstVal, secondVal, 20);
		        int i = 0;
		        for(double v = firstVal + 0.5*dZ; v <= secondVal; v+=dZ){
		            String formattedElevationStr = format.format(v); 
		            String formattedElevationRange = format.format(v-0.5*dZ)+"/"+format.format(v+0.5*dZ); 
		            elevations.addItem(formattedElevationStr);
		            formattedValuesToRealValues.put(formattedElevationStr, formattedElevationRange);
		            if(formattedElevationStr.equals(currentElevation)){
		                elevations.setSelectedIndex(i);
		            }
		            i++;
		        }
		    } else {
		        int i=0;
		        for(String elevationStr : availableElevations){
		            Float elevation = Float.parseFloat(elevationStr);
		            String formattedElevationStr = format.format(elevation); 
		            elevations.addItem(formattedElevationStr);
		            formattedValuesToRealValues.put(formattedElevationStr, elevationStr);
		            if(elevationStr.equals(currentElevation)){
		                elevations.setSelectedIndex(i);
		            }
		            i++;
		        }
		    }
		    label.setStylePrimaryName("labelStyle");
		    elevations.setEnabled(true);
		}
	}
	
	private double getOptimumDz(double firstVal, double secondVal, int numberOfSteps) {
	    double dz = (secondVal - firstVal)/numberOfSteps;
	    double[] niceSteps = new double[]{1e-3,1e-2,1e-1,1,5,10,20,50,100,250,500,1000,10000};
	    double last = dz;
	    for(double test : niceSteps){
	        if(dz > test) {
	            last = test;
	            continue;
	        } else {
	            dz = last;
	            break;
	        }
	    }
        return dz;
    }

    @Override
    public void setUnitsAndDirection(String units, boolean positive){
	    this.units = units;
	    if(positive) {
	        label.setText("Elevation");
	        elevations.setTitle("Select the elevation");
	    } else {
	        label.setText("Depth");
	        elevations.setTitle("Select the depth");
	    }
	    if(units != null){
	        label.setText(label.getText()+" ("+units+"):");
	    }else{
	        label.setText(label.getText()+":");
	    }
	}
	
	@Override
    public String getSelectedElevation(){
	    if(!elevations.isEnabled()) return null;
	    int index = elevations.getSelectedIndex();
	    if(index < 0)
	        return null;
	    if(continuous){
	        return elevations.getValue(index);
	    } else {
	        return formattedValuesToRealValues.get(elevations.getValue(index));
	    }
	}

    @Override
    public void setSelectedElevation(String currentElevation) {
        for(int i=0; i < elevations.getItemCount(); i++){
            String elevation = elevations.getValue(i);
            if(currentElevation.equals(elevation)){
                elevations.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(elevations.getItemCount() > 1)
            elevations.setEnabled(enabled);
        else 
            elevations.setEnabled(false);
        
        if(!elevations.isEnabled()){
            label.setStylePrimaryName("inactiveLabelStyle");
        } else {
            label.setStylePrimaryName("labelStyle");
        }
    }

    @Override
    public int getNElevations() {
        return elevations.getItemCount();
    }

    @Override
    public String getVerticalUnits() {
        return units;
    }

    @Override
    public String getSelectedElevationRange() {
//        return continuousRange;
        if(!elevations.isEnabled()) return null;
        int index = elevations.getSelectedIndex();
        if(index < 0)
            return null;
        return formattedValuesToRealValues.get(elevations.getValue(index));
    }

    @Override
    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }
    @Override
    public boolean isContinuous() {
        return continuous;
    }
}
