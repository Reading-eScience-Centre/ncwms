package uk.ac.rdg.resc.edal.ncwms;

import uk.ac.rdg.resc.edal.dataset.Dataset;
import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.util.Extents;

/**
 * 
 * 
 * @author Guy
 * @author Jon
 */
/*
 * TODO Write this properly
 */
//@Root(name = "variable")
public class NcwmsLayerMetadata {
//    @Attribute(name = "id")
    private String id;

//    @Attribute(name = "title", required = false)
    private String title = null;

//    @Attribute(name = "colorScaleRange", required = false)
    private String colorScaleRangeStr = null; // comma-separated pair of floats

//    @Attribute(name = "palette", required = false)
    private String paletteName = ColourPalette.DEFAULT_PALETTE_NAME;

//    @Attribute(name = "scaling", required = false)
    private String scaling = "linear"; // TODO Should be an enum really

//    @Attribute(name = "numColorBands", required = false)
    private int numColorBands = ColourPalette.MAX_NUM_COLOURS;

    private Extent<Float> colorScaleRange = null;

    private boolean logScaling = false;

    /**
     * Checks that the information in the XML is valid: specifically, checks the
     * colorScaleRange attribute. Also checks to see if the colour palette is
     * loaded: if not, reverts to the default palette.
     */
//    @Validate
    public void validate() /* throws PersistenceException */{
        /* Check the colour scale range */
        if (this.colorScaleRangeStr != null) {
            try {
                this.colorScaleRange = parseColorScaleRangeString(this.colorScaleRangeStr);
                /*
                 * Make sure we are using the right format for the string that
                 * will be serialized to the config file
                 */
                this.colorScaleRangeStr = formatColorScaleRange(this.colorScaleRange);
            } catch (Exception e) {
//                throw new PersistenceException("Invalid colorScaleRange attribute for variable "
//                        + this.id);
                e.printStackTrace();
            }
        }

        /*
         * Set the scaling of this variable TODO: it's a bit nasty to catch the
         * runtime exception, but this at least allows us to reuse the code in
         * setScaling().
         */
//        try {
        this.setScaling(this.scaling);
//        } catch (IllegalArgumentException iae) {
//            throw new PersistenceException(iae.getMessage());
//        }

        /* Check that the default number of color bands is within range */
        if (this.numColorBands > ColourPalette.MAX_NUM_COLOURS)
            this.numColorBands = ColourPalette.MAX_NUM_COLOURS;
    }

    private static Extent<Float> parseColorScaleRangeString(String colorScaleRangeStr)
            throws Exception {
        colorScaleRangeStr = colorScaleRangeStr.trim();

        // First try splitting on a space character
        String[] els = colorScaleRangeStr.split(" ");
        if (els.length == 2) {
            // Guard against the case in which commas are used as decimal
            // separators
            return parseColorScaleRangeStrings(els[0].replace(',', '.'), els[1].replace(',', '.'));
        } else if (els.length == 1) {
            // We are probably parsing a value from an old version of ncWMS that
            // used a comma as a delimiter
            els = colorScaleRangeStr.split(",");
            if (els.length == 2) {
                // The elements probably use full stops (periods) as the decimal
                // separator
                return parseColorScaleRangeStrings(els[0], els[1]);
            } else if (els.length == 4) {
                // We are probably in a locale where the comma is used as the
                // decimal separator
                // We must use full stops as the decimal separator in all
                // locales
                // because Float.parseFloat() is not localized
                return parseColorScaleRangeStrings(els[0] + "." + els[1], els[2] + "." + els[3]);
            }
        }
        // We can't parse the string
        throw new Exception();
    }

    private static Extent<Float> parseColorScaleRangeStrings(String minStr, String maxStr) {
        float min = Float.parseFloat(minStr);
        float max = Float.parseFloat(maxStr);
        if (max < min)
            max = min;
        return Extents.newExtent(min, max);
    }

    private static String formatColorScaleRange(Extent<Float> colorScaleRange) {
        // In previous versions of ncWMS we used a comma as a separator,
        // which caused problems in certain locales where a comma is used
        // as a decimal separator
        return Float.toString(colorScaleRange.getLow()) + " "
                + Float.toString(colorScaleRange.getHigh());
    }

    /**
     * Gets the ID of this variable, which is unique within the containing
     * {@link Dataset} and corresponds with {@link Feature#getId()}.
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the title that the administrator has set for this variable, or null
     * if no title has been set.
     * 
     * @return
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the default colour scale range for this variable, or null if not
     * set.
     * 
     * @return
     */
    public Extent<Float> getColorScaleRange() {
        return this.colorScaleRange;
    }

    public void setColorScaleRange(Extent<Float> colorScaleRange) {
        this.colorScaleRange = colorScaleRange;
        this.colorScaleRangeStr = colorScaleRange == null ? null
                : formatColorScaleRange(colorScaleRange);
    }

    public String getPaletteName() {
        return this.paletteName;
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }

    /**
     * Return true if this variable is to use logarithmic scaling by default
     */
    public boolean isLogScaling() {
        return this.logScaling;
    }

    /**
     * Returns the number of colour bands to use when creating images of this
     * variable.
     */
    public int getNumColorBands() {
        return this.numColorBands;
    }

    public void setNumColorBands(int numColorBands) {
        if (numColorBands < 0)
            this.numColorBands = 5;
        else if (numColorBands > ColourPalette.MAX_NUM_COLOURS)
            this.numColorBands = ColourPalette.MAX_NUM_COLOURS;
        else
            this.numColorBands = numColorBands;
    }

    /**
     * scaling must be "linear" or "logarithmic" or this will throw an
     * IllegalArgumentException
     * 
     * @throws IllegalArgumentException
     */
    public void setScaling(String scaling) {
        /* Get whether we want to use linear or log scaling by default */
        if (scaling.equalsIgnoreCase("linear")) {
            this.logScaling = false;
            this.scaling = scaling;
        } else if (scaling.equalsIgnoreCase("logarithmic")) {
            this.logScaling = true;
            this.scaling = scaling;
        } else {
            throw new IllegalArgumentException("Scaling must be \"linear\" or \"logarithmic\"");
        }
    }

}
