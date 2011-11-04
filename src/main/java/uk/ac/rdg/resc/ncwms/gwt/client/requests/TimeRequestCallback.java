package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public abstract class TimeRequestCallback implements RequestCallback {
    
    private List<String> availableTimesteps;

    public abstract void onError(Request request, Throwable exception);

    public void onResponseReceived(Request request, Response response) {
        JSONValue jsonMap = JSONParser.parseLenient(response.getText());
        JSONObject parentObj = jsonMap.isObject();

        JSONValue timestepsJson = parentObj.get("timesteps");
        if (timestepsJson != null) {
            availableTimesteps = new ArrayList<String>();
            JSONArray timestepsArr = timestepsJson.isArray();
            for(int i=0; i< timestepsArr.size(); i++){
                availableTimesteps.add(timestepsArr.get(i).isString().stringValue());
            }
        }
    }

    public List<String> getAvailableTimesteps() {
        return availableTimesteps;
    }

}
