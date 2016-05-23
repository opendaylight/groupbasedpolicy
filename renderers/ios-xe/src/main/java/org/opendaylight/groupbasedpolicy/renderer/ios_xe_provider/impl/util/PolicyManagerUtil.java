/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ios_xe_provider.impl.util;

import org.opendaylight.groupbasedpolicy.api.sf.ChainActionDefinition;
import org.opendaylight.sfc.provider.api.SfcProviderRenderedPathAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.TenantId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicyManagerUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyManagerUtil.class);

    public static ServiceFunctionPath getServicePath(List<ParameterValue> params) {
        if (params == null || params.isEmpty()) {
            LOG.error("Cannot found service path, parameter value is null");
            return null;
        }
        Map<String, Object> paramsMap = new HashMap<>();
        for (ParameterValue value : params) {
            if (value.getName() == null)
                continue;
            if (value.getIntValue() != null) {
                paramsMap.put(value.getName().getValue(), value.getIntValue());
            } else if (value.getStringValue() != null) {
                paramsMap.put(value.getName().getValue(), value.getStringValue());
            }
        }
        String chainName = null;
        for (String name : paramsMap.keySet()) {
            if (name.equals(ChainActionDefinition.SFC_CHAIN_NAME)) {
                chainName = (String) paramsMap.get(name);
            }
        }
        if (chainName == null) {
            LOG.error("Cannot found service path, chain name is null");
            return null;
        }
        ServiceFunctionPath serviceFunctionPath = findServiceFunctionPath(new SfcName(chainName));
        if (serviceFunctionPath == null) {
            LOG.error("Service function path not found for name {}", chainName);
            return null;
        }
        return serviceFunctionPath;
    }

    public static RenderedServicePath createRenderedPath(ServiceFunctionPath sfp, TenantId tenantId) {
        RenderedServicePath renderedServicePath;
        // Try to read existing RSP
        RspName rspName = new RspName(sfp.getName().getValue() + tenantId.getValue() + "-gbp-rsp");
        renderedServicePath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (renderedServicePath != null) {
            return renderedServicePath;
        }
        LOG.info("Rendered service path with name {} not found, creating a new one ..", rspName.getValue());
        CreateRenderedPathInput input = new CreateRenderedPathInputBuilder()
                .setParentServiceFunctionPath(sfp.getName().getValue())
                .setName(rspName.getValue())
                .setSymmetric(sfp.isSymmetric())
                .build();
        renderedServicePath = SfcProviderRenderedPathAPI.createRenderedServicePathAndState(sfp, input);
        LOG.info("Rendered service path {} created", rspName.getValue());
        return renderedServicePath;
    }

    private static ServiceFunctionPath findServiceFunctionPath(SfcName chainName) {
        ServiceFunctionPaths allPaths = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
        for (ServiceFunctionPath serviceFunctionPath : allPaths.getServiceFunctionPath()) {
            if (serviceFunctionPath.getServiceChainName().equals(chainName)) {
                return serviceFunctionPath;
            }
        }
        return null;
    }

    public static RenderedServicePath createSymmetricRenderedPath(ServiceFunctionPath sfp, RenderedServicePath rsp,
                                                                  TenantId tenantId) {
        RenderedServicePath reversedRenderedPath;
        // Try to read existing RSP
        RspName rspName = new RspName(sfp.getName().getValue() + tenantId.getValue() + "-gbp-rsp-Reverse");
        reversedRenderedPath = SfcProviderRenderedPathAPI.readRenderedServicePath(rspName);
        if (reversedRenderedPath != null) {
            return reversedRenderedPath;
        }
        LOG.info("Reversed rendered service path with name {} not found, creating a new one ..", rspName.getValue());
        reversedRenderedPath = SfcProviderRenderedPathAPI.createSymmetricRenderedServicePathAndState(rsp);
        LOG.info("Rendered service path {} created", rspName.getValue());
        return reversedRenderedPath;
    }

}
