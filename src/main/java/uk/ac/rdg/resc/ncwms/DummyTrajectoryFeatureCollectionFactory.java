/*******************************************************************************
 * Copyright (c) 2014 The University of Reading
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

package uk.ac.rdg.resc.ncwms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.geotoolkit.referencing.crs.DefaultGeographicCRS;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.Phenomenon;
import uk.ac.rdg.resc.edal.Unit;
import uk.ac.rdg.resc.edal.cdm.feature.FeatureCollectionFactory;
import uk.ac.rdg.resc.edal.coverage.TrajectoryCoverage;
import uk.ac.rdg.resc.edal.coverage.domain.TrajectoryDomain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.TrajectoryDomainImpl;
import uk.ac.rdg.resc.edal.coverage.impl.TrajectoryCoverageImpl;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.FeatureCollection;
import uk.ac.rdg.resc.edal.feature.TrajectoryFeature;
import uk.ac.rdg.resc.edal.feature.impl.TrajectoryFeatureImpl;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.position.GeoPosition;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalCrs.PositiveDirection;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.edal.position.impl.GeoPositionImpl;
import uk.ac.rdg.resc.edal.position.impl.HorizontalPositionImpl;
import uk.ac.rdg.resc.edal.position.impl.TimePositionJoda;
import uk.ac.rdg.resc.edal.position.impl.VerticalCrsImpl;
import uk.ac.rdg.resc.edal.position.impl.VerticalPositionImpl;
import uk.ac.rdg.resc.edal.util.AbstractBigList;
import uk.ac.rdg.resc.edal.util.BigList;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Extents;

public class DummyTrajectoryFeatureCollectionFactory extends FeatureCollectionFactory {

    private class TFC implements FeatureCollection<TrajectoryFeature> {
        private TrajectoryFeature feature;
        private VerticalCrsImpl vcrs = new VerticalCrsImpl(Unit.getUnit("m"),
                PositiveDirection.DOWN, false);

        public TFC() {
            List<GeoPosition> list = new ArrayList<GeoPosition>();
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(0, 0), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(0, 10), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(10, 10), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(10, 8), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(10, 6), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(10, 4), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            list.add(new GeoPositionImpl(new HorizontalPositionImpl(10, 0), 0.0, vcrs,
                    new TimePositionJoda(0L)));
            TrajectoryDomain domain = new TrajectoryDomainImpl(list);
            TrajectoryCoverageImpl coverage = new TrajectoryCoverageImpl("", domain);
            coverage.addMember("val", domain, "values", Phenomenon.getPhenomenon("values"),
                    Unit.getUnit("m"), new AbstractBigList<Double>() {
                        @Override
                        public Double get(long index) {
                            if (index == 0) {
                                return 0.0;
                            } else if (index < 4) {
                                return null;
                            } else {
                                return Double.NaN;
                            }
                        }

                        @Override
                        public long sizeAsLong() {
                            return 7;
                        }
                    }, Double.class);
            feature = new TrajectoryFeatureImpl("tf", "tf_id", "", coverage, this);
        }

        @Override
        public String getId() {
            return "tfc";
        }

        @Override
        public String getName() {
            return "TFC";
        }

        @Override
        public TrajectoryFeature getFeatureById(String id) {
            return feature;
        }

        @Override
        public Collection<TrajectoryFeature> getFeatures() {
            return CollectionUtils.setOf(feature);
        }

        @Override
        public Set<String> getMemberIdsInCollection() {
            return CollectionUtils.setOf("tf");
        }

        @Override
        public Class<TrajectoryFeature> getFeatureType() {
            return TrajectoryFeature.class;
        }

        @Override
        public Collection<? extends TrajectoryFeature> findFeatures(BoundingBox boundingBox,
                Extent<Double> zRange, Extent<TimePosition> tRange, Set<String> memberNames) {
            return CollectionUtils.setOf(feature);
        }

        @Override
        public BoundingBox getCollectionBoundingBox() {
            return new BoundingBoxImpl(-180, -90, 180, 90, DefaultGeographicCRS.WGS84);
        }

        @Override
        public Extent<VerticalPosition> getCollectionVerticalExtent() {
            return Extents.newExtent((VerticalPosition) new VerticalPositionImpl(0.0, vcrs),
                    new VerticalPositionImpl(1.0, vcrs));
        }

        @Override
        public Extent<TimePosition> getCollectionTimeExtent() {
            return Extents.newExtent((TimePosition) new TimePositionJoda(0L), new TimePositionJoda(
                    10000L));
        }
    }

    @Override
    public FeatureCollection<? extends Feature> read(String location, String id, String name)
            throws IOException {
        return new TFC();
    }

}
