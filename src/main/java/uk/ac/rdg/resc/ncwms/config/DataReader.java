/*
 * Copyright (c) 2007 The University of Reading
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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oro.io.GlobFilenameFilter;
import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.Domain;
import uk.ac.rdg.resc.edal.coverage.domain.impl.HorizontalDomain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * Abstract superclass for classes that read data and metadata from datasets.
 * Only one instance of each DataReader class will be created, hence subclasses
 * must be thread-safe (no instance variables etc).
 * @see DefaultDataReader
 * @author jdb
 */
public abstract class DataReader
{
    /**
     * Maps class names to DataReader objects.  Only one DataReader object of
     * each class will ever be created.
     */
    private static Map<String, DataReader> readers = new HashMap<String, DataReader>();
    
    /**
     * This class can only be instantiated through forName()
     */
    protected DataReader(){}
    
    /**
     * Gets a DataReader of the given class.  Only one instance of each class
     * will be returned, hence subclasses of DataReader must be thread-safe.
     * @param dataReaderClassName The name of the subclass of DataReader
     * @throws Exception if there was an error creating the DataReader
     * @throws ClassCastException if {@code dataReaderClassName} isn't the name
     * of a valid DataReader subclass
     */
    public static DataReader forName(String dataReaderClassName)
            throws Exception
    {
        String clazz = DefaultDataReader.class.getName();
        if (dataReaderClassName != null && !dataReaderClassName.trim().equals(""))
        {
            clazz = dataReaderClassName;
        }
        // TODO make this thread safe.  Can this be done without explicit locking?
        // See Bloch, Effective Java.
        if (!readers.containsKey(clazz))
        {
            // Create the DataReader object
            Object drObj = Class.forName(clazz).newInstance();
            // this will throw a ClassCastException if drObj is not a DataReader
            readers.put(clazz, (DataReader)drObj);
        }
        return readers.get(clazz);
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
     * @return an List of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     */
    public abstract List<Float> read(String filename, Layer layer,
        int tIndex, int zIndex, Domain<HorizontalPosition> domain)
        throws IOException;

    /**
     * <p>Reads vertical section data from a file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.</p>
     * <p>This default implementation simply makes multiple calls to read().
     * Subclasses are encouraged to make more efficient implementations.</p>
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndices The indices along the vertical axis.
     * @param domain The list of real-world x-y points for which we need data.
     * In the case of a GetMap operation this will usually be a {@link HorizontalGrid}.
     * @return a List of floating point numbers for each elevation; each list
     * contains a value for each point in the {@code targetDomain}, in the same
     * order.  Missing values (e.g. land pixels in oceanography data} are
     * represented as nulls.
     * @throws IOException if an input/output exception occurred when reading data
     */
    public List<List<Float>> readVerticalSection(String filename, Layer layer, int tIndex,
        List<Integer> zIndices, Domain<HorizontalPosition> domain) throws IOException
    {
        if (zIndices == null) zIndices = Arrays.asList(-1);
        List<List<Float>> data = new ArrayList<List<Float>>(zIndices.size());
        for (int zIndex : zIndices) {
            data.add(this.read(filename, layer, tIndex, zIndex, domain));
        }
        return data;
    }

    /**
     * <p>Reads a timeseries of data from a file from a single xyz point.  This
     * method knows nothing about aggregation: it simply reads data from the
     * given file.  Missing values (e.g. land pixels in oceanography data) will
     * be represented by null.</p>
     * <p>If the provided Layer doesn't have a time axis then {@code tIndices}
     * must be a single-element list with value -1.  In this case the returned
     * "timeseries" of data will be a single data value. (TODO: make this more
     * sensible.)</p>
     * <p>This default implementation simply makes multiple calls to
     * {@link #read(java.lang.String, uk.ac.rdg.resc.ncwms.metadata.Layer, int,
     * int, uk.ac.rdg.resc.ncwms.datareader.HorizontalDomain) read()},
     * which is not very efficient because the same file may be opened and closed
     * multiple times (a particular problem when reading from OPeNDAP servers).
     * Subclasses are encouraged to override this with a more efficient method.</p>
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndices the indices along the time axis within this file
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param xy the horizontal position of the point
     * @return an array of floating-point data values, one for each point in
     * {@code tIndices}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     * @todo Validity checking on tIndices and layer.hasTAxis()?
     */
    public List<Float> readTimeseries(String filename, Layer layer,
        List<Integer> tIndices, int zIndex, HorizontalPosition xy)
        throws IOException {

        HorizontalDomain pointList = new HorizontalDomain(xy);
        List<Float> tsData = new ArrayList<Float>();
        for (int tIndex : tIndices) {
            tsData.add(this.read(filename, layer, tIndex, zIndex, pointList).get(0));
        }

        return tsData;
    }
    
    /**
     * Reads and returns the metadata for all the layers (i.e. variables) in the
     * given dataset.
     * @param dataset The dataset from which we'll read data
     * @return Map of layer IDs mapped to {@link LayerImpl} objects
     * @throws FileNotFoundException if the location does not match
     * any existing files on the server
     * @throws IOException if there was an error reading from the data source
     */
    public final Map<String, Layer> getAllLayers(Dataset ds)
        throws FileNotFoundException, IOException
    {
        Map<String, LayerImpl> scalarLayers = CollectionUtils.newLinkedHashMap();
        String location = ds.getLocation();
        if (WmsUtils.isOpendapLocation(location))
        {
            this.updateLayers(location, ds, scalarLayers);
        }
        else
        {
            // The location represents locally-held data so we do a glob expansion
            List<File> files = expandGlobExpression(location);
            if (files.size() == 0)
            {
                throw new FileNotFoundException(location + " does not match any files");
            }
            for (File file : files)
            {
                // Read the metadata from the file and update the Map.
                // TODO: only do this if the file's last modified date has changed?
                // This would require us to keep the previous metadata...
                this.updateLayers(file.getPath(), ds, scalarLayers);
            }
        }

        // We create a new Map that combines the scalar and vector layers
        Map<String, Layer> allLayers = CollectionUtils.newLinkedHashMap();
        // Add the scalar layers
        for (LayerImpl scalarLayer : scalarLayers.values())
        {
            allLayers.put(scalarLayer.getId(), scalarLayer);
        }
        // Now create the vector layers
        for (VectorLayer vecLayer : WmsUtils.findVectorLayers(scalarLayers.values()))
        {
            // We wrap the returned vector layer as a VectorLayerImpl to allow
            // some of the properties of the layer to be overridden by the
            // Variable objects, as for scalar layers
            allLayers.put(vecLayer.getId(), new VectorLayerImpl(ds, vecLayer));
        }

        return allLayers;
    }
    
    /**
     * Reads layer metadata from the given location and updates the map of layer
     * objects.
     * @param location the location of the specific file from which we'll read
     * data, ({@literal i.e.} one element resulting from the expansion of a glob
     * aggregation).
     * @param ds The dataset from which we're reading data
     */
    private void updateLayers(String location, Dataset ds, Map<String, LayerImpl> layers)
            throws IOException
    {
        for (CoverageMetadata lm : this.readLayerMetadata(location))
        {
            String layerId = lm.getId();
            LayerImpl layer = layers.get(layerId);
            if (layer == null)
            {
                // We need to create a new layer
                layer = new LayerImpl(lm, ds, this);
                layers.put(layerId, layer);
            }
            // Now we update the timesteps in the layer
            int i = 0;
            for (DateTime dt : lm.getTimeValues())
            {
                layer.addTimestepInfo(dt, location, i);
                i++;
            }
        }
    }

    /**
     * Reads metadata for each layer in the data at the given location.
     * @param location Full path to a single file, NcML file or OPENDAP dataset,
     * ({@literal i.e.} one element resulting from the expansion of a glob
     * aggregation).
     */
    protected abstract Collection<CoverageMetadata> readLayerMetadata(String location)
            throws IOException;

    /**
     * Expands a glob expression to give a List of absolute paths to files.  This
     * method recursively searches directories, allowing for glob expressions like
     * {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.
     * @return a a List of absolute paths to files matching the given glob
     * expression
     * @throws IllegalArgumentException if the glob expression does not represent
     * an absolute path (according to {@code new File(globExpression).isAbsolute()}).
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    public static List<File> expandGlobExpression(String globExpression)
    {
        // Check that the glob expression represents an absolute path.  Relative
        // paths would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        File globFile = new File(globExpression);
        if (!globFile.isAbsolute())
        {
            throw new IllegalArgumentException("Location must be an absolute path");
        }

        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
            // We "pop off" the last component of the glob pattern and place
            // it in the first component of the pathComponents List.  We therefore
            // ensure that the pathComponents end up in the right order.
            File parent = globFile.getParentFile();
            // For a top-level directory, getName() returns an empty string,
            // hence we use getPath() in this case
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
            globFile = parent;
        }

        // We must have at least two path components: one directory and one
        // filename or glob expression
        List<File> searchPaths = new ArrayList<File>();
        searchPaths.add(new File(pathComponents.get(0)));
        int i = 1; // Index of the glob path component

        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();

                    for (File match : dir.listFiles(globFilter))
                    {
                        newSearchPaths.add(match);
                    }
                }
            }
            // Next time we'll search based on these new matches and will use
            // the next globComponent
            searchPaths = newSearchPaths;
            i++;
        }

        // Now we've done all our searching, we'll only retain the files from
        // the list of search paths
        List<File> files = new ArrayList<File>();
        for (File path : searchPaths)
        {
            if (path.isFile()) files.add(path);
        }
        return files;
    }
}
