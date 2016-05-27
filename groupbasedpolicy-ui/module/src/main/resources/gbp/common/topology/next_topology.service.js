define(['next-ui'], function () {
    'use strict';

    /**
     * Service to handle next directive
     */
    function NextTopologyService() {
        /**
         * Create base nx topology object
         * @param nx
         * @returns {nx.graphic.Topology}
         */
        this.getNxTopClass = function (topoColors) {
            return new nx.graphic.Topology({
                        adaptive: true,
                        scalable: true,
                        nodeConfig: {
                            label: 'model.label',
                            iconType: 'model.icon',
                            color: function (node, model) {
                                return topoColors.getItem('forwarding-box');
                            }
                        },
                        linkConfig: {
                            // connected to hosts links have different colors
                            width: function (model, link) {
                                return model._data.gLinks.length > 2 ? 5 : 3;
                            },
                            linkType: 'curve'
                        },
                        tooltipManagerConfig: {
                            nodeTooltipContentClass: 'ExtendedTooltip',
                            showLinkTooltip: false
                        },
                        dataProcessor: 'force',
                        identityKey: 'id',
                        showIcon: true,
                        theme: 'blue',
                        enableSmartNode: false,
                        linkInstanceClass: 'ExtendedLink',
                        nodeInstanceClass: 'ExtendedNode'
                    });
        };

        /**
         * Method for getting right link's color based on status
         * @param status
         * @param topoColors
         * @returns {*}
         */
        this.getLinkColor = function (status, topoColors) {
            var color = null;

            if ( status ) {
                // all links are operational
                if ( status.operational === status.configured ) {
                    color =  topoColors.getItem('operational');
                } else {
                    // operational less than configured
                    if (status.operational < status.configured && status.operational) {
                        color = topoColors.getItem('operational-mixed');
                    } else {
                        // if operational and something else presented
                        if (!status.operational && status.configured) {
                            // if the link is between forwarding boxes, it's considered down
                            /*if ( model.source()._data.type === 'forwarding-box' && model.target()._data.type === 'forwarding-box' ) {
                                color = topoColors.getItem('down');
                            } else {
                                // otherwise just configured connection
                                color = topoColors.getItem('configured');
                            }*/

                            color = topoColors.getItem('down');
                            // otherwise
                        } else {
                            color = topoColors.getItem('default');
                        }
                    }
                }

            } else {
                color = topoColors.getItem('default');
            }

            return color;
        };

        /**
         * Service for reading topo nodes data from local storage
         * @returns {*}
         */
        this.readTopologyDataFromLS = function(){
            var data = null;

            try {
                data = JSON.parse(localStorage.getItem("verizonTopologyData"));
            } catch(e) {
                console.info('Local Storage read parse error:', e);
            }

            return data;
        };

        /**
         * Set loaded nodes data from local storage
         * @param data
         * @param topo
         * @param nodesDict
         */
        this.setTopologyDataFromLS = function (data, topo, nodesDict) {
            if ( data && data.nodes ) {

                data.nodes.forEach(function(node, index){
                    var nodeInst = topo.getNode(nodesDict.getItem(node.nodeName));
                    if(nodeInst !== undefined)
                        nodeInst.position({'x': node.x, 'y': node.y});
                });
            }
        };

        /**
         * Prepare and save data to local storage
         * @param topo
         */
        this.saveTopologyDataToLS = function (topo) {
            var data = {'nodes': []},
                nodesLayer = topo.getLayer('nodes');

            // prepare data for writing
            nodesLayer.eachNode(function(node){
                data.nodes.push({
                    'x': node.x(),
                    'y': node.y(),
                    'nodeName': node.model()._data['node-id']
                });
            });

            // save to local storage
            try {
                localStorage.setItem("verizonTopologyData", JSON.stringify(data));
            } catch(e) {
                console.info('Local Storage save error:', e);
            }
        };

        /**
         * Service for fade out all topo layers
         * @param topo
         */
        this.fadeOutAllLayers = function(topo){
            nx.each(topo.layers(), function(layer) {
                layer.fadeOut(true);
            }, this);
        };

        /**
         * Service for fade in all topo layers
         * @param topo
         */
        this.fadeInAllLayers = function(topo){
            //fade out all layers
            var linksLayerHighlightElements = topo.getLayer('links').highlightedElements(),
                nodeLayerHighlightElements = topo.getLayer('nodes').highlightedElements();

            //Clears previous
            nodeLayerHighlightElements.clear();
            linksLayerHighlightElements.clear();

            nx.each(topo.layers(), function(layer) {
                layer.fadeIn(true);
            }, this);
        };

        /**
         * Service for highlighting node with(without) links
         * @param topo
         * @param targetId
         * @param noLinks
         */
        this.highlightNode = function (topo, targetId, noLinks) {
            var nodeLayer = topo.getLayer('nodes'),
                linksLayer = topo.getLayer('links'),
                linksLayerHighlightElements = linksLayer.highlightedElements(),
                nodeLayerHighlightElements = nodeLayer.highlightedElements();

            //Clears previous
            nodeLayerHighlightElements.clear();
            linksLayerHighlightElements.clear();

            //Highlight node
            nodeLayerHighlightElements.add(topo.getNode(targetId));
            if(!noLinks) {
                //highlight links
                linksLayerHighlightElements.addRange(nx.util.values(topo.getNode(targetId).links()));
            }
            else{
                linksLayer.fadeOut(true);
            }
        };

        /**
         * Service for highlighting link
         * @param topo
         * @param linkId
         */
        this.highlightLink = function(topo, linkId) {
            var nodeLayer = topo.getLayer('nodes'),
                linksLayer = topo.getLayer('links'),
                linksLayerHighlightElements = linksLayer.highlightedElements(),
                nodeLayerHighlightElements = nodeLayer.highlightedElements(),
                link = topo.getLink(linkId);

            //Clears previous
            nodeLayerHighlightElements.clear();
            linksLayerHighlightElements.clear();

            //highlight link
            linksLayerHighlightElements.add(link);
            //highlight connected nodes
            nodeLayerHighlightElements.addRange(nx.util.values({source: topo.getNode(link.model().sourceID()), target: topo.getNode(link.model().targetID())}));
        };

        /**
         * Service for highlighting selected links path
         * @param topo
         * @param links - array of nx links obj
         */
        this.highlightPath = function(topo, links){
                // clear the path layer and get its instance
            var pathLayer = this.clearPathLayer(topo),
                // define a path
                path = new nx.graphic.Topology.Path({
                    'pathWidth': 5,
                    'links': links,
                    'arrow': 'cap'
                });

            // add the path
            pathLayer.addPath(path);
        };

        /**
         * Completely clear all paths from path layer
         * @param topo
         * @returns {*} path instance
         */
        this.clearPathLayer = function(topo){
            var pathLayer = topo.getLayer("paths");
            pathLayer.clear();
            return pathLayer;
        };

        /**
         * Service for returning nx tooltip skeleton
         * @returns {{content: *[]}}
         */
        this.getTooltipContent  = function () {
            return {
                content: [
                    {
                        tag: "div",
                        props: {
                            class: "n-topology-tooltip-header"
                        },
                        content: [
                            {
                                tag: 'span',
                                props: {
                                    class: "n-topology-tooltip-header-text"
                                },
                                content: '{#node.model.label}'
                            }
                        ]
                    },
                    {
                        tag: "div",
                        props: {
                            class: "n-topology-tooltip-content n-list"
                        },
                        content: [
                            {
                                tag: 'ul',
                                props: {
                                    class: "n-list-wrap",
                                    style: "font-size: 0.8em"
                                },
                                content: [
                                    {
                                        tag: 'li',
                                        props: {
                                            class: "n-list-item-i",
                                            role: "listitem"
                                        },
                                        content: [
                                            {
                                                tag: "label",
                                                content: "Status",
                                                props: {
                                                    style: "display: block; margin-top: 10px;"
                                                }
                                            },
                                            {
                                                tag: "span",
                                                content: "{#node.model.status}"
                                            }
                                        ]
                                    },
                                    {
                                        tag: 'li',
                                        props: {
                                            class: "n-list-item-i",
                                            role: "listitem"
                                        },
                                        content: [
                                            {
                                                tag: "label",
                                                content: "DataPath ID",
                                                props: {
                                                    style: "display: block; margin-top: 10px;"
                                                }
                                            },
                                            {
                                                tag: "span",
                                                content: "{#node.model.datapath-id}"
                                            }
                                        ]
                                    },
                                    {
                                        tag: 'li',
                                        props: {
                                            class: "n-list-item-i",
                                            role: "listitem"
                                        },
                                        content: [
                                            {
                                                tag: "label",
                                                content: "Type",
                                                props: {
                                                    style: "display: block; margin-top: 10px;"
                                                }
                                            },
                                            {
                                                tag: "span",
                                                content: "{#node.model.data.type}"
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            };
        };
    }

    NextTopologyService.$inject=[];

    return NextTopologyService;

});
