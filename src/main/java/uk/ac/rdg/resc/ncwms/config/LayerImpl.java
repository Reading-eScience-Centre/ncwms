/*
 * Copyright (c) 2006 The University of Reading
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.Domain;
import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * A concrete Layer implementation that supports  time aggregation through the
 * {@link AbstractTimeAggregatedLayer} superclass.  Instances of this class are
 * returned through the {@link DataReader#getAllLayers(uk.ac.rdg.resc.ncwms.config.Dataset)}
 * method.
 *
 * @author Jon Blower
 */
final class LayerImpl extends AbstractTimeAggregatedLayer
{
    private final Dataset dataset;
    private final DataReader dataReader;
    
    public LayerImpl(CoverageMetadata lm, Dataset ds, DataReader dr)
    {
        super(lm);
        this.dataset = ds;
        this.dataReader = dr;
    }

    /**
     * Gets the human-readable Title of this Layer.  If the sysadmin has set a
     * title for this layer in the config file, this title will be returned.
     * If not, the title that was read in the relevant {@link DataReader} will
     * be used.
     */
    @Override
    public String getTitle()
    {
        Variable var = this.getVariable();
        if (var != null && var.getTitle() != null) return var.getTitle();
        else return super.getTitle();
    }

    @Override public Dataset getDataset() { return this.dataset; }
    
    /**
     * Returns an approximate range of values that this layer can take.  This
     * is merely a hint, for example to suggest to clients sensible default
     * values for choosing a colour scale.
     * @return an approximate range of values that this layer can take.
     */
    @Override
    public Extent<Float> getApproxValueRange()
    {
        return this.getVariable().getColorScaleRange();
    }
    
    /**
     * @return true if this variable can be queried through the GetFeatureInfo
     * function.  Delegates to Dataset.isQueryable().
     */
    @Override
    public boolean isQueryable()
    {
        return this.dataset.isQueryable();
    }

    /**
     * Return true if we are to use logarithmic colour scaling by default for
     * this layer.
     * @return true if we are to use logarithmic colour scaling by default for
     * this layer.
     */
    @Override
    public boolean isLogScaling()
    {
        return this.getVariable().isLogScaling();
    }

    /**
     * Returns the default colour palette to be used if the client does not
     * specify one in a GetMap request
     * @return the default colour palette to be used if the client does not
     * specify one
     */
    @Override
    public ColorPalette getDefaultColorPalette()
    {
        return ColorPalette.get(this.getVariable().getPaletteName());
    }

    @Override
    public int getDefaultNumColorBands()
    {
        return this.getVariable().getNumColorBands();
    }

    /**
     * Gets the {@link Variable} object that is associated with this Layer.
     * The Variable object allows the sysadmin to override certain properties.
     */
    private Variable getVariable()
    {
        return this.dataset.getVariables().get(this.getId());
    }

    /**
     * {@inheritDoc}
     * <p>This implementation makes a single call to the underlying DataReader
     * and is thus more efficient than making multiple calls to readSinglePoint().</p>
     */
    @Override
    public List<Float> readHorizontalPoints(DateTime time, double elevation,
            Domain<HorizontalPosition> domain)
        throws InvalidDimensionValueException, IOException
    {
        int zIndex = this.findAndCheckElevationIndex(elevation);
        FilenameAndTimeIndex fti = this.findAndCheckFilenameAndTimeIndex(time);
        return this.readHorizontalDomain(fti, zIndex, domain);
    }
    
    /** Reads a set of horizontal posiitions based upon t and z indices rather than natural values */
    List<Float> readHorizontalDomain(FilenameAndTimeIndex fti, int zIndex, Domain<HorizontalPosition> domain)
        throws IOException
    {
        return this.dataReader.read(fti.filename, this, fti.tIndexInFile, zIndex, domain);
    }

