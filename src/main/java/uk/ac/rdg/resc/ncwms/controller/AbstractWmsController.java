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

import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.ParseException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.ProfileCoverage;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.grid.GridCell2D;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates2D;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridCoordinates2DImpl;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.edal.exceptions.InvalidLineStringException;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.edal.graphics.Charting;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.graphics.MapPlotter;
import uk.ac.rdg.resc.edal.graphics.MapStyleDescriptor;
import uk.ac.rdg.resc.edal.graphics.PlotStyle;
import uk.ac.rdg.resc.edal.graphics.formats.ImageFormat;
import uk.ac.rdg.resc.edal.graphics.formats.KmzFormat;
import uk.ac.rdg.resc.edal.position.CalendarSystem;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.LonLatPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.Vector2D;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.position.impl.VerticalPositionImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.CurrentUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.FeatureNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.Wms1_1_1Exception;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * <p>
 * This Controller is the entry point for all standard WMS operations (GetMap,
 * GetCapabilities, GetFeatureInfo). Only one WmsController object is created.
 * Spring manages the creation of this object and the injection of the objects
 * that it needs (i.e. its dependencies), such as the {@linkplain ServerConfig
 * configuration object}. The Spring configuration file
 * <tt>web/WEB-INF/WMS-servlet.xml</tt> defines all this information and also
 * defines that this Controller will handle all requests to the URI pattern
 * <tt>/wms</tt>. (See the SimpleUrlHandlerMapping in
 * <tt>web/WEB-INF/WMS-servlet.xml</tt>).
 * </p>
 * 
 * <p>
 * See the {@link #handleRequestInternal handleRequestInternal()} method for
 * more information.
 * </p>
 * 
 * <p>
 * <i>(Note that we cannot use a CommandController here because there is no
 * (apparent) way in Spring to use case-insensitive parameter names to bind
 * request parameters to an object.)</i>
 * </p>
 * 
 * @author Jon Blower
 */
public abstract class AbstractWmsController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(AbstractWmsController.class);
    private static final String FEATURE_INFO_XML_FORMAT = "text/xml";
    private static final String FEATURE_INFO_GML_FORMAT = "application/vnd.ogc.gml";
    private static final String FEATURE_INFO_PNG_FORMAT = "image/png";

    // These objects will be injected by Spring
    protected ServerConfig serverConfig;

    /**
     * Called automatically by Spring after all the dependencies have been
     * injected.
     */
    public void init() throws Exception {
        /*
         * We initialize the ColorPalettes. We need to do this from here because
         * we need a way to find out the real path of the directory containing
         * the palettes. Therefore we need a way of getting at the
         * ServletContext object, which isn't available from the ColorPalette
         * class.
         */
        File paletteLocationDir = this.serverConfig.getPaletteFilesLocation(this.getServletContext());
        if (paletteLocationDir != null && paletteLocationDir.exists() && paletteLocationDir.isDirectory()) {
            ColorPalette.loadPalettes(paletteLocationDir);
        } else {
            log.info("Directory of palette files does not exist or is not a directory");
        }
    }

    /**
     * <p>
     * Entry point for all requests to the WMS. This method first creates a
     * <tt>RequestParams</tt> object from the URL query string. This object
     * provides methods for retrieving parameter values, based on the fact that
     * WMS parameter <i>names</i> are case-insensitive.
     * </p>
     * 
     * <p>
     * Based on the value of the REQUEST parameter this method then delegates to
     * {@link #getCapabilities getCapabilities()}, {@link #getMap getMap()} or
     * {@link #getFeatureInfo getFeatureInfo()}. If the information returned
     * from this method is to be presented as an XML/JSON/HTML document, the
     * method returns a ModelAndView object containing the name of a JSP page
     * and the data that the JSP needs to render. If the information is to be
     * presented as an image, the method writes the image to the servlet's
     * output stream, then returns null.
     * </p>
     * 
     * <p>
     * Any Exceptions that are thrown by this method or its delegates are
     * automatically handled by Spring and converted to XML to be presented to
     * the user. See the <a href="../exceptions/package-summary.html">Exceptions
     * package</a> for more details.
     * </p>
     */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws Exception {

        // Create an object that allows request parameters to be retrieved in
        // a way that is not sensitive to the case of the parameter NAMES
        // (but is sensitive to the case of the parameter VALUES).
        RequestParams params = new RequestParams(httpServletRequest.getParameterMap());

        try {
            // Check the REQUEST parameter to see if we're producing a
            // capabilities
            // document, a map or a FeatureInfo
            String request = params.getMandatoryString("request");
            return this.dispatchWmsRequest(request, params, httpServletRequest, httpServletResponse);
        } catch (WmsException wmse) {
            wmse.printStackTrace();
            // We don't log these errors
            String wmsVersion = params.getWmsVersion();
            if (wmsVersion != null && wmsVersion.equals("1.1.1")) {
                // We create a new exception type to ensure that the correct
                // JSP is used to render it. This class also translates any
                // exception codes that are different in 1.1.1 (i.e.
                // InvalidCRS/SRS)
                throw new Wms1_1_1Exception(wmse);
            }
            throw wmse;
        } catch (SocketException se) {
            // SocketExceptions usually happen when the client has aborted the
            // connection, so there's nothing we can do here
            return null;
        } catch (IOException ioe) {
            // Filter out Tomcat ClientAbortExceptions, which for some reason
            // don't inherit from SocketException.
            // We check the class name to avoid a compile-time dependency on the
            // Tomcat libraries
            if (ioe.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
                return null;
            }
            // Other types of IOException are potentially interesting and
            // must be rethrown to avoid hiding errors (maybe they
            // represent internal errors when reading data for instance).
            throw ioe;
        } catch (Exception e) {
            e.printStackTrace();
            // An unexpected (internal) error has occurred
            throw e;
        }
    }

    /**
     * Object that returns a Feature given a layer Name, which is
     * unique within a Capabilities document
     */
    public static interface FeatureFactory {
        /**
         * Returns a Feature given a layer name, which is unique within a
         * Capabilities document
         */
        public Feature getFeature(String layerName) throws FeatureNotDefinedException;
    }

    /**
     * Dispatches the request to the relevant methods (e.g. getCapabilities(),
     * getMap()). Subclasses can override this to perform any pre- or post-
     * processing before/after calling these handlers.
     * 
     * @param request
     *            The value of the REQUEST parameter from the client, e.g.
     *            "GetCapabilities". Will never be null.
     * @todo Use an enum instead of the request string
     */
    protected abstract ModelAndView dispatchWmsRequest(String request, RequestParams params,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception;

    /**
     * Executes the GetCapabilities operation, returning a ModelAndView for
     * display of the information as an XML document. If the user has requested
     * VERSION=1.1.1 the information will be rendered using
     * <tt>web/WEB-INF/jsp/capabilities_xml_1_1_1.jsp</tt>. If the user
     * specifies VERSION=1.3.0 (or does not specify a version) the information
     * will be rendered using <tt>web/WEB-INF/jsp/capabilities_xml.jsp</tt>.
     * 
     * @param datasets
     *            The collection of datasets to include in the Capabilities
     *            document. Must not be null
     * @param lastUpdateTime
     *            The last update time of this Capabilities document, or null if
     *            unknown
     * @throws IOException
     *             if there was an i/o error getting the dataset(s) from the
     *             underlying data store
     */
    protected ModelAndView getCapabilities(Collection<? extends Dataset> datasets, TimePosition lastUpdateTime,
            RequestParams params, HttpServletRequest httpServletRequest)
            throws WmsException, IOException {
        // Check the SERVICE parameter
        String service = params.getMandatoryString("service");
        if (!service.equals("WMS")) {
            throw new WmsException("The value of the SERVICE parameter must be \"WMS\"");
        }

        // Check the VERSION parameter (not compulsory for GetCapabilities)
        String versionStr = params.getWmsVersion();

        // Do UPDATESEQUENCE negotiation according to WMS 1.3.0 spec (sec
        // 7.2.3.5)
        String updateSeqStr = params.getString("updatesequence");
        if (updateSeqStr != null) {
            TimePosition updateSequence;
            try {
                updateSequence = TimeUtils.iso8601ToDateTime(updateSeqStr, CalendarSystem.CAL_ISO_8601);
            } catch (ParseException iae) {
                throw new InvalidUpdateSequence(updateSeqStr + " is not a valid ISO date-time");
            }
            if (updateSequence.getValue() == lastUpdateTime.getValue()) {
                throw new CurrentUpdateSequence(updateSeqStr);
            } else if (updateSequence.getValue() > lastUpdateTime.getValue()) {
                throw new InvalidUpdateSequence(updateSeqStr + " is later than the current server updatesequence value");
            }
        }

        // Find out whether we are going to represent times in a verbose or
        // concise way in the Capabilities document.
        boolean verboseTimes = params.getBoolean("verbose", false);

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("config", this.serverConfig);
        models.put("datasets", datasets);
        // We use the current time if the last update time is unknown
        models.put("lastUpdate", lastUpdateTime == null ? new TimePositionJoda() : lastUpdateTime);
        models.put("wmsBaseUrl", httpServletRequest.getRequestURL().toString());
        // Show only a subset of the CRS codes that we are likely to use.
        // Otherwise Capabilities doc gets very large indeed.
        // TODO: make configurable in admin app
        String[] supportedCrsCodes = new String[] { "EPSG:4326", "CRS:84", // Plate Carree
                "EPSG:41001", // Mercator
                "EPSG:27700", // British National Grid
                // See http://nsidc.org/data/atlas/ogc_services.html for useful
                // stuff about polar stereographic projections
                "EPSG:3408", // NSIDC EASE-Grid North
                "EPSG:3409", // NSIDC EASE-Grid South
                "EPSG:3857", // Google Maps
                "EPSG:32661", // North Polar stereographic
                "EPSG:32761" // South Polar stereographic
        };
        models.put("supportedCrsCodes", supportedCrsCodes); // */HorizontalGrid.SUPPORTED_CRS_CODES);
        models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
        models.put("layerLimit", WmsUtils.LAYER_LIMIT);
        models.put("featureInfoFormats", new String[] { FEATURE_INFO_PNG_FORMAT, FEATURE_INFO_XML_FORMAT });
        models.put("legendWidth", ColorPalette.LEGEND_WIDTH);
        models.put("legendHeight", ColorPalette.LEGEND_HEIGHT);
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        models.put("verboseTimes", verboseTimes);
        // Do WMS version negotiation. From the WMS 1.3.0 spec:
        // * If a version unknown to the server and higher than the lowest
        // supported version is requested, the server shall send the highest
        // version it supports that is less than the requested version.
        // * If a version lower than any of those known to the server is
        // requested,
        // then the server shall send the lowest version it supports.
        // We take the version to be 1.3.0 if not specified
        WmsVersion wmsVersion = versionStr == null ? WmsVersion.VERSION_1_3_0 : new WmsVersion(versionStr);
        if (wmsVersion.compareTo(WmsVersion.VERSION_1_3_0) >= 0) {
            // version is >= 1.3.0. Send 1.3.0 Capabilities
            return new ModelAndView("capabilities_xml", models);
        } else {
            // version is < 1.3.0. Send 1.1.1 Capabilities
            return new ModelAndView("capabilities_xml_1_1_1", models);
        }
    }

    /**
     * Executes the GetMap operation. This methods performs the following steps:
     * <ol>
     * <li>Creates a {@link GetMapRequest} object from the given
     * {@link RequestParams}. This parses the parameters and checks their
     * validity.</li>
     * <li>Finds the relevant {@link Layer} object from the config system.</li>
     * <li>Creates a {@link HorizontalGrid} object that represents the grid on
     * which the final image will sit (based on the requested CRS and image
     * width/height).</li>
     * <li>Looks for TIME and ELEVATION parameters (TIME may be expressed as a
     * start/end range, in which case we will produce an animation).</li>
     * <li>Extracts the data, returning an array of floats, representing the
     * data values at each pixel in the final image.</li>
     * <li>Uses an {@link ImageProducer} object to turn the array of data into a
     * {@link java.awt.image.BufferedImage} (or, in the case of an animation,
     * several {@link java.awt.image.BufferedImage}s).</li>
     * <li>Uses a {@link ImageFormat} object to write the image to the servlet's
     * output stream in the requested format.</li>
     * </ol>
     * 
     * @throws WmsException
     *             if the user has provided invalid parameters
     * @throws Exception
     *             if an internal error occurs
     * @see uk.ac.rdg.resc.ncwms.datareader.DataReader#read DataReader.read()
     * @see uk.ac.rdg.resc.ncwms.datareader.DefaultDataReader#read
     *      DefaultDataReader.read()
     * @todo Separate Model and View code more cleanly
     */
    protected ModelAndView getMap(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse httpServletResponse) throws WmsException, Exception {
        // Parse the URL parameters
        GetMapRequest getMapRequest = new GetMapRequest(params);

        GetMapStyleRequest styleRequest = getMapRequest.getStyleRequest();

        // Get the ImageFormat object corresponding with the requested MIME type
        String mimeType = styleRequest.getImageFormat();
        // This throws an InvalidFormatException if the MIME type is not
        // supported
        ImageFormat imageFormat = ImageFormat.get(mimeType);

        GetMapDataRequest dr = getMapRequest.getDataRequest();

        // Check the dimensions of the image
        if (dr.getHeight() > this.serverConfig.getMaxImageHeight()
                || dr.getWidth() > this.serverConfig.getMaxImageWidth()) {
            throw new WmsException("Requested image size exceeds the maximum of "
                    + this.serverConfig.getMaxImageWidth() + "x" + this.serverConfig.getMaxImageHeight());
        }

        String layerName = WmsUtils.getWmsLayerName(dr);
        String memberName = WmsUtils.getMemberName(layerName);
        
        Feature feature = featureFactory.getFeature(layerName);

        // Create an object that will turn data into BufferedImages
        Extent<Float> scaleRange = styleRequest.getColorScaleRange();

        FeaturePlottingMetadata metadata = WmsUtils.getMetadata((Config) serverConfig, layerName);

        if (scaleRange == null)
            scaleRange = metadata.getColorScaleRange();
        Boolean logScale = styleRequest.isScaleLogarithmic();
        if (logScale == null)
            logScale = metadata.isLogScaling();
        /*
         * DEFAULT style is BOXFILL for scalar quantities, and VECTOR for vector quantities
         */
        MapStyleDescriptor styleDescriptor = new MapStyleDescriptor();

        styleDescriptor.setColorPalette(metadata.getPaletteName());
        String[] styles = styleRequest.getStyles();
        
        /*
         * We start with a default plot style
         */
        PlotStyle plotStyle = PlotStyle.DEFAULT;
        
        if (styles.length > 0) {
            String[] styleStrEls = styles[0].split("/");
            
            /*
             * We choose the plot style based on the request
             */
            String styleType = styleStrEls[0];
            try{
                plotStyle = PlotStyle.valueOf(styleType.toUpperCase());
            } catch (IllegalArgumentException iae){
                /*
                 * Ignore this, and just use default
                 */
            }

            /*
             * And set the palette
             */
            String paletteName = null;
            if(plotStyle.usesPalette()){
                if (styleStrEls.length > 1)
                    paletteName = styleStrEls[1];
                styleDescriptor.setColorPalette(paletteName);
            }
        }
        
        styleDescriptor.setScaleRange(scaleRange);
        styleDescriptor.setTransparent(styleRequest.isTransparent());
        styleDescriptor.setLogarithmic(logScale);
        styleDescriptor.setOpacity(styleRequest.getOpacity());
        styleDescriptor.setBgColor(styleRequest.getBackgroundColour());
        styleDescriptor.setNumColourBands(styleRequest.getNumColourBands());
        
        // Need to make sure that the images will be compatible with the
        // requested image format
        if (styleRequest.isTransparent() && !imageFormat.supportsFullyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType + " does not support fully-transparent pixels");
        }
        if (styleRequest.getOpacity() < 100 && !imageFormat.supportsPartiallyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType + " does not support partially-transparent pixels");
        }
        BoundingBox bbox = new BoundingBoxImpl(dr.getBbox(), WmsUtils.getCrs(dr.getCrsCode()));
        MapPlotter mapPlotter = new MapPlotter(styleDescriptor, dr.getWidth(), dr.getHeight(), bbox);
        
        VerticalPosition zValue = getElevationValue(dr.getElevationString(), feature);

        // Cycle through all the provided timesteps, extracting data for each
        // step
        List<String> tValueStrings = new ArrayList<String>();
        List<TimePosition> timeValues = getTimeValues(dr.getTimeString(), feature);
        if (timeValues.size() > 1 && !imageFormat.supportsMultipleFrames()) {
            throw new WmsException("The image format " + mimeType + " does not support multiple frames");
        }
        // Use a single null time value if the layer has no time axis
        if (timeValues.isEmpty())
            timeValues = Arrays.asList((TimePosition) null);
        for (TimePosition timeValue : timeValues) {
            // Only add a label if this is part of an animation
            String tValueStr = null;
            if (timeValues.size() > 1 && timeValue != null) {
                tValueStr = TimeUtils.dateTimeToISO8601(timeValue);
            }
            tValueStrings.add(tValueStr);
            
            mapPlotter.addToFrame(feature, memberName, zValue, timeValue, tValueStr, plotStyle);
        }
        
        // We only create a legend object if the image format requires it
        BufferedImage legend = imageFormat.requiresLegend() ? styleDescriptor.getLegend(
                feature.getName(),
                MetadataUtils.getUnitsString(feature, memberName)) : null;

        // Write the image to the client.
        // First we set the HTTP headers
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setContentType(mimeType);
        // If this is a KMZ file give it a sensible filename
        if (imageFormat instanceof KmzFormat) {
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + feature.getFeatureCollection().getId() + "_"
                    + feature.getId() + ".kmz");
        }
        // Render the images and write to the output stream
        imageFormat.writeImage(mapPlotter.getRenderedFrames(), httpServletResponse.getOutputStream(), feature, dr.getBbox(),
                tValueStrings, dr.getElevationString(), legend);

        return null;
    }
    /**
     * Executes the GetFeatureInfo operation
     * 
     * @throws WmsException
     *             if the user has provided invalid parameters
     * @throws Exception
     *             if an internal error occurs
     * @todo Separate Model and View code more cleanly
     */
    protected ModelAndView getFeatureInfo(RequestParams params, FeatureFactory featureFactory,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws WmsException, Exception {
        
        GetFeatureInfoRequest request = new GetFeatureInfoRequest(params);

        GetFeatureInfoDataRequest dr = request.getDataRequest();

        String outputFormat = request.getOutputFormat();
        // Check the output format
        if (!outputFormat.equals(FEATURE_INFO_XML_FORMAT)
                && !outputFormat.equals(FEATURE_INFO_GML_FORMAT)) {
            throw new InvalidFormatException("The output format " + request.getOutputFormat()
                    + " is not valid for GetFeatureInfo");
        }

        String layerName = WmsUtils.getWmsLayerName(dr);
        String memberName = WmsUtils.getMemberName(layerName);
        
        
        Feature feature = featureFactory.getFeature(layerName);
        memberName = MetadataUtils.getScalarMemberName(feature, memberName);
        
        // Get the grid onto which the data is being projected
        RegularGrid grid = WmsUtils.getImageGrid(dr);
        // Get the real-world coordinate values of the point of interest
        // Remember that the vertical axis is flipped
        int j = dr.getHeight() - dr.getPixelRow() - 1;
        HorizontalPosition pos = grid.transformCoordinates(new GridCoordinates2DImpl(dr.getPixelColumn(), j));

        // Transform these coordinates into lon-lat
        LonLatPosition lonLat = GISUtils.transformToWgs84LonLat(pos);
        
        // Get the elevation value requested
        VerticalPosition zValue = getElevationValue(dr.getElevationString(), feature);
        
        String timeString = dr.getTimeString();
        
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("longitude", lonLat.getLongitude());
        models.put("latitude", lonLat.getLatitude());
        
        /*
         * Find out the i,j coordinates of this point in the source grid (could
         * be null)
         */
        HorizontalGrid horizGrid = null;
        if (feature instanceof GridSeriesFeature) {
            horizGrid = ((GridSeriesFeature) feature).getCoverage().getDomain()
                    .getHorizontalGrid();
        } else if (feature instanceof GridFeature) {
            GridFeature gridFeature = (GridFeature) feature;
            horizGrid = gridFeature.getCoverage().getDomain();

        }

        LonLatPosition gridCellCentre = null;
        GridCoordinates2D gridCoordinates = null;
        if(horizGrid != null){
            GridCell2D gridCell = horizGrid.findContainingCell(pos);
            if(gridCell != null){
                gridCoordinates = gridCell.getGridCoordinates();
                gridCellCentre = GISUtils.transformToWgs84LonLat(gridCell.getCentre());
            }
        }
        
        models.put("gridCentre", gridCellCentre);
        models.put("gridCoords", gridCoordinates);
        models.put("varName", memberName);
        models.put("coords", pos);
        models.put("crs", dr.getCrsCode());

        /*
         * Get the requested timesteps. If the layer doesn't have a time
         * axis then this will return a single-element List with value null.
         */
        List<TimePosition> tValues = getTimeValues(timeString, feature);

        // Now we map date-times to data values
        // The map is kept in order of ascending time
        Map<TimePosition, Object> featureData = new LinkedHashMap<TimePosition, Object>();
        if(tValues.isEmpty()){
            Object val = WmsUtils.getFeatureValue(feature, pos, zValue, null, memberName);
            featureData.put(null, val);
        } else {
            for(TimePosition time : tValues){
                Object val = WmsUtils.getFeatureValue(feature, pos, zValue, time, memberName);
                featureData.put(time, val);
            }
        }

        models.put("data", featureData);
        if(FEATURE_INFO_XML_FORMAT.equals(outputFormat)){
            return new ModelAndView("showFeatureInfo_xml", models);
        } else {
            return new ModelAndView("showFeatureInfo_gml", models);
        }
    }
    
    /**
     * Creates and returns a PNG image with the colour scale and range for a
     * given Layer
     */
    protected ModelAndView getLegendGraphic(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse httpServletResponse) throws Exception {
        BufferedImage legend;

        // numColourBands defaults to ColorPalette.MAX_NUM_COLOURS if not set
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);

        String paletteName = params.getString("palette");

        // Find out if we just want the colour bar with no supporting text
        String colorBarOnly = params.getString("colorbaronly", "false");
        boolean vertical = params.getBoolean("vertical", true);
        if (colorBarOnly.equalsIgnoreCase("true")) {
            // We're only creating the colour bar so we need to know a width
            // and height
            int width = params.getPositiveInt("width", 50);
            int height = params.getPositiveInt("height", 200);
            // Find the requested colour palette, or use the default if not set
            ColorPalette palette = ColorPalette.get(paletteName);
            legend = palette.createColorBar(width, height, numColourBands, vertical);
        } else {
            // We're creating a legend with supporting text so we need to know
            // the colour scale range and the layer in question
            GetFeatureInfoRequest request = new GetFeatureInfoRequest(params);
            GetFeatureInfoDataRequest dr = request.getDataRequest();

            String layerName = WmsUtils.getWmsLayerName(dr);
            Feature feature = featureFactory.getFeature(layerName);
            String memberName = WmsUtils.getMemberName(layerName);

            FeaturePlottingMetadata metadata = WmsUtils.getMetadata((Config) serverConfig, layerName);

            // Layer layer = layerFactory.getLayer(layerName);

            // We default to the layer's default palette if none is specified
            ColorPalette palette = paletteName == null ? ColorPalette.get(metadata.getPaletteName()) : ColorPalette
                    .get(paletteName);

            // See if the client has specified a logarithmic scaling, defaulting
            // to the layer's default
            Boolean isLogScale = GetMapStyleRequest.isLogScale(params);
            boolean logarithmic = isLogScale == null ? metadata.isLogScaling() : isLogScale.booleanValue();

            // Now get the colour scale range
            Extent<Float> colorScaleRange = GetMapStyleRequest.getColorScaleRange(params);
            if (colorScaleRange == null) {
                // Use the layer's default range if none is specified
                colorScaleRange = metadata.getColorScaleRange();
            } else if (colorScaleRange.isEmpty()) {
                throw new WmsException("Cannot automatically create a colour scale "
                        + "for a legend graphic.  Use COLORSCALERANGE=default or specify "
                        + "the scale extremes explicitly.");
            }

            // Now create the legend image
            legend = palette
                    .createLegend(numColourBands, feature.getName(),
                            MetadataUtils.getUnitsString(feature, memberName), logarithmic,
                            colorScaleRange);
        }
        httpServletResponse.setContentType("image/png");
        ImageIO.write(legend, "png", httpServletResponse.getOutputStream());

        return null;
    }

    /**
     * Outputs a transect (data value versus distance along a path) in PNG or
     * XML format.
     */
    protected ModelAndView getTransect(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws Exception {
        // Parse the request parameters
        String layerName = params.getMandatoryString("layer");
        String memberName = WmsUtils.getMemberName(layerName);
        
        Feature feature = featureFactory.getFeature(layerName);
        memberName = MetadataUtils.getScalarMemberName(feature, memberName);;
        GridFeature gridFeature = null;
        boolean hasVerticalAxis = false;
        TimePosition tValue = null;
        VerticalPosition zValue = null;
        VerticalAxis vAxis = null;
        if(feature instanceof GridFeature){
            gridFeature = (GridFeature) feature;
        } else if(feature instanceof GridSeriesFeature){
            GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;
            
            List<TimePosition> tValues = getTimeValues(params.getString("time"), feature);
            tValue = tValues.isEmpty() ? null : tValues.get(0);
            zValue = getElevationValue(params.getString("elevation"), feature);
            vAxis = gridSeriesFeature.getCoverage().getDomain().getVerticalAxis();
            if(vAxis != null && vAxis.size() > 1){
                hasVerticalAxis = true;
            }
            gridFeature = gridSeriesFeature.extractGridFeature(gridSeriesFeature.getCoverage().getDomain()
                    .getHorizontalGrid(), zValue, tValue, CollectionUtils.setOf(memberName));
        } else {
            throw new WmsException("Cannot get a transect for a non-gridded feature");
        }
        /*
         * At no point do we want the non-plottable member name, so replace it
         * with a guaranteed plottable member
         */

        String crsCode = params.getMandatoryString("crs");
        String lineString = params.getMandatoryString("linestring");
        String outputFormat = params.getMandatoryString("format");
        if (!outputFormat.equals(FEATURE_INFO_PNG_FORMAT) && !outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
            throw new InvalidFormatException(outputFormat);
        }

        // Parse the line string, which is in the form "x1 y1, x2 y2, x3 y3"
        final LineString transect = new LineString(lineString, WmsUtils.getCrs(crsCode), params.getWmsVersion());
        log.debug("Got {} control points", transect.getControlPoints().size());

        // Find the optimal number of points to sample the layer's source grid
        HorizontalDomain transectDomain = Charting.getOptimalTransectDomain(gridFeature.getCoverage()
                .getDomain(), transect);
        log.debug("Using transect consisting of {} points", transectDomain.getDomainObjects().size());

        // Now output the data in the selected format
        response.setContentType(outputFormat);
        if (outputFormat.equals(FEATURE_INFO_PNG_FORMAT)) {
            String datasetId = WmsUtils.getDatasetId(layerName);
            String copyright = ((Config) serverConfig).getDatasetById(datasetId).getCopyrightStatement();
            JFreeChart chart = Charting.createTransectPlot(gridFeature, memberName, transect, copyright, hasVerticalAxis);
            int width = 400;
            int height = 300;

            // If we have a layer with more than one elevation value, let's also
            // create a vertical section plot underneath.
            if (hasVerticalAxis) {
                /*
                 * This can only be true if we have a GridSeriesFeature, so we
                 * can cast
                 */
                JFreeChart verticalSectionChart = createVerticalSectionChart(params,
                        (GridSeriesFeature) feature, tValue, transect, transectDomain);

                // Create the combined chart with both the transect and the
                // vertical section
                CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis(
                        "distance along path (arbitrary units)"));
                plot.setGap(20.0);
                plot.add(chart.getXYPlot(), 1);
                plot.add(verticalSectionChart.getXYPlot(), 1);
                plot.setOrientation(PlotOrientation.VERTICAL);
                String title = feature.getName() + " ("
                        + MetadataUtils.getUnitsString(feature, memberName) + ")" + " at " + zValue
                        + vAxis.getVerticalCrs().getUnits().getUnitString();
                chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);

                // set left margin to 10 to avoid number wrap at color bar
                RectangleInsets r = new RectangleInsets(0, 10, 0, 0); 
                chart.setPadding(r);

                // Use the legend from the vertical section chart
                chart.addSubtitle(verticalSectionChart.getSubtitle(0));

                height = 600;
            }

            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
        } else if (outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
            // Read the data from the data source, without using the tile cache
            if(!Number.class.isAssignableFrom(feature.getCoverage().getScalarMetadata(memberName).getValueType())){
                throw new IllegalArgumentException("The member "+memberName+" contains non-numerical data");
            }
            List<Float> transectData = new ArrayList<Float>();
            List<HorizontalPosition> positions = transectDomain.getDomainObjects();
            for (HorizontalPosition pos : positions) {
                transectData.add(((Number) gridFeature.getCoverage().evaluate(pos)).floatValue());
            }
            
            // Output data as XML using a template
            // First create an ordered map of ProjectionPoints to data values
            Map<HorizontalPosition, Float> dataPoints = new LinkedHashMap<HorizontalPosition, Float>();
            List<? extends HorizontalPosition> points = transectDomain.getDomainObjects();
            for (int i = 0; i < points.size(); i++) {
                dataPoints.put(points.get(i), transectData.get(i));
            }

            Map<String, Object> models = new HashMap<String, Object>();
            models.put("crs", crsCode);
            models.put("layer", feature);
            models.put("linestring", lineString);
            models.put("data", dataPoints);
            return new ModelAndView("showTransect_xml", models);
        }
        return null;
    }

    /**
     * Outputs a timeseries plot in PNG or JPEG format.
     */
    protected ModelAndView getTimeseries(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, IOException {
        String layerName = params.getMandatoryString("layer");
        
        PointSeriesFeature pointSeriesFeature = null;
        Feature feature = featureFactory.getFeature(layerName);
        final String memberName = MetadataUtils.getScalarMemberName(feature, WmsUtils.getMemberName(layerName));
        String outputFormat = params.getMandatoryString("format");
        if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                && !"image/jpg".equals(outputFormat)) {
            throw new InvalidFormatException(outputFormat + " is not a valid output format");
        }
        
        String timeString = params.getMandatoryString("time");
        Extent<TimePosition> timeRange = getTimeRange(timeString, feature);
        
        if(feature instanceof PointSeriesFeature){
            pointSeriesFeature = (PointSeriesFeature) feature;
            /*
             * TODO extract sub-feature according to times
             * TODO do this for all features (extraction of sub-feature, that is)
             */
        } else if(feature instanceof GridSeriesFeature){
            String crsCode = params.getString("crs");
            String point = params.getString("point");
            VerticalPosition zValue = getElevationValue(params.getString("elevation"), feature);
            // Get the required coordinate reference system, forcing longitude-first
            // axis order.
            final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
            
            // The location of the vertical profile
            String[] coords = point.trim().split(" +"); // allows one or more spaces
            // to be used as a delimiter
            if (coords.length != 2) {
                throw new WmsException("Invalid POINT format");
            }
            int lonIndex = 0;
            int latIndex = 1;
            // If we have lat lon order...
            if (crsCode.equalsIgnoreCase("EPSG:4326") && params.getWmsVersion().equalsIgnoreCase("1.3.0")) {
                // Swap the co-ordinates to lon lat order
                latIndex = 0;
                lonIndex = 1;
            }
            
            double x, y;
            try {
                x = Double.parseDouble(coords[lonIndex]);
                y = Double.parseDouble(coords[latIndex]);
            } catch (NumberFormatException nfe) {
                throw new WmsException("Invalid POINT format");
            }
            HorizontalPosition pos = new HorizontalPositionImpl(x, y, crs);
            
            GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;

            pointSeriesFeature = gridSeriesFeature.extractPointSeriesFeature(pos, zValue,
                    timeRange, CollectionUtils.setOf(memberName));
            
        } else {
            throw new WmsException("Cannot get a time series for this type of feature");
        }
        
        JFreeChart chart = Charting.createTimeseriesPlot(pointSeriesFeature, memberName);
        response.setContentType(outputFormat);
        int width = 500;
        int height = 400;
        if ("image/png".equals(outputFormat)) {
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
        } else {
            // Must be a JPEG
            ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width, height);
        }
        
        return null;
    }
    
    /**
     * Outputs a vertical profile plot in PNG or JPEG format.
     */
    protected ModelAndView getVerticalProfile(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, IOException {
        String layerName = params.getMandatoryString("layer");

        ProfileFeature profileFeature = null;
        Feature feature = featureFactory.getFeature(layerName);
        final String memberName = MetadataUtils.getScalarMemberName(feature, WmsUtils.getMemberName(layerName));
        String outputFormat = params.getMandatoryString("format");
        if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                && !"image/jpg".equals(outputFormat)) {
            throw new InvalidFormatException(outputFormat + " is not a valid output format");
        }
        if(feature instanceof ProfileFeature){
            profileFeature = (ProfileFeature) feature;
        } else if(feature instanceof GridSeriesFeature){
            GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;
    
            String crsCode = params.getMandatoryString("crs");
            String point = params.getMandatoryString("point");
            List<TimePosition> tValues = getTimeValues(params.getString("time"), feature);
            TimePosition tValue = tValues.isEmpty() ? null : tValues.get(0);
    
            // Get the required coordinate reference system, forcing longitude-first
            // axis order.
            final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
    
            // The location of the vertical profile
            String[] coords = point.trim().split(" +"); // allows one or more spaces
            // to be used as a delimiter
            if (coords.length != 2) {
                throw new WmsException("Invalid POINT format");
            }
            int lonIndex = 0;
            int latIndex = 1;
            // If we have lat lon order...
            if (crsCode.equalsIgnoreCase("EPSG:4326") && params.getWmsVersion().equalsIgnoreCase("1.3.0")) {
                // Swap the co-ordinates to lon lat order
                latIndex = 0;
                lonIndex = 1;
            }
    
            double x, y;
            try {
                x = Double.parseDouble(coords[lonIndex]);
                y = Double.parseDouble(coords[latIndex]);
            } catch (NumberFormatException nfe) {
                throw new WmsException("Invalid POINT format");
            }
            HorizontalPosition pos = new HorizontalPositionImpl(x, y, crs);
    
            profileFeature = gridSeriesFeature.extractProfileFeature(pos, tValue,
                    CollectionUtils.setOf(memberName));
                    
        } else {
            throw new WmsException("Cannot get a vertical profile of this type of feature");
        }
        // Now create the vertical profile plot
        JFreeChart chart = Charting.createVerticalProfilePlot(profileFeature, memberName);

        response.setContentType(outputFormat);
        int width = 500;
        int height = 400;
        if ("image/png".equals(outputFormat)) {
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
        } else {
            // Must be a JPEG
            ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width, height);
        }

        return null;
    }

    private JFreeChart createVerticalSectionChart(RequestParams params, GridSeriesFeature feature, TimePosition tValue,
            LineString lineString, HorizontalDomain transectDomain) throws WmsException,
            InvalidDimensionValueException, IOException {
        // Look for styling parameters in the URL
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);
        Extent<Float> scaleRange = GetMapStyleRequest.getColorScaleRange(params);
        
        // Parse the request parameters
        String layerName = params.getMandatoryString("layer");
        String memberName = MetadataUtils.getScalarMemberName(feature, WmsUtils.getMemberName(layerName));

        FeaturePlottingMetadata metadata = WmsUtils.getMetadata((Config) serverConfig, layerName);
        if (scaleRange == null)
            scaleRange = metadata.getColorScaleRange();
        // TODO: deal with auto scale ranges - look at actual values extracted
        Boolean logScale = GetMapStyleRequest.isLogScale(params);
        if (logScale == null)
            logScale = metadata.isLogScaling();
        // TODO: repeats code from GetLegendGraphic
        String paletteName = params.getString("palette");
        /*
         * If paletteName is null, this will get the default palette
         */
        ColorPalette palette = ColorPalette.get(paletteName);

        // Read data from each elevation in the source grid
        List<List<Float>> sectionData = new ArrayList<List<Float>>();
        for(HorizontalPosition pos : transectDomain.getDomainObjects()){
            final ProfileCoverage pCoverage = feature.extractProfileFeature(pos, tValue, CollectionUtils.setOf(memberName)).getCoverage();
            final Class<?> clazz = pCoverage.getScalarMetadata(memberName).getValueType();
            List<Float> pointProfile = new AbstractList<Float>() {
                @Override
                @SuppressWarnings("unchecked")
                public Float get(int index) {
                    List<?> values = pCoverage.getValues();
                    if (values == null)
                        return null;
                    if (clazz == Float.class) {
                        return ((List<Float>) values).get(index);
                    } else if (clazz == Vector2D.class) {
                        return ((List<Vector2D<Float>>) values).get(index).getMagnitude();
                    } else {
                        // Shouldn't happen
                        throw new UnsupportedOperationException("Unsupported layer type");
                    }
                }

                @Override
                public int size() {
                    return (int) pCoverage.size();
                }
            };
            sectionData.add(pointProfile);
        }

        /*
         * If the user has specified COLORSCALERANGE=auto, we will use the
         * actual minimum and maximum values of the extracted data to generate
         * the scale
         */
        float max = Float.NEGATIVE_INFINITY;
        float min = Float.POSITIVE_INFINITY;
        if (scaleRange.isEmpty()) {
            for (List<Float> data : sectionData) {
                Extent<Float> minMax = Extents.findMinMax(data);
                max = Math.max(max, minMax.getHigh());
                min = Math.min(min, minMax.getLow());
            }
            scaleRange = Extents.newExtent(min, max);
        }

        VerticalPosition zValue = getElevationValue(params.getString("elevation"), feature);

        return Charting.createVerticalSectionChart(feature, memberName, lineString, scaleRange,
                palette, numColourBands, logScale, zValue, tValue);
    }

    /**
     * Generate the vertical section JfreeChart object
     */
    protected ModelAndView getVerticalSection(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, InvalidFormatException, IOException,
            InvalidCrsException, InvalidLineStringException {

        // Parse the request parameters
        String layerStr = params.getMandatoryString("layer");
        Feature feature = featureFactory.getFeature(layerStr);
        if(!(feature instanceof GridSeriesFeature)){
            throw new WmsException(
                    "Can only create vertical section chart from a GridSeriesFeature");
        } else {
            GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;
            
            String crsCode = params.getMandatoryString("crs");
            String lineStr = params.getMandatoryString("linestring");
            List<TimePosition> tValues = getTimeValues(params.getString("time"), feature);
            TimePosition tValue = tValues.isEmpty() ? null : tValues.get(0);
    
            // Parse the parameters connected with styling
            // TODO repeats code from GetMap and GetLegendGraphic
            String outputFormat = params.getMandatoryString("format");
            if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                    && !"image/jpg".equals(outputFormat)) {
                throw new InvalidFormatException(outputFormat + " is not a valid output format");
            }
    
            // Get the required coordinate reference system, forcing longitude-first
            // axis order.
            final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);
    
            // Parse the line string, which is in the form "x1 y1, x2 y2, x3 y3"
            final LineString lineString = new LineString(lineStr, crs, params.getMandatoryWmsVersion());
            log.debug("Got {} control points", lineString.getControlPoints().size());
    
            // Find the optimal number of points to sample the layer's source grid
            HorizontalDomain transectDomain = Charting.getOptimalTransectDomain(gridSeriesFeature
                    .getCoverage().getDomain().getHorizontalGrid(), lineString);
            log.debug("Using transect consisting of {} points", transectDomain.getDomainObjects().size());
    
            JFreeChart chart = createVerticalSectionChart(params, gridSeriesFeature, tValue, lineString, transectDomain);
    
            response.setContentType(outputFormat);
            int width = 500;
            int height = 400;
            if ("image/png".equals(outputFormat)) {
                ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
            } else {
                // Must be a JPEG
                ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width, height);
            }
    
            return null;
        }
    }

    /** Compares double values based upon their absolute value */
    private static final Comparator<Double> ABSOLUTE_VALUE_COMPARATOR = new Comparator<Double>() {
        @Override public int compare(Double d1, Double d2) {
            return Double.compare(Math.abs(d1), Math.abs(d2));
        }
    };

    /**
     * Gets the elevation value requested by the client, regardless of whether
     * it is present in the coverage
     * 
     * @param zValue
     *            the value of the ELEVATION string from the request
     * @return the elevation value requested by the client. Returns
     *         {@link Layer#getDefaultElevationValue()
     *         layer.getDefaultElevationValue()} if zValue is null and the layer
     *         supports a default elevation value. Returns {@link Double#NaN} if
     *         the layer does not have an elevation axis.
     * @throws InvalidDimensionValueException
     *             if the provided z value is not a valid number, or if zValue
     *             is null and the layer does not support a default elevation
     *             value
     */
    static VerticalPosition getElevationValue(String zValue, Feature feature) throws InvalidDimensionValueException {
        /*
         * TODO Check exactly what the required behaviour is
         */
        if (feature instanceof PointSeriesFeature) {
            if (zValue != null && !zValue.equals("")) {
                return new VerticalPositionImpl(Double.parseDouble(zValue),
                        ((PointSeriesFeature) feature).getVerticalPosition()
                                .getCoordinateReferenceSystem());
            } else {
                return ((PointSeriesFeature) feature).getVerticalPosition();
            }
        }
        VerticalAxis vAxis = WmsUtils.getVerticalAxis(feature);

        if (vAxis == null || vAxis.size() == 0) {
            return new VerticalPositionImpl(Double.NaN, null);
        }
        if (zValue == null) {
            double defaultVal;
            if (vAxis.getVerticalCrs().isPressure()) {
                defaultVal = Collections.max(vAxis.getCoordinateValues());
            } else {
                defaultVal = Collections.min(vAxis.getCoordinateValues(), ABSOLUTE_VALUE_COMPARATOR);
            }
            return new VerticalPositionImpl(defaultVal, vAxis.getVerticalCrs());
        }

        // Check to see if this is a single value (the
        // user hasn't requested anything of the form z1,z2 or start/stop/step)
        if (zValue.contains(",") || zValue.contains("/")) {
            throw new InvalidDimensionValueException("elevation", zValue);
        }

        try {
            return new VerticalPositionImpl(Double.parseDouble(zValue), vAxis.getVerticalCrs());
        } catch (NumberFormatException nfe) {
            throw new InvalidDimensionValueException("elevation", zValue);
        }
    }

    private static Extent<TimePosition> getTimeRange(String timeString, Feature feature)
            throws InvalidDimensionValueException {
        TimeAxis tAxis = WmsUtils.getTimeAxis(feature);
        
        // If the layer does not have a time axis return an extent
        if (tAxis == null)
            throw new IllegalArgumentException("Cannot get a time range - there is no time axis");
        
        if(timeString == null){
            List<TimePosition> timeValues = getTimeValues(timeString, feature);
            TimePosition tPos = timeValues.get(0);
            return Extents.newExtent(tPos, tPos);
        }
        
        String[] startStop = timeString.split("/");
        if (startStop.length == 1) {
            // This is a single time value
            TimePosition tValue = findTValue(startStop[0], tAxis);
            return Extents.newExtent(tValue, tValue);
        } else if (startStop.length == 2) {
            // Use all time values from start to stop inclusive
            return Extents.newExtent(findTValue(startStop[0], tAxis), findTValue(startStop[1], tAxis));
        } else {
            throw new InvalidDimensionValueException("time", timeString);
        }
    }
    
    /**
     * Gets the list of time values requested by the client. If the layer does
     * not have a time axis the timeString will be ignored and an empty List
     * will be returned.
     * 
     * @param timeString
     *            the string provided for the TIME parameter, or null if there
     *            was no TIME parameter in the client's request
     * @return the list of time values requested by the client or an empty List
     *         if the layer does not have a time axis.
     * @throws InvalidDimensionValueException
     *             if the time string cannot be parsed, or if any of the
     *             requested times are not valid times for the layer
     */
    static List<TimePosition> getTimeValues(String timeString, Feature feature)
            throws InvalidDimensionValueException {

        TimeAxis tAxis = WmsUtils.getTimeAxis(feature);
        // If the layer does not have a time axis return an empty list
        if (tAxis == null)
            return Collections.emptyList();

        // Use the default time if none is specified
        if (timeString == null) {
            TimePosition defaultDateTime;

            int index = TimeUtils.findTimeIndex(tAxis.getCoordinateValues(), new TimePositionJoda());
            if (index < 0) {
                // We can calculate the insertion point
                int insertionPoint = -(index + 1); // see docs for
                // Collections.binarySearch()
                // We return the index of the most recent past time
                if (insertionPoint > 0) {
                    index = insertionPoint - 1; // The most recent past time
                } else {
                    index = 0; // All DateTimes on the axis are in the future,
                    // so we take the earliest
                }
            }

            defaultDateTime = tAxis.getCoordinateValue(index);

            if (defaultDateTime == null) {
                // Must specify a TIME: this layer does not support a default
                // time value
                throw new InvalidDimensionValueException("time", timeString);
            }
            return Arrays.asList(defaultDateTime);
        }

        // Interpret the time specification
        List<TimePosition> tValues = new ArrayList<TimePosition>();
        for (String t : timeString.split(",")) {
            String[] startStop = t.split("/");
            if (startStop.length == 1) {
                // This is a single time value
                tValues.add(findTValue(startStop[0], tAxis));
            } else if (startStop.length == 2) {
                // Use all time values from start to stop inclusive
                tValues.addAll(findTValues(startStop[0], startStop[1], tAxis));
            } else {
                throw new InvalidDimensionValueException("time", t);
            }
        }
        return tValues;
    }

    /**
     * Gets the index of the DateTime corresponding with the given ISO string,
     * checking that the time is valid for the given layer.
     * 
     * @throws InvalidDimensionValueException
     *             if the layer does not contain the given time, or if the given
     *             ISO8601 string is not valid.
     */
    static int findTIndex(String isoDateTime, TimeAxis tAxis)
            throws InvalidDimensionValueException {
        TimePosition target;
        if (isoDateTime.equalsIgnoreCase("current")) {
            target = WmsUtils.getClosestToCurrentTime(tAxis);
        } else {
            try {
                /*
                 * ISO date strings do not have spaces. However, spaces can be
                 * generated by decoding + symbols from URLs. If the date string
                 * has a space in it, something's going wrong anyway. Chances
                 * are it's this.
                 */
                isoDateTime = isoDateTime.replaceAll(" ", "+");
                target = TimeUtils.iso8601ToDateTime(isoDateTime, tAxis.getCalendarSystem());
            } catch (ParseException e) {
                throw new InvalidDimensionValueException("time", isoDateTime);
            }
        }

        /*
         * Find the equivalent DateTime in the Layer. Note that we can't simply
         * use the contains() method of the List, since this is based on
         * equals(). We want to find the DateTime with the same millisecond
         * instant.
         */
        int index = TimeUtils.findTimeIndex(tAxis.getCoordinateValues(), target);
        if (index < 0) {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        return index;
    }

    /**
     * Gets the DateTime corresponding with the given ISO string, checking that
     * the time is valid for the given layer.
     * 
     * @throws InvalidDimensionValueException
     *             if the layer does not contain the given time, or if the given
     *             ISO8601 string is not valid.
     */
    private static TimePosition findTValue(String isoDateTime, TimeAxis tAxis) throws InvalidDimensionValueException {
        if(tAxis == null){
            return null;
        }
        return tAxis.getCoordinateValue(findTIndex(isoDateTime, tAxis));
    }

    /**
     * Gets a List of integers representing indices along the time axis starting
     * from isoDateTimeStart and ending at isoDateTimeEnd, inclusive.
     * 
     * @param isoDateTimeStart
     *            ISO8601-formatted String representing the start time
     * @param isoDateTimeEnd
     *            ISO8601-formatted String representing the start time
     * @return List of Integer indices
     * @throws InvalidDimensionValueException
     *             if either of the start or end values were not found in the
     *             axis, or if they are not valid ISO8601 times.
     */
    private static List<TimePosition> findTValues(String isoDateTimeStart, String isoDateTimeEnd, TimeAxis tAxis)
            throws InvalidDimensionValueException {
        if(tAxis == null) {
            throw new InvalidDimensionValueException("time", isoDateTimeStart + "/" + isoDateTimeEnd);
        }
        int startIndex = findTIndex(isoDateTimeStart, tAxis);
        int endIndex = findTIndex(isoDateTimeEnd, tAxis);
        if (startIndex > endIndex) {
            throw new InvalidDimensionValueException("time", isoDateTimeStart + "/" + isoDateTimeEnd);
        }
        List<TimePosition> layerTValues = tAxis.getCoordinateValues();
        List<TimePosition> tValues = new ArrayList<TimePosition>();
        for (int i = startIndex; i <= endIndex; i++) {
            tValues.add(layerTValues.get(i));
        }
        return tValues;
    }

    /**
     * Called by Spring to shutdown the controller. This implementation does
     * nothing: subclasses should override if necessary to free resources.
     */
    public void shutdown() {
    }

    /**
     * Called by the Spring framework to inject the object that represents the
     * server's configuration.
     */
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }
}
