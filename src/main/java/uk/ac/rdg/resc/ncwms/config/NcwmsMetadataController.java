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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.ncwms.controller.AbstractMetadataController;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.LayerFactory;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @author Jon Blower
 */
class NcwmsMetadataController extends AbstractMetadataController
{
    private final Config serverConfig;

    public NcwmsMetadataController(Config serverConfig, LayerFactory layerFactory)
    {
        super(layerFactory);
        this.serverConfig = serverConfig;
    }

    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
        HttpServletResponse response, UsageLogEntry usageLogEntry)
        throws MetadataException
    {
        try
        {
            // Check for a "url" parameter, which means that we're delegating to
            // a third-party layer server (TODO)
            String url = request.getParameter("url");
            if (url != null && !url.trim().equals(""))
            {
                usageLogEntry.setRemoteServerUrl(url);
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
        return super.handleRequest(request, response, usageLogEntry);
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
    protected ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry)
        throws Exception
    {
        Map<String, ? extends Dataset> allDatasets = this.serverConfig.getAllDatasets();
        String menu = "default";
        // Let's see if the client has requested a specific menu.  If so, we'll
        // construct the menu based on the appropriate JSP.
        String menuFromRequest = request.getParameter("menu");
        if (menuFromRequest != null && !menuFromRequest.trim().equals(""))
        {
            menu = menuFromRequest.toLowerCase();
        }
        usageLogEntry.setMenu(menu);
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("serverTitle", this.serverConfig.getTitle());
        models.put("datasets", allDatasets);
        return new ModelAndView(menu + "Menu", models);
    }
    
}
