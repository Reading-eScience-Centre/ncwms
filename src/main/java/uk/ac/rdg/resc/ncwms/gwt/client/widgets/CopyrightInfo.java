package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.Label;

public class CopyrightInfo extends BaseSelector implements CopyrightInfoIF {
    private Label copyright;
    public CopyrightInfo() {
        super("Copyright");
        copyright = new Label();
        copyright.setStylePrimaryName("lightLabel");
        label.setTitle("Information about the copyright of this dataset");
        add(copyright);
    }
    
    @Override
    public void setCopyrightInfo(String copyright){
        this.copyright.setText(copyright);
        this.copyright.setTitle("Copyright information about the current dataset");
        setEnabled(true);
    }
    
    @Override
    public void setEnabled(boolean enabled){
        if(enabled){
            label.setStylePrimaryName("labelStyle");
        } else {
            label.setStylePrimaryName("inactiveLabelStyle");
        }
    }

    @Override
    public boolean hasCopyright() {
        return (copyright.getText() != null && !copyright.getText().equals(""));
    }

    @Override
    public String getCopyrightInfo() {
        return copyright.getText();
    }
}
