package uk.ac.rdg.resc.edal.ncwms.config;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.geotoolkit.referencing.crs.DefaultGeographicCRS;

import uk.ac.rdg.resc.edal.dataset.DatasetFactory;
import uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.graphics.style.MapImage;
import uk.ac.rdg.resc.edal.graphics.style.sld.StyleSLDParser;
import uk.ac.rdg.resc.edal.graphics.style.util.FeatureCatalogue;
import uk.ac.rdg.resc.edal.graphics.utils.PlottingDomainParams;
import uk.ac.rdg.resc.edal.ncwms.NcwmsCatalogue;

public class SLDParserTester {

	public static void main(String[] args) {
		try {
			// create the plotting domain
			final int WIDTH = 1024;
			final int HEIGHT = 512;
	        BoundingBox bbox = new BoundingBoxImpl(-180, -90, 180, 90, DefaultGeographicCRS.WGS84);
			PlottingDomainParams params = new PlottingDomainParams(WIDTH, HEIGHT, bbox, null, null, null, null, null);

			// load the datasets from a config file
			DatasetFactory.setDefaultDatasetFactoryClass(CdmGridDatasetFactory.class);
			NcwmsConfig config = NcwmsConfig.readFromFile(new File("C:\\Users\\Charles\\.ncWMS-edal\\config.xml"));

			// create the feature catalogue
			FeatureCatalogue catalogue = new NcwmsCatalogue(config);

			try {
			    Thread.sleep(3000);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			
			// process each XML file
			final File folder = new File("N:\\Documents\\SLDInput");
			for (final File fileEntry : folder.listFiles()) {
				String fileName = fileEntry.getName();
				if (fileName.endsWith("xml")) {
					System.out.println("Processing " + fileName);
					// create the image from the XML file
					MapImage mapImage = StyleSLDParser.createImage(fileEntry);
					BufferedImage image = mapImage.drawImage(params, catalogue);
					// write the image to a PNG file
					File outputfile = new File("N:\\Documents\\SLDOutput\\" + fileName.replace("xml", "png"));
					ImageIO.write(image, "png", outputfile);
					// create the legend
					BufferedImage legend = mapImage.getLegend(250);
					outputfile = new File("N:\\Documents\\SLDOutput\\" + fileName.replace(".xml", "_lgd.png"));
					ImageIO.write(legend, "png", outputfile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		NcwmsConfig.shutdown();
	}

}
