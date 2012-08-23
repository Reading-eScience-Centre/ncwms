package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.Label;

public class Info extends BaseSelector implements InfoIF {
    private Label info;
    public Info() {
        super("Copyright");
        info = new Label();
        info.setStylePrimaryName("lightLabel");
        label.setTitle("Information about the data");
        add(info);
    }
    
    @Override
    public void setInfo(String info){
        this.info.setText(info);
        this.info.setTitle("Information about the data");
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
    public boolean hasInfo() {
        return (info.getText() != null && !info.getText().equals(""));
    }

    @Override
    public String getInfo() {
        return info.getText();
    }
}
