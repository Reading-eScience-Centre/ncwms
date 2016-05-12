/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
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
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.wms.RequestParams;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.WmsServlet;

/**
 * Servlet implementation class NcWmsServlet
 * 
 * @author Guy Griffiths
 * @author Nathan Potter
 */
public class NcwmsServlet extends WmsServlet implements Servlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NcwmsServlet.class);

    /**
     * @see WmsServlet#WmsServlet()
     */
    public NcwmsServlet() {
        super();
    }

    @Override
    public void destroy() {
        super.destroy();
        NcwmsConfig.shutdown();
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        /*
         * Retrieve the pre-loaded catalogue and wire it up
         */
        Object config = servletConfig.getServletContext().getAttribute("NcwmsCatalogue");
        if (config instanceof NcwmsCatalogue) {
            NcwmsCatalogue ncwmsCatalogue = (NcwmsCatalogue) config;
            setCatalogue(ncwmsCatalogue);
        } else {
            String message;
            if (config == null) {
                message = "ncWMS configuration object is null";
            } else {
                message = "ncWMS configuration object is incorrect type:"
                        + config.getClass()
                        + "\nThe \"NcwmsConfig\" attribute of the ServletContext has been incorrectly set.";
            }
            throw new ServletException(message);
        }

        try {
            /*
             * Set the palettes to advertise in GetCapabilities
             */
            List<String> palettesToAdd = new ArrayList<>();
            String advertisedPalettesStr = servletConfig.getServletContext().getInitParameter(
                    "advertisedPalettes");
            if (advertisedPalettesStr != null) {
                String[] palettes = advertisedPalettesStr.split(",");
                for (String palette : palettes) {
                    palettesToAdd.add(palette);
                }
            }
            setCapabilitiesAdvertisedPalettes(palettesToAdd);
        } catch (Exception e) {
            /*
             * If we cannot set the advertised palettes, log it
             */
            log.error("Problem setting advertised palettes", e);
        }
    }

    @Override
    protected void dispatchWmsRequest(String request, RequestParams params,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            WmsCatalogue catalogue) throws Exception {
        /*-
         * For dynamic datasets, users can either specify the DATASET URL
         * parameter, or they can prepend the layer names with the path:
         * 
         * alias/location/under/alias/variable_in_location
         * 
         * For example, if the local path /mnt/data/ has the alias "local", and
         * the file /mnt/data/models/global/may1981.nc containing the variable
         * "sst" is required for a GetMap request, either:
         * 
         * DATASET=local/models/global/may1981.nc&LAYERS=sst
         * 
         * or
         * 
         * LAYERS=local/models/global/may1981.nc/sst
         * 
         * is a valid request. To implement this for all requests, we look for
         * the DATASET parameter and if it exists we prepend it to LAYERS,
         * QUERY_LAYERS (for GetFeatureInfo) and LAYERNAME (for many GetMetadata
         * requests)
         * 
         * To make matters (slightly) more complex, the dataset ID can be
         * encoded within the URL, such that for a normal endpoint of:
         * http://localhost:8080/ncWMS2/wms
         * 
         * the URL:
         * http://localhost:8080/ncWMS2/wms/local/models/global/may1981.nc?...&LAYER=sst...
         * 
         * is exactly equivalent to the two examples above.
         */

        String dataset = params.getString("DATASET");

        /*
         * Code below thanks to NDP:
         */
        // Check the path for dynamic dataset content
        String pathDataset = httpServletRequest.getPathInfo();
        if (pathDataset != null) {
            // Found a dynamic dataset name, woot!

            // Dump leading / chars.
            while (pathDataset.startsWith("/") && pathDataset.length() > 0)
                pathDataset = pathDataset.substring(1);

            // If there's still something left, make it the dataset name.
            if (pathDataset.length() > 0)
                dataset = pathDataset;
        }
        /*
         * End of code from NDP
         */

        if (dataset != null) {
            Map<String, String> newParams = new HashMap<>();
            newParams.put("DATASET", dataset);
            String layersStr = params.getString("LAYERS");
            if (layersStr != null) {
                String[] layers = layersStr.split(",");
                StringBuilder newLayers = new StringBuilder();
                for (String layer : layers) {
                    newLayers.append(combineDatasetAndLayer(dataset, layer) + ",");
                }
                newLayers.deleteCharAt(newLayers.length() - 1);
                newParams.put("LAYERS", newLayers.toString());
            }
            String queryLayersStr = params.getString("QUERY_LAYERS");
            if (queryLayersStr != null) {
                String[] queryLayers = queryLayersStr.split(",");
                StringBuilder newQueryLayers = new StringBuilder();
                for (String queryLayer : queryLayers) {
                    newQueryLayers.append(combineDatasetAndLayer(dataset, queryLayer) + ",");
                }
                newQueryLayers.deleteCharAt(newQueryLayers.length() - 1);
                newParams.put("QUERY_LAYERS", newQueryLayers.toString());
            }
            String layerNameStr = params.getString("LAYERNAME");
            if (layerNameStr != null) {
                newParams.put("LAYERNAME", combineDatasetAndLayer(dataset, layerNameStr));
            }
            params = params.mergeParameters(newParams);
        }
        super.dispatchWmsRequest(request, params, httpServletRequest, httpServletResponse,
                catalogue);
    }

    /**
     * For dynamic datasets specified on the URL, for some operations (e.g.
     * GetMetadata&item=menu) we end up exposing a full (i.e. dataset included)
     * layer name which then would get the dataset ID prepended to it again.
     * 
     * To avoid that, we check if the layername is already of the form
     * "dataset/layername"
     * 
     * @param dataset
     *            The dataset ID
     * @param layer
     *            The layer ID
     * @return The combined ID
     */
    private String combineDatasetAndLayer(String dataset, String layer) {
        if (layer.startsWith(dataset + "/")) {
            /*
             * Dataset ID is already included in layer name.
             */
            return layer;
        } else {
            /*
             * Dataset ID is not already included in layer name.
             */
            return dataset + "/" + layer;
        }
    }
}
