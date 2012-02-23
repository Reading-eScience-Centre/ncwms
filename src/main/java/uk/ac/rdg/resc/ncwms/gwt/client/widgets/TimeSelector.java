package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.Collections;
import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

public class TimeSelector extends BaseSelector implements TimeSelectorIF {
	private ListBox dates;
	private ListBox times;
	private String id;
	
	public TimeSelector(String id, String label, final TimeDateSelectionHandler handler) {
	    super(label);
	    this.id = id;
	    init(handler);
	}
	
	public TimeSelector(String id, final TimeDateSelectionHandler handler) {
		super("Time");
		this.id = id;
		init(handler);
	}
	
	private void init(final TimeDateSelectionHandler handler){
		dates = new ListBox();
		dates.setName("date_selector");
		dates.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handler.dateSelected(id, getSelectedDate());
            }
        });
		dates.setTitle("Adjust the date");
		add(dates);

		times = new ListBox();
		times.setName("time_selector");
		times.setTitle("Adjust the time");
		times.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                handler.timeSelected(id, getSelectedDateTime());
            }
        });
		add(times);
	}
	
	public void setId(String id){
	    this.id = id;
	}
	
	public void populateDates(List<String> availableDates){
		dates.clear();
		if(availableDates == null || availableDates.size() == 0){
			dates.setEnabled(false);
			times.setEnabled(false);
			label.setStylePrimaryName("inactiveLabelStyle");
		} else {
		    Collections.sort(availableDates);
			for(String item : availableDates){
				dates.addItem(item);
			}
			dates.setEnabled(true);
			label.setStylePrimaryName("labelStyle");
		}
	}

	public void populateTimes(List<String> availableTimes){
	    times.clear();
	    
	    if(availableTimes == null || availableTimes.size() == 0 || !dates.isEnabled()){
	        times.setEnabled(false);
	    } else {
	        Collections.sort(availableTimes);
	        for(String item : availableTimes){
	            times.addItem(item);
	        }
	        if(availableTimes.size() > 1){
	            times.setEnabled(true);
	        } else {
	            times.setEnabled(false);
	        }
	    }
	}
	
	public String getSelectedDate(){
	    int i = dates.getSelectedIndex();
	    // TODO Look at this more carefully for case when no times are present
	    if(i != -1){
	        return dates.getValue(i);//+"T"+times.getValue(j);
	    } else {
	        return null;
	    }
	}
	
	public String getSelectedDateTime(){
		int i = dates.getSelectedIndex();
		int j = times.getSelectedIndex();
		// TODO Look at this more carefully for case when no times are present
		if(i != -1 && j != -1){
			return dates.getValue(i)+"T"+times.getValue(j);
		} else {
			return null;
		}
	}

	public boolean selectDate(String dateString) {
		for(int i=0; i<dates.getItemCount(); i++){
			if(dates.getValue(i).equals(dateString)){
				dates.setSelectedIndex(i);
				return true;
			}
		}
		return false;
	}
	
	public boolean selectTime(String timeString) {
	    if(selectDate(timeString.substring(0,10))){
	        String time = timeString.substring(11);
	        for(int i=0; i<times.getItemCount(); i++){
	            if(times.getValue(i).equals(time)){
	                times.setSelectedIndex(i);
	                return true;
	            }
	        }
	    }
	    return false;
	}
	
	public String returnTimes(){
	    StringBuilder s = new StringBuilder();
	    for(int i=0; i < dates.getItemCount(); i++){
	        s.append(dates.getValue(i)+"T"+times.getValue(0)+",");
	    }
	    return s.toString();
	}
	
	public void setEnabled(boolean enabled){
	    if(times.getItemCount() > 1)
	        times.setEnabled(enabled);
	    else
	        times.setEnabled(false);
	    if(dates.getItemCount() > 1)
	        dates.setEnabled(enabled);
	    else
	        dates.setEnabled(false);
	    
	    if(!times.isEnabled() && !dates.isEnabled()){
	        label.setStylePrimaryName("inactiveLabelStyle");
	    } else {
	        label.setStylePrimaryName("labelStyle");
	    }
	        
	}
}
