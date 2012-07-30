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
import uk.ac.rdg.resc.edal.coverage.DiscreteCoverage;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.position.CalendarSystem;
import uk.ac.rdg.resc.edal.position.TimePeriod;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.TimePeriodImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
import uk.ac.rdg.resc.ncwms.exceptions.FeatureNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
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

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws MetadataException {
        try {
            String item = request.getParameter("item");
            if (item == null) {
                throw new Exception("Must provide an ITEM parameter");
            } else if (item.equals("menu")) {
                return this.showMenu(request);
            } else if (item.equals("layerDetails")) {
                return this.showLayerDetails(request);
            } else if (item.equals("timesteps")) {
                return this.showTimesteps(request);
            } else if (item.equals("minmax")) {
                return this.showMinMax(request);
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
    protected abstract ModelAndView showMenu(HttpServletRequest request) throws Exception;

    /**
     * Shows an JSON document containing the details of the given variable
     * (units, zvalues, tvalues etc). See showLayerDetails.jsp.
     */
    private ModelAndView showLayerDetails(HttpServletRequest request) throws Exception {
        Feature feature = getFeature(request);

        String layerName = request.getParameter("layerName");
        String memberName = WmsUtils.getMemberName(layerName);

        TimeAxis tAxis = WmsUtils.getTimeAxis(feature);

        // Find the time the user has requested (this is the time that is
        // currently displayed on the Godiva2 site). If no time has been
        // specified we use the current time
        CalendarSystem calendarSystem = null;
        if (tAxis != null) {
            calendarSystem = tAxis.getCalendarSystem();
        }
        TimePosition targetDateTime = new TimePositionJoda(calendarSystem);
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
        if (tAxis != null)
            timeValues = tAxis.getCoordinateValues();
        else
            timeValues = Collections.emptyList();
        TimePosition nearestDateTime = timeValues.isEmpty() ? new TimePositionJoda() : timeValues
                .get(0);

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
        String plottableLayerName = WmsUtils.getPlottableLayerName(feature, layerName);
        FeaturePlottingMetadata plottingMetadata = WmsUtils.getMetadata(config, plottableLayerName);

        String units = "";
        ScalarMetadata scalarMetadata = feature.getCoverage().getScalarMetadata(
                WmsUtils.getPlottableMemberName(feature, memberName));
        if (scalarMetadata != null) {
            units = scalarMetadata.getUnits().getUnitString();
        }
        models.put("featureMetadata", plottingMetadata);
        models.put("memberName", memberName);
        models.put("dataset", WmsUtils.getDataset(config, request.getParameter("layerName")));
        models.put("datesWithData", datesWithData);
        models.put("units", units);
        models.put("nearestTimeIso", TimeUtils.dateTimeToISO8601(nearestDateTime));
        // The names of the palettes supported by this layer. Actually this
        // will be the same for all layers, but we can't put this in the menu
        // because there might be several menu JSPs.
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        return new ModelAndView("showLayerDetails", models);
    }

    private Feature getFeature(HttpServletRequest request) throws FeatureNotDefinedException {
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
        Feature feature = getFeature(request);

        TimeAxis tAxis = WmsUtils.getTimeAxis(feature);
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
        boolean onSameDay = dt1.getYear() == dt2.getYear()
                && dt1.getMonthOfYear() == dt2.getMonthOfYear()
                && dt1.getDayOfMonth() == dt2.getDayOfMonth();
        log.debug("onSameDay({}, {}) = {}", new Object[] { dt1, dt2, onSameDay });
        return onSameDay;
    }

    /**
     * Shows an XML document containing the minimum and maximum values for the
     * tile given in the parameters.
     */
    private ModelAndView showMinMax(HttpServletRequest request) throws Exception {
        RequestParams params = new RequestParams(request.getParameterMap());
        /*
         * We only need the bit of the GetMap request that pertains to data
         * extraction
         */
        GetMapDataRequest dr = new GetMapDataRequest(params, params.getWmsVersion());

        String layerName = WmsUtils.getLayerName(dr);
        final String memberName = WmsUtils.getMemberName(layerName);

        // Get the variable we're interested in
        Feature feature = featureFactory.getFeature(layerName);
        if (feature instanceof GridSeriesFeature) {
            /*
             * If we have a grid series feature, extract the relevant portion of
             * the data
             */
       
            RegularGrid grid = WmsUtils.getImageGrid(dr);
            VerticalPosition zValue = AbstractWmsController.getElevationValue(
                    dr.getElevationString(), feature);
            /*
             * Get the requested timestep (taking the first only if an animation
             * is requested)
             */
            List<TimePosition> timeValues = AbstractWmsController.getTimeValues(dr.getTimeString(),
                    feature);
            TimePosition tValue = timeValues.isEmpty() ? null : timeValues.get(0);

            feature = ((GridSeriesFeature) feature).extractGridFeature(grid, zValue, tValue,
                    CollectionUtils.setOf(memberName));
        }
        
        /*
         * Now we have a feature with the data. If it has a discrete coverage,
         * get the value range
         */
        Extent<Float> valueRange = null;
        if (feature.getCoverage() instanceof DiscreteCoverage) {
            DiscreteCoverage<?,?> discreteCoverage = (DiscreteCoverage<?, ?>) feature.getCoverage();
            Class<?> clazz = discreteCoverage.getScalarMetadata(memberName).getValueType();
            final List<?> values = discreteCoverage.getValues(memberName);
            if (Number.class.isAssignableFrom(clazz)) {
                valueRange = Extents.findMinMax(new AbstractList<Float>() {
                    @Override
                    public Float get(int index) {
                        return ((Number)values.get(index)).floatValue();
                    }

                    @Override
                    public int size() {
                        return values.size();
                    }
                });
            } else {
                /*
                 * We have a non-numerical coverage.  It doesn't matter what we return
                 */
                valueRange = Extents.newExtent(0f,100f);
            }
        } else {
            /*
             * We don't know how to handle non-discrete coverages.
             * 
             * At the time of writing, there are no coverages which are non-discrete
             */
            throw new UnsupportedOperationException(
                    "Coverage must be discrete for min-max requests");
        }
        if(valueRange.getLow() == null){
            /*
             * We only have null values in this feature. It doesn't really
             * matter what we return...
             */
            valueRange = Extents.newExtent(0f,100f);
        }
        if(valueRange.getLow().equals(valueRange.getHigh())){
            valueRange = Extents.newExtent(valueRange.getLow()/1.1f, valueRange.getHigh()*1.1f);
        }
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
    private ModelAndView showAnimationTimesteps(HttpServletRequest request) throws WmsException {
        Feature feature = getFeature(request);
        TimeAxis tAxis = WmsUtils.getTimeAxis(feature);

        String startStr = request.getParameter("start");
        String endStr = request.getParameter("end");
        if (startStr == null || endStr == null) {
            throw new WmsException("Must provide values for start and end");
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

        timeStrings.put("Full (" + (endIndex - startIndex + 1) + " frames)", startStr + "/"
                + endStr);
        addTimeString("Daily", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withDays(1));
        addTimeString("Weekly", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withWeeks(1));
        addTimeString("Monthly", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withMonths(1));
        addTimeString("Bi-monthly", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withMonths(2));
        addTimeString("Twice-yearly", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withMonths(6));
        addTimeString("Yearly", timeStrings, tValues, startIndex, endIndex,
                new TimePeriodImpl().withYears(1));

        return new ModelAndView("showAnimationTimesteps", "timeStrings", timeStrings);
    }

    private static void addTimeString(String label, Map<String, String> timeStrings,
            List<TimePosition> tValues, int startIndex, int endIndex, TimePeriod resolution) {
        List<TimePosition> timesteps = getAnimationTimesteps(tValues, startIndex, endIndex,
                resolution);
        // We filter out all the animations with less than one timestep
        if (timesteps.size() > 1) {
            String timeString = getTimeString(timesteps);
            timeStrings.put(label + " (" + timesteps.size() + " frames)", timeString);
        }
    }

    private static List<TimePosition> getAnimationTimesteps(List<TimePosition> tValues,
            int startIndex, int endIndex, TimePeriod resolution) {
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
