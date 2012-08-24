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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.graphics.formats.AviFormat;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.controller.AbstractWmsController.FeatureFactory;
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * Controller for generating screenshots from the Godiva2 site.
 * 
 * @author Guy Griffiths
 */
public class ScreenshotController extends MultiActionController {

    private FeatureFactory featureFactory;

    public void setFeatureFactory(FeatureFactory featureFactory) {
        this.featureFactory = featureFactory;
    }
    
    /**
     * Creates a screenshot, saves it on the server and returns the URL to the
     * screenshot.
     * 
     * @throws MetadataException
     *             (which is rendered as a JSON exception object)
     * @throws WmsException 
     * @throws IOException 
     */
    public ModelAndView createScreenshot(HttpServletRequest request, HttpServletResponse response)
            throws MetadataException, WmsException, IOException {
        String fullRequest = request.getRequestURL().toString();
        String servletUrl = fullRequest.substring(0,
                fullRequest.indexOf(request.getServletPath()) + 1);

        RequestParams params = new RequestParams(request.getParameterMap());

        if (params.getString("image") != null && params.getString("image").equalsIgnoreCase("true")) {
            String time = params.getString("time");
            BufferedImage image = drawScreenshot(params, servletUrl, time);

            response.setContentType("image/png");
            OutputStream output = response.getOutputStream();
            ImageIO.write(image, "png", output);
            return null;
        } else {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURL() + "?" + request.getQueryString() + "&image=true";
            out.println("<html><body>To save the image, right click and select \"Save As\"<br>"
                    + "Note: The image may take a long time to appear, depending on the speed of the data servers<br>"
                    + "<img src=\"" + url + "\"</body></html>");
            return null;
        }
    }
    
