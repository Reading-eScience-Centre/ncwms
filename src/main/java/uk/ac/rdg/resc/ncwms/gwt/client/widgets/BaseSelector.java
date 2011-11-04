package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class BaseSelector extends HorizontalPanel {
	protected Label label;

	public BaseSelector(String title) {
		super();
		
		label = new Label(title+":");
		label.setStylePrimaryName("labelStyle");
		label.setWordWrap(true);
		add(label);
	}
	
	public void setTitle(String title){
		label.setText(title+":");
	}
}
