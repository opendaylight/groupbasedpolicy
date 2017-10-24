/*
 * Copyright (c) 2017 Cisco Systems. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp;

import java.util.Collections;

import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GbpGpeEntryDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.GpeEnableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.InterfaceDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.ItrRemoteLocatorSetDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LispDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocalMappingDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.LocatorSetDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapRegisterDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapResolverDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.MapServerDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.NativeForwardPathDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.VniTableDom;
import org.opendaylight.groupbasedpolicy.renderer.vpp.commands.lisp.dom.VrfSubtableDom;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.local.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.hmac.key.grouping.HmacKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServer;

public class LispCommandWrapper {
    public static AbstractLispCommand<Lisp> enableLisp() {
        LispDom lispDom = new LispDom();
        lispDom.setEnabled(true);
        return new ConfigureLispStatusCommand(lispDom);
    }

    public static AbstractLispCommand<LocatorSet> addLocatorSet(String locatorName, String interfaceName,
        short priority, short weight) {
        InterfaceDom interfaceDom = new InterfaceDom();
        interfaceDom.setInterfaceName(interfaceName);
        interfaceDom.setPriority(priority);
        interfaceDom.setWeight(weight);

        LocatorSetDom locatorSetDom = new LocatorSetDom();
        locatorSetDom.setInterfaces(Collections.singletonList(interfaceDom.getSALObject()));
        locatorSetDom.setLocatorName(locatorName);
        return new ConfigureLocatorSetCommand(locatorSetDom);
    }

    public static AbstractLispCommand<VniTable> mapVniToVrf(long vni, long vrfTableId) {
        VrfSubtableDom vrfSubtableDom = new VrfSubtableDom();
        vrfSubtableDom.setTableId(vrfTableId);

        VniTableDom vniTableDom = new VniTableDom();
        vniTableDom.setVirtualNetworkIdentifier(vni);
        vniTableDom.setVrfSubtable(vrfSubtableDom.getSALObject());

        return new ConfigureVrfToVniMappingCommand(vniTableDom);
    }

    public static AbstractLispCommand<MapRegister> enableMapRegister() {
        MapRegisterDom mapRegisterDom = new MapRegisterDom();
        mapRegisterDom.setEnabled(true);
        return new ConfigureMapRegisterStatusCommand(mapRegisterDom);
    }

    public static AbstractLispCommand<MapResolver> addMapResolver(IpAddress ipAddress) {
        MapResolverDom mapResolverDom = new MapResolverDom();
        mapResolverDom.setIpAddress(ipAddress);

        return new ConfigureMapResolverCommand(mapResolverDom);
    }

    public static AbstractLispCommand<MapServer> addMapServer(IpAddress ipAddress) {
        MapServerDom mapServerDom = new MapServerDom();
        mapServerDom.setIpAddress(ipAddress);

        return new ConfigureMapServerCommand(mapServerDom);
    }

    public static AbstractLispCommand<LocalMapping> addLocalMappingInEidTable(String mappingName, Eid eid,
        String locatorName, HmacKey hmacKey) {
        LocalMappingDom localMappingDom = new LocalMappingDom();
        localMappingDom.setMappingId(new MappingId(mappingName));
        localMappingDom.setEid(eid);
        localMappingDom.setLocatorName(locatorName);
        localMappingDom.setHmacKey(hmacKey);

        return new ConfigureLocalMappingInEidTableCommand(localMappingDom);
    }

    public static AbstractLispCommand<LocalMapping> deleteLocalMappingFromEidTable(String mappingName, long vni) {
        LocalMappingDom localMappingDom = new LocalMappingDom();
        localMappingDom.setMappingId(new MappingId(mappingName));
        localMappingDom.setVni(vni);

        return new ConfigureLocalMappingInEidTableCommand(localMappingDom);
    }

    public static AbstractLispCommand<LispFeatureData> deleteLispFeatureData() {
        return new DeleteLispFeatureDataCommand();
    }

    public static AbstractLispCommand<GpeFeatureData> enableGpe() {
        GpeEnableDom gpeEnableDom = new GpeEnableDom();
        gpeEnableDom.setEnabled(true);

        return new ConfigureGpeCommand(gpeEnableDom);
    }

    public static AbstractLispCommand<GpeEntry> addGpeSendMapregisterAction(String entryName, RemoteEid rEid, long vni,
        long vrf) {
        GbpGpeEntryDom gpeEntryDom = new GbpGpeEntryDom();
        gpeEntryDom.setId(entryName);
        gpeEntryDom.setRemoteEid(rEid);
        gpeEntryDom.setVni(vni);
        gpeEntryDom.setVrf(vrf);
        gpeEntryDom.setAction(MapReplyAction.SendMapRequest);

        return new ConfigureGpeEntryCommand(gpeEntryDom);
    }

    public static AbstractLispCommand<GpeEntry> deleteGpeEntry(String entryName) {
        GbpGpeEntryDom gpeEntryDom = new GbpGpeEntryDom();
        gpeEntryDom.setId(entryName);

        return new ConfigureGpeEntryCommand(gpeEntryDom);
    }

    public static AbstractLispCommand<ItrRemoteLocatorSet> addItrRloc(String locatorSetName) {
        ItrRemoteLocatorSetDom itrRemoteLocatorSetDom = new ItrRemoteLocatorSetDom();
        itrRemoteLocatorSetDom.setLocatorSetName(locatorSetName);

        return new ConfigureItrRemoteLocatorSetCommand(itrRemoteLocatorSetDom);
    }

    public static AbstractLispCommand<NativeForwardPath> addNativeForwardEntry(long vrf, IpAddress nextHopIp) {
        NativeForwardPathDom nativeForwardPathDom = new NativeForwardPathDom();
        nativeForwardPathDom.setVrfId(vrf);
        nativeForwardPathDom.setNextHopIp(nextHopIp);
        return new ConfigureNativeForwardPathCommand(nativeForwardPathDom);
    }

    public static AbstractLispCommand<NativeForwardPathsTables> deleteNativeForwardPathsTables() {
        return new DeleteNativeForwardPathsTablesDeleteCommand();
    }
}
