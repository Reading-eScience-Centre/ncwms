package uk.ac.rdg.resc.ncwms.config;

import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
import uk.ac.rdg.resc.ncwms.exceptions.FeatureNotDefinedException;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

public class NcwmsFeatureFactory implements FeatureFactory {
    
    private final NcwmsController controller;
    
    public NcwmsFeatureFactory(NcwmsController controller) {
        this.controller = controller;
    }
    
    @Override
    public Feature getFeature(String layerName) throws FeatureNotDefinedException {
        // Split the layer name on the slash character
        String[] parts = layerName.split("/");
        if (parts.length == 3) {
            String datasetId = parts[0];
            Dataset ds = controller.getConfig().getDatasetById(datasetId);
            if (ds == null)
                throw new FeatureNotDefinedException(layerName);

            String featureId = parts[1];// layerName.substring(slashIndex + 1);
            Feature feature = ds.getFeatureById(featureId);
            if (feature == null)
                throw new FeatureNotDefinedException(layerName);

            return feature;
        } else {
            // We don't bother looking for the position in the string where
            // the parse error occurs
            throw new FeatureNotDefinedException(layerName);
        }
    }
}
