package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.user.client.ui.IsWidget;

public interface CopyrightInfoIF extends IsWidget {
    public void setCopyrightInfo(String copyright);
    public void setEnabled(boolean enabled);
    public boolean hasCopyright();
}
