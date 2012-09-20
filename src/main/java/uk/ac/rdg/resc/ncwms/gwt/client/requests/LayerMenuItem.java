package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.ArrayList;
import java.util.List;

public class LayerMenuItem {
    private String title;
    private String id = null;
    private boolean plottable = true;
    private List<LayerMenuItem> childItems = null;
    private LayerMenuItem parent = null;
    
    public LayerMenuItem(String title, String id, boolean plottable) {
        this.title = title;
        this.id = id;
        this.plottable = plottable;
    }
    
    public void addChildItem(LayerMenuItem item){
        if(childItems == null){
            childItems = new ArrayList<LayerMenuItem>();
        }
        childItems.add(item);
        item.parent = this;
    }
    
    public LayerMenuItem getParent() {
        return parent;
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getId(){
        return id;
    }
    
    public boolean isPlottable(){
        return plottable;
    }
    
    public List<LayerMenuItem> getChildren(){
        return childItems;
    }
    
    public boolean isLeaf(){
        return childItems == null || childItems.size() == 0;
    }
}