    public ModelAndView createAvi(HttpServletRequest request, HttpServletResponse response)
            throws MetadataException, WmsException, IOException {
        String fullRequest = request.getRequestURL().toString();
        String servletUrl = fullRequest.substring(0,
                fullRequest.indexOf(request.getServletPath()) + 1);
        
        RequestParams params = new RequestParams(request.getParameterMap());
        
        String timeString = params.getString("time");
        List<String> tValueStrings = new ArrayList<String>();
        
        Feature feature = featureFactory.getFeature(params.getMandatoryString("layer"));
        
        List<TimePosition> timeValues = AbstractWmsController.getTimeValues(timeString, feature);
        for (TimePosition timeValue : timeValues) {
            String tValueStr = null;
            if (timeValue != null) {
                tValueStr = TimeUtils.dateTimeToISO8601(timeValue);
            }
            tValueStrings.add(tValueStr);
        }
        
        List<BufferedImage> frames = new ArrayList<BufferedImage>();
        for(String time : tValueStrings) {
            frames.add(drawScreenshot(params, servletUrl, time));
        }
        int frameRate = params.getPositiveInt("frameRate", 24);
        AviFormat aviFormat = new AviFormat();
        
        response.setContentType(aviFormat.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename=ncwms-export.avi");
        final OutputStream output = response.getOutputStream();
        aviFormat.writeImage(frames, output, frameRate);
        return null;
    }
    
    
    
    private BufferedImage drawScreenshot(RequestParams params, String servletUrl, String time) throws WmsException, IOException {
        String baseLayerUrl = params.getString("baseUrl");
        String baseLayerNames = params.getString("baseLayers");
        if (baseLayerUrl == null || baseLayerNames == null || baseLayerUrl.equals("")
                || baseLayerNames.equals("")) {
            baseLayerUrl = "http://www2.demis.nl/wms/wms.ashx?WMS=BlueMarble&LAYERS=Earth%20Image";
        } else {
            baseLayerUrl += "&LAYERS=" + baseLayerNames;
        }
        
        int mapHeight = params.getPositiveInt("mapHeight", 384);
        int mapWidth = params.getPositiveInt("mapWidth", 512);
        
        int textSpace = 25;
        if (params.getString("layerTitle") != null) {
            String[] titleElements = params.getString("layerTitle").split(",");
            textSpace += 15 * titleElements.length;
        }
        if (time != null) {
            textSpace += 20;
        }
        if (params.getString("elevation") != null) {
            textSpace += 20;
        }

        BufferedImage image = new BufferedImage(mapWidth + 180, mapHeight + textSpace,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(Color.white);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setBackground(Color.white);

        // Draw labels on the image
        Font font = new Font("SansSerif", Font.BOLD, 16);
        g.setPaint(Color.black);
        g.setFont(font);
        int vPos = 20;
        int hPos = 10;

        String title = params.getString("layerTitle");
        if (title != null) {
            int indent = 0;
            String[] titleElements = title.split(",");
            if (titleElements.length >= 2) {
                for (int i = titleElements.length - 1; i > 0; i--) {
                    g.drawString(titleElements[i], hPos + indent, vPos);
                    vPos += 15;
                    g.drawString("\u21b3", hPos + indent + 8, vPos - 2);
                    indent += 20;
                    font = new Font("SansSerif", Font.BOLD, 14);
                    g.setFont(font);
                }
            }
            if (titleElements.length > 0) {
                g.drawString(titleElements[0], hPos + indent, vPos);
                vPos += 15;
            }
        }
        vPos += 5;

        font = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(font);
        if (time != null) {
            g.drawString("Time: " + time, hPos, vPos);
            vPos += 20;
        }

        String el = params.getString("elevation");
        if (el != null) {
            String depth = el;
            String u = params.getString("zUnits");
            String units = "";
            if (u != null)
                units = u;
            if (depth.startsWith("-"))
                g.drawString("Depth: " + el.substring(1) + units, hPos, vPos);
            else
                g.drawString("Elevation: " + el + units, hPos, vPos);
            vPos += 30;
        }

        Float minLon = Float.parseFloat(params.getString("bbox").split(",")[0]);
        Float maxLon = Float.parseFloat(params.getString("bbox").split(",")[2]);
        Float minLat = Float.parseFloat(params.getString("bbox").split(",")[1]);
        Float maxLat = Float.parseFloat(params.getString("bbox").split(",")[3]);
        Float lonRange = maxLon - minLon;

        if (minLon >= -180 && maxLon <= 180) {
            BufferedImage im = getImage(params, minLon, minLat, maxLon, maxLat, mapWidth,
                    mapHeight, baseLayerUrl, time);
            g.drawImage(im, 0, textSpace, null);
        } else if (minLon < -180 && maxLon <= 180) {
            int lefWidth = (int) (mapWidth * (-180 - minLon) / (lonRange));
            BufferedImage im = getImage(params, minLon + 360, minLat, 180f, maxLat, lefWidth,
                    mapHeight, baseLayerUrl, time);
            g.drawImage(im, 0, textSpace, null);
            im = getImage(params, -180f, minLat, maxLon, maxLat, mapWidth - lefWidth,
                    mapHeight, baseLayerUrl, time);
            g.drawImage(im, lefWidth, textSpace, null);
        } else if (minLon >= -180 && maxLon > 180) {
            int rightWidth = (int) (mapWidth * (maxLon - 180f) / (lonRange));
            BufferedImage im = getImage(params, minLon, minLat, 180f, maxLat, mapWidth
                    - rightWidth, mapHeight, baseLayerUrl, time);
            g.drawImage(im, 0, textSpace, null);
            im = getImage(params, -180f, minLat, maxLon - 360, maxLat, rightWidth, mapHeight,
                    baseLayerUrl, time);
            g.drawImage(im, mapWidth - rightWidth, textSpace, null);
        } else if (minLon < -180 && maxLon > 180) {
            int leftWidth = (int) (mapWidth * (-180 - minLon) / (lonRange));
            BufferedImage im = getImage(params, minLon + 360, minLat, 180f, maxLat, leftWidth,
                    mapHeight, baseLayerUrl, time);
            g.drawImage(im, 0, textSpace, null);

            int rightWidth = (int) (mapWidth * (maxLon - 180f) / (lonRange));
            im = getImage(params, -180f, minLat, maxLon - 360, maxLat, rightWidth, mapHeight,
                    baseLayerUrl, time);
            g.drawImage(im, mapWidth - rightWidth, textSpace, null);

            im = getImage(params, -180f, minLat, 180f, maxLat, mapWidth - leftWidth
                    - rightWidth, mapHeight, baseLayerUrl, time);
            g.drawImage(im, leftWidth, textSpace, null);
        }

        URL url = createWMSUrl(params, false, minLon, minLat, maxLon, maxLat, mapWidth,
                mapHeight, servletUrl, time);
        BufferedImage wmsLayer;
        if (url != null) {
            wmsLayer = ImageIO.read(url);
            g.drawImage(wmsLayer, 0, textSpace, null);
        }

        BufferedImage colorBar;
        url = createColorbarUrl(params, mapHeight, servletUrl);
        if (url != null) {
            InputStream in = null;
            try {
                URLConnection conn = url.openConnection();
                in = conn.getInputStream();
                colorBar = ImageIO.read(in);
                g.drawImage(colorBar, mapWidth, textSpace, mapWidth + 30,
                        textSpace + mapHeight, 0, 0, 1, colorBar.getHeight(), null);
            } finally {
                if (in != null)
                    in.close();
            }
        }

        String sr = params.getString("scaleRange");
        int topPos = textSpace + 10;
        int botPos = topPos + mapHeight - 10;
        int midPos = botPos + (topPos - botPos) / 2;
        hPos = mapWidth + 40;

        String u = params.getString("units");
        if (u != null && !u.equals("")) {
            g.drawString("Units: " + u, hPos, midPos);
        }

        font = new Font("SansSerif", Font.PLAIN, 11);
        g.setFont(font);
        if (sr != null) {
            String[] scaleVals = sr.split(",");
            float topVal = Float.parseFloat(scaleVals[1]);
            float botVal = Float.parseFloat(scaleVals[0]);
            String log = params.getString("logScale");
            double highMedVal;
            double lowMedVal;
            if (log != null && Boolean.parseBoolean(log)) {
                double aThird = Math.log(topVal / botVal) / 3.0;
                highMedVal = Math.exp(Math.log(botVal) + 2 * aThird);
                lowMedVal = Math.exp(Math.log(botVal) + aThird);
            } else {
                highMedVal = botVal + 2 * (topVal - botVal) / 3;
                lowMedVal = botVal + (topVal - botVal) / 3;
            }
            int highMedPos = botPos + 2 * (topPos - botPos) / 3;
            int lowMedPos = botPos + (topPos - botPos) / 3;
            DecimalFormat format = new DecimalFormat("##0.00");
            g.drawString(format.format(topVal), hPos, topPos);
            g.drawString(format.format(highMedVal), hPos, highMedPos);
            g.drawString(format.format(lowMedVal), hPos, lowMedPos);
            g.drawString(format.format(botVal), hPos, botPos);
        }

        return image;
    }

    private BufferedImage getImage(RequestParams params, Float minLon, Float minLat, Float maxLon,
            Float maxLat, int width, int height, String bgUrl, String time) throws IOException {
        BufferedImage image = null;
        URL baseUrl = createWMSUrl(params, true, minLon, minLat, maxLon, maxLat, width, height,
                bgUrl, time);
        image = ImageIO.read(baseUrl);
        ImageIO.write(image, "png", new File("/home/guy/00wtf.png"));
        return image;
    }

    private URL createWMSUrl(RequestParams params, boolean baseLayer, Float minLon, Float minLat,
            Float maxLon, Float maxLat, int width, int height, String baseWmsUrl, String time) {
        StringBuilder url = new StringBuilder();
        if (baseLayer) {
            Pattern p = Pattern.compile(" ");
            if (baseWmsUrl != null) {
                Matcher m = p.matcher(baseWmsUrl);
                if (m != null)
                    baseWmsUrl = m.replaceAll("%20");
            }
            url.append(baseWmsUrl);
        } else {
            url.append(baseWmsUrl);
            url.append(params.getString("dataset") + "?SERVICE=WMS&LAYERS="
                    + params.getString("layer"));
            String style = params.getString("style");
            String palette = params.getString("palette");
            if (style != null && palette != null)
                url.append("&STYLES=" + style + "/" + palette);
            String scaleRange = params.getString("scaleRange");
            if (scaleRange != null)
                url.append("&COLORSCALERANGE=" + scaleRange);
            String numColorBands = params.getString("numColorBands");
            if (numColorBands != null)
                url.append("&NUMCOLORBANDS=" + numColorBands);
            if (time != null)
                url.append("&TIME=" + time);
            String elevation = params.getString("elevation");
            if (elevation != null)
                url.append("&ELEVATION=" + elevation);
        }

        url.append("&TRANSPARENT=true");
        url.append("&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&FORMAT=image/png&WIDTH=" + width
                + "&HEIGHT=" + height);
        url.append("&BBOX=" + minLon + "," + minLat + "," + maxLon + "," + maxLat);
        url.append("&SRS=" + params.getString("crs"));
        try {
            return new URL(url.toString().replaceAll(" ", "%20"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private URL createColorbarUrl(RequestParams params, int mapHeight, String baseUrl) {
        String url = baseUrl + params.getString("dataset")
                + "?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=" + mapHeight;
        String numColorBands = params.getString("numColorBands");
        if (numColorBands != null)
            url += "&NUMCOLORBANDS=" + numColorBands;
        String palette = params.getString("palette");
        if (palette != null)
            url += "&PALETTE=" + palette;
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
