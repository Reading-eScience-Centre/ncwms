/*
 * Copyright (c) 2010 The University of Reading
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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;

/**
 * Brings time aggregation capabilities to the {@link AbstractScalarLayer} class.
 * This class allows for the fact that different timesteps might be contained
 * within different files within the {@link #getDataset() dataset}.  If two files
 * contain information for the same time, the file with the shorter forecast time
 * is chosen, because this is more likely to be the "more accurate" data.  This
 * logic implements the "best estimate" timeseries of a forecast model run collection.
 * @author Jon
 */
abstract class AbstractTimeAggregatedLayer extends AbstractScalarLayer
{
    /** These are sorted into ascending order of time */
    protected final List<TimestepInfo> timesteps = new ArrayList<TimestepInfo>();

    /**
     * A view of the {@link #timesteps} as an unmodifiable List of DateTimes,
     * for the {@link #getTimeValues()} method.
     */
    private final List<DateTime> dateTimes = 
        new AbstractList<DateTime>() {
            @Override public DateTime get(int index) {
                return timesteps.get(index).getDateTime();
            }
            @Override public int size() {
                return timesteps.size();
            }
        };
    
    /**
     * Creates an AbstractTimeAggregatedLayer.  This will not extract the
     * dateTimes from the CoverageMetadata objects.  Clients will need to
     * call addTimestepInfo() as required.
     */
    public AbstractTimeAggregatedLayer(CoverageMetadata lm)
    {
        super(lm);
    }

    /**
     * <p>Returns the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.</p>
     * <p>Note that this implementation returns an unmodifiable <i>view</i> on
     * the internal list of TimestepInfo objects, therefore if
     * {@link #addTimestepInfo(org.joda.time.DateTime, java.lang.String, int) addTimestepInfo()}
     * is called when iterating over the List returned by this method, undefined
     * behaviour might result.  However, this Layer will usually be constructed
     * during a metadata-reading operation and then will be treated as a read-only
     * object, so this conflict will not occur in typical usage.</p>
     * @return the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     */
    @Override
    public List<DateTime> getTimeValues()
    {
        return this.dateTimes;
    }

    /**
     * Adds a new TimestepInfo to this layer.  If a TimestepInfo object
     * already exists for this timestep, the TimestepInfo object with the lower
     * indexInFile value is chosen (this is most likely to be the result of a
     * shorter forecast lead time and therefore more accurate).
     * @param timestep The real date/time of this timestep
     * @param filename The filename containing this timestep
     * @param indexInFile The index of this timestep in the file
     * @throws NullPointerException if {@code timestep} or {@code filename}
     * is null.
     * @throws IllegalArgumentException if {@code indexInFile} is less than zero
     */
    void addTimestepInfo(DateTime dt, String filename, int indexInFile)
    {
        TimestepInfo tInfo = new TimestepInfo(dt, filename, indexInFile);
        // Find the insertion point in the List of timesteps
        int index = WmsUtils.findTimeIndex(this.getTimeValues(), dt);
        if (index >= 0)
        {
            // We already have a timestep for this time
            TimestepInfo existingTStep = this.timesteps.get(index);
            if (tInfo.getIndexInFile() < existingTStep.getIndexInFile())
            {
                // The new info probably has a shorter forecast time and so we
                // replace the existing version with this one
                existingTStep = tInfo;
            }
        }
        else
        {
            // We need to insert the TimestepInfo object into the list at the
            // correct location to ensure that the list is sorted in ascending
            // order of time.
            int insertionPoint = -(index + 1); // see docs for Collections.binarySearch()
            this.timesteps.add(insertionPoint, tInfo);
        }
    }

    /**
     * Simple class that holds information about which files in an aggregation
     * hold which timesteps for a variable.  Instances of this class are
     * immutable.
     */
    protected static class TimestepInfo
    {
        private DateTime timestep;
        private String filename;
        private int indexInFile;

        /**
         * Creates a new TimestepInfo object
         * @param timestep The real date/time of this timestep
         * @param filename The filename containing this timestep
         * @param indexInFile The index of this timestep in the file
         * @throws NullPointerException if {@code timestep} or {@code filename}
         * is null.
         * @throws IllegalArgumentException if {@code indexInFile} is less than zero
         */
        public TimestepInfo(DateTime timestep, String filename, int indexInFile)
        {
            if (timestep == null || filename == null)
            {
                throw new NullPointerException();
            }
            if (indexInFile < 0)
            {
                throw new IllegalArgumentException("indexInFile must be >= 0");
            }
            this.timestep = timestep;
            this.filename = filename;
            this.indexInFile = indexInFile;
        }

        public String getFilename()
        {
            return this.filename;
        }

        public int getIndexInFile()
        {
            return this.indexInFile;
        }

        /**
         * @return the date-time that this timestep represents
         */
        public DateTime getDateTime()
        {
            return this.timestep;
        }

        /**
         * Compares all fields for equality, using only the millisecond value
         * to compare the timesteps.
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (!(obj instanceof TimestepInfo)) return false;
            TimestepInfo otherTstep = (TimestepInfo)obj;
            return this.timestep.isEqual(otherTstep.timestep) && // Compares based on millisecond value only
                   this.indexInFile == otherTstep.indexInFile &&
                   this.filename.equals(otherTstep.filename);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 41 * hash + this.timestep.hashCode();
            hash = 41 * hash + this.filename.hashCode();
            hash = 41 * hash + this.indexInFile;
            return hash;
        }
    }

}
