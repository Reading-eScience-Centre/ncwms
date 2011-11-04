package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.HashMap;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class LayerSelectorCombo extends Button {
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

        tree = new Tree();
        popup.add(tree);
    }

    /**
     * Populates the layer tree from a JSON object
     * 
     * @param json
     */
    public void populateLayers(JSONObject json) {
        String nodeLabel = json.get("label").isString().stringValue();
        JSONValue children = json.get("children");
        setHTML("<big>" + nodeLabel + "</big>");
        JSONArray childrenArray = children.isArray();
        for (int i = 0; i < childrenArray.size(); i++) {
            addNode(childrenArray.get(i).isObject(), null);
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
                }
            });
            parentNode.addItem(leaf);
        } else {
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
        layerSelectionHandler.layerSelected(id);
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
