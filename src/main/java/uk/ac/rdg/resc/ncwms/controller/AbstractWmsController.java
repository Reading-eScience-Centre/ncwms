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

import java.awt.Font;
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
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.ProfileCoverage;
import uk.ac.rdg.resc.edal.coverage.domain.PointSeriesDomain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.PointSeriesDomainImpl;
import uk.ac.rdg.resc.edal.coverage.grid.GridCell2D;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.BorderedGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridCoordinates2DImpl;
import uk.ac.rdg.resc.edal.coverage.impl.PointSeriesCoverageImpl;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.edal.exceptions.InvalidLineStringException;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.feature.TrajectoryFeature;
import uk.ac.rdg.resc.edal.feature.UniqueMembersFeatureCollection;
import uk.ac.rdg.resc.edal.feature.impl.PointSeriesFeatureImpl;
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
import uk.ac.rdg.resc.edal.position.GeoPosition;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.LonLatPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.Vector2D;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.LittleBigList;
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
 * <tt>web/WEB-INF/ncWMS-beans.xml</tt> defines all this information and also
 * defines that this Controller will handle all requests to the URI pattern
 * <tt>/wms</tt>. (See the SimpleUrlHandlerMapping in
 * <tt>web/WEB-INF/ncWMS-beans.xml</tt>).
 * </p>
 * 
 * <p>
 * See the {@link #handleRequestInternal handleRequestInternal()} method for
 * more information.
 * </p>
 * 
 * @author Jon Blower
 * @author Guy Griffiths
 */
public abstract class AbstractWmsController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(AbstractWmsController.class);
    private static final String FEATURE_INFO_XML_FORMAT = "text/xml";
    private static final String FEATURE_INFO_GML_FORMAT = "application/vnd.ogc.gml";
    private static final String FEATURE_INFO_PNG_FORMAT = "image/png";
    /**
     * The maximum number of layers that can be requested in a single GetMap
     * operation
     */
    public static final int LAYER_LIMIT = 1;

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
        File paletteLocationDir = this.serverConfig.getPaletteFilesLocation(this
                .getServletContext());
        if (paletteLocationDir != null && paletteLocationDir.exists()
                && paletteLocationDir.isDirectory()) {
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
            return this
                    .dispatchWmsRequest(request, params, httpServletRequest, httpServletResponse);
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
            if (ioe.getClass().getName()
                    .equals("org.apache.catalina.connector.ClientAbortException")) {
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
     * Object that returns a Feature given a layer Name, which is unique within
     * a Capabilities document
     */
    public static interface FeatureFactory {
        /**
         * Returns a {@link FeatureCollection} given a layer name, which is
         * unique within a Capabilities document.
         */
        public FeatureCollection<? extends Feature> getFeatureCollection(String layerName)
                throws FeatureNotDefinedException;
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
    protected ModelAndView getCapabilities(Collection<? extends Dataset> datasets,
            TimePosition lastUpdateTime, RequestParams params, HttpServletRequest httpServletRequest)
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
                updateSequence = TimeUtils.iso8601ToDateTime(updateSeqStr,
                        CalendarSystem.CAL_ISO_8601);
            } catch (ParseException iae) {
                throw new InvalidUpdateSequence(updateSeqStr + " is not a valid ISO date-time");
            }
            if (updateSequence.getValue() == lastUpdateTime.getValue()) {
                throw new CurrentUpdateSequence(updateSeqStr);
            } else if (updateSequence.getValue() > lastUpdateTime.getValue()) {
                throw new InvalidUpdateSequence(updateSeqStr
                        + " is later than the current server updatesequence value");
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
        String[] supportedCrsCodes = new String[] { "EPSG:4326", "CRS:84", // Plate
                                                                           // Carree
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
        models.put("layerLimit", LAYER_LIMIT);
        models.put("featureInfoFormats", new String[] { FEATURE_INFO_PNG_FORMAT,
                FEATURE_INFO_XML_FORMAT });
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
        WmsVersion wmsVersion = versionStr == null ? WmsVersion.VERSION_1_3_0 : new WmsVersion(
                versionStr);
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
     */
    protected ModelAndView getMap(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse httpServletResponse) throws WmsException, Exception {
        GetMapRequest getMapRequest = new GetMapRequest(params);
        GetMapStyleRequest styleRequest = getMapRequest.getStyleRequest();
        String mimeType = styleRequest.getImageFormat();
        /*
         * This only supports a simple image format (i.e. not KMZ). If we want
         * KMZ, get it from a separate method
         */
        ImageFormat imageFormat = ImageFormat.get(mimeType);
        /*
         * Need to make sure that the images will be compatible with the
         * requested image format
         */
        if (styleRequest.isTransparent() && !imageFormat.supportsFullyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType
                    + " does not support fully-transparent pixels");
        }
        if (styleRequest.getOpacity() < 100 && !imageFormat.supportsPartiallyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType
                    + " does not support partially-transparent pixels");
        }

        GetMapDataRequest mapDataRequest = getMapRequest.getDataRequest();

        /*
         * Check the dimensions of the image
         */
        if (mapDataRequest.getHeight() > this.serverConfig.getMaxImageHeight()
                || mapDataRequest.getWidth() > this.serverConfig.getMaxImageWidth()) {
            throw new WmsException("Requested image size exceeds the maximum of "
                    + this.serverConfig.getMaxImageWidth() + "x"
                    + this.serverConfig.getMaxImageHeight());
        }

        String[] layers = mapDataRequest.getLayers();
        String[] styles = styleRequest.getStyles();

        if (layers.length != styles.length) {
            throw new WmsException("Must have exactly one style per layer requested");
        }

        if (layers.length > LAYER_LIMIT) {
            throw new WmsException("Only " + LAYER_LIMIT + " layer(s) can be plotted at once");
        }

        /*
         * The following is only valid if we have a LAYER_LIMIT of 1.
         * 
         * This is currently the case, since we have made a conscious decision
         * to only support single layer querying (multiple layers can of course
         * be composited by the client). If this changes, changes will have to
         * be made here (specifically looping through all layers and styles)
         */

        String layerName = layers[0];
        String style = styles[0];

        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        if(featureCollection == null){
            throw new WmsException("Layer not yet loaded");
        }
        
        String memberName = WmsUtils.getMemberName(layerName);

        FeaturePlottingMetadata metadata = WmsUtils.getMetadata((Config) serverConfig, layerName);

        /*
         * Now set all of the styling information for this layer
         */
        Extent<Float> scaleRange = styleRequest.getColorScaleRange();
        if (scaleRange == null) {
            scaleRange = metadata.getColorScaleRange();
        }
        Boolean logScale = styleRequest.isScaleLogarithmic();
        if (logScale == null) {
            logScale = metadata.isLogScaling();
        }

        /*
         * We start with a default plot style
         */
        PlotStyle plotStyle = PlotStyle.DEFAULT;

        String[] styleStrEls = style.split("/");
        /*
         * We choose the plot style based on the request
         */
        String styleType = styleStrEls[0];
        try {
            plotStyle = PlotStyle.valueOf(styleType.toUpperCase());
        } catch (IllegalArgumentException iae) {
            /*
             * Ignore this, and just use default
             */
        }

        MapStyleDescriptor styleDescriptor = new MapStyleDescriptor();
        styleDescriptor.setColorPalette(metadata.getPaletteName());

        /*
         * And set the palette
         */
        String paletteName = null;
        if (plotStyle.usesPalette()) {
            if (styleStrEls.length > 1) {
                paletteName = styleStrEls[1];
            }
            styleDescriptor.setColorPalette(paletteName);
        }
        styleDescriptor.setScaleRange(scaleRange);
        styleDescriptor.setTransparent(styleRequest.isTransparent());
        styleDescriptor.setLogarithmic(logScale);
        styleDescriptor.setOpacity(styleRequest.getOpacity());
        styleDescriptor.setBgColor(styleRequest.getBackgroundColour());
        styleDescriptor.setNumColourBands(styleRequest.getNumColourBands());
        /*
         * All styling information set
         */

        /*
         * Create the map plotter object
         */
        BoundingBox bbox = new BoundingBoxImpl(mapDataRequest.getBbox(),
                WmsUtils.getCrs(mapDataRequest.getCrsCode()));
        MapPlotter mapPlotter = new MapPlotter(styleDescriptor, mapDataRequest.getWidth(),
                mapDataRequest.getHeight(), bbox, mapDataRequest.isAnimation());

        /*
         * All this is needed for KML.
         */
        String name = "";
        String description = "";
        String zString = "";
        String units = "";
        List<String> timeStrings = null;

        if (featureCollection instanceof UniqueMembersFeatureCollection) {
            /*-
             * There will only be a single feature plotted. This means that the
             * UI will have sent:
             * 
             * ELEVATION - a single value or none
             * TIME - a single value or none for a map, multiple values for an animation
             * 
             * No COLORBY/XXXX parameters
             */

            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection)
                    .getFeatureContainingMember(memberName);

            VerticalPosition zValue = GISUtils.getExactElevation(
                    mapDataRequest.getElevationString(), GISUtils.getVerticalAxis(feature));

            /*
             * Cycle through all the provided timesteps, extracting data for
             * each step
             */
            List<TimePosition> timeValues = WmsUtils.getTimePositionsForString(
                    mapDataRequest.getTimeString(), feature);
            /*
             * Use a single null time value if the layer has no time axis
             */
            if (timeValues.isEmpty()) {
                timeValues = Arrays.asList((TimePosition) null);
            }

            timeStrings = new ArrayList<String>();
            for (TimePosition timeValue : timeValues) {
                /*
                 * Needed for KML
                 */
                String timeString = TimeUtils.dateTimeToISO8601(timeValue);
                timeStrings.add(timeString);

                /*
                 * Only add a label if this is part of an animation
                 */
                String label = null;
                if (mapDataRequest.isAnimation()) {
                    label = timeString;
                }

                /*
                 * This will layer images on top of one another, unless an
                 * animation is set, in which case, each different time value
                 * creates a new frame
                 */
                mapPlotter.addToFrame(feature, memberName, zValue, timeValue, label, plotStyle);
            }

            /*
             * Needed for KML
             */
            units = MetadataUtils.getScalarMetadata(feature, memberName).getUnits().getUnitString();
            description = MetadataUtils.getScalarMetadata(feature, memberName).getDescription();
            if (zValue != null) {
                zString = zValue.toString();
            }
        } else {
            /*
             * Multiple features will be plotted. The most common use case is
             * in-situ data, but this is not guaranteed. What is guaranteed:
             * 
             * ELEVATION - a range, or none
             * 
             * TIME - a range, or none
             * 
             * COLORBY/TIME - if features can vary by time. If not present,
             * defaults to the latest in the range.
             * 
             * COLORBY/DEPTH - if features can vary by depth. If not present,
             * defaults to the closest to sea level in the range.
             * 
             * We don't currently support animations with this, because we
             * haven't decided on a protocol that defines the time steps
             */
            if (mapDataRequest.isAnimation()) {
                throw new WmsException("Cannot create animations with this type of dataset");
            }

            Collection<? extends Feature> features = getMatchingFeatures(mapDataRequest,
                    featureCollection, BorderedGrid.getLargeBoundingBox(bbox,
                            mapDataRequest.getWidth(), mapDataRequest.getHeight(), 8), memberName);

            TimePosition colorByTime = null;
            if (mapDataRequest.getColorbyTimeString() != null) {
                colorByTime = TimeUtils.iso8601ToDateTime(mapDataRequest.getColorbyTimeString(),
                        CalendarSystem.CAL_ISO_8601);
            }

            Double colorByDepth = null;
            if (mapDataRequest.getColorbyElevationString() != null) {
                colorByDepth = Double.parseDouble(mapDataRequest.getColorbyElevationString());
            }

            for (Feature feature : features) {
                VerticalPosition vPos = GISUtils.getClosestElevationTo(colorByDepth, GISUtils.getVerticalAxis(feature));
                TimePosition tPos = GISUtils.getClosestTimeTo(colorByTime,
                        GISUtils.getTimeAxis(feature, false));
                mapPlotter.addToFrame(feature, memberName, vPos, tPos, null, plotStyle);
            }

            /*
             * Needed for KML.
             * 
             * Note that we don't set zString or timeStrings. That's because we
             * don't have a single value z, and we don't support animations for
             * this type of Dataset
             */
            if (features.size() > 0) {
                Feature feature = features.iterator().next();
                ScalarMetadata scalarMetadata = MetadataUtils
                        .getScalarMetadata(feature, memberName);
                if(scalarMetadata != null){
                    description = scalarMetadata.getDescription();
                    units = scalarMetadata.getUnits().getUnitString();
                }
            }
        }
        /*
         * Needed for KML
         */
        name = metadata.getTitle();

        /*
         * Write the image to the client. First we set the HTTP headers
         */
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setContentType(mimeType);

        Integer frameRate = null;
        String fpsString = params.getString("frameRate");
        if (fpsString != null) {
            try {
                frameRate = Integer.parseInt(fpsString);
            } catch (NumberFormatException nfe) {
                /*
                 * Ignore this and just use the default
                 */
            }
        }

        BufferedImage legend = null;
        if (imageFormat.requiresLegend()) {
            legend = styleDescriptor.getLegend(name, units);
        }
        if (imageFormat instanceof KmzFormat) {
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + name
                    + ".kmz");
        }

        /*
         * Write the images to the output stream
         */
        imageFormat.writeImage(mapPlotter.getRenderedFrames(),
                httpServletResponse.getOutputStream(), name, description, mapDataRequest.getBbox(),
                timeStrings, zString, legend, frameRate);

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
        GetFeatureInfoDataRequest featureDataRequest = request.getDataRequest();

        String outputFormat = request.getOutputFormat();
        // Check the output format
        if (!outputFormat.equals(FEATURE_INFO_XML_FORMAT)
                && !outputFormat.equals(FEATURE_INFO_GML_FORMAT)) {
            throw new InvalidFormatException("The output format " + request.getOutputFormat()
                    + " is not valid for GetFeatureInfo");
        }

        String[] layers = featureDataRequest.getLayers();

        if (layers.length > LAYER_LIMIT) {
            throw new WmsException("Only " + LAYER_LIMIT + " layer(s) can be requested at once");
        }

        /*
         * The following is only valid if we have a LAYER_LIMIT of 1.
         * 
         * This is currently the case, since we have made a conscious decision
         * to only support single layer querying (multiple layers can of course
         * be composited by the client). If this changes, changes will have to
         * be made here (specifically looping through all layers and styles)
         */

        String layerName = layers[0];
        String memberName = WmsUtils.getMemberName(layerName);

        FeatureCollection<? extends Feature> featureCollection = featureFactory.getFeatureCollection(layerName);
        if(featureCollection == null){
            throw new WmsException("Layer not yet loaded");
        }

        /*
         * Get the grid onto which the data is being projected
         */
        RegularGrid grid = WmsUtils.getImageGrid(featureDataRequest);
        /*
         * Get the real-world coordinate values of the point of interest
         * Remember that the vertical axis is flipped
         */
        int j = featureDataRequest.getHeight() - featureDataRequest.getPixelRow() - 1;
        HorizontalPosition pos = grid.transformCoordinates(new GridCoordinates2DImpl(
                featureDataRequest.getPixelColumn(), j));
        /*
         * Transform these coordinates into lon-lat
         */
        final LonLatPosition lonLat = GISUtils.transformToWgs84LonLat(pos);

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("longitude", lonLat.getLongitude());
        models.put("latitude", lonLat.getLatitude());
        models.put("crs", featureDataRequest.getCrsCode());

        String timeString = featureDataRequest.getTimeString();

        /*-
         * Now we map date-times to data values
         * The map is kept in order of ascending time
         */
        List<FeatureInfo> featureData = new ArrayList<FeatureInfo>();

        if (featureCollection instanceof UniqueMembersFeatureCollection) {
            /*-
             * There will only be a single feature plotted. This means that the
             * UI will have sent:
             * 
             * ELEVATION - a single value or none
             * TIME - a single value or none for a map, multiple values for an animation
             * 
             * No COLORBY/XXXX parameters
             */

            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection)
                    .getFeatureContainingMember(memberName);

            /*
             * We may be dealing with a member which has multiple children, in
             * which case get the member name of the representative child (whose
             * value we want to return)
             */
            memberName = MetadataUtils.getScalarMemberName(feature, memberName);

            VerticalPosition zValue = GISUtils.getExactElevation(
                    featureDataRequest.getElevationString(), GISUtils.getVerticalAxis(feature));

            HorizontalGrid horizGrid = null;
            if (feature instanceof GridSeriesFeature) {
                horizGrid = ((GridSeriesFeature) feature).getCoverage().getDomain()
                        .getHorizontalGrid();
            } else if (feature instanceof GridFeature) {
                GridFeature gridFeature = (GridFeature) feature;
                horizGrid = gridFeature.getCoverage().getDomain();
            }

            LonLatPosition gridCellCentre = null;
            if (horizGrid != null) {
                GridCell2D gridCell = horizGrid.findContainingCell(pos);
                if (gridCell != null) {
                    gridCellCentre = GISUtils.transformToWgs84LonLat(gridCell.getCentre());
                }
            }

            /*
             * Get the requested timesteps. If the layer doesn't have a time
             * axis then this will return a zero-element List.
             */
            List<TimePosition> tValues = WmsUtils.getTimePositionsForString(timeString, feature);

            Map<TimePosition, Object> timesAndValues = new HashMap<TimePosition, Object>();
            if (tValues.isEmpty()) {
                Object val = WmsUtils.getFeatureValue(feature, pos, zValue, null, memberName);
                timesAndValues.put(null, val);
            } else {
                for (TimePosition time : tValues) {
                    Object val = WmsUtils.getFeatureValue(feature, pos, zValue, time, memberName);
                    timesAndValues.put(time, val);
                }
            }

            featureData.add(new FeatureInfo(featureCollection.getId(), feature.getId(), memberName,
                    gridCellCentre, timesAndValues));
        } else {
            /*
             * First get a 8 pixel bounding box around the clicked position
             */
            HorizontalPosition lowerCorner = grid.transformCoordinates(new GridCoordinates2DImpl(
                    featureDataRequest.getPixelColumn() - 4, j - 4));
            HorizontalPosition upperCorner = grid.transformCoordinates(new GridCoordinates2DImpl(
                    featureDataRequest.getPixelColumn() + 4, j + 4));
            BoundingBox bbox = new BoundingBoxImpl(lowerCorner, upperCorner);

            Collection<? extends Feature> features = getMatchingFeatures(featureDataRequest,
                    featureCollection, bbox, memberName);

            TimePosition colorByTime = null;
            if (featureDataRequest.getColorbyTimeString() != null) {
                colorByTime = TimeUtils.iso8601ToDateTime(
                        featureDataRequest.getColorbyTimeString(), CalendarSystem.CAL_ISO_8601);
            }

            Double colorByDepth = null;
            if (featureDataRequest.getColorbyElevationString() != null) {
                colorByDepth = Double.parseDouble(featureDataRequest.getColorbyElevationString());
            }

            for (Feature feature : features) {
                if(memberName == null || memberName.equals("*")){
                    memberName = null;
                } else {
                    memberName = MetadataUtils.getScalarMemberName(feature, memberName);
                }

                /*
                 * If we have a trajectory feature, we need to get an exact
                 * position to evaulate.
                 * 
                 * This means the nearest position to the click which is still
                 * within the bounding box
                 * 
                 * If this is null we don't want to plot it.
                 * 
                 * If someone complains about this not returning features on a
                 * line (i.e. away from a measurement point, but still on the
                 * trajectory), we either:
                 * 
                 * Adjust the getTrajectoryPosition to ignore the bounding box.
                 * This is almost certainly a very bad idea. A trajectory
                 * bounding box can be mostly empty due to the nature of
                 * bounding boxes. So clicking on a feature may return feature
                 * info for a trajectory feature which is out of view but (e.g.)
                 * encircles the desired one
                 * 
                 * Start indicating the location of the data points with dots.
                 * We currently plot arrow heads at all data points, but only if
                 * our arrow head density is not too high
                 */
                VerticalPosition vPos;
                TimePosition tPos;
                if(feature instanceof TrajectoryFeature){
                    GeoPosition trajectoryPosition = GISUtils.getTrajectoryPosition((TrajectoryFeature) feature, pos, bbox);
                    if(trajectoryPosition == null){
                        continue;
                    }
                    pos = trajectoryPosition.getHorizontalPosition();
                    vPos = trajectoryPosition.getVerticalPosition();
                    tPos = trajectoryPosition.getTimePosition();
                } else {
                    vPos = GISUtils.getClosestElevationTo(colorByDepth, GISUtils.getVerticalAxis(feature));
                    tPos = GISUtils.getClosestTimeTo(colorByTime,
                            GISUtils.getTimeAxis(feature, false));
                }
                
                Object value = WmsUtils.getFeatureValue(feature, pos, vPos, tPos, memberName);

                Map<TimePosition, Object> timesAndValues = new HashMap<TimePosition, Object>();
                timesAndValues.put(tPos, value);
                HorizontalPosition actualPos = GISUtils
                        .getClosestHorizontalPositionTo(pos, feature);
                FeatureInfo featureInfo = new FeatureInfo(featureCollection.getId(),
                        feature.getId(), memberName, actualPos, timesAndValues);
                featureData.add(featureInfo);
            }
        }

        /*
         * Sort the features in order of distance from the clicked point.
         */
        Collections.sort(featureData, new Comparator<FeatureInfo>() {
            @Override
            public int compare(FeatureInfo arg0, FeatureInfo arg1) {
                Double dist0 = Math.pow(arg0.getActualPos().getY() - lonLat.getLatitude(), 2.0)
                        + Math.pow(arg0.getActualPos().getX() - lonLat.getLongitude(), 2.0);
                Double dist1 = Math.pow(arg1.getActualPos().getY() - lonLat.getLatitude(), 2.0)
                        + Math.pow(arg1.getActualPos().getX() - lonLat.getLongitude(), 2.0);
                return dist0.compareTo(dist1);
            }
        });

        if (featureData.size() > featureDataRequest.getFeatureCount()) {
            featureData = featureData.subList(0, featureDataRequest.getFeatureCount());
        }

        models.put("data", featureData);
        if (FEATURE_INFO_XML_FORMAT.equals(outputFormat)) {
            return new ModelAndView("showFeatureInfo_xml", models);
        } else {
            return new ModelAndView("showFeatureInfo_gml", models);
        }
    }

    /*
     * Convenience method (otherwise we repeat code). Gets matching features
     * from an in-situ feature collection using a given bounding box, member
     * name, and GetMapDataRequest
     */
    private static Collection<? extends Feature> getMatchingFeatures(GetMapDataRequest dataRequest,
            FeatureCollection<? extends Feature> featureCollection, BoundingBox bbox,
            String memberName) throws WmsException {
        Extent<TimePosition> tRange = null;
        Extent<Double> zRange = null;

        try {
            tRange = TimeUtils.getTimeRangeForString(dataRequest.getTimeString(),
                    CalendarSystem.CAL_ISO_8601);
        } catch (ParseException pe) {
            throw new WmsException("Cannot create time range from string: "
                    + dataRequest.getTimeString());
        }

        try {
            zRange = WmsUtils.getElevationRangeForString(dataRequest.getElevationString());
        } catch (IllegalArgumentException e) {
            throw new WmsException("Cannot create depth range from string: "
                    + dataRequest.getElevationString());
        }

        Set<String> members;
        if(memberName.equals("*")){
            members = null;
        } else {
            members = CollectionUtils.setOf(memberName);
        }
        
        return featureCollection.findFeatures(bbox, zRange, tRange, members);
    }

    /**
     * Creates and returns a PNG image with the colour scale and range for a
     * given Layer
     */
    protected ModelAndView getColorbar(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse httpServletResponse) throws Exception {
        BufferedImage legend;

        // numColourBands defaults to ColorPalette.MAX_NUM_COLOURS if not set
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);

        String paletteName = params.getString("palette");

        boolean vertical = params.getBoolean("vertical", true);

        int width = params.getPositiveInt("width", 50);
        int height = params.getPositiveInt("height", 200);
        // Find the requested colour palette, or use the default if not set
        ColorPalette palette = ColorPalette.get(paletteName);
        legend = palette.createColorBar(width, height, numColourBands, vertical);

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

        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        if(featureCollection == null){
            throw new WmsException("Layer not yet loaded");
        }

        if (!(featureCollection instanceof UniqueMembersFeatureCollection)) {
            throw new WmsException(
                    "Cannot get a transect - we have multiple features which make up this layer");
        } else {
            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection)
                    .getFeatureContainingMember(memberName);

            memberName = MetadataUtils.getScalarMemberName(feature, memberName);
            ;
            GridFeature gridFeature = null;
            boolean hasVerticalAxis = false;
            TimePosition tValue = null;
            VerticalPosition zValue = null;
            VerticalAxis vAxis = null;
            if (feature instanceof GridFeature) {
                gridFeature = (GridFeature) feature;
            } else if (feature instanceof GridSeriesFeature) {
                GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;

                List<TimePosition> tValues = WmsUtils.getTimePositionsForString(
                        params.getString("time"), feature);
                tValue = tValues.isEmpty() ? null : tValues.get(0);
                zValue = GISUtils.getExactElevation(params.getString("elevation"),
                        GISUtils.getVerticalAxis(feature));
                vAxis = gridSeriesFeature.getCoverage().getDomain().getVerticalAxis();
                if (vAxis != null && vAxis.size() > 1) {
                    hasVerticalAxis = true;
                }
                gridFeature = gridSeriesFeature.extractGridFeature(gridSeriesFeature.getCoverage()
                        .getDomain().getHorizontalGrid(), zValue, tValue,
                        CollectionUtils.setOf(memberName));
            } else {
                throw new WmsException("Cannot get a transect for a non-gridded feature");
            }
            /*
             * At no point do we want the non-plottable member name, so replace
             * it with a guaranteed plottable member
             */

            String crsCode = params.getMandatoryString("crs");
            String lineString = params.getMandatoryString("linestring");
            String outputFormat = params.getMandatoryString("format");
            if (!outputFormat.equals(FEATURE_INFO_PNG_FORMAT)
                    && !outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
                throw new InvalidFormatException(outputFormat);
            }

            // Parse the line string, which is in the form "x1 y1, x2 y2, x3 y3"
            final LineString transect = new LineString(lineString, WmsUtils.getCrs(crsCode),
                    params.getWmsVersion());
            log.debug("Got {} control points", transect.getControlPoints().size());

            // Find the optimal number of points to sample the layer's source
            // grid
            HorizontalDomain transectDomain = Charting.getOptimalTransectDomain(gridFeature
                    .getCoverage().getDomain(), transect);
            log.debug("Using transect consisting of {} points", transectDomain.getDomainObjects()
                    .size());

            // Now output the data in the selected format
            response.setContentType(outputFormat);
            if (outputFormat.equals(FEATURE_INFO_PNG_FORMAT)) {
                String datasetId = WmsUtils.getDatasetId(layerName);
                String copyright = ((Config) serverConfig).getDatasetById(datasetId)
                        .getCopyrightStatement();
                JFreeChart chart = Charting.createTransectPlot(gridFeature, memberName, transect,
                        copyright, hasVerticalAxis);
                int width = 400;
                int height = 300;

                // If we have a layer with more than one elevation value, let's
                // also
                // create a vertical section plot underneath.
                if (hasVerticalAxis) {
                    /*
                     * This can only be true if we have a GridSeriesFeature, so
                     * we can cast
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
                    String title = MetadataUtils.getScalarMetadata(feature, memberName).getTitle()
                            + " at " + zValue;
                    chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
                    /*
                     * This is not ideal. We have already added the copyright
                     * label to the first chart, but then we extract the actual
                     * plot (ignoring the copyright), so we need to add it again
                     * here
                     */
                    if (copyright != null) {
                        final TextTitle textTitle = new TextTitle(copyright);
                        textTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
                        textTitle.setPosition(RectangleEdge.BOTTOM);
                        textTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                        chart.addSubtitle(textTitle);
                    }

                    // set left margin to 10 to avoid number wrap at color bar
                    RectangleInsets r = new RectangleInsets(0, 10, 0, 0);
                    chart.setPadding(r);

                    // Use the legend from the vertical section chart
                    chart.addSubtitle(verticalSectionChart.getSubtitle(0));

                    height = 600;
                }

                ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, width, height);
            } else if (outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
                // Read the data from the data source, without using the tile
                // cache
                if (!Number.class.isAssignableFrom(feature.getCoverage()
                        .getScalarMetadata(memberName).getValueType())) {
                    throw new IllegalArgumentException("The member " + memberName
                            + " contains non-numerical data");
                }
                List<Float> transectData = new ArrayList<Float>();
                List<HorizontalPosition> positions = transectDomain.getDomainObjects();
                for (HorizontalPosition pos : positions) {
                    transectData.add(((Number) gridFeature.getCoverage().evaluate(pos))
                            .floatValue());
                }

                // Output data as XML using a template
                // First create an ordered map of ProjectionPoints to data
                // values
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
        }
        return null;
    }

    /**
     * Outputs a timeseries plot in PNG or JPEG format.
     */
    protected ModelAndView getTimeseries(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, IOException {
        String outputFormat = params.getMandatoryString("format");
        if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                && !"image/jpg".equals(outputFormat)) {
            throw new InvalidFormatException(outputFormat + " is not a valid output format for a profile plot");
        }
        
        String[] layers = params.getMandatoryString("layer").split(",");
        /*
         * For vertical profiles, we need the full path of the feature, so layer
         * IDs are of the form:
         * 
         * featureCollection/feature/member
         */
        List<Feature> featuresToPlot = new ArrayList<Feature>();
        
        String baseMemberName = null;
        
        for(String layer : layers) {
            String[] layerParts = layer.split("/");
            if(layerParts.length != 3){
                throw new WmsException(
                        "For timeseries', layer IDs must be of the form dataset/feature/variable.  These IDs can be obtained by performing a GetFeatureInfo request with the output format set to text/xml");
            }
            FeatureCollection<? extends Feature> featureCollection = featureFactory.getFeatureCollection(layerParts[0]+"/dummy");
            if(featureCollection == null){
                throw new WmsException("Layer not yet loaded");
            }
            featuresToPlot.add(featureCollection.getFeatureById(layerParts[1]));
            if(baseMemberName == null){
                baseMemberName = layerParts[2];
            } else {
                if(!baseMemberName.equals(layerParts[2])){
                    throw new WmsException("For a timeseries plot, all variables need to be the same");
                }
            }
        }

        String timeString = params.getMandatoryString("time");

        List<Feature> timeseriesFeatures = new ArrayList<Feature>();
        for (Feature feature : featuresToPlot) {
            final String memberName = MetadataUtils.getScalarMemberName(feature, baseMemberName);
            Feature timeseriesFeature = null;

            if (feature instanceof PointSeriesFeature) {
                Extent<TimePosition> timeRange;
                try {
                    timeRange = TimeUtils.getTimeRangeForString(timeString,
                            ((PointSeriesFeature) feature).getCoverage().getDomain()
                                    .getCalendarSystem());
                } catch (ParseException e) {
                    throw new WmsException(
                            "Time range is invalid - cannot create a time series plot");
                }
                timeseriesFeature = ((PointSeriesFeature) feature).extractSubFeature(timeRange,
                        CollectionUtils.setOf(memberName));
            } else if (feature instanceof GridSeriesFeature) {
                Extent<TimePosition> timeRange;
                try {
                    timeRange = TimeUtils.getTimeRangeForString(timeString,
                            ((GridSeriesFeature) feature).getCoverage().getDomain()
                                    .getCalendarSystem());
                } catch (ParseException e) {
                    throw new WmsException(
                            "Time range is invalid - cannot create a time series plot");
                }
                VerticalPosition zValue = GISUtils.getExactElevation(params.getString("elevation"),
                        GISUtils.getVerticalAxis(feature));
                
                HorizontalPosition pos = getHorizontalPosition(params);

                GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;

                timeseriesFeature = gridSeriesFeature.extractPointSeriesFeature(pos, zValue,
                        timeRange, CollectionUtils.setOf(memberName));
            } else if (feature instanceof TrajectoryFeature) {
                timeseriesFeature = feature;
            } else {
                /*
                 * We don't have a time axis, but since we might have multiple
                 * features, we may be able to plot a time series anyway.
                 * 
                 * Chances are this will be about as useful as a chocolate
                 * teapot, but:
                 * 
                 * 1. Someone might want it
                 * 
                 * 2. It'll take more work to disable it on the front end than
                 * it did to implement...
                 */
                TimeAxis timeAxis = GISUtils.getTimeAxis(feature, true);
                HorizontalPosition hPos = getHorizontalPosition(params);
                Double targetDepth = null;
                if (params.getString("elevation") != null) {
                    targetDepth = Double.parseDouble(params.getString("elevation"));
                }
                VerticalPosition vPos = GISUtils.getClosestElevationTo(targetDepth, GISUtils.getVerticalAxis(feature));
                
                assert(timeAxis.getCoordinateValues().size() == 1);
                
                PointSeriesDomain domain = new PointSeriesDomainImpl(timeAxis.getCoordinateValues());
                
                ScalarMetadata scalarMetadata = MetadataUtils.getScalarMetadata(feature, memberName);
                PointSeriesCoverageImpl coverage = new PointSeriesCoverageImpl(scalarMetadata.getDescription(), domain);
                Object value = WmsUtils.getFeatureValue(feature, hPos, vPos, timeAxis.getCoordinateValue(0), memberName);
                LittleBigList<Object> littleBigList = new LittleBigList<Object>();
                littleBigList.add(value);
                coverage.addMember(baseMemberName, domain, scalarMetadata.getDescription(),
                        scalarMetadata.getParameter(), scalarMetadata.getUnits(), littleBigList,
                        scalarMetadata.getValueType());
                timeseriesFeature = new PointSeriesFeatureImpl(scalarMetadata.getTitle(),
                        scalarMetadata.getName(), scalarMetadata.getDescription(), coverage, hPos,
                        vPos, null);
            }
            if (timeseriesFeature != null) {
                timeseriesFeatures.add(timeseriesFeature);
            }
        }

        if (timeseriesFeatures.size() == 0) {
            throw new WmsException("Cannot plot a timeseries of " + params.getMandatoryString("layer") + " at this point");
        }

        JFreeChart chart = Charting.createTimeseriesPlot(timeseriesFeatures, baseMemberName);
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
    
    private HorizontalPosition getHorizontalPosition(RequestParams params) throws WmsException{
        String crsCode = params.getString("crs");
        /*
         * Get the required coordinate reference system, forcing
         * longitude-first axis order.
         */
        final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);

        String point = params.getString("point");
        // The location of the vertical profile
        String[] coords = point.trim().split(" +"); // allows one or
                                                    // more spaces
        // to be used as a delimiter
        if (coords.length != 2) {
            throw new WmsException("Invalid POINT format");
        }
        int lonIndex = 0;
        int latIndex = 1;
        // If we have lat lon order...
        if (crsCode.equalsIgnoreCase("EPSG:4326")
                && params.getWmsVersion().equalsIgnoreCase("1.3.0")) {
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
        return new HorizontalPositionImpl(x, y, crs);
    }

    /**
     * Outputs a vertical profile plot in PNG or JPEG format.
     */
    protected ModelAndView getVerticalProfile(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, IOException {
        String outputFormat = params.getMandatoryString("format");
        if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                && !"image/jpg".equals(outputFormat)) {
            throw new InvalidFormatException(outputFormat + " is not a valid output format for a profile plot");
        }
        
        String[] layers = params.getMandatoryString("layer").split(",");
        /*
         * For vertical profiles, we need the full path of the feature, so layer
         * IDs are of the form:
         * 
         * featureCollection/feature/member
         */
        List<Feature> featuresToPlot = new ArrayList<Feature>();
        
        String baseMemberName = null;
        
        for(String layer : layers) {
            String[] layerParts = layer.split("/");
            if(layerParts.length != 3){
                throw new WmsException(
                        "For vertical profiles, layer IDs must be of the form dataset/feature/variable.  These IDs can be obtained by performing a GetFeatureInfo request with the output format set to text/xml");
            }
            FeatureCollection<? extends Feature> featureCollection = featureFactory.getFeatureCollection(layerParts[0]+"/dummy");
            if(featureCollection == null){
                throw new WmsException("Layer not yet loaded");
            }
            featuresToPlot.add(featureCollection.getFeatureById(layerParts[1]));
            if(baseMemberName == null){
                baseMemberName = layerParts[2];
            } else {
                if(!baseMemberName.equals(layerParts[2])){
                    throw new WmsException("For a profile plot, all variables need to be the same");
                }
            }
        }

        String timeString = params.getMandatoryString("time");

        List<ProfileFeature> profileFeatures = new ArrayList<ProfileFeature>();
        for (Feature feature : featuresToPlot) {
            final String memberName = MetadataUtils.getScalarMemberName(feature, baseMemberName);
            ProfileFeature profileFeature = null;

            if (feature instanceof ProfileFeature) {
                profileFeature = (ProfileFeature) feature;
            } else if (feature instanceof GridSeriesFeature) {
                GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;

                String crsCode = params.getMandatoryString("crs");
                String point = params.getMandatoryString("point");
                List<TimePosition> tValues = WmsUtils
                        .getTimePositionsForString(timeString, feature);
                TimePosition tValue = tValues.isEmpty() ? null : tValues.get(0);

                // Get the required coordinate reference system, forcing
                // longitude-first
                // axis order.
                final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);

                // The location of the vertical profile
                String[] coords = point.trim().split(" +"); // allows one or
                                                            // more spaces
                // to be used as a delimiter
                if (coords.length != 2) {
                    throw new WmsException("Invalid POINT format");
                }
                int lonIndex = 0;
                int latIndex = 1;
                // If we have lat lon order...
                if (crsCode.equalsIgnoreCase("EPSG:4326")
                        && params.getWmsVersion().equalsIgnoreCase("1.3.0")) {
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
            if (profileFeature != null) {
                profileFeatures.add(profileFeature);
            }
        }
        // Now create the vertical profile plot
        JFreeChart chart = Charting.createVerticalProfilePlot(profileFeatures, baseMemberName);

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

    private JFreeChart createVerticalSectionChart(RequestParams params, GridSeriesFeature feature,
            TimePosition tValue, LineString lineString, HorizontalDomain transectDomain)
            throws WmsException, InvalidDimensionValueException, IOException {
        // Look for styling parameters in the URL
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);
        Extent<Float> scaleRange = GetMapStyleRequest.getColorScaleRange(params);

        // Parse the request parameters
        String layerName = params.getMandatoryString("layer");
        String memberName = MetadataUtils.getScalarMemberName(feature,
                WmsUtils.getMemberName(layerName));

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
        for (HorizontalPosition pos : transectDomain.getDomainObjects()) {
            final ProfileCoverage pCoverage = feature.extractProfileFeature(pos, tValue,
                    CollectionUtils.setOf(memberName)).getCoverage();
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

        VerticalPosition zValue = GISUtils.getExactElevation(params.getString("elevation"),
                GISUtils.getVerticalAxis(feature));

        return Charting.createVerticalSectionChart(feature, memberName, lineString, scaleRange,
                palette, numColourBands, logScale, zValue, tValue);
    }

    /**
     * Generate the vertical section JfreeChart object
     */
    protected ModelAndView getVerticalSection(RequestParams params, FeatureFactory featureFactory,
            HttpServletResponse response) throws WmsException, InvalidFormatException, IOException,
            InvalidCrsException, InvalidLineStringException {
        String layerName = params.getMandatoryString("layer");
        String memberName = WmsUtils.getMemberName(layerName);

        FeatureCollection<? extends Feature> featureCollection = featureFactory
                .getFeatureCollection(layerName);
        if(featureCollection == null){
            throw new WmsException("Layer not yet loaded");
        }

        if (!(featureCollection instanceof UniqueMembersFeatureCollection)) {
            throw new WmsException(
                    "Cannot get a vertical section - we have multiple features which make up this layer");
        } else {
            Feature feature = ((UniqueMembersFeatureCollection<? extends Feature>) featureCollection)
                    .getFeatureContainingMember(memberName);
            if (!(feature instanceof GridSeriesFeature)) {
                throw new WmsException(
                        "Can only create vertical section chart from a GridSeriesFeature");
            } else {
                GridSeriesFeature gridSeriesFeature = (GridSeriesFeature) feature;

                String crsCode = params.getMandatoryString("crs");
                String lineStr = params.getMandatoryString("linestring");
                List<TimePosition> tValues = WmsUtils.getTimePositionsForString(
                        params.getString("time"), feature);
                TimePosition tValue = tValues.isEmpty() ? null : tValues.get(0);

                // Parse the parameters connected with styling
                // TODO repeats code from GetMap and GetLegendGraphic
                String outputFormat = params.getMandatoryString("format");
                if (!"image/png".equals(outputFormat) && !"image/jpeg".equals(outputFormat)
                        && !"image/jpg".equals(outputFormat)) {
                    throw new InvalidFormatException(outputFormat + " is not a valid output format");
                }

                // Get the required coordinate reference system, forcing
                // longitude-first
                // axis order.
                final CoordinateReferenceSystem crs = WmsUtils.getCrs(crsCode);

                // Parse the line string, which is in the form
                // "x1 y1, x2 y2, x3 y3"
                final LineString lineString = new LineString(lineStr, crs,
                        params.getMandatoryWmsVersion());
                log.debug("Got {} control points", lineString.getControlPoints().size());

                // Find the optimal number of points to sample the layer's
                // source grid
                HorizontalDomain transectDomain = Charting
                        .getOptimalTransectDomain(gridSeriesFeature.getCoverage().getDomain()
                                .getHorizontalGrid(), lineString);
                log.debug("Using transect consisting of {} points", transectDomain
                        .getDomainObjects().size());

                JFreeChart chart = createVerticalSectionChart(params, gridSeriesFeature, tValue,
                        lineString, transectDomain);

                response.setContentType(outputFormat);
                int width = 500;
                int height = 400;
                if ("image/png".equals(outputFormat)) {
                    ChartUtilities
                            .writeChartAsPNG(response.getOutputStream(), chart, width, height);
                } else {
                    // Must be a JPEG
                    ChartUtilities.writeChartAsJPEG(response.getOutputStream(), chart, width,
                            height);
                }

                return null;
            }
        }
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
