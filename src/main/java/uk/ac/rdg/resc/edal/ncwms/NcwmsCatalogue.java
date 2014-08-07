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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig.DatasetStorage;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDataset;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsVariable;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.WmsLayerMetadata;
import uk.ac.rdg.resc.edal.wms.exceptions.EdalLayerNotFoundException;
import uk.ac.rdg.resc.edal.wms.util.ContactInfo;
import uk.ac.rdg.resc.edal.wms.util.ServerInfo;

public class NcwmsCatalogue extends WmsCatalogue implements DatasetStorage {

    protected NcwmsConfig config;
    protected Map<String, Dataset> datasets;
    protected Map<String, WmsLayerMetadata> layerMetadata;

    private DateTime lastUpdateTime = new DateTime();

    public NcwmsCatalogue(NcwmsConfig config) throws IOException {
        /*
         * Initialise the storage for datasets and layer metadata.
         */
        datasets = new HashMap<String, Dataset>();
        layerMetadata = new HashMap<String, WmsLayerMetadata>();

        this.config = config;
        this.config.setDatasetLoadedHandler(this);
        this.config.loadDatasets();
    }

    /**
     * @return The NcwmsConfig object used by this catalogue. Package-private
     *         since this should not be accessed by external users
     */
    public NcwmsConfig getConfig() {
        return config;
    }

    /**
     * Removes a dataset from the catalogue. This will also delete any config
     * information about the dataset from the config file.
     * 
     * @param id
     *            The ID of the dataset to remove
     */
    public void removeDataset(String id) {
        datasets.remove(id);
        config.removeDataset(config.getDatasetInfo(id));
    }

    /**
     * Changes a dataset's ID. This will also change the name in the saved
     * config file.
     * 
     * @param oldId
     *            The old ID
     * @param newId
     *            The new ID
     */
    public void changeDatasetId(String oldId, String newId) {
        Dataset dataset = datasets.get(oldId);
        datasets.remove(oldId);
        datasets.put(newId, dataset);
        config.changeDatasetId(config.getDatasetInfo(oldId), newId);
    }

    @Override
    public synchronized void datasetLoaded(Dataset dataset, Collection<NcwmsVariable> variables) {
        /*
         * If we already have a dataset with this ID, it will be replaced. This
         * is exactly what we want.
         */
        datasets.put(dataset.getId(), dataset);

        /*
         * Now add the layer metadata to a map for future reference
         */
        for (NcwmsVariable ncwmsVariable : variables) {
            String layerName = getLayerName(ncwmsVariable.getNcwmsDataset().getId(),
                    ncwmsVariable.getId());
            layerMetadata.put(layerName, ncwmsVariable);
        }
        lastUpdateTime = new DateTime();

        /*
         * The config has changed, so we save it.
         */
        try {
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ServerInfo getServerInfo() {
        return config.getServerInfo();
    }

    @Override
    public ContactInfo getContactInfo() {
        return config.getContactInfo();
    }

    @Override
    public boolean allowsGlobalCapabilities() {
        return config.getServerInfo().allowsGlobalCapabilities();
    }

    @Override
    public DateTime getServerLastUpdate() {
        return lastUpdateTime;
    }

    @Override
    public Collection<Dataset> getAllDatasets() {
        /*
         * This catalogue stores all possible datasets, but this method must
         * only return those which are available (i.e. not disabled and ready to
         * go)
         */
        List<Dataset> allDatasets = new ArrayList<Dataset>();
        for (Dataset dataset : datasets.values()) {
            NcwmsDataset datasetInfo = config.getDatasetInfo(dataset.getId());
            if (!datasetInfo.isDisabled() && datasetInfo.isReady()) {
                allDatasets.add(dataset);
            }
        }
        return allDatasets;
    }

    @Override
    public String getDatasetTitle(String datasetId) {
        NcwmsDataset datasetInfo = config.getDatasetInfo(datasetId);
        if (datasetInfo == null) {
            return "";
        }
        return datasetInfo.getTitle();
    }

    @Override
    public Dataset getDatasetFromId(String datasetId) {
        if (datasets.containsKey(datasetId)) {
            return datasets.get(datasetId);
        } else {
            /*
             * Check to see if we have a dynamic service defined which this dataset ID can map to
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(datasetId);
            if (dynamicService == null || dynamicService.isDisabled()) {
                return null;
            }
            String datasetPath = datasetId.substring(dynamicService.getAlias().length());

            /*
             * Check if we allow this path or if it is disallowed by the dynamic
             * dataset regex
             */
            if (!dynamicService.getIdMatchPattern().matcher(datasetPath).matches()) {
                return null;
            }

            String datasetUrl = dynamicService.getServicePath() + datasetPath;

            String title = datasetId;
            while (title.startsWith("/") && title.length() > 0)
                title = title.substring(1);

            try {
                DatasetFactory datasetFactory = DatasetFactory.forName(dynamicService
                        .getDataReaderClass());
                Dataset dynamicDataset = datasetFactory.createDataset("dynamic", datasetUrl);
                return dynamicDataset;
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                    | IOException | EdalException e) {
                /*
                 * TODO log error
                 */
                return null;
            }

        }
    }

    @Override
    public Dataset getDatasetFromLayerName(String layerName) throws EdalLayerNotFoundException {
        int finalSlashIndex = layerName.lastIndexOf("/");
        if (finalSlashIndex < 0) {
            throw new EdalLayerNotFoundException(
                    "The WMS layer name is malformed.  It should be of the form \"dataset/variable\"");
        }
        String datasetId = layerName.substring(0, finalSlashIndex);
        Dataset dataset = getDatasetFromId(datasetId);
        if (dataset == null) {
            throw new EdalLayerNotFoundException("The dataset given in the layer name " + layerName
                    + " does not exist");
        }
        return dataset;
    }

    @Override
    public String getVariableFromId(String layerName) throws EdalLayerNotFoundException {
        int finalSlashIndex = layerName.lastIndexOf("/");
        if (finalSlashIndex < 0) {
            throw new EdalLayerNotFoundException(
                    "The WMS layer name is malformed.  It should be of the form \"dataset/variable\"");
        }
        return layerName.substring(finalSlashIndex+1);
    }

    @Override
    public String getLayerName(String datasetId, String variableId) {
        return datasetId + "/" + variableId;
    }

    @Override
    public WmsLayerMetadata getLayerMetadata(final String layerName)
            throws EdalLayerNotFoundException {
        if (layerMetadata.containsKey(layerName)) {
            return layerMetadata.get(layerName);
        } else {
            /*
             * We don't have any stored metadata, but there may be a dynamic
             * dataset.
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService == null) {
                throw new EdalLayerNotFoundException("The layer: " + layerName + " doesn't exist");
            }
            /*
             * We have a dynamic dataset. Return sensible defaults
             */
            NcwmsVariable metadata = new NcwmsVariable(layerName.substring(layerName
                    .lastIndexOf("/")), null, ColourPalette.DEFAULT_PALETTE_NAME, null, null,
                    new Color(0, true), "linear", 250);
            return metadata;
        }
    }

    private NcwmsDynamicService getDynamicServiceFromLayerName(String layerName) {
        NcwmsDynamicService dynamicService = null;
        for (NcwmsDynamicService testDynamicService : config.getDynamicServices()) {
            if (layerName.startsWith(testDynamicService.getAlias())) {
                dynamicService = testDynamicService;
            }
        }
        return dynamicService;
    }
}
