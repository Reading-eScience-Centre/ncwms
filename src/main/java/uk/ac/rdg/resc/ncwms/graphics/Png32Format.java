/*
 * Copyright (c) 2008 The University of Reading
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

import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes 32-bit (ARGB) PNG images using the ImageIO class.  Only one instance of this class
 * will ever be created, so this class contains no member variables to ensure
 * thread safety.  Some browsers have problems with {@link PngFormat indexed PNGs},
 * and some clients find it easier to merge 32-bit images with others.
 * @author jdb
 */
public class Png32Format extends PngFormat
{
    /**
     * Protected default constructor to prevent direct instantiation.
     */
    protected Png32Format() {}
    
    @Override
    public String getMimeType()
    {
        return "image/png;mode=32bit";
    }

    @Override
    public void writeImage(List<BufferedImage> frames, OutputStream out) throws IOException
    {
        List<BufferedImage> frames32bit = new ArrayList<BufferedImage>(frames.size());
        for (BufferedImage source : frames)
        {
            frames32bit.add(convertARGB(source));
        }
        super.writeImage(frames32bit, out);
    }

    /**
     * Converts the source image to 32-bit colour (ARGB).
     * @param source the source image to convert
     * @return a copy of the source image with a 32-bit (ARGB) colour depth
     */
    private static BufferedImage convertARGB(BufferedImage source)
    {
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp convertOp = new ColorConvertOp(
            source.getColorModel().getColorSpace(),
            dest.getColorModel().getColorSpace(),
            null
        );
        convertOp.filter(source, dest);
        return dest;
    }
}
