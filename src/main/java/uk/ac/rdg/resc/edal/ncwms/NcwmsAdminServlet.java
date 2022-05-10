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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig.DatasetState;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.graphics.utils.ColourPalette;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsContact;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicCacheInfo;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsServerInfo;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.edal.wms.RequestParams;

/**
 * An {@link HttpServlet} which deals with the admin pages of ncWMS -
 * adding/removing datasets, updating contact info, configuring cache etc.
 *
 * @author Guy Griffiths
 * @author Nathan Potter
 */
public class NcwmsAdminServlet extends HttpServlet {
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
        Object config = servletConfig.getServletContext()
                .getAttribute(NcwmsApplicationServlet.CONTEXT_NCWMS_CATALOGUE);
        if (config instanceof NcwmsCatalogue) {
            catalogue = (NcwmsCatalogue) config;
        } else {
            throw new ServletException("ncWMS configuration object is incorrect type.  The \""
                    + NcwmsApplicationServlet.CONTEXT_NCWMS_CATALOGUE
                    + "\" attribute of the ServletContext has been incorrectly set.");
        }

        /*
         * Retrieve the pre-loaded velocity engine
         */
        Object engine = servletConfig.getServletContext()
                .getAttribute(NcwmsApplicationServlet.CONTEXT_VELOCITY_ENGINE);
        if (engine instanceof VelocityEngine) {
            velocityEngine = (VelocityEngine) engine;
        } else {
            throw new ServletException("VelocityEngine object is incorrect type.  The \""
                    + NcwmsApplicationServlet.CONTEXT_VELOCITY_ENGINE
                    + "\" attribute of the ServletContext has been incorrectly set.");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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

        /*
         * Redirect URLs of the form http://hostname/ncWMS2/admin to
         * http://hostname/ncWMS2/admin/
         */
        if (path == null) {
            response.sendRedirect(request.getRequestURI() + "/");
            return;
        }

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

        context.put("version", NcwmsApplicationServlet.getVersion());
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
        DatasetConfig dataset = catalogue.getConfig().getDatasetInfo(datasetId);

        Template template = velocityEngine.getTemplate("templates/dataset_status.vm");
        VelocityContext context = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(context);

        context.put("dataset", dataset);

        PrintWriter writer;
        try {
            writer = response.getWriter();
        } catch (IOException e1) {
            /*
             * This is a problem getting the output writer
             */
            log.error("Problem writing to output stream");
            return;
        }

        /*
         * We don't do a full analysis of the request headers. We just want to
         * know whether text/plain is preferred to text/html.
         */
        String accepts = request.getHeader("Accept");
        boolean useHtml = true;
        double plainWeight = 0.0;
        double htmlWeight = 0.0;
        if (accepts != null) {
            String[] acceptTypes = accepts.split(",");
            for (String acceptType : acceptTypes) {
                String[] atSplit = acceptType.split(";");

                double weight = 1.0;
                if (atSplit.length > 1) {
                    /*
                     * Weighting, should be of the form "q=0.9"
                     */
                    try {
                        weight = Double.parseDouble(atSplit[1].split("=")[1]);
                    } catch (Exception e) {
                        /*
                         * The weighting is not properly defined
                         */
                    }
                }

                String type = atSplit[0];
                if (type.trim().equalsIgnoreCase("text/plain")) {
                    plainWeight = weight;
                } else if (type.trim().equalsIgnoreCase("text/html")) {
                    htmlWeight = weight;
                }
            }
            useHtml = htmlWeight >= plainWeight;
        }

        if (useHtml) {
            response.setContentType("text/html");
            try {
                template.merge(context, writer);
                return;
            } catch (ResourceNotFoundException | ParseErrorException
                    | MethodInvocationException e) {
                /*
                 * These are issues with the templating. Write as plaintext
                 * instead
                 */
            }
        }

        /*-
         * Write as plaintext.  We'll get here if:
         * 
         * 1) We want to use plaintext
         * 2) HTML templating failed
         */
        response.setContentType("text/plain");

        if (dataset != null) {
            writer.write("Dataset: " + dataset.getId() + " (" + dataset.getLocation() + "): "
                    + dataset.getState() + "\n");
            if (dataset.getState() == DatasetState.ERROR) {
                writer.write("\nStack trace:\n");
                Throwable error = dataset.getException();
                StackTraceElement[] stackTrace = error.getStackTrace();
                for (StackTraceElement el : stackTrace) {
                    writer.write("\t" + el.toString() + "\n");
                }
                if (error.getCause() != null) {
                    writer.write("\nCaused by:\n\t" + error.getCause().toString() + "\n");
                }
            }
        } else {
            writer.write("Dataset: "+datasetId+" not found on this server\n");
        }
    }

