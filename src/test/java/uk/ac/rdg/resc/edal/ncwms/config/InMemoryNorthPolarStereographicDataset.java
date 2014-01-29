/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
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
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joda.time.Chronology;

import uk.ac.rdg.resc.edal.dataset.AbstractGridDataset;
import uk.ac.rdg.resc.edal.dataset.DataReadingStrategy;
import uk.ac.rdg.resc.edal.dataset.GridDataSource;
import uk.ac.rdg.resc.edal.dataset.plugins.VectorPlugin;
import uk.ac.rdg.resc.edal.exceptions.DataReadingException;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.grid.RegularGridImpl;
import uk.ac.rdg.resc.edal.metadata.GridVariableMetadata;
import uk.ac.rdg.resc.edal.metadata.Parameter;
import uk.ac.rdg.resc.edal.ncwms.NcwmsCatalogue;
import uk.ac.rdg.resc.edal.position.VerticalCrs;
import uk.ac.rdg.resc.edal.util.Array4D;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;

/**
 * This is an in-memory dataset which provides x- and y- vector variables for a
 * dataset on a native north polar stereographic grid. It can be used for
 * testing of vector transformations. It also exposes the method
 * {@link InMemoryNorthPolarStereographicDataset#getWmsVariables()} for
 * convenience, allowing this to be easily inserted into an
 * {@link NcwmsCatalogue}.
 * 
 * @author Guy Griffiths
 */
public class InMemoryNorthPolarStereographicDataset extends AbstractGridDataset {
    public InMemoryNorthPolarStereographicDataset() throws EdalException {
        super("northPole", getGridVariables());
        addVariablePlugin(new VectorPlugin("allx_u", "allx_v", "All X", false));
        addVariablePlugin(new VectorPlugin("ally_u", "ally_v", "All Y", false));
    }

    private static Collection<GridVariableMetadata> getGridVariables() {
        HorizontalGrid hDomain;
        try {
            hDomain = new RegularGridImpl(-4350000, -4350000, 8350000, 8350000,
                    GISUtils.getCrs("EPSG:32661"), 100, 100);
        } catch (InvalidCrsException e) {
            e.printStackTrace();
            hDomain = null;
        }
        GridVariableMetadata xumetadata = new GridVariableMetadata("allx_u", new Parameter(
                "allx_u", "All X,  u-component", "...", "none", null), hDomain, null, null, true);
        GridVariableMetadata xvmetadata = new GridVariableMetadata("allx_v", new Parameter(
                "allx_v", "All X,  v-component", "...", "none", null), hDomain, null, null, true);
        GridVariableMetadata yumetadata = new GridVariableMetadata("ally_u", new Parameter(
                "ally_u", "All Y,  u-component", "...", "none", null), hDomain, null, null, true);
        GridVariableMetadata yvmetadata = new GridVariableMetadata("ally_v", new Parameter(
                "ally_v", "All Y,  v-component", "...", "none", null), hDomain, null, null, true);
        List<GridVariableMetadata> metadataList = new ArrayList<GridVariableMetadata>();
        metadataList.add(xumetadata);
        metadataList.add(xvmetadata);
        metadataList.add(yumetadata);
        metadataList.add(yvmetadata);
        return metadataList;
    }

    public Collection<NcwmsVariable> getWmsVariables() {
        NcwmsDataset ds = new NcwmsDataset();
        ds.setId("northPole");
        ds.setTitle("North Polar Stereographic");
        ds.setLocation("inmemory");

        NcwmsVariable xuVar = new NcwmsVariable("allx_u", "All X u-comp", "", Extents.newExtent(0f,
                100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250, null, null, null);
        NcwmsVariable xvVar = new NcwmsVariable("allx_v", "All X v-comp", "", Extents.newExtent(0f,
                100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250, null, null, null);
        NcwmsVariable yuVar = new NcwmsVariable("ally_u", "All Y u-comp", "", Extents.newExtent(0f,
                100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250, null, null, null);
        NcwmsVariable yvVar = new NcwmsVariable("ally_v", "All Y v-comp", "", Extents.newExtent(0f,
                100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250, null, null, null);

        NcwmsVariable xmagVar = new NcwmsVariable("allx_uallx_v-mag", "All X magnitude", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);
        NcwmsVariable xdirVar = new NcwmsVariable("allx_uallx_v-dir", "All X direction", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);

        NcwmsVariable ymagVar = new NcwmsVariable("ally_ually_v-mag", "All Y magnitude", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);
        NcwmsVariable ydirVar = new NcwmsVariable("ally_ually_v-dir", "All Y direction", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);

        NcwmsVariable xgroupVar = new NcwmsVariable("allx_uallx_v-group", "All X", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);
        NcwmsVariable ygroupVar = new NcwmsVariable("ally_ually_v-group", "All Y", "",
                Extents.newExtent(0f, 100f), ColourPalette.DEFAULT_PALETTE_NAME, "linear", 250,
                null, null, null);
        List<NcwmsVariable> vars = new ArrayList<NcwmsVariable>();
        xuVar.setNcwmsDataset(ds);
        vars.add(xuVar);
        xvVar.setNcwmsDataset(ds);
        vars.add(xvVar);
        yuVar.setNcwmsDataset(ds);
        vars.add(yuVar);
        yvVar.setNcwmsDataset(ds);
        vars.add(yvVar);
        xmagVar.setNcwmsDataset(ds);
        vars.add(xmagVar);
        xdirVar.setNcwmsDataset(ds);
        vars.add(xdirVar);
        ymagVar.setNcwmsDataset(ds);
        vars.add(ymagVar);
        ydirVar.setNcwmsDataset(ds);
        vars.add(ydirVar);
        xgroupVar.setNcwmsDataset(ds);
        vars.add(xgroupVar);
        ygroupVar.setNcwmsDataset(ds);
        vars.add(ygroupVar);

        return vars;
    }

    @Override
    public GridFeature readFeature(String featureId) throws DataReadingException {
        throw new UnsupportedOperationException("Not implemented - this is a test dataset");
    }

    @Override
    protected GridDataSource openGridDataSource() throws IOException {
        return new GridDataSource() {
            @Override
            public Array4D<Number> read(final String variableId, int tmin, int tmax, int zmin,
                    int zmax, int ymin, int ymax, int xmin, int xmax) throws IOException {
                return new Array4D<Number>((tmax - tmin + 1), (zmax - zmin + 1), (ymax - ymin + 1),
                        (xmax - xmin + 1)) {
                    @Override
                    public Number get(int... coords) {
                        if (variableId.equalsIgnoreCase("allx_u")) {
                            return 10;
                        } else if (variableId.equalsIgnoreCase("allx_v")) {
                            return 0;
                        } else if (variableId.equalsIgnoreCase("ally_u")) {
                            return 0;
                        } else if (variableId.equalsIgnoreCase("ally_v")) {
                            return 10;
                        }
                        return null;
                    }

                    @Override
                    public void set(Number value, int... coords) {
                    }
                };
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    protected DataReadingStrategy getDataReadingStrategy() {
        return DataReadingStrategy.BOUNDING_BOX;
    }

    @Override
    public Chronology getDatasetChronology() {
        return null;
    }

    @Override
    public VerticalCrs getDatasetVerticalCrs() {
        return null;
    }
}