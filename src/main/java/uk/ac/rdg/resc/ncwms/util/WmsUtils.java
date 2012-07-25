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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.Coverage;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.TimeAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.VerticalAxisImpl;
import uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.VectorComponent;
import uk.ac.rdg.resc.edal.coverage.metadata.VectorComponent.VectorDirection;
import uk.ac.rdg.resc.edal.coverage.metadata.VectorMetadata;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.Vector2D;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.position.impl.VerticalPositionImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

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
    /**
     * The maximum number of layers that can be requested in a single GetMap
     * operation
     */
    public static final int LAYER_LIMIT = 1;

    static
    {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() { throw new AssertionError(); }

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
    public static Extent<Float> estimateValueRange(Feature feature, String member) throws IOException
    {
        List<Float> dataSample = readDataSample(feature, member);
        return Extents.findMinMax(dataSample);
    }

    private static List<Float> readDataSample(Feature feature, String member) throws IOException {
        /*
         * TODO FIX.  This is easy.  Just check the type of feature and act appropriately
         */
        Class<?> clazz = feature.getCoverage().getScalarMetadata(member).getValueType();
        if(!Number.class.isAssignableFrom(clazz)){
            throw new IllegalArgumentException("Cannot read a data sample from a non-numerical field");
        }
        /*
         * Read a low-resolution grid of data covering the entire spatial extent
         */
        List<?> values = null;
        List<Float> ret = new ArrayList<Float>();
        if(feature instanceof GridFeature){
            GridFeature gridFeature = (GridFeature) feature;
            GridFeature loResFeature = gridFeature.extractGridFeature(new RegularGridImpl(gridFeature.getCoverage()
                    .getDomain().getCoordinateExtent(), 100, 100), CollectionUtils.setOf(member));
            values = loResFeature.getCoverage().getValues(member);
        } else if (feature instanceof GridSeriesFeature){
            GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;
            GridFeature loResFeature = gridSeriesFeature.extractGridFeature(new RegularGridImpl(
                    gridSeriesFeature.getCoverage().getDomain().getHorizontalGrid()
                            .getCoordinateExtent(), 100, 100),
                    getUppermostElevation(gridSeriesFeature),
                    getClosestToCurrentTime(gridSeriesFeature.getCoverage().getDomain()
                            .getTimeAxis()), CollectionUtils.setOf(member));
            values = loResFeature.getCoverage().getValues(member);
        } else if (feature instanceof PointSeriesFeature){
            values = ((PointSeriesFeature) feature).getCoverage().getValues(member);
        } else if (feature instanceof ProfileFeature){
            ProfileFeature profileFeature = (ProfileFeature) feature;
            values = profileFeature.getCoverage().getValues(member);
        }
        for(Object r : values){
            Number num = (Number) r;
            if(num == null || num.equals(Float.NaN) || num.equals(Double.NaN)){
                ret.add(null);
            } else {
                ret.add(num.floatValue());
            }
        }
        return ret;
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
    
    public static TimePosition getClosestToCurrentTime(TimeAxis tAxis){
        if (tAxis == null) return null; // no time axis
        int index = TimeUtils.findTimeIndex(tAxis.getCoordinateValues(), new TimePositionJoda());
        if (index < 0) {
            // We can calculate the insertion point
            int insertionPoint = -(index + 1); 
            // We set the index to the most recent past time
            if (insertionPoint > 0) index = insertionPoint - 1; // The most recent past time
            else index = 0; // All DateTimes on the axis are in the future, so we take the earliest
        }
        
        return tAxis.getCoordinateValue(index);
    }

    public static VerticalPosition getUppermostElevation(GridSeriesFeature feature){
        VerticalAxis vAxis = feature.getCoverage().getDomain().getVerticalAxis();
        // We must access the elevation values via the accessor method in case
        // subclasses override it.
        if (vAxis == null) {
            return new VerticalPositionImpl(Double.NaN, null);
        }

        double value;
        if (vAxis.getVerticalCrs().isPressure()) {
            // The vertical axis is pressure. The default (closest to the
            // surface)
            // is therefore the maximum value.
            value = Collections.max(vAxis.getCoordinateValues());
        } else {
            // The vertical axis represents linear height, so we find which
            // value is closest to zero (the surface), i.e. the smallest
            // absolute value
            value = Collections.min(vAxis.getCoordinateValues(), new Comparator<Double>() {
                @Override public int compare(Double d1, Double d2) {
                    return Double.compare(Math.abs(d1), Math.abs(d2));
                }
            });
        }
        return new VerticalPositionImpl(value, vAxis.getVerticalCrs());
    }
    
    public static Dataset getDataset(Config serverConfig, String layerName){
        int slashIndex = layerName.indexOf("/");
        String datasetId = layerName.substring(0, slashIndex);
        return serverConfig.getDatasetById(datasetId);
    }
    
    public static FeaturePlottingMetadata getMetadata(Config serverConfig, String layerName) throws WmsException {
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 3) {
            throw new WmsException("Layers should be of the form Dataset/Grid/Variable");
        }
        
        String datasetId = layerParts[0];
        String featureId = layerParts[1];
        String memberId = layerParts[2];
        
        Dataset dataset = serverConfig.getDatasetById(datasetId);
        
        String featureAndVarId = featureId+"/"+memberId;
        return dataset.getPlottingMetadataMap().get(featureAndVarId);
    }
    
    /**
     * This method returns a member of the feature which is plottable.
     * 
     * If the memberName is a scalar member of the feature, it is returned.
     * Otherwise, the metadata tree is searched to find the parent metadata with
     * the name {@code memberName}. A scalar child member of this metadata is
     * then returned
     * 
     * For example, if memberName is an instance of {@link VectorMetadata}, then
     * the name of the {@link VectorComponent} corresponding to the magnitude
     * will be returned.
     * 
     * This method is the one to modify if new metadata types are added
     * 
     * @param feature
     *            the feature containing the plottable member
     * @param memberName
     *            the name of the desired member
     */
    public static String getPlottableMemberName(Feature feature, String memberName){
        if(feature.getCoverage().getScalarMemberNames().contains(memberName)){
            return memberName;
        } else {
            RangeMetadata descendentMetadata = getDescendentMetadata(feature.getCoverage()
                    .getRangeMetadata(), memberName);
            if(descendentMetadata == null){
                return null;
            } else {
                if(descendentMetadata instanceof VectorMetadata){
                    VectorMetadata vectorMetadata = (VectorMetadata) descendentMetadata;
                    
                    Set<String> vectorComponentNames = vectorMetadata.getMemberNames();
                    for(String vectorComponentName : vectorComponentNames){
                        VectorComponent vectorComponent = vectorMetadata.getMemberMetadata(vectorComponentName);
                        if(vectorComponent.getDirection() == VectorDirection.MAGNITUDE){
                            return vectorComponentName;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static RangeMetadata getDescendentMetadata(RangeMetadata topMetadata, String memberName){
        if(topMetadata.getMemberNames().contains(memberName)){
            return topMetadata.getMemberMetadata(memberName);
        } else {
            for(String childMember : topMetadata.getMemberNames()){
                RangeMetadata memberMetadata = topMetadata.getMemberMetadata(childMember);
                if(!(memberMetadata instanceof ScalarMetadata)){
                    return getDescendentMetadata(memberMetadata, memberName);
                }
            }
        }
        return null;
    }
    
    /**
     * This method returns a layer name which is plottable.
     * 
     * It uses getPlottableMemberName, but checks the layer name format
     * 
     * @param feature
     *            the feature containing the plottable member
     * @param layerName
     *            the name of the desired layer
     * @throws WmsException 
     */
    public static String getPlottableLayerName(Feature feature, String layerName) throws WmsException{
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 3) {
            throw new WmsException("Layers should be of the form Dataset/Grid/Variable");
        }
        return layerParts[0]+"/"+layerParts[1]+"/"+getPlottableMemberName(feature, layerParts[2]);
    }
    
    public static boolean isVectorLayer(Coverage<?> coverage, String memberName){
        if(coverage.getScalarMetadata(memberName).getValueType() == Vector2D.class){
            return true;
        }
        return false;
    }
    
    public static BoundingBox getWmsBoundingBox(Feature feature){
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
        if ((minLon < 180 && maxLon > 180) || (minLon < -180 && maxLon > -180) || minLon >= maxLon)
        {
            minLon = -180.0;
            maxLon = 180.0;
        }
        if (minLat >= maxLat)
        {
            minLat = -90.0;
            maxLat = 90.0;
        }
        // Sometimes the bounding boxes can be NaN, e.g. for a VerticalPerspectiveView
        // that encompasses more than the Earth's disc
        minLon = Double.isNaN(minLon) ? -180.0 : minLon;
        minLat = Double.isNaN(minLat) ?  -90.0 : minLat;
        maxLon = Double.isNaN(maxLon) ?  180.0 : maxLon;
        maxLat = Double.isNaN(maxLat) ?   90.0 : maxLat;
        double[] bbox = {minLon, minLat, maxLon, maxLat};
        return new BoundingBoxImpl(bbox, inBbox.getCoordinateReferenceSystem());
    }
    
    private static BoundingBox getBoundingBoxForSinglePosition(HorizontalPosition pos){
        return new BoundingBoxImpl(new double[] { pos.getX()-1.0, pos.getY()-1.0, pos.getX()+1.0,
                pos.getY()+1.0 }, pos.getCoordinateReferenceSystem());
    }

    /**
     * Utility method for getting the layer name (unique within a Capabilities
     * document) from the given GetMapRequest, checking that there is only one
     * layer in the request
     */
    public static String getLayerName(GetMapDataRequest getMapDataRequest) throws WmsException {
        // Find which layer the user is requesting
        String[] layers = getMapDataRequest.getLayers();
        if (layers.length == 0) {
            throw new WmsException("Must provide a value for the LAYERS parameter");
        }
        // TODO: support more than one layer (superimposition, difference, mask)
        if (layers.length > LAYER_LIMIT) {
            throw new WmsException("You may only create a map from " + LAYER_LIMIT + " layer(s) at a time");
        }
        return layers[0];
    }
    
    /**
     * Utility method for getting the dataset ID from the given layer name
     */
    public static String getDatasetId(String layerName) throws WmsException {
        // Find which layer the user is requesting
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 3) {
            throw new WmsException("Layers should be of the form Dataset/Grid/Variable");
        }
        return layerParts[0];
    }

    /**
     * Utility method for getting the member name from the given layer name
     */
    public static String getMemberName(String layerName) throws WmsException {
        // Find which layer the user is requesting
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 3) {
            throw new WmsException("Layers should be of the form Dataset/Grid/Variable");
        }
        return layerParts[2];
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

    /**
     * Utility to get the vertical axis of a feature, if it exists
     * @param feature the feature to check
     * @return the {@link VerticalAxis}, or <code>null</code> if none exists
     */
    public static VerticalAxis getVerticalAxis(Feature feature) {
        if (feature instanceof GridSeriesFeature) {
            return ((GridSeriesFeature) feature).getCoverage().getDomain().getVerticalAxis();
        } else if (feature instanceof ProfileFeature) {
            ProfileFeature profileFeature = (ProfileFeature) feature;
            return new VerticalAxisImpl("z", profileFeature.getCoverage().getDomain().getZValues(),
                    profileFeature.getCoverage().getDomain().getVerticalCrs());

        } else {
            return null;
        }
    }

    /**
     * Utility to get the time axis of a feature, if it exists
     * @param feature the feature to check
     * @return the {@link TimeAxis}, or <code>null</code> if none exists
     */
    public static TimeAxis getTimeAxis(Feature feature) {
        if (feature instanceof GridSeriesFeature) {
            return ((GridSeriesFeature) feature).getCoverage().getDomain().getTimeAxis();
        } else if (feature instanceof PointSeriesFeature) {
            return new TimeAxisImpl("time", ((PointSeriesFeature) feature).getCoverage().getDomain().getTimes());
        } else {
            return null;
        }
    }
}
