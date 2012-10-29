/*
 * Copyright (c) 2007 The University of Reading
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
 */

package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.MetadataUtils;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.graphics.formats.ImageFormat;

/**
 * Displays the front page of the ncWMS application (i.e. jsp/index.jsp).
 *
 * @author Jon Blower
 */
public class FrontPageController extends AbstractController
{
    // These objects will be injected by Spring
    private Config config;
    
    public class IndexEntry {
        private RangeMetadata metadata;
        private BoundingBox bbox;

        public IndexEntry(RangeMetadata metadata, BoundingBox bbox) {
            super();
            this.metadata = metadata;
            this.bbox = bbox;
        }

        public RangeMetadata getMetadata() {
            return metadata;
        }

        public BoundingBox getBbox() {
            return bbox;
        }

    }
    
    /**
     * Entry point
     */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws Exception
    {
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("config", this.config);
        models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
        
        Map<Dataset, List<IndexEntry>> entries = new HashMap<Dataset, List<IndexEntry>>();
        for(Dataset dataset : config.getAllDatasets().values()){
            FeatureCollection<? extends Feature> featureCollection = dataset.getFeatureCollection();
            if(featureCollection == null){
                continue;
            }
            Collection<? extends Feature> features = featureCollection.getFeatures();
            List<IndexEntry> layers = new ArrayList<IndexEntry>();
            Set<String> used = new HashSet<String>();
            BoundingBox bbox = featureCollection.getCollectionBoundingBox();
            for(Feature feature : features){
                List<RangeMetadata> plottableLayers = MetadataUtils.getPlottableLayers(feature);
                for(RangeMetadata metadata : plottableLayers){
                    if(!used.contains(metadata.getName())){
                        layers.add(new IndexEntry(metadata, bbox));
                        used.add(metadata.getName());
                    }
                }
            }
            entries.put(dataset, layers);
        }
        models.put("layers", entries);
        
        return new ModelAndView("index", models); // results in display of jsp/index.jsp
    }

    /**
     * Called by the Spring framework to inject the config object
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }
    
}
