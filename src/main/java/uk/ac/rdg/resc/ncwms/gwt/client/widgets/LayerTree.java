package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class LayerTree extends Tree {
    private LayerSelectionHandler layerSelectionHandler;

    public LayerTree(LayerSelectionHandler layerSelectionHandler) {
        this.layerSelectionHandler = layerSelectionHandler;
//        TreeItem root = new TreeItem("Root");
//        root.addItem("node1");
//        root.addItem("node2");
//        TreeItem node3 = new TreeItem("Node3");
//        root.addItem(node3);
//        
//        addItem(root);
    }
    
    /**
     * Populates the layer tree from a JSON object
     * @param json
     */
    public void populateLayers(JSONObject json){
        addNode(json, null);
    }
    
    private void addNode(JSONObject json, TreeItem parentNode){
        // The JSONObject is an array of leaf nodes
        JSONValue children = json.get("children"); 
        if(children == null){
            String label = json.get("label").isString().stringValue();
            final String id = json.get("id").isString().stringValue();
            Label leaf = new Label(label);
            
            leaf.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    layerSelectionHandler.layerSelected(id);
                }
            });
            parentNode.addItem(leaf);
        } else {
            TreeItem nextNode = new TreeItem(json.get("label").isString().stringValue());
            if(parentNode == null){
                addItem(nextNode);
            } else {
                parentNode.addItem(nextNode);
            }
            JSONArray childrenArray = children.isArray();
            for(int i=0; i<childrenArray.size(); i++){
                addNode(childrenArray.get(i).isObject(), nextNode);
            }
        }

    }
}
