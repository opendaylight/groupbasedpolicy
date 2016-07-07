define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').service('ResolvedPolicyListService', ResolvedPolicyListService);

    ResolvedPolicyListService.$inject = ['$filter', 'Restangular', 'ResolvedPolicyService'];

    function ResolvedPolicyListService($filter, Restangular, ResolvedPolicyService) {
        /* methods */
        this.createList = createList;
        this.generateLinkId = generateLinkId;


        function ResolvedPolicyList() {
            /* properties */
            this.data = [];

            /* methods */
            this.aggregateResolvedPolicies = aggregateResolvedPolicies;
            this.get = get;
            this.clearData = clearData;
            this.setData = setData;

            /* Implementation */
            /**
             * fills ResolvedPolicyList object with data
             * @param data
             */
            function setData(data) {
                var self = this;

                data && data.forEach(function (dataElement) {
                    self.data.push(ResolvedPolicyList.createObject(dataElement));
                });
            }

            function clearData() {
                var self = this;
                self.data = [];
            }

            function get(tenant, successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('operational').one('resolved-policy:resolved-policies');

                return restObj.get().then(function(data) {
                    if(tenant) {
                        self.data = $filter('filter')(data['resolved-policies']['resolved-policy'], {
                            'consumer-tenant-id': tenant,
                            'provider-tenant-id': tenant
                        });
                    }
                    else {
                        self.data = data['resolved-policies']['resolved-policy'];
                    }

                    successCallback();
                });
            }

            function aggregateResolvedPolicies() {
                var self = this,
                    result = {epgs: {}, contracts: {}};

                self.data.forEach(function(rp) {
                    processEpg(result, rp, 'consumer');
                    processEpg(result, rp, 'provider');

                    if(rp.hasOwnProperty('policy-rule-group-with-endpoint-constraints')) {
                        processConstraints(
                            result,
                            rp['policy-rule-group-with-endpoint-constraints'],
                            rp['provider-epg-id'],
                            rp['consumer-epg-id']
                        );
                    }
                });

                return result;

            }

            function processEpg(returnValue, resolvedPolicyItem, type) {
                if(!returnValue.epgs.hasOwnProperty(resolvedPolicyItem[type+'-epg-id'])) {
                    returnValue.epgs[resolvedPolicyItem[type+'-epg-id']] = {
                        'provided-contracts' : [],
                        'consumed-contracts':[]
                    };
                }



            }

            function processConstraints(returnValue, constraints, providerEpgId, consumerEpgId) {
                constraints.forEach(function (element) {
                    element['policy-rule-group'].forEach(function (el) {
                        var linkId = generateLinkId(el['contract-id'], providerEpgId, consumerEpgId);

                        updateEpg(returnValue, el['contract-id'], providerEpgId, 'provided');
                        updateEpg(returnValue, el['contract-id'], consumerEpgId, 'consumed');

                        if (!returnValue.contracts.hasOwnProperty(linkId)) {
                            returnValue.contracts[linkId] = {
                                'contract-id': el['contract-id'],
                                'subjects': {}
                            };
                        }

                        if (!returnValue.contracts[linkId].subjects.hasOwnProperty(el['subject-name'])) {
                            returnValue.contracts[linkId].subjects[el['subject-name']] = { 'resolved-rule': [] };
                        }

                        returnValue.contracts[linkId].subjects[el['subject-name']]['resolved-rule'].push(el['resolved-rule']);
                    });
                });
            }

            function updateEpg(returnValue, contractId, epgId, epgType) {
                returnValue.epgs[epgId][epgType+'-contracts'].push(contractId);
            }
        }

        function generateLinkId(contractId, providerEpgId, consumerEpgId) {
            return contractId + '_' + providerEpgId + '_' + consumerEpgId;
        }

        function createList(data) {
            var obj = new ResolvedPolicyList();

            if(data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return ResolvedPolicyListService;
});
