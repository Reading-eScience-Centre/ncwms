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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.domain.Extent;
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
    private static final String DYNAMIC_DATASET_CACHE_NAME = "dynamicDatasetCache";

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

        setCache(config.getCacheSettings());

        /*
         * Configure cache for dynamic datasets. Keep up to 10 dynamic datasets
         * in memory, and expire them after 10 minutes of inactivity.
         * 
         * This cache is primarily for dynamic EN3 datasets or other datasets
         * which are expensive to create. For the normal gridded case datasets
         * are cheap to create (but expensive to read) so this cache will have
         * little effect on performance. For datasets where a spatial index
         * needs to be built, this will save a lot of time. However it is
         * probably only rarely (if ever) that this will be used in practice.
         */
        CacheConfiguration cacheConfig = new CacheConfiguration(DYNAMIC_DATASET_CACHE_NAME, 0)
                .eternal(false).maxEntriesLocalHeap(10).timeToLiveSeconds(10 * 60)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                .persistence(new PersistenceConfiguration().strategy(Strategy.NONE))
                .transactionalMode(TransactionalMode.OFF);

        /*
         * If we already have a cache, we can assume that the configuration has
         * changed, so we remove and re-add it.
         */
        Cache dynamicDatasetCache = new Cache(cacheConfig);
        cacheManager.addCache(dynamicDatasetCache);
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
         * Re-sort the datasets map according to the titles of the datasets, so
         * that they appear in the menu in this order.
         */
        List<Map.Entry<String, Dataset>> entryList = new ArrayList<Map.Entry<String, Dataset>>(
                datasets.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Dataset>>() {
            public int compare(Map.Entry<String, Dataset> d1, Map.Entry<String, Dataset> d2) {
                return config.getDatasetInfo(d1.getKey()).getTitle()
                        .compareTo(config.getDatasetInfo(d2.getKey()).getTitle());
            }
        });

        datasets = new LinkedHashMap<String, Dataset>();
        for (Map.Entry<String, Dataset> entry : entryList) {
            datasets.put(entry.getKey(), entry.getValue());
        }

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
            if (datasetInfo != null && !datasetInfo.isDisabled() && datasetInfo.isReady()) {
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
             * We may have a dynamic dataset. First check the dynamic dataset
             * cache.
             */
            Cache dynamicDatasetCache = cacheManager.getCache(DYNAMIC_DATASET_CACHE_NAME);
            Element element = dynamicDatasetCache.get(datasetId);
            if (element != null && element.getObjectValue() != null) {
                return (Dataset) element.getObjectValue();
            }
            /*
             * Check to see if we have a dynamic service defined which this
             * dataset ID can map to
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(datasetId);
            if (dynamicService == null || dynamicService.isDisabled()) {
                return null;
            }
            /*
             * We do the +1 so that the datasetPath doesn't start with a /
             */
            String datasetPath = datasetId.substring(dynamicService.getAlias().length() + 1);

            /*
             * Check if we allow this path or if it is disallowed by the dynamic
             * dataset regex
             */
            if (!dynamicService.getIdMatchPattern().matcher(datasetPath).matches()) {
                return null;
            }

            String datasetUrl = dynamicService.getServicePath() + "/" + datasetPath;

            String title = datasetId;
            while (title.startsWith("/") && title.length() > 0)
                title = title.substring(1);

            try {
                DatasetFactory datasetFactory = DatasetFactory.forName(dynamicService
                        .getDataReaderClass());
                Dataset dynamicDataset = datasetFactory.createDataset(datasetId, datasetUrl);
                /*
                 * Store in the cache
                 */
                dynamicDatasetCache.put(new Element(datasetId, dynamicDataset));
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
        return layerName.substring(finalSlashIndex + 1);
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
            final NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService == null) {
                throw new EdalLayerNotFoundException("The layer: " + layerName + " doesn't exist");
            }
            /*
             * We have a dynamic dataset. Return sensible defaults
             */
            WmsLayerMetadata metadata = new WmsLayerMetadata() {
                @Override
                public boolean isQueryable() {
                    return dynamicService.isQueryable();
                }

                @Override
                public Boolean isLogScaling() {
                    return false;
                }

                @Override
                public boolean isDisabled() {
                    return dynamicService.isDisabled();
                }

                @Override
                public String getTitle() {
                    return layerName.substring(layerName.lastIndexOf("/"));
                }

                @Override
                public String getPalette() {
                    return ColourPalette.DEFAULT_PALETTE_NAME;
                }

                @Override
                public Integer getNumColorBands() {
                    return 250;
                }

                @Override
                public Color getNoDataColour() {
                    return null;
                }

                @Override
                public String getMoreInfo() {
                    return null;
                }

                @Override
                public String getDescription() {
                    return null;
                }

                @Override
                public String getCopyright() {
                    return null;
                }

                @Override
                public Extent<Float> getColorScaleRange() {
                    return null;
                }

                @Override
                public Color getBelowMinColour() {
                    return null;
                }

                @Override
                public Color getAboveMaxColour() {
                    return null;
                }
            };
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
