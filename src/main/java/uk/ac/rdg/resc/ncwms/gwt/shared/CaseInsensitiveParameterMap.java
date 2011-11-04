package uk.ac.rdg.resc.ncwms.gwt.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaseInsensitiveParameterMap {
    public Map<String, String> pMap;
    
    private CaseInsensitiveParameterMap() {
    }
    
    public static CaseInsensitiveParameterMap getMapFromList(Map<String, List<String>> initMap) {
        final CaseInsensitiveParameterMap ret = new CaseInsensitiveParameterMap();
        ret.pMap = new HashMap<String, String>();
        for(String key : initMap.keySet()){
            List<String> vals = initMap.get(key);
            if(vals != null && vals.size() >= 1)
                ret.pMap.put(key.toLowerCase(), vals.get(0));
        }
        return ret;
    }

    public static CaseInsensitiveParameterMap getMapFromArray (Map<String, String[]> initMap) {
        final CaseInsensitiveParameterMap ret = new CaseInsensitiveParameterMap();
        ret.pMap = new HashMap<String, String>();
        for(String key : initMap.keySet()){
            String[] vals = initMap.get(key);
            if(vals != null && vals.length >= 1)
                ret.pMap.put(key.toLowerCase(), vals[0]);
        }
        return ret;
    }
    
    public String get(String key){
        return pMap.get(key.toLowerCase());
    }
}
