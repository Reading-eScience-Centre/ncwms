package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ErrorBox extends DialogBox {
    
    public static void popupErrorMessage(String message){
        ErrorBox errorBox = new ErrorBox(message);
        errorBox.center();
    }
    
    private ErrorBox(String message){
        super();
        setText("Error");
        VerticalPanel panel = new VerticalPanel();
        Label messageLabel = new Label(message);
        messageLabel.setWordWrap(true);
        Button closeButton = new Button("Close");
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ErrorBox.this.hide();
            }
        });
        panel.add(messageLabel);
        panel.setCellHorizontalAlignment(messageLabel, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(messageLabel, HasVerticalAlignment.ALIGN_MIDDLE);
        panel.add(closeButton);
        panel.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(closeButton, HasVerticalAlignment.ALIGN_MIDDLE);
        panel.setWidth("200px");
        panel.setHeight("150px");
        setWidget(panel);
    }
}
