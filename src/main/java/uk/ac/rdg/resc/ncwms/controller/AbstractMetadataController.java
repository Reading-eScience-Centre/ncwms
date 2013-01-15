package uk.ac.rdg.resc.ncwms.controller;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.DiscreteCoverage;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.feature.UniqueMembersFeatureCollection;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.graphics.PlotStyle;
import uk.ac.rdg.resc.edal.position.CalendarSystem;
import uk.ac.rdg.resc.edal.position.TimePeriod;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalCrs;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.TimePeriodImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
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

    protected final FeatureFactory featureFactory;

    protected Config config;

    protected AbstractMetadataController(Config config, FeatureFactory featureFactory) {
        this.config = config;
        this.featureFactory = featureFactory;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws MetadataException {
        try {
            RequestParams params = new RequestParams(request.getParameterMap());
            String item = params.getString("item");
            if (item == null) {
                throw new Exception("Must provide an ITEM parameter");
            } else if (item.equals("menu")) {
                return this.showMenu(params);
            } else if (item.equals("layerDetails")) {
                return this.showLayerDetails(params);
            } else if (item.equals("timesteps")) {
                return this.showTimesteps(params);
            } else if (item.equals("minmax")) {
                return this.showMinMax(params);
            } else if (item.equals("animationTimesteps")) {
                return this.showAnimationTimesteps(params);
            }
            throw new Exception("Invalid value for ITEM parameter");
        } catch (Exception e) {
            /*
             * Wrap all exceptions in a MetadataException. These will be
             * automatically displayed via displayMetadataException.jsp, in JSON
             * format
             */
            e.printStackTrace();
            throw new MetadataException(e);
        }
    }

    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy. May differ between implementations
     */
    protected abstract ModelAndView showMenu(RequestParams params) throws Exception;

    /**
     * Shows an JSON document containing the details of the given variable
     * (units, zvalues, tvalues etc). See showLayerDetails.jsp.
     */
    protected ModelAndView showLayerDetails(RequestParams params) throws Exception {
        
        String layerName = params.getMandatoryString("layerName");
        
        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        
        String memberName = WmsUtils.getMemberName(layerName);

        BoundingBox bbox;
        boolean multiFeature;
        String units;
        
        Map<String, Object> models = new HashMap<String, Object>();
        Set<PlotStyle> styles = new HashSet<PlotStyle>();
        
        if(featureCollection instanceof UniqueMembersFeatureCollection) {
            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection)
                    .getFeatureContainingMember(memberName);
            multiFeature = false;
            bbox = WmsUtils.getWmsBoundingBox(feature);
            styles = WmsUtils.getBaseStyles(feature, memberName);
            VerticalAxis vAxis = GISUtils.getVerticalAxis(feature);
            List<TimePosition> timeValues = GISUtils.getTimes(feature, false);
            // Find the time the user has requested (this is the time that is
            // currently displayed on the Godiva2 site). If no time has been
            // specified we use the current time
            CalendarSystem calendarSystem = null;
            if (timeValues != null && timeValues.size() > 0) {
                calendarSystem = timeValues.get(0).getCalendarSystem();
            }
            TimePosition targetDateTime = new TimePositionJoda(calendarSystem);
            String targetDateIso = params.getString("time");
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
            if (timeValues == null)
                timeValues = Collections.emptyList();
            TimePosition nearestDateTime = GISUtils.getClosestTimeTo(targetDateTime, timeValues);
            
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
            
            units = MetadataUtils.getUnitsString(feature, memberName);
            if(calendarSystem != null) {
                models.put("tAxisUnits", TimeUtils.getTimeAxisUnits(calendarSystem));
            } else {
                models.put("tAxisUnits", "none");
            }
            models.put("datesWithData", datesWithData);
            models.put("nearestTimeIso", TimeUtils.dateTimeToISO8601(nearestDateTime));
            models.put("vaxis", vAxis);
        } else {
            multiFeature = true;
            
            bbox = featureCollection.getCollectionBoundingBox();
            units = "";
            if(featureCollection.getFeatures() != null) {
                for(Feature feature : featureCollection.getFeatures()){
                    if(feature.getCoverage().getScalarMemberNames().contains(memberName)){
                        units = MetadataUtils.getUnitsString(feature, memberName);
                        styles = WmsUtils.getBaseStyles(feature, memberName);
                        break;
                    }
                }
            }
            
            Extent<VerticalPosition> verticalExtent = featureCollection.getCollectionVerticalExtent();
            Double startZ = verticalExtent.getLow().getZ();
            Double endZ = verticalExtent.getHigh().getZ();
            /*
             * TODO sort out order.
             */
            VerticalCrs verticalCrs = verticalExtent.getHigh().getCoordinateReferenceSystem();
            models.put("startZ", startZ.toString());
            models.put("endZ", endZ.toString());
            models.put("verticalCrs", verticalCrs);
            
            TimePosition startTime = featureCollection.getCollectionTimeExtent().getLow();
            TimePosition endTime = featureCollection.getCollectionTimeExtent().getHigh();
            
            models.put("startTime", TimeUtils.dateTimeToISO8601(startTime));
            models.put("endTime", TimeUtils.dateTimeToISO8601(endTime));
            models.put("tAxisUnits", TimeUtils.getTimeAxisUnits(startTime.getCalendarSystem()));
            
            TimePosition targetDateTime = new TimePositionJoda(startTime.getCalendarSystem());
            String targetDateIso = params.getString("time");
            if (targetDateIso != null && !targetDateIso.trim().equals("")) {
                try {
                    targetDateTime = TimeUtils.iso8601ToDateTime(targetDateIso, startTime.getCalendarSystem());
                } catch (IllegalArgumentException iae) {
                    // targetDateIso was not valid for the layer's chronology
                    // We swallow this exception: targetDateTime will remain
                    // unchanged.
                }
            }
            if(targetDateTime.compareTo(startTime) < 0){
                targetDateTime = startTime;
            } else if(targetDateTime.compareTo(endTime) > 0){
                targetDateTime = endTime;
            }
            models.put("nearestTimeIso", TimeUtils.dateTimeToISO8601(targetDateTime));
        }
        
        models.put("bbox", bbox);
        models.put("multiFeature", multiFeature);
        models.put("units", units);
        models.put("styles", styles);
        
        FeaturePlottingMetadata plottingMetadata = WmsUtils.getMetadata(config, layerName);
        models.put("featureMetadata", plottingMetadata);
        models.put("memberName", memberName);
        models.put("dataset", WmsUtils.getDataset(config, layerName));
        /*
         * The names of the palettes supported by this layer. Actually this will
         * be the same for all layers, but we can't put this in the menu because
         * there might be several menu JSPs.
         */
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        return new ModelAndView("showLayerDetails", models);
    }

    /**
     * Finds all the timesteps that occur on the given date, which will be
     * provided in the form "2007-10-18".
     */
    private ModelAndView showTimesteps(RequestParams params) throws Exception {
        String layerName = params.getMandatoryString("layerName");
        
        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        
        if(!(featureCollection instanceof UniqueMembersFeatureCollection)){
            throw new WmsException(
                    "The method GetMetadata, item=timesteps is not valid for datasets with a continuous time axis");
        } else {
            String memberName = WmsUtils.getMemberName(layerName);
            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection).getFeatureContainingMember(memberName);
            List<TimePosition> tValues = GISUtils.getTimes(feature, false);
            
            if (tValues == null || tValues.isEmpty())
                return null; // return no data if no time axis present

            String dayStr = params.getString("day");
            if (dayStr == null) {
                throw new Exception("Must provide a value for the day parameter");
            }
            // We can do a get(0) here, because we have already checked that tValues.isEmpty() is false
            TimePosition date = TimeUtils.iso8601ToDate(dayStr, tValues.get(0).getCalendarSystem());
            // List of date-times that fall on this day
            List<TimePosition> timesteps = new ArrayList<TimePosition>();
            // Search exhaustively through the layer's valid time values
            // TODO: inefficient: should stop once last day has been found.
            for (TimePosition tVal : tValues) {
                if (onSameDay(tVal, date)) {
                    timesteps.add(tVal);
                }
            }
            log.debug("Found {} timesteps on {}", timesteps.size(), dayStr);

            return new ModelAndView("showTimesteps", "timesteps", timesteps);    
        }
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
    private ModelAndView showMinMax(RequestParams params) throws Exception {
        
        /*
         * We only need the bit of the GetMap request that pertains to data
         * extraction
         */
        GetMapDataRequest dr = new GetMapDataRequest(params, params.getWmsVersion());
        String[] layerNames = dr.getLayers();
        if(layerNames == null || layerNames.length != 1) {
            throw new WmsException("Must request exactly one WMS layer to get min/max");
        }
        String layerName = layerNames[0];
        String memberName = WmsUtils.getMemberName(layerName);

        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        
        Extent<Float> valueRange = null;
        RegularGrid grid = WmsUtils.getImageGrid(dr);
        
        if(featureCollection instanceof UniqueMembersFeatureCollection){
            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection).getFeatureContainingMember(memberName);
            
            memberName = MetadataUtils.getScalarMemberName(feature, memberName);
    
            /*
             * If we have a grid series feature, extract the relevant portion of
             * the data
             */
            VerticalPosition zValue = GISUtils.getExactElevation(dr.getElevationString(),
                    GISUtils.getVerticalAxis(feature));
            /*
             * Get the requested timestep (taking the first only if an animation
             * is requested)
             */
            List<TimePosition> timeValues = WmsUtils.getTimePositionsForString(dr.getTimeString(), feature);
            TimePosition tValue = timeValues.isEmpty() ? null : timeValues.get(0);
            
            valueRange = getFeatureMinMax(feature, zValue, tValue, memberName, grid);
        } else {
            Extent<TimePosition> tRange = TimeUtils.getTimeRangeForString(dr.getTimeString(),
                    CalendarSystem.CAL_ISO_8601);
            Extent<Double> zRange = WmsUtils.getElevationRangeForString(dr.getElevationString());
            Collection<? extends Feature> features = featureCollection.findFeatures(
                    grid.getCoordinateExtent(), zRange, tRange, CollectionUtils.setOf(memberName));
            
            TimePosition colorByTime = null;
            if(dr.getColorbyTimeString() != null){
                colorByTime = TimeUtils.iso8601ToDateTime(dr.getColorbyTimeString(), CalendarSystem.CAL_ISO_8601);
            }
            
            Double colorByDepth = null;
            if(dr.getColorbyElevationString() != null){
                colorByDepth = Double.parseDouble(dr.getColorbyElevationString()); 
            }
            
            float max = -Float.MAX_VALUE;
            float min = Float.MAX_VALUE;
            for(Feature feature : features){
                memberName = MetadataUtils.getScalarMemberName(feature, memberName);
                
                VerticalPosition vPos  = GISUtils.getClosestElevationTo(colorByDepth, GISUtils.getVerticalAxis(feature));
                TimePosition tPos = GISUtils.getClosestTimeTo(colorByTime, GISUtils.getTimes(feature, false));
                
                Extent<Float> minMax = getFeatureMinMax(feature, vPos, tPos, memberName, null);
                
                if(minMax.getLow() != null && minMax.getLow() < min){
                    min = minMax.getLow();
                } 
                if(minMax.getHigh() != null && minMax.getHigh() > max){
                    max = minMax.getHigh();
                }
            }
            if(max < min){
                /*
                 * We haven't found any features with values.
                 */
                valueRange = Extents.emptyExtent(Float.class);
            } else {
                valueRange = Extents.newExtent(min, max);
            }
        }
        
        if(valueRange.getLow() == null){
            /*
             * We only have null values in this feature. It doesn't really
             * matter what we return...
             */
            valueRange = Extents.emptyExtent(Float.class);
        } else if(valueRange.getLow().equals(valueRange.getHigh())){
            valueRange = Extents.newExtent(valueRange.getLow()/1.1f, valueRange.getHigh()*1.1f);
        }
        return new ModelAndView("showMinMax", "valueRange", valueRange);
    }
    
    private Extent<Float> getFeatureMinMax(Feature feature, VerticalPosition zValue, TimePosition tValue, String memberName, RegularGrid grid){
        memberName = MetadataUtils.getScalarMemberName(feature, memberName);
        
        if(feature instanceof PointSeriesFeature){
            PointSeriesFeature pointSeriesFeature = (PointSeriesFeature) feature;
            Object value = pointSeriesFeature.getCoverage().evaluate(tValue, memberName);
            if(Number.class.isAssignableFrom(value.getClass())){
                return Extents.newExtent(((Number)value).floatValue(), ((Number)value).floatValue());
            } else {
                return Extents.emptyExtent(Float.class);
            }
        } else if(feature instanceof ProfileFeature){
            ProfileFeature profileFeature = (ProfileFeature) feature;
            Object value = profileFeature.getCoverage().evaluate(zValue, memberName);
            
            if(value == null || (!Number.class.isAssignableFrom(value.getClass()))){
                return Extents.emptyExtent(Float.class);
            } else {
                return Extents.newExtent(((Number)value).floatValue(), ((Number)value).floatValue());
            }
        } else if(feature instanceof GridSeriesFeature) {
            feature = ((GridSeriesFeature) feature).extractGridFeature(grid, zValue, tValue,
                    CollectionUtils.setOf(memberName));
        } else if (feature instanceof GridFeature){
            feature = ((GridFeature) feature).extractGridFeature(grid, CollectionUtils.setOf(memberName));
        }
        
        Extent<Float> valueRange;
        /*
         * Now we have a feature with the data. If it has a discrete coverage,
         * get the value range
         */
        if (feature.getCoverage() instanceof DiscreteCoverage) {
            DiscreteCoverage<?,?> discreteCoverage = (DiscreteCoverage<?, ?>) feature.getCoverage();
            Class<?> clazz = discreteCoverage.getScalarMetadata(memberName).getValueType();
            final List<?> values = discreteCoverage.getValues(memberName);
            if (Number.class.isAssignableFrom(clazz)) {
                valueRange = Extents.findMinMax(new AbstractList<Float>() {
                    @Override
                    public Float get(int index) {
                        Number val = (Number)values.get(index);
                        return val == null ? null : val.floatValue();
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
            return Extents.emptyExtent(Float.class);
        }
        return valueRange;
    }

    /**
     * Calculates the TIME strings necessary to generate animations for the
     * given layer at hourly, daily, weekly, monthly and yearly resolution.
     * 
     * @param params
     * @return
     * @throws java.lang.Exception
     */
    private ModelAndView showAnimationTimesteps(RequestParams params) throws WmsException {
        String layerName = params.getMandatoryString("layerName");
        String memberName = WmsUtils.getMemberName(layerName);
        
        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        
        if(featureCollection instanceof UniqueMembersFeatureCollection) {
            Feature feature = ((UniqueMembersFeatureCollection<?>) featureCollection)
                    .getFeatureContainingMember(memberName);
            
            List<TimePosition> tValues = GISUtils.getTimes(feature, false);

            if(tValues == null){
                throw new WmsException("There is no time axis - cannot create animation");
            }
            String startStr = params.getString("start");
            String endStr = params.getString("end");
            if (startStr == null || endStr == null) {
                throw new WmsException("Must provide values for start and end");
            }

            // Find the start and end indices along the time axis
            int startIndex = WmsUtils.findTIndex(startStr, tValues);
            int endIndex = WmsUtils.findTIndex(endStr, tValues);

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
        } else {
            throw new WmsException("Animations are not supported for this type of variable");
        }
        
        
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
