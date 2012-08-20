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
package uk.ac.rdg.resc.ncwms.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.ncwms.cache.TileCache;
import uk.ac.rdg.resc.ncwms.controller.AbstractMetadataController;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.exceptions.OperationNotSupportedException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * <p>
 * WmsController for ncWMS
 * </p>
 * 
 * @author Jon Blower
 */
public final class NcwmsController extends AbstractWmsController {
    // This object handles requests for non-standard metadata
    private AbstractMetadataController metadataController;

    // Cache of recently-extracted data arrays: will be set by Spring
    private TileCache tileCache;

    private FeatureFactory featureFactory;

    public void setMetadataController(AbstractMetadataController metadataController) {
        this.metadataController = metadataController;
    }

    public void setFeatureFactory(FeatureFactory featureFactory) {
        this.featureFactory = featureFactory;
    }

    @Override
    protected ModelAndView dispatchWmsRequest(String request, RequestParams params,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws Exception {
        if (request.equals("GetCapabilities")) {
            return getCapabilities(params, httpServletRequest);
        } else if (request.equals("GetMap")) {
            return getMap(params, featureFactory, httpServletResponse);
        } else if (request.equals("GetFeatureInfo")) {
            // Look to see if we're requesting data from a remote server
            String url = params.getString("url");
            if (url != null && !url.trim().equals("")) {
                NcwmsMetadataController.proxyRequest(url, httpServletRequest, httpServletResponse);
                return null;
            }
            return getFeatureInfo(params, featureFactory, httpServletRequest, httpServletResponse);
        }
        // The REQUESTs below are non-standard and could be refactored into
        // a different servlet endpoint
        else if (request.equals("GetMetadata")) {
            // This is a request for non-standard metadata. (This will one
            // day be replaced by queries to Capabilities fragments, if
            // possible.)
            // Delegate to the NcwmsMetadataController
            return metadataController.handleRequest(httpServletRequest, httpServletResponse);
        } else if (request.equals("GetLegendGraphic")) {
            // This is a request for an image that contains the colour scale
            // and range for a given layer
            return getLegendGraphic(params, featureFactory, httpServletResponse);
            /*
             * } else if (request.equals("GetKML")) { // This is a request for a
             * KML document that allows the selected // layer(s) to be displayed
             * in Google Earth in a manner that // supports region-based
             * overlays. Note that this is distinct // from simply setting "KMZ"
             * as the output format of a GetMap // request: GetKML will give
             * generally better results, but relies // on callbacks to this
             * server. Requesting KMZ files from GetMap // returns a standalone
             * KMZ file. return getKML(params, httpServletRequest); } else if
             * (request.equals("GetKMLRegion")) { // This is a request for a
             * particular sub-region from Google Earth. logUsage = false; // We
             * don't log usage for this operation return getKMLRegion(params,
             * httpServletRequest);
             */
        } else if (request.equals("GetTimeseries")) {
            return getTimeseries(params, featureFactory, httpServletResponse);
        } else if (request.equals("GetTransect")) {
            return getTransect(params, featureFactory, httpServletResponse);
        } else if (request.equals("GetVerticalProfile")) {
            return getVerticalProfile(params, featureFactory, httpServletResponse);
        } else if (request.equals("GetVerticalSection")) {
            return getVerticalSection(params, featureFactory, httpServletResponse);
        } else {
            throw new OperationNotSupportedException(request);
        }
    }

    /**
     * Performs the GetCapabilities operation.
     */
    private ModelAndView getCapabilities(RequestParams params, HttpServletRequest httpServletRequest)
            throws WmsException, IOException {
        TimePosition lastUpdate;
        Collection<? extends Dataset> datasets;

        // The DATASET parameter is an optional parameter that allows a
        // Capabilities document to be generated for a single dataset only
        String datasetId = params.getString("dataset");

        if (datasetId == null || datasetId.trim().equals("")) {
            // No specific dataset has been chosen so we create a Capabilities
            // document including every dataset.
            // First we check to see that the system admin has allowed us to
            // create a global Capabilities doc (this can be VERY large)
            Map<String, ? extends Dataset> allDatasets = getConfig().getAllDatasets();
            if (this.getConfig().getAllowsGlobalCapabilities()) {
                datasets = allDatasets.values();
            } else {
                throw new WmsException("Cannot create a Capabilities document "
                        + "that includes all datasets on this server. "
                        + "You must specify a dataset identifier with &amp;DATASET=");
            }
            // The last update time for the Capabilities doc is the last time
            // any of the datasets were updated
            lastUpdate = getConfig().getLastUpdateTime();
        } else {
            // Look for this dataset
            Dataset ds = getConfig().getDatasetById(datasetId);
            if (ds == null) {
                throw new WmsException("There is no dataset with ID " + datasetId);
            } else if (!ds.isReady()) {
                throw new WmsException("The dataset with ID " + datasetId + " is not ready for use");
            }
            datasets = Arrays.asList(ds);
            // The last update time for the Capabilities doc is the last time
            // this particular dataset was updated
            lastUpdate = ds.getLastUpdateTime();
        }

        return getCapabilities(datasets, lastUpdate, params, httpServletRequest);
    }

    /**
     * Called by Spring to shut down the controller. This shuts down the tile
     * cache.
     */
    @Override
    public void shutdown() {
        if (tileCache != null)
            tileCache.shutdown();
    }

    /** Returns the server configuration cast down to a {@link Config} object */
    Config getConfig() {
        return (Config) serverConfig;
    }

    /** Called by Spring to set the tile cache */
    public void setTileCache(TileCache tileCache) {
        this.tileCache = tileCache;
    }
}
