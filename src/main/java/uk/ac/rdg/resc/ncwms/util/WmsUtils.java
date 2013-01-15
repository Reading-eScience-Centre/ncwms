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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.feature.TrajectoryFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.graphics.PlotStyle;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.GeoPositionImpl;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * <p>
 * Collection of static utility methods that are useful in the WMS application.
 * </p>
 * 
 * <p>
 * Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, some of these
 * functions are also available as JSP2.0 functions. For example:
 * </p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * </code>
 * 
 * @author Jon Blower
 * @author Guy Griffiths
 */
public class WmsUtils {
    /**
     * The versions of the WMS standard that this server supports
     */
    public static final Set<String> SUPPORTED_VERSIONS = new HashSet<String>();

    static {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() {
        throw new AssertionError();
    }

    /**
     * Creates a directory, throwing an Exception if it could not be created and
     * it does not already exist.
     */
    public static void createDirectory(File dir) throws Exception {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                return;
            } else {
                throw new Exception(dir.getPath() + " already exists but it is a regular file");
            }
        } else {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new Exception("Could not create directory " + dir.getPath());
            }
        }
    }

    /**
     * Converts a string of the form "x1,y1,x2,y2" into a bounding box of four
     * doubles.
     * 
     * @throws WmsException
     *             if the format of the bounding box is invalid
     */
    public static double[] parseBbox(String bboxStr, boolean lonFirst) throws WmsException {
        String[] bboxEls = bboxStr.split(",");
        // Check the validity of the bounding box
        if (bboxEls.length != 4) {
            throw new WmsException("Invalid bounding box format: need four elements");
        }
        double[] bbox = new double[4];
        try {
            if (lonFirst) {
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
        } catch (NumberFormatException nfe) {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
        }
        if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) {
            throw new WmsException("Invalid bounding box format");
        }
        return bbox;
    }

    /**
     * @return true if the given location represents an OPeNDAP dataset. This
     *         method simply checks to see if the location string starts with
     *         "http://", "https://" or "dods://".
     */
    public static boolean isOpendapLocation(String location) {
        return location.startsWith("http://") || location.startsWith("dods://")
                || location.startsWith("https://");
    }

    /**
     * @return true if the given location represents an NcML aggregation.
     *         dataset. This method simply checks to see if the location string
     *         ends with ".xml" or ".ncml", following the same procedure as the
     *         Java NetCDF library.
     */
    public static boolean isNcmlAggregation(String location) {
        return location.endsWith(".xml") || location.endsWith(".ncml");
    }

    /**
     * Finds a {@link CoordinateReferenceSystem} with the given code, forcing
     * longitude-first axis order.
     * 
     * @param crsCode
     *            The code for the CRS
     * @return a coordinate reference system with the longitude axis first
     * @throws InvalidCrsException
     *             if a CRS matching the code cannot be found
     * @throws NullPointerException
     *             if {@code crsCode} is null
     */
    public static CoordinateReferenceSystem getCrs(String crsCode) throws InvalidCrsException {
        if (crsCode == null)
            throw new NullPointerException("CRS code cannot be null");
        try {
            // the "true" means "force longitude first"
            return CRS.decode(crsCode, true);
        } catch (Exception e) {
            throw new InvalidCrsException(crsCode);
        }
    }

    /**
     * Gets a {@link RegularGrid} representing the image requested by a client
     * in a GetMap operation
     * 
     * @param dr
     *            Object representing a GetMap request
     * @return a RegularGrid representing the requested image
     */
    public static RegularGrid getImageGrid(GetMapDataRequest dr) throws InvalidCrsException {
        CoordinateReferenceSystem crs = getCrs(dr.getCrsCode());
        BoundingBox bbox = new BoundingBoxImpl(dr.getBbox(), crs);
        return new RegularGridImpl(bbox, dr.getWidth(), dr.getHeight());
    }

    public static Dataset getDataset(Config serverConfig, String layerName) {
        int slashIndex = layerName.indexOf("/");
        String datasetId = layerName.substring(0, slashIndex);
        return serverConfig.getDatasetById(datasetId);
    }

    public static FeaturePlottingMetadata getMetadata(Config serverConfig, String layerName)
            throws WmsException {
        
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 2) {
            throw new WmsException("Layers should be of the form Dataset/Variable");
        }

        String datasetId = layerParts[0];
        String memberId = layerParts[1];

        Dataset dataset = serverConfig.getDatasetById(datasetId);

        FeaturePlottingMetadata metadata = dataset.getPlottingMetadataMap().get(memberId);
        if(metadata == null){
            metadata = new FeaturePlottingMetadata();
        }
        return metadata;
    }

    /**
     * Utility method for getting the layer name (unique within a Capabilities
     * document) from the given GetMapRequest, checking that there is only one
     * layer in the request
     */
    public static String getWmsLayerName(GetMapDataRequest getMapDataRequest) throws WmsException {
        // Find which layer the user is requesting
        String[] layers = getMapDataRequest.getLayers();
        if (layers.length == 0) {
            throw new WmsException("Must provide a value for the LAYERS parameter");
        }
        if (layers.length > AbstractWmsController.LAYER_LIMIT) {
            throw new WmsException("You may only create a map from " + AbstractWmsController.LAYER_LIMIT
                    + " layer(s) at a time");
        }
        /*
         * Note that this method is only appropriate for a LAYER_LIMIT of 1.
         */
        return layers[0];
    }

    /**
     * Utility method for getting the dataset ID from the given layer name
     */
    public static String getDatasetId(String layerName) throws WmsException {
        // Find which layer the user is requesting
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 2) {
            throw new WmsException("Layers should be of the form Dataset/Variable");
        }
        return layerParts[0];
    }

    /**
     * Utility method for getting the member name from the given layer name
     */
    public static String getMemberName(String layerName) throws WmsException {
        // Find which layer the user is requesting
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 2) {
            throw new WmsException("Layers should be of the form Dataset/Variable");
        }
        return layerParts[1];
    }

    /*
     * Starting here, we have methods which are only found in the wmsUtls.tld
     * tag library.
     * 
     * Don't delete them just because you can't find where they're used in Java
     * code
     */

    public static class StyleInfo {
        private String stylename;
        private String palettename;

        public StyleInfo(String stylename, String palettename) {
            super();
            this.stylename = stylename;
            this.palettename = palettename;
        }

        public String getStylename() {
            return stylename;
        }

        public String getPalettename() {
            return palettename;
        }

        @Override
        public String toString() {
            return stylename + "/" + palettename;
        }
    }

    /**
     * Gets the styles available for a particular layer
     * 
     * @param feature
     *            the feature containing the layer
     * @param memberName
     *            the member of the coverage
     * @param palettes
     *            the available palettes to generate styles for
     * @return A list of styles
     */
    public static List<StyleInfo> getStylesWithPalettes(Feature feature, String memberName, Set<String> palettes) {
        Set<PlotStyle> baseStyles = getBaseStyles(feature, memberName);

        List<StyleInfo> ret = new ArrayList<StyleInfo>();
        for (PlotStyle style : baseStyles) {
            boolean usesPalette = false;
            if(style == PlotStyle.DEFAULT){
                ScalarMetadata scalarMetadata = MetadataUtils.getScalarMetadata(feature, memberName);
                if(scalarMetadata != null){
                    usesPalette = PlotStyle.getDefaultPlotStyle(feature, scalarMetadata).usesPalette();
                }
            } else {
                usesPalette = style.usesPalette();
            }
            if (usesPalette) {
                for (String palette : palettes) {
                    ret.add(new StyleInfo(style.name(), palette));
                }
            } else {
                ret.add(new StyleInfo(style.name(), ""));
            }
        }
        return ret;
    }

    /**
     * Gets the base styles (i.e. without palette names) available for a particular layer
     * 
     * @param feature
     *            the feature containing the layer
     * @param memberName
     *            the member of the coverage
     * @return A list of styles
     */
    public static Set<PlotStyle> getBaseStyles(Feature feature, String memberName) {
        Set<PlotStyle> styles = new LinkedHashSet<PlotStyle>();
        styles.add(PlotStyle.DEFAULT);
        RangeMetadata metadata = MetadataUtils.getMetadataForFeatureMember(feature, memberName);
        if(metadata instanceof ScalarMetadata){
            ScalarMetadata scalarMetadata = (ScalarMetadata) metadata;
            for(PlotStyle style : PlotStyle.getAllowedPlotStyles(feature, scalarMetadata)){
                styles.add(style);
            }
        }
        return styles;
    }

    /**
     * Returns the RuntimeException name. This used in
     * 'displayDefaultException.jsp' to show the exception name, to go around
     * the use of '${exception.class.name}' where the word 'class' is deemed as
     * Java keyword by Tomcat 7.0
     * 
     */
    public static String getExceptionName(Exception e) {
        return e.getClass().getName();
    }

    /**
     * Utility method to check if a particular child member of a metadata is
     * scalar
     * 
     * @param metadata
     *            the parent metadata object
     * @param memberName
     *            the member to check
     * @return true if the child member is an instance of {@link ScalarMetadata}
     */
    public static boolean memberIsScalar(RangeMetadata metadata, String memberName) {
        return metadata.getMemberMetadata(memberName) instanceof ScalarMetadata;
    }

    /**
     * Utility method to return the child metadata of a {@link RangeMetadata}
     * object
     * 
     * @param metadata
     *            the parent metadata object
     * @param memberName
     *            the desired child member id
     * @return the child {@link RangeMetadata}
     */
    public static RangeMetadata getChildMetadata(RangeMetadata metadata, String memberName) {
        return metadata.getMemberMetadata(memberName);
    }

    /*
     * End of methods only present for the taglib
     */

    /*
     * The following methods all depend on the class type of the feature. If new
     * feature types are added, these methods should be looked at, since they
     * are likely to need to change
     */

    public static Object getFeatureValue(Feature feature, HorizontalPosition pos,
            VerticalPosition zPos, TimePosition time, String memberName) {
        /*
         * TODO check for position threshold. We don't necessarily want to
         * return a value...
         */
        if (feature instanceof GridSeriesFeature) {
            return ((GridSeriesFeature) feature).getCoverage().evaluate(
                    new GeoPositionImpl(pos, zPos, time), memberName);
        } else if (feature instanceof PointSeriesFeature) {
            return ((PointSeriesFeature) feature).getCoverage().evaluate(time, memberName);
        } else if (feature instanceof ProfileFeature) {
            return ((ProfileFeature) feature).getCoverage().evaluate(zPos, memberName);
        } else if (feature instanceof GridFeature) {
            return ((GridFeature) feature).getCoverage().evaluate(pos, memberName);
        } else if (feature instanceof TrajectoryFeature) {
            return ((TrajectoryFeature) feature).getCoverage().evaluate(
                    new GeoPositionImpl(pos, zPos, time), memberName);
        }
        return null;
    }

    public static BoundingBox getWmsBoundingBox(Feature feature) {
        BoundingBox inBbox;
        if (feature instanceof GridSeriesFeature) {
            inBbox = ((GridSeriesFeature) feature).getCoverage().getDomain().getHorizontalGrid()
                    .getCoordinateExtent();
        } else if (feature instanceof GridFeature) {
            inBbox = ((GridFeature) feature).getCoverage().getDomain().getCoordinateExtent();
        } else if (feature instanceof PointSeriesFeature) {
            HorizontalPosition pos = ((PointSeriesFeature) feature).getHorizontalPosition();
            return getBoundingBoxForSinglePosition(pos);
        } else if (feature instanceof ProfileFeature) {
            HorizontalPosition pos = ((ProfileFeature) feature).getHorizontalPosition();
            return getBoundingBoxForSinglePosition(pos);
        } else if (feature instanceof TrajectoryFeature) {
            TrajectoryFeature trajectoryFeature = (TrajectoryFeature) feature;
            trajectoryFeature.getCoverage().getDomain().getDomainObjects();
            return ((TrajectoryFeature) feature).getCoverage().getDomain().getCoordinateBounds();
        } else {
            throw new IllegalArgumentException("Unknown feature type");
        }
        // TODO: should take into account the cell bounds
        double minLon = inBbox.getMinX() % 360;
        double maxLon = inBbox.getMaxX() % 360;
        double minLat = inBbox.getMinY();
        double maxLat = inBbox.getMaxY();
        // Correct the bounding box in case of mistakes or in case it
        // crosses the date line
        if ((minLon < 180 && maxLon > 180) || (minLon < -180 && maxLon > -180) || minLon >= maxLon) {
            minLon = -180.0;
            maxLon = 180.0;
        }
        if (minLat >= maxLat) {
            minLat = -90.0;
            maxLat = 90.0;
        }
        // Sometimes the bounding boxes can be NaN, e.g. for a
        // VerticalPerspectiveView
        // that encompasses more than the Earth's disc
        minLon = Double.isNaN(minLon) ? -180.0 : minLon;
        minLat = Double.isNaN(minLat) ? -90.0 : minLat;
        maxLon = Double.isNaN(maxLon) ? 180.0 : maxLon;
        maxLat = Double.isNaN(maxLat) ? 90.0 : maxLat;
        double[] bbox = { minLon, minLat, maxLon, maxLat };
        return new BoundingBoxImpl(bbox, inBbox.getCoordinateReferenceSystem());
    }

    private static BoundingBox getBoundingBoxForSinglePosition(HorizontalPosition pos) {
        return new BoundingBoxImpl(new double[] { pos.getX() - 1.0, pos.getY() - 1.0,
                pos.getX() + 1.0, pos.getY() + 1.0 }, pos.getCoordinateReferenceSystem());
    }

    public static Extent<Double> getElevationRangeForString(String elevationString) {
        if(elevationString == null || elevationString.equals("")){
            return null;
        }
        String[] parts = elevationString.split("/");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Cannot determine depths from string: "
                    + elevationString);
        } else if (parts.length == 1) {
            return Extents.newExtent(Double.parseDouble(parts[0]), Double.parseDouble(parts[0]));
        } else {
            double firstVal = Double.parseDouble(parts[0]);
            double secondVal = Double.parseDouble(parts[1]);
            if(firstVal < secondVal)
                return Extents.newExtent(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            else
                return Extents.newExtent(Double.parseDouble(parts[1]), Double.parseDouble(parts[0]));
        }
    }

    public static List<TimePosition> getTimePositionsForString(String timeString, Feature feature)
            throws InvalidDimensionValueException {
        List<TimePosition> tValues = new ArrayList<TimePosition>();
        if (feature == null) {
            return tValues;
        }
        List<TimePosition> tAxis = GISUtils.getTimeAxis(feature, false);
        if (tAxis == null || tAxis.size() == 0) {
            return tValues;
        }
        if(timeString == null || timeString.equals("")) {
            return tAxis.subList(0, 1);
        }
        for (String t : timeString.split(",")) {
            String[] startStop = t.split("/");
            if (startStop.length == 1) {
                // This is a single time value
                TimePosition time = findTValue(startStop[0], tAxis);
                tValues.add(time);
            } else if (startStop.length == 2) {
                // Use all time values from start to stop inclusive
                tValues.addAll(findTValues(startStop[0], startStop[1], tAxis));
            } else {
                throw new InvalidDimensionValueException("time", t);
            }
        }
        return tValues;
    }

    private static TimePosition findTValue(String isoDateTime, List<TimePosition> tValues)
            throws InvalidDimensionValueException {
        if (tValues == null) {
            return null;
        }
        int tIndex = findTIndex(isoDateTime, tValues);
        if(tIndex < 0) {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        return tValues.get(tIndex);
    }
    
    private static List<TimePosition> findTValues(String isoDateTimeStart, String isoDateTimeEnd,
            List<TimePosition> tValues) throws InvalidDimensionValueException {
        if (tValues == null) {
            throw new InvalidDimensionValueException("time", isoDateTimeStart + "/"
                    + isoDateTimeEnd);
        }
        int startIndex = findTIndex(isoDateTimeStart, tValues);
        int endIndex = findTIndex(isoDateTimeEnd, tValues);
        if (startIndex > endIndex) {
            throw new InvalidDimensionValueException("time", isoDateTimeStart + "/"
                    + isoDateTimeEnd);
        }
        List<TimePosition> returnTValues = new ArrayList<TimePosition>();
        for (int i = startIndex; i <= endIndex; i++) {
            returnTValues.add(tValues.get(i));
        }
        return returnTValues;
    }
    
    public static int findTIndex(String isoDateTime, List<TimePosition> tValues)
            throws InvalidDimensionValueException {
        TimePosition target;
        if (isoDateTime.equalsIgnoreCase("current")) {
            target = GISUtils.getClosestToCurrentTime(tValues);
        } else {
            if(tValues == null || tValues.size() == 0) {
                return -1;
            }
            try {
                /*
                 * ISO date strings do not have spaces. However, spaces can be
                 * generated by decoding + symbols from URLs. If the date string
                 * has a space in it, something's going wrong anyway. Chances
                 * are it's this.
                 */
                isoDateTime = isoDateTime.replaceAll(" ", "+");
                target = TimeUtils.iso8601ToDateTime(isoDateTime, tValues.get(0).getCalendarSystem());
            } catch (ParseException e) {
                throw new InvalidDimensionValueException("time", isoDateTime);
            }
        }

        long targetMillis = target.getValue();
        for(int i = 0; i < tValues.size(); i++) {
            if(targetMillis == tValues.get(i).getValue()) {
                return i;
            }
        }
        return -1;
    }

}
