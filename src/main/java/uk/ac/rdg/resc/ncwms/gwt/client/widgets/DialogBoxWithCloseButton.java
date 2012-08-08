/*
 * Copyright 2010
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.ac.rdg.resc.ncwms.gwt.client.widgets;

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * Extended DialogBox widget with close button inside the pop-up header
 * 
 * @author L.Pelov
 * 
 * Slight modification to have a hardcoded close image.  Original code from:
 * http://code.google.com/p/wcinteractions/source/browse/trunk/MVP/src/com/mvp/client/ui/widget/DialogBoxExt.java
 * 
 * @author Guy Griffiths
 */
public class DialogBoxWithCloseButton extends DialogBox {

    private HorizontalPanel captionPanel = new HorizontalPanel();

    private Widget closeWidget = null;

    /**
     * You have to provide a widget here. If click on the widget the dialog box
     * will be closed.
     * 
     * @param closeDialogBox
     */
    public DialogBoxWithCloseButton() {
        super();

        closeWidget = new Image("img/cross.png");

        // empty header could case a problem!
        setHTML("&nbsp;");
    }

    @Override
    public void setHTML(String html) {
        if (closeWidget != null) {
            setCaption(html, closeWidget);
        } else {
            super.setHTML(html);
        }
    }

    @Override
    public void setHTML(SafeHtml html) {
        if (closeWidget != null) {
            setCaption(html.asString(), closeWidget);
        } else {
            super.setHTML(html);
        }
    }

    /**
     * Makes a new caption and replace the old one.
     * 
     * @param txt
     * @param w
     */
    private void setCaption(String txt, Widget w) {
        captionPanel.setWidth("100%");
        captionPanel.add(new HTML(txt));
        captionPanel.add(w);
        captionPanel.setCellHorizontalAlignment(w, HasHorizontalAlignment.ALIGN_RIGHT);
        // make sure that only when you click on this icon the widget will be
        // closed!, don't make the field too width
        captionPanel.setCellWidth(w, "1%");
        captionPanel.addStyleName("Caption");

        // Get the cell element that holds the caption
        Element td = getCellElement(0, 1);

        // Remove the old caption
        td.setInnerHTML("");

        // append our horizontal panel
        td.appendChild(captionPanel.getElement());
    }

    /**
     * Close handler, which will hide the dialog box
     */
    private class DialogBoxCloseHandler {
        public void onClick(Event event) {
            hide();
        }
    }

    /**
     * Function checks if the browser event is was inside the caption region
     * 
     * @param event
     *            browser event
     * @return true if event inside the caption panel (DialogBox header)
     */
    protected boolean isHeaderCloseControlEvent(NativeEvent event) {
        // return isWidgetEvent(event, captionPanel.getWidget(1));
        return isWidgetEvent(event, closeWidget);
    }

    /**
     * Overrides the browser event from the DialogBox
     */
    @Override
    public void onBrowserEvent(Event event) {
        if (isHeaderCloseControlEvent(event)) {

            switch (event.getTypeInt()) {
            case Event.ONMOUSEUP:
            case Event.ONCLICK:
                new DialogBoxCloseHandler().onClick(event);
                break;
            case Event.ONMOUSEOVER:
                break;
            case Event.ONMOUSEOUT:
                break;
            }

            return;
        }

        // go to the DialogBox browser event
        super.onBrowserEvent(event);
    }

    /**
     * Function checks if event was inside a given widget
     * 
     * @param event
     *            - current event
     * @param w
     *            - widget to prove if event was inside
     * @return - true if event inside the given widget
     */
    protected boolean isWidgetEvent(NativeEvent event, Widget w) {
        EventTarget target = event.getEventTarget();

        if (Element.is(target)) {
            boolean t = w.getElement().isOrHasChild(Element.as(target));
            // GWT.log("isWidgetEvent:" + w + ':' + target + ':' + t);
            return t;
        }
        return false;
    }

}
