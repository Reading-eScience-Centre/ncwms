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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig.DatasetStorage;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDataset;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsVariable;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.WmsLayerMetadata;
import uk.ac.rdg.resc.edal.wms.exceptions.WmsLayerNotFoundException;
import uk.ac.rdg.resc.edal.wms.util.ContactInfo;

public class NcwmsCatalogue extends WmsCatalogue implements DatasetStorage {

    private NcwmsConfig config;
    private Map<String, Dataset> datasets;
    private Map<String, WmsLayerMetadata> layerMetadata;

    private DateTime lastUpdateTime = new DateTime();

    public NcwmsCatalogue(NcwmsConfig config) throws IOException {
        /*
         * Initialise the storage for datasets and layer metadata.
         */
        datasets = new HashMap<String, Dataset>();
        layerMetadata = new HashMap<String, WmsLayerMetadata>();

        this.config = config;

        this.config.loadDatasets(this);
    }

    @Override
    public void datasetLoaded(Dataset dataset, Collection<NcwmsVariable> variables) {
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
    }

    @Override
    public int getMaxSimultaneousLayers() {
        return 1;
    }

    @Override
    public int getMaxImageWidth() {
        return config.getServerInfo().getMaxImageWidth();
    }

    @Override
    public int getMaxImageHeight() {
        return config.getServerInfo().getMaxImageHeight();
    }

    @Override
    public String getServerName() {
        return config.getServerInfo().getTitle();
    }

    @Override
    public String getServerAbstract() {
        return config.getServerInfo().getAdminPassword();
    }

    @Override
    public List<String> getServerKeywords() {
        return config.getServerInfo().getKeywords();
    }

    @Override
    public ContactInfo getContactInfo() {
        return config.getContactInfo();
    }

    @Override
    public DateTime getServerLastUpdate() {
        return lastUpdateTime;
    }

    @Override
    public Collection<Dataset> getAllDatasets() {
        return datasets.values();
    }

    @Override
    public String getDatasetTitle(String datasetId) {
        NcwmsDataset datasetInfo = config.getDatasetInfo(datasetId);
        if(datasetInfo == null) {
            return "";
        }
        return datasetInfo.getTitle();
    }

    @Override
    public Dataset getDatasetFromId(String layerName) {
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 2) {
            /*
             * TODO
             */
            throw new RuntimeException("This should be a concrete WmsException, not a Runtime one!");
        }
        return datasets.get(layerParts[0]);
    }

    @Override
    public String getVariableFromId(String layerName) {
        String[] layerParts = layerName.split("/");
        if (layerParts.length != 2) {
            /*
             * TODO
             */
            throw new RuntimeException("This should be a concrete WmsException, not a Runtime one!");
        }
        return layerParts[1];
    }

    @Override
    public String getLayerName(String datasetId, String variableId) {
        return datasetId + "/" + variableId;
    }

    @Override
    public WmsLayerMetadata getLayerMetadata(final String layerName)
            throws WmsLayerNotFoundException {
        if (!layerMetadata.containsKey(layerName)) {
            throw new WmsLayerNotFoundException("The layer: " + layerName + " doesn't exist");
        }
        return layerMetadata.get(layerName);
    }
}
