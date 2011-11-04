package uk.ac.rdg.resc.ncwms.gwt.client;

import uk.ac.rdg.resc.ncwms.gwt.client.widgets.AnimationButton;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.ElevationSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerSelectorCombo;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.LayerTree;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.MapArea;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.PaletteSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.TimeSelector;
import uk.ac.rdg.resc.ncwms.gwt.client.widgets.UnitsInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


/**
 * A class containing static methods for returning different layouts.  This means that
 * multiple viewports can be configured here from the same codebase with very little change 
 * 
 * @author guy
 */
public class LayoutManager {
    public static Widget getMyOceanViewerV2Layout(LayerSelectorCombo layerSelector,
                                                  UnitsInfo unitsInfo,
                                                  TimeSelector timeSelector,
                                                  ElevationSelector elevationSelector,
                                                  PaletteSelector paletteSelector,
                                                  AnimationButton anim,
                                                  Anchor kmzLink,
                                                  Anchor permalink,
                                                  Anchor email,
                                                  Anchor docLink,
                                                  Anchor screenshot,
                                                  Image myOceanLogo,
                                                  MapArea mapArea){
        kmzLink.setStylePrimaryName("linkStyle");
        permalink.setStylePrimaryName("linkStyle");
        email.setStylePrimaryName("linkStyle");
        docLink.setStylePrimaryName("linkStyle");
        screenshot.setStylePrimaryName("linkStyle");
        
        VerticalPanel selectors = new VerticalPanel();
        selectors.add(layerSelector);
        selectors.add(unitsInfo);
        selectors.add(timeSelector);
        selectors.add(elevationSelector);
        selectors.setHeight(myOceanLogo.getHeight()+"px");
        
        HorizontalPanel bottomPanel = new HorizontalPanel();
        bottomPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        anim.setWidth("16px");
        bottomPanel.add(anim);
        bottomPanel.setWidth("100%");
        bottomPanel.add(kmzLink);
        bottomPanel.add(permalink);
        bottomPanel.add(email);
        bottomPanel.add(screenshot);
        bottomPanel.add(docLink);
        
        HorizontalPanel topPanel = new HorizontalPanel();

        topPanel.add(myOceanLogo);
        topPanel.add(selectors);
        
        HorizontalPanel mapPalettePanel = new HorizontalPanel();
        mapPalettePanel.add(mapArea);
        mapPalettePanel.add(paletteSelector);
                
        VerticalPanel vPanel = new VerticalPanel();
        vPanel.add(topPanel);
        /*
         * The image height is hardcoded here, because when running with IE8, myOceanLogo.getHeight()
         * returns 0 instead of the actual height...
         */
        vPanel.setCellHeight(topPanel, "148px");
//        vPanel.setCellHeight(topPanel, myOceanLogo.getHeight()+"px");
        vPanel.setCellWidth(topPanel, ((int) mapArea.getMap().getSize().getWidth()+100)+"px");
        vPanel.add(mapPalettePanel);
        
        vPanel.add(bottomPanel);
        vPanel.setCellHeight(bottomPanel, "100%");
        vPanel.setCellVerticalAlignment(bottomPanel, HasVerticalAlignment.ALIGN_MIDDLE);
        
        ScrollPanel scrollPanel = new ScrollPanel(vPanel);
        
        return scrollPanel;
    }
}
