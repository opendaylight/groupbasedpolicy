define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').service('ResolvedPolicyListService', ResolvedPolicyListService);

    ResolvedPolicyListService.$inject = ['$filter', 'Restangular', 'ResolvedPolicyService'];

    function ResolvedPolicyListService($filter, Restangular, ResolvedPolicyService) {
        /* methods */
        this.createList = createList;


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

                if (data) {
                    data.forEach(function (dataElement) {
                        self.data.push(ResolvedPolicyService.createObject(dataElement));
                    });
                }
            }

            /**
             * Clears data property of ResolvedPolicyList object
             */
            function clearData() {
                var self = this;
                self.data = [];
            }

            /**
             * Reads data from operational datastore and filters it by tenant property if available
             * @param tenant
             * @param successCallback
             */
            function get(tenant, successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('operational').one('resolved-policy:resolved-policies');

                return restObj.get().then(function (data) {
                    if (tenant) {
                        self.data = $filter('filter')(data['resolved-policies']['resolved-policy'], {
                            'consumer-tenant-id': tenant,
                            'provider-tenant-id': tenant,
                        });
                    }
                    else {
                        self.data = data['resolved-policies']['resolved-policy'];
                    }

                    successCallback();
                });
            }

            /**
             * Process resolved policies and returns object with epgs and contracts properties
             * @returns {{epgs: {}, contracts: {}}}
             */
            function aggregateResolvedPolicies() {
                var self = this,
                    result = { epgs: {}, contracts: {} };

                self.data.forEach(function (rp) {
                    processEpg(result, rp, 'consumer');
                    processEpg(result, rp, 'provider');

                    if (rp.hasOwnProperty('policy-rule-group-with-endpoint-constraints')) {
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

            /**
             * Checks if exists object in returnValue.epgs and if not, creates one
             * @param returnValue
             * @param resolvedPolicyItem
             * @param type
             */
            function processEpg(returnValue, resolvedPolicyItem, type) {
                if (!returnValue.epgs.hasOwnProperty(resolvedPolicyItem[type + '-epg-id'])) {
                    returnValue.epgs[resolvedPolicyItem[type + '-epg-id']] = {
                        'provided-contracts': [],
                        'consumed-contracts': [],
                    };
                }



            }

            /**
             * Process policy-rule-group-with-endpoint-constraints from resolved-policy.
             * Creates contracts and updates epgs with contract objects
             * @param returnValue
             * @param constraints
             * @param providerEpgId
             * @param consumerEpgId
             */
            function processConstraints(returnValue, constraints, providerEpgId, consumerEpgId) {
                constraints.forEach(function (element) {
                    element['policy-rule-group'].forEach(function (el) {
                        var linkId = generateLinkId(el['contract-id'], providerEpgId, consumerEpgId);

                        if (!returnValue.contracts.hasOwnProperty(linkId)) {
                            returnValue.contracts[linkId] = {
                                'contract-id': el['contract-id'],
                                'linkId': linkId,
                                'subjects': {},
                                'type': '',
                            };
                        }

                        if (!returnValue.contracts[linkId].subjects.hasOwnProperty(el['subject-name'])) {
                            returnValue.contracts[linkId].subjects[el['subject-name']] = { 'resolved-rule': [] };
                        }

                        returnValue.contracts[linkId].subjects[el['subject-name']]['resolved-rule'] =
                            returnValue.contracts[linkId].subjects[el['subject-name']]['resolved-rule'].concat(el['resolved-rule']);

                        Object.keys(returnValue.contracts[linkId].subjects).forEach(function(key) {
                            returnValue.contracts[linkId].type =
                                getContractType(returnValue.contracts[linkId].subjects[key]) ? 'chain' : 'allow';
                        })

                        updateEpg(returnValue, returnValue.contracts[linkId], providerEpgId, 'provided');
                        updateEpg(returnValue, returnValue.contracts[linkId], consumerEpgId, 'consumed');
                    });
                });
            }

            /**
             * Updates epgobject with contract object
             * @param returnValue
             * @param contract
             * @param epgId
             * @param epgType
             */
            function updateEpg(returnValue, contract, epgId, epgType) {
                returnValue.epgs[epgId][epgType + '-contracts'].push(contract);
            }
        }

        /**
         * Creates ResolvedPolicyList object
         * @param data
         * @returns {ResolvedPolicyList}
         */
        function createList(data) {
            var obj = new ResolvedPolicyList();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }

        /**
         * creates linkId string from input parameters
         * @param contractId
         * @param providerEpgId
         * @param consumerEpgId
         * @returns {string}
         */
        function generateLinkId(contractId, providerEpgId, consumerEpgId) {
            return contractId + '++' + providerEpgId + '++' + consumerEpgId;
        }

        function getContractType(subject) {
            return subject['resolved-rule'].some(function(s) {
                return s.action.some(function (a) {
                    return a['action-definition-id'] === 'Action-Chain';
                });
            });
        }
    }

    return ResolvedPolicyListService;
});
