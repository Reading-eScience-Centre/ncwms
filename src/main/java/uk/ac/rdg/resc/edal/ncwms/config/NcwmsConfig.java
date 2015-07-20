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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.catalogue.jaxb.CacheInfo;
import uk.ac.rdg.resc.edal.catalogue.jaxb.CatalogueConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;

/**
 * Deals purely with the (de)serialisation of the ncWMS config file. This
 * extends the basic XML config to add contact info and dynamically defined
 * services
 * 
 * @author Guy Griffiths
 */
@XmlType(propOrder = { "dynamicServices", "contact", "serverInfo" })
@XmlRootElement(name = "config")
public class NcwmsConfig extends CatalogueConfig {
    private static final Logger log = LoggerFactory.getLogger(NcwmsConfig.class);

    /* Included in XML - see setDynamicServices for details */
    private Map<String, NcwmsDynamicService> dynamicServices = new LinkedHashMap<String, NcwmsDynamicService>();

    @XmlElement(name = "contact")
    private NcwmsContact contact = new NcwmsContact();
    @XmlElement(name = "server")
    private NcwmsServerInfo serverInfo = new NcwmsServerInfo();

    /*
     * Used for JAX-B
     */
    @SuppressWarnings("unused")
    private NcwmsConfig() {
    }

    public NcwmsConfig(File configFile) throws IOException, JAXBException {
        super(configFile);
        dynamicServices = new LinkedHashMap<String, NcwmsDynamicService>();
    }

    public NcwmsConfig(DatasetConfig[] datasets, NcwmsDynamicService[] dynamicServices,
            NcwmsContact contact, NcwmsServerInfo serverInfo, CacheInfo cacheInfo) {
        super(datasets, cacheInfo);
        setDynamicServices(dynamicServices);
        this.contact = contact;
        this.serverInfo = serverInfo;
    }

    public NcwmsContact getContactInfo() {
        return contact;
    }

    public NcwmsServerInfo getServerInfo() {
        return serverInfo;
    }

    /*
     * By making getDynamicServices() and setDynamicServices() both deal with
     * arrays of NcwmsDynamicService, JAXB is able to instantiate them. If we
     * used Collections instead this would not work.
     */

    /**
     * @return The {@link NcwmsDynamicService}s which have been configured on
     *         this server
     */
    public NcwmsDynamicService[] getDynamicServices() {
        return dynamicServices.values().toArray(new NcwmsDynamicService[0]);
    }

    @XmlElementWrapper(name = "dynamicServices")
    @XmlElement(name = "dynamicService", required = false)
    private void setDynamicServices(NcwmsDynamicService[] dynamicServices) {
        this.dynamicServices = new LinkedHashMap<String, NcwmsDynamicService>();
        for (NcwmsDynamicService dynamicService : dynamicServices) {
            this.dynamicServices.put(dynamicService.getAlias(), dynamicService);
        }
    }

    public synchronized void addDynamicService(NcwmsDynamicService dynamicService) {
        dynamicServices.put(dynamicService.getAlias(), dynamicService);
    }

    public synchronized void removeDynamicService(NcwmsDynamicService dynamicService) {
        dynamicServices.remove(dynamicService.getAlias());
    }

    public synchronized void changeDynamicServiceId(NcwmsDynamicService dynamicService,
            String newAlias) {
        dynamicServices.remove(dynamicService.getAlias());
        dynamicService.setAlias(newAlias);

        dynamicServices.put(newAlias, dynamicService);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Contact Info\n");
        sb.append("------------\n");
        sb.append(contact.toString());
        sb.append("\n\n");
        sb.append("Server Info\n");
        sb.append("-----------\n");
        sb.append(serverInfo.toString());
        sb.append("\n");
        return sb.toString();
    }

    public static NcwmsConfig deserialise(Reader xmlConfig) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(NcwmsConfig.class);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        Source source = new StreamSource(xmlConfig);
        NcwmsConfig config = (NcwmsConfig) unmarshaller.unmarshal(source, NcwmsConfig.class)
                .getValue();

        return config;
    }

    public static NcwmsConfig readFromFile(File configFile) throws JAXBException, IOException {
        NcwmsConfig config;
        if (!configFile.exists()) {
            /*
             * If the file doesn't exist, create it with some default values
             */
            log.warn("No config file exists in the given location (" + configFile.getAbsolutePath()
                    + ").  Creating one with defaults");
            config = new NcwmsConfig(new DatasetConfig[0], new NcwmsDynamicService[0],
                    new NcwmsContact(), new NcwmsServerInfo(), new CacheInfo());
            config.configFile = configFile;
            config.save();
        } else {
            /*
             * Otherwise read the file
             */
            config = deserialise(new FileReader(configFile));
            config.configFile = configFile;
        }
        return config;
    }
}
