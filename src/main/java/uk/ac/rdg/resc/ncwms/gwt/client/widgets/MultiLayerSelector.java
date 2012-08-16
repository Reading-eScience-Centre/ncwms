package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.rdg.resc.ncwms.gwt.client.handlers.LayerSelectionHandler;
import uk.ac.rdg.resc.ncwms.gwt.client.requests.LayerMenuItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MultiLayerSelector extends VerticalPanel implements LayerSelectorIF {
    private LayerSelectionHandler layerSelectionHandler;
    private Tree tree;
    private Map<String, String> layerIdToTitle;
    private List<CheckBox> layers;

    public MultiLayerSelector(LayerSelectionHandler layerHandler) {
        this.layerSelectionHandler = layerHandler;

        layerIdToTitle = new HashMap<String, String>();
        layers = new ArrayList<CheckBox>();

        PushButton button = new PushButton("Refresh");
        button.setTitle("Click to refresh the layers list");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                layerSelectionHandler.refreshLayerList();
            }
        });
        tree = new Tree();
//        tree.addOpenHandler(new OpenHandler<TreeItem>() {
//            @Override
//            public void onOpen(OpenEvent<TreeItem> event) {
//                TreeItem selected = event.getTarget();
//                for(int i=0; i< tree.getItemCount(); i++){
//                    TreeItem other = tree.getItem(i);
//                    if(!other.equals(selected) && other.getState()){
//                        other.setState(false);
//                    }
//                }
//                
//            }
//        });
        add(tree);
        add(button);
    }
    @Override
    public void populateLayers(LayerMenuItem topItem){
        tree.clear();
        String nodeLabel = topItem.getTitle();
        List<LayerMenuItem> children = topItem.getChildren();
        TreeItem rootLabel = new TreeItem(nodeLabel);
        tree.addItem(rootLabel);
        for(LayerMenuItem child : children){
            addNode(child, rootLabel);
        }
    }
    
    private void addNode(LayerMenuItem item, final TreeItem parentNode) {
        if(item.isLeaf()){
            /*
             * We have a leaf node
             */
            if(parentNode == null){
                /*
                 * This is an empty dataset (probably it is still loading)
                 */
                String nodeLabel = item.getTitle();
                TreeItem nextNode = new TreeItem(nodeLabel);
                tree.addItem(nextNode);
            }
            
            final String parentName = parentNode.getText();
            final String label = item.getTitle();
            final String id = item.getId();
            layerIdToTitle.put(id, "<big>" + parentName + "</big> > " + label);
            CheckBox leaf = new CheckBox(label);
            leaf.setFormValue(id);
            layers.add(leaf);
            leaf.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override
                public void onValueChange(ValueChangeEvent<Boolean> event) {
                    if(event.getValue()){
                        layerSelectionHandler.layerSelected(id, true);
                    } else {
                        layerSelectionHandler.layerDeselected(id);
                    }
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

    @Override
    public void setEnabled(boolean enabled) {
        if(enabled)
            setStylePrimaryName("hiddenButton");
        else
            setStylePrimaryName("inactiveHiddenButton");
    }
    @Override
    public List<String> getSelectedIds() {
        List<String> ret = new ArrayList<String>();
        for(CheckBox layer : layers){
            if(layer.getValue()){
                ret.add(layer.getFormValue());
            }
        }
        return ret;
    }
    
    @Override
    public void setSelectedLayer(String id) {
    }
    @Override
    public List<String> getTitleElements() {
        // TODO Auto-generated method stub
        return null;
    }
}