var modules = ['app/gbp-old/gbp.module',
               'app/gbp-old/gbp.services'
               ];


define(modules, function(gbpOld) {

    gbpOld.register.controller('gbpCtrl', ['$scope', '$rootScope', 'GBPTenantServices','DesignGbpFactory', 'GBPConstants',
        function ($scope, $rootScope, GBPTenantServices, DesignGbpFactory, GBPConstants) {
            $rootScope['section_logo'] = 'logo_gbp';
            $scope.view_path =  'src/app/gbp-old/views/';

            $scope.mainView = {
                main: true,
                'policy-renderer': false,
                governance: false
            };

            $scope.policyRendererView = {
                slider : true,
                basic : true,
                policy: false,
                tenants : false,
                l2l3 : false,
                epg : false,
                contracts : false,
                docs : false,
                groupMenu: false,
                classifiers: false,
                actions: false,
                registerEndpoint : false,
                registerL3PrefixEndpoint : false
            };

            $scope.subMenuView = {
                governance : false,
                policy : false,
                policySub : false,
                renderers : false,
                endpoints : false
            };

            $scope.breadcrumbs = {'l1' : null,
                                  'l2' : null,
                                  'l3' : null};

            $scope.wizards = {
                accessModelWizard: false,
                actionReferenceWizard: false
            };

            $scope.setBreadcrumb = function(level, label, visible){
                $scope.breadcrumbs[level] = visible ? label : null;
                if(level === 'l1'){
                    $scope.breadcrumbs.l2 = null;
                    $scope.breadcrumbs.l3 = null;
                }
                if(level === 'l2'){
                    $scope.breadcrumbs.l3 = null;
                }
            };

            $scope.setViewExpand = function(menu, expand, show){
                $scope[menu][expand] = show ? true : !$scope[menu][expand];
                for ( var property in $scope[menu] ) {
                    $scope[menu][property] = expand !== property ? false : $scope[menu][expand];
                }

            };

            $scope.toggleExpandedMM = function(expand, show, broadcast){
                $scope.setViewExpand('mainView',expand, show);

                if ( broadcast ) {
                    $scope.$broadcast(broadcast);
                }
            };

            $scope.sliceLabel = function(label){
                return label.length > GBPConstants.numbers.displayLabelLength ? label.slice(0,GBPConstants.numbers.displayLabelLength)+'...' : label;
            };

            $scope.getDisplayLabel = function(obj, labelArray, dontSlice){
                var ret = '';

                if((typeof labelArray) === 'string'){
                    ret = obj[labelArray];
                } else if (angular.isFunction(labelArray)) {
                    ret = labelArray(obj);
                } else {
                    labelArray.some(function(labelParam) {
                        if(angular.isFunction(labelParam)) {
                            ret = labelParam(obj);
                        } else if(obj.hasOwnProperty(labelParam)) {
                            ret = obj[labelParam];
                        }
                        return ret;
                    });
                }

                return dontSlice ? ret : $scope.sliceLabel(ret);
            };

            // TENANTS
            $scope.tenantList = [];
            $scope.selectedTenant = null;
            $scope.tenantDisplayLabel = ['name' , 'id'];

            $scope.loadTenants = function() {
                GBPTenantServices.load(
                    function(tenants) {
                        $scope.tenantList = tenants;
                        console.log('$scope.tenantList', $scope.tenantList);
                    },
                    function(){
                        //TODO error
                    });

                DesignGbpFactory.setMainClass();
            };

            $scope.setTenant = function(selectedTenant) {
                $scope.selectedTenant = selectedTenant;
                $scope.$broadcast('GBP_TENANT_RELOAD', $scope.selectedTenant);
            };

            $scope.loadTenants();

            $scope.$on('GBP_GLOBAL_TENANT_RELOAD',function(){
                $scope.loadTenants();
            });

            $scope.sendReloadEventFromRoot = function(eventName, val) {
                $scope.$broadcast(eventName, val);
            };


            // TODO: rework, use $scope.setViewContent or something
            $scope.showWizard = function(wizardName, broadcast, broadcastedData, path) {
                $scope.wizards[wizardName] = true;

                if ( broadcast ) {
                    $scope.sendReloadEventFromRoot(broadcast, {data: broadcastedData, path: path});
                }
            };

            $scope.closeWizard = function(wizardName) {
                $scope.wizards[wizardName] = false;
            };

            $scope.updateList = function(list, object, key) {
                var elementPos = list.map(function(x) {return x[key]; }).indexOf(object[key]);

                if(elementPos < 0) {
                    list.push(object);
                }
                else {
                    list[elementPos] = object;
                }
            };

    }]);

    gbpOld.register.controller('governanceCtrl', ['$rootScope','$scope',
        function ($rootScope, $scope) {
            $scope.menuTpl = 'main-menu';
            $scope.menuBox = null;
            $scope.contentTpl = 'main';
            // $scope.selectedTenant = null;
            $scope.rendererList = [];
            var broadcastObj = {};

            $scope.rendererList.push({'name' : 'of_overlay', 'id' : 0});

            $scope.toggleExpanded = function(expand, menu, tplType){
                var type = tplType ? tplType : 'contentTpl';
                $scope.menuBox = null;

                $scope[type] = expand;

                if ( menu && menu === false ) {
                    $scope.menuTpl = !menu ? null : menu;
                }
            };



            $scope.setViewContent = function(tplName, data, broadcast, tplType) {

                if ( $scope[tplType] === tplName ) {
                    $scope.$broadcast(broadcast, data, true);
                } else {

                    broadcastObj[tplType] = {};
                    broadcastObj[tplType].name = broadcast;
                    broadcastObj[tplType].data = data;

                    $scope.toggleExpanded(tplName, null, tplType);
                    $scope.$apply();

                }
            };

            $scope.broadcastCalling = function(tplType){
                $scope.$broadcast(broadcastObj[tplType].name, broadcastObj[tplType].data);
            };

            $scope.checkBroadcastCalling = function(tplName){
                var objClickAction = {
                    'epg-detail': function(){
                        $scope.broadcastCalling('contentTpl');
                    },
                    'subject-detail': function(){
                        $scope.broadcastCalling('contentTpl');
                    }
                };

                if ( angular.isFunction(objClickAction[tplName]) ) {
                    objClickAction[tplName]();
                }

            };


            $scope.$on('GOV_INIT', function(){
                $scope.menuTpl = 'main-menu';
                $scope.contentTpl = 'main';
                // $scope.$emit('GBP_GLOBAL_TENANT_RELOAD');
                $scope.menuBox = null;
            });

            $scope.$on('GBP_TENANT_RELOAD', function(e, obj){
                $scope.selectedTenant = obj;
            });


    }]);

    gbpOld.register.controller('boxStaticCtrl',['$scope',
        function($scope){

        $scope.selectedObj = null;

        $scope.getLabel = function(id, type){
                var objAction = {
                providerItems: function(){
                    var name = $scope.selectedObj[type][id].attributes.objData['name'];
                    return name ? name + ' : ' + id : id;
                },
                consumerItems: function(){
                    var name = $scope.selectedObj[type][id].attributes.objData['name'];
                    return name ? name + ' : ' + id : id;
                }
            };

            if ( angular.isFunction(objAction[type]) ) {
                return objAction[type]();
            }
        };

        $scope.$on('SET_SEL_STA_OBJ', function(obj, data, apply){
            $scope.selectedObj = data;

            if ( apply ) {
                $scope.$apply();
            }

        });

    }]);

    gbpOld.register.controller('graphCtrl', ['$scope', function($scope){
        var paper = null,
            paperScale = 1;

        $scope.init = function(paperInstance){
            paper = paperInstance;
            $scope.loadMouseScrollEvent();
        };


        $scope.zoom = function(out){
            paperScale = out ? paperScale - 0.1 : paperScale >= 1 ? 1 : paperScale + 0.1;
            paper.scale(paperScale, paperScale);
        };



        $scope.loadMouseScrollEvent = function(){
            /*mouse wheel event for zooming*/
            var graph = document.getElementById('graph'),
            MouseWheelHandler = function(e){
                var mouseEvent = window.event || e; // old IE support
                var delta = Math.max(-1, Math.min(1, (mouseEvent.wheelDelta || -mouseEvent.detail)));
                $scope.zoom(delta === 1 ? false : true);
            };

            if (graph.addEventListener) {
                // IE9, Chrome, Safari, Opera
                graph.addEventListener("mousewheel", MouseWheelHandler, false);
                // Firefox
                graph.addEventListener("DOMMouseScroll", MouseWheelHandler, false);
            }
            // IE 6/7/8
            else {graph.attachEvent("onmousewheel", MouseWheelHandler);}
            /*mouse wheel event for zooming - end*/
        };

    }]);

    gbpOld.register.controller('expressedPolicyCtrl', ['$scope', 'JointGraphFactory', 'JointGraphOffsetFactory', 'GBPConstants',
        function ($scope, JointGraphFactory, JointGraphOffsetFactory, GBPConstants) {
            var paper = JointGraphFactory.createGraph(),
                epgItems = [],
                contractItems = [],
                linkItems = [];

            var objClickAction = {
                epg: function(data){
                    $scope.setViewContent('epg-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                },
                contract: function(data){
                    $scope.setViewContent('contract-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                }
            };

            paper.on('cell:pointerdown', function(cell) {
                if ( angular.isFunction(objClickAction[cell.model.attributes.objType]) ) {
                    objClickAction[cell.model.attributes.objType](cell.model.attributes.objData);
                }
            });

            var createEpgLinks = function(epg, epgItem, contracts) {
                var providers = epg['provider-named-selector'] && epg['provider-named-selector'].length>0 ? epg['provider-named-selector'] : [];
                var consumers = epg['consumer-named-selector'] && epg['consumer-named-selector'].length>0 ? epg['consumer-named-selector'] : [];
                var consumerLinkItems = [];
                var providerLinkItems = [];

                consumers.forEach(function(c) {
                    c.contract.forEach(function(con) {
                        consumerLinkItems.push(JointGraphFactory.createLink(contracts[con].id, epgItem.id, 'green'));
                    });
                });

                providers.forEach(function(p) {
                    p.contract.forEach(function(con) {
                        providerLinkItems.push(JointGraphFactory.createLink(epgItem.id, contracts[con].id, 'blue'));
                    });
                });

                JointGraphFactory.addItemList(paper.model, providerLinkItems);
                JointGraphFactory.addItemList(paper.model, consumerLinkItems);

            };

            var loadData = function() {
                if($scope.selectedTenant) {
                    var offsetObj = {
                            ow: 100,
                            oh: 100,
                            w: 100,
                            h: 100
                        },
                        marginObj = {
                            w: 50,
                            h: 80
                        },
                        offsetHobj = {
                            contract: 0,
                            epg: 0
                        },
                        itemsArray = {
                            contract: [],
                            epg: []
                        };

                    JointGraphFactory.reloadGraph(paper.model);

                    if ( $scope.selectedTenant && $scope.selectedTenant.contract ) {

                        $scope.selectedTenant.contract.forEach(function(c, i) {
                            var label = c.description ? $scope.sliceLabel(c.description) : c.id,
                                width = JointGraphFactory.getLabelLength(label.length);
                                item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.contract, c, 'Click to see contract info', GBPConstants.colors.graph['subject'], 'Contract');

                            itemsArray.contract.push(item);

                            JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width, paper.options.height), paper);
                            JointGraphFactory.addItem(paper.model, item);
                            contractItems[c.id] = item;
                        });

                        offsetHobj.contract = offsetObj.h;

                    }

                    if ( $scope.selectedTenant && $scope.selectedTenant['endpoint-group'] ) {

                        JointGraphOffsetFactory.resetOffsets(offsetObj, offsetObj.ow, offsetObj.h > 400 ? offsetObj.h : 400);
                        $scope.selectedTenant['endpoint-group'].forEach(function(e, i) {
                            var label = e.name || e.id,
                                width = JointGraphFactory.getLabelLength(label.length);
                                item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.epg, e, 'Click to see epg info', GBPConstants.colors.graph['pns'], 'EP group');

                            itemsArray.epg.push(item);

                            JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width, paper.options.height), paper);
                            JointGraphFactory.addItem(paper.model, item);
                            epgItems[e.id] = item;

                            createEpgLinks(e, item, contractItems);
                        });

                    }

                    offsetHobj.epg = JointGraphOffsetFactory.getCurrentOffset(itemsArray.contract, 'y');
                    JointGraphOffsetFactory.checkObjsHoffsets(itemsArray.epg ,offsetHobj.epg, paper);
                }

            };

            $scope.getPaperObj = function(){
                return paper;
            };

            $scope.$on('GBP_TENANT_RELOAD',function(){
                if ($scope.selectedTenant) {
                    loadData();
                }
                else {
                    JointGraphFactory.reloadGraph(paper.model);
                }
            });

            loadData();
    }]);

    gbpOld.register.controller('deliveredPolicyCtrl', ['$scope', 'GPBServices', 'JointGraphFactory', 'GBPGovernanceServices', 'JointGraphOffsetFactory', 'GBPConstants',
        function ($scope, GPBServices, JointGraphFactory, GBPGovernanceServices, JointGraphOffsetFactory, GBPConstants) {
            var paper = JointGraphFactory.createGraph(),
                providerItems = {},
                consumerItems = {},
                subjectItems = {};

            var getEpList = function() {
                var providerEpKeys = Object.keys(providerItems),
                    consumerEpKeys = Object.keys(consumerItems),
                    epList = providerEpKeys.map(function (k) {
                        return providerItems[k].attributes.objData;
                    });

                consumerEpKeys.forEach(function(k) {
                    if(providerEpKeys.indexOf(k) === -1) {
                        epList.push(consumerItems[k].attributes.objData);
                    }
                });

                return epList;
            };

            var getSubjList = function() {
                return Object.keys(subjectItems).map(function (k) {
                    return subjectItems[k].attributes.objData;
                });
            };

            var objClickAction = {
                subject: function(data){
                        data.providerItems = providerItems;
                        data.consumerItems = consumerItems;
                    $scope.setViewContent('subject-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                },
                consumer: function(data) {
                    $scope.setViewContent('epg-content-static', data, 'SET_SEL_STA_OBJ', 'menuBox');
                },
                provider: function(data) {
                    $scope.setViewContent('epg-content-static', data, 'SET_SEL_STA_OBJ', 'menuBox');
                }
            };

            var objDblClickAction = {
                consumer: function(data){
                       $scope.setBreadcrumb('l3', 'Endpoint group detail', true);
                       $scope.setViewContent('epg-detail', { ep: data, epList: getEpList() }, 'SET_SELECTED_EPG','contentTpl');
                },
                provider: function(data){
                       $scope.setBreadcrumb('l3', 'Endpoint group detail', true);
                       $scope.setViewContent('epg-detail', { ep: data, epList: getEpList() }, 'SET_SELECTED_EPG','contentTpl');
                },
                subject: function(data) {
                        var obj = { subject: data,
                                    subjList: Object.keys(subjectItems).map(function (k) {
                                                return subjectItems[k].attributes.objData;
                                            }),
                                    providerItems: providerItems,
                                    consumerItems: consumerItems
                                };
                        $scope.setBreadcrumb('l3', 'Subject detail', true);
                        $scope.setViewContent('subject-detail', obj, 'SET_SELECTED_SUBJECT','contentTpl');
                }
            };

            paper.on('cell:pointerdown', function(cell) {
                if ( angular.isFunction(objClickAction[cell.model.attributes.objType]) ) {
                    objClickAction[cell.model.attributes.objType](cell.model.attributes.objData);
                }
            });

            paper.on('cell:pointerdblclick', function(cell) {
                objDblClickAction[cell.model.attributes.objType](cell.model.attributes.objData);
            });

            var createSubjectLinks = function(subject, subjectItem, providerItems, consumerItems) {
                var providerLinkItems = subject.providers.map(function(p) {
                        return JointGraphFactory.createLink(providerItems[p].id, subjectItem.id, 'green');
                    });
                    consumerLinkItems = subject.consumers.map(function(c) {
                        return JointGraphFactory.createLink(subjectItem.id, consumerItems[c].id, 'blue');
                    });

                JointGraphFactory.addItemList(paper.model, providerLinkItems);
                JointGraphFactory.addItemList(paper.model, consumerLinkItems);
            };

            var loadData = function() {
                if($scope.selectedTenant) {
                    providerItems = {};
                    consumerItems = {};
                    subjectItems = {};

                    var classifierInstances = $scope.selectedTenant['subject-feature-instances'] &&
                                          $scope.selectedTenant['subject-feature-instances']['classifier-instance'] &&
                                          $scope.selectedTenant['subject-feature-instances']['classifier-instance'].length > 0 ? $scope.selectedTenant['subject-feature-instances']['classifier-instance'] : [];

                    var offsetObj = {
                            ow: 100,
                            oh: 100,
                            w: 100,
                            h: 100
                        },
                        marginObj = {
                            w: 50,
                            h: 80
                        },
                        offsetHobj = {
                            pEpg: 0,
                            cEpg: 0,
                            subject: 0
                        },
                        itemsArray = {
                            pEpg: [],
                            cEpg: [],
                            subject: []
                        };

                    JointGraphFactory.reloadGraph(paper.model);

                    GBPGovernanceServices.getEPGsAndSubjects($scope.selectedTenant.id, classifierInstances, function(data){
                        data.providers.forEach(function(p, i) {
                            var relatedObj = GPBServices.getPropFromListByProp($scope.selectedTenant['endpoint-group'], 'id', p.id),
                                label = relatedObj.name || p.id,
                                width = JointGraphFactory.getLabelLength(label.length);

                            relatedObj.rules = p.rules;
                            var item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.provider, relatedObj, 'Click to see epg info, doubleclick to see Endpoint group detail', GBPConstants.colors.graph['pns'], 'Provider EPG');

                            itemsArray.pEpg.push(item);

                            JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width, paper.options.height), paper);
                            JointGraphFactory.addItem(paper.model, item);
                            providerItems[p.id] = item;
                        });

                        offsetHobj.pEpg = offsetObj.h;

                        JointGraphOffsetFactory.resetOffsets(offsetObj, offsetObj.ow, 500);
                        data.consumers.forEach(function(c, i) {
                            var relatedObj = GPBServices.getPropFromListByProp($scope.selectedTenant['endpoint-group'], 'id', c.id),
                                label = relatedObj.name || c.id,
                                width = JointGraphFactory.getLabelLength(label.length);

                            relatedObj.rules = c.rules;
                            var item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.consumer, relatedObj, 'Click to see epg info, doubleclick to see Endpoint group detail', GBPConstants.colors.graph['cns'], 'Consumer EPG');

                            itemsArray.cEpg.push(item);

                            JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width, paper.options.height), paper);
                            JointGraphFactory.addItem(paper.model, item);
                            consumerItems[c.id] = item;
                        });

                        JointGraphOffsetFactory.resetOffsets(offsetObj, offsetObj.ow, offsetHobj.pEpg > 300 ? offsetHobj.pEpg : 300);
                        data.subjects.forEach(function(s, i) {
                            var label = s.name,
                                width = JointGraphFactory.getLabelLength(label.length),
                                item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.subject, s, 'Click to see subject info, doubleclick to see Subject detail', GBPConstants.colors.graph['subject'], 'Subject');

                            itemsArray.subject.push(item);

                            JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width, paper.options.height), paper);
                            JointGraphFactory.addItem(paper.model, item);
                            subjectItems[s.name] = item;
                            createSubjectLinks(s, item, providerItems, consumerItems);
                        });

                        offsetHobj.pEpg = JointGraphOffsetFactory.getCurrentOffset(itemsArray.pEpg, 'y');
                        JointGraphOffsetFactory.checkObjsHoffsets(itemsArray.subject ,offsetHobj.pEpg, paper);
                        offsetHobj.subject = JointGraphOffsetFactory.getCurrentOffset(itemsArray.subject, 'y');
                        JointGraphOffsetFactory.checkObjsHoffsets(itemsArray.cEpg ,offsetHobj.subject, paper);

                    }, function(){});

                }


                // paper.scaleContentToFit();
                // paper.fitToContent();
            };

            $scope.getPaperObj = function(){
                return paper;
            };


            $scope.$on('GBP_TENANT_RELOAD',function(){
                if ($scope.selectedTenant) {
                    loadData();
                }
                else {
                    JointGraphFactory.reloadGraph(paper.model);
                }
            });

            loadData();
    }]);

    gbpOld.register.controller('subjectDetailCtrl', ['$scope', 'GPBServices', 'JointGraphFactory', 'GBPGovernanceServices', 'JointGraphOffsetFactory', 'GBPConstants',
        function ($scope, GPBServices, JointGraphFactory, GBPGovernanceServices, JointGraphOffsetFactory, GBPConstants) {
            $scope.selectedSubject = null;
            $scope.subjectList = [];

            var paper = JointGraphFactory.createGraph(),
                subjectItem = null,
                ruleItems = {},
                subjectItems = {};

            var createSubjectLinks = function(subjectItem, ruleItem) {
                var linkItem = JointGraphFactory.createLink(subjectItem.id, ruleItem.id, 'blue');
                JointGraphFactory.addItem(paper.model, linkItem);
            };

            paper.on('cell:pointerdown', function(cell) {
                var objClickAction = {
                    subject: function(data){
                        $scope.setViewContent('subject-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                    },
                    rule: function(data){
                        $scope.setViewContent('rule-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                    }
                };

                // console.log('cell.model.attributes.objType', cell.model.attributes.objType, cell.model.attributes.objData);
                if ( angular.isFunction(objClickAction[cell.model.attributes.objType]) ) {
                    objClickAction[cell.model.attributes.objType](cell.model.attributes.objData);
                }
            });

            var loadData = function() {
                if($scope.selectedSubject) {
                    subjectItem = null;
                    ruleItems = {};

                    var offsetObj = {
                            ow: 100,
                            oh: 100,
                            w: 100,
                            h: 100
                        },
                        marginObj = {
                            w: 50,
                            h: 80
                        };

                    JointGraphFactory.reloadGraph(paper.model);

                    var label = $scope.selectedSubject.name || $scope.selectedSubject.id,
                        width = JointGraphFactory.getLabelLength(label.length);
                        subjectItem = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.subject, $scope.selectedSubject, 'Click to see subject info', GBPConstants.colors.graph['subject'], 'Subject');

                    JointGraphFactory.addItem(paper.model, subjectItem);

                    JointGraphOffsetFactory.resetOffsets(offsetObj, offsetObj.ow, 300);
                    $scope.selectedSubject.rules.forEach(function(r, i) {
                        var label = r.name,
                            width = JointGraphFactory.getLabelLength(label.length);
                            item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, GBPConstants.objType.rule, r, 'Click to see rule info', GBPConstants.colors.graph['cns'], 'Rule');

                        JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width), paper);
                        JointGraphFactory.addItem(paper.model, item);
                        ruleItems[r.name] = item;

                        createSubjectLinks(subjectItem, item);
                    });
                }
            };

            $scope.getPaperObj = function(){
                return paper;
            };

            $scope.$on('SET_SELECTED_SUBJECT', function(event, data){
                $scope.selectedSubject = data.subject;
                $scope.subjectList = data.subjList;
                loadData();
            });

            $scope.setSubject = function(subject) {
                $scope.selectedSubject = subject;
                loadData();
            };

            loadData();
    }]);

    gbpOld.register.controller('epgDetailCtrl', ['$scope', 'JointGraphFactory', 'TopologyDataLoaders', 'GBPEpgServices', 'JointGraphOffsetFactory', 'GBPConstants',
        function ($scope, JointGraphFactory, TopologyDataLoaders, GBPEpgServices, JointGraphOffsetFactory, GBPConstants) {
            var paper = JointGraphFactory.createGraph(),
                epgItem = {},
                epItems = {};

            $scope.epgDisplayLabel = ['name', 'id'];
            $scope.epgList = [];
            $scope.selectedEpg = null;

            $scope.$on('SET_SELECTED_EPG', function(event, epg){
                $scope.selectedEpg = epg.ep;
                $scope.epgList = epg.epList;
                loadData();
            });

            $scope.setEpg = function(epg){
                selectedEpg = epg;
                loadData();
            };

            paper.on('cell:pointerdown', function(cell) {
                var objClickAction = {
                    ep: function(data){
                        $scope.setViewContent('ep-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                    },
                    epg: function(data){
                        $scope.setViewContent('epg-content-static',data, 'SET_SEL_STA_OBJ', 'menuBox');
                    },
                };

                if ( angular.isFunction(objClickAction[cell.model.attributes.objType]) ) {
                    objClickAction[cell.model.attributes.objType](cell.model.attributes.objData);
                }
            });

            var loadData = function() {
                if($scope.selectedTenant && $scope.selectedEpg) {
                    epgItem = {};
                    epItems = {};
                    links = [];

                    JointGraphFactory.reloadGraph(paper.model);

                    TopologyDataLoaders.getEndpointsFromEndpointGroup($scope.selectedTenant.id, $scope.selectedEpg.id, function(data){
                       var offsetObj = {
                            ow: 100,
                            oh: 100,
                            w: 100,
                            h: 100
                            },
                            marginObj = {
                                w: 50,
                                h: 80
                            };

                        var label = $scope.selectedEpg.name || $scope.selectedEpg.id,
                            width = JointGraphFactory.getLabelLength(label.length);
                            epgItem = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, 'epg', $scope.selectedEpg, 'Click to see epg info', GBPConstants.colors.graph['subject'], 'EPG');

                        JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width), paper);
                        JointGraphFactory.addItem(paper.model, epgItem);

                        JointGraphOffsetFactory.resetOffsets(offsetObj, offsetObj.ow, 500);
                        if(data && data.output && data.output['ui-endpoint'] && data.output['ui-endpoint'].length){
                            data.output['ui-endpoint'].forEach(function(ep, i){
                                var label = ep['mac-address'] + ':' + ep['l2-context'],
                                    width = JointGraphFactory.getLabelLength(label.length);
                                    item = JointGraphFactory.createElement(label, offsetObj.w, offsetObj.h, width, null, 'ep', ep, 'Click to see ep info', GBPConstants.colors.graph['pns'], 'Endpoint');

                                JointGraphOffsetFactory.updateOffsets(JointGraphOffsetFactory.createWHObj(width), offsetObj, marginObj, JointGraphOffsetFactory.createWHObj(paper.options.width), paper);
                                JointGraphFactory.addItem(paper.model, item);
                                epItems[label] = item;
                                links.push(JointGraphFactory.createLink(item.id, epgItem.id, 'green'));
                            });

                            JointGraphFactory.addItemList(paper.model, links);
                        }
                    }, function(){});
                }
            };

            $scope.getPaperObj = function(){
                return paper;
            };

            // init();
    }]);

    gbpOld.register.controller('policyRendererCtrl', ['$scope', '$http', '$timeout', 'PGNServices', 'TopoServices', 'GBPTenantServices', 'GBPConstants', 'JointGraphFactory','GBPJointGraphBuilder',
        function ($scope, $http, $timeout, PGNServices, TopoServices, GBPTenantServices, GBPConstants, JointGraphFactory, GBPJointGraphBuilder) {

            $scope.topologyData = { nodes: [], links: [] };
            $scope.topologyType = null;
            $scope.topologyArgs = {};
            $scope.legend = {};
            $scope.showLegend = false;

            var paper = JointGraphFactory.createGraph();

            var reloadShowLegend = function() {
                $scope.showLegend = !$.isEmptyObject($scope.legend);
            };

            $scope.settingsSigma = {
                defaultLabelColor: '#fff',
                doubleClickEnabled: false,
                labelThreshold: 8
            };

            $scope.settingsAtlas = {
                adjustSizes: true,
                gravity: 0.2
            };

            $scope.viewTopo = {
                box: false,
                button: false
            };

            paper.on('cell:pointerdown', function(cellView, evt) {
                if (cellView.model.isLink() && cellView.model.attributes.objData) {
                    $scope.$broadcast('SET_LINK_DATA', cellView.model.attributes.objData);
                }
            });

            $scope.mandatoryProperties = [];
            $scope.loadTopology = function(type, args) {
                if ($scope.selectedTenant) {
                    $scope.topologyType = type;
                    $scope.topologyArgs = args;
                    GBPJointGraphBuilder.loadTopology(args, paper, type);
                }
            };
            $scope.toggleExpanded = function(expand, show) {
                $scope.setViewExpand('policyRendererView',expand, show, 'l2');

                if($scope.policyRendererView[expand] && $scope.selectedTenant) {
                    $scope.topologyArgs.tenantId = $scope.selectedTenant.id;

                    if((expand === 'epg' || expand === 'contracts' || expand === 'classifiers' || expand === 'actions' || expand === 'renderers') && ($scope.topologyType !== GBPConstants.strings.config)) {
                        $scope.loadTopology(GBPConstants.strings.config, $scope.topologyArgs);
                    } else if((expand === 'l2l3' || expand === 'registerEndpoint' || expand === 'registerL3PrefixEndpoint') && ($scope.topologyType !== GBPConstants.strings.l2l3)) {
                        $scope.loadTopology(GBPConstants.strings.l2l3, $scope.topologyArgs);
                    }
                }
            };

            $scope.reloadTopo = function() {
                if($scope.selectedTenant) {
                    $scope.topologyArgs.tenantId = $scope.selectedTenant.id;
                    GBPJointGraphBuilder.loadTopology($scope.topologyArgs, paper, $scope.topologyType);
                }
            };

            $scope.getDisplayLabelsFromCtrl = function(eventName, val) {
                $scope.$broadcast(eventName, val);
            };

            $scope.validateMandatory = function(newObj, mandatoryProps){
                var ret = true,
                    notFilledProps = [];

                mandatoryProps.forEach(function(el){
                    if(newObj[el] === '' || newObj[el] === null || newObj[el] === undefined){
                        notFilledProps.push(el);
                        ret = false;
                    }
                });

                return {'status' : ret, 'notFilledProps' : notFilledProps};
            };

            $scope.validate = function(value, errors){
                errors.int32 = !(parseInt(value) >= -2147483648 && parseInt(value) <= 2147483647);
            };



            $scope.validateForm = function(form) {
                return form.$valid;
            };

            $scope.$on('GBP_TENANT_RELOAD', function(e, obj){
                $scope.selectedTenant = obj;
            });

    }]);

    gbpOld.register.controller('linkDataCtrl',['$scope', function($scope){
        $scope.showTable = false;

        $scope.show = function(){
            $scope.showTable = true;
        };

        $scope.close = function(){
            $scope.showTable = false;
        };

        $scope.$on('SET_LINK_DATA', function(e, obj){
           $scope.linkData = obj;
           $scope.show();
           $scope.$apply();
        });
    }]);

    gbpOld.register.controller('crudCtrl',['$scope',  function($scope){
        $scope.selectedObj = null;
        $scope.label = '';
        $scope.q = {};

        $scope.add = function() {
            $scope.selectedObj = null;
            $scope.showForm();
        };

        $scope.modify = function() {
            $scope.$emit('PGN_EDIT_ELEM');
        };

        $scope.init = function(label) {
            $scope.label = label;
        };


        $scope.$on('EV_SET_SEL_CLASS', function(event, selObj){
            $scope.selectedObj = selObj;
        });
    }]);

    gbpOld.register.controller('contractCtrl', ['$scope','GBPContractServices', '$filter', function($scope, GBPContractServices, $filter){
        $scope.list = [];
        $scope.selectedContract = null;
        $scope.newContractObj = GBPContractServices.createObj();
        $scope.displayLabel = ['description','id'];
        $scope.crudLabel = 'Contract list';

        $scope.internalView = {
            contract: false,
            edit: "view"
        };

        var path = null,
            mandatoryProperties = [],
            clear = function(){
                $scope.list = [];
                $scope.internalView = {
                    contract: false,
                    edit: "view"
                };
                $scope.selectedContract = null;
                $scope.newContractObj = GBPContractServices.createObj();
            };

        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                $scope.selectedContract = null;
                path = GBPContractServices.createPathObj($scope.selectedTenant.id);

                GBPContractServices.load(path, function(data){
                    $scope.list = data;
                    // $scope.$broadcast('GBP_CONTRACT_RELOAD');
                    $scope.sendReloadEventFromRoot('GBP_CONTRACT_RELOAD');
                }, function(){

                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            var resp = $scope.validateMandatory($scope.newContractObj, mandatoryProperties);
            if(resp.status){
                path = GBPContractServices.createPathObj($scope.selectedTenant.id, $scope.newContractObj.id);
                GBPContractServices.send(path, $scope.newContractObj, function(data){
                    $scope.init();
                    $scope.internalView.contract = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }else{
                alert($filter('translate')('GBP_MANDATORY_NOT_FILLED')+': '+resp.notFilledProps.join(', '));
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedContract ) {
                path = GBPContractServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id);
                GBPContractServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedContract = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newContractObj = GBPContractServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedContract = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.contract = true;
            }else {
                $scope.internalView.contract = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.contract) {
                angular.copy(selectedObj, $scope.newContractObj);
            }
            $scope.sendReloadEventFromRoot('GBP_CONTRACT_RELOAD');
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.contract = true;
            $scope.selectedContract = null;
            $scope.internalView.edit = "add";
        };

        $scope.close = function(){
            $scope.internalView.contract = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if(!event.defaultPrevented) {
                if ( $scope.selectedContract ) {
                    $scope.internalView.contract = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedContract, $scope.newContractObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(event){
            $scope.init();
        });

        $scope.$on('GBP_CONTRACTS_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('clauseCtrl', ['$scope','GBPClauseServices', 'GBPSubjectServices',
        function($scope, GBPClauseServices, GBPSubjectServices){
        $scope.list = [];
        $scope.selectedClause = null;
        $scope.newClauseObj = GBPClauseServices.createObj();
        $scope.internalView = {
            clause: false,
            edit: "view"
        };
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Clause list';

        $scope.subjects = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_SUBJECTS_LABEL', $scope.subjects);

        var path = null,

            clear = function(){
                $scope.list = [];
                $scope.selectedClause = null;
                $scope.newClauseObj = GBPClauseServices.createObj();
                $scope.internalView = {
                    clause: false,
                    edit: "view"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs']) {
                var objToPush = templateObj || "";
                $scope.newClauseObj['subject-refs'].push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs']) {
                $scope.newClauseObj['subject-refs'].splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs'] && $scope.newClauseObj['subject-refs'].length >= index) {
                $scope.newClauseObj['subject-refs'][index] = value;
            }
        };
        //move to separate ctrl /\

        var loadSubjects = function() {
            $scope.getDisplayLabelsFromCtrl('GBP_SUBJECTS_LABEL', $scope.subjects);

            GBPSubjectServices.load(path, function(data){
                $scope.subjects.options = data;
            }, function(){
                //TODO: error cbk
            });
        };

        $scope.init = function() {
            if ( $scope.selectedContract ) {
                path = GBPClauseServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id);

                GBPClauseServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.clauseForm)){
                path = GBPClauseServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.newClauseObj.name);
                GBPClauseServices.send(path, $scope.newClauseObj, function(data){
                    $scope.init();
                    $scope.internalView.clause = false;
                    $scope.reloadNewObj();
                    $scope.internalView.clause = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedClause ) {
                path = GBPClauseServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedClause.name);
                GBPClauseServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedClause = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newClauseObj = GBPClauseServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedClause = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.clause = true;
            }else {
                $scope.internalView.clause = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.clause) {
                angular.copy(selectedObj, $scope.newClauseObj);
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.clause = true;
            $scope.internalView.edit = "add";
            $scope.selectedClause = null;
        };

        $scope.close = function(){
            $scope.internalView.clause = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedClause ) {
                    $scope.internalView.clause = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedClause, $scope.newClauseObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_CONTRACT_RELOAD',function(){
            $scope.internalView.clause = false;
            $scope.init();
        });

        $scope.$on('GBP_SUBJECT_RELOAD',function(){
            loadSubjects();
        });
    }]);

    gbpOld.register.controller('subjectCtrl', ['$scope','GBPSubjectServices', '$filter', function($scope, GBPSubjectServices, $filter){
        $scope.list = [];
        $scope.selectedSubject = null;
        $scope.newSubjectObj = GBPSubjectServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            subject : false,
            edit : "view"
        };
        $scope.crudLabel = 'Subject list';
        $scope.errors = {};

        var path = null,
            mandatoryProperties = ['order'],

            clear = function(){
                $scope.list = [];
                $scope.selectedSubject = null;
                $scope.newSubjectObj = GBPSubjectServices.createObj();
                $scope.internalView = {
                    subject : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedContract ) {
                $scope.selectedSubject = null;
                path = GBPSubjectServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id);

                GBPSubjectServices.load(path, function(data){
                    $scope.list = data;
                    $scope.sendReloadEventFromRoot('GBP_SUBJECT_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.subjectForm)){
                path = GBPSubjectServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.newSubjectObj.name);
                GBPSubjectServices.send(path, $scope.newSubjectObj, function(data){
                    $scope.init();
                    $scope.internalView.subject = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedSubject ) {
                path = GBPSubjectServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name);
                GBPSubjectServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedSubject = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newSubjectObj = GBPSubjectServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedSubject = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.subject = true;
            }else {
                $scope.internalView.subject = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.subject) {
                angular.copy(selectedObj, $scope.newSubjectObj);
            }
            $scope.sendReloadEventFromRoot('GBP_SUBJECT_RELOAD');
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.subject = true;
            $scope.internalView.edit = "add";
            $scope.selectedSubject = null;
        };

        $scope.close = function(){
            $scope.internalView.subject = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedSubject ) {
                    $scope.internalView.subject = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedSubject, $scope.newSubjectObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_CONTRACT_RELOAD',function(){
            $scope.internalView.subject = false;
            $scope.init();
        });

        $scope.$on('GBP_SUBJECTS_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('ruleCtrl', ['$scope','GBPRuleServices', '$filter', function($scope, GBPRuleServices, $filter){
        $scope.list = [];
        $scope.selectedRule = null;
        $scope.newRuleObj = GBPRuleServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            rule : false,
            edit : "view"
        };
        $scope.crudLabel = 'Rule list';
        $scope.errors = {};

        var path = null,
            mandatoryProperties = ['order'],

            clear = function(){
                $scope.list = [];
                $scope.selectedRule = null;
                $scope.newRuleObj = GBPRuleServices.createObj();
                $scope.internalView = {
                    rule : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedSubject ) {
                $scope.selectedRule = null;
                path = GBPRuleServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name);

                GBPRuleServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.rulesForm)){
                path = GBPRuleServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.newRuleObj.name);
                GBPRuleServices.send(path, $scope.newRuleObj, function(data){
                    $scope.init();
                    $scope.internalView.rule = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedRule ) {
                path = GBPRuleServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name);
                GBPRuleServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedRule = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newRuleObj = GBPRuleServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedRule = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.rule = true;
            }else {
                $scope.internalView.rule = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.rule) {
                angular.copy(selectedObj, $scope.newRuleObj);
            }
            $scope.$broadcast('GBP_RULE_RELOAD');
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.rule = true;
            $scope.internalView.edit = "add";
            $scope.selectedRule = null;
        };

        $scope.close = function(){
            $scope.internalView.rule = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedRule ) {
                $scope.internalView.rule = true;
                $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedRule, $scope.newRuleObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_SUBJECT_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('actionRefCtrl', ['$scope','GBPActionRefsServices', 'GBPActionInstanceServices', '$filter', function($scope, GBPActionRefsServices, GBPActionInstanceServices, $filter){
        $scope.list = [];
        $scope.selectedActionRef = null;
        $scope.newActionRefObj = GBPActionRefsServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            actionRef : false,
            edit : "view"
        };
        $scope.crudLabel = 'Action ref list';
        $scope.errors = {};

        $scope.actionInstanceNames = {'options' : [], 'labels' : $scope.displayLabel};

        var path = null,
            mandatoryProperties = ['order'],

            actionInstanceNamesLoad = function() {
                var actionInstancePath = GBPActionInstanceServices.createPathObj($scope.selectedTenant.id);
                GBPActionInstanceServices.load(actionInstancePath, function(data){
                    $scope.actionInstanceNames.options = data;
                },function(){
                    //TODO: error cbk
                });
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedActionRef = null;
                $scope.newActionRefObj = GBPActionRefsServices.createObj();
                $scope.internalView = {
                    actionRef : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedRule ) {
                $scope.selectedActionRef = null;
                path = GBPActionRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name);

                GBPActionRefsServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });

                actionInstanceNamesLoad();
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.actionRefForm)){
                path = GBPActionRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name, $scope.newActionRefObj.name);
                GBPActionRefsServices.send(path, $scope.newActionRefObj, function(data){
                    $scope.init();
                    $scope.internalView.actionRef = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedActionRef ) {
                path = GBPActionRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name, $scope.selectedActionRef.name);
                GBPActionRefsServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedActionRef = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newActionRefObj = GBPActionRefsServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedActionRef = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.actionRef = true;
            }else {
                $scope.internalView.actionRef = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.actionRef) {
                angular.copy(selectedObj, $scope.newActionRefObj);
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.actionRef = true;
            $scope.internalView.edit = "add";
            $scope.selectedActionRef = null;
        };

        $scope.close = function(){
            $scope.internalView.actionRef = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedActionRef ) {
                    $scope.internalView.actionRef = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedActionRef, $scope.newActionRefObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_RULE_RELOAD',function(){
            $scope.internalView.actionRef = false;
            $scope.init();
        });

        $scope.$on('GBP_ACTION_INSTANCE_RELOAD',function(){
            actionInstanceNamesLoad();
        });
    }]);

    gbpOld.register.controller('classifierRefCtrl', ['$scope','GBPClassifierRefsServices', 'GBPClassifierInstanceServices', '$filter', function($scope, GBPClassifierRefsServices, GBPClassifierInstanceServices, $filter){
        $scope.list = [];
        $scope.selectedClassifierRef = null;
        $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            classifierRef : false,
            edit : "view"
        };

        $scope.instanceNames = {'options' : [], 'labels' : $scope.displayLabel};

        $scope.formDirections = ['in', 'out', 'bidirectional'];
        $scope.formConnectionTracking = ['normal', 'reflexive'];

        $scope.crudLabel = 'Classifier ref list';

        var path = null,

            instanceNamesLoad = function() {
                var classifierInstancePath = GBPClassifierInstanceServices.createPathObj($scope.selectedTenant.id);
                GBPClassifierInstanceServices.load(classifierInstancePath, function(data){
                    $scope.instanceNames.options = data;
                },function(){
                    //TODO: error cbk
                });
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedClassifierRef = null;
                $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
                $scope.internalView = {
                    classifierRef : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedRule ) {
                $scope.selectedClassifierRef = null;
                path = GBPClassifierRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name);



                GBPClassifierRefsServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });

                instanceNamesLoad();
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.classifierRefForm)){
                path = GBPClassifierRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name, $scope.newClassifierRefObj.name);
                GBPClassifierRefsServices.send(path, $scope.newClassifierRefObj, function(data){
                    $scope.init();
                    $scope.internalView.classifierRef = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if ( $scope.selectedClassifierRef ) {
                path = GBPClassifierRefsServices.createPathObj($scope.selectedTenant.id, $scope.selectedContract.id, $scope.selectedSubject.name, $scope.selectedRule.name, $scope.selectedClassifierRef.name);
                GBPClassifierRefsServices.delete(path, function(data){
                    $scope.init();
                    $scope.selectedClassifierRef = null;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedClassifierRef = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.classifierRef = true;
            }else {
                $scope.internalView.classifierRef = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.classifierRef) {
                angular.copy(selectedObj, $scope.newClassifierRefObj);
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.classifierRef = true;
            $scope.internalView.edit = "add";
            $scope.selectedClassifierRef = null;
        };

        $scope.close = function(){
            $scope.internalView.classifierRef = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedClassifierRef ) {
                    $scope.internalView.classifierRef = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedClassifierRef, $scope.newClassifierRefObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_RULE_RELOAD',function(){
            $scope.internalView.classifierRef = false;
            $scope.init();
        });

        $scope.$on('GBP_CLASSIFIER_INSTANCE_RELOAD',function(){
            instanceNamesLoad();
        });
    }]);

    gbpOld.register.controller('tenantCtrl', ['$scope', 'GBPTenantServices', '$filter', function($scope, GBPTenantServices, $filter){
        $scope.list = [];
        $scope.selectedTenantObj = null;
        $scope.newTenantObj = GBPTenantServices.createObj();
        $scope.displayLabel = ['name' , 'id'];
        $scope.crudLabel = 'Tenants list';

        $scope.view = {
            tenant: false,
            edit: "view"
        };

        $scope.init = function() {
            GBPTenantServices.load(
                function(data) {
                    $scope.list = data;
                    $scope.newTenantObj = GBPTenantServices.createObj();
                    $scope.selectedTenantObj = null;
                },
                function(){
                    //TODO error
                });
        };

        $scope.save = function(){
            if($scope.validateForm($scope.tenantForm)){
                path = GBPTenantServices.createPathObj($scope.newTenantObj.id);
                GBPTenantServices.send(path, $scope.newTenantObj, function(data){
                    $scope.init();
                    $scope.view.tenant = false;
                    $scope.view.edit = "view";
                    $scope.$emit('GBP_GLOBAL_TENANT_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

         $scope.delete = function() {
            if($scope.selectedTenantObj) {
                path = GBPTenantServices.createPathObj($scope.selectedTenantObj.id);

                GBPTenantServices.delete(path, function(data){
                    $scope.init();
                    $scope.view.tenant = false;
                    $scope.$emit('GBP_GLOBAL_TENANT_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedTenantObj = selectedObj;

            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.tenant = true;
            }
            else {
                $scope.view.tenant = false;
                $scope.view.edit = "view";
            }

            if ($scope.view.tenant) {
                angular.copy(selectedObj, $scope.newTenantObj);
            }
        };

        $scope.showForm = function() {
            $scope.newTenantObj = GBPTenantServices.createObj();
            $scope.selectedTenantObj = null;
            $scope.view.tenant = true;
            $scope.view.edit = "add";
        };

        $scope.close = function(){
            $scope.view.tenant = false;
            $scope.view.edit = "view";
        };

       $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedTenantObj ) {
                    $scope.view.tenant = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedTenantObj, $scope.newTenantObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_GLOBAL_TENANT_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('epgCtrl',['$scope', 'GBPEpgServices', 'GBPContractServices', '$filter',
        function($scope, GBPEpgServices, GBPContractServices, $filter){
        $scope.selectedEpg = null;
        $scope.newEpgObj = GBPEpgServices.createObj();
        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'Group list';

        $scope.igpOpts = ['allow', 'require-contract'];

        $scope.contracts = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_CONTRACTS_LABEL', $scope.contracts);

        $scope.list = [];

        $scope.internalView = {
            epg: false,
            edit: "view"
        };

        var loadContracts = function() {
                GBPContractServices.load(path, function(data){
                    $scope.contracts.options = data;
                }, function(){
                    //TODO: error cbk
                });
            },

            mandatoryProperties = ['name'],

            clear = function(){
                $scope.list = [];
                $scope.selectedEpg = null;
                $scope.newEpgObj = GBPEpgServices.createObj();
                 $scope.internalView = {
                    epg: false,
                    edit: "view"
                };
            };

        $scope.init = function() {
            if ($scope.selectedTenant) {
                path = GBPEpgServices.createPathObj($scope.selectedTenant.id);

                GBPEpgServices.load(path, function(data){
                    $scope.list = data;
                    // $scope.$broadcast('GBP_EPG_RELOAD');
                    $scope.sendReloadEventFromRoot('GBP_EPG_RELOAD');
                }, function(){
                    //TODO: error cbk
                });

                loadContracts();
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.epgForm)){
                path = GBPEpgServices.createPathObj($scope.selectedTenant.id, $scope.newEpgObj.id);
                GBPEpgServices.send(path, $scope.newEpgObj, function(data){
                    $scope.init();
                    $scope.internalView.epg = false;
                    $scope.reloadNewObj();
                    $scope.internalView.edit = "view";
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedEpg) {
                path = GBPEpgServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id);
                GBPEpgServices.delete(path, function(data){
                    $scope.init();
                    $scope.internalView.epg = false;
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.internalView.epg = true;
            $scope.reloadNewObj();
            $scope.selectedEpg = null;
            $scope.internalView.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newEpgObj = GBPEpgServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedEpg = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.epg = true;
            }else {
                $scope.internalView.epg = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.epg) {
                angular.copy(selectedObj, $scope.newEpgObj);
            }
            $scope.sendReloadEventFromRoot('GBP_EPG_RELOAD');
        };

        $scope.close = function(){
            $scope.internalView.epg = false;
            $scope.internalView.edit = "view";
        };
        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.internalView.epg = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedEpg, $scope.newEpgObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_CONTRACT_RELOAD',function(){
            loadContracts();
        });

        $scope.$on('GBP_EPG_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('cnsCtrl',['$scope', 'GBPConNamedSelServices', function($scope, GBPConNamedSelServices){
        $scope.list = [];
        $scope.selectedCNS = null;
        $scope.newCNSObj = GBPConNamedSelServices.createObj();
        $scope.internalView = {
            cns: false,
            edit: "view"
        };
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Consumer named selectors list';

        var clear = function(){
                $scope.list = [];
                $scope.selectedCNS = null;
                $scope.newCNSObj = GBPConNamedSelServices.createObj();
                $scope.internalView = {
                    cns: false,
                    edit: "view"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newCNSObj && $scope.newCNSObj.contract) {
                var objToPush = templateObj || "";
                $scope.newCNSObj.contract.push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newCNSObj && $scope.newCNSObj.contract) {
                $scope.newCNSObj.contract.splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newCNSObj && $scope.newCNSObj.contract && $scope.newCNSObj.contract.length >= index) {
                $scope.newCNSObj.contract[index] = value;
            }
        };
        //move to separate ctrl /\

        $scope.init = function() {
            if ($scope.selectedTenant && $scope.selectedEpg) {
                path = GBPConNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id);

                GBPConNamedSelServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.cnsForm)){
                path = GBPConNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id, $scope.newCNSObj.name);
                GBPConNamedSelServices.send(path, $scope.newCNSObj, function(data){
                    $scope.init();
                    $scope.internalView.cns = false;
                    $scope.internalView.cns = "view";
                    $scope.reloadNewObj();
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedEpg && $scope.selectedCNS) {
                path = GBPConNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id, $scope.selectedCNS.name);
                GBPConNamedSelServices.delete(path, function(data){
                    $scope.init();
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.selectedCNS = null;
            $scope.internalView.cns = true;
            $scope.internalView.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newCNSObj = GBPConNamedSelServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedCNS = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.cns = true;
            }else {
                $scope.internalView.cns = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.cns) {
                angular.copy(selectedObj, $scope.newCNSObj);
            }
        };

        $scope.close = function(){
            $scope.internalView.cns = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.internalView.cns = true;
                    $scope.internalView.edit = "add";
                    angular.copy($scope.selectedCNS, $scope.newCNSObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_EPG_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('pnsCtrl',['$scope', 'GBPProNamedSelServices', function($scope, GBPProNamedSelServices){
        $scope.list = [];
        $scope.selectedPNS = null;
        $scope.newPNSObj = GBPProNamedSelServices.createObj();
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Provider named selectors list';
        $scope.internalView = {
            pns: false,
            edit: "view"
        };

        var clear = function(){
                $scope.list = [];
                $scope.selectedPNS = null;
                $scope.newPNSObj = GBPProNamedSelServices.createObj();
                $scope.internalView = {
                    pns: false,
                    edit: "view"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newPNSObj && $scope.newPNSObj.contract) {
                var objToPush = templateObj || "";
                $scope.newPNSObj.contract.push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newPNSObj && $scope.newPNSObj.contract) {
                $scope.newPNSObj.contract.splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newPNSObj && $scope.newPNSObj.contract && $scope.newPNSObj.contract.length >= index) {
                $scope.newPNSObj.contract[index] = value;
            }
        };
        //move to separate ctrl /\

        $scope.init = function() {
            if ($scope.selectedTenant && $scope.selectedEpg) {
                path = GBPProNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id);

                GBPProNamedSelServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.pnsForm)){
                path = GBPProNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id, $scope.newPNSObj.name);
                GBPProNamedSelServices.send(path, $scope.newPNSObj, function(data){
                    $scope.init();
                    $scope.internalView.pns = false;
                    $scope.reloadNewObj();
                    $scope.internalView.cns = "view";
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedEpg && $scope.selectedPNS) {
                path = GBPProNamedSelServices.createPathObj($scope.selectedTenant.id, $scope.selectedEpg.id, $scope.selectedPNS.name);
                GBPProNamedSelServices.delete(path, function(data){
                    $scope.init();
                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.pns = true;
            $scope.internalView.edit = "add";
            $scope.selectedPNS = null;
        };

        $scope.reloadNewObj = function() {
            $scope.newPNSObj = GBPProNamedSelServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedPNS = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.pns = true;
            }else {
                $scope.internalView.pns = false;
                $scope.internalView.edit = "view";
            }

            if($scope.internalView.pns) {
                angular.copy(selectedObj, $scope.newPNSObj);
            }
        };

        $scope.close = function(){
            $scope.internalView.pns = false;
            $scope.internalView.edit = "view";
        };
        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.internalView.pns = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedPNS, $scope.newPNSObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_EPG_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('l2FloodCtrl', ['$scope', 'GBPL2FloodDomainServices', 'GBPL2BridgeDomainServices', '$filter', function($scope, GBPL2FloodDomainServices, GBPL2BridgeDomainServices, $filter){
        $scope.list = [];
        $scope.selectedL2Flood = null;
        $scope.newL2FloodObj = GBPL2FloodDomainServices.createObj();
        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'L2 Flood Domain list';

        $scope.l2bridge = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L2BRIDGE_LABEL', $scope.l2bridge);

        $scope.view = {
            l2flood: false,
            edit: "view"
        };

        var path = null,

            loadL2BridgeList = function() {
                GBPL2BridgeDomainServices.load(GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id), function(data){
                    $scope.l2bridge.options = data;
                }, function(){

                });
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedL2Flood = null;
                $scope.newL2FloodObj = GBPL2FloodDomainServices.createObj();
                $scope.view = {
                    l2flood: false,
                    edit: "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id);

                GBPL2FloodDomainServices.load(path, function(data){
                    $scope.list = data;
                    // clear objects
                    $scope.newL2FloodObj = GBPL2FloodDomainServices.createObj();
                    $scope.selectedL2Flood = null;
                }, function(){

                });

                loadL2BridgeList();
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.l2FloodForm)){
                path = GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id, $scope.newL2FloodObj.id);
                GBPL2FloodDomainServices.send(path, $scope.newL2FloodObj, function(data){
                    $scope.init();
                    $scope.view.l2flood = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L2FLOOD_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedL2Flood) {
                path = GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id, $scope.selectedL2Flood.id);
                GBPL2FloodDomainServices.delete(path, function(data){
                    $scope.init();
                    $scope.view.l2flood = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L2FLOOD_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedL2Flood = selectedObj;

            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.l2flood = true;
            }
            else {
                $scope.view.l2flood = false;
                $scope.view.edit = "view";
            }

            if ($scope.view.l2flood) {
                angular.copy(selectedObj, $scope.newL2FloodObj);
            }

            $scope.sendReloadEventFromRoot('GBP_L2FLOOD_RELOAD');
        };

        $scope.showForm = function() {
            $scope.newL2FloodObj = GBPL2FloodDomainServices.createObj();
            $scope.selectedL2Flood = null;
            $scope.view.l2flood = true;
            $scope.view.edit = "add";
        };

        $scope.close = function(){
            $scope.view.l2flood = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedL2Flood ) {
                    $scope.view.l2flood = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedL2Flood, $scope.newL2FloodObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_L2BRIDGE_RELOAD',function(){
            loadL2BridgeList();
        });

        $scope.$on('GBP_L2FLOOD_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('l2BridgeCtrl', ['$scope', 'GBPL2BridgeDomainServices', 'GBPL3ContextServices', '$filter', function($scope, GBPL2BridgeDomainServices, GBPL3ContextServices, $filter){
        $scope.list = [];
        $scope.selectedL2Bridge = null;
        $scope.newL2BridgeObj = GBPL2BridgeDomainServices.createObj();
        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'L2 Bridge Domain list';

        $scope.l3context = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L3CONTEXT_LABEL', $scope.l3context);

        $scope.view = {
            l2bridge: false,
            edit: "view"
        };

        var path = null,

            loadL3ContextList = function() {
                GBPL3ContextServices.load(GBPL3ContextServices.createPathObj($scope.selectedTenant.id), function(data){
                    $scope.l3context.options = data;
                    //$scope.$broadcast('GBP_L2BRIDGE_RELOAD');
                }, function(){

                });
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedL2Bridge = null;
                $scope.newL2BridgeObj = GBPL2BridgeDomainServices.createObj();
                $scope.view = {
                    l2bridge: false,
                    edit: "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id);

                GBPL2BridgeDomainServices.load(path, function(data){
                    $scope.list = data;
                    $scope.newL2BridgeObj = GBPL2BridgeDomainServices.createObj();
                    $scope.selectedL2Bridge = null;
                    // $scope.$broadcast('GBP_L2BRIDGE_RELOAD');
                }, function(){

                });

                loadL3ContextList();
            }else{
                clear();
            }
        };



        $scope.save = function(){
            if($scope.validateForm($scope.l2BridgeForm)){
                path = GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id, $scope.newL2BridgeObj.id);
                GBPL2BridgeDomainServices.send(path, $scope.newL2BridgeObj, function(data){
                    $scope.init();
                    $scope.view.l2bridge = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L2BRIDGE_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedL2Bridge) {
                path = GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id, $scope.selectedL2Bridge.id);
                GBPL2BridgeDomainServices.delete(path, function(data){
                    $scope.init();
                    $scope.view.l2bridge = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L2BRIDGE_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedL2Bridge = selectedObj;

            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.l2bridge = true;
            }
            else {
                $scope.view.l2bridge = false;
                $scope.view.edit = "view";
            }

            if ($scope.view.l2bridge) {
                angular.copy(selectedObj, $scope.newL2BridgeObj);
            }

            $scope.sendReloadEventFromRoot('GBP_L2BRIDGE_RELOAD');
        };

        $scope.showForm = function() {
            $scope.newL2BridgeObj = GBPL2BridgeDomainServices.createObj();
            $scope.selectedL2Bridge = null;
            $scope.view.l2bridge = true;
            $scope.view.edit = "add";
        };

        $scope.close = function(){
            $scope.view.l2bridge = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedL2Bridge ) {
                    $scope.view.l2bridge = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedL2Bridge, $scope.newL2BridgeObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_L3CONTEXT_RELOAD',function(){
            loadL3ContextList();
        });

        $scope.$on('GBP_L2BRIDGE_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('l3ContextCtrl', ['$scope', 'GBPL3ContextServices', '$filter', function($scope, GBPL3ContextServices, $filter){ //GBPContractServices
        $scope.list = [];
        $scope.selectedL3Context = null;
        $scope.newL3ContextObj = GBPL3ContextServices.createObj();
        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'L3 Context list';

        $scope.view = {
            l3context: false,
            edit: "view"
        };

        var path = null,

            clear = function(){
                $scope.list = [];
                $scope.view = {
                    l3context: false,
                    edit: "view"
                };
                $scope.selectedL3Context = null;
                $scope.newL3ContextObj = GBPL3ContextServices.createObj();
            };

        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPL3ContextServices.createPathObj($scope.selectedTenant.id);

                GBPL3ContextServices.load(path, function(data){
                    $scope.list = data;
                    $scope.newL3ContextObj = GBPL3ContextServices.createObj();
                    $scope.selectedL3Context = null;
                }, function(){

                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
             if($scope.validateForm($scope.l3ContextForm)){
                path = GBPL3ContextServices.createPathObj($scope.selectedTenant.id, $scope.newL3ContextObj.id);
                GBPL3ContextServices.send(path, $scope.newL3ContextObj, function(data){
                    $scope.init();
                    $scope.view.l3context = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L3CONTEXT_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedL3Context) {
                path = GBPL3ContextServices.createPathObj($scope.selectedTenant.id, $scope.selectedL3Context.id);
                GBPL3ContextServices.delete(path, function(data){
                    $scope.init();
                    $scope.view.l3context = false;
                    $scope.view.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_L3CONTEXT_RELOAD');

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedL3Context = selectedObj;

            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.l3context = true;
            }
            else {
                $scope.view.l3context = false;
                $scope.view.edit = "view";
            }

            if($scope.view.l3context) {
                angular.copy(selectedObj, $scope.newL3ContextObj);
            }

            $scope.sendReloadEventFromRoot('GBP_L3CONTEXT_RELOAD');
        };

        $scope.showForm = function() {
            $scope.newL3ContextObj = GBPL3ContextServices.createObj();
            $scope.selectedL3Context = null;
            $scope.view.l3context = true;
            $scope.view.edit = "add";
        };

        $scope.close = function(){
            $scope.view.l3context = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedL3Context ) {
                    $scope.view.l3context = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedL3Context, $scope.newL3ContextObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_L3CONTEXT_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('subnetCtrl', ['$scope', 'GBPSubnetServices', 'GBPL2FloodDomainServices', 'GBPL2BridgeDomainServices', 'GBPL3ContextServices', '$filter', function($scope, GBPSubnetServices, GBPL2FloodDomainServices, GBPL2BridgeDomainServices, GBPL3ContextServices, $filter){
        $scope.list = [];
        $scope.selectedSubnet = null;
        $scope.newSubnetObj = GBPSubnetServices.createObj();
        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'Subnet list';

        $scope.l2L3List = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L2FLOOD_LABEL', $scope.l2L3List);

        $scope.view = {
            subnet: false,
            edit: "view"
        };


        var path = null,

            loadL2L3List = function() {
                $scope.l2L3List.options = [];

                GBPL3ContextServices.load(GBPL3ContextServices.createPathObj($scope.selectedTenant.id), function(l3ContextData){
                    $scope.l2L3List.options = $scope.l2L3List.options.concat(l3ContextData);
                }, function(){

                });

                GBPL2FloodDomainServices.load(GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id), function(l2FloodData){
                    $scope.l2L3List.options = $scope.l2L3List.options.concat(l2FloodData);
                }, function(){

                });

                GBPL2BridgeDomainServices.load(GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id), function(l2BridgeData){
                    $scope.l2L3List.options = $scope.l2L3List.options.concat(l2BridgeData);
                }, function(){

                });
            },

            clear = function(){
                $scope.list = [];
                $scope.view = {
                    subnet: false,
                    edit: "view"
                };
                $scope.selectedSubnet = null;
                $scope.newSubnetObj = GBPSubnetServices.createObj();
            };

        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPSubnetServices.createPathObj($scope.selectedTenant.id);

                GBPSubnetServices.load(path, function(data){
                    $scope.list = data;
                    $scope.newSubnetObj = GBPSubnetServices.createObj();
                    $scope.selectedSubnet = null;

                    $scope.view.subnet = false;
                    $scope.view.edit = "view";
                    //$scope.sendReloadEventFromRoot('GBP_L2BRIDGE_RELOAD');
                }, function(){

                });

                loadL2L3List();
            }else{
                clear();
            }
        };

        $scope.save = function(){
           if($scope.validateForm($scope.subnetForm)){
                path = GBPSubnetServices.createPathObj($scope.selectedTenant.id, $scope.newSubnetObj.id);
                GBPSubnetServices.send(path, $scope.newSubnetObj, function(data){
                    $scope.init();
                    $scope.view.subnet = false;
                    $scope.view.edit = "view";

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedSubnet) {
                path = GBPSubnetServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id);
                GBPSubnetServices.delete(path, function(data){
                    $scope.init();
                    $scope.view.subnet = false;
                    $scope.view.edit = "view";

                    $scope.reloadTopo();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedSubnet = selectedObj;

            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.subnet = true;
            }
            else {
                $scope.view.subnet = false;
                $scope.view.edit = "view";
            }

            if($scope.view.subnet) {
                angular.copy(selectedObj, $scope.newSubnetObj);
            }

            $scope.sendReloadEventFromRoot('GBP_SUBNET_RELOAD');
        };

        $scope.showForm = function() {
            $scope.newSubnetObj = GBPSubnetServices.createObj();
            $scope.selectedSubnet = null;
            $scope.view.subnet = true;
            $scope.view.edit = "add";
        };

        $scope.close = function(){
            $scope.view.subnet = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedSubnet ) {
                    $scope.view.subnet = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedSubnet, $scope.newSubnetObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_GATEWAY_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_L3CONTEXT_RELOAD',function(){
            loadL2L3List();
        });

        $scope.$on('GBP_L2BRIDGE_RELOAD',function(){
            loadL2L3List();
        });

        $scope.$on('GBP_L2FLOOD_RELOAD',function(){
            loadL2L3List();
        });

        $scope.$on('GBP_PREFIX_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('gatewayCtrl', ['$scope', 'GBPGatewayServices', function($scope, GBPGatewayServices){
        $scope.list = [];
        $scope.gatewayView = false;
        $scope.selectedGateway = null;
        $scope.newGatewayObj = GBPGatewayServices.createObj();
        $scope.displayLabel = 'gateway';
        $scope.crudLabel = 'Gateway list';
        $scope.internalView = {
            gateway: false,
            edit: "view"
        };

        var path = null,

            clear = function(){
                $scope.list = [];
                $scope.gatewayView = false;
                $scope.selectedGateway = null;
                $scope.newGatewayObj = GBPGatewayServices.createObj();
            };

        $scope.init = function() {
            if ( $scope.selectedTenant && $scope.selectedSubnet ) {
                path = GBPGatewayServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id);

                GBPGatewayServices.load(path, function(data){
                    $scope.list = data;
                    $scope.newGatewayObj = GBPGatewayServices.createObj();
                    $scope.internalView.gateway = false;
                    $scope.selectedGateway = null;
                }, function(){

                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.gatewayForm)){
                path = GBPGatewayServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id, $scope.newGatewayObj.gateway);
                GBPGatewayServices.send(path, $scope.newGatewayObj, function(data){
                    $scope.init();
                    $scope.internalView.gateway = false;
                    $scope.internalView.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_GATEWAY_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedSubnet && $scope.selectedGateway) {
                path = GBPGatewayServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id, $scope.selectedGateway.gateway);
                GBPGatewayServices.delete(path, function(data){
                    $scope.init();
                    $scope.internalView.gateway = false;
                    $scope.sendReloadEventFromRoot('GBP_GATEWAY_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedGateway = selectedObj;
            angular.copy(selectedObj, $scope.newGatewayObj);
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.gateway = true;
            }else {
                $scope.internalView.gateway = false;
                $scope.internalView.edit = "view";
            }

            $scope.sendReloadEventFromRoot('GBP_GATEWAY_SET');
        };

        $scope.showForm = function() {
            $scope.newGatewayObj = GBPGatewayServices.createObj();
            $scope.internalView.gateway = true;
            $scope.internalView.edit = "add";
            $scope.selectedGateway = null;
        };

        $scope.close = function(){
            $scope.internalView.gateway = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedGateway ) {
                    $scope.internalView.gateway = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedGateway, $scope.newGatewayObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_SUBNET_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_PREFIX_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('prefixCtrl', ['$scope', 'GBPPrefixServices', function($scope, GBPPrefixServices){
        $scope.list = [];
        $scope.selectedPrefix = null;
        $scope.newPrefixObj = GBPPrefixServices.createObj();
        $scope.displayLabel = 'prefix';
        $scope.crudLabel = 'Prefix list';
        $scope.internalView = {
            prefix: false,
            edit: "view"
        };

        var path = null,

            clear = function(){
                $scope.list = [];
                $scope.selectedPrefix = null;
                $scope.newPrefixObj = GBPPrefixServices.createObj();
                $scope.internalView = {
                    prefix: false,
                    edit: "view"
                };
            };

        $scope.init = function() {
            if ( $scope.selectedTenant && $scope.selectedSubnet && $scope.selectedGateway) {
                path = GBPPrefixServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id, $scope.selectedGateway.gateway);

                GBPPrefixServices.load(path, function(data){
                    $scope.list = data;
                    $scope.newPrefixObj = GBPPrefixServices.createObj();
                    $scope.internalView.prefix = false;
                    $scope.selectedPrefix = null;
                }, function(){

                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.prefixForm)){
                path = GBPPrefixServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id, $scope.selectedGateway.gateway, $scope.newPrefixObj.prefix);
                GBPPrefixServices.send(path, $scope.newPrefixObj, function(data){
                    $scope.init();
                    $scope.internalView.prefix = false;
                    $scope.internalView.edit = "view";
                    $scope.sendReloadEventFromRoot('GBP_PREFIX_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            path = GBPPrefixServices.createPathObj($scope.selectedTenant.id, $scope.selectedSubnet.id, $scope.selectedGateway.gateway, $scope.selectedPrefix.prefix);
            GBPPrefixServices.delete(path, function(data){
                $scope.init();
                $scope.internalView.prefix = false;
                $scope.sendReloadEventFromRoot('GBP_PREFIX_RELOAD');
            }, function(){
                //TODO: error cbk
            });
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedPrefix = selectedObj;
            $scope.internalView.edit = $scope.internalView.edit == "edit" ? $scope.internalView.edit : "view";
            if(selectedObj){
                $scope.internalView.prefix = true;
            }else {
                $scope.internalView.prefix = false;
                $scope.internalView.edit = "view";
            }

            angular.copy(selectedObj, $scope.newPrefixObj);
        };

        $scope.showForm = function() {
            $scope.newPrefixObj = GBPPrefixServices.createObj();
            $scope.internalView.prefix = true;
            $scope.internalView.edit = "add";
            $scope.selectedPrefix = null;
        };

        $scope.close = function(){
            $scope.internalView.prefix = false;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedPrefix ) {
                    $scope.internalView.prefix = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedPrefix, $scope.newPrefixObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_GATEWAY_SET',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('renderersCtrl', ['$scope', 'GPBServices', function($scope, GPBServices){ //GBPContractServices
        $scope.classifierDefinitions = {'options' : [], 'labels' : null};
        $scope.actionDefinitions = {'options' : [], 'labels' : null};

        //reload on event?

        var loadDefinitions = function() {
            GPBServices.getDefinitions(function(classifierDefs, actionDefs) {
                $scope.classifierDefinitions.options = classifierDefs;
                $scope.getDisplayLabelsFromCtrl('GBP_CLASSIFIERS_LABEL', $scope.classifierDefinitions);

                $scope.actionDefinitions.options = actionDefs;
                $scope.getDisplayLabelsFromCtrl('GBP_ACTIONS_LABEL', $scope.actionDefinitions);
            });
        };

        loadDefinitions();
    }]);

    gbpOld.register.controller('paramCtrl', ['$scope', 'GPBServices', function($scope, GPBServices){
        $scope.value = null;

        $scope.init = function(param, paramValues) {
            $scope.parameter = param;

            if(paramValues) {
                paramValues.forEach(function(p) {
                    if($scope.parameter.name === p.name) {
                        $scope.value = GPBServices.getInstanceParamValue(p);
                    }
                });
            }
        };

        $scope.$on('GBP_SAVE_PARAM', function(event){
            if($scope.value !== '' && $scope.value !== null) {
                $scope.addParam($scope.parameter.name, $scope.parameter.type, $scope.value);
            }
        });

        $scope.$on('GBP_SET_PARAM_VALUE', function(event, name, intVal, strVal) {
            //console.info($scope.parameter, ' got GBP_SET_PARAM_VALUE', name, intVal, strVal, event);

        });

        $scope.$on('GBP_RESET_PARAM', function(event){
            $scope.value = null;
        });
    }]);

    gbpOld.register.controller('classifiersCtrl', ['$scope', 'GBPClassifierInstanceServices', 'GPBServices', '$filter',
        function($scope, GBPClassifierInstanceServices, GPBServices, $filter){
        $scope.list = [];
        $scope.classifiersView = false;
        $scope.displayLabel = 'name';
        $scope.selectedClassifier = null;
        $scope.crudLabel = 'Classifiers';
        $scope.newClassifierObj = GBPClassifierInstanceServices.createObj();
        $scope.edit = "view";

        var mandatoryProperties = ['name'],

            clear = function(){
                $scope.list = [];
                $scope.classifiersView = false;
                $scope.selectedClassifier = null;
                $scope.newClassifierObj = GBPClassifierInstanceServices.createObj();
                $scope.edit = "view";
            };

        $scope.getDefinitionObjParams = function(id){
            return GPBServices.getDefinitionObjParams($scope.classifierDefinitions.options, id);
        };

        $scope.reloadDefs = function(){
            $scope.defs = angular.copy($scope.getDefinitionObjParams($scope.newClassifierObj['classifier-definition-id']));
        };

        $scope.addParam = function(name, type, value) {
            $scope.newClassifierObj['parameter-value'].push(GPBServices.createParamObj(name, type, value));
        };

        var saveParams = function() {
            $scope.newClassifierObj['parameter-value'] = [];
            $scope.$broadcast('GBP_SAVE_PARAM');
        };


        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPClassifierInstanceServices.createPathObj($scope.selectedTenant.id);
                GBPClassifierInstanceServices.load(path, function(data){
                    $scope.list = data;
                    $scope.reloadDefs();
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.classifierForm)){
                path = GBPClassifierInstanceServices.createPathObj($scope.selectedTenant.id, $scope.newClassifierObj.name);
                saveParams();
                GBPClassifierInstanceServices.send(path, $scope.newClassifierObj, function(data){
                    $scope.init();
                    $scope.classifiersView = false;
                    $scope.edit = "view";
                $scope.sendReloadEventFromRoot('GBP_CLASSIFIER_INSTANCE_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.newClassifierObj = GBPClassifierInstanceServices.createObj();
            $scope.selectedClassifier = null;
            $scope.classifiersView = true;
            $scope.edit = "add";
            $scope.reloadDefs();
        };

        $scope.reload = function(selectedObj){
            $scope.selectedClassifier = selectedObj;
            $scope.sendReloadEventFromRoot('GBP_CLASSIFIER_INSTANCE_RELOAD');

            $scope.edit = $scope.edit == "edit" ? $scope.edit : "view";
            if(selectedObj){
                $scope.classifiersView = true;
            }
            else {
                $scope.classifiersView = false;
                $scope.edit = "view";
            }

            if($scope.classifiersView) {
                angular.copy(selectedObj, $scope.newClassifierObj);
            }

            $scope.reloadDefs();
        };

        $scope.close = function(){
            $scope.classifiersView = false;
            $scope.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedClassifier ) {
                    $scope.classifiersView = true;
                    angular.copy($scope.selectedClassifier, $scope.newClassifierObj);
                    $scope.edit = "edit";
                    $scope.reloadDefs();
                }
                event.defaultPrevented = true;
            }
        });

        $scope.delete = function(){
            path = GBPClassifierInstanceServices.createPathObj($scope.selectedTenant.id, $scope.selectedClassifier.name);
            GBPClassifierInstanceServices.delete(path, function(data){
                $scope.init();
                $scope.classifiersView = false;
                $scope.sendReloadEventFromRoot('GBP_CLASSIFIER_INSTANCE_RELOAD');
            }, function(){
                //TODO: error cbk
            });
        };

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_CLASSIFIERS_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('actionsCtrl', ['$scope', 'GBPActionInstanceServices', 'GPBServices', '$filter',
        function($scope, GBPActionInstanceServices, GPBServices, $filter){
        $scope.list = [];
        $scope.actionsView = false;
        $scope.displayLabel = 'name';
        $scope.selectedAction = null;
        $scope.crudLabel = 'Actions';
        $scope.newActionObj = GBPActionInstanceServices.createObj();
        $scope.edit = "view";

        var mandatoryProperties = ['name'],

            clear = function(){
                $scope.list = [];
                $scope.actionsView = false;
                $scope.selectedAction = null;
                $scope.newActionObj = GBPActionInstanceServices.createObj();
                $scope.edit = "view";
            };

        $scope.getDefinitionObjParams = function(id){
            return GPBServices.getDefinitionObjParams($scope.actionDefinitions.options, id);
        };

        $scope.reloadDefs = function(){
            $scope.defs = angular.copy($scope.getDefinitionObjParams($scope.newActionObj['action-definition-id']));
        };

        $scope.addParam = function(name, type, value) {
            $scope.newActionObj['parameter-value'].push(GPBServices.createParamObj(name, type, value));
        };

        var saveParams = function() {
            $scope.newActionObj['parameter-value'] = [];
            $scope.$broadcast('GBP_SAVE_PARAM');
        };


        $scope.init = function() {
            if ( $scope.selectedTenant ) {
                path = GBPActionInstanceServices.createPathObj($scope.selectedTenant.id);
                GBPActionInstanceServices.load(path, function(data){
                    $scope.list = data;
                    $scope.reloadDefs();
                }, function(){
                    //TODO: error cbk
                });
            }else{
                clear();
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.actionsForm)){
                path = GBPActionInstanceServices.createPathObj($scope.selectedTenant.id, $scope.newActionObj.name);
                saveParams();

                GBPActionInstanceServices.send(path, $scope.newActionObj, function(data){
                    $scope.init();
                    $scope.actionsView = false;
                    $scope.edit = "view";
                $scope.sendReloadEventFromRoot('GBP_ACTION_INSTANCE_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.newActionObj = GBPActionInstanceServices.createObj();
            $scope.selectedAction = null;
            $scope.actionsView = true;
            $scope.edit = "add";
            $scope.reloadDefs();
        };

        $scope.reload = function(selectedObj){
            $scope.selectedAction = selectedObj;
            $scope.sendReloadEventFromRoot('GBP_ACTION_INSTANCE_RELOAD');

            $scope.edit = $scope.edit == "edit" ? $scope.edit : "view";
            if(selectedObj){
                $scope.actionsView = true;
            }
            else {
                $scope.actionsView = false;
                $scope.edit = "view";
            }

            if($scope.actionsView) {
                angular.copy(selectedObj, $scope.newActionObj);
            }

            $scope.reloadDefs();
        };

        $scope.close = function(){
            $scope.actionsView = false;
            $scope.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedAction ) {
                    $scope.actionsView = true;
                    angular.copy($scope.selectedAction, $scope.newActionObj);
                    $scope.edit = "edit";
                    $scope.reloadDefs();
                }
                event.defaultPrevented = true;
            }
        });

        $scope.delete = function(){
            path = GBPActionInstanceServices.createPathObj($scope.selectedTenant.id, $scope.selectedAction.name);
            GBPActionInstanceServices.delete(path, function(data){
                $scope.init();
                $scope.actionsView = false;
                $scope.sendReloadEventFromRoot('GBP_ACTION_INSTANCE_RELOAD');
            }, function(){
                //TODO: error cbk
            });
        };

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_ACTIONS_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });
    }]);

    gbpOld.register.controller('endpointCtrl', ['$scope', 'GBPEndpointServices', 'GPBServices', 'GBPL2FloodDomainServices', 'GBPL2BridgeDomainServices', 'GBPL3ContextServices', 'GBPEpgServices', '$filter', 'GBPSubnetServices',
        function($scope, GBPEndpointServices, GPBServices, GBPL2FloodDomainServices, GBPL2BridgeDomainServices, GBPL3ContextServices, GBPEpgServices, $filter, GBPSubnetServices){
        $scope.list = [];
        $scope.selectedEndpoint = null;
        $scope.newEndpointObj = GBPEndpointServices.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
        $scope.displayLabel = function(obj) {
            return obj['mac-address'] + ':' + obj['l2-context'];
        };
        $scope.crudLabel = 'Endpoint list';

        $scope.l2context = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L2FLOOD_LABEL', $scope.l2context);

        $scope.l3context = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L3CONTEXT_LABEL', $scope.l3context);

        $scope.subnet = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_SUBNET_LABEL', $scope.subnet);

        $scope.epg = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_EPG_LABEL', $scope.epg);

        $scope.networkContainment = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L2FLOOD_LABEL', $scope.networkContainment);

        $scope.view = {
            endpoint: false,
            edit: "view"
        };

        var path = null,
            mandatoryProperties = [],

            loadEpgOptions = function() {
                $scope.epg.options = [];

                path = GBPEpgServices.createPathObj($scope.selectedTenant.id);
                GBPEpgServices.load(path, function(data){
                    $scope.epg.options = data;
                }, function(){
                    //TODO: error cbk
                });
            },

            loadL2ContextOptions = function() {
                removeL2ContextOptions($scope.networkContainment.options, $scope.l2context.options);
                $scope.l2context.options = [];

                path = GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id);

                GBPL2FloodDomainServices.load(path, function(data){
                    $scope.l2context.options = $scope.l2context.options.concat(data);
                    $scope.networkContainment.options = $scope.networkContainment.options.concat(data);
                }, function(){

                });

                path = GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id);
                GBPL2BridgeDomainServices.load(path, function(data){
                    $scope.l2context.options = $scope.l2context.options.concat(data);
                    $scope.networkContainment.options = $scope.networkContainment.options.concat(data);
                }, function(){

                });
            },

            loadL3ContextOptions = function(){
                removeL2ContextOptions($scope.networkContainment.options, $scope.l3context.options);
                $scope.l3context.options = [];

                GBPL3ContextServices.load(GBPL3ContextServices.createPathObj($scope.selectedTenant.id), function(data){
                    $scope.l3context.options = data;
                    $scope.networkContainment.options = $scope.networkContainment.options.concat(data);
                }, function(){

                });
            },

            loadSubnetOptions = function(){
                $scope.subnet.options = [];

                GBPSubnetServices.load(GBPSubnetServices.createPathObj($scope.selectedTenant.id), function(data){
                    $scope.subnet.options = data;
                    $scope.networkContainment.options = $scope.networkContainment.options.concat(data);
                }, function(){

                });
            },

            loadNetworkCotnaninemnt = function(){
                $scope.networkContainment.options = [];

                loadL2ContextOptions();
                loadL3ContextOptions();
                loadSubnetOptions();
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedEndpoint = null;
                $scope.newEndpointObj = GBPEndpointServices.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
                $scope.view = {
                    endpoint: false,
                    edit: "view"
                };
            },
            removeL2ContextOptions = function(arr1, arr2) {
                arr1 = arr1.filter( function( el ) {
                  return arr2.indexOf( el ) < 0;
                });
            };

        $scope.init = function() {
            if ($scope.selectedTenant) {

                GBPEndpointServices.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });

                loadEpgOptions();
                /*loadL2ContextOptions();
                loadL3ContextOptions();
                loadSubnetOptions();*/
                loadNetworkCotnaninemnt();
            }else{
                clear();
            }
        };

        $scope.addNewL3address = function() {
            if($scope.newEndpointObj) {
                if(!$scope.newEndpointObj['l3-address']){
                    $scope.newEndpointObj['l3-address'] = [];
                }
                var objToPush = {'l3-context' : '', 'ip-address' : ''};
                $scope.newEndpointObj['l3-address'].push(objToPush);
            }
        };

        $scope.deleteNewL3address = function(index){
            if($scope.newEndpointObj) {
                $scope.newEndpointObj['l3-address'].splice(index, 1);
            }
        };

        $scope.addNewLeafListEl = function(prop) {
            if($scope.newEndpointObj) {
                if(!$scope.newEndpointObj[prop]){
                    $scope.newEndpointObj[prop] = [];
                }
                var objToPush = "";
                $scope.newEndpointObj[prop].push(objToPush);
            }
        };

        $scope.updateLeafListEl = function(index, value, prop) {
            if($scope.newEndpointObj && $scope.newEndpointObj[prop] && $scope.newEndpointObj[prop].length >= index) {
                $scope.newEndpointObj[prop][index] = value;
            }
        };

        $scope.deleteNewLeafListEl = function(index, prop){
            if($scope.newEndpointObj) {
                $scope.newEndpointObj[prop].splice(index, 1);
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.endpointForm)){
                GBPEndpointServices.send(path, $scope.newEndpointObj, function(data){
                    $scope.init();
                    $scope.view.endpoint = false;
                    $scope.reloadNewObj();
                    $scope.view.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedEndpoint) {
                GBPEndpointServices.delete(path, $scope.selectedEndpoint, function(data){
                    $scope.init();
                    $scope.view.endpoint = false;
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.view.endpoint = true;
            $scope.reloadNewObj();
            $scope.selectedEndpoint = null;

            $scope.view.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newEndpointObj = GBPEndpointServices.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedEndpoint = selectedObj;
            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.endpoint = true;
            }
            else {
                $scope.view.endpoint = false;
                $scope.view.edit = "view";
            }

            if($scope.view.endpoint) {
                angular.copy(selectedObj, $scope.newEndpointObj);
            }
        };

        $scope.close = function(){
            $scope.view.endpoint = false;
            $scope.view.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEndpoint ) {
                    $scope.view.endpoint = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedEndpoint, $scope.newEndpointObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_EPG_RELOAD',function(){
            loadEpgOptions();
        });

        $scope.$on('GBP_L2BRIDGE_RELOAD',function(){
            //loadL2ContextOptions();
            loadNetworkCotnaninemnt();
        });

        $scope.$on('GBP_L2FLOOD_RELOAD',function(){
            //loadL2ContextOptions();
            loadNetworkCotnaninemnt();
        });

        $scope.$on('GBP_L3CONTEXT_RELOAD',function(){
            //loadL3ContextOptions();
            loadNetworkCotnaninemnt();
        });

        $scope.$on('GBP_SUBNET_RELOAD',function(){
            //loadSubnetOptions();
            loadNetworkCotnaninemnt();
        });
    }]);

    gbpOld.register.controller('l3EndpointCtrl', ['$scope', 'GBPEndpointL3Services', 'GPBServices', 'GBPEpgServices', 'GBPL3ContextServices', 'GBPL2FloodDomainServices', 'GBPL2BridgeDomainServices', '$filter',
        function($scope, GBPEndpointL3Services, GPBServices, GBPEpgServices, GBPL3ContextServices, GBPL2FloodDomainServices, GBPL2BridgeDomainServices, $filter){
        $scope.list = [];
        $scope.selectedEndpoint = null;
        $scope.newEndpointObj = GBPEndpointL3Services.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
        $scope.displayLabel = function(obj) {
            return obj['ip-prefix'] + ':' + obj['l3-context'];
        };
        $scope.crudLabel = 'L3 Prefix endpoint list';

        $scope.l2context = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L2FLOOD_LABEL', $scope.l2context);

        $scope.l3context = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_L3CONTEXT_LABEL', $scope.l3context);

        $scope.epg = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_EPG_LABEL', $scope.epg);

        $scope.view = {
            endpoint: false,
            edit: "view"
        };

        var path = null,
            mandatoryProperties = [],

            loadEpgOptions = function() {
                $scope.epg.options = [];

                path = GBPEpgServices.createPathObj($scope.selectedTenant.id);
                GBPEpgServices.load(path, function(data){
                    $scope.epg.options = data;
                }, function(){
                    //TODO: error cbk
                });
            },

            loadL2ContextOptions = function() {
                $scope.l2context.options = [];

                path = GBPL2FloodDomainServices.createPathObj($scope.selectedTenant.id);

                GBPL2FloodDomainServices.load(path, function(data){
                    $scope.l2context.options = $scope.l2context.options.concat(data);
                }, function(){

                });

                path = GBPL2BridgeDomainServices.createPathObj($scope.selectedTenant.id);
                GBPL2BridgeDomainServices.load(path, function(data){
                    $scope.l2context.options = $scope.l2context.options.concat(data);
                }, function(){

                });
            },

            loadL3ContextOptions = function(){
                $scope.l3context.options = [];

                GBPL3ContextServices.load(GBPL3ContextServices.createPathObj($scope.selectedTenant.id), function(data){
                    $scope.l3context.options = data;
                }, function(){

                });
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedEndpoint = null;
                $scope.newEndpointObj = GBPEndpointL3Services.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
                $scope.view = {
                    endpoint: false,
                    edit: "view"
                };
            };

        $scope.init = function() {
            if ($scope.selectedTenant) {

                GBPEndpointL3Services.load(path, function(data){
                    $scope.list = data;
                }, function(){
                    //TODO: error cbk
                });

                loadEpgOptions();
                loadL2ContextOptions();
                loadL3ContextOptions();
            }else{
                clear();
            }
        };

        $scope.addNewL2gateways = function() {
            if($scope.newEndpointObj) {
                if(!$scope.newEndpointObj['endpoint-l2-gateways']){
                    $scope.newEndpointObj['endpoint-l2-gateways'] = [];
                }
                var objToPush = {'l2-context' : '', 'mac-address' : ''};
                $scope.newEndpointObj['endpoint-l2-gateways'].push(objToPush);
            }
        };

        $scope.deleteNewL2gateways = function(index){
            if($scope.newEndpointObj) {
                $scope.newEndpointObj['endpoint-l2-gateways'].splice(index, 1);
            }
        };

        $scope.addNewL3gateways = function() {
            if($scope.newEndpointObj) {
                if(!$scope.newEndpointObj['endpoint-l3-gateways']){
                    $scope.newEndpointObj['endpoint-l3-gateways'] = [];
                }
                var objToPush = {'l3-context' : '', 'ip-address' : ''};
                $scope.newEndpointObj['endpoint-l3-gateways'].push(objToPush);
            }
        };

        $scope.deleteNewL3gateways = function(index){
            if($scope.newEndpointObj) {
                $scope.newEndpointObj['endpoint-l3-gateways'].splice(index, 1);
            }
        };

        $scope.addNewLeafListEl = function(prop) {
            if($scope.newEndpointObj) {
                if(!$scope.newEndpointObj[prop]){
                    $scope.newEndpointObj[prop] = [];
                }
                var objToPush = "";
                $scope.newEndpointObj[prop].push(objToPush);
            }
        };

        $scope.updateLeafListEl = function(index, value, prop) {
            if($scope.newEndpointObj && $scope.newEndpointObj[prop] && $scope.newEndpointObj[prop].length >= index) {
                $scope.newEndpointObj[prop][index] = value;
            }
        };

        $scope.deleteNewLeafListEl = function(index, prop){
            if($scope.newEndpointObj) {
                $scope.newEndpointObj[prop].splice(index, 1);
            }
        };

        $scope.save = function(){
            if($scope.validateForm($scope.l3EndpointForm)){
                GBPEndpointL3Services.send(path, $scope.newEndpointObj, function(data){
                    $scope.init();
                    $scope.view.endpoint = false;
                    $scope.reloadNewObj();
                    $scope.view.edit = "view";
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.delete = function() {
            if($scope.selectedTenant && $scope.selectedEndpoint) {
                GBPEndpointL3Services.delete(path, $scope.selectedEndpoint, function(data){
                    $scope.init();
                }, function(){
                    //TODO: error cbk
                });
            }
        };

        $scope.showForm = function() {
            $scope.view.endpoint = true;
            $scope.reloadNewObj();
            $scope.selectedEndpoint = null;
            $scope.view.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newEndpointObj = GBPEndpointL3Services.createObj($scope.selectedTenant ? $scope.selectedTenant.id : null);
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedEndpoint = selectedObj;
            angular.copy(selectedObj, $scope.newEndpointObj);
            $scope.view.edit = $scope.view.edit == "edit" ? $scope.view.edit : "view";
            if(selectedObj){
                $scope.view.endpoint = true;
            }
            else {
                $scope.view.endpoint = false;
                $scope.view.edit = "view";
            }
        };

        $scope.close = function(){
            $scope.view.endpoint = false;
            $scope.view.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEndpoint ) {
                    $scope.view.endpoint = true;
                    $scope.view.edit = "edit";
                    angular.copy($scope.selectedEndpoint, $scope.newEndpointObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_EPG_RELOAD',function(){
            loadEpgOptions();
        });

        $scope.$on('GBP_L2BRIDGE_RELOAD',function(){
            loadL2ContextOptions();
        });

        $scope.$on('GBP_L2FLOOD_RELOAD',function(){
            loadL2ContextOptions();
        });

        $scope.$on('GBP_L3CONTEXT_RELOAD',function(){
            loadL3ContextOptions();
        });
    }]);

    gbpOld.register.controller('accessModelWizardCtrl', ['$scope', '$filter', 'GBPTenantServices', 'GBPEpgServices', 'GBPContractServices', 'GPBServices', function($scope, $filter, GBPTenantServices, GBPEpgServices, GBPContractServices, GPBServices){
        $scope.wizardPage = null;

        $scope.selectedTenant = null;
        $scope.tenant = null;
        //$scope.epgList = [];
        $scope.newContractObj = null;

        $scope.init = function() {
            $scope.setPage('tenants');
        };

        $scope.setPage = function(pageName, object) {
            $scope.wizardPage = pageName;

            switch(pageName) {
                case 'contracts':
                    if(object) {
                        $scope.tenant = object;
                    }

                    break;

                case 'summary':
                    $scope.tenant['endpoint-group'] = $scope.tenant['endpoint-group'] ? $scope.tenant['endpoint-group'] : [];

                    if(object) {
                        object.forEach(function(o) {
                            $scope.tenant['endpoint-group'].push(GPBServices.stripNullValues(o));
                        });
                    }

                    break;

                 case 'epgs':
                    $scope.tenant['contract'] = $scope.tenant['contract'] ? $scope.tenant['contract'] : [];

                    if(object) {
                        object.forEach(function(o) {
                            $scope.tenant['contract'].push(GPBServices.stripNullValues(o));
                        });
                    }

                    break;
            }
        };

        $scope.submit = function(object) {
            $scope.tenant['endpoint-group'] = $scope.tenant['endpoint-group'] ? $scope.tenant['endpoint-group'] : [];

            if(object) {
                object.forEach(function(o) {
                    $scope.tenant['endpoint-group'].push(GPBServices.stripNullValues(o));
                });
            }


            path = GBPTenantServices.createPathObj($scope.tenant.id);
            GBPTenantServices.send(path, $scope.tenant, function(data){
                $scope.wizards.accessModelWizard = false;
                $scope.sendReloadEventFromRoot('GBP_GLOBAL_TENANT_RELOAD');
                $scope.reloadTopo();
            }, function(){
                //TODO: error cbk
            });
        };

        // $scope.updateList = function(list, object, key) {
        //     var elementPos = list.map(function(x) {return x[key]; }).indexOf(object[key]);

        //     if(elementPos < 0) {
        //         list.push(object);
        //     }
        //     else {
        //         list[elementPos] = object;
        //     }
        // };
    }]);

    gbpOld.register.controller('wizardTenantCtrl', ['$scope', '$filter', 'GBPTenantServices', function($scope, $filter, GBPTenantServices){
        // $scope.tenantList = [];
        $scope.newTenantObj = GBPTenantServices.createObj();
        $scope.displayLabel = ['name' , 'id'];

        $scope.view = {
            tenantEdit: false
        };

        // $scope.init = function() {
        //     $scope.getTenants();
        // };

        // $scope.getTenants = function() {
        //     GBPTenantServices.load(
        //         function(data) {
        //             $scope.tenantList = data;
        //             $scope.newTenantObj = GBPTenantServices.createObj();
        //         },
        //         function(){
        //             //TODO error
        //         }
        //     );
        // };

        $scope.reloadTenants = function(selectedObject) {
            if(!selectedObject) {
                selectedObject = GBPTenantServices.createObj();
                 $scope.view.tenantEdit = false;
            }
            else {
                $scope.view.tenantEdit = true;
            }

            $scope.selectedTenant = selectedObject;
            $scope.newTenantObj = selectedObject;
        };

        $scope.getNewTenantObject = function() {
            return GBPTenantServices.createObj();
        };
    }]);

    gbpOld.register.controller('wizardEpgCtrl', ['$scope', '$filter', 'GBPEpgServices', function($scope, $filter, GBPEpgServices){
        $scope.list = [];
        $scope.newEpgObj = GBPEpgServices.createObj();
        $scope.selectedEpg = null;
        $scope.epgFormView = true;

        $scope.displayLabel = ['name', 'id'];
        $scope.crudLabel = 'Group list';

        $scope.igpOpts = ['allow', 'require-contract'];

        $scope.init = function() {

        };

        $scope.showForm = function() {
            $scope.epgFormView = true;
            $scope.newEpgObj = GBPEpgServices.createObj();
        };

        $scope.save = function() {
            $scope.updateList($scope.list, $scope.newEpgObj, "id");
            $scope.reload($scope.newEpgObj);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newEpgObj);
        };

        $scope.delete = function() {
            if($scope.selectedEpg) {
                var index = $scope.list.indexOf($scope.selectedEpg);
                $scope.list.splice(index, 1);
                $scope.epgFormView = false;
            }
            //$scope.newEpgObj = GBPEpgServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedEpg = selectedObj;
            $scope.newEpgObj = selectedObj;
            $scope.epgFormView = true;
            $scope.$broadcast('WIZARD_EPG_RELOAD');
        };

        $scope.close = function() {
            $scope.epgFormView = false;
            $scope.newEpgObj = GBPEpgServices.createObj();
            $scope.selectedEpg = null;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.epgFormView = true;
                    angular.copy($scope.selectedEpg, $scope.newEpgObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on("WIZARD_CNS_RELOAD", function(event, args){
            //$scope.selectedEpg['consumer-named-selector'] = args;
            $scope.newEpgObj['consumer-named-selector'] = args;
            $scope.updateList($scope.list, $scope.newEpgObj, "id");
        });

        $scope.$on("WIZARD_PNS_RELOAD", function(event, args){
            //$scope.selectedEpg['provider-named-selector'] = args;
            $scope.newEpgObj['provider-named-selector'] = args;
            $scope.updateList($scope.list, $scope.newEpgObj, "id");
        });
    }]);

    gbpOld.register.controller('wizardContractCtrl', ['$scope', '$filter', 'GBPContractServices', function($scope, $filter, GBPContractServices){
        $scope.list = [];
        $scope.newContractObj = GBPContractServices.createObj();
        $scope.selectedContract = null;

        $scope.displayLabel = ['description', 'id'];
        $scope.crudLabel = 'Contract list';

        $scope.contractFormView = true;

        $scope.init = function() {

        };

        $scope.showForm = function() {
            $scope.contractFormView = true;
            $scope.newContractObj = GBPContractServices.createObj();
        };

        $scope.save = function() {
            $scope.updateList($scope.list, $scope.newContractObj, "id");
            $scope.reload($scope.newContractObj);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newContractObj);
        };

        $scope.delete = function() {
            if($scope.selectedContract) {
                var index = $scope.list.indexOf($scope.selectedContract);
                $scope.list.splice(index, 1);
                $scope.contractFormView = false;
                $scope.newContractObj = GBPContractServices.createObj();
                $scope.selectedContract = null;
            }
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedContract = selectedObj;
            $scope.newContractObj = selectedObj;
            $scope.contractFormView = true;
        };

        $scope.close = function() {
            $scope.contractFormView = false;
            //$scope.newContractObj = GBPContractServices.createObj();
            //$scope.selectedContract = null;
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedContract ) {
                    $scope.contractFormView = true;
                    angular.copy($scope.selectedContract, $scope.newContractObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on("WIZARD_SUBJECT_RELOAD", function(event, args){
            //$scope.selectedEpg['consumer-named-selector'] = args;
            $scope.newContractObj['subject'] = args;
            $scope.updateList($scope.list, $scope.newContractObj, "id");
        });

        $scope.$on("WIZARD_CLAUSE_RELOAD", function(event, args){
            //$scope.selectedEpg['consumer-named-selector'] = args;
            $scope.newContractObj['clause'] = args;
            $scope.updateList($scope.list, $scope.newContractObj, "id");
        });
    }]);

    gbpOld.register.controller('wizardCnsCtrl',['$scope', 'GBPConNamedSelServices', function($scope, GBPConNamedSelServices){
        $scope.list = [];
        $scope.selectedCNS = null;
        $scope.newCNSObj = GBPConNamedSelServices.createObj();
        $scope.internalView = {
            cns: false,
            edit: "view"
        };
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Consumer named selectors list';

        $scope.contractList = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_CONTRACTS_LABEL', $scope.contractList);

        var clear = function(){
                $scope.list = [];
                $scope.selectedCNS = null;
                $scope.newCNSObj = GBPConNamedSelServices.createObj();
                $scope.internalView = {
                    cns: false,
                    edit: "add"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newCNSObj && $scope.newCNSObj.contract) {
                var objToPush = templateObj || "";
                $scope.newCNSObj.contract.push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newCNSObj && $scope.newCNSObj.contract) {
                $scope.newCNSObj.contract.splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newCNSObj && $scope.newCNSObj.contract && $scope.newCNSObj.contract.length >= index) {
                $scope.newCNSObj.contract[index] = value;
            }
        };
        //move to separate ctrl /\

        $scope.init = function() {
            clear();

            if($scope.tenant && $scope.tenant['contract'].length>0) {
                $scope.contractList.options = $scope.tenant['contract'];
            }

            if($scope.selectedEpg && $scope.selectedEpg['consumer-named-selector']) {
                $scope.list = $scope.selectedEpg['consumer-named-selector'];
            }
        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newCNSObj, "name");
            $scope.reload($scope.newCNSObj);
            $scope.$emit('WIZARD_CNS_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newCNSObj);
        };

        $scope.delete = function() {
            if($scope.selectedCNS) {
                var index = $scope.list.indexOf($scope.selectedCNS);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    cns: false,
                    edit: "add"
                };
                $scope.$emit('WIZARD_CNS_RELOAD', $scope.list);
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.selectedCNS = null;
            $scope.internalView.cns = true;
            $scope.internalView.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newCNSObj = GBPConNamedSelServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedCNS = selectedObj;
            $scope.newCNSObj = selectedObj;
            $scope.internalView.cns = true;
        };

        $scope.close = function(){
            $scope.internalView.cns = false;
            //$scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.internalView.cns = true;
                    $scope.internalView.edit = "add";
                    angular.copy($scope.selectedCNS, $scope.newCNSObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('WIZARD_EPG_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('wizardPnsCtrl',['$scope', 'GBPProNamedSelServices', function($scope, GBPProNamedSelServices){
        $scope.list = [];
        $scope.selectedPNS = null;
        $scope.newPNSObj = GBPProNamedSelServices.createObj();
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Provider named selectors list';
        $scope.internalView = {
            pns: false,
            edit: "view"
        };

        $scope.contractList = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_CONTRACTS_LABEL', $scope.contractList);

        var clear = function(){
                $scope.list = [];
                $scope.selectedPNS = null;
                $scope.newPNSObj = GBPProNamedSelServices.createObj();
                $scope.internalView = {
                    pns: false,
                    edit: "view"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newPNSObj && $scope.newPNSObj.contract) {
                var objToPush = templateObj || "";
                $scope.newPNSObj.contract.push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newPNSObj && $scope.newPNSObj.contract) {
                $scope.newPNSObj.contract.splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newPNSObj && $scope.newPNSObj.contract && $scope.newPNSObj.contract.length >= index) {
                $scope.newPNSObj.contract[index] = value;
            }
        };
        //move to separate ctrl /\

        $scope.init = function() {
            clear();

            if($scope.tenant && $scope.tenant['contract'].length>0) {
                $scope.contractList.options = $scope.tenant['contract'];
            }

            if($scope.selectedEpg && $scope.selectedEpg['provider-named-selector']) {
                $scope.list = $scope.selectedEpg['provider-named-selector'];
            }
        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newPNSObj, "name");
            $scope.reload($scope.newPNSObj);
            $scope.$emit('WIZARD_PNS_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newPNSObj);
        };

        $scope.delete = function() {
            if($scope.selectedPNS) {
                var index = $scope.list.indexOf($scope.selectedPNS);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    pns: false,
                    edit: "add"
                };
                $scope.$emit('WIZARD_PNS_RELOAD', $scope.list);
            }
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.selectedPNS = null;
            $scope.internalView.pns = true;
            $scope.internalView.edit = "add";
        };

        $scope.reloadNewObj = function() {
            $scope.newPNSObj = GBPProNamedSelServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedPNS = selectedObj;
            $scope.newPNSObj = selectedObj;
            $scope.internalView.pns = true;
        };

        $scope.close = function(){
            $scope.internalView.pns = false;
            $scope.internalView.edit = "view";
        };
        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedEpg ) {
                    $scope.internalView.pns = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedPNS, $scope.newPNSObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('WIZARD_EPG_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('wizardSubjectCtrl', ['$scope','GBPSubjectServices', '$filter', function($scope, GBPSubjectServices, $filter){
        $scope.list = [];
        $scope.selectedSubject = null;
        $scope.newSubjectObj = GBPSubjectServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            subject : false,
            edit : "view"
        };
        $scope.crudLabel = 'Subject list';
        $scope.errors = {};


        var path = null,
            mandatoryProperties = ['order'],

            clear = function(){
                $scope.list = [];
                $scope.selectedSubject = null;
                $scope.newSubjectObj = GBPSubjectServices.createObj();
                $scope.internalView = {
                    subject : false,
                    edit : "view"
                };
            };

        $scope.init = function() {

        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newSubjectObj, "name");
            $scope.reload($scope.newSubjectObj);
            $scope.$emit('WIZARD_SUBJECT_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newSubjectObj);
        };

        $scope.delete = function() {
            if($scope.selectedSubject) {
                var index = $scope.list.indexOf($scope.selectedSubject);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    subject: false,
                    edit: "add"
                };
                $scope.reloadNewObj();
                $scope.$emit('WIZARD_SUBJECT_RELOAD', $scope.list);
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newSubjectObj = GBPSubjectServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedSubject = selectedObj;
            $scope.newSubjectObj = selectedObj;
            $scope.internalView.subject = true;
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.subject = true;
            $scope.internalView.edit = "add";
            $scope.selectedSubject = null;
        };

        $scope.close = function(){
            $scope.internalView.subject = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedSubject ) {
                    $scope.internalView.subject = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedSubject, $scope.newSubjectObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_CONTRACT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on('GBP_SUBJECTS_LABEL', function(event, obj){
            obj.labels = $scope.displayLabel;
        });

        $scope.$on("WIZARD_RULE_RELOAD", function(event, args){
            //$scope.selectedEpg['consumer-named-selector'] = args;
            $scope.newSubjectObj['rule'] = args;
            $scope.updateList($scope.list, $scope.newSubjectObj, "id");
            $scope.$emit('WIZARD_SUBJECT_RELOAD', $scope.list);
        });
    }]);

    gbpOld.register.controller('wizardRuleCtrl', ['$scope','GBPRuleServices', '$filter', function($scope, GBPRuleServices, $filter){
        $scope.list = [];
        $scope.selectedRule = null;
        $scope.newRuleObj = GBPRuleServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            rule : false,
            edit : "view"
        };
        $scope.crudLabel = 'Rule list';
        $scope.errors = {};

        var path = null,
            mandatoryProperties = ['order'],

            clear = function(){
                $scope.list = [];
                $scope.selectedRule = null;
                $scope.newRuleObj = GBPRuleServices.createObj();
                $scope.internalView = {
                    rule : false,
                    edit : "view"
                };
            };

        $scope.init = function() {

        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newRuleObj, "name");
            $scope.reload($scope.newRuleObj);
            $scope.$emit('WIZARD_RULE_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newRuleObj);
        };

        $scope.delete = function() {
            if($scope.selectedRule) {
                var index = $scope.list.indexOf($scope.selectedRule);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    rule: false,
                    edit: "add"
                };
                $scope.reloadNewObj();
                $scope.$emit('WIZARD_RULE_RELOAD', $scope.list);
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newRuleObj = GBPRuleServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedRule = selectedObj;
            $scope.newRuleObj = selectedObj;
            $scope.internalView.rule = true;
            $scope.$broadcast('WIZARD_RULE_RELOAD');
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.rule = true;
            $scope.internalView.edit = "add";
            $scope.selectedRule = null;
        };

        $scope.close = function(){
            $scope.internalView.rule = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedRule ) {
                $scope.internalView.rule = true;
                $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedRule, $scope.newRuleObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('GBP_SUBJECT_RELOAD',function(){
            $scope.init();
        });

        $scope.$on("WIZARD_CLASREF_RELOAD", function(event, args){
            $scope.newRuleObj['classifier-ref'] = args;
            $scope.updateList($scope.list, $scope.newRuleObj, "name");
            $scope.$emit('WIZARD_RULE_RELOAD', $scope.list);
        });

        $scope.$on("WIZARD_ACTIONREF_RELOAD", function(event, args){
            $scope.newRuleObj['action-ref'] = args;
            $scope.updateList($scope.list, $scope.newRuleObj, "name");
            $scope.$emit('WIZARD_RULE_RELOAD', $scope.list);
        });
    }]);

    gbpOld.register.controller('wizardClauseCtrl', ['$scope','GBPClauseServices', 'GBPSubjectServices',
        function($scope, GBPClauseServices, GBPSubjectServices){
        $scope.list = [];
        $scope.selectedClause = null;
        $scope.newClauseObj = GBPClauseServices.createObj();
        $scope.internalView = {
            clause: false,
            edit: "view"
        };
        $scope.displayLabel = 'name';
        $scope.crudLabel = 'Clause list';

        $scope.subjects = {'options' : [], 'labels' : null};
        $scope.getDisplayLabelsFromCtrl('GBP_SUBJECTS_LABEL', $scope.subjects);

        var path = null,

            clear = function(){
                $scope.list = [];
                $scope.selectedClause = null;
                $scope.newClauseObj = GBPClauseServices.createObj();
                $scope.internalView = {
                    clause: false,
                    edit: "view"
                };
            };

        //move to separate ctrl \/
        $scope.addNewElem = function(templateObj) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs']) {
                $scope.init();
                var objToPush = templateObj || "";
                $scope.newClauseObj['subject-refs'].push(objToPush);
            }
        };

        $scope.deleteElemAt = function(index) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs']) {
                $scope.newClauseObj['subject-refs'].splice(index, 1);
            }
        };

        $scope.updateAt = function(index, value) {
            if($scope.newClauseObj && $scope.newClauseObj['subject-refs'] && $scope.newClauseObj['subject-refs'].length >= index) {
                $scope.newClauseObj['subject-refs'][index] = value;
            }
        };
        //move to separate ctrl /\

        $scope.init = function() {
            if($scope.selectedContract && $scope.selectedContract['subject'].length>0) {
                $scope.subjects.options = $scope.selectedContract['subject'];
            }
        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newClauseObj, "name");
            $scope.reload($scope.newClauseObj);
            $scope.$emit('WIZARD_CLAUSE_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newClauseObj);
        };

        $scope.delete = function() {
            if($scope.selectedClause) {
                var index = $scope.list.indexOf($scope.selectedClause);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    clause: false,
                    edit: "add"
                };
                $scope.$emit('WIZARD_CLAUSE_RELOAD', $scope.list);
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newClauseObj = GBPClauseServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedClause = selectedObj;
            $scope.newClauseObj = selectedObj;
            $scope.internalView.clause = true;
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.clause = true;
            $scope.internalView.edit = "add";
            $scope.selectedClause = null;
        };

        $scope.close = function(){
            $scope.internalView.clause = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedClause ) {
                    $scope.internalView.clause = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedClause, $scope.newClauseObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('WIZARD_SUBJECT_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('wizardActionRefCtrl', ['$scope','GBPActionRefsServices', 'GBPActionInstanceServices', '$filter', function($scope, GBPActionRefsServices, GBPActionInstanceServices, $filter){
        $scope.list = [];
        $scope.selectedActionRef = null;
        $scope.newActionRefObj = GBPActionRefsServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            actionRef : false,
            edit : "view"
        };
        $scope.crudLabel = 'Action ref list';

        $scope.actionInstanceNames = {'options' : [], 'labels' : $scope.displayLabel};

        var path = null,
            mandatoryProperties = ['order'],

            actionInstanceNamesLoad = function() {
                if($scope.tenant) {
                    var actionInstancePath = GBPActionInstanceServices.createPathObj($scope.tenant.id);
                    GBPActionInstanceServices.load(actionInstancePath, function(data){
                        $scope.actionInstanceNames.options = data;
                    },function(){
                        //TODO: error cbk
                    });
                }
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedActionRef = null;
                $scope.newActionRefObj = GBPActionRefsServices.createObj();
                $scope.internalView = {
                    actionRef : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            actionInstanceNamesLoad();
        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newActionRefObj, "name");
            $scope.reload($scope.newActionRefObj);
            $scope.$emit('WIZARD_ACTIONREF_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newActionRefObj);
        };

        $scope.delete = function() {
            if($scope.selectedActionRef) {
                var index = $scope.list.indexOf($scope.selectedActionRef);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    actionRef: false,
                    edit: "add"
                };
                $scope.$emit('WIZARD_ACTIONREF_RELOAD', $scope.list);
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newActionRefObj = GBPActionRefsServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedActionRef = selectedObj;
            $scope.newActionRefObj = selectedObj;
            $scope.internalView.actionRef = true;
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.actionRef = true;
            $scope.internalView.edit = "add";
            $scope.selectedActionRef = null;
        };

        $scope.close = function(){
            $scope.internalView.actionRef = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedActionRef ) {
                    $scope.internalView.actionRef = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedActionRef, $scope.newActionRefObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('WIZARD_RULE_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('wizardClassifierRefCtrl', ['$scope','GBPClassifierRefsServices', 'GBPClassifierInstanceServices', '$filter', function($scope, GBPClassifierRefsServices, GBPClassifierInstanceServices, $filter){
        $scope.list = [];
        $scope.selectedClassifierRef = null;
        $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
        $scope.displayLabel = 'name';
        $scope.internalView = {
            classifierRef : false,
            edit : "view"
        };

        $scope.instanceNames = {'options' : [], 'labels' : $scope.displayLabel};

        $scope.formDirections = ['in', 'out', 'bidirectional'];
        $scope.formConnectionTracking = ['normal', 'reflexive'];

        $scope.crudLabel = 'Classifier ref list';

        var path = null,

            instanceNamesLoad = function() {
                if($scope.tenant) {
                    var classifierInstancePath = GBPClassifierInstanceServices.createPathObj($scope.tenant.id);
                    GBPClassifierInstanceServices.load(classifierInstancePath, function(data){
                        $scope.instanceNames.options = data;
                    },function(){
                        //TODO: error cbk
                    });
                }
            },

            clear = function(){
                $scope.list = [];
                $scope.selectedClassifierRef = null;
                $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
                $scope.internalView = {
                    classifierRef : false,
                    edit : "view"
                };
            };

        $scope.init = function() {
            instanceNamesLoad();
        };

        $scope.save = function(){
            $scope.updateList($scope.list, $scope.newClassifierRefObj, "name");
            $scope.reload($scope.newClassifierRefObj);
            $scope.$emit('WIZARD_CLASREF_RELOAD', $scope.list);
            $scope.$broadcast('EV_SET_SEL_CLASS', $scope.newClassifierRefObj);
        };

        $scope.delete = function() {
            if($scope.selectedClassifierRef) {
                var index = $scope.list.indexOf($scope.selectedClassifierRef);
                $scope.list.splice(index, 1);
                $scope.internalView = {
                    classifierRef: false,
                    edit: "add"
                };
                $scope.$emit('WIZARD_CLASREF_RELOAD', $scope.list);
            }
        };

        $scope.reloadNewObj = function() {
            $scope.newClassifierRefObj = GBPClassifierRefsServices.createObj();
        };

        $scope.reload = function(selectedObj) {
            $scope.selectedClassifierRef = selectedObj;
            $scope.newClassifierRefObj = selectedObj;
            $scope.internalView.classifierRef = true;
        };

        $scope.showForm = function() {
            $scope.reloadNewObj();
            $scope.internalView.classifierRef = true;
            $scope.internalView.edit = "add";
            $scope.selectedClassifierRef = null;
        };

        $scope.close = function(){
            $scope.internalView.classifierRef = false;
            $scope.internalView.edit = "view";
        };

        $scope.$on('PGN_EDIT_ELEM', function(event){
            if (!event.defaultPrevented) {
                if ( $scope.selectedClassifierRef ) {
                    $scope.internalView.classifierRef = true;
                    $scope.internalView.edit = "edit";
                    angular.copy($scope.selectedClassifierRef, $scope.newClassifierRefObj);
                }
                event.defaultPrevented = true;
            }
        });

        $scope.$on('WIZARD_RULE_RELOAD',function(){
            $scope.init();
        });
    }]);

    gbpOld.register.controller('rendererStateCtrl', ['$scope', 'GPBServices', function($scope, GPBServices){
        $scope.data = {'subject-feature-definitions' : {}};
        $scope.view_path = 'src/app/gbp-old/views/governance';

        var init = function(){
            GPBServices.getDefinitions(function(classifiersDefs, actionsDefs){
                $scope.data['subject-feature-definitions']['classifier-definition'] = classifiersDefs;
                $scope.data['subject-feature-definitions']['action-definition'] = actionsDefs;
            }, function(){
                //error
            });
        };

        init();
    }]);

    gbpOld.register.controller('layerCtrl', ['$scope', function($scope){

        var moveOffset = 1;
        $scope.currentDisplayIndex = 1;
        $scope.displayOffsets = [-1, 0, 1];
        $scope.expanded = true;

        $scope.init = function(key, value){
            $scope.data = value;
            if($scope.checkData(value, 'Array')){
                $scope.setActData($scope.data[$scope.data.length - 1]);
            }
        };

        $scope.toggleExpanded = function(){
            $scope.expanded = !$scope.expanded;
        };

        $scope.shiftDisplayNext = function() {
            $scope.currentDisplayIndex = Math.min($scope.currentDisplayIndex + moveOffset, $scope.data.length - 2);
        };

        $scope.shiftDisplayPrev = function() {
            $scope.currentDisplayIndex = Math.max($scope.currentDisplayIndex - moveOffset, 1);
        };

        $scope.showPrevButton = function() {
            return $scope.currentDisplayIndex > 1;
        };

        $scope.showNextButton = function() {
            return $scope.data && $scope.currentDisplayIndex < $scope.data.length - 2;
        };

        $scope.setActData = function(data) {
            $scope.actSelected = data;
        };

        $scope.checkData = function(data, type){
            var result = {'Array' : data instanceof Array,
                        'Object' : data instanceof Object};

            return result[type];
        };
    }]);

    gbpOld.register.controller('actionReferenceWizardCtrl', ['$scope', '$filter', 'GBPRuleServices', 'GBPActionInstanceServices', function($scope, $filter, GBPRuleServices, GBPActionInstanceServices){
        $scope.wizardPage = null;
        $scope.path = {};
        $scope.rule = {};

        $scope.actionInstanceNames = {'options' : [], 'labels' : "name"};

        var actionInstanceNamesLoad = function() {
            var actionInstancePath = GBPActionInstanceServices.createPathObj($scope.selectedTenant.id);
            GBPActionInstanceServices.load(actionInstancePath, function(data){
                $scope.actionInstanceNames.options = data;
            },function(){
                //TODO: error cbk
            });
        };

        $scope.init = function() {
            $scope.setPage('reference');
        };

        $scope.setPage = function(pageName, object) {
            $scope.wizardPage = pageName;
        };

        $scope.submit = function() {
            //if($scope.validateForm($scope.actionsForm)){
                $scope.actionInstanceNames.options.forEach(function(i) {
                    path = GBPActionInstanceServices.createPathObj($scope.path.tenantId, i.name);
                //saveParams();

                    GBPActionInstanceServices.send(path, i, function(data){
                    $scope.sendReloadEventFromRoot('GBP_ACTION_INSTANCE_RELOAD');
                    }, function(){
                        //TODO: error cbk
                    });
                });

            //}

            //if($scope.validateForm($scope.rulesForm)){
                path = GBPRuleServices.createPathObj($scope.path.tenantId, $scope.path.contractId, $scope.path.subjectId, $scope.path.ruleId);
                GBPRuleServices.send(path, $scope.rule, function(data){

                    $scope.wizards.actionReferenceWizard = false;

                    //$scope.sendReloadEventFromRoot('GBP_TENANT_RELOAD');
                }, function(){
                    //TODO: error cbk
                });
            //}
            //$scope.
        };

        $scope.$on('ACTION_RULE_WIZARD_LOAD', function(event, data){
            $scope.rule = angular.copy(data.data);
            $scope.path = data.path;
        });

        $scope.$on('WIZARD_ACTIONREF_ADD', function(event, data){
            if(!$scope.rule['action-ref']) {
                $scope.rule['action-ref'] = [];
            }
            $scope.updateList($scope.rule['action-ref'], data, "name");
        });

        $scope.$on('WIZARD_ACTIONREF_DELETE', function(event, data){
            $scope.rule['action-ref'].splice(data, 1);
        });

        $scope.$on('WIZARD_ACTIONINSTANCE_ADD', function(event, data){
            $scope.updateList($scope.actionInstanceNames.options, data, "name");

            $scope.setPage('reference');
        });

        $scope.$on('GBP_TENANT_RELOAD',function(){
            actionInstanceNamesLoad();
        });

    }]);

    gbpOld.register.controller('actionsRefListCtrl', ['$scope', '$filter', function($scope, $filter){

        $scope.actionReferenceForm = false;

        $scope.showForm = function(object) {
            $scope.actionReferenceForm = true;
            $scope.newActionRefObj = object || null;
        };

        $scope.closeForm = function() {
            $scope.actionReferenceForm = false;
        };

        $scope.save = function(){
            $scope.$emit('WIZARD_ACTIONREF_ADD', $scope.newActionRefObj);
            $scope.resetObject();
        };

        $scope.deleteElemAt = function(index) {
            $scope.$emit('WIZARD_ACTIONREF_DELETE', index);
            $scope.resetObject();
        };

        $scope.resetObject = function() {
            $scope.newActionRefObj = null;
        };



    }]);

    gbpOld.register.controller('actionInstanceWizardCtrl', ['$scope', '$filter', 'GPBServices', 'GBPActionInstanceServices', function($scope, $filter, GPBServices, GBPActionInstanceServices){
        $scope.actionDefinitions = {'options' : [], 'labels' : "name"};
        $scope.newActionObj = GBPActionInstanceServices.createObj();

        var loadDefinitions = function() {
            GPBServices.getDefinitions(function(classifierDefs, actionDefs) {
                $scope.actionDefinitions.options = actionDefs;
                //$scope.getDisplayLabelsFromCtrl('GBP_ACTIONS_LABEL', $scope.actionDefinitions);
            });
        };

        $scope.reloadDefs = function(){
            $scope.defs = angular.copy($scope.getDefinitionObjParams($scope.newActionObj['action-definition-id']));

            //TODO: rework
            if($scope.defs.length && $scope.defs[0].name === 'sfc-chain-name') {
                GPBServices.getServiceFunctionChains(function(data) {
                    $scope.serviceFunctionChains = data;
                });
            }
        };

        $scope.getDefinitionObjParams = function(id){
            return GPBServices.getDefinitionObjParams($scope.actionDefinitions.options, id);
        };

        $scope.save = function(){
            $scope.newActionObj['parameter-value'] = [];
            $scope.$broadcast('GBP_SAVE_PARAM');
            $scope.$emit('WIZARD_ACTIONINSTANCE_ADD', $scope.newActionObj);
            $scope.resetObject();
        };

        $scope.resetObject = function() {
            $scope.newActionObj = GBPActionInstanceServices.createObj();
        };

        $scope.saveParam = function() {

        };

        $scope.addParam = function(name, type, value) {
            $scope.newActionObj['parameter-value'].push(GPBServices.createParamObj(name, type, value));
        };

        loadDefinitions();

    }]);

});


