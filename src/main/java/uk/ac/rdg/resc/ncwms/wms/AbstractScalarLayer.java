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

package uk.ac.rdg.resc.ncwms.wms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;

import uk.ac.rdg.resc.edal.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 * Partial implementation of the {@link Layer} interface, providing convenience
 * methods and default implementations of some methods.  Most properties are
 * set through the provided setter methods.
 * @todo implement a makeImmutable() method, which prevents futher changes?
 * This could be called by the metadata-reading operation to ensure that
 * all future operations are read-only.
 * @author Jon
 */
public abstract class AbstractScalarLayer implements ScalarLayer
{
    /** TODO: may belong elsewhere (e.g. a CDM package), in which case should be made unmodifiable */
    private static final Set<String> PRESSURE_UNITS =
        CollectionUtils.setOf("Pa", "hPa", "bar", "millibar", "decibar", "atmosphere", "atm", "pascal");

    /** Compares double values based upon their absolute value */
    private static final Comparator<Double> ABSOLUTE_VALUE_COMPARATOR = new Comparator<Double>() {
        @Override public int compare(Double d1, Double d2) {
            return Double.compare(Math.abs(d1), Math.abs(d2));
        }
    };

    private final CoverageMetadata cm;

    /**
     * Creates an AbstractScalarLayer from a CoverageMetadata object.
     * @param id An identifier that is unique within this layer's
     * {@link #getDataset() dataset}.
     * @throws NullPointerException if {@code cm == null}
     */
    public AbstractScalarLayer(CoverageMetadata cm)
    {
        if (cm == null) throw new NullPointerException("CoverageMetadata can't be null");
        this.cm = cm;
    }

    @Override public String getId() { return this.cm.getId(); }

    /**
     * Returns a layer name that is unique on this server, created from the
     * {@link #getDataset() dataset} id and the {@link #getId() layer id} by the
     * {@link WmsUtils#createUniqueLayerName(java.lang.String, java.lang.String)}
     * method.
     */
    @Override
    public String getName()
    {
        return WmsUtils.createUniqueLayerName(this.getDataset().getId(), this.getId());
    }

    @Override public String getTitle() { return this.cm.getTitle(); }

    @Override public String getLayerAbstract() { return this.cm.getDescription(); }

    @Override public String getUnits() { return this.cm.getUnits(); }

    @Override public String getElevationUnits() { return this.cm.getElevationUnits(); }

    @Override public List<Double> getElevationValues() { return this.cm.getElevationValues(); }

    @Override
    public boolean isElevationPositive() { return this.cm.isElevationPositive(); }

    @Override
    public boolean isElevationPressure() { return this.cm.isElevationPressure(); }

    @Override
    public GeographicBoundingBox getGeographicBoundingBox() {
        return this.cm.getGeographicBoundingBox();
    }

    @Override
    public HorizontalGrid getHorizontalGrid() {
        return this.cm.getHorizontalGrid();
    }

    @Override public List<DateTime> getTimeValues() { return this.cm.getTimeValues(); }

    /**
     * Returns true if this layer has a time axis.  This is a convenience method
     * that simply checks if the list of timesteps is not empty.
     */
    protected boolean hasTimeAxis() { return !this.getTimeValues().isEmpty(); }

    /**
     * Returns true if this layer has an elevation axis.  This is a convenience
     * method that simply checks if the list of elevation values is not empty.
     */
    protected boolean hasElevationAxis() { return !this.getElevationValues().isEmpty(); }
    
    /**
     * Get the time value that is nearest to the current time.  This will be
     * used if the client specifies "TIME=current" in a request.  Implementations
     * may choose to ensure that this only returns values in the past, which
     * would tend to prevent this returning a time representing a forecast.
     * @return the time value that is closest to the current time, or null if this
     * layer doesn't have a time axis.
     */
    @Override
    public DateTime getCurrentTimeValue()
    {
        int currentTimeIndex = this.getCurrentTimeIndex();
        if (currentTimeIndex < 0) return null; // this layer doesn't have a time axis
        return this.getTimeValues().get(currentTimeIndex);
    }
    
    /**
     * Gets the time value that will be used by default if a client does not
     * explicitly provide a time parameter in a request ({@literal e.g.} GetMap),
     * or null if this layer does not have a time axis.  This implementation
     * returns the same as {@link #getCurrentTimeValue()}.
     */
    @Override
    public DateTime getDefaultTimeValue()
    {
        return this.getCurrentTimeValue();
    }

    /**
     * Returns the Chronology: null if this layer has no time axis, or the
     * Chronology of the first timestep on the axis if a time axis is present.
     * @return
     */
    @Override
    public Chronology getChronology()
    {
       if (this.hasTimeAxis()) {
           return this.getTimeValues().get(0).getChronology();
       }
       return null;
    }

