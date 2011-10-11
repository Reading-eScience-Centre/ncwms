/**
 * Class: Godiva2.Time
 *
 */
 
 Godiva2.Time = OpenLayers.Class({

    /**
     * Property: calendar
     * {<OpenLayers.Map>} The DHTML calendar for normal (365- or 366-day) years
     */
    calendar: null,

    /**
     * Property: basicDataSelector
     * The basicDataSelector for 360-day years
     *
     */
    basicDataSelector: null,

    isoTValue: null,

    /**
     * Godiva2 Elements relating to time
     */
    timeEls: null,

    /**
     * Constructor: Godiva2.Time
     * Create a Godiva2 Time Dimension.  For example:
     *
     * > var time = new Godiva2.Time();

     * Parameters:
     * layer - details of the layer which the TimeDimension represents
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
        this.timeEls = {};
        this.timeEls.dateEl = document.getElementById('date');
        this.timeEls.timeEl = document.getElementById('time');
        this.timeEls.utcEl = document.getElementById('utc');
        this.timeEls.tValuesEl = document.getElementById('tValues');
        this.timeEls.setFramesEl = document.getElementById('setFrames');
        this.timeEls.panelTextEl = document.getElementById('panelText');
    },

    /**
     * Method: destroy
     * The destroy method is used to perform any clean up before the component
     * is dereferenced.  Typically this is where event listeners are removed
     * to prevent memory leaks.
     */
    destroy: function () {
        if (this.calendar) {
            this.calendar = null;
        }
    },

    /**
     * Method: setLayer
     *
     */
    configure: function () {
        if (this.calendar != null) this.calendar.hide();
        this.timeEls.dateEl.innerHTML = '';
        this.timeEls.timeEl.innerHTML = '';
        this.timeEls.utcEl.style.visibility = 'hidden';
    },

    /**
     * Method: setupCalendar
     *
     */
    setupCalendar: function () {
        // TODO: link up with CSS
        this.timeEls.panelTextEl.style.width = "390px";
        if (this.calendar == null) {
            // Set up the calendar
            this.calendar = Calendar.setup({
                flat : 'calendar', // ID of the parent element
                align : 'bl', // Aligned to top-left of parent element
                weekNumbers : false,
                flatCallback : this.getSelectedDateAndLoadTimesteps.bind(this),
                disableFunc: this.isDateDisabled.bind(this)
            });
        }
        // Find the range of valid years.
        var minYear = 100000000;
        var maxYear = -100000000;
        for (var year in layer.details.datesWithData) {
            if (typeof layer.details.datesWithData[year] != 'function') { // avoid built-in functions
                if (year < minYear) minYear = year;
                if (year > maxYear) maxYear = year;
            }
        }
        this.calendar.setRange(minYear, maxYear);

        // Under Internet Explorer 7 (possibly other versions too) the calendar
        // behaves oddly for early dates (e.g. year = 0031).  Therefore we
        // don't use calendar.date to get the selected date, and we pass a clone
        // of the selected date to the calendar to protect against mutation.
        this.calendar.setDate(new Date(layer.details.nearestTime.getTime()));

        // N.B. For some reason the call to show() seems sometimes to toggle the
        // visibility of the zValues selector.  Hence we set this visibility
        // below, in updateMap()
        this.calendar.show();
        // Manually refresh the calendar to ensure that the correct dates are disabled
        this.calendar.refresh();
    },

    /**
     * Method: setupBasicDateSelector
     * We can't use the fancy DHTML calendar for unusual calendar systems
     * so we'll set up a very simple system of drop-down boxes
     */
    setupBasicDateSelector: function () {
        if (this.calendar != null) this.calendar.hide();
        // Widen the panel text to accommodate the calendar
        this.timeEls.panelTextEl.style.width = "512px";
        this.timeEls.dateEl.innerHTML = '<b>Date/time: </b>';

        if (this.basicDateSelector == null) {
            this.basicDateSelector = new BasicDateSelector({
                el: this.timeEls.dateEl,
                // Called when the selected date changes
                callback: function(year, month, day) {
                    // Create an ISO representation of the day and load
                    // the timesteps
                    this.loadTimesteps(year + '-' + month + '-' + day);
                }
            });
        }
        this.basicDateSelector.setup(layer.details.datesWithData, layer.details.nearestTimeIso);
        this.basicDateSelector.show(this.timeEls.dateEl);
    },

    /**
     * Method: getSelectedDate
     * Function that is called when a user clicks on a date in the calendar
     */
    getSelectedDateAndLoadTimesteps: function (cal, force) {
        if (cal.dateClicked || force) {
            var selectedDate = new Date(cal.date.getTime());
            // Print out date, e.g. "15 Oct 2007"
            var prettyPrintDate = selectedDate.getFullYear() == 0
                ? selectedDate.print('%d %b 0000')
                : selectedDate.print('%d %b %Y');
            this.timeEls.dateEl.innerHTML = '<b>Date/time: </b>' + prettyPrintDate;
            this.loadTimesteps(selectedDate);
        }
    },

    /**
     * Method: loadTimesteps
     * Updates the time selector control.  Finds all the timesteps that occur on
     * the same day as the currently-selected date.  Called from the calendar
     * control when the user selects a new date
     */
    loadTimesteps: function (selectedDate) {
        //dumpProps(this);
        var selectedIsoDate = this.makeIsoDate(selectedDate);
        // Get the timesteps for this day
        getTimesteps(layer.details.server, {
            callback: this.updateTimesteps.bind(this),
            layerName: layer.details.id,
            day: selectedIsoDate
        });
    },

    /**
     * Method: updateTimesteps
     * Called when we have received the timesteps from the server
     */
    updateTimesteps: function (selectedIsoDate, times) {
        // We'll get back a JSON array of ISO8601 times ("hh:mm:ss", UTC, no date information)
        // First we load the currently-selected time (if there is one)
        var timeSelect = this.timeEls.tValuesEl;
        var selectedTimeStr = null;
        if (timeSelect && timeSelect.selectedIndex >= 0) {
            selectedTimeStr = timeSelect.options[timeSelect.selectedIndex].text;
        }
        // Build the select box
        var s = '<select id="tValues">';
        for (var i = 0; i < times.length; i++) {
            // Construct the full ISO Date-time
            var isoDateTime = selectedIsoDate + 'T' + times[i];// + 'Z';
            // Strip off the trailing "Z" and any zero-length milliseconds
            var stopIndex = times[i].length;
            if (times[i].endsWith('.000Z')) {
                stopIndex -= 5;
            } else if (times[i].endsWith('.00Z')) {
                stopIndex -= 4;
            } else if (times[i].endsWith('.0Z')) {
                stopIndex -= 3;
            } else if (times[i].endsWith('Z')) {
                stopIndex -= 1;
            }
            var text = times[i].substring(0, stopIndex);
            s += '<option value="' + isoDateTime + '"'
            if (selectedTimeStr && selectedTimeStr == text) {
                s += ' selected="selected"';
            }
            s += '>' + text + '</option>';
        }
        s += '</select>';
        this.timeEls.timeEl.innerHTML = s;
        document.getElementById('tValues').onchange = map.update.bind(map);

        //timeSelect = this.timeEls.tValuesEl;
        timeSelect = $('tValues');
        // If there was a previously-selected time, select it
        if (selectedTimeStr) {
            for (i = 0; i < timeSelect.options.length; i++) {
                if (timeSelect.options[i].text == selectedTimeStr) {
                    timeSelect.selectedIndex = i;
                    break;
                }
            }
        }

        // If we're autoloading, set the right time in the selection box
        if (autoLoad != null && autoLoad.isoTValue != null) {
            for (i = 0; i < timeSelect.options.length; i++) {
                if (timeSelect.options[i].value == autoLoad.isoTValue) {
                    timeSelect.selectedIndex = i;
                    break;
                }
            }
        }
        this.timeEls.utcEl.style.visibility = 'visible';
        this.timeEls.setFramesEl.style.visibility = 'visible';

        gotTimesteps();
    },

    /**
     * Method: isDateDisabled
     * Function that is used by the calendar to see whether a date should be disabled
     */
    isDateDisabled: function (date, year, month, day) {
        var datesWithData = layer.details.datesWithData;
        
        // datesWithData is a hash of year numbers mapped to a hash of month numbers
        // to an array of day numbers, i.e. {2007 : {0 : [3,4,5]}}.
        // Month numbers are zero-based.
        if (datesWithData == null ||
            datesWithData[year] == null ||
            datesWithData[year][month] == null) {
            // No data for this year or month
            return true;
        }
        
        // Cycle through the array of days for this month, looking for the one we want
        var numDays = datesWithData[year][month].length;
        for (var d = 0; d < numDays; d++) {
            if (datesWithData[year][month][d] == day) {
                return false; // We have data for this day
            }
        }
        // If we've got this far, we've found no data
        return true;
    },

    /**
     * Method: makeIsoDate
     * Gets an ISO Date ("yyyy-mm-dd") for the given Javascript date object.
     * Does not contain the time.
     */
    makeIsoDate: function (date) {
        // Watch out for low-numbered years when generating the ISO string
        var year = date.getFullYear();
        if (year == 0) return date.print('0000-%m-%d'); // need workaround for year zero
        var prefix = '';
        if (year < 10) prefix = '000';
        else if (year < 100) prefix = '00';
        else if (year < 1000) prefix = '0';
        return prefix + date.print('%Y-%m-%d');
    },

    getNearestTime: function () {
        return layer.details.nearestTime;
    },

    layerHasNoTime: function () {
        return layer.details.datesWithData == null;
    },

    layerHasNormalYear: function () {
        return layer.details.timeAxisUnits == null || layer.details.timeAxisUnits == "ISO8601"
    },

    // Returns true if the user has selected a time series
    timeSeriesSelected: function () {
        return $('firstFrame').innerHTML != '' && $('lastFrame').innerHTML != '';
    },

    CLASS_NAME: "Godiva2.Time"
 });