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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.wms.util.WmsUtils;

/**
 * Deals purely with the (de)serialisation of the ncWMS config file. All objects
 * referred to as "datasets" or "variables" within this are <i>not</i> instances
 * of the EDAL types, but simply the configuration information needed to
 * generate them.
 * 
 * @author Guy Griffiths
 */
@XmlType(name = "config", propOrder = { "datasets", "contact", "serverInfo", "cacheInfo" })
@XmlRootElement(name = "config")
public class NcwmsConfig {
//    private static final Logger log = LoggerFactory.getLogger(NcwmsConfig.class);

    /* Included in XML - see setDatasets for details */
    private Map<String, NcwmsDataset> datasets;
    @XmlElement(name = "contact")
    private NcwmsContact contact = new NcwmsContact();
    @XmlElement(name = "server")
    private NcwmsServerInfo serverInfo = new NcwmsServerInfo();
    @XmlElement(name = "cache")
    private NcwmsCacheInfo cacheInfo = new NcwmsCacheInfo();
    @XmlTransient
    private DatasetStorage datasetStorage = null;
    @XmlTransient
    private File configFile;
    @XmlTransient
    private File configBackup;

    /** The scheduler that will handle the background (re)loading of datasets */
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    /**
     * Contains handles to background threads that can be used to cancel
     * reloading of datasets. Maps dataset IDs to Future objects
     */
    private static Map<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();

    /*
     * Used for JAX-B
     */
    @SuppressWarnings("unused")
    private NcwmsConfig() {
    }
    
    public NcwmsConfig(File configFile) throws IOException, JAXBException {
        datasets = new LinkedHashMap<String, NcwmsDataset>();
        this.configFile = configFile;
        save();
    }

    public NcwmsConfig(NcwmsDataset[] datasets, NcwmsContact contact, NcwmsServerInfo serverInfo,
            NcwmsCacheInfo cacheInfo) {
        super();
        setDatasets(datasets);
        this.contact = contact;
        this.serverInfo = serverInfo;
        this.cacheInfo = cacheInfo;
    }

    public void setDatasetLoadedHandler(DatasetStorage datasetStorage) {
        this.datasetStorage = datasetStorage;
    }

    public void loadDatasets() {
        /*
         * Loop through all NcwmsDatasets and load Datasets from each.
         * 
         * Do this in a manner which means that they are "reloaded" every second
         * (and checked as to whether they need to actually have anything done
         * to them)
         * 
         * Also during the load, return WmsLayerMetadatas (these are just the
         * NcwmsVariables...)
         */
        for (final NcwmsDataset dataset : datasets.values()) {
            scheduleReload(dataset);
        }
    }

    private void scheduleReload(final NcwmsDataset dataset) {
        if (datasetStorage == null) {
            throw new IllegalStateException(
                    "You need to set something to handle loaded datasets before loading them.");
        }
        Runnable reloader = new Runnable() {
            @Override
            public void run() {
                /*
                 * This will check to see if the metadata need reloading, then
                 * go ahead if so.
                 */
                dataset.refresh(datasetStorage);
            }
        };
        /*
         * Run the task immediately, and then redo it every 1s
         */
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(reloader, 0, 1,
                TimeUnit.SECONDS);
        /* We need to keep a handle to the Future object so we can cancel it */
        futures.put(dataset.getId(), future);
    }

    public NcwmsContact getContactInfo() {
        return contact;
    }

    public NcwmsServerInfo getServerInfo() {
        return serverInfo;
    }

    public NcwmsCacheInfo getCacheSettings() {
        return cacheInfo;
    }

    public NcwmsDataset getDatasetInfo(String datasetId) {
        return datasets.get(datasetId);
    }

    /*
     * By making getDatasets() and setDatasets() both deal with arrays of
     * NcwmsDataset, JAXB is able to instantiate them. If we used Collections
     * instead this would not work.
     * 
     * Both methods are private, so the fact that that the result of getDatasets
     * isn't backed by the Map is not a problem - it is only used by JAXB which
     * only uses it when persisting the config to disk anyway.
     */
    public NcwmsDataset[] getDatasets() {
        return datasets.values().toArray(new NcwmsDataset[0]);
    }

