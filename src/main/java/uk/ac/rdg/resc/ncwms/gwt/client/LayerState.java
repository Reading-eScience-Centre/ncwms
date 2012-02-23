package uk.ac.rdg.resc.ncwms.gwt.client;

public class LayerState {
    // The colour scale range
    private String scaleRange;
    // The current style name
    private String style;
    // The current palette name
    private String palette;
    // The current number of colour bands
    private int nColorBands;
    // Whether we are viewing a logarithmic scale
    private boolean logScale;
    private String currentTime;
    private String currentElevation = null;

    public LayerState() {
        scaleRange = "-50:50";
        style = "boxfill";
        palette = "rainbow";
        nColorBands = 50;
        logScale = false;
        currentTime = null;
        currentElevation = null;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public String getCurrentElevation() {
        return currentElevation;
    }

    public void setCurrentElevation(String currentElevation) {
        this.currentElevation = currentElevation;
    }

    public String getScaleRange() {
        return scaleRange;
    }

    public void setScaleRange(String scaleRange) {
        this.scaleRange = scaleRange;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getPalette() {
        return palette;
    }

    public void setPalette(String palette) {
        this.palette = palette;
    }

    public int getNColorBands() {
        return nColorBands;
    }

    public void setNColorBands(int nColorBands) {
        this.nColorBands = nColorBands;
    }

    public boolean isLogScale() {
        return logScale;
    }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }
}
