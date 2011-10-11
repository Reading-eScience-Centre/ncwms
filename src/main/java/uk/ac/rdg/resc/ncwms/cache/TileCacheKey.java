package uk.ac.rdg.resc.ncwms.cache;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import org.geotoolkit.referencing.CRS;

import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;

/**
 * Key that is used to identify a particular data array (tile) in a
 * {@link TileCache}. TileCacheKeys are immutable.
 * 
 * @see TileCache
 * @author Jon Blower
 */
public class TileCacheKey implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String layerId; // The unique identifier of this layer
    private String crsCode; // The CRS code used for this tile
    private double[] bbox; // Bounding box as [minX, minY, maxX, maxY]
    private int width; // Width of tile in pixels
    private int height; // Height of tile in pixels
    private String filepath; // Full path to the file containing the data
    private long lastModified = 0; // The time at which the file was last
                                   // modified
    // (used to check for changes to the file). Not
    // used for OPeNDAP datasets.
    private long fileSize = 0; // The size of the file in bytes
    // (used to check for changes to the file)
    // Not used for OPeNDAP datasets.
    private int tIndex; // The t index of this tile in the file
    private int zIndex; // The z index of this tile in the file
    private long datasetLastModified = 0; // The time (in ms since the epoch) at
                                          // which
    // the relevant Dataset was modified (not used
    // for local files)

    // TileCacheKeys are immutable so these properties can be stored to save
    // repeated recomputation:
    // TODO: make transient or generate dynamically. Improve algorithms!
    private String str; // String representation of this key
    private int hashCode; // Hash code for this key

    /**
     * Creates a key for the storing and locating of data arrays in a TileCache.
     * If the filepath represents a local file (including an NcML file) then we
     * store the last modified time of the file and the file size so that
     * thelayer key won't match the cache if the contents of the file change. If
     * the filepath represents an NcML file or OPeNDAP aggregation we store the
     * last-modified time of the relevant
     * {@link uk.ac.rdg.resc.ncwms.config.Dataset Dataset} object, meaning that
     * when the metadata for the Dataset is reloaded all the Keys relevant to
     * this Dataset become invalid. See the Javadoc comments for
     * {@link TileCache}.
     * 
     * @throws IllegalArgumentException
     *             if the given filepath exists on the server but does not
     *             represent a file (e.g. it is a directory)
     */
    public TileCacheKey(String filepath, Feature feature, RegularGrid grid, int tIndex, int zIndex, Dataset dataset) {
        this.layerId = feature.getId();
        this.setGrid(grid);
        this.filepath = filepath;
        File f = new File(filepath);
        if (f.exists()) {
            if (f.isFile()) {
                // This is a local data file or an NcML file
                this.lastModified = f.lastModified();
                this.fileSize = f.length();
            } else {
                throw new IllegalArgumentException(filepath + " exists but is not a valid file on this server");
            }
        }
        if (WmsUtils.isOpendapLocation(filepath) || WmsUtils.isNcmlAggregation(filepath)) {
            // This is an OPeNDAP dataset or NcML aggregation, so we need
            // to store the last-modified time of the relevant Dataset
            this.datasetLastModified = dataset.getLastUpdateTime().getValue();
        }
        this.tIndex = tIndex;
        this.zIndex = zIndex;

        // Create a String representation of this key
        StringBuffer buf = new StringBuffer();
        buf.append(this.layerId);
        buf.append(",");
        buf.append(this.crsCode);
        buf.append(",{");
        for (double bboxVal : this.bbox) {
            buf.append(bboxVal);
            buf.append(",");
        }
        buf.append("},");
        buf.append(this.width);
        buf.append(",");
        buf.append(this.height);
        buf.append(",");
        buf.append(this.filepath);
        buf.append(",");
        buf.append(this.lastModified);
        buf.append(",");
        buf.append(this.fileSize);
        buf.append(",");
        buf.append(this.tIndex);
        buf.append(",");
        buf.append(this.zIndex);
        buf.append(",");
        buf.append(this.datasetLastModified);

        // Create and store the string representations and hash code for this
        // key. The key is immutable so these will not change.
        // TODO: create the hash code in a better way
        this.str = buf.toString();
        this.hashCode = this.str.hashCode();
    }

    /**
     * Returns an integer code that is used by ehcache to test for equality of
     * TileCacheKeys. Two different TileCacheKeys can theoretically generate the
     * same hash code, although this is unlikely. Ehcache uses this to reduce
     * the search space before calling {@link #equals} to check for definite
     * equality. (Note that just implementing equals() will not do!)
     */
    @Override
    public int hashCode() {
        return this.hashCode;
    }

    /**
     * @return a string representation of this key
     */
    @Override
    public String toString() {
        return this.str;
    }

    /**
     * This is called by ehcache after the hashcodes of the objects have been
     * compared for equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TileCacheKey))
            return false;

        TileCacheKey other = (TileCacheKey) o;

        // For speed we start with the cheap comparisons (i.e. not the string
        // comparisons) and the things that are most likely to be different.
        return this.tIndex == other.tIndex && this.zIndex == other.zIndex && this.fileSize == other.fileSize
                && this.lastModified == other.lastModified && this.datasetLastModified == other.datasetLastModified
                && this.width == other.width && this.height == other.height && this.crsCode.equals(other.crsCode)
                && this.filepath.equals(other.filepath) && this.layerId.equals(other.layerId)
                && Arrays.equals(this.bbox, other.bbox);
    }

    /**
     * Sets the properties of this Key that relate to the horizontal grid of the
     * image. Some CRSs have multiple, equivalent, codes (e.g. CRS:84 and
     * EPSG:4326). Furthermore, for CRSs with longitude axes, some
     * apparently-different bounding boxes are functionally equivalent (e.g. 360
     * degrees = 0 degrees). This method sets the CRS and bbox to standard
     * values to ensure that data are retrieved accurately and without
     * unnecessary repetition.
     */
    @SuppressWarnings("deprecation")
    private void setGrid(RegularGrid grid) {
        this.width = grid.getXAxis().size();
        this.height = grid.getYAxis().size();
        BoundingBox boundingBox = grid.getCoordinateExtent();
        this.bbox = new double[] { boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMaxX(),
                boundingBox.getMaxY() };
        if (GISUtils.isWgs84LonLat(grid.getCoordinateReferenceSystem())) {
            // Make sure we always use the same code for lat-lon projections
            this.crsCode = "CRS:841";
            // Constrain longitudes to range [-180,180] to canonicalise them
            this.bbox[0] = GISUtils.constrainLongitude180(this.bbox[0]);
            this.bbox[2] = GISUtils.constrainLongitude180(this.bbox[2]);
        } else {
            // This should work for all CRS objects we obtain from the
            // Geotoolkit
            // CRS factories (see
            // http://lists.osgeo.org/pipermail/geotoolkit/2010-April/000347.html)
            this.crsCode = CRS.getDeclaredIdentifier(grid.getCoordinateReferenceSystem());
        }
    }
}
