package uk.ac.rdg.resc.ncwms.gwt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private Map<String, String> outputMap;
	
	@Override
	public void init() throws ServletException {
	    super.init();
	    outputMap = loadProperties(getServletContext());
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
	    super.init(config);
	    outputMap = loadProperties(getServletContext());
	}
	

	protected static Map<String, String> loadProperties(ServletContext context) {
	    Map<String, String> returnMap = new HashMap<String, String>();
	    Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(context.getRealPath("config.properties"))));
            for(Object key : props.keySet()){
                if(key instanceof String){
                    returnMap.put((String)key, props.getProperty((String)key));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnMap;
    }

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder ret = new StringBuilder("{");
        for(String key : outputMap.keySet()){
            ret.append("\""+key+"\":\""+outputMap.get(key)+"\",");
        }
        ret.append("}");
        response.setContentType("application/json");
        response.getWriter().write(ret.toString());
	}
}
