/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;

public class SubjectFeaturesTest {

    @Test
    public void testGetClassifier() {
        assertEquals(Classifier.ETHER_TYPE_CL, SubjectFeatures.getClassifier(EtherTypeClassifierDefinition.ID));
        assertEquals(Classifier.IP_PROTO_CL, SubjectFeatures.getClassifier(IpProtoClassifierDefinition.ID));
        assertEquals(Classifier.L4_CL, SubjectFeatures.getClassifier(L4ClassifierDefinition.ID));
    }

    @Test
    public void testGetActions() {
        assertNotNull(SubjectFeatures.getActions());
    }

    @Test
    public void testGetAction() {
        Assert.assertEquals(AllowActionDefinition.DEFINITION,
                SubjectFeatures.getAction(AllowActionDefinition.ID).getActionDef());
    }

}
