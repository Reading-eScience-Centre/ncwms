package uk.ac.rdg.resc.ncwms.gwt.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String url = URLDecoder.decode(request.getQueryString(), "UTF-8");
		InputStream in = null;
		OutputStream out = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			response.setContentType(conn.getContentType());
			response.setContentLength(conn.getContentLength());
			in = conn.getInputStream();
			out = response.getOutputStream();
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) >= 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}
}
