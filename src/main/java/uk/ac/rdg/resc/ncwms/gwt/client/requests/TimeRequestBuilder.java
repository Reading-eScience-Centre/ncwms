package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.URL;

public class TimeRequestBuilder extends RequestBuilder {

    public TimeRequestBuilder(String layerId, String day, String baseUrl) {
        super(GET, URL.encode(baseUrl + "?request=GetMetadata&item=timesteps&layerName="+layerId+"&day="+day));
    }
    
    public void setCallback(TimeRequestCallback trc){
        super.setCallback(trc);
    }
}