    /**
     * Gets the index in the {@link #getTimeValues() list of valid timesteps}
     * of the past or present timestep that is closest to the current time, or
     * -1 if this layer does not have a time axis.  If all timesteps in this Layer
     * are in the future then this will return 0.
     * @return the index in the {@link #getTimeValues() list of valid timesteps}
     * of the timestep that is closest to the current time, or -1 if this layer
     * does not have a time axis.
     * @todo should this always be a time in the past or present (not the future)
     * unless all the values are in the future?
     */
    protected int getCurrentTimeIndex()
    {
        if (this.getTimeValues().size() == 0) return -1; // no time axis
        int index = WmsUtils.findTimeIndex(this.getTimeValues(), new DateTime());
        if (index >= 0) {
            // Exact match.  Very unlikely!
            return index;
        } else {
            // We can calculate the insertion point
            int insertionPoint = -(index + 1); // see docs for Collections.binarySearch()
            // We return the index of the most recent past time
            if (insertionPoint > 0) return insertionPoint - 1; // The most recent past time
            else return 0; // All DateTimes on the axis are in the future, so we take the earliest
        }
    }

    /**
     * Searches the list of timesteps for the specified date-time, returning
     * the index of the date-time, or throwing an {@link InvalidDimensionValueException}
     * if the specified date-time is not a valid timestep for this layer.  If
     * this layer does not have a time axis, this will return -1.
     */
    public int findAndCheckTimeIndex(DateTime target) throws InvalidDimensionValueException
    {
        if (!this.hasTimeAxis()) return -1;
        int index = WmsUtils.findTimeIndex(this.getTimeValues(), target);
        if (index >= 0) return index;
        throw new InvalidDimensionValueException("time", WmsUtils.dateTimeToISO8601(target));
    }

    /**
     * <p>Gets the elevation value that will be used by default if a client does not
     * explicitly provide an elevation parameter in a request ({@literal e.g.} GetMap),
     * or {@link Double#NaN} if this layer does not support a default elevation
     * value (or does not have an elevation axis).</p>
     * <p>This implementation simply returns the value that is closest to zero
     * (zero will usually be the surface), or NaN if this layer doesn't have
     * an elevation axis.</p>
     * @return the default elevation value or {@link Double#NaN}
     */
    @Override
    public double getDefaultElevationValue()
    {
        // We must access the elevation values via the accessor method in case
        // subclasses override it.
        if (!this.hasElevationAxis()) return Double.NaN;

        if (PRESSURE_UNITS.contains(this.getElevationUnits()))
        {
            // The vertical axis is pressure.  The default (closest to the surface)
            // is therefore the maximum value.
            return Collections.max(this.getElevationValues());
        }
        else
        {
            // The vertical axis represents linear height, so we find which
            // value is closest to zero (the surface), i.e. the smallest
            // absolute value
            return Collections.min(this.getElevationValues(), ABSOLUTE_VALUE_COMPARATOR);
        }
    }

    /**
     * Finds the index of a certain z value (within the {@link #getElevationValues()
     * list of elevation values}) by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal, or -1 if
     * targetVal is not found in the list of elevation values, or if this
     * layer does not have an elevation axis.
     */
    protected int findElevationIndex(double targetVal)
    {
        // We must access this via the accessor method in case subclasses override it.
        List<Double> zVals = this.getElevationValues();
        for (int i = 0; i < zVals.size(); i++)
        {
            // The fuzzy comparison fails for zVal == 0.0 so we do a direct
            // comparison too
            if (zVals.get(i) == targetVal ||
                Math.abs((zVals.get(i) - targetVal) / targetVal) < 1e-5)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Searches the list of elevation values for the specified value, returning
     * the index of the elevation value, or throwing an {@link InvalidDimensionValueException}
     * if the specified elevation is not valid for this layer.  If this layer
     * does not have an elevation axis, this will return -1.
     */
    public int findAndCheckElevationIndex(double targetVal) throws InvalidDimensionValueException
    {
        if (!this.hasElevationAxis()) return -1;
        int index = this.findElevationIndex(targetVal);
        if (index >= 0) return index;
        throw new InvalidDimensionValueException("elevation", "" + targetVal);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always returns false.</p>
     */
    @Override
    public boolean isLogScaling()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always returns the default palette from {@link ColorPalette}.</p>
     */
    @Override
    public ColorPalette getDefaultColorPalette()
    {
        return ColorPalette.get(null);
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readHorizontalPoints(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.datareader.PointList) Layer.readHorizontalPoints()} that
     * makes repeated calls to
     * {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readSinglePoint()}.
     * This implementation is not expected to be maximally efficient and subclasses
     * are encouraged to override this.</p>
     * @return a List of data values
     */
    @Override
    public List<Float> readHorizontalPoints(DateTime time,
            double elevation, Domain<HorizontalPosition> domain)
            throws InvalidDimensionValueException, IOException
    {
        List<? extends HorizontalPosition> points = domain.getDomainObjects();
        List<Float> vals = new ArrayList<Float>(points.size());
        for (HorizontalPosition xy : points) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }

    /**
     * <p>Simple but naive implementation of
     * {@link Layer#readTimeseries(java.util.List, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readTimeseries()}
     * that makes repeated
     * calls to {@link Layer#readSinglePoint(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition) Layer.readSinglePoint()}.
     * This implementation
     * is not expected to be maximally efficient and subclasses are encouraged
     * to override this.</p>
     */
    @Override
    public List<Float> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException, IOException
    {
        // TODO: could check validity of all the times before we start
        // potentially-lengthy data-reading operations
        List<Float> vals = new ArrayList<Float>(times.size());
        for (DateTime time : times) {
            vals.add(this.readSinglePoint(time, elevation, xy));
        }
        return vals;
    }
    
}
