package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;

public interface PaletteSelectorIF extends IsWidget {
    public void setId(String id);
    public void populatePalettes(List<String> availablePalettes);
    public String getSelectedPalette();
    public void selectPalette(String paletteString);
    public boolean setScaleRange(String scaleRange);
    public String getScaleRange();
    public int getNumColorBands();
    public void setNumColorBands(int nBands);
    public void setLogScale(boolean logScale);
    public boolean isLogScale();
    public boolean isLocked();
    public void setEnabled(boolean enabled);
}
