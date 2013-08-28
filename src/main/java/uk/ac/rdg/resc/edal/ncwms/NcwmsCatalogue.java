package uk.ac.rdg.resc.edal.ncwms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.dataset.GridDataset;
import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;
import uk.ac.rdg.resc.edal.wms.WmsCatalogue;

public class NcwmsCatalogue extends WmsCatalogue {
    
    private Map<String, Dataset> datasets;
    
    public NcwmsCatalogue() throws IOException {
        /*
         * When finished, this catalogue will read dataset locations/configurations from disk.
         * 
         * For now, we initialise some stuff statically so that we can do an end-to-end test
         */
        datasets = new HashMap<String, Dataset>();
        CdmGridDatasetFactory cdmGridDatasetFactory = new CdmGridDatasetFactory();
        GridDataset dataset = cdmGridDatasetFactory.createDataset("/home/guy/Data/FOAM_ONE/FOAM_one.ncml");
        datasets.put("foam", dataset);
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
    public Dataset getDatasetFromId(String layerName) {
        String[] layerParts = layerName.split("/");
        if(layerParts.length != 2) {
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
        if(layerParts.length != 2) {
            /*
             * TODO
             */
            throw new RuntimeException("This should be a concrete WmsException, not a Runtime one!");
        }
        return layerParts[1];
    }

}
