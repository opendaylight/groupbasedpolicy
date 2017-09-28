/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.General;
import org.opendaylight.groupbasedpolicy.renderer.vpp.util.VppIidFactory;
import org.opendaylight.groupbasedpolicy.util.DataStoreHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.NatInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.NatInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.MappingTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.config.nat.instances.nat.instance.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class NatInstanceCommand extends AbstractConfigCommand {

    private final General.Operations operation;
    private final List<ExternalIpAddressPool> dynamicEntries;
    private final Map<Long, List<MappingEntryBuilder>> staticEntries;
    private final Long DEFAULT_FIB = 0L;

    public NatInstanceCommand(NatInstanceCommandBuilder builder) {
        operation = builder.getOperation();
        dynamicEntries = builder.getDynamicEntries();
        staticEntries = builder.getStaticEntries();
    }

    public General.Operations getOperation() {
        return operation;
    }

    public List<ExternalIpAddressPool> getDynamicEntries() {
        return dynamicEntries;
    }

    public Map<Long, List<MappingEntryBuilder>> getStaticEntries() {
        return staticEntries;
    }

    public List<MappingEntryBuilder> getStaticEntries(long fibId) {
        if (staticEntries == null) {
            return Collections.emptyList();
        }
        return staticEntries.get(fibId);
    }

    @Override
    public InstanceIdentifier<NatInstances> getIid() {
        return VppIidFactory.getNatInstancesIid();
    }

    @Override
    void put(ReadWriteTransaction rwTx) {
        rwTx.put(LogicalDatastoreType.CONFIGURATION, getIid(), buildNatInstances(), true);
    }

    @Override
    void merge(ReadWriteTransaction rwTx) {
        rwTx.merge(LogicalDatastoreType.CONFIGURATION, getIid(), buildNatInstances(), true);
    }

    @Override
    void delete(ReadWriteTransaction rwTx) {
        DataStoreHelper.removeIfExists(LogicalDatastoreType.CONFIGURATION, getIid(), rwTx);
    }

    private NatInstances buildNatInstances() {
        List<NatInstance> instances = Lists.newArrayList();
        NatInstanceBuilder builder = new NatInstanceBuilder();
        builder.setId(DEFAULT_FIB).setExternalIpAddressPool(this.getDynamicEntries());
        if (getStaticEntries().keySet().contains(DEFAULT_FIB)) {
            addStaticEntries(builder, getStaticEntries().get(DEFAULT_FIB));
            getStaticEntries().remove(DEFAULT_FIB);
        }
        instances.add(builder.build());
        builder.setExternalIpAddressPool(null);
        instances.addAll(getStaticEntries().entrySet().stream().map(entry -> {
            builder.setId(entry.getKey());
            addStaticEntries(builder, getStaticEntries().get(entry.getKey()));
            return builder.build();
        }).collect(Collectors.toList()));
        return new NatInstancesBuilder().setNatInstance(instances).build();
    }

    private void addStaticEntries(@Nonnull NatInstanceBuilder builder,
            @Nonnull List<MappingEntryBuilder> staticEntries) {
        builder.setMappingTable(null);
        AtomicInteger ai = new AtomicInteger();
        List<MappingEntry> mappingEntries = staticEntries.stream().map(me -> {
            int value = ai.get();
            ai.incrementAndGet();
            return me.setIndex((long) value).build();
        }).collect(Collectors.toList());
        MappingTable mappingTable = new MappingTableBuilder().setMappingEntry(mappingEntries).build();
        builder.setMappingTable(mappingTable);
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("NatInstanceCommand [operations=")
            .append(operation)
            .append(", dynamicEntries=")
            .append(Arrays.toString(dynamicEntries.toArray()))
            .append(", staticEntries= [");

        staticEntries.forEach((aLong, mappingEntryBuilders) -> {
            stringBuffer.append("VrfId= ").append(aLong).append(", mappingEntries= [");
            mappingEntryBuilders.forEach(
                    mappingEntryBuilder -> stringBuffer.append(mappingEntryBuilder.build().toString()).append(", "));
            stringBuffer.append("]");
        });
        stringBuffer.append("]");

        return stringBuffer.toString();
    }

    public static class NatInstanceCommandBuilder {

        private General.Operations operation;
        private List<ExternalIpAddressPool> dynamicEntries;
        private Map<Long, List<MappingEntryBuilder>> staticEntries;

        public NatInstanceCommandBuilder setOperation(General.Operations operation) {
            this.operation = operation;
            return this;
        }

        public NatInstanceCommandBuilder setDynamicEntries(List<ExternalIpAddressPool> dynamicEntries) {
            this.dynamicEntries = dynamicEntries;
            return this;
        }

        public NatInstanceCommandBuilder setStaticEntries(Map<Long, List<MappingEntryBuilder>> staticEntries) {
            this.staticEntries = staticEntries;
            return this;
        }

        public NatInstanceCommandBuilder setStaticEntries(Long fibId, List<MappingEntryBuilder> staticEntries) {
            if (staticEntries == null) {
                this.staticEntries = new HashMap<Long, List<MappingEntryBuilder>>();
            }
            this.staticEntries.put(fibId, staticEntries);
            return this;
        }

        public General.Operations getOperation() {
            return operation;
        }

        public List<ExternalIpAddressPool> getDynamicEntries() {
            return dynamicEntries;
        }

        public Map<Long, List<MappingEntryBuilder>> getStaticEntries() {
            return staticEntries;
        }

        public List<MappingEntryBuilder> getStaticEntries(long fibId) {
            if (staticEntries == null) {
                return Collections.emptyList();
            }
            return staticEntries.get(Long.valueOf(fibId));
        }

        public NatInstanceCommand build() {
            Preconditions.checkNotNull(operation, "Operation of NAT command not specified.");
            if (staticEntries == null) {
                staticEntries = new HashMap<Long, List<MappingEntryBuilder>>();
            }
            if (dynamicEntries == null) {
                dynamicEntries = Lists.newArrayList();
            }
            return new NatInstanceCommand(this);
        }

        @Override
        public String toString() {
            return build().toString();
        }
    }
}
