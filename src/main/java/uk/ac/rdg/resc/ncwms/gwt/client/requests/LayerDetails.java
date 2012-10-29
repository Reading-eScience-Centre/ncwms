package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.List;

public class LayerDetails {
    private final String id;
    
    private String units = null;
    private String extents = "-180,-90,180,90";
    private String scaleRange = null;
    private int nColorBands = 50;
    private boolean logScale = false;
    private List<String> supportedStyles = null;
    private String zUnits = null;
    private boolean zPositive = true;
    private List<String> availableZs = null;
    private String moreInfo = null;
    private String copyright = null;
    private List<String> availablePalettes = null;
    private String selectedPalette = null;
    private List<String> availableDates = null;
    private String nearestTime = null;
    private String nearestDate = null;
    
    private boolean multiFeature = false;
    private String startTime = null;
    private String endTime = null;
    private String startZ = null;
    private String endZ = null;

    public LayerDetails(String layerId) {
        id = layerId;
    }
    
    public String getId(){
        return id;
    }

    public String getUnits() {
        return units;
    }

    public String getExtents() {
        return extents;
    }

    public String getScaleRange() {
        return scaleRange;
    }

    public int getNumColorBands() {
        return nColorBands;
    }

    public boolean isLogScale() {
        return logScale;
    }

    public List<String> getSupportedStyles() {
        return supportedStyles;
    }

    public String getZUnits() {
        return zUnits;
    }

    public boolean isZPositive() {
        return zPositive;
    }

    public List<String> getAvailableZs() {
        return availableZs;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public String getCopyright() {
        return copyright;
    }

    public List<String> getAvailablePalettes() {
        return availablePalettes;
    }

    public String getSelectedPalette() {
        return selectedPalette;
    }

    public List<String> getAvailableDates() {
        return availableDates;
    }

    public String getNearestDateTime() {
        return nearestTime;
    }

    public String getNearestDate() {
        return nearestDate;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }

    public void setExtents(String extents) {
        this.extents = extents;
    }

    public void setScaleRange(String scaleRange) {
        this.scaleRange = scaleRange;
    }

    public void setNColorBands(int nColorBands) {
        this.nColorBands = nColorBands;
    }

    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }

    public void setSupportedStyles(List<String> supportedStyles) {
        this.supportedStyles = supportedStyles;
    }

    public void setZUnits(String zUnits) {
        this.zUnits = zUnits;
    }

    public void setZPositive(boolean zPositive) {
        this.zPositive = zPositive;
    }

    public void setAvailableZs(List<String> availableZs) {
        this.availableZs = availableZs;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public void setAvailablePalettes(List<String> availablePalettes) {
        this.availablePalettes = availablePalettes;
    }

    public void setSelectedPalette(String selectedPalette) {
        this.selectedPalette = selectedPalette;
    }

    public void setAvailableDates(List<String> availableDates) {
        this.availableDates = availableDates;
    }

    public void setNearestTime(String nearestTime) {
        this.nearestTime = nearestTime;
    }

    public void setNearestDate(String nearestDate) {
        this.nearestDate = nearestDate;
    }

    public boolean isMultiFeature() {
        return multiFeature;
    }

    public void setMultiFeature(boolean multiFeature) {
        this.multiFeature = multiFeature;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStartZ() {
        return startZ;
    }

    public String getEndZ() {
        return endZ;
    }

    public void setStartZ(String startZ) {
        this.startZ = startZ;
    }

    public void setEndZ(String endZ) {
        this.endZ = endZ;
    }
}
