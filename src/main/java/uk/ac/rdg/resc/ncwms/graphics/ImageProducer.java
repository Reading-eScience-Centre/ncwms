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

package uk.ac.rdg.resc.ncwms.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * An object that is used to render data into images. Instances of this class
 * must be created through the {@link Builder}.
 * 
 * @author Jon Blower
 */
public final class ImageProducer {
    private static final Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    public static enum Style {
        BOXFILL, VECTOR
    };

    private Style style;
    // Width and height of the resulting picture
    private int picWidth;
    private int picHeight;
    private boolean transparent;
    private int opacity;
    private int numColourBands;
    private boolean logarithmic; // True if the colour scale is to be
                                 // logarithmic,
    // false if linear
    private Color bgColor;
    private ColorPalette colorPalette;

    /**
     * Colour scale range of the picture. An {@link Range#isEmpty() empty Range}
     * means that the picture will be auto-scaled.
     */
    private Extent<Float> scaleRange;

    /**
     * The length of arrows in pixels, only used for vector plots
     */
    private float arrowLength = 10.0f;

    // set of rendered images, ready to be turned into a picture
    private List<BufferedImage> renderedFrames = new ArrayList<BufferedImage>();

    // If we need to cache the frame data and associated labels (we do this if
    // we have to auto-scale the image) this is where we put them.
    private static final class Components {
        private final List<Float> x;
        private final List<Float> y;

        public Components(List<Float> x, List<Float> y) {
            this.x = x;
            this.y = y;
        }

        public List<Float> getMagnitudes() {
            return y == null ? x : WmsUtils.getMagnitudes(x, y);
        }
    }

    private List<Components> frameData;

    private List<String> labels;

    /** Prevents direct instantiation */
    private ImageProducer() {
    }

    public BufferedImage getLegend(Layer layer) {
        return colorPalette.createLegend(numColourBands, layer.getTitle(), layer.getUnits(),
                logarithmic, scaleRange);
    }

    public int getPicWidth() {
        return picWidth;
    }

    public int getPicHeight() {
        return picHeight;
    }

    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Adds a frame of scalar data to this ImageProducer. If the data cannot yet
     * be rendered into a BufferedImage, the data and label are stored.
     */
    public void addFrame(List<Float> data, String label) {
        addFrame(data, null, label);
    }

    /**
     * Adds a frame of vector data to this ImageProducer. If the data cannot yet
     * be rendered into a BufferedImage, the data and label are stored.
     */
    public void addFrame(List<Float> xData, List<Float> yData, String label) {
        logger.debug("Adding frame with label {}", label);
        Components comps = new Components(xData, yData);
        if (scaleRange.getLow() == null && scaleRange.getHigh() == null) {
            logger.debug("Auto-scaling, so caching frame");
            if (frameData == null) {
                frameData = new ArrayList<Components>();
                labels = new ArrayList<String>();
            }
            frameData.add(comps);
            labels.add(label);
        } else {
            logger.debug("Scale is set, so rendering image");
            renderedFrames.add(createImage(comps, label));
        }
    }

    /**
     * Returns the {@link IndexColorModel} which will be used by this
     * ImageProducer
     */
    public IndexColorModel getColorModel() {
        return colorPalette.getColorModel(numColourBands, opacity, bgColor, transparent);
    }

    /**
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set. The scale must be set before calling
     * this method.
     */
    private BufferedImage createImage(Components comps, String label) {
        // Create the pixel array for the frame
        byte[] pixels = new byte[picWidth * picHeight];
        // We get the magnitude of the input data (takes care of the case
        // in which the data are two components of a vector)
        List<Float> magnitudes = comps.getMagnitudes();
        for (int i = 0; i < pixels.length; i++) {
            // The image coordinate system has the vertical axis increasing
            // downward, but the data's coordinate system has the vertical axis
            // increasing upwards. The method below flips the axis
            int dataIndex = getDataIndex(i);
            pixels[i] = (byte) getColourIndex(magnitudes.get(dataIndex));
        }

        // Create a ColorModel for the image
        ColorModel colorModel = getColorModel();

        // Create the Image
        DataBuffer buf = new DataBufferByte(pixels, pixels.length);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(picWidth, picHeight);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);

        // Add the label to the image
        // TODO: colour needs to change with different palettes!
        if (label != null && !label.equals("")) {
            Graphics2D gfx = (Graphics2D) image.getGraphics();
            gfx.setPaint(new Color(0, 0, 143));
            gfx.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
            gfx.setPaint(new Color(255, 151, 0));
            gfx.drawString(label, 10, image.getHeight() - 5);
        }

