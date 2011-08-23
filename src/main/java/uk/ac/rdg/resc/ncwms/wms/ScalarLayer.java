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
import java.util.List;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;

/**
 * A {@link Layer} that contains a single data value at each point in its grid.
 * @author Jon
 */
public interface ScalarLayer extends Layer
{

    /**
     * <p>Reads a single item of data from a point in space and time.  Returns
     * null for points outside the domain of the layer, or for
     * missing values (e.g. land pixels in oceanography data).</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal.</p>
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link #getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param xy The horizontal location from which this method will extract
     * data.  Data will be extracted from the nearest grid point to this location,
     * unless the point is outside the domain of the layer, in which case
     * null will be returned.
     * @return a single item of data from the given point in space and time
     * @throws NullPointerException if {@code xy} is null or if this
     * layer has a time axis and {@code time} is null.
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if {@code time} is not a valid time in this
     * Layer.
     * @throws IOException if there was an error reading from the data source
     */
    public Float readSinglePoint(DateTime time, double elevation, HorizontalPosition xy)
        throws InvalidDimensionValueException, IOException;

    /**
     * <p>Reads data at a number of horizontal locations at a single time and
     * elevation.  This is the method to use for reading a {@link HorizontalGrid}
     * of data.  Missing values (e.g. land pixels in oceanography data) will
     * be represented by null.</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal.</p>
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link #getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param points The collections of horizontal locations from which we are to
     * read data.  The returned List of data values will contain one value for
     * each item in this list in the same order.  This method will extract data
     * from the nearest grid points to each item in this list, returning
     * null for any points outside the domain of this Layer.
     * @return a List of data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws NullPointerException if {@code pointList} is null or if this
     * layer has a time axis and {@code time} is null.
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if {@code time} is not a valid time in this
     * Layer.
     * @throws IOException if there was an error reading from the data source
     */
    public List<Float> readHorizontalPoints(DateTime time, double elevation,
            Domain<HorizontalPosition> points)
        throws InvalidDimensionValueException, IOException;

    /**
     * <p>Reads data at a number of horizontal locations at a single time for a
     * number of elevations.  Missing values (e.g. land pixels in oceanography
     * data) will be represented by null.</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal.</p>
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link #getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevations The elevations for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * any of these values do not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * @param points The collections of horizontal locations from which we are to
     * read data.  The returned List of data values will contain one value for
     * each item in this list in the same order.  This method will extract data
     * from the nearest grid points to each item in this list, returning
     * null for any points outside the domain of this Layer.
     * @return a List of Lists: one List for every elevation value.  Each list
     * contains one value for each point in the {@code pointList}, in the same order.
     * @throws NullPointerException if {@code pointList} or {@code elevations}
     * is null or if this layer has a time axis and {@code time} is null.
     * @throws InvalidDimensionValueException if any of the {@code elevations}
     * is not valid in this Layer, or if {@code time} is not a valid time in this
     * Layer.
     * @throws IOException if there was an error reading from the data source
     * @todo What if this layer has no elevation axis?
     */
    public List<List<Float>> readVerticalSection(DateTime time, List<Double> elevations,
            Domain<HorizontalPosition> points)
        throws InvalidDimensionValueException, IOException;

    /**
     * <p>Reads a timeseries of data at a single xyz point from this Layer.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.</p>
     * <p>This method will perform no interpolation in time or elevation, but
     * will perform nearest-neighbour interpolation in the horizontal, i.e. it
     * will extract data from the nearest grid point to {@code xy}.  If {@code xy}
     * is outside the domain of this Layer, this method will return a List of
     * nulls, to retain consistency with other read...() methods
     * in this interface.</p>
     * <p>If this method is called on a Layer that has no time axis, an
     * InvalidDimensionValueException will be thrown, unless {@code times} is
     * an empty list (because all times are invalid for the axis).</p>
     * @param times The list of time instants for which we require data.  If a
     * value in this list is not found in {@link #getTimeValues()}, this method
     * will throw an {@link InvalidDimensionValueException}.  This may not be null
     * but it may be empty (in which case this method will return an empty list).
     * @param elevation The elevation for which we require data (in the
     * {@link #getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link #getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param xy The horizontal location from which this method will extract
     * data.  Data will be extracted from the nearest grid point to this location,
     * unless the point is outside the domain of the layer.
     * @return a List of data values, one for each point in
     * {@code times}, in the same order.
     * @throws NullPointerException if {@code times} or {@code xy} is null
     * @throws InvalidDimensionValueException if {@code elevation} is not a valid
     * elevation in this Layer, or if any of the {@code times} are not valid
     * times for this layer.
     * @throws IOException if there was an error reading from the data source
     */
    public List<Float> readTimeseries(List<DateTime> times, double elevation,
        HorizontalPosition xy) throws InvalidDimensionValueException, IOException;


}
