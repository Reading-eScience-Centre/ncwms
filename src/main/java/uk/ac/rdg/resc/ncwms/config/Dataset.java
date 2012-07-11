package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.cdm.feature.GridSeriesFeatureCollectionFactory;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * A dataset object in the ncWMS configuration system: contains a number of
 * Layer objects, which are held in memory and loaded periodically, triggered by
 * the {@link Config} object.
 * 
 * @author Jon Blower
 * @todo A lot of these methods can be made package-private
 */
@Root(name = "dataset")
public class Dataset implements uk.ac.rdg.resc.ncwms.wms.Dataset {
    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);

    // Unique ID for this dataset
    @Attribute(name = "id")
    private String id;

    // Location of this dataset (NcML file, OPeNDAP location etc)
    @Attribute(name = "location")
    private String location;

    @Attribute(name = "queryable", required = false)
    private boolean queryable = true; // True if we want GetFeatureInfo enabled
    // for this dataset

    // We'll use a default data reader unless this is overridden in the config file
    @Attribute(name = "dataReaderClass", required = false)
    private String featureCollectionFactoryClass = "";

    @Attribute(name = "copyrightStatement", required = false)
    private String copyrightStatement = "";

    @Attribute(name = "moreInfo", required = false)
    private String moreInfo = "";

    // Set true to disable the dataset without removing it completely
    @Attribute(name = "disabled", required = false)
    private boolean disabled = false; 

    @Attribute(name = "title")
    private String title;

    // The update interval in minutes. -1 means "never update automatically"
    @Attribute(name = "updateInterval", required = false)
    private int updateInterval = -1;

    /*
     * We don't do "private List<Variable> variable..." here because if we do,
     * the config file will contain "<variable class="java.util.ArrayList>",
     * presumably because the definition doesn't clarify what sort of List
     * should be used. This allows the admin to override certain auto-detected
     * parameters of the variables within the dataset (e.g. title, min and max
     * values) This is a temporary store of variables that are read from the
     * config file. The real set of all variables is in the variables Map.
     */
    @ElementList(name = "variables", type = FeaturePlottingMetadata.class, required = false)
    private ArrayList<FeaturePlottingMetadata> variableList = new ArrayList<FeaturePlottingMetadata>();

    private Config config;

    private State state = State.NEEDS_REFRESH; // State of this dataset.

    // Set if there is an error loading the dataset
    private Exception err; 
    // The number of consecutive times we've seen an error when loading a dataset
    private int numErrorsInARow = 0;
    
    /*
     * Used to express progress with loading the metadata for this dataset, one
     * line at a time
     */
    private List<String> loadingProgress = new ArrayList<String>(); 
    
     
    /*
     * This contains the map of variable IDs to Variable objects. We use a
     * LinkedHashMap so that the order of datasets in the Map is preserved.
     */
    private Map<String, FeaturePlottingMetadata> metadata = new LinkedHashMap<String, FeaturePlottingMetadata>();

    /*
     * The time at which this dataset's stored Layers were last successfully
     * updated, or null if the Layers have not yet been loaded
     */
    private TimePosition lastSuccessfulUpdateTime = null;

    /*
     * The time at which we last got an error when updating the dataset's
     * metadata, or null if we've never seen an error
     */
    private TimePosition lastFailedUpdateTime = null;

    private FeatureCollection<GridSeriesFeature> features;

    /**
     * Checks that the data we have read are valid. Checks that there are no
     * duplicate variable IDs.
     */
    @Validate
    public void validate() throws PersistenceException {
        List<String> varIds = new ArrayList<String>();
        for (FeaturePlottingMetadata var : variableList) {
            String varId = var.getId();
            if (varIds.contains(varId)) {
                throw new PersistenceException("Duplicate variable id %s", varId);
            }
            varIds.add(varId);
        }
    }

    /**
     * Called when we have checked that the configuration is valid. Populates
     * the variables hashmap.
     */
    @Commit
    public void build() {
        /*
         * We already know from validate() that there are no duplicate variable
         * IDs
         */
        for (FeaturePlottingMetadata var : variableList) {
            var.setDataset(this);
            metadata.put(var.getId(), var);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id.trim();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location.trim();
    }

    /**
     * Called when a new dataset is created to set up a back-link to the
     * {@link Config} object. This is only called by {@link Config}.
     */
    void setConfig(Config config) {
        this.config = config;
    }

    /**
     * @return true if this dataset is ready for use
     */
    @Override
    public synchronized boolean isReady() {
        return !isDisabled() && (state == State.READY || state == State.UPDATING);
    }

    /**
     * @return true if this dataset is not ready because it is being loaded
     */
    @Override
    public synchronized boolean isLoading() {
        return !isDisabled() && (state == State.NEEDS_REFRESH || state == State.LOADING);
    }

    @Override
    public boolean isError() {
        // Note that we don't use state == ERROR here because it's possible for
        // a dataset to be loading and have an error from a previous loading
        // attempt that an admin might want to see
        return err != null;
    }

    /**
     * If this Dataset has not been loaded correctly, this returns the Exception
     * that was thrown. If the dataset has no errors, this returns null.
     */
    @Override
    public Exception getException() {
        return err;
    }

    public State getState() {
        return state;
    }

    /**
     * @return true if this dataset can be queried using GetFeatureInfo
     */
    public boolean isQueryable() {
        return queryable;
    }

    public void setQueryable(boolean queryable) {
        this.queryable = queryable;
    }

    /**
     * @return the human-readable Title of this dataset
     */
    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "id: " + id + ", location: " + location;
    }

    public String getDataReaderClass() {
        return featureCollectionFactoryClass;
    }

    void setDataReaderClass(String dataReaderClass) throws Exception {
        featureCollectionFactoryClass = dataReaderClass;
    }

    /**
     * @return the update interval for this dataset in minutes
     */
    public int getUpdateInterval() {
        return updateInterval;
    }

    /**
     * Sets the update interval for this dataset in minutes
     */
    void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    /**
     * @return a DateTime object representing the time at which this dataset was
     *         last updated, or null if the dataset has never been loaded.
     */
    @Override
    public TimePosition getLastUpdateTime() {
        return lastSuccessfulUpdateTime;
    }

    /**
     * Returns the layer in this dataset with the given id, or null if there is
     * no layer in this dataset with the given id.
     * 
     * @param layerId
     *            The layer identifier, unique within this dataset. Note that
     *            this is distinct from the layer name, which is unique on the
     *            server.
     * @return the layer in this dataset with the given id, or null if there is
     *         no layer in this dataset with the given id.
     */
    @Override
    public GridSeriesFeature getFeatureById(String featureId) {
        if(features == null)
            return null;
        return features.getFeatureById(featureId);
    }

    /**
     * @return a Collection of all the layers in this dataset.
     */
    @Override
    public FeatureCollection<GridSeriesFeature> getFeatureCollection() {
        return features;
    }

    /**
     * Returns true if this dataset has been disabled, which will make it
     * invisible to the outside world.
     * 
     * @return true if this dataset has been disabled
     */
    @Override
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Called by the admin application to hide a dataset completely from public
     * view
     * 
     * @param disabled
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Gets the copyright statement for this dataset, replacing "${year}" as
     * appropriate with the current year.
     * 
     * @return The copyright statement, or the empty string if no copyright
     *         statement has been set.
     */
    @Override
    public String getCopyrightStatement() {
        if (copyrightStatement == null || copyrightStatement.trim().equals("")) {
            return "";
        }
        int currentYear = new TimePositionJoda().getYear();
        // Don't forget to escape dollar signs and backslashes in the regexp
        return copyrightStatement.replaceAll("\\$\\{year\\}", "" + currentYear);
    }

    public void setCopyrightStatement(String copyrightStatement) {
        this.copyrightStatement = copyrightStatement;
    }

    @Override
    public String getMoreInfoUrl() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
    }

    /**
     * Gets an explanation of the current progress with loading this dataset.
     * Will be displayed in the admin application when isLoading() == true. Each
     * element in the returned list is a stage in loading the dataset.
     */
    public List<String> getLoadingProgress() {
        return loadingProgress;
    }

    private void appendLoadingProgress(String loadingProgress) {
        this.loadingProgress.add(loadingProgress);
    }

    @Override
    public Map<String, FeaturePlottingMetadata> getPlottingMetadataMap() {
        return metadata;
    }

    public void addVariable(FeaturePlottingMetadata var) {
        var.setDataset(this);
        variableList.add(var);
        metadata.put(var.getId(), var);
    }

    /**
     * Forces this dataset to be refreshed the next time it has an opportunity
     */
    void forceRefresh() {
        err = null;
        state = State.NEEDS_REFRESH;
    }

    /**
     * Called by the scheduled reloader in the {@link Config} object to load the
     * Layers from the data files and store them in memory. This method is
     * called periodially by the config object and is not called by any other
     * client. This is also the only method that can update the
     * {@link #getState()} of the dataset. Therefore we know that multiple
     * threads will not be calling this method simultaneously and we don't have
     * to synchronize anything.
     */
    void loadLayers() {
        loadingProgress = new ArrayList<String>();
        // Include the id of the dataset in the thread for debugging purposes
        // Comment this out to use the default thread names (e.g.
        // "pool-2-thread-1")
        Thread.currentThread().setName("load-metadata-" + id);

        // Check to see if this dataset needs to have its metadata refreshed
        if (!needsRefresh())
            return;

        // Now load the layers and manage the state of the dataset
        try {
            // if lastUpdateTime == null, this dataset has never previously been
            // loaded.
            state = lastSuccessfulUpdateTime == null ? State.LOADING : State.UPDATING;

            doLoadLayers();

            // Update the state of this dataset. If we've got this far there
            // were no errors.
            err = null;
            numErrorsInARow = 0;
            state = State.READY;
            lastSuccessfulUpdateTime = new TimePositionJoda();

            logger.debug("Loaded metadata for {}", id);

            // Update the state of the config object
            config.setLastUpdateTime(lastSuccessfulUpdateTime);
            config.save();
        } catch (Exception e) {
            state = State.ERROR;
            numErrorsInARow++;
            lastFailedUpdateTime = new TimePositionJoda();
            // Reduce logging volume by only logging the error if it's a new
            // type of exception.
            if (err == null || err.getClass() != e.getClass()) {
                logger.error(e.getClass().getName() + " loading metadata for dataset " + id, e);
            }
            err = e;
        }
    }

    /**
     * @return true if the metadata from this dataset needs to be reloaded.
     */
    private boolean needsRefresh() {
        logger.debug("Last update time for dataset {} is {}", id, lastSuccessfulUpdateTime);
        logger.debug("State of dataset {} is {}", id, state);
        logger.debug("Disabled = {}", disabled);
        if (disabled || state == State.LOADING || state == State.UPDATING) {
            return false;
        } else if (state == State.NEEDS_REFRESH) {
            return true;
        } else if (state == State.ERROR) {
            // We implement an exponential backoff for reloading datasets that
            // have
            // errors, which saves repeatedly hammering remote servers
            double delaySeconds = Math.pow(2, numErrorsInARow);
            // The maximum interval between refreshes is 10 minutes
            delaySeconds = Math.min(delaySeconds, 10 * 60);
            // lastFailedUpdateTime should never be null: this is defensive
            boolean needsRefresh = lastFailedUpdateTime == null ? true
                    : (new TimePositionJoda().getValue() > lastFailedUpdateTime.getValue()
                            + (1000 * (long) delaySeconds));
            logger.debug("delay = {} seconds, needsRefresh = {}", delaySeconds, needsRefresh);
            return needsRefresh;
        } else if (updateInterval < 0) {
            return false; // We never update this dataset
        } else {
            // State = READY. Check the age of the metadata
            // Return true if we are after the next scheduled update
            return (new TimePositionJoda().getValue() > lastSuccessfulUpdateTime.getValue() + 60 * 1000 * updateInterval);
        }
    }

    /**
     * Does the job of loading the metadata from this dataset.
     */
    private void doLoadLayers() throws Exception {
        logger.debug("Getting data reader of type {}", featureCollectionFactoryClass);
        GridSeriesFeatureCollectionFactory fcFactory = GridSeriesFeatureCollectionFactory.forName(featureCollectionFactoryClass);
        // Look for OPeNDAP datasets and update the credentials provider
        // accordingly
        config.updateCredentialsProvider(this);
        // Read the layers from the data reader
        features = fcFactory.read(location, id, title);
        appendLoadingProgress("loaded layers");
        // Look for overriding attributes in the configuration
        readLayerConfig();
        appendLoadingProgress("attributes overridden");
        appendLoadingProgress("Finished loading metadata");
    }

    /**
     * Read the configuration information from individual layers from the config
     * file.
     */
    private void readLayerConfig() {
        for (String featureId : features.getFeatureIds()) {
            GridSeriesFeature feature = features.getFeatureById(featureId);
            /*
             * First add all the scalar members
             */
            for(String member : feature.getCoverage().getScalarMemberNames()){
                String memberId = feature.getId()+"/"+member;
             // Load the Variable object from the config file or create a new
                // one if it doesn't exist.
                FeaturePlottingMetadata plottingMetadata = getPlottingMetadataMap().get(memberId);
                
                if (plottingMetadata == null) {
                    plottingMetadata = new FeaturePlottingMetadata();
                    plottingMetadata.setId(memberId);
                    addVariable(plottingMetadata);
                }
                // If there is no title set for this layer in the config file, we
                // use the title that was read by the DataReader.
                if (plottingMetadata.getTitle() == null)
                    plottingMetadata.setTitle(feature.getCoverage().getScalarMetadata(member).getName());

                /*
                 * 
                 * Set the colour scale range. If this isn't specified in the
                 * config information, load an "educated guess" at the scale
                 * range from the source data.
                 */
                if (plottingMetadata.getColorScaleRange() == null) {
                    appendLoadingProgress("Reading min-max data for layer " + memberId);
                    Extent<Float> valueRange;
                    try {
                        valueRange = WmsUtils.estimateValueRange(feature, member);
                        if (valueRange.isEmpty()) {
                            // We failed to get a valid range. Just guess at a scale
                            valueRange = Extents.newExtent(-50.0f, 50.0f);
                        } else if (valueRange.getLow().equals(valueRange.getHigh())) {
                            /*
                             * This happens occasionally if the above algorithm
                             * happens to hit an area of uniform data. We make sure
                             * that the max is greater than the min.
                             */
                            valueRange = Extents.newExtent(valueRange.getLow(),
                                    valueRange.getHigh() + 1.0f);
                        } else {
                            /*
                             * Set the scale range of the layer, factoring in a 10%
                             * expansion to deal with the fact that the sample data
                             * we read might not be representative
                             */
                            float diff = valueRange.getHigh() - valueRange.getLow();
                            valueRange = Extents.newExtent(valueRange.getLow() - 0.05f * diff, valueRange.getHigh() + 0.05f
                                    * diff);
                        }
                    } catch (Exception e) {
                        logger.error("Error reading min-max from layer " + feature.getId() + " in dataset " + id, e);
                        valueRange = Extents.newExtent(-50.0f, 50.0f);
                    }
                    plottingMetadata.setColorScaleRange(valueRange);
                }
            }
        }
    }

    /**
     * The state of a Dataset.
     */
    public static enum State {

        /** Dataset is new or has changed and needs to be loaded */
        NEEDS_REFRESH,

        /** In the process of loading */
        LOADING,

        /** Ready for use */
        READY,

        /** Dataset is ready but is internally sychronizing its metadata */
        UPDATING,

        /** An error occurred when loading the dataset. */
        ERROR;

    };
}
