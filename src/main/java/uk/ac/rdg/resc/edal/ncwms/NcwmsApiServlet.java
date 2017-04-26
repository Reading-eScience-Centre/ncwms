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
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;

/*
 * An {@link HttpServlet} which provides information about datasets from
 * DatasetConfig instance via a REST API.
 *
 * @author Jesse Lopez
 */
public class NcwmsApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private NcwmsCatalogue catalogue;

    public NcwmsApiServlet() throws IOException, Exception {
        super();
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        /*
         * Get the pre-loaded catalogue
         */
        Object config = servletConfig.getServletContext().getAttribute(NcwmsApplicationServlet.CONTEXT_NCWMS_CATALOGUE);
        if (config instanceof NcwmsCatalogue) {
            catalogue = (NcwmsCatalogue) config;
        } else {
            throw new ServletException("ncWMS configuration object is incorrect type.  The \""
                    + NcwmsApplicationServlet.CONTEXT_NCWMS_CATALOGUE
                    + "\" attribute of the ServletContext has been incorrectly set.");
        }
    }

    protected void setCatalogue(NcwmsCatalogue catalogue){
       this.catalogue = catalogue;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * Parameter NAMES not case sensitive, but parameter VALUES are
         */
        String datasetId = request.getParameter("id");
        NcwmsConfig ncwmsConfig = catalogue.getConfig();
        DatasetConfig dataset = ncwmsConfig.getDatasetInfo(datasetId);

        /*
         * Build JSON object for response
         */
        JSONObject datasetInfo = new JSONObject();
        datasetInfo.put("id", dataset.getId());
        datasetInfo.put("title", dataset.getTitle());
        datasetInfo.put("lastUpdate", dataset.getLastUpdateTime());
        datasetInfo.put("status", decodeState(dataset));
        JSONArray variables = new JSONArray();
        for (VariableConfig variable : dataset.getVariables()){
            JSONObject var = new JSONObject();
            var.put("id", variable.getId());
            var.put("title", variable.getTitle());
            variables.put(var);
        }
        datasetInfo.put("variables", variables);

        /*
         * Write out JSON object
         */
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(datasetInfo.toString());
    }

    protected String decodeState(DatasetConfig dataset) {
        String out;
        DatasetConfig.DatasetState state = dataset.getState();
        if (state == DatasetConfig.DatasetState.NEEDS_REFRESH) {
            out = "NEEDS_REFRESH";
        } else if (state == DatasetConfig.DatasetState.READY) {
            out = "READY";
        } else if (state == DatasetConfig.DatasetState.LOADING) {
            out = "LOADING";
        } else if (state == DatasetConfig.DatasetState.UPDATING) {
            out = "UPDATING";
        } else {
            out = "ERROR";
        }

        return out;
    }
}
