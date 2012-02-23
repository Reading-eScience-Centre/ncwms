package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LayerSelectorCombo extends Button implements LayerSelectorIF {
    private LayerSelectionHandler layerSelectionHandler;
    private PopupPanel popup;
    private Tree tree;
    private Map<String, String> layerIdToTitle;
    private String selectedLayer;

    public LayerSelectorCombo(LayerSelectionHandler layerHandler) {
        super("Loading");
        this.layerSelectionHandler = layerHandler;

        layerIdToTitle = new HashMap<String, String>();

        popup = new PopupPanel();
        popup.setAutoHideEnabled(true);

        setStylePrimaryName("hiddenButton");
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                popup.setPopupPosition(
                        LayerSelectorCombo.this.getAbsoluteLeft(),
                        LayerSelectorCombo.this.getAbsoluteTop()
                                + LayerSelectorCombo.this.getOffsetHeight());
                if (!popup.isShowing()) {
                    popup.show();
                } else {
//                    popup.hide();
                }
            }
        });

        VerticalPanel vPanel = new VerticalPanel();
        PushButton button = new PushButton("Refresh");
        button.setTitle("Click to refresh the layers list");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                layerSelectionHandler.refreshLayerList();
            }
        });
        tree = new Tree();
        tree.addOpenHandler(new OpenHandler<TreeItem>() {
            @Override
            public void onOpen(OpenEvent<TreeItem> event) {
                TreeItem selected = event.getTarget();
                for(int i=0; i< tree.getItemCount(); i++){
                    TreeItem other = tree.getItem(i);
                    if(!other.equals(selected) && other.getState()){
                        other.setState(false);
                    }
                }
                
            }
        });
        vPanel.add(tree);
        vPanel.add(button);
        popup.add(vPanel);
    }

    /**
     * Populates the layer tree from a JSON object
     * 
     * @param json
     */
    public void populateLayers(JSONObject json) {
        tree.clear();
        String nodeLabel = json.get("label").isString().stringValue();
        JSONValue children = json.get("children");
        setHTML("<big>" + nodeLabel + "</big>");
        JSONArray childrenArray = children.isArray();
        for (int i = 0; i < childrenArray.size(); i++) {
            addNode(childrenArray.get(i).isObject(), null);
        }
    }
    
    public void populateLayers(LayerMenuItem topItem){
        tree.clear();
        String nodeLabel = topItem.getTitle();
        List<LayerMenuItem> children = topItem.getChildren();
        setHTML("<big>" + nodeLabel + "</big>");
        for(LayerMenuItem child : children){
            addNode(child, null);
        }
    }
    
    private void addNode(LayerMenuItem item, final TreeItem parentNode) {
        if(item.isLeaf()){
            /*
             * We have a leaf node
             */
            final String parentName = parentNode.getText();
            final String label = item.getTitle();
            final String id = item.getId();
            layerIdToTitle.put(id, "<big>" + parentName + "</big> > " + label);
            Label leaf = new Label(label);
            leaf.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    setSelectedLayer(id);
                    layerSelectionHandler.layerSelected(id);
                }
            });
            parentNode.addItem(leaf);
        } else {
            /*
             * We have a branch node
             */
            String nodeLabel = item.getTitle();
            TreeItem nextNode = new TreeItem(nodeLabel);
            if (parentNode == null) {
                tree.addItem(nextNode);
            } else {
                parentNode.addItem(nextNode);
            }
            for (LayerMenuItem child : item.getChildren()) {
                addNode(child, nextNode);
            }
        }
    }

    private void addNode(JSONObject json, final TreeItem parentNode) {
        // The JSONObject is an array of leaf nodes
        JSONValue children = json.get("children");
        if (children == null) {
            /*
             * We have a leaf node
             */
            final String parentName = parentNode.getText();
            final String label = json.get("label").isString().stringValue();
            final String id = json.get("id").isString().stringValue();
            layerIdToTitle.put(id, "<big>" + parentName + "</big> > " + label);
            Label leaf = new Label(label);
            leaf.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    setSelectedLayer(id);
                    layerSelectionHandler.layerSelected(id);
                }
            });
            parentNode.addItem(leaf);
        } else {
            /*
             * We have a branch node
             */
            String nodeLabel = json.get("label").isString().stringValue();
            TreeItem nextNode = new TreeItem(nodeLabel);
            if (parentNode == null) {
                tree.addItem(nextNode);
            } else {
                parentNode.addItem(nextNode);
            }
            JSONArray childrenArray = children.isArray();
            for (int i = 0; i < childrenArray.size(); i++) {
                addNode(childrenArray.get(i).isObject(), nextNode);
            }
        }
    }

    public String getSelectedId() {
        return selectedLayer;
    }

    public void setSelectedLayer(String id) {
        selectedLayer = id;
        setHTML(layerIdToTitle.get(id));
        if (popup.isShowing()) {
            popup.hide();
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if(enabled)
            setStylePrimaryName("hiddenButton");
        else
            setStylePrimaryName("inactiveHiddenButton");
    }
}
