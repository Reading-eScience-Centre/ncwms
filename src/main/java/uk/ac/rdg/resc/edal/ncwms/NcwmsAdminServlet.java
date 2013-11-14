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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsCacheInfo;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsContact;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDataset;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsServerInfo;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsVariable;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;

public class NcwmsAdminServlet extends NcwmsDigestAuthServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NcwmsAdminServlet.class);

    private VelocityEngine velocityEngine;
    private NcwmsCatalogue catalogue;

    public NcwmsAdminServlet() throws IOException, Exception {
        super();
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        /*
         * Retrieve the pre-loaded catalogue and set the admin password
         */
        Object config = servletConfig.getServletContext().getAttribute("NcwmsCatalogue");
        if (config instanceof NcwmsCatalogue) {
            catalogue = (NcwmsCatalogue) config;
            setPassword(catalogue.getConfig().getServerInfo().getAdminPassword());
        } else {
            throw new ServletException(
                    "ncWMS configuration object is incorrect type.  The \"NcwmsCatalogue\" attribute of the ServletContext has been incorrectly set.");
        }
        /*
         * Retrieve the pre-loaded velocity engine
         */
        Object engine = servletConfig.getServletContext().getAttribute("VelocityEngine");
        if (engine instanceof VelocityEngine) {
            velocityEngine = (VelocityEngine) engine;
        } else {
            throw new ServletException(
                    "VelocityEngine object is incorrect type.  The \"VelocityEngine\" attribute of the ServletContext has been incorrectly set.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        /*
         * Authenticate all requests to this admin servlet
         */
        if (!authenticate(request, response)) {
            return;
        }

        /*
         * Don't cache admin requests - they are all dynamically generated
         */
        /* HTTP 1.1 */
        response.setHeader("Cache-Control", "no-cache");
        /* HTTP 1.0 */
        response.setHeader("Pragma", "no-cache");
        /* Prevents caching at the proxy server */
        response.setDateHeader("Expires", 0);

        /*
         * Parse the request
         */
        String path = request.getPathInfo();

        if ("/".equals(path)) {
            /*
             * Return the front page for admin
             */
            displayMainAdminPage(request, response);
        } else if ("/datasetStatus".equals(path)) {
            displayStatusPage(request, response);
        } else if ("/editVariables".equals(path)) {
            displayEditVariablesPage(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void displayMainAdminPage(HttpServletRequest request, HttpServletResponse response) {
        Template template = velocityEngine.getTemplate("templates/admin.vm");
        VelocityContext context = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(context);

        context.put("catalogue", catalogue);
        context.put("config", catalogue.getConfig());
        context.put("TimeUtils", TimeUtils.class);
        try {
            template.merge(context, response.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayStatusPage(HttpServletRequest request, HttpServletResponse response) {
        String datasetId = request.getParameter("dataset");
        if (StringUtils.isBlank(datasetId)) {
            throw new IllegalArgumentException(
                    "Must supply the \"dataset\" parameter to view status");
        }
        NcwmsDataset dataset = catalogue.getConfig().getDatasetInfo(datasetId);

        Template template = velocityEngine.getTemplate("templates/dataset_status.vm");
        VelocityContext context = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(context);

        context.put("dataset", dataset);
        try {
            template.merge(context, response.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayEditVariablesPage(HttpServletRequest request, HttpServletResponse response) {
        String datasetId = request.getParameter("dataset");
        if (StringUtils.isBlank(datasetId)) {
            throw new IllegalArgumentException(
                    "Must supply the \"dataset\" parameter to edit variables");
        }
        NcwmsDataset dataset = catalogue.getConfig().getDatasetInfo(datasetId);

        Template template = velocityEngine.getTemplate("templates/edit_variables.vm");
        VelocityContext context = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(context);

        context.put("dataset", dataset);
        context.put("paletteNames", ColourPalette.getPredefinedPalettes());
        try {
            template.merge(context, response.getWriter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        /*
         * Authenticate all requests to this admin servlet
         */
        if (!authenticate(request, response)) {
            return;
        }

        /*
         * Don't cache admin requests - they are all dynamically generated
         */
        /* HTTP 1.1 */
        response.setHeader("Cache-Control", "no-cache");
        /* HTTP 1.0 */
        response.setHeader("Pragma", "no-cache");
        /* Prevents caching at the proxy server */
        response.setDateHeader("Expires", 0);

        /*
         * Parse the request
         */
        String path = request.getPathInfo();

        if ("/updateConfig".equals(path)) {
            /*
             * Update the configuration
             */
            updateConfig(request, response);
        } else if ("/updateVariables".equals(path)) {
            /*
             * Update the individual variables
             */
            updateVariables(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles the submission of new configuration information from
     * admin_index.jsp
     */
    private void updateConfig(HttpServletRequest request, HttpServletResponse response) {
        NcwmsContact contact = catalogue.getConfig().getContactInfo();
        NcwmsServerInfo server = catalogue.getConfig().getServerInfo();
        NcwmsCacheInfo cache = catalogue.getConfig().getCacheSettings();

        if (request.getParameter("contact.name") != null) {
            contact.setName(request.getParameter("contact.name"));
            contact.setOrganisation(request.getParameter("contact.org"));
            contact.setTelephone(request.getParameter("contact.tel"));
            contact.setEmail(request.getParameter("contact.email"));

            /* Process the server details */
            server.setTitle(request.getParameter("server.title"));
            server.setDescription(request.getParameter("server.abstract"));
            server.setKeywords(request.getParameter("server.keywords"));
            server.setUrl(request.getParameter("server.url"));
            server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
            server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));
            server.setAllowFeatureInfo(request.getParameter("server.allowfeatureinfo") != null);
            server.setAllowGlobalCapabilities(request
                    .getParameter("server.allowglobalcapabilities") != null);

            /*
             * Save the dataset information, checking for removals First look
             * through the existing datasets for edits.
             */
            List<NcwmsDataset> datasetsToRemove = new ArrayList<NcwmsDataset>();
            /* Keeps track of dataset IDs that have been changed */
            Map<NcwmsDataset, String> changedIds = new HashMap<NcwmsDataset, String>();
            for (NcwmsDataset ds : catalogue.getConfig().getDatasets()) {
                boolean refreshDataset = false;
                if (request.getParameter("dataset." + ds.getId() + ".remove") != null) {
                    datasetsToRemove.add(ds);
                } else {
                    ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
                    String newLocation = request
                            .getParameter("dataset." + ds.getId() + ".location");
                    if (!newLocation.trim().equals(ds.getLocation().trim())) {
                        refreshDataset = true;
                    }
                    ds.setLocation(newLocation);
                    String newDataReaderClass = request.getParameter("dataset." + ds.getId()
                            + ".reader");
                    if (!newDataReaderClass.trim().equals(ds.getDataReaderClass().trim())) {
                        refreshDataset = true;
                    }
                    ds.setDataReaderClass(newDataReaderClass);
                    boolean disabled = request.getParameter("dataset." + ds.getId() + ".disabled") != null;
                    if (disabled == false && ds.isDisabled()) {
                        /* We've re-enabled the dataset so need to reload it */
                        refreshDataset = true;
                    }
                    ds.setDisabled(disabled);
                    ds.setQueryable(request.getParameter("dataset." + ds.getId() + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset."
                            + ds.getId() + ".updateinterval")));
                    ds.setMoreInfo(request.getParameter("dataset." + ds.getId() + ".moreinfo"));
                    ds.setCopyrightStatement(request.getParameter("dataset." + ds.getId()
                            + ".copyright"));

                    ds.setMetadataUrl(request.getParameter("dataset." + ds.getId() + ".metadataUrl"));
                    ds.setMetadataDesc(request.getParameter("dataset." + ds.getId()
                            + ".metadataDesc"));
                    ds.setMetadataMimetype(request.getParameter("dataset." + ds.getId()
                            + ".metadataMimetype"));

                    if (request.getParameter("dataset." + ds.getId() + ".refresh") != null) {
                        refreshDataset = true;
                    }

                    /* Check to see if we have updated the ID */
                    String newId = request.getParameter("dataset." + ds.getId() + ".id").trim();
                    if (!newId.equals(ds.getId())) {
                        changedIds.put(ds, newId);
                        /* The ID will be changed later */
                    }
                }
                if (refreshDataset) {
                    ds.forceRefresh();
                }
            }
            /* Now we can remove the datasets */
            for (NcwmsDataset ds : datasetsToRemove) {
                catalogue.removeDataset(ds.getId());
            }
            /* Now we change the ids of the relevant datasets */
            for (NcwmsDataset ds : changedIds.keySet()) {
                catalogue.changeDatasetId(ds.getId(), changedIds.get(ds));
                /*
                 * Force a refresh of the dataset. We do this in case the new ID
                 * happens to be the same as an existing dataset.
                 */
                ds.forceRefresh();
            }

            /*
             * Now look for the new datasets. The logic below means that we
             * don't have to know in advance how many new datasets the user has
             * created (or how many spaces were available in the admin page)
             */
            int i = 0;
            while (request.getParameter("dataset.new" + i + ".id") != null) {
                /* Look for non-blank ID fields */
                if (!request.getParameter("dataset.new" + i + ".id").trim().equals("")) {
                    NcwmsDataset ds = new NcwmsDataset();
                    ds.setId(request.getParameter("dataset.new" + i + ".id"));
                    ds.setTitle(request.getParameter("dataset.new" + i + ".title"));
                    ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
                    ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
                    ds.setDisabled(request.getParameter("dataset.new" + i + ".disabled") != null);
                    ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset.new" + i
                            + ".updateinterval")));
                    ds.setMoreInfo(request.getParameter("dataset.new" + i + ".moreinfo"));
                    ds.setCopyrightStatement(request.getParameter("dataset.new" + i + ".copyright"));
                    /*
                     * addDataset() contains code to ensure that the dataset
                     * loads its metadata at the next opportunity
                     */
                    catalogue.getConfig().addDataset(ds);
                }
                i++;
            }

            /* Set the properties of the cache */
            cache.setEnabled(request.getParameter("cache.enable") != null);
            cache.setElementLifetimeMinutes(Integer.parseInt(request
                    .getParameter("cache.elementLifetime")));
            cache.setMaxItemsMemory(Integer.parseInt(request
                    .getParameter("cache.maxNumItemsInMemory")));
            cache.setEnableDiskStore(request.getParameter("cache.enableDiskStore") != null);
            cache.setMaxItemsDisk(Integer.parseInt(request.getParameter("cache.maxNumItemsOnDisk")));

            /* Save the updated config information to disk */
            try {
                catalogue.getConfig().save();
            } catch (JAXBException e) {
                log.error("Problem serialising config", e);
            } catch (IOException e) {
                log.error("Problem writing config", e);
            }
        }

        /*
         * This causes a client-side redirect, meaning that the user can safely
         * press refresh in their browser without resubmitting the new config
         * information.
         */
        try {
            response.sendRedirect("");
        } catch (IOException e) {
            /*
             * This error isn't really important
             */
            log.error("Problem redirecting user after config update");
        }
    }

    private void updateVariables(HttpServletRequest request, HttpServletResponse response) {
        /* We only take action if the user pressed "save" */
        if (request.getParameter("save") != null) {
            NcwmsDataset dataset = catalogue.getConfig().getDatasetInfo(
                    request.getParameter("dataset.id"));
            Set<String> variableIds = new HashSet<String>();
            /*
             * We suppress this warning, because request.getParameterNames()
             * returns an Enumeration of Strings, but its return signature is
             * just an Enumeration
             */
            @SuppressWarnings("unchecked")
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String[] parts = parameterName.split("\\.");
                if (parts.length == 2) {
                    if (!parts[0].equals("dataset")) {
                        variableIds.add(parts[0]);
                    }
                } else {
                    continue;
                }
            }

            for (String variableId : variableIds) {
                String newTitle = request.getParameter(variableId + ".title").trim();
                /* Find the min and max colour scale range for this variable */
                float min = Float.parseFloat(request.getParameter(variableId + ".scaleMin").trim());
                float max = Float.parseFloat(request.getParameter(variableId + ".scaleMax").trim());

                /*
                 * Get the NcwmsVariable for this layer, and save the changes
                 */
                NcwmsVariable var = dataset.getVariablesById(variableId);
                var.setTitle(newTitle);
                var.setColorScaleRange(Extents.newExtent(min, max));
                var.setPaletteName(request.getParameter(variableId + ".palette"));
                var.setNumColorBands(Integer.parseInt(request.getParameter(variableId
                        + ".numColorBands")));
                var.setScaling(request.getParameter(variableId + ".scaling"));
            }
            /* Saves the new configuration information to disk */
            try {
                catalogue.getConfig().save();
            } catch (JAXBException e) {
                log.error("Problem serialising config", e);
            } catch (IOException e) {
                log.error("Problem writing config", e);
            }
        }

        /*
         * This causes a client-side redirect, meaning that the user can safely
         * press refresh in their browser without resubmitting the new config
         * information.
         */
        try {
            response.sendRedirect("");
        } catch (IOException e) {
            /*
             * This error isn't really important
             */
            log.error("Problem redirecting user after config update");
        }
    }

}
