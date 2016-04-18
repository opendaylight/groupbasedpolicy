/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache;

import java.util.List;
import java.util.Objects;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow.FlowUtils;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCache.FlowCacheBuilder;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheDefinition.FlowCacheDefinitionBuilder;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.FlowCacheCons;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.util.IidSflowNameUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.resolved.policy.rev150828.has.classifiers.Classifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCacheFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FlowCacheFactory.class);

    public static FlowCache createFlowCache(InstanceIdentifier<Classifier> classifierIid, Classifier classifier,
            FlowCacheCons.Value value) {
        FlowCacheDefinition flowCacheDefinition = createFlowCacheDefinition(classifier, value);
        if (flowCacheDefinition == null) {
            LOG.info("Cannot create flow cache for statistics of classifier {}\n{}", classifierIid, classifier);
            return null;
        }
        return new FlowCacheBuilder().setDefinition(flowCacheDefinition)
            .setName(IidSflowNameUtil.createFlowCacheName(classifierIid, value))
            .setDirection(classifier.getDirection())
            .build();
    }

    public static FlowCacheDefinition createFlowCacheDefinition(Classifier classifier,
            FlowCacheCons.Value value) {
        FlowCacheDefinitionBuilder fcdBuilder = new FlowCacheDefinitionBuilder();
        if (L4ClassifierDefinition.ID.equals(classifier.getClassifierDefinitionId())) {
            addEthTypeInfoToFlowCache(classifier, fcdBuilder);
            if (!addIpProtoInfoToFlowCache(classifier, fcdBuilder)) {
                return null;
            }
            if (!addL4InfoToFlowCache(classifier, fcdBuilder)) {
                return null;
            }
        } else if (IpProtoClassifierDefinition.ID.equals(classifier.getClassifierDefinitionId())) {
            addEthTypeInfoToFlowCache(classifier, fcdBuilder);
            if (!addIpProtoInfoToFlowCache(classifier, fcdBuilder)) {
                return null;
            }
        } else if (EtherTypeClassifierDefinition.ID.equals(classifier.getClassifierDefinitionId())) {
            addEthTypeInfoToFlowCache(classifier, fcdBuilder);
        } else {
            LOG.warn("Sflow stats will not be pulled because of unknown classifier: {}", classifier);
            return null;
        }
        fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.IP_SOURCE.get()).addValue(FlowCacheCons.Key.IP_DESTINATION.get());
        fcdBuilder.setValue(value.get());
        return fcdBuilder.build();
    }

    private static void addEthTypeInfoToFlowCache(Classifier classifier, FlowCacheDefinitionBuilder fcdBuilder) {
        List<ParameterValue> parametersAndValues = classifier.getParameterValue();
        ParameterValue ethTypeParam = getParamVal(parametersAndValues, EtherTypeClassifierDefinition.ETHERTYPE_PARAM);
        if (ethTypeParam != null) {
            fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.ETH_PROTOCOL.get());
            fcdBuilder.getFilterBuilder()
                .addValue(FlowCacheCons.Key.ETH_PROTOCOL.get() + FlowCacheCons.EQ + ethTypeParam.getIntValue());
        } else {
            fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.ETH_PROTOCOL.get());
            fcdBuilder.getFilterBuilder()
                .addValue(FlowCacheCons.Key.ETH_PROTOCOL.get() + FlowCacheCons.EQ + FlowUtils.IPv4 + FlowCacheCons.OR
                        + FlowCacheCons.Key.ETH_PROTOCOL.get() + FlowCacheCons.EQ + FlowUtils.IPv6);
        }
    }

    private static boolean addIpProtoInfoToFlowCache(Classifier classifier, FlowCacheDefinitionBuilder fcdBuilder) {
        List<ParameterValue> parametersAndValues = classifier.getParameterValue();
        ParameterValue ipProtoParam = getParamVal(parametersAndValues, IpProtoClassifierDefinition.PROTO_PARAM);
        if (ipProtoParam != null) {
            fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.IP_PROTOCOL.get());
            fcdBuilder.getFilterBuilder()
                .addValue(FlowCacheCons.Key.IP_PROTOCOL.get() + FlowCacheCons.EQ + ipProtoParam.getIntValue());
            return true;
        } else {
            LOG.trace("Cannot add ip-proto information to flow cache for Sflow-RT.");
            return false;
        }
    }

    private static boolean addL4InfoToFlowCache(Classifier classifier, FlowCacheDefinitionBuilder fcdBuilder) {
        List<ParameterValue> parametersAndValues = classifier.getParameterValue();
        ParameterValue ipProtoParam = getParamVal(parametersAndValues, IpProtoClassifierDefinition.PROTO_PARAM);
        ParameterValue dstPortParam = getParamVal(parametersAndValues, L4ClassifierDefinition.DST_PORT_PARAM);
        ParameterValue srcPortParam = getParamVal(parametersAndValues, L4ClassifierDefinition.SRC_PORT_PARAM);
        if (ipProtoParam == null || (dstPortParam == null && srcPortParam == null)) {
            LOG.trace(
                    "Cannot add L4 information to flow cache for Sflow-RT."
                            + "\nipProtoParam:{} dstPortParam:{} srcPortParam:{}",
                    ipProtoParam, dstPortParam, srcPortParam);
            return false;
        }
        if (dstPortParam != null) {
            if (!addTcpUdpPortKeys(ipProtoParam.getIntValue(), dstPortParam.getIntValue(), true, fcdBuilder)) {
                return false;
            }
        }
        if (srcPortParam != null) {
            if (!addTcpUdpPortKeys(ipProtoParam.getIntValue(), srcPortParam.getIntValue(), false, fcdBuilder)) {
                return false;
            }
        }
        return true;
    }

    private static ParameterValue getParamVal(List<ParameterValue> parametersAndValues, String paramName) {
        for (ParameterValue paramVal : parametersAndValues) {
            if (paramName.equals(paramVal.getName().getValue())) {
                return paramVal;
            }
        }
        return null;
    }

    private static boolean addTcpUdpPortKeys(Long ipProto, Long port, boolean isDstPort,
            FlowCacheDefinitionBuilder fcdBuilder) {
        if (isDstPort) {
            if (Objects.equals(ipProto, IpProtoClassifierDefinition.TCP_VALUE)) {
                fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.TCP_DST_PORT.get());
                fcdBuilder.getFilterBuilder().addValue(FlowCacheCons.Key.TCP_DST_PORT.get() + FlowCacheCons.EQ + port);
            } else if (Objects.equals(ipProto, IpProtoClassifierDefinition.UDP_VALUE)) {
                fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.UDP_DST_PORT.get());
                fcdBuilder.getFilterBuilder().addValue(FlowCacheCons.Key.UDP_DST_PORT.get() + FlowCacheCons.EQ + port);
            } else {
                LOG.info("Statistics cannot be collected for ip-proto {} and port {}", ipProto, port);
                return false;
            }
        } else {
            if (Objects.equals(ipProto, IpProtoClassifierDefinition.TCP_VALUE)) {
                fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.TCP_SRC_PORT.get());
                fcdBuilder.getFilterBuilder().addValue(FlowCacheCons.Key.TCP_SRC_PORT.get() + FlowCacheCons.EQ + port);
            } else if (Objects.equals(ipProto, IpProtoClassifierDefinition.UDP_VALUE)) {
                fcdBuilder.getKeysBuilder().addValue(FlowCacheCons.Key.UDP_SRC_PORT.get());
                fcdBuilder.getFilterBuilder().addValue(FlowCacheCons.Key.UDP_SRC_PORT.get() + FlowCacheCons.EQ + port);
            } else {
                LOG.info("Statistics cannot be collected for ip-proto {} and port {}", ipProto, port);
                return false;
            }
        }
        return true;
    }
}
