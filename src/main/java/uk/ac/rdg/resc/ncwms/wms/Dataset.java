/*
 * Copyright (c) 2009 The University of Reading
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

package uk.ac.rdg.resc.ncwms.wms;

import java.util.Map;

import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.ncwms.config.FeaturePlottingMetadata;

/**
 * Represents a Dataset, {@literal i.e.} a collection of {@link Layer}s.  Datasets are
 * represented in ncWMS as non-displayable container layers.
 *
 * @author Jon Blower
 */
public interface Dataset
{
    /** Returns a unique ID for this dataset (unique on this server) */
    public String getId();

    /**
     * @return the human-readable Title of this dataset
     */
    public String getTitle();

    /**
     * Returns a copyright statement for this dataset.
     * @todo replace with Rights?
     */
    public String getCopyrightStatement();

    /**
     * Returns a web link to more information about this dataset.
     */
    public String getMoreInfoUrl();

    /**
     * <p>Returns the date/time at which this dataset was last updated.
     * This is used for Capabilities document version control in the
     * UPDATESEQUENCE part of the Capabilities document.</p>
     * <p>If the dataset has never been loaded, this method will return null.</p>
     * <p>If the last update time is unknown, the safest
     * thing to do is to return the current date/time.  This will mean that
     * clients should never cache the Capabilities document.</p>
     * @return the date/time at which this dataset was last updated, or null if
     * the dataset has never been loaded.
     */
    public TimePosition getLastUpdateTime();

    /**
     * Gets the {@link GridSeriesFeature} with the given {@link Feature#getId() id}.  The id
     * is unique within the dataset, not necessarily on the whole server.
     * @return The layer with the given id, or null if there is no layer with
     * the given id.
     */
    public GridSeriesFeature<Float> getFeatureById(String featureId);

    /**
     * Gets the {@link FeatureCollection} that comprises this dataset
     */
    public FeatureCollection<GridSeriesFeature<Float>> getFeatureCollection();

    /**
     * Returns true if the dataset is ready for use.  If the dataset is ready,
     * {@link #isLoading()} and {@link #isError()} will return false and
     * {@link #getException()} will return null.
     * @return true if the dataset is ready for use.
     */
    public boolean isReady();

    /**
     * Returns true if the dataset is not ready because it is being loaded.
     * Note that there could be an outstanding error from a previous loading
     * attempt, in which case {@link #isError()} will also return true and
     * {@link #getException()} will return the exception.
     * @return true if the dataset is not ready because it is being loaded.
     */
    public boolean isLoading();

    /**
     * Returns true if there is an error with this dataset. More details about
     * the error can be found by calling {@link #getException()}.  Note that a
     * dataset could be loading, in which case this returns the error from the
     * previous attempt at loading.
     * @return true if there is an error with this dataset.
     */
    public boolean isError();

    /**
     * If a dataset cannot be used due to an error, this can be called to find
     * out more details.
     * @return Exception object containing more details about the error, or null
     * if there is no error.
     */
    public Exception getException();

    /**
     * Returns true if this dataset cannot be used because access has been
     * disabled by the system administrator.
     * @return true if this dataset cannot be used because access has been
     * disabled
     */
    public boolean isDisabled();
    
    /**
     * Gets the configuration information for all the
     * {@link FeaturePlottingMetadata}s in this dataset. This information allows
     * the system administrator to manually set certain properties that would
     * otherwise be auto-detected.
     * 
     * @return A {@link Map} of variable IDs to {@link FeaturePlottingMetadata}
     *         objects. The variable ID is unique within a dataset and
     *         corresponds with the {@link Feature#getId()}.
     * @see FeaturePlottingMetadata
     */
    public Map<String, FeaturePlottingMetadata> getPlottingMetadataMap();
}
