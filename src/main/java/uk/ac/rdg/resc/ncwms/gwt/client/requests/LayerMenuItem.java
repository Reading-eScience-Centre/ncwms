package uk.ac.rdg.resc.ncwms.gwt.client.requests;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

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
    
    public static LayerMenuItem getTreeFromJson(JSONObject json) {
        String nodeLabel = json.get("label").isString().stringValue();
        JSONValue children = json.get("children");
        LayerMenuItem rootItem = new LayerMenuItem(nodeLabel, "rootId");
        JSONArray childrenArray = children.isArray();
        for (int i = 0; i < childrenArray.size(); i++) {
            addNode(childrenArray.get(i).isObject(), rootItem);
        }
        return rootItem;
    }
    
    private static void addNode(JSONObject json, LayerMenuItem parentItem) {
        final String label = json.get("label").isString().stringValue();
        JSONValue idJson = json.get("id");
        // TODO add gridded info (but be aware that it might not be present)
        final String id;
        if(idJson != null && !idJson.toString().equals(""))
            id = idJson.isString().stringValue();
        else
            id = "branchNode";
        LayerMenuItem newChild = new LayerMenuItem(label, id);
        parentItem.addChildItem(newChild);
        
        // The JSONObject is an array of leaf nodes
        JSONValue children = json.get("children");
        if (children != null) {
            /*
             * We have a branch node
             */
            JSONArray childrenArray = children.isArray();
            for (int i = 0; i < childrenArray.size(); i++) {
                addNode(childrenArray.get(i).isObject(), newChild);
            }
        }
    }
}
