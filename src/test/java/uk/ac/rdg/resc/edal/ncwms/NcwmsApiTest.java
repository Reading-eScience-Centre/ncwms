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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsContact;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsServerInfo;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsSupportedCrsCodes;
import uk.ac.rdg.resc.edal.util.Extents;

public class NcwmsApiTest {

    private NcwmsApiServlet servlet;
    private DatasetConfig dataset;
    private NcwmsConfig config;

    @Before
    public void setUp() throws IOException, Exception {
        servlet = new NcwmsApiServlet();

        // Required for tests, from NcwmsConfigTest.java
        VariableConfig[] variables = new VariableConfig[] { new VariableConfig("varId", "A Variable", "A data-related quantity",
                Extents.newExtent(-10f, 10f), "redblue", null, null, null, "linear", 250) };

        dataset = new DatasetConfig(variables);
        dataset.setId("datasetId");
        dataset.setTitle("A Dataset");
        dataset.setLocation("/home/guy/Data/FOAM_ONE/FOAM_one.ncml");
        dataset.setCopyrightStatement("copyright message");
        dataset.setDisabled(false);
        dataset.setDownloadable(true);
        dataset.setMetadataDesc("this is metadata");
        dataset.setMetadataMimetype("text/xml");
        dataset.setMetadataUrl("http://www.google.com");
        dataset.setMoreInfo("more info");
        dataset.setQueryable(true);
        dataset.setLastSuccessfulUpdateTime(DateTime.parse("2012-01-01"));
        DatasetConfig[] datasets = new DatasetConfig[] { dataset };

        NcwmsContact contact = new NcwmsContact("Guy", "ReSC", "5217", "g.g");

        NcwmsServerInfo serverInfo = new NcwmsServerInfo("servername", true, 100, 50,
                "a fake server", Arrays.asList("fake", "con", "front"), "http://google.com",
                true);
        CacheInfo cacheInfo = new CacheInfo(true, 2000, 10.0f);
        String[] codes = {"CRS:187", "EPSG:187"};
        NcwmsSupportedCrsCodes crsCodes = new NcwmsSupportedCrsCodes(codes);

        config = new NcwmsConfig(datasets, new NcwmsDynamicService[0], contact, serverInfo, cacheInfo, crsCodes);
    }

    @Test
    public void testDecodeState() throws Exception {
        dataset.setState(DatasetConfig.DatasetState.NEEDS_REFRESH);
        assertTrue(servlet.decodeState(dataset).equals("NEEDS_REFRESH"));
        dataset.setState(DatasetConfig.DatasetState.READY);
        assertTrue(servlet.decodeState(dataset).equals("READY"));
        dataset.setState(DatasetConfig.DatasetState.LOADING);
        assertTrue(servlet.decodeState(dataset).equals("LOADING"));
        dataset.setState(DatasetConfig.DatasetState.UPDATING);
        assertTrue(servlet.decodeState(dataset).equals("UPDATING"));
        dataset.setState(DatasetConfig.DatasetState.ERROR);
        assertTrue(servlet.decodeState(dataset).equals("ERROR"));
    }

    @Test
    public void testJsonApiReponse() throws Exception {
        // Prepare mocked + spied classes
        HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockedResponse = mock(HttpServletResponse.class);
        NcwmsCatalogue mockedNcwmsCatalogue = mock(NcwmsCatalogue.class);

        // Prepare requests
        JSONObject datasetInfo = new JSONObject();
        datasetInfo.put("id", "datasetId");
        datasetInfo.put("title", "A Dataset");
        datasetInfo.put("lastUpdate", DateTime.parse("2012-01-01"));
        datasetInfo.put("status", servlet.decodeState(dataset));
        JSONArray variables = new JSONArray();
        for (VariableConfig variable : dataset.getVariables()){
            JSONObject var = new JSONObject();
            var.put("id", variable.getId());
            var.put("title", "A Variable");
            variables.put(var);
        }
        datasetInfo.put("variables", variables);
        servlet.setCatalogue(mockedNcwmsCatalogue);

        when(mockedRequest.getParameter("id")).thenReturn("datasetId");
        when(mockedNcwmsCatalogue.getConfig()).thenReturn(config);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(mockedResponse.getWriter()).thenReturn(pw);

        servlet.doGet(mockedRequest, mockedResponse);

        String result = sw.getBuffer().toString().trim();
        assertTrue(result.equals(datasetInfo.toString()));
    }
}
