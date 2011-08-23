/*
 * Copyright (c) 2009 The University of Reading
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;

/**
 * Controller for generating screenshots from the Godiva2 site.
 * @author Abdul Rauf Butt
 * @author Jon Blower
 */
public class ScreenshotController extends MultiActionController
{
    private static final Logger log = LoggerFactory.getLogger(ScreenshotController.class);

    /** We only need one random number generator */
    private static final Random RANDOM = new Random();

    /** Directory where the screenshots will be stored (full path) */
    private File screenshotCache;

    /**
     * Called by Spring to initialize the controller: this method creates a
     * directory for screenshots in the ncWMS working directory.
     * @throws Exception if the directory for the screenshots could not be created.
     */
    public void init() throws Exception
    {
        if (this.screenshotCache.exists())
        {
            if (this.screenshotCache.isDirectory())
            {
                log.debug("Screenshots directory already exists");
            }
            else
            {
                throw new Exception(this.screenshotCache.getPath() + " already exists but is not a directory");
            }
        }
        else
        {
            if (this.screenshotCache.mkdirs())
            {
                log.debug("Screenshots directory " + this.screenshotCache.getPath()
                    + " created");
            }
            else
            {
                throw new Exception("Screenshots directory " + this.screenshotCache.getPath()
                    + " could not be created");
            }
        }
    }

    private static final class BoundingBox
    {
        float minXValue;
        float maxXValue;
        float minYValue;
        float maxYValue;
    }

    /**
     * Creates a screenshot, saves it on the server and returns the URL to the
     * screenshot.
     * @throws MetadataException (which is rendered as a JSON exception object)
     */
    public ModelAndView createScreenshot(HttpServletRequest request, HttpServletResponse response) throws MetadataException
    {
        log.debug("Called createScreenshot");
        try
        {
            return createScreenshot(request);
        }
        catch (Exception e)
        {
            log.error("Error creating screenshot", e);
            throw new MetadataException(e);
        }
    }

