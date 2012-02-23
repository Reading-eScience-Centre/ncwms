package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.ArrayList;
import java.util.List;

public class LayerMenuItem {
    private String title;
    private String id = null;
    private boolean gridded = true;
    private List<LayerMenuItem> childItems = null;
    
    public LayerMenuItem(String title, String id) {
        this.title = title;
        this.id = id;
    }
    
    public LayerMenuItem(String title, String id, boolean gridded) {
        this.title = title;
        this.id = id;
        this.gridded = gridded;
    }
    
    public void addChildItem(LayerMenuItem item){
        if(childItems == null){
            childItems = new ArrayList<LayerMenuItem>();
        }
        childItems.add(item);
    }
    
    public String getTitle(){
        return title;
    }
    
    public String getId(){
        return id;
    }
    
    public boolean isGridded(){
        return gridded;
    }
    
    public List<LayerMenuItem> getChildren(){
        return childItems;
    }
    
    public boolean isLeaf(){
        return childItems == null || childItems.size() == 0;
    }
}
