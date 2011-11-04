package uk.ac.rdg.resc.ncwms.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Godiva implements EntryPoint {

    @Override
    public void onModuleLoad() {
        RootLayoutPanel mainWindow = RootLayoutPanel.get();
        mainWindow.add(new Label("This is a test..."));
    }

}
