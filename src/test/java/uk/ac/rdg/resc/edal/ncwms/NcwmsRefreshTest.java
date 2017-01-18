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

import java.util.Arrays;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.Before;

import uk.ac.rdg.resc.edal.ncwms.config.*;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.util.Extents;


public class NcwmsRefreshTest {

    private NcwmsRefreshServlet servlet;
    private DatasetConfig dataset;
    private NcwmsConfig config;

    @Before
    public void setUp() throws IOException, Exception {
        servlet = new NcwmsRefreshServlet();

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
    public void test() throws Exception {
        HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockedResponse = mock(HttpServletResponse.class);
        NcwmsCatalogue spyNcwmsCatalogue = spy(NcwmsCatalogue.class);

        when(mockedRequest.getParameter("id")).thenReturn("datasetId");
        when(spyNcwmsCatalogue.getConfig()).thenReturn(config);
        servlet.setCatalogue(spyNcwmsCatalogue);
        servlet.doGet(mockedRequest, mockedResponse);

        assertTrue(dataset.needsRefresh());
    }
}
