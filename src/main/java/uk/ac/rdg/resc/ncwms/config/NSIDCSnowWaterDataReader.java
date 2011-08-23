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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.position.LonLatPosition;
import uk.ac.rdg.resc.edal.util.Utils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * DataReader for NSIDC snow/water data.  This is an example of how to create
 * a DataReader for files that can't be read by the Java-NetCDF libraries.
 * Another alternative method would be to create an IO Service Provider for
 * this dataset.
 *
 * @author Jon Blower
 */
public class NSIDCSnowWaterDataReader extends DataReader
{
    private static final Logger logger = LoggerFactory.getLogger(NSIDCSnowWaterDataReader.class);
    
    /**
     * The number of rows of data in the grid
     */
    private static final int ROWS = 721;
    /**
     * The number of columns of data in the grid
     */
    private static final int COLS = 721;
    /**
     * radius of the earth (km), authalic sphere based on International datum
     */
    private static final double RE_KM = 6371.228;
    /**
     * nominal cell size in kilometers
     */
    private static final double CELL_KM = 25.067525;
    
    private static final GeographicBoundingBox BBOX = 
          new DefaultGeographicBoundingBox(-180, 180, 0, 90);

    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location ({@literal i.e.} one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the individual file
     * @throws IOException if there was an error reading from the data source
     */
    @Override
    protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException
    {
        String filename = new File(location).getName();
        final DateTime timestep;
        try
        {
            // SimpleDateFormats aren't thread safe so we have to keep creating
            // new ones.
            DateFormat df = new SimpleDateFormat("'NL'yyyyMM'.v01.NSIDC8'");
            timestep = new DateTime(df.parse(filename).getTime());
        }
        catch(Exception e)
        {
            logger.error("Error parsing filepath " + location, e);
            // TODO: not really an IOException
            throw new IOException("Error parsing filepath " + location);
        }

        // We only create one CoverageMetadata object
        CoverageMetadata lm = new CoverageMetadata() {

            @Override public String getId() { return "swe"; }

            @Override public String getTitle() { return "snow_water_equivalent"; }

            @Override
            public String getDescription() { return "Snow Water Equivalent (SWE)"; }

            @Override
            public String getUnits() { return "mm"; }

            @Override
            public GeographicBoundingBox getGeographicBoundingBox() {
                return BBOX;
            }

            @Override
            // This is not used in this class
            public HorizontalGrid getHorizontalGrid() { return null; }

            @Override
            public Chronology getChronology() { return timestep.getChronology(); }

            @Override
            public List<DateTime> getTimeValues() { return Arrays.asList(timestep); }

            @Override
            public List<Double> getElevationValues() { return Collections.emptyList(); }

            @Override public String getElevationUnits() { return ""; }

            @Override public boolean isElevationPositive() { return false; }

            @Override public boolean isElevationPressure() { return false; }

        };
        
        return Arrays.asList(lm);
    }
    
    /**
     * Reads data from a file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param domain The list of real-world x-y points for which we need data
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws IOException if there is an error reading from the source data
     */
    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex,
            Domain<HorizontalPosition> domain)
        throws IOException
    {
        // Find the file containing the data
        logger.debug("Reading data from " + filename);

        // Create an array to hold the data
        List<Float> picData = WmsUtils.nullArrayList(domain.getDomainObjects().size());
        
        FileInputStream fin = null;
        ByteBuffer data = null;
        // Read the whole of the file into memory
        try
        {
            fin = new FileInputStream(filename);
            data = ByteBuffer.allocate(ROWS * COLS * 2);
            data.order(ByteOrder.LITTLE_ENDIAN);
            // Read the whole of the file into memory
            fin.getChannel().read(data);
        }
        finally
        {
            try { if (fin != null) fin.close(); } catch (IOException ioe) {}
        }
        
        int picIndex = 0;
        for (HorizontalPosition point : domain.getDomainObjects())
        {
            LonLatPosition lonLat = Utils.transformToWgs84LonLat(point);
            if (lonLat.getLatitude() >= 0.0 && lonLat.getLatitude() <= 90.0)
            {
                // Find the index in the source data
                int dataIndex = latLonToIndex(lonLat.getLatitude(), lonLat.getLongitude());
                // two bytes per pixel
                short val = data.getShort(dataIndex * 2);
                if (val > 0) picData.set(picIndex, (float)val);
            }
            picIndex++;
        }
        
        return picData;
    }
    
    /**
     * convert geographic coordinates (spherical earth) to
     *	azimuthal equal area or equal area cylindrical grid coordinates
     *
     *	status = ezlh_convert (grid, lat, lon, &r, &s)
     *
     *	input : grid - projection name "[NSM][lh]"
     *          where l = "low"  = 25km resolution
     *                     h = "high" = 12.5km resolution
     *		lat, lon - geo. coords. (decimal degrees)
     *
     *	output: r, s - column, row coordinates
     *
     *	result: status = 0 indicates normal successful completion
     *			-1 indicates error status (point not on grid)
     */
    private static int latLonToIndex(double lat, double lon)
    {        
        double Rg = RE_KM / CELL_KM;
        
        double r0 = (COLS - 1) / 2.0;
        double s0 = (ROWS - 1) / 2.0;
        
        double phi = Math.toRadians(lat);
        double lam = Math.toRadians(lon);
        
        double rho = 2 * Rg * Math.sin(Math.PI / 4.0 - phi / 2.0);
        
        int col = (int)Math.round(r0 + rho * Math.sin(lam));
        int row = (int)Math.round(s0 + rho * Math.cos(lam));
        
        int index = row * COLS + col;
        return index;
    }
    
}
