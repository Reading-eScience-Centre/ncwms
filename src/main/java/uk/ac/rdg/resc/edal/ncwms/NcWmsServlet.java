package uk.ac.rdg.resc.edal.ncwms;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.rdg.resc.edal.wms.GetMapParameters;
import uk.ac.rdg.resc.edal.wms.RequestParams;
import uk.ac.rdg.resc.edal.wms.WmsServlet;
import uk.ac.rdg.resc.edal.wms.exceptions.WmsException;

/**
 * Servlet implementation class NcWmsServlet
 */
public class NcWmsServlet extends WmsServlet implements Servlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see WmsServlet#WmsServlet()
     */
    public NcWmsServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            setCatalogue(new NcwmsCatalogue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /*-
     * Test URL for this servlet.
     * http://localhost:8080/ncWMS/wms?REQUEST=GetMap&VERSION=1.3.0&FORMAT=image/png&CRS=CRS:84&BBOX=-180,-90,180,90&WIDTH=1024&HEIGHT=512&LAYERS=foam/TMP&STYLES=boxfill/alg&COLORSCALERANGE=265,305&TIME=2010-01-30T12:00:00.000Z&ELEVATION=5&NUMCOLORBANDS=50
     */
}
