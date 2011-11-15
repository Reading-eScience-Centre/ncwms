package uk.ac.rdg.resc.ncwms.controller;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.GridCoverage2D;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.position.CalendarSystem;
import uk.ac.rdg.resc.edal.position.TimePeriod;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.Vector2D;
import uk.ac.rdg.resc.edal.position.impl.TimePeriodImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionImpl;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
import uk.ac.rdg.resc.ncwms.exceptions.FeatureNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * Controller that handles all requests for non-standard metadata by the Godiva2
 * site. Eventually Godiva2 will be changed to accept standard metadata (i.e.
 * fragments of GetCapabilities)... maybe.
 * 
 * @author Jon Blower
 * @author Guy Griffiths
 */
public abstract class AbstractMetadataController {
    private static final Logger log = LoggerFactory.getLogger(AbstractMetadataController.class);

    private final FeatureFactory featureFactory;

    protected Config config;

    protected AbstractMetadataController(Config config, FeatureFactory featureFactory) {
        this.config = config;
        this.featureFactory = featureFactory;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response,
            UsageLogEntry usageLogEntry) throws MetadataException {
        try {
            String item = request.getParameter("item");
            usageLogEntry.setWmsOperation("GetMetadata:" + item);
            if (item == null) {
                throw new Exception("Must provide an ITEM parameter");
            } else if (item.equals("menu")) {
                return this.showMenu(request, usageLogEntry);
            } else if (item.equals("layerDetails")) {
                return this.showLayerDetails(request, usageLogEntry);
            } else if (item.equals("timesteps")) {
                return this.showTimesteps(request);
            } else if (item.equals("minmax")) {
                return this.showMinMax(request, usageLogEntry);
            } else if (item.equals("animationTimesteps")) {
                return this.showAnimationTimesteps(request);
            }
            throw new Exception("Invalid value for ITEM parameter");
        } catch (Exception e) {
            // Wrap all exceptions in a MetadataException. These will be
            // automatically
            // displayed via displayMetadataException.jsp, in JSON format
            e.printStackTrace();
            throw new MetadataException(e);
        }
    }

    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy. May differ between implementations
     */
    protected abstract ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception;

