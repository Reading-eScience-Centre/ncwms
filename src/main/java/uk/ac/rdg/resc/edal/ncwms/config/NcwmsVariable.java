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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourPalette;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.wms.WmsLayerMetadata;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NcwmsVariable implements WmsLayerMetadata {
    @XmlAttribute(name = "id", required = true)
    private String id;

    @XmlAttribute(name = "title")
    private String title = null;
    
    @XmlAttribute(name = "description")
    private String description = null;

    @XmlAttribute(name = "colorScaleRange")
    @XmlJavaTypeAdapter(ScaleRangeAdapter.class)
    private Extent<Float> colorScaleRange = null;

    @XmlAttribute(name = "palette")
    private String paletteName = ColourPalette.DEFAULT_PALETTE_NAME;

    @XmlAttribute(name = "scaling")
    private String scaling = "linear";

    @XmlAttribute(name = "numColorBands")
    private int numColorBands = ColourPalette.MAX_NUM_COLOURS;

    @XmlAttribute(name = "metadataUrl")
    private String metadataUrl = "";

    @XmlAttribute(name = "metadataDesc")
    private String metadataDesc = "";

    @XmlAttribute(name = "metadataMimetype")
    private String metadataMimetype = "";

    /* The dataset to which this variable belongs */
    @XmlTransient
    private NcwmsDataset dataset;

    NcwmsVariable() {
    }

    public NcwmsVariable(String id, String title, String description, Extent<Float> colorScaleRange,
            String paletteName, String scaling, int numColorBands, String metadataUrl,
            String metadataDesc, String metadataMimetype) {
        super();
        this.id = id;
        this.title = title;
        this.description = description;
        this.colorScaleRange = colorScaleRange;
        this.paletteName = paletteName;
        this.scaling = scaling;
        this.numColorBands = numColorBands;
        this.metadataUrl = metadataUrl;
        this.metadataDesc = metadataDesc;
        this.metadataMimetype = metadataMimetype;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Extent<Float> getColorScaleRange() {
        return colorScaleRange;
    }

    @Override
    public String getPalette() {
        return paletteName;
    }

    @Override
    public Boolean isLogScaling() {
        return scaling.equalsIgnoreCase("log");
    }

    @Override
    public Integer getNumColorBands() {
        return numColorBands;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public String getMetadataDesc() {
        return metadataDesc;
    }

    public String getMetadataMimetype() {
        return metadataMimetype;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCopyright() {
        return dataset.getCopyrightStatement();
    }

    @Override
    public String getMoreInfo() {
        return dataset.getMoreInfo();
    }

    @Override
    public boolean isQueryable() {
        return dataset.isQueryable();
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setColorScaleRange(Extent<Float> colorScaleRange) {
        this.colorScaleRange = colorScaleRange;
    }

    public void setPaletteName(String paletteName) {
        this.paletteName = paletteName;
    }

    public void setScaling(String scaling) {
        this.scaling = scaling;
    }

    public void setNumColorBands(int numColorBands) {
        this.numColorBands = numColorBands;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public void setMetadataDesc(String metadataDesc) {
        this.metadataDesc = metadataDesc;
    }

    public void setMetadataMimetype(String metadataMimetype) {
        this.metadataMimetype = metadataMimetype;
    }

    private static class ScaleRangeAdapter extends XmlAdapter<String, Extent<Float>> {
        private ScaleRangeAdapter() {
        }

        @Override
        public Extent<Float> unmarshal(String scaleRangeStr) throws Exception {
            String[] split = scaleRangeStr.split(" ");
            return Extents.newExtent(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
        }

        @Override
        public String marshal(Extent<Float> scaleRange) throws Exception {
            return scaleRange.getLow() + " " + scaleRange.getHigh();
        }

        private static ScaleRangeAdapter adapter = new ScaleRangeAdapter();

        @SuppressWarnings("unused")
        public static ScaleRangeAdapter getInstance() {
            return adapter;
        }
    }

    void setNcwmsDataset(NcwmsDataset dataset) {
        this.dataset = dataset;
    }

    public NcwmsDataset getNcwmsDataset() {
        return dataset;
    }
}
