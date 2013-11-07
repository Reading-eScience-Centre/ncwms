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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.implement.EscapeHtmlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;
import uk.ac.rdg.resc.edal.graphics.formats.ImageFormat;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.util.GISUtils;

/**
 * The main entry point of ncWMS. This deals with loading the configuration
 * object and storing it in the ServletContext so that other servlets can use
 * it.
 * 
 * It also deals with any requests which are not specific to WMS or
 * administration. This is any front-page requests as well as any other
 * information we may want to expose to non-admin users.
 * 
 * @author Guy Griffiths
 */
public class NcwmsApplicationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NcwmsApplicationServlet.class);

    private final VelocityEngine velocityEngine;
    private NcwmsCatalogue catalogue;

    public NcwmsApplicationServlet() {
        super();
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine = new VelocityEngine();
        velocityEngine.init(props);
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        NcwmsConfig config;
        /*
         * Set the default dataset factory - will be used when a dataset factory
         * name is not specified
         */
        DatasetFactory.setDefaultDatasetFactoryClass(CdmGridDatasetFactory.class);

        /*
         * Load the XML config for ncWMS, or create it if it doesn't yet exist.
         */
        Properties appProperties = new Properties();

        String configDir = null;
        String homeDir = System.getProperty("user.home");
        try {
            /*
             * See if we have a properties file which defines a configDir,
             * replacing $HOME with the actual home directory
             */
            appProperties.load(getClass().getResourceAsStream("/config.properties"));
            configDir = appProperties.getProperty("configDir");
            configDir = configDir.replaceAll("\\$HOME", homeDir);
        } catch (Exception e) {
            configDir = null;
            e.printStackTrace();
        }

        /*
         * If we didn't define a config directory, use the user's home as a
         * default
         */
        if (configDir == null) {
            configDir = homeDir + File.separator + ".ncWMS-edal";
        }

        File configFile = new File(configDir + File.separator, "config.xml");
        try {
            if (configFile.exists()) {
                config = NcwmsConfig.readFromFile(configFile);
            } else {
                config = new NcwmsConfig(configFile);
            }

        } catch (JAXBException e) {
            log.error("Config file is invalid - creating new one", e);
            try {
                config = new NcwmsConfig(configFile);
            } catch (Exception e1) {
                throw new ServletException("Old config is invalid, and a new one cannot be created", e1);
            }
        } catch (FileNotFoundException e) {
            /*
             * We shouldn't get here. It means that we've checked that a config
             * file exists and then the FileReader has thrown a
             * FileNotFoundException
             */
            log.error(
                    "Cannot find config file - has it been deleted during startup?  Creating new one",
                    e);
            try {
                config = new NcwmsConfig(configFile);
            } catch (Exception e1) {
                throw new ServletException("Old config is missing, and a new one cannot be created", e1);
            }
        } catch (IOException e) {
            log.error("Problem writing new config file", e);
            throw new ServletException("Cannot create a new config file", e);
        }
        try {
            catalogue = new NcwmsCatalogue(config);
        } catch (IOException e) {
            log.error("Problem loading datasets", e);
        }

        /*
         * Store the config in the ServletContext, so that the other servlets
         * can access it. All other servlets are loaded after this one.
         */
        servletConfig.getServletContext().setAttribute("NcwmsCatalogue", catalogue);
    }

    /**
     * This method is requesting the front page since that is all this servlet
     * does (apart from config initialisation and sharing). Return it here.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException {
        /* HTTP 1.1 */
        response.setHeader("Cache-Control", "no-cache");
        /* HTTP 1.0 */
        response.setHeader("Pragma", "no-cache");
        /* Prevents caching at the proxy server */
        response.setDateHeader("Expires", 0);
        /*
         * Just return the front page. If we want some more (dynamic) web pages
         * available here, we need to do some extra handling of what the URL
         * actually says
         */
        Template template = velocityEngine.getTemplate("templates/index.vm");
        VelocityContext context = new VelocityContext();
        EventCartridge ec = new EventCartridge();
        ec.addEventHandler(new EscapeHtmlReference());
        ec.attachToContext(context);

        context.put("catalogue", catalogue);
        context.put("config", catalogue.getConfig());
        context.put("GISUtils", GISUtils.class);
        context.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
        template.merge(context, response.getWriter());
    }
}
