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

import java.util.List;

import org.joda.time.Chronology;
import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;

/**
 * A displayable layer, contained within a {@link Dataset}.
 * @todo allow for "stepless" time and elevation axes, plus regularly-spaced
 * ones that could save space in Capabilities docs.
 * @todo factor out a Dimension class, which could hold the values of a dimension,
 * the default value, any operations to find indices along the axis, plus how
 * the values can be rendered in a Capabilities doc (e.g. if regularly-spaced).
 * @author Jon
 */
public interface Layer
{
    /** Returns the {@link Dataset} to which this layer belongs. */
    public Dataset getDataset();

    /** Returns an ID that is unique <b>within the {@link #getDataset() dataset}</b>. */
    public String getId();

    /** Returns a human-readable title */
    public String getTitle();

    /** Returns a (perhaps-lengthy) description of this layer */
    public String getLayerAbstract();

    /**
     * Returns an identifier for this layer that is unique <b>within a
     * Capabilities document</b>.  This is used as an identifier in GetMap
     * and other layer-specific requests.
     * @return an identifier for this layer that is unique on within a Capabilities
     * document.
     */
    public String getName();

    /**
     * Returns the layer's units.
     * @todo What if the layer has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getUnits();

    /**
     * Returns true if this Layer can be queried through GetFeatureInfo
     */
    public boolean isQueryable();

    /**
     * Returns the geographic extent of this layer in latitude-longitude
     * coordinates.  Note that this extent is not necessarily precise so
     * specifying the coordinate system is unnecessary.
     * @return the geographic extent of this layer in WGS84 latitude-longitude.
     */
    public BoundingBox getBoundingBox();

    /**
     * Returns the layer's horizontal grid, which is an object
     * that translates from real world coordinates to grid coordinates.
     * @return the horizontal grid of this layer.
     */
    public HorizontalGrid getHorizontalGrid();

    /**
     * Returns the {@link Chronology} used to interpret {@link DateTime}s that
     * represent the {@link #getTimeValues() time values} of this layer.
     * @return the Chronology used to interpret this layer's time values, or null
     * if this layer has no time values.
     */
    public Chronology getChronology();

    /**
     * Returns the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     * @return the list of time instants that are valid for this layer, in
     * chronological order, or an empty list if this Layer does not have a time axis.
     */
    public List<DateTime> getTimeValues();

    /**
     * Get the time value that is nearest to the current time.  This will be
     * used if the client specifies "TIME=current" in a request.  Implementations
     * may choose to ensure that this only returns values in the past, which
     * would tend to prevent this returning a time representing a forecast.
     * @return the time value that is closest to the current time, or null if this
     * layer doesn't have a time axis.
     */
    public DateTime getCurrentTimeValue();

    /**
     * <p>Get the time value that will be used by default if a client does not
     * explicitly provide a time parameter in a request ({@literal e.g.} GetMap),
     * or null if this layer does not support a default time value (or does not
     * have a time axis).</p>
     * <p>Note that this may frequently be the same as the {@link #getCurrentTimeValue()
     * current time value}.</p>
     * @return the default time value or null
     */
    public DateTime getDefaultTimeValue();

    /**
     * Returns the list of elevation values that are valid for this layer, or
     * an empty list if this Layer does not have a vertical axis.  Note that the
     * values in this list do not have to be ordered (although they usually will
     * be).  Clients must make no assumptions about ordering.
     * @return the list of elevation values that are valid for this layer, or
     * an empty list if this Layer does not have a vertical axis.
     */
    public List<Double> getElevationValues();

    /**
     * Get the elevation value that will be used by default if a client does not
     * explicitly provide an elevation parameter in a request ({@literal e.g.} GetMap),
     * or {@link Double#NaN} if this layer does not support a default elevation
     * value (or does not have an elevation axis).
     * @return the default elevation value or {@link Double#NaN}
     */
    public double getDefaultElevationValue();

    /**
     * Returns the units of the vertical axis
     * @todo What if the axis has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getElevationUnits();

    /**
     * Returns true if the positive direction of the elevation axis is up
     * @return true if the positive direction of the elevation axis is up
     * @todo Make the name and meaning of this method clearer
     */
    public boolean isElevationPositive();

    /**
     * Returns true if the vertical axis represents pressure.  In this case the
     * values of elevation will be positive, but will increase downward.
     * @todo This is a lousy name!
     */
    public boolean isElevationPressure();

    /**
     * Returns an approximate range of values that this layer can take.  This
     * is merely a hint, for example to suggest to clients sensible default
     * values for choosing a colour scale.
     * @return an approximate range of values that this layer can take.
     */
    public Extent<Float> getApproxValueRange();

    /**
     * Returns true if images generated from this Layer should be scaled
     * logarithmically by default (appropriate for quantities that vary over
     * several orders of magnitude).
     * @return true if images generated from this Layer should be scaled
     * logarithmically by default.
     */
    public boolean isLogScaling();

    /**
     * Returns the default colour palette to be used if the client does not
     * specify one in a GetMap request
     * @return the default colour palette to be used if the client does not
     * specify one
     */
    public ColorPalette getDefaultColorPalette();

    /**
     * Returns the default number of colour bands to be used in palettes that
     * style this layer.
     * @return number from 1 to {@link ColorPalette#MAX_NUM_COLOURS} inclusive
     */
    public int getDefaultNumColorBands();
}
