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

package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.geotoolkit.referencing.CRS;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.JulianChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.SimpleVectorLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * <p>Collection of static utility methods that are useful in the WMS application.</p>
 *
 * <p>Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, these functions
 * are also available as JSP2.0 functions. For example:</p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * </code>
 *
 * @author Jon Blower
 */
public class WmsUtils
{
    /**
     * The versions of the WMS standard that this server supports
     */
    public static final Set<String> SUPPORTED_VERSIONS = new HashSet<String>();

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_DATE_TIME_PARSER =
        ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_TIME_FORMATTER =
        ISODateTimeFormat.time().withZone(DateTimeZone.UTC);

    private static final String EMPTY_STRING = "";

    // Patterns are immutable and therefore thread-safe.
    private static final Pattern MULTIPLE_WHITESPACE = Pattern.compile("\\s+");

    /**
     * <p>A {@link Comparator} that compares {@link DateTime} objects based only
     * on their millisecond instant values.  This can be used for
     * {@link Collections#sort(java.util.List, java.util.Comparator) sorting} or
     * {@link Collections#binarySearch(java.util.List, java.lang.Object,
     * java.util.Comparator) searching} {@link List}s of {@link DateTime} objects.</p>
     * <p>The ordering defined by this Comparator is <i>inconsistent with equals</i>
     * because it ignores the Chronology of the DateTime instants.</p>
     * <p><i>(Note: The DateTime object inherits from Comparable, not
     * Comparable&lt;DateTime&gt;, so we can't use the methods in Collections
     * directly.  However we can reuse the {@link DateTime#compareTo(java.lang.Object)}
     * method.)</i></p>
     */
    public static final Comparator<DateTime> DATE_TIME_COMPARATOR =
        new Comparator<DateTime>()
    {
        @Override
        public int compare(DateTime dt1, DateTime dt2) {
            return dt1.compareTo(dt2);
        }
    };
    
