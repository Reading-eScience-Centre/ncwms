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

import java.util.List;

import org.joda.time.Chronology;
import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * A VectorLayer, some of whose properties can be overridden by the ncWMS
 * config system.
 * @author Jon
 */
final class VectorLayerImpl implements VectorLayer
{
    private final Dataset ds;
    private final VectorLayer wrappedLayer;

    /** Wraps an existing VectorLayer */
    public VectorLayerImpl(Dataset ds, VectorLayer vecLayer)
    {
        this.ds = ds;
        this.wrappedLayer = vecLayer;
    }

    @Override public Dataset getDataset() { return this.ds; }

    ////////////////////////////////////////////
    //// Values overridden in configuration ////
    ////////////////////////////////////////////

    /**
     * Gets the {@link Variable} object that is associated with this Layer.
     * The Variable object allows the sysadmin to override certain properties.
     */
    private Variable getVariable()
    {
        return this.ds.getVariables().get(this.getId());
    }

    /**
     * Gets the human-readable Title of this Layer.  If the sysadmin has set a
     * title for this layer in the config file, this title will be returned.
     * If not, the wrapped layer's title will be used.
     */
    @Override
    public String getTitle()
    {
        Variable var = this.getVariable();
        if (var != null && var.getTitle() != null) return var.getTitle();
        else return this.wrappedLayer.getTitle();
    }

    @Override
    public Extent<Float> getApproxValueRange()
    {
        return this.getVariable().getColorScaleRange();
    }

    @Override
    public boolean isLogScaling()
    {
        return this.getVariable().isLogScaling();
    }

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

    /////////////////////////
    //// Wrapped methods ////
    /////////////////////////

    @Override
    public ScalarLayer getEastwardComponent() {
       return this.wrappedLayer.getEastwardComponent();
    }

    @Override
    public ScalarLayer getNorthwardComponent() {
        return this.wrappedLayer.getNorthwardComponent();
    }

    @Override
    public String getId() { return this.wrappedLayer.getId(); }

    @Override
    public String getLayerAbstract()  { return this.wrappedLayer.getLayerAbstract(); }

    @Override
    public String getName()  { return this.wrappedLayer.getName(); }

    @Override
    public String getUnits()  { return this.wrappedLayer.getUnits(); }

    @Override
    public boolean isQueryable()  { return this.wrappedLayer.isQueryable(); }

    @Override
    public BoundingBox getBoundingBox() {
        return this.wrappedLayer.getBoundingBox();
    }

    @Override
    public HorizontalGrid getHorizontalGrid() {
        return this.wrappedLayer.getHorizontalGrid();
    }

    @Override
    public Chronology getChronology() {
        return this.wrappedLayer.getChronology();
    }

    @Override
    public List<DateTime> getTimeValues() {
        return this.wrappedLayer.getTimeValues();
    }

    @Override
    public DateTime getCurrentTimeValue() {
        return this.wrappedLayer.getCurrentTimeValue();
    }

    @Override
    public DateTime getDefaultTimeValue() {
        return this.wrappedLayer.getDefaultTimeValue();
    }

    @Override
    public List<Double> getElevationValues() {
        return this.wrappedLayer.getElevationValues();
    }

    @Override
    public double getDefaultElevationValue() {
        return this.wrappedLayer.getDefaultElevationValue();
    }

    @Override
    public String getElevationUnits() {
        return this.wrappedLayer.getElevationUnits();
    }

    @Override
    public boolean isElevationPositive() {
        return this.wrappedLayer.isElevationPositive();
    }

    @Override
    public boolean isElevationPressure() {
        return this.wrappedLayer.isElevationPressure();
    }

}