    /**
     * Shows an JSON document containing the details of the given variable
     * (units, zvalues, tvalues etc). See showLayerDetails.jsp.
     */
    private ModelAndView showLayerDetails(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception {
        GridSeriesFeature<?> feature = getFeature(request);
        usageLogEntry.setFeature(feature);
        TimeAxis tAxis = feature.getCoverage().getDomain().getTimeAxis();
        
        // Find the time the user has requested (this is the time that is
        // currently displayed on the Godiva2 site). If no time has been
        // specified we use the current time
        CalendarSystem calendarSystem = null;
        if(tAxis != null){
            calendarSystem = tAxis.getCalendarSystem();
        }
        TimePosition targetDateTime = new TimePositionImpl(calendarSystem);
        String targetDateIso = request.getParameter("time");
        if (targetDateIso != null && !targetDateIso.trim().equals("")) {
            try {
                targetDateTime = TimeUtils.iso8601ToDateTime(targetDateIso, calendarSystem);
            } catch (IllegalArgumentException iae) {
                // targetDateIso was not valid for the layer's chronology
                // We swallow this exception: targetDateTime will remain
                // unchanged.
            }
        }

        Map<Integer, Map<Integer, List<Integer>>> datesWithData = new LinkedHashMap<Integer, Map<Integer, List<Integer>>>();
        List<TimePosition> timeValues;
        if(tAxis != null)
            timeValues = tAxis.getCoordinateValues();
        else
            timeValues = Collections.emptyList();
        TimePosition nearestDateTime = timeValues.isEmpty() ? new TimePositionImpl(0) : timeValues.get(0);

        /*
         * Takes an array of time values for a layer and turns it into a Map of
         * year numbers to month numbers to day numbers, for use in
         * showVariableDetails.jsp. This is used to provide a list of days for
         * which we have data. Also calculates the nearest value on the time
         * axis to the time we're currently displaying on the web interface.
         */
        for (TimePosition dateTime : timeValues) {
            /*
             * We must make sure that dateTime() is in UTC or getDayOfMonth()
             * etc might return unexpected results
             */
            // TODO deal with this UTC issue
//            dateTime = dateTime.withZone(DateTimeZone.UTC);
            
            // See whether this dateTime is closer to the target dateTime than
            // the current closest value
            long d1 = dateTime.differenceInMillis(targetDateTime);
            long d2 = nearestDateTime.differenceInMillis(targetDateTime);
            if (Math.abs(d1) < Math.abs(d2))
                nearestDateTime = dateTime;

            int year = dateTime.getYear();
            Map<Integer, List<Integer>> months = datesWithData.get(year);
            if (months == null) {
                months = new LinkedHashMap<Integer, List<Integer>>();
                datesWithData.put(year, months);
            }
            // We need to subtract 1 from the month number as Javascript months
            // are 0-based (Joda-time months are 1-based). This retains
            // compatibility with previous behaviour.
            int month = dateTime.getMonthOfYear();
            List<Integer> days = months.get(month);
            if (days == null) {
                days = new ArrayList<Integer>();
                months.put(month, days);
            }
            int day = dateTime.getDayOfMonth();
            if (!days.contains(day))
                days.add(day);
        }

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("feature", feature);
        /*
         * request.getParameter("layerName") cannot fail - if it would fail, it
         * will have done so in the earlier getFeature call, so this point won't
         * be reached
         */
        models.put("featureMetadata", WmsUtils.getMetadata(config, request.getParameter("layerName")));
        models.put("dataset", WmsUtils.getDataset(config, request.getParameter("layerName")));
        models.put("datesWithData", datesWithData);
        models.put("units", feature.getCoverage().getRangeMetadata(null).getUnits().getUnitString());
        models.put("nearestTimeIso", TimeUtils.dateTimeToISO8601(nearestDateTime));
        // The names of the palettes supported by this layer. Actually this
        // will be the same for all layers, but we can't put this in the menu
        // because there might be several menu JSPs.
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        return new ModelAndView("showLayerDetails", models);
    }

    /**
     * @return the Layer that the user is requesting, throwing an Exception if
     *         it doesn't exist or if there was a problem reading from the data
     *         store.
     */
    private GridSeriesFeature<?> getFeature(HttpServletRequest request) throws FeatureNotDefinedException {
        String layerName = request.getParameter("layerName");
        if (layerName == null) {
            throw new FeatureNotDefinedException("null");
        }
        return featureFactory.getFeature(layerName);
    }

    /**
     * Finds all the timesteps that occur on the given date, which will be
     * provided in the form "2007-10-18".
     */
    private ModelAndView showTimesteps(HttpServletRequest request) throws Exception {
        GridSeriesFeature<?> feature = getFeature(request);
        TimeAxis tAxis = feature.getCoverage().getDomain().getTimeAxis();
        if (tAxis == null || tAxis.getCoordinateValues().isEmpty())
            return null; // return no data if no time axis present

        String dayStr = request.getParameter("day");
        if (dayStr == null) {
            throw new Exception("Must provide a value for the day parameter");
        }
        TimePosition date = TimeUtils.iso8601ToDate(dayStr, tAxis.getCalendarSystem());
        // List of date-times that fall on this day
        List<TimePosition> timesteps = new ArrayList<TimePosition>();
        // Search exhaustively through the layer's valid time values
        // TODO: inefficient: should stop once last day has been found.
        for (TimePosition tVal : tAxis.getCoordinateValues()) {
            if (onSameDay(tVal, date)) {
                timesteps.add(tVal);
            }
        }
        log.debug("Found {} timesteps on {}", timesteps.size(), dayStr);

        return new ModelAndView("showTimesteps", "timesteps", timesteps);
    }

    /**
     * @return true if the two given DateTimes fall on the same day.
     */
    private static boolean onSameDay(TimePosition dt1, TimePosition dt2) {
        // We must make sure that the DateTimes are both in UTC or the field
        // comparisons will not do what we expect
        // TODO re-implement this check somehow
        // dt1 = dt1.withZone(DateTimeZone.UTC);
        // dt2 = dt2.withZone(DateTimeZone.UTC);
        boolean onSameDay = dt1.getYear() == dt2.getYear() && dt1.getMonthOfYear() == dt2.getMonthOfYear()
                && dt1.getDayOfMonth() == dt2.getDayOfMonth();
        log.debug("onSameDay({}, {}) = {}", new Object[] { dt1, dt2, onSameDay });
        return onSameDay;
    }

    /**
     * Shows an XML document containing the minimum and maximum values for the
     * tile given in the parameters.
     */
    @SuppressWarnings("unchecked")
    private ModelAndView showMinMax(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception {
        RequestParams params = new RequestParams(request.getParameterMap());
        // We only need the bit of the GetMap request that pertains to data
        // extraction
        // TODO: the hard-coded "1.3.0" is ugly: it basically means that the
        // GetMapDataRequest object will look for "CRS" instead of "SRS"
        GetMapDataRequest dr = new GetMapDataRequest(params, params.getWmsVersion());

        // Get the variable we're interested in
        GridSeriesFeature<?> feature = featureFactory.getFeature(dr.getLayers()[0]);
        usageLogEntry.setFeature(feature);

        // Get the grid onto which the data is being projected
        RegularGrid grid = WmsUtils.getImageGrid(dr);

        // Get the value on the z axis
        double zValue = AbstractWmsController.getElevationValue(dr.getElevationString(), feature);

        // Get the requested timestep (taking the first only if an animation is
        // requested)
        List<TimePosition> timeValues = AbstractWmsController.getTimeValues(dr.getTimeString(), feature);
        TimePosition tValue = timeValues.isEmpty() ? null : timeValues.get(0);

        // Now read the data and calculate the minimum and maximum values
        List<Float> magnitudes;
        final GridCoverage2D<?> hGridCoverage = feature.extractHorizontalGrid(tValue, zValue, grid);
        Class<?> clazz = hGridCoverage.getRangeMetadata(null).getValueType();
        if(clazz == Float.class){
            magnitudes = (List<Float>) hGridCoverage.getValues();
        } else if (clazz == Vector2D.class) {
            magnitudes =  new AbstractList<Float>() {
                @Override
                public Float get(int index) {
                    Object obj = hGridCoverage.getValues().get(index);
                    return obj == null ? null : ((Vector2D<Float>)obj).getMagnitude();
                }

                @Override
                public int size() {
                    return hGridCoverage.getValues().size();
                }
            };
        } else {
            throw new IllegalStateException("Invalid Layer type");
        }
        
        Extent<Float> valueRange = Extents.findMinMax(magnitudes);
        return new ModelAndView("showMinMax", "valueRange", valueRange);
    }

    /**
     * Calculates the TIME strings necessary to generate animations for the
     * given layer at hourly, daily, weekly, monthly and yearly resolution.
     * 
     * @param request
     * @return
     * @throws java.lang.Exception
     */
    private ModelAndView showAnimationTimesteps(HttpServletRequest request) throws Exception {
        TimeAxis tAxis = getFeature(request).getCoverage().getDomain().getTimeAxis();
        String startStr = request.getParameter("start");
        String endStr = request.getParameter("end");
        if (startStr == null || endStr == null) {
            throw new Exception("Must provide values for start and end");
        }

        // Find the start and end indices along the time axis
        int startIndex = AbstractWmsController.findTIndex(startStr, tAxis);
        int endIndex = AbstractWmsController.findTIndex(endStr, tAxis);
        List<TimePosition> tValues = tAxis.getCoordinateValues();

        // E.g.: {
        // "Full" : "start/end",
        // "Hourly" : "t1,t2,t3,t4",
        // "Daily" : "ta,tb,tc,td"
        // etc
        // }
        Map<String, String> timeStrings = new LinkedHashMap<String, String>();

        timeStrings.put("Full (" + (endIndex - startIndex + 1) + " frames)", startStr + "/" + endStr);
        addTimeString("Daily", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withDays(1));
        addTimeString("Weekly", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withWeeks(1));
        addTimeString("Monthly", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withMonths(1));
        addTimeString("Bi-monthly", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withMonths(2));
        addTimeString("Twice-yearly", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withMonths(6));
        addTimeString("Yearly", timeStrings, tValues, startIndex, endIndex, new TimePeriodImpl().withYears(1));

        return new ModelAndView("showAnimationTimesteps", "timeStrings", timeStrings);
    }

    private static void addTimeString(String label, Map<String, String> timeStrings, List<TimePosition> tValues,
            int startIndex, int endIndex, TimePeriod resolution) {
        List<TimePosition> timesteps = getAnimationTimesteps(tValues, startIndex, endIndex, resolution);
        // We filter out all the animations with less than one timestep
        if (timesteps.size() > 1) {
            String timeString = getTimeString(timesteps);
            timeStrings.put(label + " (" + timesteps.size() + " frames)", timeString);
        }
    }

    private static List<TimePosition> getAnimationTimesteps(List<TimePosition> tValues, int startIndex, int endIndex,
            TimePeriod resolution) {
        List<TimePosition> times = new ArrayList<TimePosition>();
        times.add(tValues.get(startIndex));
        for (int i = startIndex + 1; i <= endIndex; i++) {
            TimePosition lastdt = times.get(times.size() - 1);
            TimePosition thisdt = tValues.get(i);
            lastdt.plus(resolution);
            if (thisdt.getValue() >= lastdt.plus(resolution).getValue()) {
                times.add(thisdt);
            }
        }
        return times;
    }

    private static String getTimeString(List<TimePosition> timesteps) {
        if (timesteps.size() == 0)
            return "";
        StringBuilder builder = new StringBuilder(TimeUtils.dateTimeToISO8601(timesteps.get(0)));
        for (int i = 1; i < timesteps.size(); i++) {
            builder.append("," + TimeUtils.dateTimeToISO8601(timesteps.get(i)));
        }
        return builder.toString();
    }
}
