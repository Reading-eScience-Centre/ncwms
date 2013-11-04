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

package uk.ac.rdg.resc.edal.ncwms.config;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class NcwmsServerInfo {
    @XmlElement(name = "title")
    private String title = "ncWMS Server";
    @XmlElement(name = "allowFeatureInfo")
    private boolean allowFeatureInfo = true;
    @XmlElement(name = "maxImageWidth")
    private int maxImageWidth = 1024;
    @XmlElement(name = "maxImageHeight")
    private int maxImageHeight = 1024;
    @XmlElement(name = "abstract")
    private String description = "";
    /* Comma-separated keywords list */
    @XmlElement(name = "keywords")
    @XmlJavaTypeAdapter(KeywordsAdapter.class)
    private List<String> keywords = null;
    @XmlElement(name = "url")
    private String url = "";
    @XmlElement(name = "adminpassword", required=true)
    private String adminPassword= "ncWMS";
    @XmlElement(name = "allowglobalcapabilities")
    private boolean globalCapabilities = true;

    NcwmsServerInfo() {
    }

    public NcwmsServerInfo(String title, boolean allowFeatureInfo, int maxImageWidth,
            int maxImageHeight, String description, List<String> keywords, String url,
            String adminPassword, boolean globalCapabilities) {
        super();
        this.title = title;
        this.allowFeatureInfo = allowFeatureInfo;
        this.maxImageWidth = maxImageWidth;
        this.maxImageHeight = maxImageHeight;
        this.description = description;
        this.keywords = keywords;
        this.url = url;
        this.adminPassword = adminPassword;
        this.globalCapabilities = globalCapabilities;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAllowFeatureInfo() {
        return allowFeatureInfo;
    }

    public int getMaxImageWidth() {
        return maxImageWidth;
    }

    public int getMaxImageHeight() {
        return maxImageHeight;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getUrl() {
        return url;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public boolean allowsGlobalCapabilities() {
        return globalCapabilities;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ");
        sb.append(title);
        sb.append("\nFeatureInfo Allowed: ");
        sb.append(allowFeatureInfo);
        sb.append("\nMax image size: ");
        sb.append(maxImageWidth);
        sb.append(", ");
        sb.append(maxImageHeight);
        sb.append("\nAbstract: ");
        sb.append(description);
        sb.append("\nKeywords: ");
        sb.append(keywords);
        sb.append("\nUrl: ");
        sb.append(url);
        sb.append("\nAdmin P/W: ");
        sb.append(adminPassword);
        sb.append("\nGlobal Capabilities: ");
        sb.append(globalCapabilities);
        return sb.toString();
    }
    
    private static class KeywordsAdapter extends XmlAdapter<String, List<String>> {
        private KeywordsAdapter() {}
        
        @Override
        public List<String> unmarshal(String scaleRangeStr) throws Exception {
            String[] split = scaleRangeStr.split(",\\s*");
            return Arrays.asList(split);
        }
    
        @Override
        public String marshal(List<String> keywords) throws Exception {
            StringBuilder sb = new StringBuilder();
            for(String keyword : keywords) {
                sb.append(keyword);
                sb.append(",");
            }
            return sb.substring(0, sb.length()-1);
        }
        
        private static KeywordsAdapter adapter = new KeywordsAdapter();
        
        @SuppressWarnings("unused")
        public static KeywordsAdapter getInstance() {
            return adapter;
        }
    }
}