    @XmlElementWrapper(name = "datasets")
    @XmlElement(name = "dataset")
    public void setDatasets(NcwmsDataset[] datasets) {
        this.datasets = new LinkedHashMap<String, NcwmsDataset>();
        for (NcwmsDataset dataset : datasets) {
            this.datasets.put(dataset.getId(), dataset);
        }
    }

    public synchronized void addDataset(NcwmsDataset dataset) {
        datasets.put(dataset.getId(), dataset);
        scheduleReload(dataset);
    }

    public synchronized void removeDataset(NcwmsDataset dataset) {
        datasets.remove(dataset.getId());
        futures.get(dataset.getId()).cancel(true);
        futures.remove(dataset.getId());
    }

    public synchronized void changeDatasetId(NcwmsDataset dataset, String newId) {
        datasets.remove(dataset.getId());
        ScheduledFuture<?> removedScheduler = futures.remove(dataset.getId());
        dataset.setId(newId);

        datasets.put(newId, dataset);
        futures.put(dataset.getId(), removedScheduler);
    }

    public synchronized void save() throws IOException, JAXBException {
        if (configFile == null) {
            throw new IllegalStateException("No location set for config file");
        }
        /* Take a backup of the existing config file */
        if (configBackup == null) {
            String backupName = configFile.getAbsolutePath() + ".backup";
            configBackup = new File(backupName);
        }
        /* Copy current config file to the backup file. */
        if (configFile.exists()) {
            /* Delete existing backup */
            configBackup.delete();
            WmsUtils.copyFile(configFile, configBackup);
        }

        serialise(this, new FileWriter(configFile));
    }

    public static void shutdown() {
        scheduler.shutdownNow();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Datasets\n");
        sb.append("--------\n");
        for (NcwmsDataset dataset : datasets.values()) {
            sb.append(dataset.toString());
            sb.append("\n");
        }
        sb.append("\n");
        sb.append("Contact Info\n");
        sb.append("------------\n");
        sb.append(contact.toString());
        sb.append("\n\n");
        sb.append("Server Info\n");
        sb.append("-----------\n");
        sb.append(serverInfo.toString());
        sb.append("\n\n");
        sb.append("Cache Info\n");
        sb.append("----------\n");
        sb.append(cacheInfo.toString());
        return sb.toString();
    }

    public interface DatasetStorage {
        public void datasetLoaded(Dataset dataset, Collection<NcwmsVariable> variables);
    }

    public static void serialise(NcwmsConfig config, Writer writer) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(NcwmsConfig.class);

        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        marshaller.marshal(config, writer);
    }

    public static NcwmsConfig deserialise(Reader xmlConfig) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(NcwmsConfig.class);

        Unmarshaller unmarshaller = context.createUnmarshaller();
//        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File("/home/guy/Workspace/edal-java/ncwms/src/main/resources/schemas/schema1.xsd"));
//        unmarshaller.setSchema(schema);
        NcwmsConfig config = (NcwmsConfig) unmarshaller.unmarshal(xmlConfig);

        return config;
    }

    public static void generateSchema(final String path) throws IOException, JAXBException {
        JAXBContext context = JAXBContext.newInstance(NcwmsConfig.class);
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName)
                    throws IOException {
                return new StreamResult(new File(path, suggestedFileName));
            }
        });
    }

    public static NcwmsConfig readFromFile(File configFile) throws FileNotFoundException,
            JAXBException {
        NcwmsConfig config = deserialise(new FileReader(configFile));
        config.configFile = configFile;
        return config;
    }

//    public static void main(String[] args) throws IOException, JAXBException {
//        generateSchema("/home/guy/Workspace/edal-java/ncwms/src/main/resources/schemas/");
//    }
}
