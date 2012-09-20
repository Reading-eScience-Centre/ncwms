package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.TimeDateSelectionHandler;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class TimeSelector extends BaseSelector implements TimeSelectorIF {
    private static final String[] allTimes = new String[24];
    
    static {
        NumberFormat format = NumberFormat.getFormat("00");
        for(int i=0; i < 24; i++){
            allTimes[i] = format.format(i) + ":00:00.000Z";
        }
    }
    
	private ListBox dates;
	private ListBox times;
	private String id;
	
	/*
	 * These are used when we have a continuous time axis
	 */
	private boolean continuous = false;
	private Label endLabel;
	private ListBox endDates;
	private ListBox endTimes;
    private TimeDateSelectionHandler handler;
    private Button goButton;
	
	
	public TimeSelector(String id, String label, final TimeDateSelectionHandler handler) {
	    super(label);
	    this.id = id;
	    this.handler = handler;
	    initDiscrete();
	}
	
	public TimeSelector(String id, final TimeDateSelectionHandler handler) {
		super("Time");
		this.id = id;
		this.handler = handler;
		initDiscrete();
	}
	
	private void initDiscrete(){
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
		
		setCellVerticalAlignment(dates, HasVerticalAlignment.ALIGN_BOTTOM);
        setCellVerticalAlignment(times, HasVerticalAlignment.ALIGN_BOTTOM);
	}
	
	private void initContinuous(){
	    dates = new ListBox();
	    dates.setName("date_selector");
	    dates.addChangeHandler(new ChangeHandler() {
	        @Override
	        public void onChange(ChangeEvent event) {
	            // TODO change the end date range
	        }
	    });
	    dates.setTitle("Adjust the start date");
	    add(dates);
	    
	    times = new ListBox();
	    times.setName("time_selector");
	    times.setTitle("Adjust the start time");
	    times.addChangeHandler(new ChangeHandler() {
	        @Override
	        public void onChange(ChangeEvent event) {
	            // TODO change the end time range (for this date)
	        }
	    });
	    add(times);
	    
	    endLabel = new Label(" to ");
	    add(endLabel);
	    
	    endDates = new ListBox();
	    endDates.setName("end_date_selector");
	    endDates.setTitle("Adjust the end date");
	    add(endDates);
	    
	    endTimes = new ListBox();
	    endTimes.setName("end_time_selector");
	    endTimes.setTitle("Adjust the end time");
	    add(endTimes);
	    
	    goButton = new Button("Go");
	    goButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                handler.timeSelected(id, getSelectedDateTime());
            }
        });
	    add(goButton);
	}
	
	@Override
    public void setId(String id){
	    this.id = id;
	}
	
	@Override
    public void populateDates(List<String> availableDates){
	    dates.clear();
	    if(availableDates == null || availableDates.size() == 0){
	        dates.setEnabled(false);
	        times.setEnabled(false);
	        label.setStylePrimaryName("inactiveLabelStyle");
	    } else {
	        if(continuous){
    	        if(availableDates.size() != 2){
                    throw new IllegalArgumentException(
                            "For a continuous time axis, you must provide exactly 2 dates");
    	        }
    	        
    	        availableDates = getDatesInRange(availableDates.get(0), availableDates.get(1));
    	        
    	        for(String item : availableDates){
    	            dates.addItem(item);
    	            endDates.addItem(item);
    	        }
    	        for(String item : allTimes){
    	            times.addItem(item);
    	            endTimes.addItem(item);
    	        }
    	        dates.setEnabled(true);
    	        endDates.setEnabled(true);
    	        label.setStylePrimaryName("labelStyle");
	        } else {
    		    Collections.sort(availableDates);
    			for(String item : availableDates){
    				dates.addItem(item);
    			}
    			dates.setEnabled(true);
    			label.setStylePrimaryName("labelStyle");
	        }
		}
	}

	@Override
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
	
	@Override
    public String getSelectedDate(){
	    int i = dates.getSelectedIndex();
	    // TODO Look at this more carefully for case when no times are present
	    if(i != -1){
	        return dates.getValue(i);//+"T"+times.getValue(j);
	    } else {
	        return null;
	    }
	}
	
	@Override
    public String getSelectedDateTime(){
	    if(continuous){
	        int i = dates.getSelectedIndex();
	        int j = times.getSelectedIndex();
	        int k = endDates.getSelectedIndex();
	        int l = endTimes.getSelectedIndex();
	        // TODO Look at this more carefully for case when no times are present
	        if(i != -1 && j != -1 && k != -1 && l != -1){
                return dates.getValue(i) + "T" + times.getValue(j) + "/" + endDates.getValue(k)
                        + "T" + endTimes.getValue(l);
	        } else {
	            return null;
	        }
	    } else {
    		int i = dates.getSelectedIndex();
    		int j = times.getSelectedIndex();
    		// TODO Look at this more carefully for case when no times are present
    		if(i != -1 && j != -1){
    			return dates.getValue(i)+"T"+times.getValue(j);
    		} else {
    			return null;
    		}
	    }
	}
	
	@Override
	public String getSelectedTime() {
	    int i = times.getSelectedIndex();
	    // TODO Look at this more carefully for case when no times are present
	    if(i != -1){
	        return times.getValue(i);
	    } else {
	        return null;
	    }
	}

	@Override
    public boolean selectDate(String dateString) {
		for(int i=0; i<dates.getItemCount(); i++){
			if(dates.getValue(i).equals(dateString)){
				dates.setSelectedIndex(i);
				return true;
			}
		}
		return false;
	}
	
	@Override
    public boolean selectDateTime(String timeString) {
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
	
//	@Override
//    public String returnTimes(){
//	    StringBuilder s = new StringBuilder();
//	    for(int i=0; i < dates.getItemCount(); i++){
//	        s.append(dates.getValue(i)+"T"+times.getValue(0)+",");
//	    }
//	    return s.toString();
//	}
	
	@Override
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
	
	@Override
	public boolean hasMultipleTimes() {
	    return (dates.getItemCount() > 1) || (times.getItemCount() > 1);
	}

    @Override
    public void setContinuous(boolean continuous) {
        if(continuous != this.continuous){
            this.continuous = continuous;
            if(dates != null)
                remove(dates);
            if(times != null)
                remove(times);
            if(endLabel != null)
                remove(endLabel);
            if(endDates != null)
                remove(endDates);
            if(endTimes != null)
                remove(endTimes);
            if(goButton != null)
                remove(goButton);
            dates = null;
            times = null;
            endLabel = null;
            endDates = null;
            endTimes = null;
            goButton = null;
            if(continuous) {
                initContinuous();
            } else {
                initDiscrete();
            }
        }
    }

    @Override
    public boolean isContinuous() {
        return continuous;
    }
    
    private List<String> getDatesInRange(String startDateTimeStr, String endDateTimeStr){
        DateTimeFormat parser = DateTimeFormat.getFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
        DateTimeFormat datePrinter = DateTimeFormat.getFormat("yyyy-MM-dd");
        
        Date startDate = parser.parse(startDateTimeStr);
        Date endDate = parser.parse(endDateTimeStr);
        startDate.setMinutes(0);
        startDate.setSeconds(0);
        
        endDate.setHours(endDate.getHours() + 1);
        endDate.setMinutes(0);
        endDate.setSeconds(0);
        
        List<String> dates = new ArrayList<String>();
        while(startDate.getTime() < endDate.getTime()) {
            dates.add(datePrinter.format(startDate));
            startDate.setDate(startDate.getDate() + 1);
        }
        return dates;
    }
}
