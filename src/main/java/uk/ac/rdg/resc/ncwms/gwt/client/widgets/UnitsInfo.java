package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.Label;

public class UnitsInfo extends BaseSelector {
    private Label units;
    public UnitsInfo() {
        super("Units");
        units = new Label();
        units.setStylePrimaryName("lightLabel");
        label.setTitle("Units of measurement for the data");
        add(units);
    }
    
    public void setUnits(String units){
        this.units.setText(units);
        this.units.setTitle("The units of the data are "+units);
        setEnabled(true);
    }
    
    public void setEnabled(boolean enabled){
        if(enabled){
            label.setStylePrimaryName("labelStyle");
        } else {
            label.setStylePrimaryName("inactiveLabelStyle");
        }
    }
}
