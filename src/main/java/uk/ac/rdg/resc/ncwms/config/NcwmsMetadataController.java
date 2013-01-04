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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import uk.ac.rdg.resc.edal.coverage.Coverage;
import uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.UniqueMembersFeatureCollection;
import uk.ac.rdg.resc.godiva.shared.LayerMenuItem;
import uk.ac.rdg.resc.ncwms.controller.AbstractMetadataController;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
import uk.ac.rdg.resc.ncwms.controller.RequestParams;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @author Jon Blower
 */
public class NcwmsMetadataController extends AbstractMetadataController
{
    public NcwmsMetadataController(Config serverConfig, FeatureFactory layerFactory)
    {
        super(serverConfig, layerFactory);
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
        HttpServletResponse response)
        throws MetadataException
    {
        try
        {
            // Check for a "url" parameter, which means that we're delegating to
            // a third-party layer server (TODO)
            String url = request.getParameter("url");
            if (url != null && !url.trim().equals(""))
            {
                proxyRequest(url, request, response);
                return null; // proxyRequest writes directly to the response object
            }
        }
        catch(Exception e)
        {
            // Wrap all exceptions in a MetadataException.  These will be automatically
            // displayed via displayMetadataException.jsp, in JSON format
            throw new MetadataException(e);
        }
        return super.handleRequest(request, response);
    }
    
    /**
     * Forwards the request to a third party.  In this case this server is acting
     * as a proxy.
     * @param url The URL to the third party server (e.g. "http://myhost.com/ncWMS/wms")
     * @param request Http request object.  All query string parameters (except "&url=")
     * will be copied from this request object to the request to the third party server.
     * @param response Http response object
     * @todo move to a utility class?
     */
    static void proxyRequest(String url, HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Download the data from the remote URL
        // TODO: is there a proxy class we can invoke here?
        StringBuffer fullURL = new StringBuffer(url);
        boolean firstTime = true;
        for (Object urlParamNameObj : request.getParameterMap().keySet())
        {
            fullURL.append(firstTime ? "?" : "&");
            firstTime = false;
            String urlParamName = (String)urlParamNameObj;
            if (!urlParamName.equalsIgnoreCase("url"))
            {
                fullURL.append(urlParamName + "=" + request.getParameter(urlParamName));
            }
        }
        InputStream in = null;
        OutputStream out = null;
        try
        {
            // TODO: better error handling
            URLConnection conn = new URL(fullURL.toString()).openConnection();
            // Set header information (TODO: do all headers)
            response.setContentType(conn.getContentType());
            response.setContentLength(conn.getContentLength());
            in = conn.getInputStream();
            out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) >= 0)
            {
                out.write(buf, 0, len);
            }
        }
        finally
        {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
    
    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy.
     */
    @Override
    protected ModelAndView showMenu(RequestParams params) throws Exception {
        Map<String, ? extends Dataset> allDatasets = super.config.getAllDatasets();
       /*
        * Go through all datasets and construct a tree for each 
        */
        Map<Dataset, LayerMenuItem> datasets = new LinkedHashMap<Dataset, LayerMenuItem>();
        for(String datasetId : allDatasets.keySet()){
            Dataset dataset = allDatasets.get(datasetId);
            
            /*
             * Where we have multiple features with the same member name, we
             * group them
             */
            FeatureCollection<? extends Feature> featureCollection = dataset.getFeatureCollection();
            LayerMenuItem datasetRoot;
            if(featureCollection instanceof UniqueMembersFeatureCollection) {
                datasetRoot = new LayerMenuItem(dataset.getTitle(), null, false);
            } else {
                datasetRoot = new LayerMenuItem(dataset.getTitle(), dataset.getId()+"/*", true);
            }
            /*
             * For every feature...
             */
            if(featureCollection != null && featureCollection.getFeatures() !=  null){
                featureCollection.getMemberIdsInCollection();
                
                Set<String> memberNamesUsed = new LinkedHashSet<String>();
                for(Feature feature : featureCollection.getFeatures()){
                    Coverage<?> coverage = feature.getCoverage();
                    RangeMetadata topMetadata = coverage.getRangeMetadata();
                    addRangeMetadataToTree(datasetRoot, topMetadata, memberNamesUsed, datasetId);
                }
            }
            datasets.put(dataset, datasetRoot);
        }
        
        String menu = "default";
        // Let's see if the client has requested a specific menu. If so, we'll
        // construct the menu based on the appropriate JSP.
        String menuFromRequest = params.getString("menu");
        if (menuFromRequest != null && !menuFromRequest.trim().equals("")) {
            menu = menuFromRequest.toLowerCase();
        }
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("serverTitle", super.config.getTitle());
        models.put("datasets", datasets);
        return new ModelAndView(menu + "Menu", models);
    }

    /**
     * Adds an item to the tree, provided its members are not already part of the
     * tree
     * 
     * @param parentMenuItem
     *            The parent tree node to add to
     * @param topMetadata
     *            The top-level metadata object to be processed
     * @param memberNamesUsed
     *            The member names already used. If new members with these names
     *            would be added, they are ignored
     * @param datasetId
     *            The ID of the containing dataset (used to make the full ID)
     */
    private void addRangeMetadataToTree(LayerMenuItem parentMenuItem, RangeMetadata topMetadata,
            Set<String> memberNamesUsed, String datasetId) {
        Set<String> memberNames = topMetadata.getMemberNames();
        /*
         * For all members of the metadata
         */
        for(String memberName : memberNames){
            /*
             * Have we already done this one?
             */
            if(!memberNamesUsed.contains(memberName)){
                memberNamesUsed.add(memberName);
                /*
                 * Get the metadata
                 */
                RangeMetadata memberMetadata = topMetadata.getMemberMetadata(memberName);
                /*
                 * Create the full ID
                 */
                String id = datasetId + "/" + memberName;
                if(memberMetadata instanceof ScalarMetadata){
                    /*
                     * We have a leaf node - add it to the tree
                     */
                    ScalarMetadata scalarMetadata = (ScalarMetadata) memberMetadata;
                    LayerMenuItem leafItem = new LayerMenuItem(scalarMetadata.getTitle(), id, true);
                    parentMenuItem.addChildItem(leafItem);
                } else {
                    /*
                     * Whether this parent metadata is plottable
                     */
                    List<ScalarMetadata> representativeChildren = memberMetadata.getRepresentativeChildren();
                    boolean plottable = true;
                    if(representativeChildren == null || representativeChildren.size() == 0){
                        plottable = false;
                    }
                    
                    /*
                     * We have a branch node, so we recurse with the current member metadata
                     */
                    LayerMenuItem nodeItem = new LayerMenuItem(memberMetadata.getTitle(), id, plottable);
                    parentMenuItem.addChildItem(nodeItem);
                    
                    addRangeMetadataToTree(nodeItem, memberMetadata, memberNamesUsed, datasetId);
                }
            }
        }
    }
}
