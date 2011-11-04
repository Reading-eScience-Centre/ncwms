package uk.ac.rdg.resc.ncwms.gwt.server;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.ncwms.gwt.shared.CaseInsensitiveParameterMap;

public class ScreenshotServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private int vPos = 0;
    private int hPos = 0;
    private int mapHeight = 384;
    private int mapWidth = 512;
    private String wmsUrl = null;
    private String wmsLayer = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Map<String,String> props = ConfigServlet.loadProperties(getServletContext());
        
        if(props.containsKey("baseMapUrl"))
            wmsUrl = props.get("baseMapUrl");
        else
            wmsUrl = "http://www2.demis.nl/wms/wms.ashx";
            
        if(props.containsKey("baseMapLayer"))
            wmsLayer = props.get("baseMapLayer");
        else
            wmsLayer = "Earth Image&WMS=BlueMarble";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CaseInsensitiveParameterMap params = CaseInsensitiveParameterMap.getMapFromArray(request.getParameterMap());
        if(params.get("image") != null && params.get("image").equalsIgnoreCase("true")){
            mapHeight = Integer.parseInt(params.get("mapHeight"));
            mapWidth = Integer.parseInt(params.get("mapWidth"));
    
            BufferedImage myOceanLogo = ImageIO.read(new File(this.getServletContext().getRealPath(
                    "img/myocean-logo-small.jpg")));
    
            BufferedImage image = new BufferedImage(mapWidth + 180, mapHeight + myOceanLogo.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setPaint(Color.white);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setBackground(Color.white);
    
            // Get MyOcean logo and draw on image?
            g.drawImage(myOceanLogo, 0, 0, null);
    
            // Draw labels on the image
            Font font = new Font("SansSerif", Font.BOLD, 16);
            g.setPaint(Color.black);
            g.setFont(font);
            vPos = 20;
            hPos = myOceanLogo.getWidth() + 10;
    
            String ds = params.get("datasetName");
            if (ds != null) {
                g.drawString(ds, hPos, vPos);
                vPos += 15;
            }
    
            font = new Font("SansSerif", Font.BOLD, 14);
            g.setFont(font);
            String lyr = params.get("layerName");
            g.drawString("\u21b3", hPos + 8, vPos - 2);
            if (lyr != null) {
                g.drawString(lyr, hPos + 20, vPos);
                vPos += 20;
            }
            String t = params.get("time");
            if (t != null) {
                g.drawString("Time: " + t, hPos, vPos);
                vPos += 20;
            }
    
            String el = params.get("elevation");
            if (el != null) {
                String depth = el;
                String u = params.get("zUnits");
                String units = "";
                if (u != null)
                    units = u;
                if (depth.startsWith("-"))
                    g.drawString("Depth: " + el.substring(1) + units, hPos, vPos);
                else
                    g.drawString("Elevation: " + el + units, hPos, vPos);
                vPos += 30;
            }
    
            Float minLon = Float.parseFloat(params.get("bbox").split(",")[0]);
            Float maxLon = Float.parseFloat(params.get("bbox").split(",")[2]);
            Float minLat = Float.parseFloat(params.get("bbox").split(",")[1]);
            Float maxLat = Float.parseFloat(params.get("bbox").split(",")[3]);
            Float lonRange = maxLon - minLon;
    
            if (minLon >= -180 && maxLon <= 180) {
                BufferedImage im = getImage(params, minLon, minLat, maxLon, maxLat, mapWidth, mapHeight);
                g.drawImage(im, 0, myOceanLogo.getHeight(), null);
            } else if (minLon < -180 && maxLon <= 180) {
                int lefWidth = (int) (mapWidth * (-180-minLon)/(lonRange));
                BufferedImage im = getImage(params, minLon + 360, minLat, 180f, maxLat, lefWidth, mapHeight);
                g.drawImage(im, 0, myOceanLogo.getHeight(), null);
                im = getImage(params, -180f, minLat, maxLon, maxLat, mapWidth - lefWidth, mapHeight);
                g.drawImage(im, lefWidth, myOceanLogo.getHeight(), null);
            } else if (minLon >= -180 && maxLon > 180) {
                int rightWidth = (int) (mapWidth * (maxLon - 180f)/(lonRange));
                BufferedImage im = getImage(params, minLon, minLat, 180f, maxLat, mapWidth - rightWidth, mapHeight);
                g.drawImage(im, 0, myOceanLogo.getHeight(), null);
                im = getImage(params, -180f, minLat, maxLon - 360, maxLat, rightWidth, mapHeight);
                g.drawImage(im, mapWidth - rightWidth, myOceanLogo.getHeight(), null);
            } else if (minLon < -180 && maxLon > 180) {
                int leftWidth = (int) (mapWidth * (-180-minLon)/(lonRange));
                BufferedImage im = getImage(params, minLon + 360, minLat, 180f, maxLat, leftWidth, mapHeight);
                g.drawImage(im, 0, myOceanLogo.getHeight(), null);
                
                int rightWidth = (int) (mapWidth * (maxLon - 180f)/(lonRange));
                im = getImage(params, -180f, minLat, maxLon - 360, maxLat, rightWidth, mapHeight);
                g.drawImage(im, mapWidth - rightWidth, myOceanLogo.getHeight(), null);
                
                im = getImage(params, -180f, minLat, 180f, maxLat, mapWidth - leftWidth - rightWidth, mapHeight);
                g.drawImage(im, leftWidth, myOceanLogo.getHeight(), null);
            }
    
            BufferedImage wmsLayer;
            URL url = createWMSUrl(params, false, minLon, minLat, maxLon, maxLat, mapWidth, mapHeight);
            if (url != null) {
                InputStream in = null;
                try {
                    URLConnection conn = url.openConnection();
                    in = conn.getInputStream();
                    wmsLayer = ImageIO.read(in);
                    g.drawImage(wmsLayer, 0, myOceanLogo.getHeight(), null);
                } finally {
                    if (in != null)
                        in.close();
                }
            }
    
            BufferedImage colorBar;
            url = createColorbarUrl(params);
            if (url != null) {
                InputStream in = null;
                try {
                    URLConnection conn = url.openConnection();
                    in = conn.getInputStream();
                    colorBar = ImageIO.read(in);
                    g.drawImage(colorBar, mapWidth, myOceanLogo.getHeight(), mapWidth + 30, myOceanLogo.getHeight()
                            + mapHeight, 0, 0, 1, colorBar.getHeight(), null);
                } finally {
                    if (in != null)
                        in.close();
                }
            }
    
            String sr = params.get("scaleRange");
            int topPos = myOceanLogo.getHeight() + 10;
            int botPos = topPos + mapHeight - 10;
            int midPos = botPos + (topPos - botPos) / 2;
            hPos = mapWidth + 40;
    
            String u = params.get("units");
            if (u != null) {
                g.drawString("Units: " + u, hPos, midPos);
            }
    
            font = new Font("SansSerif", Font.PLAIN, 11);
            g.setFont(font);
            if (sr != null) {
                String[] scaleVals = sr.split(",");
                float topVal = Float.parseFloat(scaleVals[1]);
                float botVal = Float.parseFloat(scaleVals[0]);
                String log = params.get("logScale");
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
                DecimalFormat format = new DecimalFormat("##0.0");
                g.drawString(format.format(topVal), hPos, topPos);
                g.drawString(format.format(highMedVal), hPos, highMedPos);
                g.drawString(format.format(lowMedVal), hPos, lowMedPos);
                g.drawString(format.format(botVal), hPos, botPos);
            }
    
            response.setContentType("image/png");
            OutputStream output = response.getOutputStream();
            ImageIO.write(image, "png", output);
        } else {
            PrintWriter out = response.getWriter();
            String url = request.getRequestURL()+"?"+request.getQueryString()+"&image=true";
            out.println("<html><body>To save the image, right click and select \"Save As\"<br>"+
                        "Note: The image may take a long time to appear, depending on the speed of the data servers<br>"+
                        "<img src=\""+url+"\"</body></html>");
        }
    }

    private BufferedImage getImage(CaseInsensitiveParameterMap params, Float minLon, Float minLat, Float maxLon,
            Float maxLat, int width, int height) throws IOException {
        URL url = createWMSUrl(params, true, minLon, minLat, maxLon, maxLat, width, height);
        if (url != null) {
            InputStream in = null;
            try {
                URLConnection conn = url.openConnection();
                in = conn.getInputStream();
                BufferedImage im = ImageIO.read(in);
                return im;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null)
                    in.close();
            }
        }
        return null;
    }

    private URL createWMSUrl(CaseInsensitiveParameterMap params, boolean baseLayer, Float minLon, Float minLat,
            Float maxLon, Float maxLat, int width, int height) {
        StringBuilder url = new StringBuilder();
        if (baseLayer) {
            url.append(wmsUrl);
//            url.append(params.get("baseUrl"));
//            String layers = params.get("layers");
            String layers = wmsLayer;
            Pattern p = Pattern.compile(" ");
            if (layers != null) {
                Matcher m = p.matcher(layers);
                if (m != null)
                    layers = m.replaceAll("%20");
                if(url.toString().contains("?"))
                    url.append("&");
                else
                    url.append("?");
                url.append("LAYERS=" + layers);
            }
        } else {
            url.append(params.get("dataset") + "?SERVICE=WMS&LAYERS=" + params.get("layer"));
            String style = params.get("style");
            String palette = params.get("palette");
            if (style != null && palette != null)
                url.append("&STYLES=" + style+"/"+palette);
            String scaleRange = params.get("scaleRange");
            if (scaleRange != null)
                url.append("&COLORSCALERANGE=" + scaleRange);
            String numColorBands = params.get("numColorBands");
            if (numColorBands != null)
                url.append("&NUMCOLORBANDS=" + numColorBands);
            String time = params.get("time");
            if (time != null)
                url.append("&TIME=" + time);
            String elevation = params.get("elevation");
            if (elevation != null)
                url.append("&ELEVATION=" + elevation);
        }

        url.append("&TRANSPARENT=true");
        url.append("&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&FORMAT=image/png&WIDTH=" + width + "&HEIGHT="
                + height);
        url.append("&BBOX=" + minLon+","+minLat+","+maxLon+","+maxLat);
        url.append("&SRS=" + params.get("crs"));

        try {
            return new URL(url.toString());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private URL createColorbarUrl(CaseInsensitiveParameterMap params) {
        String url = params.get("dataset") + "?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=" + mapHeight;
        String numColorBands = params.get("numColorBands");
        if (numColorBands != null)
            url += "&NUMCOLORBANDS=" + numColorBands;
        String palette = params.get("palette");
        if (palette != null)
            url += "&PALETTE=" + palette;
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
