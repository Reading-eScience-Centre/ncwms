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

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import uk.ac.rdg.resc.edal.catalogue.DataCatalogue;
import uk.ac.rdg.resc.edal.catalogue.SimpleLayerNameMapper;
import uk.ac.rdg.resc.edal.catalogue.jaxb.DatasetConfig;
import uk.ac.rdg.resc.edal.catalogue.jaxb.VariableConfig;
import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.graphics.exceptions.EdalLayerNotFoundException;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.graphics.style.util.EnhancedVariableMetadata;
import uk.ac.rdg.resc.edal.graphics.style.util.LayerNameMapper;
import uk.ac.rdg.resc.edal.graphics.style.util.PlottingStyleParameters;
import uk.ac.rdg.resc.edal.graphics.style.util.SldTemplateStyleCatalogue;
import uk.ac.rdg.resc.edal.graphics.style.util.StyleCatalogue;
import uk.ac.rdg.resc.edal.metadata.VariableMetadata;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsConfig;
import uk.ac.rdg.resc.edal.ncwms.config.NcwmsDynamicService;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.util.ContactInfo;
import uk.ac.rdg.resc.edal.wms.util.ServerInfo;

/**
 * An extension of {@link DataCatalogue} to add WMS-specific capabilities for
 * ncWMS.
 *
 * @author Guy Griffiths
 */
public class NcwmsCatalogue extends DataCatalogue implements WmsCatalogue {
    private static final String DYNAMIC_DATASET_CACHE_NAME = "dynamicDatasetCache";
    private StyleCatalogue styleCatalogue;

    public NcwmsCatalogue(NcwmsConfig config) throws IOException {
        super(config, new SimpleLayerNameMapper());
        this.styleCatalogue = SldTemplateStyleCatalogue.getStyleCatalogue();

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
        return (NcwmsConfig) super.getConfig();
    }

    @Override
    public ServerInfo getServerInfo() {
        return ((NcwmsConfig) config).getServerInfo();
    }

    @Override
    public ContactInfo getContactInfo() {
        return ((NcwmsConfig) config).getContactInfo();
    }

    @Override
    public Dataset getDatasetFromId(String datasetId) {
        Dataset dataset = super.getDatasetFromId(datasetId);
        if (dataset != null) {
            return dataset;
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
            
            if(datasetId.equals(dynamicService.getAlias())) {
                /*
                 * Just the alias has been requested.  This isn't a dataset!
                 */
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
    public EnhancedVariableMetadata getLayerMetadata(final VariableMetadata variableMetadata)
            throws EdalLayerNotFoundException {
        try {
            return super.getLayerMetadata(variableMetadata);
        } catch (EdalLayerNotFoundException e) {
            /*
             * The layer is not defined in the XmlDataCatalogue. However, we may
             * still have a dynamic dataset
             */
            final String layerName = getLayerNameMapper().getLayerName(
                    variableMetadata.getDataset().getId(), variableMetadata.getId());

            final NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService == null) {
                throw new EdalLayerNotFoundException("The layer: " + layerName + " doesn't exist");
            }
            /*
             * We have a dynamic dataset. Return sensible defaults
             */
            EnhancedVariableMetadata metadata = new EnhancedVariableMetadata() {
                @Override
                public String getId() {
                    return variableMetadata.getId();
                }

                @Override
                public String getTitle() {
                    return variableMetadata.getId();
                }

                @Override
                public PlottingStyleParameters getDefaultPlottingParameters() {
                    return new PlottingStyleParameters(null, ColourPalette.DEFAULT_PALETTE_NAME,
                            null, null, null, false, ColourPalette.MAX_NUM_COLOURS, 1f);
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
            };
            return metadata;
        }
    }

    private NcwmsDynamicService getDynamicServiceFromLayerName(String layerName) {
        NcwmsDynamicService dynamicService = null;
        for (NcwmsDynamicService testDynamicService : ((NcwmsConfig) config).getDynamicServices()) {
            if (layerName.startsWith(testDynamicService.getAlias())) {
                dynamicService = testDynamicService;
            }
        }
        if (dynamicService == null
                || !dynamicService.getIdMatchPattern().matcher(layerName).matches()) {
            return null;
        }
        return dynamicService;
    }

    @Override
    public LayerNameMapper getLayerNameMapper() {
        return layerNameMapper;
    }

    @Override
    public StyleCatalogue getStyleCatalogue() {
        return styleCatalogue;
    }

    @Override
    public String getDatasetTitle(String datasetId) {
        DatasetConfig datasetInfo = config.getDatasetInfo(datasetId);
        if (datasetInfo != null) {
            return datasetInfo.getTitle();
        } else if (getDynamicServiceFromLayerName(datasetId) != null) {
            return "Dynamic service from " + datasetId;
        } else {
            throw new EdalLayerNotFoundException(datasetId
                    + " does not refer to an existing dataset");
        }
    }

    @Override
    public boolean isDownloadable(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            xmlVariable.isDownloadable();
        } else {
            /*
             * We may be dealing with a dynamic dataset
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService != null) {
                return dynamicService.isDownloadable();
            }
        }
        return false;
    }

    @Override
    public boolean isQueryable(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            return xmlVariable.isQueryable();
        } else {
            /*
             * We may be dealing with a dynamic dataset
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService != null) {
                return dynamicService.isQueryable();
            }
        }
        return false;
    }

    @Override
    public boolean isDisabled(String layerName) {
        VariableConfig xmlVariable = getXmlVariable(layerName);
        if (xmlVariable != null) {
            return xmlVariable.isDisabled();
        } else {
            /*
             * We may be dealing with a dynamic dataset
             */
            NcwmsDynamicService dynamicService = getDynamicServiceFromLayerName(layerName);
            if (dynamicService != null) {
                return dynamicService.isDisabled();
            } else {
                return true;
            }
        }
    }

    private VariableConfig getXmlVariable(String layerName) {
        DatasetConfig datasetInfo = config.getDatasetInfo(getLayerNameMapper()
                .getDatasetIdFromLayerName(layerName));
        if (datasetInfo != null) {
            return datasetInfo.getVariableById(getLayerNameMapper().getVariableIdFromLayerName(
                    layerName));
        } else {
            return null;
        }
    }
}
