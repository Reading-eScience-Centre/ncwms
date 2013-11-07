/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
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
 ******************************************************************************/

package uk.ac.rdg.resc.edal.ncwms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a servlet which performs digest authentication. Servlets that wish to
 * use authentication should subclass this, call the setPassword method, and
 * call the authenticate() method on any HTTP methods which require authentication.
 * 
 * This was based on the code from:
 * https://gist.github.com/usamadar/2912088
 * 
 * @author Guy Griffiths
 */
public abstract class NcwmsDigestAuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(NcwmsDigestAuthServlet.class);

    private String authMethod = "auth";
    private String userName = "admin";
    private String password = null;
    private String realm = "Login required to administer ncWMS";

    public String nonce;
    public ScheduledExecutorService nonceRefreshExecutor;

    /**
     * Default constructor to initialize stuff
     * 
     */
    public NcwmsDigestAuthServlet() throws IOException, Exception {
        nonce = calculateNonce();
        nonceRefreshExecutor = Executors.newScheduledThreadPool(1);

        nonceRefreshExecutor.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                nonce = calculateNonce();
            }
        }, 1, 5, TimeUnit.MINUTES);
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void destroy() {
        super.destroy();
        nonceRefreshExecutor.shutdownNow();
    }

    protected boolean authenticate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isBlank(authHeader) || StringUtils.isBlank(password)) {
            response.addHeader("WWW-Authenticate", getAuthenticateHeader());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            if (StringUtils.isBlank(password)) {
                log.error("Admin password has not been set - admin pages are not accessible");
            }
            return false;
        } else {
            if (authHeader.startsWith("Digest")) {
                /*
                 * Parse the values of the Authentication header into a hashmap
                 */
                HashMap<String, String> headerValues = parseHeader(authHeader);

                String method = request.getMethod();

                String ha1 = DigestUtils.md5Hex(userName + ":" + realm + ":" + password);

                String qop = headerValues.get("qop");

                String reqURI = headerValues.get("uri");

                String ha2 = DigestUtils.md5Hex(method + ":" + reqURI);

                /*
                 * We have disabled qop - auth-int here, since this consumes the
                 * input stream, which we don't want for post requests
                 * 
                 * TODO perhaps this could be handled better?
                 */
//                if (!StringUtils.isBlank(qop) && qop.equals("auth-int")) {
//                    String entityBodyMd5 = DigestUtils.md5Hex(requestBody);
//                    ha2 = DigestUtils.md5Hex(method + ":" + reqURI + ":" + entityBodyMd5);
//                } else {
//                }

                String serverResponse;

                if (StringUtils.isBlank(qop)) {
                    serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + ha2);

                } else {
                    String nonceCount = headerValues.get("nc");
                    String clientNonce = headerValues.get("cnonce");

                    serverResponse = DigestUtils.md5Hex(ha1 + ":" + nonce + ":" + nonceCount + ":"
                            + clientNonce + ":" + qop + ":" + ha2);

                }
                String clientResponse = headerValues.get("response");

                if (!serverResponse.equals(clientResponse)) {
                    response.addHeader("WWW-Authenticate", getAuthenticateHeader());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        " This Servlet only supports Digest Authorization");
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns a short description of the servlet.
     * 
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This Servlet Implements The HTTP Digest Auth as per RFC2617";
    }

    /**
     * Gets the Authorization header string minus the "AuthType" and returns a
     * hashMap of keys and values
     * 
     * @param headerString
     * @return
     */
    private HashMap<String, String> parseHeader(String headerString) {
        // seperte out the part of the string which tells you which Auth scheme
        // is it
        String headerStringWithoutScheme = headerString.substring(headerString.indexOf(" ") + 1)
                .trim();
        HashMap<String, String> values = new HashMap<String, String>();
        String keyValueArray[] = headerStringWithoutScheme.split(",");
        for (String keyval : keyValueArray) {
            if (keyval.contains("=")) {
                String key = keyval.substring(0, keyval.indexOf("="));
                String value = keyval.substring(keyval.indexOf("=") + 1);
                values.put(key.trim(), value.replaceAll("\"", "").trim());
            }
        }
        return values;
    }

    private String getAuthenticateHeader() {
        String header = "";

        header += "Digest realm=\"" + realm + "\",";
        if (!StringUtils.isBlank(authMethod)) {
            header += "qop=" + authMethod + ",";
        }
        header += "nonce=\"" + nonce + "\",";
        header += "opaque=\"" + getOpaque(realm, nonce) + "\"";

        return header;
    }

    /**
     * Calculate the nonce based on current time-stamp upto the second, and a
     * random seed
     */
    public String calculateNonce() {
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
        String fmtDate = f.format(d);
        Random rand = new Random(100000);
        Integer randomInt = rand.nextInt();
        return DigestUtils.md5Hex(fmtDate + randomInt.toString());
    }

    private String getOpaque(String domain, String nonce) {
        return DigestUtils.md5Hex(domain + nonce);
    }

    /**
     * Returns the request body as String
     * 
     * @param request
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unused")
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }
        String body = stringBuilder.toString();
        return body;
    }
}