    private ModelAndView createScreenshot(HttpServletRequest request) throws Exception
    {
		String title = request.getParameter("title").replaceAll("&gt;", ">"); //"Hello World";
		String time = request.getParameter("time"); //"null";
		String elevation = request.getParameter("elevation"); //"null";
        String units = request.getParameter("units");
		String upperValue = request.getParameter("upperValue"); //1.0967412;
        String twoThirds = request.getParameter("twoThirds");
        String oneThird = request.getParameter("oneThird");
		String lowerValue = request.getParameter("lowerValue"); //-0.9546131;
        boolean isLatLon = "true".equalsIgnoreCase(request.getParameter("latLon"));

        // Find the URL of this server from the request
        StringBuffer requestUrl = request.getRequestURL();
        String server = requestUrl.substring(0, requestUrl.indexOf("screenshots"));

        String BGparam = request.getParameter("urlBG");
        String FGparam = request.getParameter("urlFG");
        String urlStringPalette = request.getParameter("urlPalette");

        if(BGparam == null || FGparam == null || urlStringPalette == null) {
            // TODO: better error handling
            throw new Exception("Null BG, FG or palette param");
        }

        String urlStringBG = BGparam;
        String urlStringFG = server + FGparam;

        BoundingBox BBOX = new BoundingBox();
        String[] serverName = urlStringBG.split("\\?");
        StringBuffer result = buildURL(serverName[1], serverName[0], "BG", BBOX);
        serverName = urlStringFG.split("\\?");
        StringBuffer resultFG = buildURL(serverName[1], serverName[0], "FG", BBOX);

        float minX1 = 0;
        float minX2 = 0;
        float maxX1 = 0;
        float maxX2 = 0;
        int WIDTH_OF_BG_IMAGE1 = 0;
        int WIDTH_OF_BG_IMAGE2 = 0;
        int START_OF_IMAGE3 = 0;
        int START_OF_IMAGE4 = 0;
        final int WIDTH_TOTAL = 512;
        final int HEIGHT_TOTAL = 400;
        final int WIDTH_OF_FINAL_IMAGE = 650;
        final int HEIGHT_OF_FINAL_IMAGE = 480;
        String URL1 = "";
        String URL2 = "";
        float coverage = 0;

        boolean isGT180 = false;
        boolean isReplicate = false;

        String bboxParam = "&BBOX=" + BBOX.minXValue + "," + BBOX.minYValue + "," + BBOX.maxXValue + "," + BBOX.maxYValue;

        if(isLatLon && (Float.compare(BBOX.minXValue,-180)<0 )) // means we need to generate two URLs
		{

			if( (Float.compare(BBOX.minXValue,-180) < 0 ) )
			{
				minX1 = -180; //minXValue;
                if (Float.compare(BBOX.maxXValue,180) > 0) // It will only happen for the case of zoom out: when maxX > 180
                {
                    maxX1 = BBOX.maxXValue - 360;
                    isReplicate = true;
                }
                else{
                    maxX1 = BBOX.maxXValue;
                }
				minX2 = BBOX.minXValue + 360;
				maxX2 = +180;

                float rangeofImg1 =  Math.abs(maxX1 - minX1);
                float rangeofImg2 =  Math.abs(maxX2 - minX2);
                float totalSpan = rangeofImg1 + rangeofImg2;

                // in normal viewing case, the span is 360
                // with first zoom-in, the span becomes 180
                // with first zoom out, the spam becoms 720
                if (isReplicate) {
                    coverage =  (rangeofImg1/(totalSpan*2));
                } else {
                    coverage =  (rangeofImg1/totalSpan);
                }

                WIDTH_OF_BG_IMAGE1 = Math.round(((float) (WIDTH_TOTAL)*coverage));   // RHS Image

                if (isReplicate)
                {
                    WIDTH_OF_BG_IMAGE2 =  (WIDTH_TOTAL/2) - WIDTH_OF_BG_IMAGE1;
                    START_OF_IMAGE3 = WIDTH_OF_BG_IMAGE1 + WIDTH_OF_BG_IMAGE2;
                    START_OF_IMAGE4 = START_OF_IMAGE3 + WIDTH_OF_BG_IMAGE2;
                }
                else{
                    WIDTH_OF_BG_IMAGE2 =  WIDTH_TOTAL - WIDTH_OF_BG_IMAGE1;          // LHS Image
                }
			}

        String bboxParam1 = "&BBOX=" + minX1 + "," + BBOX.minYValue + "," + maxX1 + "," + BBOX.maxYValue;
        String bboxParam2 = "&BBOX=" + minX2 + "," + BBOX.minYValue + "," + maxX2 + "," + BBOX.maxYValue;

        URL1 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE1 + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam1;
        URL2 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE2 + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam2;
        isGT180 = true;
        }

        else
        {
            URL1 = result.toString() + "WIDTH=" + WIDTH_TOTAL + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam;
        }


        String URL3 = resultFG.toString() + "WIDTH=" + WIDTH_TOTAL + "&HEIGHT=" + HEIGHT_TOTAL + bboxParam;

		BufferedImage bimgBG1 = null;
        BufferedImage bimgBG2 = null;

		BufferedImage bimgFG = null;
		BufferedImage bimgPalette = null;
        if(isGT180){
            bimgBG1 = downloadImage(URL1); //(path[0]);  // right-hand side
            bimgBG2 = downloadImage(URL2); //(path[1]);  // left-hand side
        }
        else{
            bimgBG1 = downloadImage(URL1);
        }
        bimgFG = downloadImage(URL3);
        bimgPalette = downloadImage(urlStringPalette);//(path[2]);

        /* Prepare the final Image */
        int type = BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(WIDTH_OF_FINAL_IMAGE, HEIGHT_OF_FINAL_IMAGE, type);
        Graphics2D g = image.createGraphics();

        // The Font and Text
        Font font = new Font("SansSerif", Font.BOLD, 12);
        g.setFont(font);
        g.setBackground(Color.white);
        g.fillRect(0, 0, WIDTH_OF_FINAL_IMAGE, HEIGHT_OF_FINAL_IMAGE);

        g.setColor(Color.black);
        g.drawString(title, 0, 10);
        if (time != null) {
            g.drawString("Time: " + time, 0, 30);
        }
        if (elevation != null) {
            g.drawString(elevation, 0, 50);
        }

        // Now draw the image
        if(isGT180){
            g.drawImage(bimgBG1, null, WIDTH_OF_BG_IMAGE2, 60);
            g.drawImage(bimgBG2, null, 0, 60);
            if(isReplicate) {
                g.drawImage(bimgBG2, null, START_OF_IMAGE3, 60);
                g.drawImage(bimgBG1, null, START_OF_IMAGE4, 60);
            }
        }
        else{
            g.drawImage(bimgBG1, null, 0, 60);
        }
        g.drawImage(bimgFG, null, 0, 60);
        g.drawImage(bimgPalette, WIDTH_TOTAL, 60, 45, HEIGHT_TOTAL, null);

        g.drawString(upperValue, 560, 63);
        g.drawString(twoThirds, 560, 192);
        if (units != null) {
            g.drawString("Units: " + units, 560, 258);
        }
        g.drawString(oneThird, 560, 325);
        g.drawString(lowerValue, 560, 460);

        g.dispose();

        String imageName = "snapshot" + RANDOM.nextLong() + System.currentTimeMillis() + ".png";
        ImageIO.write(image, "png", getImageFile(imageName));	// write the image to the screenshots directory
        String screenshotUrl = "screenshots/getScreenshot?img="+ imageName;
        return new ModelAndView("showScreenshotUrl", "url", screenshotUrl);
    }

