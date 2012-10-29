package uk.ac.rdg.resc.ncwms.controller;

import java.util.Map;

import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;

public class FeatureInfo {
    private String featureCollectionId;
    private String featureId;
    private String memberId;
    private HorizontalPosition actualPos;
    private Map<TimePosition, Object> timesAndValues;

    public FeatureInfo(String featureCollectionId, String featureId, String memberId,
            HorizontalPosition actualPos, Map<TimePosition, Object> timesAndValues) {
        super();
        this.featureCollectionId = featureCollectionId;
        this.featureId = featureId;
        this.memberId = memberId;
        this.actualPos = actualPos;
        this.timesAndValues = timesAndValues;
    }
    
    public String getFeatureCollectionId() {
        return featureCollectionId;
    }

    public String getFeatureId() {
        return featureId;
    }
    
    public String getMemberId() {
        return memberId;
    }

    public HorizontalPosition getActualPos() {
        return actualPos;
    }

    public Map<TimePosition, Object> getTimesAndValues() {
        return timesAndValues;
    }
}
