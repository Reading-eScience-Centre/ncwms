package uk.ac.rdg.resc.ncwms.controller;

import java.util.Map;

import uk.ac.rdg.resc.edal.position.LonLatPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;

public class FeatureInfo {
    private String memberId;
    private String featureId;
    private LonLatPosition actualPos;
    private Map<TimePosition, Object> timesAndValues;

    public FeatureInfo(String featureId, String memberId, LonLatPosition actualPos, Map<TimePosition, Object> timesAndValues) {
        super();
        this.featureId = featureId;
        this.memberId = memberId;
        this.actualPos = actualPos;
        this.timesAndValues = timesAndValues;
    }

    public String getFeatureId() {
        return featureId;
    }
    
    public String getMemberId() {
        return memberId;
    }

    public LonLatPosition getActualPos() {
        return actualPos;
    }

    public Map<TimePosition, Object> getTimesAndValues() {
        return timesAndValues;
    }
}
