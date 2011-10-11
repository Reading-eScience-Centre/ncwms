/**
 * Class: Godiva2.Menu
 *
 */

 Godiva2.Menu = OpenLayers.Class({

    /**
     * Property: calendar
     * {<OpenLayers.Map>} The DHTML calendar for normal (365- or 366-day) years
     */
    tree: null,
    treeEls: null,
    servers: null,

    /**
     * Constructor: Godiva2.Menu
     * Create a Godiva2 Menu.  For example:
     *
     * > var menu = new Godiva2.Menu();

     * Parameters:
     * layer - details of the layer which the TimeDimension represents
     */
    initialize: function () {
        // (There is some stuff in OpenLayers classes about
	// creating a displayClassName.  Perhaps this should be
	// reinstated, but I don't really know what it does.)
        this.treeEls = {};
        this.treeEls.layerPathEl = document.getElementById('layerPath');
        this.servers = [''];
    },

    /**
     * Method: destroy
     * The destroy method is used to perform any clean up before the component
     * is dereferenced.  Typically this is where event listeners are removed
     * to prevent memory leaks.
     */
    destroy: function () {
        if (this.tree) {
            this.tree = null;
        }
    },

    setup: function () {
        if (this.tree == null) {
            this.tree = new YAHOO.widget.TreeView('layerSelector');
            // Add an event callback that gets fired when a tree node is clicked
            this.tree.subscribe('labelClick', this.treeNodeClicked.bind(this));
        } else {
            // Clear the contents of the tree
            this.tree.removeChildren(this.tree.getRoot());
        }

        // The servers can be specified using the global "servers" array above
        // but if not, we'll just use the default server
        if (typeof servers == 'undefined' || this.servers == null) {
            this.servers = [''];
        }

        // Add a root node in the tree for each server.  If the user has supplied
        // a "menu" option then this will be sent to all the servers.
        for (var i = 0; i < this.servers.length; i++) {
            var layerRootNode = new YAHOO.widget.TextNode(
                {label: "Loading ...", server: this.servers[i]},
                this.tree.getRoot(),
                this.servers.length == 1 // Only show expanded if this is the only server
            );
            layerRootNode.multiExpand = false;
            getMenu(layerRootNode, {
                menu: autoLoad.menu,
                callback : this.gotLayers.bind(this)
            });
        }
    },

    gotLayers: function (layerRootNode, layers) {
        layerRootNode.data.label = layers.label;
        layerRootNode.label = layers.label;
        // Add layers recursively.
        this.addNodes(layerRootNode, layers.children);
        this.tree.draw();
        var node;
        if (layer.details == null) {
            // The user hasn't yet selected a layer
            // Look to see if we are auto-loading a certain layer
            if (autoLoad.layer != null) {
                node = this.tree.getNodeByProperty('id', autoLoad.layer);
                if (node == null) {
                    alert("Layer " + autoLoad.layer + " not found");
                } else {
                    this.expandParents(node);
                    this.treeNodeClicked(node); // act as if we have clicked this node
                }
            }
        } else { // layer.details != null
            // The user has selected a layer, so we must make sure that
            // the correct node in the tree is expanded
            node = this.tree.getNodeByProperty('id', layer.details.id);
            if (node != null) {
                this.expandParents(node);
            }
        }
    },

    expandParents: function (node) {
        if (node.parent != null) {
            node.parent.expand();
            this.expandParents(node.parent);
        }
    },

    /**
     * Method: addNodes
     * Recursive method to add nodes to the layer selector tree control
     */
    addNodes: function (parentNode, layerArray) {
        for (var i = 0; i < layerArray.length; i++) {
            var layer = layerArray[i];
            if (layer.server == null) {
                // If the layer does not specify a server explicitly, use the URL of
                // the server that provided this layer
                layer.server = parentNode.data.server;
            }
            // The treeview control uses the layer.label string for display
            var newNode = new YAHOO.widget.TextNode(
                {label: layer.label, id: layer.id, server: layer.server},
                parentNode,
                false
            );
            if (typeof layer.children != 'undefined') {
                newNode.multiExpand = false;
                this.addNodes(newNode, layer.children);
            }
        }
    },

    /**
     * Method: treeNodeClicked
     * Called when a node in the tree has been clicked
     */
    treeNodeClicked: function (node) {
        // We're only interested if this is a displayable layer, i.e. it has an id.
        if (typeof node.data.id != 'undefined') {
            // Update the breadcrumb trail
            var s = node.data.label;
            var theNode = node;
            while(theNode.parent != this.tree.getRoot()) {
                theNode = theNode.parent;
                s = theNode.data.label + ' &gt; ' + s;
            }
            $('layerPath').innerHTML = s;

            // See if we're auto-loading a certain time value
            if (autoLoad.isoTValue != null) {
                time.isoTValue = autoLoad.isoTValue;
            }
            if (time.isoTValue == null ) {
                // Set to the present time if we don't already have a time selected
                // Set milliseconds to zero (don't know how to create a format string
                // that includes milliseconds).
                // TODO: this only works correctly in GMT time zone!  Need to take
                // time zone into accout to do this correctly, but error is not
                // very important.
                time.isoTValue = new Date().print('%Y-%m-%dT%H:%M:%SZ');
            }

            // Get the details of this layer from the server, calling layerSelected()
            // when we have the result
            getLayerDetails(node.data.server, {
                callback: layerSelected,
                layerName: node.data.id,
                time: time.isoTValue
            });
        }
    },

    CLASS_NAME: "Godiva2.Menu"
 });