    private static StringBuffer buildURL(String url, String serverName, String type, BoundingBox bb) {

        String[] params = url.split("&");
        StringBuffer result = new StringBuffer();
        result.append(serverName);
        result.append("?");
        String separator = "&";

        for (int i=0; i< params.length; i++){
            if(params[i].startsWith("BBOX")){
                String tempParam = params[i];
                String bbValues = tempParam.substring(5); // to remove BBOX= from the start of the string
                String [] bbox = bbValues.split(",");
                if(type.equals("BG")==true){
                    bb.minXValue = (float) Double.parseDouble(bbox[0]);
                    bb.maxXValue = (float) Double.parseDouble(bbox[2]);
                    bb.minYValue = (float) Double.parseDouble(bbox[1]);
                    bb.maxYValue = (float) Double.parseDouble(bbox[3]);
                }
                for (int indx=0; indx< bbox.length; indx++){
                    //out.print("bbox param " + indx + ": " + bbox[indx]);
                }
                continue;
            }
            if(params[i].startsWith("WIDTH") || params[i].startsWith("HEIGHT")){
                continue;
            }

            result.append(params[i]);
            result.append(separator);
        }
        return result;
    }

	private static BufferedImage downloadImage(String path) throws IOException {
	    return ImageIO.read(new URL(path));
	}

    /**
     * Downloads a screenshot from the server
     * @param request
     * @param response
     */
    public void getScreenshot(HttpServletRequest request, HttpServletResponse response)
        throws Exception
    {
        log.debug("Called getScreenshot with params {}", request.getParameterMap());
        String imageName = request.getParameter("img");
        if (imageName == null) throw new Exception("Must give a screenshot image name");
        File screenshotFile = this.getImageFile(imageName);
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new FileInputStream(screenshotFile);
            byte[] imageBytes = new byte[1024]; // read 1MB at a time
            response.setContentType("image/png");
            out = response.getOutputStream();
            int n;
            do
            {
                n = in.read(imageBytes);
                if (n >= 0)
                {
                    out.write(imageBytes);
                }
            } while (n >= 0);
        }
        catch (FileNotFoundException fnfe)
        {
            // rethrow this exception
            throw new Exception(imageName + " not found");
        }
        finally
        {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    private File getImageFile(String imageName)
    {
        return new File(this.screenshotCache, imageName);
    }

    /**
     * Called by Spring to inject the location of the cache of screenshot images
     */
    public void setScreenshotCache(File screenshotCache)
    {
        this.screenshotCache = screenshotCache;
    }

}
