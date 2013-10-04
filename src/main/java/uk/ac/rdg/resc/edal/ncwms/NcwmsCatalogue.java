package uk.ac.rdg.resc.edal.ncwms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.GridDataset;
import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;
import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;
import uk.ac.rdg.resc.edal.wms.WmsLayerMetadata;

public class NcwmsCatalogue extends WmsCatalogue {

    private Map<String, Dataset> datasets;

    public NcwmsCatalogue() throws IOException {
        /*
         * When finished, this catalogue will read dataset
         * locations/configurations from disk.
         * 
         * For now, we initialise some stuff statically so that we can do an
         * end-to-end test
         */
        datasets = new HashMap<String, Dataset>();
        CdmGridDatasetFactory cdmGridDatasetFactory = new CdmGridDatasetFactory();
        GridDataset dataset = cdmGridDatasetFactory
                .createDataset("foam", "/home/guy/Data/FOAM_ONE/FOAM_one.ncml");
        datasets.put(dataset.getId(), dataset);
        for (String varId : dataset.getVariableIds()) {
            System.out.println(varId + " is part of the foam dataset");
        }
    }

    @Override
    public int getMaxSimultaneousLayers() {
        return 1;
    }

    @Override
    public int getMaxImageWidth() {
        return 1024;
    }

    @Override
    public int getMaxImageHeight() {
        return 512;
    }
    
    @Override
    public String getServerName() {
        return "ncWMS Server";
    }
    
    @Override
    public String getServerAbstract() {
        return "This server isn't a real server - it's a mock up for WMS testing...";
    }
    
    @Override
    public List<String> getServerKeywords() {
        return Arrays.asList("test","mockup","fake");
    }
    
    @Override
    public String getServerContactName() {
        return "Guy & \"'>< Griffiths";
    }
    
    @Override
    public String getServerContactOrganisation() {
        return "ESSC";
    }
    
    @Override
    public String getServerContactTelephone() {
        return "x5217";
    }
    
    @Override
    public String getServerContactEmail() {
        return "g.g@r.ac.uk";
    }
    
    @Override
    public List<Dataset> getAllDatasets() {
        return new ArrayList<Dataset>(datasets.values());
    }
    
    @Override
    public String getDatasetTitle(String datasetId) {
        return "Full title for " + datasets.get(datasetId).getId();
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
        return datasetId+"/"+variableId;
    }
    
    @Override
    public WmsLayerMetadata getLayerMetadata(final String layerName) {
        return new WmsLayerMetadata() {
            
            @Override
            public Boolean isLogScaling() {
                return false;
            }
            
            @Override
            public String getTitle() {
                return "A better title for "+layerName;
            }
            
            @Override
            public String getDescription() {
                return "This layer ("+layerName+") is a WMS layer.";
            }
            
            @Override
            public String getPalette() {
                return "default";
            }
            
            @Override
            public Integer getNumColorBands() {
                return 123;
            }
            
            @Override
            public Extent<Float> getColorScaleRange() {
                return Extents.newExtent(-123f, 123f);
            }

            @Override
            public String getCopyright() {
                return "COPYRIGHT INFO";
            }

            @Override
            public String getMoreInfo() {
                return "MORE INFO";
            }
            
            @Override
            public boolean isQueryable() {
                return true;
            }
        };
    }

}