    static
    {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() { throw new AssertionError(); }

    /**
     * Converts a {@link DateTime} object into an ISO8601-formatted String.
     */
    public static String dateTimeToISO8601(DateTime dateTime)
    {
        return ISO_DATE_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Converts an ISO8601-formatted String into a {@link DateTime} object
     * @throws IllegalArgumentException if the string is not a valid ISO date-time,
     * or if it is not valid within the Chronology (e.g. 31st July in a 360-day
     * calendar).
     */
    public static DateTime iso8601ToDateTime(String isoDateTime, Chronology chronology)
    {
        try
        {
            return ISO_DATE_TIME_PARSER.withChronology(chronology).parseDateTime(isoDateTime);
        }
        catch(RuntimeException re)
        {
            re.printStackTrace();
            throw re;
        }
    }
    
    /**
     * Formats a DateTime as the time only
     * in the format "HH:mm:ss", e.g. "14:53:03".  Time zone offset is zero (UTC).
     */
    public static String formatUTCTimeOnly(DateTime dateTime)
    {
        return ISO_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Searches the given list of timesteps for the specified date-time using the binary
     * search algorithm.  Matches are found based only upon the millisecond
     * instant of the target DateTime, not its Chronology.
     * @param  target The timestep to search for.
     * @return the index of the search key, if it is contained in the list;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>list.size()</tt> if all
     *	       elements in the list are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.  If this Layer does not have a time
     *         axis this method will return -1.
     */
    public static int findTimeIndex(List<DateTime> dtList, DateTime target)
    {
        return Collections.binarySearch(dtList, target, DATE_TIME_COMPARATOR);
    }
    
    /**
     * Creates a directory, throwing an Exception if it could not be created and
     * it does not already exist.
     */
    public static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdirs();
            if (!created)
            {
                throw new Exception("Could not create directory "
                    + dir.getPath());
            }
        }
    }
    
    /**
     * Creates a unique name for a Layer (for display in the Capabilities
     * document) based on a dataset ID and a Layer ID that is unique within a
     * dataset.
     * @todo doesn't belong in generic WmsUtils: specific to ncWMS
     */
    public static String createUniqueLayerName(String datasetId, String layerId)
    {
        return datasetId + "/" + layerId;
    }
    
    /**
     * Converts a string of the form "x1,y1,x2,y2" into a bounding box of four
     * doubles.
     * @throws WmsException if the format of the bounding box is invalid
     */
    public static double[] parseBbox(String bboxStr, boolean lonFirst) throws WmsException
    {
        String[] bboxEls = bboxStr.split(",");
        // Check the validity of the bounding box
        if (bboxEls.length != 4)
        {
            throw new WmsException("Invalid bounding box format: need four elements");
        }
        double[] bbox = new double[4];
        try
        {
            if(lonFirst){
                bbox[0] = Double.parseDouble(bboxEls[0]);
                bbox[1] = Double.parseDouble(bboxEls[1]);
                bbox[2] = Double.parseDouble(bboxEls[2]);
                bbox[3] = Double.parseDouble(bboxEls[3]);
            } else {
                bbox[0] = Double.parseDouble(bboxEls[1]);
                bbox[1] = Double.parseDouble(bboxEls[0]);
                bbox[2] = Double.parseDouble(bboxEls[3]);
                bbox[3] = Double.parseDouble(bboxEls[2]);
            }
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
        }
        if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3])
        {
            throw new WmsException("Invalid bounding box format");
        }
        return bbox;
    }

    /**
     * Calculates the magnitude of the vector components given in the provided
     * Lists.  The two lists must be of the same length.  For any element in the
     * component lists, if either east or north is null, the magnitude will also
     * be null.
     * @return a List of the magnitudes calculated from the components.
     */
    public static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData)
    {
        if (eastData == null || northData == null) throw new NullPointerException();
        if (eastData.size() != northData.size())
        {
            throw new IllegalArgumentException("east and north data components must be the same length");
        }
        List<Float> mag = new ArrayList<Float>(eastData.size());
        for (int i = 0; i < eastData.size(); i++)
        {
            Float east = eastData.get(i);
            Float north = northData.get(i);
            Float val = null;
            if (east != null && north != null)
            {
                val = (float)Math.sqrt(east * east + north * north);
            }
            mag.add(val);
        }
        if (mag.size() != eastData.size()) throw new AssertionError();
        return mag;
    }
    
    /**
     * @return true if the given location represents an OPeNDAP dataset.
     * This method simply checks to see if the location string starts with "http://",
     * "https://" or "dods://".
     */
    public static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://")
            || location.startsWith("https://");
    }
    
    /**
     * @return true if the given location represents an NcML aggregation. dataset.
     * This method simply checks to see if the location string ends with ".xml"
     * or ".ncml", following the same procedure as the Java NetCDF library.
     */
    public static boolean isNcmlAggregation(String location)
    {
        return location.endsWith(".xml") || location.endsWith(".ncml");
    }

    /**
     * Estimate the range of values in this layer by reading a sample of data
     * from the default time and elevation.  Works for both Scalar and Vector
     * layers.
     * @return
     * @throws IOException if there was an error reading from the source data
     */
    public static Extent<Float> estimateValueRange(Layer layer) throws IOException
    {
        if (layer instanceof ScalarLayer)
        {
            List<Float> dataSample = readDataSample((ScalarLayer)layer);
            return Extents.findMinMax(dataSample);
        }
        else if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> eastDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> northDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> magnitudes = WmsUtils.getMagnitudes(eastDataSample, northDataSample);
            return Extents.findMinMax(magnitudes);
        }
        else
        {
            throw new IllegalStateException("Unrecognized layer type");
        }
    }

    private static List<Float> readDataSample(ScalarLayer layer) throws IOException
    {
        try {
            // Read a low-resolution grid of data covering the entire spatial extent
            return layer.readHorizontalPoints(
                layer.getDefaultTimeValue(),
                layer.getDefaultElevationValue(),
                new RegularGridImpl(layer.getBoundingBox(), 100, 100)
            );
        } catch (InvalidDimensionValueException idve) {
            // This would only happen due to a programming error in getDefaultXValue()
            throw new IllegalStateException(idve);
        }
    }

    /**
     * Replaces instances of duplicated whitespace with a single space.
     */
    public static String removeDuplicatedWhiteSpace(String theString)
    {
        return MULTIPLE_WHITESPACE.matcher(theString).replaceAll(" ");
    }

    /**
     * Finds the VectorLayers that can be derived from the given collection of
     * ScalarLayers, by examining the layer Titles (usually CF standard names)
     * and looking for "eastward_X"/"northward_X" pairs.
     */
    public static List<VectorLayer> findVectorLayers(Collection<? extends ScalarLayer> scalarLayers)
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, ScalarLayer[]> components = new LinkedHashMap<String, ScalarLayer[]>();
        for (ScalarLayer layer : scalarLayers)
        {
            if (layer.getTitle().contains("eastward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new ScalarLayer[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().contains("northward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("northward_", "");
                // Look to see if we've already found the eastward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the eastward component yet
                    components.put(vectorKey, new ScalarLayer[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
        }

        // Now add the vector quantities to the collection of Layer objects
        List<VectorLayer> vectorLayers = new ArrayList<VectorLayer>();
        for (String key : components.keySet())
        {
            ScalarLayer[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                // We've found both components.  Create a new Layer object
                VectorLayer vec = new SimpleVectorLayer(key, comps[0], comps[1]);
                vectorLayers.add(vec);
            }
        }

        return vectorLayers;
    }

    /**
     * Returns true if the given layer is a VectorLayer.  This is used in the
     * wmsUtils.tld taglib, since an "instanceof" function is not available in
     * JSTL.
     */
    public static boolean isVectorLayer(Layer layer)
    {
        return layer instanceof VectorLayer;
    }
    
    /**
     * Returns the RuntimeException name. This used in 'displayDefaultException.jsp'
     * to show the exception name, to go around the use of '${exception.class.name}' where 
     * the word 'class' is deemed as Java keyword by Tomcat 7.0 
     *  
     */
    public static String getExceptionName(Exception e)
    {
        return e.getClass().getName();
    }

    /**
     * <p>Returns the string to be used to display units for the TIME dimension
     * in Capabilities documents.  For standard (ISO) chronologies, this will
     * return "ISO8601".  For 360-day chronologies this will return "360_day".
     * For other chronologies this will return "unknown".</p>
     * @todo Should match up with {@link CdmUtils#CHRONOLOGIES}.
     */
    public static String getTimeAxisUnits(Chronology chronology)
    {
        if (chronology instanceof ISOChronology) return "ISO8601";
        // The following are the CF names for these calendars
        if (chronology instanceof JulianChronology) return "julian";
//        if (chronology instanceof ThreeSixtyDayChronology) return "360_day";
//        if (chronology instanceof NoLeapChronology) return "noleap";
//        if (chronology instanceof AllLeapChronology) return "all_leap";
        return "unknown";
    }

    /**
     * <p>Returns a string representing the given List of DateTimes, suitable for
     * inclusion in a Capabilities document.  For regularly-spaced data, this
     * will return a string of the form "start/stop/period".  For irregularly-spaced
     * data this will return the times as a comma-separated list.  For lists with
     * some regular and some irregular spacings, this will use a combination of
     * both approaches.</p>
     * <p>All DateTimes in the provided list are assumed to be in the same
     * {@link Chronology} as the first element of the list.  If this is not the
     * case, undefined behaviour may result.</p>
     * @param times The List of DateTimes to convert to a String.  If this List
     * contains no entries, an empty string will be returned.
     * @return a string representing the given List of DateTimes, suitable for
     * inclusion in a Capabilities document.
     * @throws NullPointerException if {@code times == null}
     */
    public static String getTimeStringForCapabilities(List<DateTime> times)
    {
        // Take care of some simple cases
        if (times == null) throw new NullPointerException();
        if (times.size() == 0) return EMPTY_STRING;
        if (times.size() == 1) return dateTimeToISO8601(times.get(0));

        // We look for sublists that are regularly-spaced
        // This is a simple class that holds the indices of the start and end
        // of these sublists, together with the spacing of the items
        class SubList
        {
            int first, last;
            long spacing;
            int length() { return last - first + 1; }
        }

        List<SubList> subLists = new ArrayList<SubList>();
        SubList currentSubList = new SubList();
        currentSubList.first = 0;
        currentSubList.spacing = times.get(1).getMillis() - times.get(0).getMillis();

        for (int i = 1; i < times.size() - 1; i++)
        {
            long spacing = times.get(i+1).getMillis() - times.get(i).getMillis();
            if (spacing != currentSubList.spacing)
            {
                // Finish off the current sublist and add it to the collection
                currentSubList.last = i;
                subLists.add(currentSubList);
                // Create a new sublist, starting at this point
                currentSubList = new SubList();
                currentSubList.first = i;
                currentSubList.spacing = spacing;
            }
        }

        // Now add the last time
        currentSubList.last = times.size() - 1;
        subLists.add(currentSubList);

        // We now have a collection of sub-lists, each regularly spaced in time.
        // However, we can't simply print these to strings because the some of
        // the times (those on the borders between sublists) would appear twice.
        // For these border times, we need to decide which sublist they belong
        // to.  We choose this by attempting to make the longest sublist possible,
        // until there are no more border times to assign.

        // We must make sure not to deal with the same sublist repeatedly, so
        // we store the indices of the sublists we have dealt with.
        Set<Integer> subListsDone = new HashSet<Integer>(subLists.size());
        boolean done;
        do
        {
            // First we find the longest sublist
            int longestSubListIndex = -1;
            SubList longestSubList = null;
            for (int i = 0; i < subLists.size(); i++)
            {
                // Don't look at sublists we've already dealt with
                if (subListsDone.contains(i)) continue;
                SubList subList = subLists.get(i);
                if (longestSubList == null || subList.length() > longestSubList.length())
                {
                    longestSubListIndex = i;
                    longestSubList = subList;
                }
            }
            subListsDone.add(longestSubListIndex);

            // Now we remove the DateTimes at the borders of this sublist from
            // the adjacent sublists.  Therefore the longest sublist "claims"
            // the borders from its neighbours.
            if (longestSubListIndex > 0)
            {
                // Check the previous sublist
                SubList prevSubList = subLists.get(longestSubListIndex - 1);
                if (prevSubList.last == longestSubList.first)
                {
                    prevSubList.last--;
                }
            }
            if (longestSubListIndex < subLists.size() - 1)
            {
                // Check the next sublist
                SubList nextSubList = subLists.get(longestSubListIndex + 1);
                if (nextSubList.first == longestSubList.last)
                {
                    nextSubList.first++;
                }
            }

            // Check to see if there are any borders that appear in two sublists
            done = true;
            for (int i = 1; i < subLists.size() - 1; i++)
            {
                SubList prev = subLists.get(i - 1);
                SubList cur = subLists.get(i);
                SubList next = subLists.get(i + 1);
                if (prev.last == cur.first || cur.last == next.first)
                {
                    // We still have a contested border
                    done = false;
                    break;
                }
            }
        } while (!done);

        // Now we can simply print out our sublists, comma-separated
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < subLists.size(); i++)
        {
            SubList subList = subLists.get(i);
            List<DateTime> timeList = times.subList(subList.first, subList.last + 1);
            if (timeList.size() > 0)
            {
                if (i > 0) str.append(",");
                str.append(getRegularlySpacedTimeString(timeList, subList.spacing));
            }
        }

        return str.toString();
    }

    /**
     * <p>Creates a time string, suitable for a Capabilities document, that represents
     * the given list of DateTimes, which have previously been calculated to be
     * regularly-spaced according to the given period.</p>
     * <p><i>"Package-private" rather than "private" to enable visibility in unit tests</i></p>
     * @param times The list of DateTimes, which have been previously calculated
     * to be regularly-spaced.  This can have one or more elements; if it only
     * has one or two elements then the {@code period} is ignored.
     * @param period The interval between the dateTimes, in milliseconds
     * @throws IllegalArgumentException if {@code times} has zero elements.
     */
    static StringBuilder getRegularlySpacedTimeString(List<DateTime> times, long period)
    {
        if (times.size() == 0) throw new IllegalArgumentException();
        StringBuilder str = new StringBuilder();
        str.append(dateTimeToISO8601(times.get(0)));
        if (times.size() == 2)
        {
            // No point in specifying the interval, just write the two times
            str.append(",");
            str.append(dateTimeToISO8601(times.get(1)));
        }
        else if (times.size() > 2)
        {
            str.append("/");
            str.append(dateTimeToISO8601(times.get(times.size() - 1)));
            str.append("/");
            str.append(getPeriodString(period));
        }
        return str;
    }

    /**
     * <p>Gets a representation of the given period as an ISO8601 string, e.g.
     * "P1D" for one day, "PT3.5S" for 3.5s.</p>
     * <p>For safety, this will only express periods in days, hours, minutes
     * and seconds.  These are the only durations that are constant in their
     * millisecond length across different Chronologies.  (Years and months
     * can be different lengths between and within Chronologies.)</p>
     * @param period The period in milliseconds
     * @return a representation of the given period as an ISO8601 string
     */
    public static String getPeriodString(long period)
    {
        StringBuilder str = new StringBuilder("P");
        long days = period / DateTimeConstants.MILLIS_PER_DAY;
        if (days > 0)
        {
            str.append(days + "D");
            period -= days * DateTimeConstants.MILLIS_PER_DAY;
        }
        if (period > 0) str.append("T");
        long hours = period / DateTimeConstants.MILLIS_PER_HOUR;
        if (hours > 0)
        {
            str.append(hours + "H");
            period -= hours * DateTimeConstants.MILLIS_PER_HOUR;
        }
        long minutes = period / DateTimeConstants.MILLIS_PER_MINUTE;
        if (minutes > 0)
        {
            str.append(minutes + "M");
            period -= minutes * DateTimeConstants.MILLIS_PER_MINUTE;
        }
        // Now the period represents the number of milliseconds
        if (period > 0)
        {
            long seconds = period / DateTimeConstants.MILLIS_PER_SECOND;
            long millis = period % DateTimeConstants.MILLIS_PER_SECOND;
            str.append(seconds);
            if (millis > 0) str.append("." + addOrRemoveZeros(millis));
            str.append("S");
        }

        return str.toString();
    }

    /** Adds leading zeros and removes trailing zeros as appropriate, to make
     * the given number of milliseconds suitable for placing after a decimal
     * point (e.g. 500 becomes 5, 5 becomes 005). */
    private static String addOrRemoveZeros(long millis)
    {
        if (millis == 0) return "";
        String s = Long.toString(millis);
        if (millis < 10) return "00" + s;

        if (millis < 100) s = "0" + s;
        // Now remove all trailing zeros
        while (s.endsWith("0"))
        {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Finds a {@link CoordinateReferenceSystem} with the given code, forcing
     * longitude-first axis order.
     * @param crsCode The code for the CRS
     * @return a coordinate reference system with the longitude axis first
     * @throws InvalidCrsException if a CRS matching the code cannot be found
     * @throws NullPointerException if {@code crsCode} is null
     */
    public static CoordinateReferenceSystem getCrs(String crsCode) throws InvalidCrsException
    {
        if (crsCode == null) throw new NullPointerException("CRS code cannot be null");
        try
        {
            // the "true" means "force longitude first"
            return CRS.decode(crsCode, true);
        }
        catch(Exception e)
        {
            throw new InvalidCrsException(crsCode);
        }
    }

    /**
     * Gets a {@link RegularGrid} representing the image requested by a client
     * in a GetMap operation
     * @param dr Object representing a GetMap request
     * @return a RegularGrid representing the requested image
     */
    public static RegularGrid getImageGrid(GetMapDataRequest dr)
            throws InvalidCrsException
    {
        CoordinateReferenceSystem crs = getCrs(dr.getCrsCode());
        BoundingBox bbox = new BoundingBoxImpl(dr.getBbox(), crs);
        return new RegularGridImpl(bbox, dr.getWidth(), dr.getHeight());
    }

    /**
     * Returns an ArrayList of null values of the given length
     */
    public static ArrayList<Float> nullArrayList(int n)
    {
        ArrayList<Float> list = new ArrayList<Float>(n);
        Collections.fill(list, null);
        return list;
    }

}
