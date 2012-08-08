package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
    private boolean firstUse = true;
    private String firstTitle = null;

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
                }
                if(firstUse) {
                    setHTML("<big>" + firstTitle + "</big>");
                    firstUse = false;
                }
            }
        });

        setHTML("<big>Click here to start</big>");
        
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
        vPanel.add(tree);
        vPanel.add(button);
        popup.add(vPanel);
    }

    
    @Override
    public void populateLayers(LayerMenuItem topItem){
        tree.clear();
        String nodeLabel = topItem.getTitle();
        if(firstUse){
            firstTitle = nodeLabel;
        } else {
            setHTML("<big>" + nodeLabel + "</big>");
        }
        List<LayerMenuItem> children = topItem.getChildren();
        if(children != null){
            for(LayerMenuItem child : children){
                addNode(child, null);
            }
        }
    }
    
    private void addNode(LayerMenuItem item, final TreeItem parentNode) {
        if(parentNode == null && item.isLeaf()){
            /*
             * This is an empty dataset (probably it is still loading)
             */
            String nodeLabel = item.getTitle();
            TreeItem nextNode = new TreeItem(nodeLabel);
            tree.addItem(nextNode);
            return;
        }
        
        String label = item.getTitle();
        final String id = item.getId();
        
        Label node = new Label(label);
        if(parentNode != null){
            final String parentName = parentNode.getText();
            layerIdToTitle.put(id, "<big>" + parentName + "</big> > " + label);
            /*
             * We don't want to be able to plot the top layer item
             */
            node.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    setSelectedLayer(id);
                    layerSelectionHandler.layerSelected(id, true);
                }
            });
        }
        
        if(item.isLeaf()){
            /*
             * We have a leaf node
             */
            parentNode.addItem(node);
        } else {
            /*
             * We have a branch node
             */
            
            TreeItem nextNode = new TreeItem(node);
            
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

    @Override
    public List<String> getSelectedIds() {
        List<String> ret = new ArrayList<String>();
        ret.add(selectedLayer);
        return ret;
    }

    @Override
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