    private void displayEditVariablesPage(HttpServletRequest request,
            HttpServletResponse response) {
        String datasetId = request.getParameter("dataset");
        if (StringUtils.isBlank(datasetId)) {
            throw new IllegalArgumentException(
                    "Must supply the \"dataset\" parameter to edit variables");
        }
        DatasetConfig dataset = catalogue.getConfig().getDatasetInfo(datasetId);

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
        } else if ("/addDataset".equals(path)) {
            /*
             * Add a new dataset
             */
            addDataset(request, response);
        } else if ("/removeDataset".equals(path)) {
            /*
             * Add a new dataset
             */
            removeDataset(request, response);
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
        CacheInfo cache = catalogue.getConfig().getCacheSettings();
        NcwmsDynamicCacheInfo dynamicCache = catalogue.getConfig().getDynamicCacheInfo();

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
        server.setAllowGlobalCapabilities(
                request.getParameter("server.allowglobalcapabilities") != null);

        /*
         * Save the dataset information, checking for removals First look
         * through the existing datasets for edits.
         */
        List<DatasetConfig> datasetsToRemove = new ArrayList<>();
        /* Keeps track of dataset IDs that have been changed */
        Map<DatasetConfig, String> changedIds = new HashMap<>();
        for (DatasetConfig ds : catalogue.getConfig().getDatasets()) {
            boolean refreshDataset = false;
            if (request.getParameter("dataset." + ds.getId() + ".remove") != null) {
                datasetsToRemove.add(ds);
            } else {
                ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
                String newLocation = request.getParameter("dataset." + ds.getId() + ".location");
                if (!newLocation.trim().equals(ds.getLocation().trim())) {
                    refreshDataset = true;
                }
                ds.setLocation(newLocation);
                String newDataReaderClass = request
                        .getParameter("dataset." + ds.getId() + ".reader");
                if (!newDataReaderClass.trim().equals(ds.getDataReaderClass().trim())) {
                    refreshDataset = true;
                }
                ds.setDataReaderClass(newDataReaderClass);
                boolean disabled = request
                        .getParameter("dataset." + ds.getId() + ".disabled") != null;
                if (disabled == false && ds.isDisabled()) {
                    /* We've re-enabled the dataset so need to reload it */
                    refreshDataset = true;
                }
                ds.setDisabled(disabled);
                ds.setQueryable(
                        request.getParameter("dataset." + ds.getId() + ".queryable") != null);
                ds.setDownloadable(
                        request.getParameter("dataset." + ds.getId() + ".downloadable") != null);
                ds.setUpdateInterval(Integer.parseInt(
                        request.getParameter("dataset." + ds.getId() + ".updateinterval")));
                ds.setMoreInfo(request.getParameter("dataset." + ds.getId() + ".moreinfo"));
                ds.setCopyrightStatement(
                        request.getParameter("dataset." + ds.getId() + ".copyright"));

                ds.setMetadataUrl(request.getParameter("dataset." + ds.getId() + ".metadataUrl"));
                ds.setMetadataDesc(request.getParameter("dataset." + ds.getId() + ".metadataDesc"));
                ds.setMetadataMimetype(
                        request.getParameter("dataset." + ds.getId() + ".metadataMimetype"));

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
        for (DatasetConfig ds : datasetsToRemove) {
            catalogue.removeDataset(ds.getId());
        }
        /* Now we change the ids of the relevant datasets */
        for (DatasetConfig ds : changedIds.keySet()) {
            catalogue.changeDatasetId(ds.getId(), changedIds.get(ds));
            /*
             * Force a refresh of the dataset. We do this in case the new ID
             * happens to be the same as an existing dataset.
             */
            ds.forceRefresh();
        }

        /*
         * Now look for the new datasets. The logic below means that we don't
         * have to know in advance how many new datasets the user has created
         * (or how many spaces were available in the admin page)
         */
        int i = 0;
        while (request.getParameter("dataset.new" + i + ".id") != null) {
            /* Look for non-blank ID fields */
            String id = request.getParameter("dataset.new" + i + ".id");
            if (id != null && !id.trim().equals("")) {
                while (catalogue.getDatasetInfo(id) != null) {
                    /*
                     * We already have a dataset with this ID. Add prime marks
                     * until it's unique.
                     */
                    id += "'";
                }
                DatasetConfig ds = new DatasetConfig();
                ds.setId(id);
                String title = request.getParameter("dataset.new" + i + ".title");
                if (title == null || title.trim().equals("")) {
                    title = id;
                }
                ds.setTitle(title);
                ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
                ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
                ds.setDisabled(request.getParameter("dataset.new" + i + ".disabled") != null);
                ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
                ds.setUpdateInterval(Integer
                        .parseInt(request.getParameter("dataset.new" + i + ".updateinterval")));
                ds.setMoreInfo(request.getParameter("dataset.new" + i + ".moreinfo"));
                ds.setCopyrightStatement(request.getParameter("dataset.new" + i + ".copyright"));
                /*
                 * addDataset() contains code to ensure that the dataset loads
                 * its metadata at the next opportunity
                 */
                catalogue.getConfig().addDataset(ds);
            }
            i++;
        }

        List<NcwmsDynamicService> dynamicServicesToRemove = new ArrayList<NcwmsDynamicService>();
        /*
         * Keeps track of dataset IDs that have been changed
         */
        Map<NcwmsDynamicService, String> changedNcwmsDynamicServiceIds = new HashMap<NcwmsDynamicService, String>();
        for (NcwmsDynamicService ds : catalogue.getConfig().getDynamicServices()) {
            if (request.getParameter("dapService." + ds.getAlias() + ".remove") != null) {
                dynamicServicesToRemove.add(ds);
            } else {
                /*
                 * Check to see if we have updated the ID
                 */
                String newId = request.getParameter("dynamicService." + ds.getAlias() + ".alias")
                        .trim();
                if (!newId.equals(ds.getAlias())) {
                    /*
                     * The ID will be changed later
                     */
                    changedNcwmsDynamicServiceIds.put(ds, newId);
                }
                String servicePath = request
                        .getParameter("dynamicService." + ds.getAlias() + ".servicePath");
                ds.setServicePath(servicePath);

                String matchRegex = request
                        .getParameter("dynamicService." + ds.getAlias() + ".datasetIdMatch");
                ds.setDatasetIdMatch(matchRegex);

                ds.setMoreInfo(
                        request.getParameter("dynamicService." + ds.getAlias() + ".moreinfo"));
                ds.setCopyrightStatement(
                        request.getParameter("dynamicService." + ds.getAlias() + ".copyright"));

                boolean disabled = request
                        .getParameter("dynamicService." + ds.getAlias() + ".disabled") != null;
                ds.setDisabled(disabled);

                boolean queryable = request
                        .getParameter("dynamicService." + ds.getAlias() + ".queryable") != null;
                ds.setQueryable(queryable);
                
                boolean downloadable = request
                        .getParameter("dynamicService." + ds.getAlias() + ".downloadable") != null;
                ds.setDownloadable(downloadable);

                String newDataReaderClass = request
                        .getParameter("dynamicService." + ds.getAlias() + ".reader");
                ds.setDataReaderClass(newDataReaderClass);
            }
        }
        /*
         * Now we can remove the dynamic Services
         */
        for (NcwmsDynamicService ds : dynamicServicesToRemove) {
            catalogue.getConfig().removeDynamicService(ds);
        }
        /*
         * Now we change the ids of the relevant dynamic Services
         */
        for (NcwmsDynamicService ds : changedNcwmsDynamicServiceIds.keySet()) {
            catalogue.getConfig().changeDynamicServiceId(ds, changedNcwmsDynamicServiceIds.get(ds));
        }

        /*
         * Now look for the new dynamic Services. The logic below means that we
         * don't have to know in advance how many new dynamic Services the user
         * has created (or how many spaces were available in the admin page)
         */
        i = 0;
        while (request.getParameter("dynamicService.new" + i + ".alias") != null) {
            /*
             * Look for non-blank alias fields
             */
            if (!request.getParameter("dynamicService.new" + i + ".alias").trim().equals("")) {
                NcwmsDynamicService ds = new NcwmsDynamicService();
                ds.setAlias(request.getParameter("dynamicService.new" + i + ".alias"));
                ds.setServicePath(request.getParameter("dynamicService.new" + i + ".servicePath"));
                ds.setDatasetIdMatch(
                        request.getParameter("dynamicService.new" + i + ".datasetIdMatch"));
                ds.setMoreInfo(request.getParameter("dynamicService.new" + i + ".moreinfo"));
                ds.setCopyrightStatement(
                        request.getParameter("dynamicService.new" + i + ".copyright"));
                ds.setDisabled(
                        request.getParameter("dynamicService.new" + i + ".disabled") != null);
                ds.setQueryable(
                        request.getParameter("dynamicService.new" + i + ".queryable") != null);
                ds.setDataReaderClass(request.getParameter("dynamicService.new" + i + ".reader"));
                catalogue.getConfig().addDynamicService(ds);
            }
            i++;
        }

        /* 
         * Set the properties of the cache
         */
        cache.setEnabled(request.getParameter("cache.enable") != null);
        String tmpMemorySize = request.getParameter("cache.inMemorySizeMB");
        if (!tmpMemorySize.isEmpty()) {
            cache.setInMemorySizeMB(Integer.parseInt(tmpMemorySize));
        }
        String tmpLifetime = request.getParameter("cache.elementLifetimeMinutes");
        if (!tmpLifetime.isEmpty()) {
            cache.setElementLifetimeMinutes(Float.parseFloat(tmpLifetime));
        }

        /*
         * Update the cache settings.
         */
        catalogue.setCache(cache);
        
        /* 
         * Set the properties of the dynamic dataset cache
         */
        dynamicCache.setEnabled(request.getParameter("dynamicCache.enable") != null);
        String nDatasets = request.getParameter("dynamicCache.nDatasets");
        if (!nDatasets.isEmpty()) {
            dynamicCache.setNumberOfDatasets(Integer.parseInt(nDatasets));
        }
        tmpLifetime = request.getParameter("dynamicCache.elementLifetimeMinutes");
        if (!tmpLifetime.isEmpty()) {
            dynamicCache.setElementLifetimeMinutes(Float.parseFloat(tmpLifetime));
        }
        if(request.getParameter("dynamicCache.empty") != null) {
            catalogue.emptyDynamicDatasetCache();
        }
        
        /*
         * Update the dynamic cache settings.
         */
        catalogue.updateDynamicDatasetCache(dynamicCache);

        /* Save the updated config information to disk */
        try {
            catalogue.getConfig().save();
        } catch (IOException e) {
            log.error("Problem writing config", e);
        }

        /*
         * This causes a client-side redirect, meaning that the user can safely
         * press refresh in their browser without resubmitting the new config
         * information.
         */
        try {
            response.sendRedirect("./");
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
            DatasetConfig dataset = catalogue.getConfig()
                    .getDatasetInfo(request.getParameter("dataset.id"));
            Set<String> variableIds = new HashSet<String>();
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
                VariableConfig var = dataset.getVariableById(variableId);
                var.setTitle(newTitle);
                var.setColorScaleRange(Extents.newExtent(min, max));
                var.setPaletteName(request.getParameter(variableId + ".palette"));
                var.setNumColorBands(
                        Integer.parseInt(request.getParameter(variableId + ".numColorBands")));
                var.setScaling(request.getParameter(variableId + ".scaling"));
                var.setDisabled(request.getParameter(variableId + ".disabled") != null);
            }
            /* Saves the new configuration information to disk */
            try {
                catalogue.getConfig().save();
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
            response.sendRedirect("./");
        } catch (IOException e) {
            /*
             * This error isn't really important
             */
            log.error("Problem redirecting user after config update");
        }
    }

    private void addDataset(HttpServletRequest request, HttpServletResponse response) {
        RequestParams params = new RequestParams(request.getParameterMap());
        String id = params.getMandatoryString("id");
        String location = params.getMandatoryString("location");
        String title = params.getString("title", id);
        String dataReader = params.getString("dataReader", "");
        String moreInfo = params.getString("moreInfo", "");
        String copyright = params.getString("copyright", "");
        boolean queryable = params.getBoolean("queryable", true);
        boolean downloadable = params.getBoolean("downloadable", false);
        // -1 means never
        int autoRefreshMinutes = params.getInt("autoRefreshMinutes", -1);

        /*
         * Perform simple sanity checks to see whether this dataset is likely to
         * work
         */
        boolean datasetOK = true;
        String message = null;
        if (catalogue.getDatasetInfo(id) != null) {
            datasetOK = false;
            message = "Dataset with ID " + id + " already exists";
        }
        try {
            if (!dataReader.isEmpty()) {
                Class.forName(dataReader);
            }
        } catch (ClassNotFoundException e) {
            datasetOK = false;
            message = "Data reading class: " + dataReader + " is not available";
        }

        /*
         * Write status as text/plain
         */
        response.setContentType("text/plain");
        try (BufferedWriter w = new BufferedWriter(response.getWriter())) {
            if (datasetOK) {
                /*
                 * Configure the dataset
                 */
                DatasetConfig ds = new DatasetConfig();
                ds.setId(id);
                ds.setTitle(title);
                ds.setLocation(location);
                ds.setDataReaderClass(dataReader);
                ds.setQueryable(queryable);
                ds.setDownloadable(downloadable);
                ds.setUpdateInterval(autoRefreshMinutes);
                ds.setMoreInfo(moreInfo);
                ds.setCopyrightStatement(copyright);
                /*
                 * addDataset() contains code to ensure that the dataset loads
                 * its metadata at the next opportunity
                 */
                catalogue.getConfig().addDataset(ds);

                try {
                    catalogue.getConfig().save();
                } catch (IOException e) {
                    log.error("Problem writing config to file", e);
                }

                w.write("Dataset " + id + " (" + location + ") is being added.\n");

                w.write("Check the status at " + request.getRequestURL().toString()
                        .replaceAll("addDataset", "datasetStatus") + "?dataset=" + id + "\n");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                w.write("Dataset " + id + " (" + location + ") cannot be added.\n");
                w.write("Cause:\n" + message + "\n");
            }
        } catch (IOException e) {
            /*
             * This can occur when creating BufferedWriter
             */
            log.error("Problem writing response to output stream", e);
        }
    }

    private void removeDataset(HttpServletRequest request, HttpServletResponse response) {
        RequestParams params = new RequestParams(request.getParameterMap());
        String id = params.getMandatoryString("id");

        /*
         * This happens synchronously
         */
        catalogue.removeDataset(id);

        boolean saved = false;
        try {
            catalogue.getConfig().save();
            saved = true;
        } catch (IOException e) {
            log.error("Problem writing config", e);
        }

        /*
         * Write status as text/plain
         */
        response.setContentType("text/plain");
        try (BufferedWriter w = new BufferedWriter(response.getWriter())) {
            w.write("Dataset " + id + " has been removed.\n");
            if (!saved) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                w.write("There was a problem saving the config file to disk.\n");
                w.write("This may mean the dataset will reappear on restart.\n");
                w.write("Please check the server logs for details\n");
            }
        } catch (IOException e) {
            /*
             * This can occur when creating BufferedWriter
             */
            log.error("Problem writing response to output stream", e);
        }
    }

}
