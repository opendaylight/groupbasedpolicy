define(['next-ui'], function() {

	var NextTopology = function(NextTopologyService){
		return {
			restrict: 'E',
			scope: {
				topologyData: '=',
				cbkFunctions: '=?',
				dictionaries: '=?',
				topo: '=?',
                topoColors: '=?'
			},
			template: '<div id="graph-container" class="col-md-12"></div>',
			link: function(scope) {
				var saveTopoInterval = null;

				scope.topo = null;

				/**
				 * Colors used for topology objects
				 * @type {nx.data.Dictionary}
				 */
				scope.topoColors = new nx.data.Dictionary({
					'operational': '#0f9d58',
					'configured': '#464646',
					'operational-mixed': '#C4AF00',
					'down': '#FF0000',
					'default': '#8C8C8C',
					'forwarding-box': '#0386d2',
					'host': '#8C8C8C'
				});

				/**
				 * init next topology graph
				 */
				scope.init = function (successCbk) {
					// register "font" icon
					nx.graphic.Icons.registerFontIcon('devicedown', 'FontAwesome', "\uf057", 20);

					var app = new nx.ui.Application();
					// set app containter
					document.getElementById('graph-container').innerHTML = '';
					app.container(document.getElementById('graph-container'));

					extendLinkClass();
					extendNodeClass();
					extendedTooltip();
					// TODO: remove when come to decision about depiction of node status
					//createNodeStatusLayer();
					defineCustomEvents();

					scope.topo = NextTopologyService.getNxTopClass(scope.topoColors);
					scope.setTopoEvents();

					// App events - if app is resized
					app.on('resize', function(){
						topo.adaptToContainer();
					});

					// Attach topo to app
					scope.topo.attach(app);

					if ( successCbk ) {
						successCbk();
					}
				};

				/**
				 * NX topo events
				 */
				scope.setTopoEvents = function () {
					// Fired when the app is ready and displayed
					scope.topo.on('ready', function (sender, event) {
						scope.topo.data(scope.topologyData);

						// load saved data
						if ( scope.dictionaries ){
							var data = NextTopologyService.readTopologyDataFromLS();
							NextTopologyService.setTopologyDataFromLS(data, scope.topo, scope.dictionaries.nodes);
						}

						// clear interval after reload data
						if ( saveTopoInterval ) {
							clearInterval(saveTopoInterval);
						}

						// set interval for saving topo nodes position
						saveTopoInterval = window.setInterval(function(){NextTopologyService.saveTopologyDataToLS(scope.topo);}, 5000);
					});

					// Fired when topology is generated
					scope.topo.on('topologyGenerated', function(sender, event) {

						// use custom events for the topology
						// TODO: remove when come to decision about depiction of node status
						//sender.attachLayer("status", "NodeStatus");
						sender.registerScene('ce', 'CustomEvents');
						sender.activateScene('ce');
						scope.topo.tooltipManager().showNodeTooltip(false);
					});
				};

				/**
				 * Watching topology data
				 */
				scope.$watch('topologyData', function(){
					//console.log('scope.topologyData', scope.topologyData);

					if( scope.topologyData.nodes.length ) { //&& initialized === false
						scope.init(scope.cbkFunctions.topologyGenerated);
					}
				});

				/**
				 * Extend base nx node class function
				 */
				function extendNodeClass(){
					nx.define('ExtendedNode', nx.graphic.Topology.Node, {
						view: function(view){

							view.content.push({
								"name": "deviceDownBadge",
								"type": "nx.graphic.Group",
								"content": [
									{
										"name": "deviceDownBadgeBg",
										"type": "nx.graphic.Rect",
										"props": {
											"class": "device-down-bg",
											"height": 1,
											"visible": false
										}
									},
									{
										"name": "deviceDownBadgeText",
										"type": "nx.graphic.Icon",
										"props": {
											"class": "icon",
											"iconType": "devicedown",
											"color": "#ff0000",
											"showIcon": true,
											"scale": 1,
											"visible": false
										}
									}
								]
							});
							return view;
						},
						methods: {
							// inherit properties/parent's data
							init: function(args){
								this.inherited(args);
								var stageScale = this.topology().stageScale();
								this.view('label').setStyle('font-size', 14 * stageScale);
							},
							// inherit parent's model
							'setModel': function(model) {
								this.inherited(model);

								// if status is down
                                this._drawDeviceDownBadge(this.model());

							},
							"_drawDeviceDownBadge": function(model){

								var badge, badgeBg, badgeText,
									icon, iconSize, iconScale,
									bound, boundMax, badgeTransform,
									badgeVisibility = model.get("status") === "configured";

								// views of badge
								badge = this.view("deviceDownBadge");
								badgeBg = this.view("deviceDownBadgeBg");
								badgeText = this.view("deviceDownBadgeText");

								// view of device icon
								icon = this.view('icon');
								iconSize = icon.size();
								iconScale = icon.scale();

								// define position of the badge
								badgeTransform = {
									x: iconSize.width * iconScale / -2.5,
									y: iconSize.height * iconScale / 3
								};

								// make badge visible
								badgeText.set("visible", badgeVisibility);

								// get bounds and apply them for white background
								bound = badge.getBound();
								boundMax = Math.max(bound.width - 6, 1);
								badgeBg.sets({
									width: boundMax,
									visible: badgeVisibility
								});

								// set position of the badge
								badgeBg.setTransform(badgeTransform.x, badgeTransform.y);
								badgeText.setTransform(badgeTransform.x, badgeTransform.y);

							},
							"_showDownBadge": function(){
								this.view("deviceDownBadgeBg").set("visible", true);
								this.view("deviceDownBadgeText").set("visible", true);
							},
							"_hideDownBadge": function(){
								this.view("deviceDownBadgeBg").set("visible", false);
								this.view("deviceDownBadgeText").set("visible", false);
							}
						}
					});
				}

				/**
				 * Extend base nx link class function
				 */
				function extendLinkClass () {
					nx.define('ExtendedLink', nx.graphic.Topology.Link, {
						view: function(view){
							view.content.push({
								name: 'badge',
								type: 'nx.graphic.Group',
								content: [
									{
										name: 'badgeBg',
										type: 'nx.graphic.Rect',
										props: {
											'class': 'link-set-circle',
											height: 1
										}
									},
									{
										name: 'badgeText',
										type: 'nx.graphic.Text',
										props: {
											'class': 'link-set-text',
											y: 1
										}
									}
								]
							});
							return view;
						},
						properties: {
							stageScale: {
								set: function (a) {
									this.view("badge").setTransform(null, null, a);
									var b = (this._width || 1) * a;
									this.view("line").dom().setStyle("stroke-width", b);
									this.view("path").dom().setStyle("stroke-width", b);
									this._stageScale = a;
									this.update();
								}
							}
						},
						methods: {
							// inherit properties/parent's data
							init: function(args){
								this.inherited(args);
								this.topology().fit();
							},
							// inherit parent's model
							'setModel': function(model) {
								this.inherited(model);
								//if(model._data.linksIntegrity){
								//	this.view('statusIcon').set('src', 'assets/images/attention.png');
								//}
							},
							// when topology's updated
							update: function(){
								this.inherited();
								// ECMP badge settings
								var badge = this.view('badge');
								var badgeText = this.view('badgeText');
								var badgeBg = this.view('badgeBg');
								var statusIcon = this.view('statusIcon');
								var status = this.model()._data.status;
								if( this.model()._data.gLinks.length > 2 ) {
									badgeText.sets({
										text: status.operational + '/' + status.configured,
										visible: true
									});
									var bound = badge.getBound();
									var boundMax = Math.max(bound.width - 6, 1);
									badgeBg.sets({width: boundMax, visible: true});
									badgeBg.setTransform(boundMax / -2);
									var centerPoint = this.centerPoint();
									badge.setTransform(centerPoint.x, centerPoint.y);
								}
								// record source & target 'node-id's
								var sourceNode = this.model().source()._data;
								this.model()._data.sourceName = this.model().source()._data.label;
								this.model()._data.targetName = this.model().target()._data.label;

								this.view("badge").visible(true);
								this.view("badgeBg").visible(true);
								this.view("badgeText").visible(true);

                                //set correct link color
                                this.set('color',this.getColor());

							},
							// generate the color for a link
							getColor: function(){
								// get color depend on status
								var color = NextTopologyService.getLinkColor(this.model()._data.status, scope.topoColors);
								// make it available outside next
								this.model()._data.linkColor = color;
								return color;
							}
						}
					});
				}

				/**
				 * Extended class for tooltip nx component
				 */
				function extendedTooltip(){
					nx.define('ExtendedTooltip', nx.ui.Component, {
						properties: {
							node: {},
							topology: {}
						},
						view: NextTopologyService.getTooltipContent()
					});

				}

				/**
				 * Define custom events for topology componets
				 */
				function defineCustomEvents(){
					nx.define('CustomEvents', nx.graphic.Topology.DefaultScene, {
						methods: {
							clickNode: function(sender, node){
								if ( scope.cbkFunctions.clickNode ) {
									scope.cbkFunctions.clickNode(node);
								}
							},
							clickLink: function(sender, link){
								if ( scope.cbkFunctions.clickLink ) {
									scope.cbkFunctions.clickLink(link);
								}
							}
						}

					});
				}


				// TODO: remove when come to decision about depiction of node status
				///**
				// * Creating new node status layer - circles representing device status - up, down
				// */
				//function createNodeStatusLayer(){
				//	nx.define("NodeStatus", nx.graphic.Topology.Layer, {
				//	nx.define("NodeStatus", nx.graphic.Topology.Layer, {
				//		methods: {
				//			draw: function() {
				//				var topo = this.topology();
				//				topo.eachNode(function(node) {
				//					var nodeStatus = node._model._data.status === 'operational' ? node._model._data.status : 'down',
				//						//type = node._model._data.type,
				//						dot = new nx.graphic.Circle({
				//							r: 6,
				//							cx: -30,
				//							cy: -0
				//						});
				//
				//					dot.set("fill", topoColors.getItem(nodeStatus));
				//					dot.attach(node);
				//					node.dot = dot;
				//				}, this);
				//			}
				//		}
				//	});
				//}

			}
		};
	};

	NextTopology.$inject=['NextTopologyService'];

	return NextTopology;
});