        if (style == Style.VECTOR) {
            // We superimpose direction arrows on top of the background
            // TODO: only do this for lat-lon projections!
            Graphics2D g = image.createGraphics();
            // TODO: control the colour of the arrows with an attribute
            // Must be part of the colour palette (here we use the colour
            // for out-of-range values)
            g.setColor(Color.BLACK);

            logger.debug("Drawing vectors, length = {} pixels", arrowLength);
            for (int i = 0; i < picWidth; i += Math.ceil(arrowLength * 1.2)) {
                for (int j = 0; j < picHeight; j += Math.ceil(arrowLength * 1.2)) {
                    int dataIndex = getDataIndex(i, j);
                    Float eastVal = comps.x.get(dataIndex);
                    Float northVal = comps.y.get(dataIndex);
                    if (eastVal != null && northVal != null) {
                        double angle = Math.atan2(northVal.doubleValue(), eastVal.doubleValue());
                        // Calculate the end point of the arrow
                        double iEnd = i + arrowLength * Math.cos(angle);
                        // Screen coordinates go down, but north is up, hence
                        // the minus sign
                        double jEnd = j - arrowLength * Math.sin(angle);
                        // logger.debug("i={}, j={}, dataIndex={}, east={}, north={}",
                        // new Object[]{i, j, dataIndex, data[0][dataIndex],
                        // data[1][dataIndex]});
                        // Draw a dot representing the data location
                        g.fillOval(i - 2, j - 2, 4, 4);
                        // Draw a line representing the vector direction and
                        // magnitude
                        g.setStroke(new BasicStroke(1));
                        g.drawLine(i, j, (int) Math.round(iEnd), (int) Math.round(jEnd));
                        // Draw the arrow on the canvas
                        // drawArrow(g, i, j, (int)Math.round(iEnd),
                        // (int)Math.round(jEnd), 2);
                    }
                }
            }
        }

