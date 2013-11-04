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
import java.io.FileReader;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.wms.WmsServlet;

/**
 * Servlet implementation class NcWmsServlet
 */
public class NcWmsServlet extends WmsServlet implements Servlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see WmsServlet#WmsServlet()
     */
    public NcWmsServlet() {
        super();
        /*
         * TODO in the real world:
         * Get Javadoc uploading somewhere
         * Cookbook documentation
         */
    }

    @Override
    public void destroy() {
        super.destroy();
        NcwmsConfig.shutdown();
    }
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        try {
            /*
             * Set the default dataset factory - will be used when a dataset
             * factory name is not specified
             */
            DatasetFactory.setDefaultDatasetFactoryClass(CdmGridDatasetFactory.class);

            /*
             * Load the XML config for ncWMS, or create it if it doesn't yet
             * exist.
             */
            /*
             * TODO Perhaps this could be configurable from a properties file in
             * the webapp somewhere?
             */
            String homeDir = System.getProperty("user.home");
            File configFile = new File(homeDir + File.separator + ".ncWMS-edal" + File.separator,
                    "config.xml");
            NcwmsConfig config;
            if (configFile.exists()) {
                config = NcwmsConfig.deserialise(new FileReader(configFile));
            } else {
                config = new NcwmsConfig();
            }

            /*
             * Create a new catalogue from this configuration. The catalogue
             * will then perform all the steps needed to load the datasets into
             * memory etc.
             * 
             * We will then have a working WMS server up-and-running.
             */
            NcwmsCatalogue ncwmsCatalogue = new NcwmsCatalogue(config);
            setCatalogue(ncwmsCatalogue);
        } catch (IOException e) {
            /*
             * TODO Deal with these exceptions
             */
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
        }
    }

    /*-
     * Test URL for this servlet.
     * http://localhost:8080/ncWMS/wms?REQUEST=GetMap&VERSION=1.3.0&FORMAT=image/png&CRS=CRS:84&BBOX=-180,-90,180,90&WIDTH=1024&HEIGHT=512&LAYERS=foam/TMP&STYLES=boxfill/alg&COLORSCALERANGE=265,305&TIME=2010-01-30T12:00:00.000Z&ELEVATION=5&NUMCOLORBANDS=50
     */
}
