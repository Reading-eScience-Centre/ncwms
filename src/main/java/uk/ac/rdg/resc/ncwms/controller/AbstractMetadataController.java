/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.LayerFactory;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @author Jon Blower
 */
public abstract class AbstractMetadataController
{
    private static final Logger log = LoggerFactory.getLogger(AbstractMetadataController.class);

    private final LayerFactory layerFactory;

    protected AbstractMetadataController(LayerFactory layerFactory)
    {
        this.layerFactory = layerFactory;
    }
    
    public ModelAndView handleRequest(HttpServletRequest request,
        HttpServletResponse response, UsageLogEntry usageLogEntry)
        throws MetadataException
    {
        try
        {
            String item = request.getParameter("item");
            usageLogEntry.setWmsOperation("GetMetadata:" + item);
            if (item == null)
            {
                throw new Exception("Must provide an ITEM parameter");
            }
            else if (item.equals("menu"))
            {
                return this.showMenu(request, usageLogEntry);
            }
            else if (item.equals("layerDetails"))
            {
                return this.showLayerDetails(request, usageLogEntry);
            }
            else if (item.equals("timesteps"))
            {
                return this.showTimesteps(request);
            }
            else if (item.equals("minmax"))
            {
                return this.showMinMax(request, usageLogEntry);
            }
            else if (item.equals("animationTimesteps"))
            {
                return this.showAnimationTimesteps(request);
            }
            throw new Exception("Invalid value for ITEM parameter");
        }
        catch(Exception e)
        {
            // Wrap all exceptions in a MetadataException.  These will be automatically
            // displayed via displayMetadataException.jsp, in JSON format
            throw new MetadataException(e);
        }
    }
    
    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy.  May differ between implementations
     */
    protected abstract ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry) throws Exception;
    
    /**
     * Shows an JSON document containing the details of the given variable (units,
     * zvalues, tvalues etc).  See showLayerDetails.jsp.
     */
    private ModelAndView showLayerDetails(HttpServletRequest request,
        UsageLogEntry usageLogEntry) throws Exception
    {
        Layer layer = this.getLayer(request);
        usageLogEntry.setLayer(layer);
        
        // Find the time the user has requested (this is the time that is
        // currently displayed on the Godiva2 site).  If no time has been
        // specified we use the current time
        DateTime targetDateTime = new DateTime(layer.getChronology());
        String targetDateIso = request.getParameter("time");
        if (targetDateIso != null && !targetDateIso.trim().equals(""))
        {
            try
            {
                targetDateTime = WmsUtils.iso8601ToDateTime(targetDateIso, layer.getChronology());
            }
            catch(IllegalArgumentException iae)
            {
                // targetDateIso was not valid for the layer's chronology
                // We swallow this exception: targetDateTime will remain
                // unchanged.
            }
        }
        
        Map<Integer, Map<Integer, List<Integer>>> datesWithData =
            new LinkedHashMap<Integer, Map<Integer, List<Integer>>>();
        List<DateTime> timeValues = layer.getTimeValues();
        DateTime nearestDateTime = timeValues.isEmpty() ? new DateTime(0) : timeValues.get(0);
        
        // Takes an array of time values for a layer and turns it into a Map of
        // year numbers to month numbers to day numbers, for use in
        // showVariableDetails.jsp.  This is used to provide a list of days for
        // which we have data.  Also calculates the nearest value on the time axis
        // to the time we're currently displaying on the web interface.
        for (DateTime dateTime : layer.getTimeValues())
        {
            // We must make sure that dateTime() is in UTC or getDayOfMonth() etc
            // might return unexpected results
            dateTime = dateTime.withZone(DateTimeZone.UTC);
            // See whether this dateTime is closer to the target dateTime than
            // the current closest value
            long d1 = new Duration(dateTime, targetDateTime).getMillis();
            long d2 = new Duration(nearestDateTime, targetDateTime).getMillis();
            if (Math.abs(d1) < Math.abs(d2)) nearestDateTime = dateTime;

            int year = dateTime.getYear();
            Map<Integer, List<Integer>> months = datesWithData.get(year);
            if (months == null)
            {
                months = new LinkedHashMap<Integer, List<Integer>>();
                datesWithData.put(year, months);
            }
            // We need to subtract 1 from the month number as Javascript months
            // are 0-based (Joda-time months are 1-based).  This retains
            // compatibility with previous behaviour.
            int month = dateTime.getMonthOfYear() - 1;
            List<Integer> days = months.get(month);
            if (days == null)
            {
                days = new ArrayList<Integer>();
                months.put(month, days);
            }
            int day = dateTime.getDayOfMonth();
            if (!days.contains(day)) days.add(day);
        }
        
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("layer", layer);
        models.put("datesWithData", datesWithData);
        models.put("nearestTimeIso", WmsUtils.dateTimeToISO8601(nearestDateTime));
        // The names of the palettes supported by this layer.  Actually this
        // will be the same for all layers, but we can't put this in the menu
        // because there might be several menu JSPs.
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        return new ModelAndView("showLayerDetails", models);
    }
    
    /**
     * @return the Layer that the user is requesting, throwing an
     * Exception if it doesn't exist or if there was a problem reading from the
     * data store.
     */
    private Layer getLayer(HttpServletRequest request) throws LayerNotDefinedException
    {
        String layerName = request.getParameter("layerName");
        if (layerName == null)
        {
            throw new LayerNotDefinedException("null");
        }
        return this.layerFactory.getLayer(layerName);
    }
    
    /**
     * Finds all the timesteps that occur on the given date, which will be provided
     * in the form "2007-10-18".
     */
    private ModelAndView showTimesteps(HttpServletRequest request)
        throws Exception
    {
        Layer layer = getLayer(request);
        if (layer.getTimeValues().isEmpty()) return null; // return no data if no time axis present
        
        String dayStr = request.getParameter("day");
        if (dayStr == null)
        {
            throw new Exception("Must provide a value for the day parameter");
        }
        DateTime date = WmsUtils.iso8601ToDateTime(dayStr, layer.getChronology());
        
        // List of date-times that fall on this day
        List<DateTime> timesteps = new ArrayList<DateTime>();
        // Search exhaustively through the layer's valid time values
        // TODO: inefficient: should stop once last day has been found.
        for (DateTime tVal : layer.getTimeValues())
        {
            if (onSameDay(tVal, date))
            {
                timesteps.add(tVal);
            }
        }
        log.debug("Found {} timesteps on {}", timesteps.size(), dayStr);
        
        return new ModelAndView("showTimesteps", "timesteps", timesteps);
    }
    
    /**
     * @return true if the two given DateTimes fall on the same day.
     */
    private static boolean onSameDay(DateTime dt1, DateTime dt2)
    {
        // We must make sure that the DateTimes are both in UTC or the field
        // comparisons will not do what we expect
        dt1 = dt1.withZone(DateTimeZone.UTC);
        dt2 = dt2.withZone(DateTimeZone.UTC);
        boolean onSameDay = dt1.getYear() == dt2.getYear()
                         && dt1.getMonthOfYear() == dt2.getMonthOfYear()
                         && dt1.getDayOfMonth() == dt2.getDayOfMonth();
        log.debug("onSameDay({}, {}) = {}", new Object[]{dt1, dt2, onSameDay});
        return onSameDay;
    }
    
    /**
     * Shows an XML document containing the minimum and maximum values for the
     * tile given in the parameters.
     */
    private ModelAndView showMinMax(HttpServletRequest request,
        UsageLogEntry usageLogEntry) throws Exception
    {
        RequestParams params = new RequestParams(request.getParameterMap());
        // We only need the bit of the GetMap request that pertains to data extraction
        // TODO: the hard-coded "1.3.0" is ugly: it basically means that the
        // GetMapDataRequest object will look for "CRS" instead of "SRS"
        GetMapDataRequest dr = new GetMapDataRequest(params, "1.3.0");
        
        // Get the variable we're interested in
        Layer layer = this.layerFactory.getLayer(dr.getLayers()[0]);
        usageLogEntry.setLayer(layer);
        
        // Get the grid onto which the data is being projected
        RegularGrid grid = WmsUtils.getImageGrid(dr);
        
        // Get the value on the z axis
        double zValue = AbstractWmsController.getElevationValue(dr.getElevationString(), layer);
        
        // Get the requested timestep (taking the first only if an animation is requested)
        List<DateTime> timeValues = AbstractWmsController.getTimeValues(dr.getTimeString(), layer);
        DateTime tValue = timeValues.isEmpty() ? null : timeValues.get(0);
        
        // Now read the data and calculate the minimum and maximum values
        List<Float> magnitudes;
        if (layer instanceof ScalarLayer)
        {
            magnitudes = ((ScalarLayer)layer).readHorizontalPoints(tValue, zValue, grid);
        }
        else if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> east = vecLayer.getEastwardComponent().readHorizontalPoints(tValue, zValue, grid);
            List<Float> north = vecLayer.getNorthwardComponent().readHorizontalPoints(tValue, zValue, grid);
            magnitudes = WmsUtils.getMagnitudes(east, north);
        }
        else
        {
            throw new IllegalStateException("Invalid Layer type");
        }

        Extent<Float> valueRange = Extents.findMinMax(magnitudes);
        return new ModelAndView("showMinMax", "valueRange", valueRange);
    }

    /**
     * Calculates the TIME strings necessary to generate animations for the
     * given layer at hourly, daily, weekly, monthly and yearly resolution.
     * @param request
     * @return
     * @throws java.lang.Exception
     */
    private ModelAndView showAnimationTimesteps(HttpServletRequest request)
        throws Exception
    {
        Layer layer = this.getLayer(request);
        String startStr = request.getParameter("start");
        String endStr = request.getParameter("end");
        if (startStr == null || endStr == null)
        {
            throw new Exception("Must provide values for start and end");
        }

        // Find the start and end indices along the time axis
        int startIndex = AbstractWmsController.findTIndex(startStr, layer);
        int endIndex = AbstractWmsController.findTIndex(endStr, layer);
        List<DateTime> tValues = layer.getTimeValues();

        // E.g.: {
        //  "Full" : "start/end",
        //  "Hourly" : "t1,t2,t3,t4",
        //  "Daily" : "ta,tb,tc,td"
        //  etc
        //  }
        Map<String, String> timeStrings = new LinkedHashMap<String, String>();

        timeStrings.put("Full (" + (endIndex - startIndex + 1) + " frames)", startStr + "/" + endStr);
        addTimeString("Daily", timeStrings, tValues, startIndex, endIndex, new Period().withDays(1));
        addTimeString("Weekly", timeStrings, tValues, startIndex, endIndex, new Period().withWeeks(1));
        addTimeString("Monthly", timeStrings, tValues, startIndex, endIndex, new Period().withMonths(1));
        addTimeString("Bi-monthly", timeStrings, tValues, startIndex, endIndex, new Period().withMonths(2));
        addTimeString("Twice-yearly", timeStrings, tValues, startIndex, endIndex, new Period().withMonths(6));
        addTimeString("Yearly", timeStrings, tValues, startIndex, endIndex, new Period().withYears(1));

        return new ModelAndView("showAnimationTimesteps", "timeStrings", timeStrings);
    }

    private static void addTimeString(String label, Map<String, String> timeStrings,
        List<DateTime> tValues, int startIndex, int endIndex, Period resolution)
    {
        List<DateTime> timesteps = getAnimationTimesteps(tValues, startIndex, endIndex, resolution);
        // We filter out all the animations with less than one timestep
        if (timesteps.size() > 1)
        {
            String timeString = getTimeString(timesteps);
            timeStrings.put(label + " (" + timesteps.size() + " frames)", timeString);
        }
    }

    private static List<DateTime> getAnimationTimesteps(List<DateTime> tValues, int startIndex,
        int endIndex, Period resolution)
    {
        List<DateTime> times = new ArrayList<DateTime>();
        times.add(tValues.get(startIndex));
        for (int i = startIndex + 1; i <= endIndex; i++)
        {
            DateTime lastdt = times.get(times.size() - 1);
            DateTime thisdt = tValues.get(i);
            if (!thisdt.isBefore(lastdt.plus(resolution)))
            {
                times.add(thisdt);
            }
        }
        return times;
    }

    private static String getTimeString(List<DateTime> timesteps)
    {
        if (timesteps.size() == 0) return "";
        StringBuilder builder = new StringBuilder(WmsUtils.dateTimeToISO8601(timesteps.get(0)));
        for (int i = 1; i < timesteps.size(); i++)
        {
            builder.append("," + WmsUtils.dateTimeToISO8601(timesteps.get(i)));
        }
        return builder.toString();
    }
    
}