        return image;
    }

    /**
     * Calculates the index of the data point in a data array that corresponds
     * with the given index in the image array, taking into account that the
     * vertical axis is flipped.
     */
    private int getDataIndex(int imageIndex) {
        int imageI = imageIndex % picWidth;
        int imageJ = imageIndex / picWidth;
        return getDataIndex(imageI, imageJ);
    }

    /**
     * Calculates the index of the data point in a data array that corresponds
     * with the given index in the image array, taking into account that the
     * vertical axis is flipped.
     */
    private int getDataIndex(int imageI, int imageJ) {
        int dataJ = picHeight - imageJ - 1;
        int dataIndex = dataJ * picWidth + imageI;
        return dataIndex;
    }

    /**
     * @return the colour index that corresponds to the given value
     */
    public int getColourIndex(Float value) {
        if (value == null) {
            return numColourBands; // represents a background pixel
        } else if (!scaleRange.contains(value)) {
            return numColourBands + 1; // represents an out-of-range pixel
        } else {
            float scaleMin = scaleRange.getLow();
            float scaleMax = scaleRange.getHigh();
            double min = logarithmic ? Math.log(scaleMin) : scaleMin;
            double max = logarithmic ? Math.log(scaleMax) : scaleMax;
            double val = logarithmic ? Math.log(value) : value;
            double frac = (val - min) / (max - min);
            // Compute and return the index of the corresponding colour
            int index = (int) (frac * numColourBands);
            // For values very close to the maximum value in the range, this
            // index might turn out to be equal to this.numColourBands due to
            // rounding error. In this case we subtract one from the index to
            // ensure that such pixels are not displayed as background pixels.
            if (index == numColourBands)
                index--;
            return index;
        }
    }

    /**
     * Gets the frames as BufferedImages, ready to be turned into a picture or
     * animation. This is called just before the picture is due to be created,
     * so subclasses can delay creating the BufferedImages until all the data
     * has been extracted (for example, if we are auto-scaling an animation, we
     * can't create each individual frame until we have data for all the frames)
     * 
     * @return List of BufferedImages
     */
    public List<BufferedImage> getRenderedFrames() {
        setScale(); // Make sure the colour scale is set before proceeding
        // We render the frames if we have not done so already
        if (frameData != null) {
            logger.debug("Rendering image frames...");
            for (int i = 0; i < frameData.size(); i++) {
                logger.debug("    ... rendering frame {}", i);
                Components comps = frameData.get(i);
                renderedFrames.add(createImage(comps, labels.get(i)));
            }
        }
        return renderedFrames;
    }

    /**
     * Makes sure that the scale is set: if we are auto-scaling, this reads all
     * of the data we have stored to find the extremes. If the scale has already
     * been set, this does nothing.
     */
    private void setScale() {
        if (scaleRange.getLow() == null && scaleRange.getHigh() == null) {
            Float scaleMin = null;
            Float scaleMax = null;
            logger.debug("Setting the scale automatically");
            // We have a cache of image data, which we use to generate the
            // colour scale
            for (Components comps : frameData) {
                // We only use the first component if this is a vector quantity
                Extent<Float> range = Extents.findMinMax(comps.x);
                // TODO: could move this logic to the Range/Ranges class
                if (range.getLow() != null && range.getHigh() != null) {
                    if (scaleMin == null || range.getLow().compareTo(scaleMin) < 0) {
                        scaleMin = range.getLow();
                    }
                    if (scaleMax == null || range.getHigh().compareTo(scaleMax) > 0) {
                        scaleMax = range.getHigh();
                    }
                }
            }
            scaleRange = Extents.newExtent(scaleMin, scaleMax);
        }
    }

    public int getOpacity() {
        return opacity;
    }

    /**
     * Builds an ImageProducer
     * 
     * @todo make error handling and validity-checking more consistent
     */
    public static final class Builder {
        private int picWidth = -1;
        private int picHeight = -1;
        private boolean transparent = false;
        private int opacity = 100;
        private int numColourBands = ColorPalette.MAX_NUM_COLOURS;
        private Boolean logarithmic = null;
        private Color bgColor = Color.WHITE;
        private Extent<Float> scaleRange = null;
        private Style style = null;
        private ColorPalette colorPalette = null;

        /**
         * Sets the style to be used. If not set or if the parameter is null,
         * {@link Style#BOXFILL} will be used
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the colour palette. If not set or if the parameter is null, the
         * default colour palette will be used. {@see ColorPalette}
         */
        public Builder palette(ColorPalette palette) {
            colorPalette = palette;
            return this;
        }

        /** Sets the width of the picture (must be set: there is no default) */
        public Builder width(int width) {
            if (width < 0)
                throw new IllegalArgumentException();
            picWidth = width;
            return this;
        }

        /** Sets the height of the picture (must be set: there is no default) */
        public Builder height(int height) {
            if (height < 0)
                throw new IllegalArgumentException();
            picHeight = height;
            return this;
        }

        /**
         * Sets whether or not background pixels should be transparent (defaults
         * to false)
         */
        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        /** Sets the opacity of the picture, from 0 to 100 (default 100) */
        public Builder opacity(int opacity) {
            if (opacity < 0 || opacity > 100)
                throw new IllegalArgumentException();
            this.opacity = opacity;
            return this;
        }

        /**
         * Sets the colour scale range. If not set (or if set to null), the min
         * and max values of the data will be used.
         */
        public Builder colourScaleRange(Extent<Float> scaleRange) {
            this.scaleRange = scaleRange;
            return this;
        }

        /**
         * Sets the number of colour bands to use in the image, from 0 to 254
         * (default 254)
         */
        public Builder numColourBands(int numColourBands) {
            if (numColourBands < 0 || numColourBands > ColorPalette.MAX_NUM_COLOURS) {
                throw new IllegalArgumentException();
            }
            this.numColourBands = numColourBands;
            return this;
        }

        /**
         * Sets whether or not the colour scale is to be spaced logarithmically
         * (default is false)
         */
        public Builder logarithmic(Boolean logarithmic) {
            this.logarithmic = logarithmic;
            return this;
        }

        /**
         * Sets the background colour, which is used only if transparent==false,
         * for background pixels. Defaults to white. If the passed-in color is
         * null, it is ignored.
         */
        public Builder backgroundColour(Color bgColor) {
            if (bgColor != null)
                this.bgColor = bgColor;
            return this;
        }

        /**
         * Checks the fields for internal consistency, then creates and returns
         * a new ImageProducer object.
         * 
         * @throws IllegalStateException
         *             if the builder cannot create a valid ImageProducer object
         */
        public ImageProducer build() {
            if (picWidth < 0 || picHeight < 0) {
                throw new IllegalStateException("picture width and height must be >= 0");
            }

            ImageProducer ip = new ImageProducer();
            ip.picWidth = picWidth;
            ip.picHeight = picHeight;
            ip.opacity = opacity;
            ip.transparent = transparent;
            ip.bgColor = bgColor;
            ip.numColourBands = numColourBands;
            ip.style = style == null ? Style.BOXFILL : style;
            ip.colorPalette = colorPalette == null ? ColorPalette.get(null) : colorPalette;
            ip.logarithmic = logarithmic == null ? false : logarithmic.booleanValue();
            // Signifies auto-scaling
            Extent<Float> emptyRange = Extents.emptyExtent();
            ip.scaleRange = scaleRange == null ? emptyRange : scaleRange;

            return ip;
        }
    }
}
