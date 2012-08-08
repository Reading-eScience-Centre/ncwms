package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

public class LayerSelector extends BaseSelector {
	private ListBox variables;
	
	public LayerSelector(String title, final LayerSelectionHandler handler) {
		super(title);
		variables = new ListBox();
		variables.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handler.layerSelected(LayerSelector.this.getSelectedId(), true);
            }
        });
		variables.setTitle("Choose the variable you wish to display");
		variables.setWidth("100%");
		add(variables);
	}
	
	public void populateVariables(Map<String, String> availableLayers){
		variables.clear();
		if(availableLayers == null || availableLayers.size() == 0){
			variables.setEnabled(false);
		} else {
			for(String item : availableLayers.keySet()){
				variables.addItem(availableLayers.get(item), item);
			}
			variables.setEnabled(true);
		}
	}
	
	public String getSelectedId(){
		int i = variables.getSelectedIndex();
		if(i != -1){
			return variables.getValue(variables.getSelectedIndex());
		} else {
			return null;
		}
	}

    public void setSelectedLayer(String currentLayerId) {
        for(int i=0; i < variables.getItemCount(); i++){
            if(currentLayerId.equals(variables.getValue(i))){
                variables.setSelectedIndex(i);
                return;
            }
        }
    }
    
    public void setEnabled(boolean enabled){
        if(variables.getItemCount() == 0)
            enabled = false;
        variables.setEnabled(enabled);
    }
}
