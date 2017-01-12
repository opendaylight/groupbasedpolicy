/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.sf;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ClassifierDefinitionId;

import com.google.common.collect.ImmutableMap;

public class SubjectFeatures {

    private static final EtherTypeClassifier ETHER_TYPE_CL = new EtherTypeClassifier(null);
    private static final IpProtoClassifier IP_PROTO_CL = new IpProtoClassifier(ETHER_TYPE_CL);
    private static final L4Classifier L4_CL = new L4Classifier(IP_PROTO_CL);

    private static final Map<ClassifierDefinitionId, org.opendaylight.groupbasedpolicy.renderer.vpp.sf.Classifier> CLASSIFIERS =
            ImmutableMap.of(ETHER_TYPE_CL.getId(), ETHER_TYPE_CL, IP_PROTO_CL.getId(), IP_PROTO_CL, L4_CL.getId(), L4_CL);

    public static Classifier getClassifier(ClassifierDefinitionId id) {
        return CLASSIFIERS.get(id);
    }
}