    @Override
    public List<List<Float>> readVerticalSection(DateTime time, List<Double> elevations,
            Domain<HorizontalPosition> points)
            throws InvalidDimensionValueException, IOException
    {
        FilenameAndTimeIndex fti = this.findAndCheckFilenameAndTimeIndex(time);
        // Defend against null values
        List<Integer> zIndices;
        if (elevations == null) {
            zIndices = Arrays.asList(-1);
        } else {
            zIndices = new ArrayList<Integer>(elevations.size());
            for (Double el : elevations) {
                zIndices.add(this.findAndCheckElevationIndex(el));
            }
        }
        return this.dataReader.readVerticalSection(fti.filename, this, fti.tIndexInFile, zIndices, points);
    }

    /**
     * Package-private method (called by
     * {@link Config#readDataGrid(uk.ac.rdg.resc.ncwms.wms.ScalarLayer,
     * org.joda.time.DateTime, double, uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid,
     * uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry) Config.readDataGrid()}) that
     * finds the file that contains the give DateTime and the index within the file.
     * @param time The time to search for
     * @return
     * @throws InvalidDimensionValueException if {@code time} is not a valid
     * DateTime for this layer.
     */
    FilenameAndTimeIndex findAndCheckFilenameAndTimeIndex(DateTime time)
        throws InvalidDimensionValueException
    {
        // Find and check the time and elevation values. Indices of -1 will be
        // returned if this layer does not have a time/elevation axis
        int tIndex = this.findAndCheckTimeIndex(time);

        // Find which file we're reading from and the time index in the file
        final String filename;
        final int tIndexInFile;
        if (tIndex < 0) {
            // This layer has no time dimension.
            final String location = this.dataset.getLocation();
            if (WmsUtils.isOpendapLocation(location)) {
                filename = location;
            } else {
                //  It's possible that the dataset's location is a glob
                // expression, so we expand it and take the first element.
                filename = DataReader.expandGlobExpression(location).get(0).getPath();
            }
            tIndexInFile = tIndex;
        } else {
            TimestepInfo tInfo = this.timesteps.get(tIndex);
            filename = tInfo.getFilename();
            tIndexInFile = tInfo.getIndexInFile();
        }
        return new FilenameAndTimeIndex(filename, tIndexInFile);
    }

    static class FilenameAndTimeIndex
    {
        /** The filename containing the data */
        String filename;
        /** The index of the timestep <b>within this file</b>, or -1 if this
         layer doesn't have a time axis. */
        int tIndexInFile;

        public FilenameAndTimeIndex(String filename, int tIndexInFile)
        {
            this.filename = filename;
            this.tIndexInFile = tIndexInFile;
        }
    }

    @Override
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
        throws InvalidDimensionValueException, IOException
    {
        HorizontalDomain singlePoint = new HorizontalDomain(xy);
        return this.readHorizontalPoints(time, elevation, singlePoint).get(0);
    }

    @Override
    public List<Float> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException, IOException
    {
        if (times == null) throw new NullPointerException("times");
        int zIndex = this.findAndCheckElevationIndex(elevation);

        // We need to group the tIndices by their containing file.  That way, we
        // can read all the time data from the same file in the same operation.
        // This maps filenames to lists of t indices within the file.  We must
        // preserve the insertion order so we use a LinkedHashMap.
        Map<String, List<Integer>> files = new LinkedHashMap<String, List<Integer>>();
        for (DateTime dt : times) {
            FilenameAndTimeIndex ft = this.findAndCheckFilenameAndTimeIndex(dt);
            List<Integer> tIndicesInFile = files.get(ft.filename);
            if (tIndicesInFile == null) {
                tIndicesInFile = new ArrayList<Integer>();
                files.put(ft.filename, tIndicesInFile);
            }
            tIndicesInFile.add(ft.tIndexInFile);
        }

        // Now we read the data from each file and add it to the timeseries
        List<Float> data = new ArrayList<Float>();
        for (String filename : files.keySet()) {
            List<Integer> tIndicesInFile = files.get(filename);
            List<Float> arr = this.dataReader.readTimeseries(filename, this, tIndicesInFile, zIndex, xy);
            data.addAll(arr);
        }

        // Check that we have the right number of data points
        if (data.size() != times.size()) {
            throw new AssertionError("Timeseries length inconsistency");
        }

        return data;
    }
}
