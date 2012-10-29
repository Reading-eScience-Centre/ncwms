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

package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.graphics.ColorPalette;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Contains those portions of a GetMap request that pertain to styling and
 * image generation.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetMapStyleRequest
{
    private String[] styles;
    private String imageFormat;
    private boolean transparent;
    private Color backgroundColour;
    private int opacity; // Opacity of the image in the range [0,100]
    private int numColourBands; // Number of colour bands to use in the image
    private Boolean logarithmic; // True if we're using a log scale, false if linear and null if not specified
    // These are the data values that correspond with the extremes of the
    // colour scale
    private Extent<Float> colorScaleRange;
    
    /**
     * Creates a new instance of GetMapStyleRequest from the given parameters
     * @throws WmsException if the request is invalid
     */
    public GetMapStyleRequest(RequestParams params) throws WmsException
    {
        // RequestParser replaces pluses with spaces: we must change back
        // to parse the format correctly
        String stylesStr = params.getMandatoryString("styles");
        if (stylesStr.trim().isEmpty()) this.styles = new String[0];
        else this.styles = stylesStr.split(",");
        
        this.imageFormat = params.getMandatoryString("format").replaceAll(" ", "+");

        this.transparent = params.getBoolean("transparent", false);
        
        try
        {
            String bgc = params.getString("bgcolor", "0xFFFFFF");
            if (bgc.length() != 8 || !bgc.startsWith("0x")) throw new Exception();
            // Parse the hexadecimal string, ignoring the "0x" prefix
            this.backgroundColour = new Color(Integer.parseInt(bgc.substring(2), 16));
        }
        catch(Exception e)
        {
            throw new WmsException("Invalid format for BGCOLOR");
        }
        
        this.opacity = params.getPositiveInt("opacity", 100);
        if (this.opacity > 100) this.opacity = 100;
        
        this.colorScaleRange = getColorScaleRange(params);
        this.numColourBands = getNumColourBands(params);
        this.logarithmic = isLogScale(params);
    }
    
    /**
     * Gets the ColorScaleRange object requested by the client
     */
    static Extent<Float> getColorScaleRange(RequestParams params) throws WmsException {
        String csr = params.getString("colorscalerange");
        if (csr == null || csr.equalsIgnoreCase("default")) {
            // The client wants the layer's default scale range to be used
            return null;
        } else if (csr.equalsIgnoreCase("auto")) {
            // The client wants the image to be scaled according to the image's
            // own min and max values (giving maximum contrast)
            return Extents.emptyExtent(Float.class);
        } else {
            // The client has specified an explicit colour scale range
            String[] scaleEls = csr.split(",");
            if (scaleEls.length == 0){
                return Extents.emptyExtent(Float.class);
            }
            Float scaleMin = Float.parseFloat(scaleEls[0]);
            Float scaleMax = Float.parseFloat(scaleEls[1]);
            if (scaleMin > scaleMax)
                throw new WmsException("Min > Max in COLORSCALERANGE");
            return Extents.newExtent(scaleMin, scaleMax);
        }
    }

    /**
     * Gets the number of colour bands requested by the client, or {@link ColorPalette#MAX_NUM_COLOURS} if none
     * has been set or the requested number was bigger than {@link ColorPalette#MAX_NUM_COLOURS}.
     * @param params The RequestParams object from the client.
     * @return the requested number of colour bands, or {@link ColorPalette#MAX_NUM_COLOURS} if none has been
     * set or the requested number was bigger than {@link ColorPalette#MAX_NUM_COLOURS}.
     * @throws WmsException if the client requested a negative number of colour
     * bands
     */
    static int getNumColourBands(RequestParams params) throws WmsException
    {
        int numColourBands = params.getPositiveInt("numcolorbands", ColorPalette.MAX_NUM_COLOURS);
        if (numColourBands > ColorPalette.MAX_NUM_COLOURS) numColourBands = ColorPalette.MAX_NUM_COLOURS;
        return numColourBands;
    }
    
    /**
     * Returns {@link Boolean#TRUE} if the client has requested a logarithmic scale,
     * {@link Boolean#FALSE} if the client has requested a linear scale,
     * or null if the client did not specify.
     * @throws WmsException if the client specified a value that is not
     * "true" or "false" (case not important).
     */
    static Boolean isLogScale(RequestParams params) throws WmsException
    {
        String logScaleStr = params.getString("logscale");
        if (logScaleStr == null) return null;
        else if (logScaleStr.equalsIgnoreCase("true")) return Boolean.TRUE;
        else if (logScaleStr.equalsIgnoreCase("false")) return Boolean.FALSE;
        else throw new WmsException("The value of LOGSCALE must be TRUE or FALSE (or can be omitted");
    }

    /**
     * @return array of style names, or an empty array if the user specified
     * "STYLES="
     */
    public String[] getStyles()
    {
        return styles;
    }

    public String getImageFormat()
    {
        return imageFormat;
    }

    /**
     * Returns {@link Boolean#TRUE} if the client has requested a logarithmic scale,
     * {@link Boolean#FALSE} if the client has requested a linear scale,
     * or null if the client did not specify.
     * @throws WmsException if the client specified a value that is not
     * "true" or "false" (case not important).
     */
    public Boolean isScaleLogarithmic()
    {
        return this.logarithmic;
    }

    public boolean isTransparent()
    {
        return transparent;
    }

    public Color getBackgroundColour()
    {
        return backgroundColour;
    }

    public int getOpacity()
    {
        return opacity;
    }

    /**
     * Gets the values that will correspond with the extremes of the colour 
     * scale.  Returns null if the client has not specified a scale range or
     * if the default scale range is to be used.  Returns an empty Range if
     * the client wants the image to be auto-scaled according to the image's own
     * min and max values.
     */
    public Extent<Float> getColorScaleRange()
    {
        return this.colorScaleRange;
    }

    public int getNumColourBands()
    {
        return numColourBands;
    }
    
}
