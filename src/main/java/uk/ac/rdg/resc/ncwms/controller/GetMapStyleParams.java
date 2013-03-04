package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;

import javax.xml.bind.JAXBException;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.graphics.style.ColourPalette;
import uk.ac.rdg.resc.edal.graphics.style.StyleXMLParser;
import uk.ac.rdg.resc.edal.graphics.style.StyleXMLParser.ColorAdapter;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ArrowLayer;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourMap;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourScale;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ContourLayer;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.ContourLayer.ContourLineStyle;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.Drawable;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.Image;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.PatternScale;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.RasterLayer;
import uk.ac.rdg.resc.edal.graphics.style.datamodel.impl.StippleLayer;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

public class GetMapStyleParams {

    private String[] layers;
    private String[] styles;

    private String xmlStyle;

    private boolean transparent = false;
    private Color backgroundColour = Color.white;
    private int opacity = 100; // Opacity of the image in the range [0,100]
    private int numColourBands = 254; // Number of colour bands to use in the image
    private boolean logarithmic = false; // True if we're using a log scale

    private Extent<Float> colorScaleRange;
    private boolean autoScale = false; // True if we're using a log scale

    private boolean xmlSpecified = false;
    
    private static ColorAdapter cAdapter = new ColorAdapter();

    public GetMapStyleParams(RequestParams params) throws WmsException {

        String layersStr = params.getString("layers");
        if (layersStr == null || layersStr.trim().isEmpty()) {
            layers = null;
        } else {
            layers = layersStr.split(",");
        }

        String stylesStr = params.getString("styles");
        if (stylesStr == null) {
            styles = null;
        } else if(stylesStr.trim().isEmpty()) {
            styles = new String[0];
        } else {
            styles = stylesStr.split(",");
        }
        
        xmlStyle = params.getString("XML_STYLE");
        
        if(xmlStyle == null) {
            xmlSpecified = false;
            if(layers == null) {
                throw new WmsException("You must specify either XML_STYLE or LAYERS and STYLES");
            }
            if (styles.length != layers.length && styles.length != 0) {
                throw new WmsException("You must request exactly one STYLE per layer, "
                        + "or use the default style for each layer with STYLES=");
            }
        } else {
            xmlSpecified = true;
        }

        this.transparent = params.getBoolean("transparent", false);

        try {
            String bgc = params.getString("bgcolor", "0xFFFFFF");
            if ((bgc.length() != 8 && bgc.length() != 10) || !bgc.startsWith("0x"))
                throw new Exception();
            // Parse the hexadecimal string, ignoring the "0x" prefix
            backgroundColour = cAdapter.unmarshal(bgc);
        } catch (Exception e) {
            throw new WmsException("Invalid format for BGCOLOR");
        }

        opacity = params.getPositiveInt("opacity", 100);
        if (opacity > 100) {
            opacity = 100;
        }

        colorScaleRange = getColorScaleRange(params);
        if(colorScaleRange == null || colorScaleRange.isEmpty()) {
            autoScale = true;
        }

        logarithmic = params.getBoolean("logscale", false);

        numColourBands = params.getPositiveInt("numcolorbands", ColourPalette.MAX_NUM_COLOURS);
        if (numColourBands > ColourPalette.MAX_NUM_COLOURS) {
            numColourBands = ColourPalette.MAX_NUM_COLOURS;
        }
    }

    /**
     * Gets the ColorScaleRange object requested by the client
     */
    private Extent<Float> getColorScaleRange(RequestParams params) throws WmsException {
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
            if (scaleEls.length == 0) {
                return Extents.emptyExtent(Float.class);
            }
            Float scaleMin = Float.parseFloat(scaleEls[0]);
            Float scaleMax = Float.parseFloat(scaleEls[1]);
            if (scaleMin > scaleMax)
                throw new WmsException("Min > Max in COLORSCALERANGE");
            return Extents.newExtent(scaleMin, scaleMax);
        }
    }

    public Image getImageGenerator() throws WmsException {
        if(xmlStyle != null) {
            try {
                return StyleXMLParser.deserialise(xmlStyle);
            } catch (JAXBException e) {
                e.printStackTrace();
                throw new WmsException("Problem parsing XML style.  Check logs for stack trace");
            }
        } else {
            
        }
        
        if(layers.length > 1) {
            throw new WmsException("Only 1 layer may be requested");
        }
        
        String layerName = layers[0];
        String style = "default/default";
        
        if(styles.length != 0) {
            style = styles[0];
        }
        
        String[] styleParts = style.split("/");
        if(styleParts.length == 0) {
            throw new WmsException("Style should be of the form STYLE/PALETTE ()");
        }
        String plotStyleName = styleParts[0];
        String paletteName = styleParts.length > 1 ? styleParts[1] : "default";
        
        Image image = new Image();
        
        Drawable layer = null;
        
        if(plotStyleName.equalsIgnoreCase("default")) {
            /*
             * TODO For the time being, we assume that "boxfill" is the default,
             * but this really depends on the feature type.
             */
            System.out.println("Need to handle DEFAULT styles better");
            plotStyleName = "boxfill";
        }
        
        if(plotStyleName.equalsIgnoreCase("boxfill")) {
            /*
             * Generate a RasterLayer
             */
            ColourScale scaleRange = new ColourScale(colorScaleRange.getLow(),
                    colorScaleRange.getHigh(), logarithmic);
            ColourMap colourPalette = new ColourMap(Color.black, Color.black, new Color(0, true),
                    paletteName, numColourBands);
            ColourScheme colourScheme = new ColourScheme(scaleRange, colourPalette);
            layer = new RasterLayer(layerName, colourScheme);
        } else if(plotStyleName.equalsIgnoreCase("contour")) {
            layer = new ContourLayer(layerName, new ColourScale(colorScaleRange.getLow(),
                    colorScaleRange.getHigh(), logarithmic), autoScale, numColourBands,
                    Color.black, 1, ContourLineStyle.SOLID, true);
        } else if(plotStyleName.equalsIgnoreCase("stipple")) {
            PatternScale scale = new PatternScale(numColourBands, colorScaleRange.getLow(), colorScaleRange.getHigh(), logarithmic);
            layer = new StippleLayer(layerName, scale);
        } else if(plotStyleName.equalsIgnoreCase("arrow")) {
            layer = new ArrowLayer(layerName, 8, Color.black);
        }
        
        if(layer == null) {
            throw new WmsException("Do not know how to plot the style: "+plotStyleName);
        }
        
        image.getLayers().add(layer);
        
        return image;
    }
    
    public boolean isTransparent() {
        return transparent;
    }
    
    /**
     * Return the opacity of the image as a percentage
     */
    public int getOpacity() {
        return opacity;
    }
    
    public int getNumLayers() {
        return layers.length;
    }
    
    public boolean isXmlDefined() {
        return xmlSpecified;
    }
}
