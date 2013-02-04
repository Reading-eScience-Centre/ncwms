package uk.ac.rdg.resc.ncwms.wms;

import java.util.ArrayList;
import java.util.List;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.VerticalPosition;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.util.WmsUtils.StyleInfo;

public class CapabilitiesLayer {
    private boolean ready = true;
    private String name = null;
    private String title = null;
    private String description = null;
    private BoundingBox bbox = null;
    private TimeAxis tAxis = null;
    private VerticalAxis vAxis = null;
    private Extent<TimePosition> tExtent = null;
    private Extent<VerticalPosition> vExtent = null;
    private boolean continuousAxes;
    private List<StyleInfo> styles = new ArrayList<WmsUtils.StyleInfo>();
    private List<CapabilitiesLayer> childLayers = new ArrayList<CapabilitiesLayer>();
    private boolean queryable;

    public CapabilitiesLayer(boolean ready, String name, String title, String description,
            BoundingBox bbox, TimeAxis tAxis, VerticalAxis vAxis, List<StyleInfo> styles, boolean queryable) {
        super();
        this.ready = ready;
        this.name = name;
        this.title = title;
        this.description = description;
        this.bbox = bbox;
        this.tAxis = tAxis;
        this.vAxis = vAxis;
        this.styles = styles;
        this.continuousAxes = false;
        this.queryable = queryable;
    }
    
    public CapabilitiesLayer(boolean ready, String name, String title, String description,
            BoundingBox bbox, Extent<TimePosition> tExtent, Extent<VerticalPosition> vExtent, List<StyleInfo> styles, boolean queryable) {
        super();
        this.ready = ready;
        this.name = name;
        this.title = title;
        this.description = description;
        this.bbox = bbox;
        this.tExtent = tExtent;
        this.vExtent = vExtent;
        this.styles = styles;
        this.continuousAxes = true;
        this.queryable = queryable;
    }

    public boolean isReady() {
        return ready;
    }
    
    public boolean isQueryable() {
        return queryable;
    }
    
    public boolean isContinuous() {
        return continuousAxes;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public TimeAxis gettAxis() {
        return tAxis;
    }

    public VerticalAxis getvAxis() {
        return vAxis;
    }
    
    public Extent<TimePosition> gettExtent() {
        return tExtent;
    }

    public Extent<VerticalPosition> getvExtent() {
        return vExtent;
    }

    public List<StyleInfo> getStyles() {
        return styles;
    }

    public List<CapabilitiesLayer> getChildLayers() {
        return childLayers;
    }
}
