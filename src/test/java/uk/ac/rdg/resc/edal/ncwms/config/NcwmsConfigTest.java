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

package uk.ac.rdg.resc.edal.ncwms.config;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.xml.sax.SAXException;

import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.util.Extents;

public class NcwmsConfigTest {

    private NcwmsConfig config;

    @Before
    public void setUp() throws Exception {
        VariableConfig[] variables = new VariableConfig[] { new VariableConfig("varId", "A Variable", "A data-related quantity",
                Extents.newExtent(-10f, 10f), "redblue", null, null, null, "linear", 250) };
        
        DatasetConfig dataset = new DatasetConfig(variables);
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
        DatasetConfig[] datasets = new DatasetConfig[] { dataset };
        
        NcwmsContact contact = new NcwmsContact("Guy", "ReSC", "5217", "g.g");
        
        NcwmsServerInfo serverInfo = new NcwmsServerInfo("servername", true, 100, 50,
                "a fake server", Arrays.asList("fake", "con", "front"), "http://google.com",
                true);
        CacheInfo cacheInfo = new CacheInfo(true, 2000, 10.0f);
        config = new NcwmsConfig(datasets, new NcwmsDynamicService[0], contact, serverInfo, cacheInfo);
    }

//    @Test
    public void testSerialise() throws JAXBException {
        StringWriter serialiseWriter = new StringWriter();
        config.serialise(serialiseWriter);
        String serialise = serialiseWriter.toString(); 
        System.out.println(serialise);
    }

//    @Test
    public void testDeserialise() throws JAXBException, SAXException, FileNotFoundException {
        NcwmsConfig deserialise = NcwmsConfig.deserialise(new StringReader(XML));
//        NcwmsConfig deserialise = NcwmsConfig.deserialise(new FileReader(new File(
//                "/home/guy/.ncWMS-edal/config.xml")));
        System.out.println(deserialise);
    }
    
    private final static String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<config><contact>"
            + "<name>Guy</name>"
            + "<organization>ReSC</organization>"
            + "<telephone>5217</telephone>"
            + "<email>g.g</email>"
            + "</contact>"
            + "<server>"
            + "<title>servername</title>"
            + "<allowFeatureInfo>true</allowFeatureInfo>"
            + "<maxImageWidth>100</maxImageWidth>"
            + "<maxImageHeight>50</maxImageHeight>"
//            + "<abstract>a fake server</abstract>"
//            + "<keywords>fake, con, front</keywords>"
            + "<url>http://google.com</url>"
            + "<adminpassword>ncWMS</adminpassword>"
            + "<allowglobalcapabilities>true</allowglobalcapabilities>" + "</server>" + "</config>";

}
