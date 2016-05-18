package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.test;

import java.util.ArrayList;

import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.IpProtoClassifierDefinition;
import org.opendaylight.groupbasedpolicy.api.sf.L4ClassifierDefinition;
import org.opendaylight.groupbasedpolicy.renderer.ofoverlay.statistics.flowcache.FlowCacheFactoryTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.common.rev140421.ParameterName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.instance.ParameterValueBuilder;

public class ParameterValueList extends ArrayList<ParameterValue> {

    public ParameterValueList() {
        super();
    }

    public ParameterValueList addEthertype(Long value) {
        this.add(newParameterValue(EtherTypeClassifierDefinition.ETHERTYPE_PARAM, value));
        return this;
    }

    public ParameterValueList addProto(Long value) {
        this.add(newParameterValue(IpProtoClassifierDefinition.PROTO_PARAM, value));
        return this;
    }

    public ParameterValueList addDstPort(Long value) {
        this.add(newParameterValue(L4ClassifierDefinition.DST_PORT_PARAM, value));
        return this;
    }

    public ParameterValueList addSrcPort(Long value) {
        this.add(newParameterValue(L4ClassifierDefinition.SRC_PORT_PARAM, value));
        return this;
    }

    public ParameterValue newParameterValue(String parameterName, Long intValue) {
        return new ParameterValueBuilder().setName(new ParameterName(parameterName))
                .setIntValue(intValue).build();
    }

}
