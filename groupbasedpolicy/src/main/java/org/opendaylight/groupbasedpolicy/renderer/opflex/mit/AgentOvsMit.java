/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.EnumInfo.EnumInfoBuilder;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyClassInfo.PolicyClassInfoBuilder;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyId;
import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyInfoBuilder;

/**
 * This class represents the schema used with the OpFlex Agent
 * reference design for Open vSwitch.
 *
 * @author tbachman
 *
 */
public class AgentOvsMit implements OpflexMit {

	private Map<String,PolicyClassInfo> metaDataMap = null;
	private Map<Long, String> classIdToStringMap = null;

	public AgentOvsMit() {
		Map<String,PolicyClassInfo> metaData = new HashMap<String, PolicyClassInfo>();
		Map<Long, String> classIdToString = new HashMap<Long, String>();

		/*
		 * Construct the MIT
		 */
		EnumInfo ei;
		PolicyPropertyInfo ppi;
		PolicyClassInfo pci;
		EnumInfoBuilder eib;
		PolicyPropertyInfoBuilder ppib;
		PolicyClassInfoBuilder pcib;
		List<PolicyPropertyInfo> ppil;
		List<PolicyPropertyId> classKeys;


        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516423l)).
             setPropName("RelatorUniverse").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(7l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516436l)).
             setPropName("GbpeTunnelEpUniverse").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(20l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516439l)).
             setPropName("DomainConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(23l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516467l)).
             setPropName("EpdrL2Discovered").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(51l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516468l)).
             setPropName("EpdrL3Discovered").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(52l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516471l)).
             setPropName("EprL2Universe").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(55l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516474l)).
             setPropName("EprL3Universe").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(58l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516518l)).
             setPropName("ObserverEpStatUniverse").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(102l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147516531l)).
             setPropName("PolicyUniverse").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(115l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(1).
             setClassName("DmtreeRoot").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713057l)).
             setPropName("DomainConfigToConfigRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(33l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713058l)).
             setPropName("GbpeEpgMappingCtxToEpgMappingRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(34l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713070l)).
             setPropName("SpanLocalEpToEpRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(46l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713083l)).
             setPropName("SpanMemberToRefRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(59l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713093l)).
             setPropName("GbpRuleToClassifierRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(69l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713094l)).
             setPropName("EpdrEndPointToGroupRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(70l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713105l)).
             setPropName("GbpSubnetsToNetworkRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(81l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713108l)).
             setPropName("GbpEpGroupToNetworkRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(84l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713113l)).
             setPropName("GbpEpGroupToProvContractRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(89l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713116l)).
             setPropName("GbpEpGroupToConsContractRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(92l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713120l)).
             setPropName("GbpBridgeDomainToNetworkRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(96l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147713124l)).
             setPropName("GbpFloodDomainToNetworkRRes").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(100l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(7).
             setClassName("RelatorUniverse").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("GbpeEncapTypeT");
        eib.setEnumValue("unknown",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("vlan",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("vxlan",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(294914l)).
             setPropName("encapType").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformSwitchingModeT");
        eib.setEnumValue("hairpin",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("intra_bd",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("intra_epg",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("intra_rd",new BigInteger(String.valueOf(4)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(294915l)).
             setPropName("mode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(294916l)).
             setPropName("multicastGroupIP").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(294913l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778571l)).
             setPropName("CdpConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(11l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778575l)).
             setPropName("DfwConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(15l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778582l)).
             setPropName("L2Config").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(22l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778584l)).
             setPropName("LacpConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(24l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778585l)).
             setPropName("LldpConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(25l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778586l)).
             setPropName("StpConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(26l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778592l)).
             setPropName("DomainConfigFromConfigRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(32l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778601l)).
             setPropName("SpanSrcGrp").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(41l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778602l)).
             setPropName("SpanDstGrp").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(42l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147778603l)).
             setPropName("SpanLocalEp").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(43l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(9).
             setClassName("PlatformConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(327682l)).
             setPropName("defaultGroup").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(327681l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147811359l)).
             setPropName("GbpeEpgMappingCtxFromEpgMappingRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(31l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(10).
             setClassName("GbpeEpgMapping").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(360449l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(360450l)).
             setPropName("state").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(11).
             setClassName("CdpConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(393217l)).
             setPropName("classid").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(393218l)).
             setPropName("encapId").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(393219l)).
             setPropName("multicastGroupIP").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(12).
             setClassName("GbpeInstContext").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2147909659l)).
             setPropName("GbpeEpgMappingCtxToEpgMappingRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(27l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(13).
             setClassName("GbpeEpgMappingCtx").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(491521l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(491522l)).
             setPropName("state").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(15).
             setClassName("DfwConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(524290l)).
             setPropName("rxDrop").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(524291l)).
             setPropName("txDrop").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(524289l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(16).
             setClassName("DfwEpCounter").
             setPolicyType(PolicyClassInfo.PolicyClassType.OBSERVABLE).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(557057l)).
             setPropName("multicastGroupIP").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(17).
             setClassName("GbpeFloodContext").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("ArpOpcodeT");
        eib.setEnumValue("reply",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("request",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("unspecified",new BigInteger(String.valueOf(0)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589829l)).
             setPropName("arpOpc").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("GbpConnTrackT");
        eib.setEnumValue("normal",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("reflexive",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589827l)).
             setPropName("connectionTracking").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589834l)).
             setPropName("dFromPort").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589835l)).
             setPropName("dToPort").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("GbpDirectionT");
        eib.setEnumValue("bidirectional",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("in",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("out",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589828l)).
             setPropName("direction").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("L2EtherTypeT");
        eib.setEnumValue("arp",new BigInteger(String.valueOf(0x0806)));
        eib.setEnumValue("fcoe",new BigInteger(String.valueOf(0x8906)));
        eib.setEnumValue("ipv4",new BigInteger(String.valueOf(0x0800)));
        eib.setEnumValue("ipv6",new BigInteger(String.valueOf(0x86DD)));
        eib.setEnumValue("mac_security",new BigInteger(String.valueOf(0x88E5)));
        eib.setEnumValue("mpls_ucast",new BigInteger(String.valueOf(0x8847)));
        eib.setEnumValue("trill",new BigInteger(String.valueOf(0x22F3)));
        eib.setEnumValue("unspecified",new BigInteger(String.valueOf(0)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589830l)).
             setPropName("etherT").
             setType(PolicyPropertyInfo.PropertyType.ENUM16).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589825l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589826l)).
             setPropName("order").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589831l)).
             setPropName("prot").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589832l)).
             setPropName("sFromPort").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(589833l)).
             setPropName("sToPort").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148073540l)).
             setPropName("GbpRuleFromClassifierRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(68l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(18).
             setClassName("GbpeL24Classifier").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622600l)).
             setPropName("rxBroadcast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622604l)).
             setPropName("rxBytes").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622596l)).
             setPropName("rxDrop").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622598l)).
             setPropName("rxMulticast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622594l)).
             setPropName("rxPackets").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622602l)).
             setPropName("rxUnicast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622601l)).
             setPropName("txBroadcast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622605l)).
             setPropName("txBytes").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622597l)).
             setPropName("txDrop").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622599l)).
             setPropName("txMulticast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622595l)).
             setPropName("txPackets").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622603l)).
             setPropName("txUnicast").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(622593l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(19).
             setClassName("GbpeEpCounter").
             setPolicyType(PolicyClassInfo.PolicyClassType.OBSERVABLE).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148139029l)).
             setPropName("GbpeTunnelEp").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(21l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(20).
             setClassName("GbpeTunnelEpUniverse").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(688133l)).
             setPropName("encapId").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("GbpeEncapTypeT");
        eib.setEnumValue("unknown",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("vlan",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("vxlan",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(688132l)).
             setPropName("encapType").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(688131l)).
             setPropName("mac").
             setType(PolicyPropertyInfo.PropertyType.MAC).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(688130l)).
             setPropName("terminatorIp").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(688129l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(21).
             setClassName("GbpeTunnelEp").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ENDPOINT).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(720897l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(720898l)).
             setPropName("state").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(22).
             setClassName("L2Config").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148237340l)).
             setPropName("DomainConfigToConfigRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(28l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(23).
             setClassName("DomainConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("LacpControlBitsT");
        eib.setEnumValue("fast-select-hot-standby",new BigInteger(String.valueOf(8)));
        eib.setEnumValue("graceful-convergence",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("load-defer",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("suspend-invididual-port",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(786437l)).
             setPropName("controlBits").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(786435l)).
             setPropName("maxLinks").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(786434l)).
             setPropName("minLinks").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("LacpModeT");
        eib.setEnumValue("active",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("mac-pin",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("passive",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(786436l)).
             setPropName("mode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(786433l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(24).
             setClassName("LacpConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(819201l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(819202l)).
             setPropName("rx").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(819203l)).
             setPropName("tx").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(25).
             setClassName("LldpConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(851971l)).
             setPropName("bpduFilter").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(851970l)).
             setPropName("bpduGuard").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(851969l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(26).
             setClassName("StpConfig").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(884739l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(27).
             setClassName("GbpeEpgMappingCtxToEpgMappingRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(917507l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(28).
             setClassName("DomainConfigToConfigRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1015810l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1015811l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1015809l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(31).
             setClassName("GbpeEpgMappingCtxFromEpgMappingRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1048578l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1048579l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1048577l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(32).
             setClassName("DomainConfigFromConfigRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1081346l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1081345l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(33).
             setClassName("DomainConfigToConfigRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1114114l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1114113l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(34).
             setClassName("GbpeEpgMappingCtxToEpgMappingRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1245188l)).
             setPropName("context").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1245187l)).
             setPropName("group").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1245186l)).
             setPropName("mac").
             setType(PolicyPropertyInfo.PropertyType.MAC).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1245185l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148728871l)).
             setPropName("EprL3Net").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(39l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148728877l)).
             setPropName("SpanLocalEpFromEpRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(45l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(38).
             setClassName("EprL2Ep").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ENDPOINT).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1277953l)).
             setPropName("ip").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(39).
             setClassName("EprL3Net").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ENDPOINT).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1343491l)).
             setPropName("label").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1343489l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("PlatformAdminStateT");
        eib.setEnumValue("off",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("on",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1343490l)).
             setPropName("state").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148827196l)).
             setPropName("SpanSrcMember").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(60l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148827197l)).
             setPropName("SpanDstMember").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(61l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(41).
             setClassName("SpanSrcGrp").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1376257l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148859964l)).
             setPropName("SpanSrcMember").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(60l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148859965l)).
             setPropName("SpanDstMember").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(61l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(42).
             setClassName("SpanDstGrp").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1409025l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1409026l)).
             setPropName("nic").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148892716l)).
             setPropName("SpanLocalEpToEpRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(44l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2148892722l)).
             setPropName("SpanMemberFromRefRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(50l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(43).
             setClassName("SpanLocalEp").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1441795l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(44).
             setClassName("SpanLocalEpToEpRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1474562l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1474563l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1474561l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(45).
             setClassName("SpanLocalEpFromEpRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1507330l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1507329l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(46).
             setClassName("SpanLocalEpToEpRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1540101l)).
             setPropName("context").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1540099l)).
             setPropName("group").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1540100l)).
             setPropName("ip").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1540098l)).
             setPropName("mac").
             setType(PolicyPropertyInfo.PropertyType.MAC).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1540097l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149023789l)).
             setPropName("SpanLocalEpFromEpRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(45l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(47).
             setClassName("EprL3Ep").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ENDPOINT).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1605635l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(49).
             setClassName("SpanMemberToRefRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1638402l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1638403l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1638401l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(50).
             setClassName("SpanMemberFromRefRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149154887l)).
             setPropName("EpdrLocalL2Ep").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(71l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(51).
             setClassName("EpdrL2Discovered").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149187656l)).
             setPropName("EpdrLocalL3Ep").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(72l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(52).
             setClassName("EpdrL3Discovered").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149285926l)).
             setPropName("EprL2Ep").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(38l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(55).
             setClassName("EprL2Universe").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1835011l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(56).
             setClassName("EpdrEndPointToGroupRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1867778l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1867779l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1867777l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(57).
             setClassName("EpdrEndPointFromGroupRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149384239l)).
             setPropName("EprL3Ep").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(47l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(58).
             setClassName("EprL3Universe").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1933314l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1933313l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(59).
             setClassName("SpanMemberToRefRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("SpanDirectionT");
        eib.setEnumValue("both",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("in",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("out",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1966082l)).
             setPropName("dir").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1966081l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149449777l)).
             setPropName("SpanMemberToRefRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(49l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(60).
             setClassName("SpanSrcMember").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(1998849l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149482545l)).
             setPropName("SpanMemberToRefRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(49l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149482558l)).
             setPropName("SpanDstSummary").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(62l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(61).
             setClassName("SpanDstMember").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031617l)).
             setPropName("dest").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031624l)).
             setPropName("dscp").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031620l)).
             setPropName("flowId").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("SpanErspanDestModeT");
        eib.setEnumValue("notVisible",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("visible",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031625l)).
             setPropName("mode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031623l)).
             setPropName("mtu").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031618l)).
             setPropName("srcPrefix").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031621l)).
             setPropName("ttl").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("SpanErspanVersionT");
        eib.setEnumValue("v1",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("v2",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031619l)).
             setPropName("version").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2031622l)).
             setPropName("vrfName").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(62).
             setClassName("SpanDstSummary").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2097153l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149580865l)).
             setPropName("GbpSubject").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(65l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149580886l)).
             setPropName("GbpEpGroupFromProvContractRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(86l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149580891l)).
             setPropName("GbpEpGroupFromConsContractRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(91l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(64).
             setClassName("GbpContract").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2129921l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149613634l)).
             setPropName("GbpRule").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(66l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(65).
             setClassName("GbpSubject").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2162689l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2162690l)).
             setPropName("order").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149646403l)).
             setPropName("GbpRuleToClassifierRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(67l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(66).
             setClassName("GbpRule").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2195459l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(67).
             setClassName("GbpRuleToClassifierRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2228226l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2228227l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2228225l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(68).
             setClassName("GbpRuleFromClassifierRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2260994l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2260993l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(69).
             setClassName("GbpRuleToClassifierRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2293762l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2293761l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(70).
             setClassName("EpdrEndPointToGroupRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2326530l)).
             setPropName("mac").
             setType(PolicyPropertyInfo.PropertyType.MAC).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2326529l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149810189l)).
             setPropName("GbpeEpgMappingCtx").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(13l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149810232l)).
             setPropName("EpdrEndPointToGroupRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(56l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(71).
             setClassName("EpdrLocalL2Ep").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2359299l)).
             setPropName("ip").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2359298l)).
             setPropName("mac").
             setType(PolicyPropertyInfo.PropertyType.MAC).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2359297l)).
             setPropName("uuid").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149842957l)).
             setPropName("GbpeEpgMappingCtx").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(13l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149843000l)).
             setPropName("EpdrEndPointToGroupRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(56l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(72).
             setClassName("EpdrLocalL3Ep").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2424834l)).
             setPropName("ipv6Autoconfig").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2424833l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149908492l)).
             setPropName("GbpeInstContext").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(12l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149908560l)).
             setPropName("GbpSubnetsFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(80l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149908563l)).
             setPropName("GbpEpGroupFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(83l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149908575l)).
             setPropName("GbpBridgeDomainFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(95l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(74).
             setClassName("GbpRoutingDomain").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457602l)).
             setPropName("address").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457606l)).
             setPropName("ipv6AdvAutonomousFlag").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457607l)).
             setPropName("ipv6AdvPreferredLifetime").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457605l)).
             setPropName("ipv6AdvValidLifetime").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457601l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457603l)).
             setPropName("prefixLen").
             setType(PolicyPropertyInfo.PropertyType.U64).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2457604l)).
             setPropName("virtualRouterIp").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(75).
             setClassName("GbpSubnet").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("GbpIntraGroupPolicyT");
        eib.setEnumValue("allow",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("require-contract",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2490370l)).
             setPropName("intraGroupPolicy").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2490369l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974028l)).
             setPropName("GbpeInstContext").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(12l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974066l)).
             setPropName("SpanMemberFromRefRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(50l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974073l)).
             setPropName("EpdrEndPointFromGroupRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(57l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974098l)).
             setPropName("GbpEpGroupToNetworkRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(82l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974101l)).
             setPropName("GbpEpGroupToProvContractRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(85l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2149974106l)).
             setPropName("GbpEpGroupToConsContractRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(90l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(76).
             setClassName("GbpEpGroup").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2555905l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150039627l)).
             setPropName("GbpSubnet").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(75l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150039631l)).
             setPropName("GbpSubnetsToNetworkRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(79l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150039635l)).
             setPropName("GbpEpGroupFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(83l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(78).
             setClassName("GbpSubnets").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2588675l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(79).
             setClassName("GbpSubnetsToNetworkRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2621442l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2621443l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2621441l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(80).
             setClassName("GbpSubnetsFromNetworkRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2654210l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2654209l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(81).
             setClassName("GbpSubnetsToNetworkRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2686979l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(82).
             setClassName("GbpEpGroupToNetworkRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2719746l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2719747l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2719745l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(83).
             setClassName("GbpEpGroupFromNetworkRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2752514l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2752513l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(84).
             setClassName("GbpEpGroupToNetworkRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2785283l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(85).
             setClassName("GbpEpGroupToProvContractRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2818050l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2818051l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2818049l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(86).
             setClassName("GbpEpGroupFromProvContractRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2850817l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150334476l)).
             setPropName("GbpeInstContext").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(12l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150334544l)).
             setPropName("GbpSubnetsFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(80l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150334547l)).
             setPropName("GbpEpGroupFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(83l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150334552l)).
             setPropName("GbpBridgeDomainToNetworkRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(88l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150334563l)).
             setPropName("GbpFloodDomainFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(99l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(87).
             setClassName("GbpBridgeDomain").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2883587l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(88).
             setClassName("GbpBridgeDomainToNetworkRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2916354l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2916353l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(89).
             setClassName("GbpEpGroupToProvContractRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2949123l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(90).
             setClassName("GbpEpGroupToConsContractRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2981890l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2981891l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2981889l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(91).
             setClassName("GbpEpGroupFromConsContractRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3014658l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3014657l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(92).
             setClassName("GbpEpGroupToConsContractRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3112962l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3112963l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3112961l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(95).
             setClassName("GbpBridgeDomainFromNetworkRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3145730l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3145729l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(96).
             setClassName("GbpBridgeDomainToNetworkRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("GbpAddressResModeT");
        eib.setEnumValue("drop",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("flood",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("unicast",new BigInteger(String.valueOf(0)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3178498l)).
             setPropName("arpMode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3178497l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("GbpAddressResModeT");
        eib.setEnumValue("drop",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("flood",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("unicast",new BigInteger(String.valueOf(0)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3178499l)).
             setPropName("neighborDiscMode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("GbpUnknownFloodModeT");
        eib.setEnumValue("drop",new BigInteger(String.valueOf(0)));
        eib.setEnumValue("flood",new BigInteger(String.valueOf(1)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3178500l)).
             setPropName("unknownFloodMode").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150662161l)).
             setPropName("GbpeFloodContext").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(17l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150662224l)).
             setPropName("GbpSubnetsFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(80l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150662227l)).
             setPropName("GbpEpGroupFromNetworkRTgt").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(83l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150662242l)).
             setPropName("GbpFloodDomainToNetworkRSrc").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(98l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(97).
             setClassName("GbpFloodDomain").
             setPolicyType(PolicyClassInfo.PolicyClassType.POLICY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3211267l)).
             setPropName("target").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(98).
             setClassName("GbpFloodDomainToNetworkRSrc").
             setPolicyType(PolicyClassInfo.PolicyClassType.RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3244034l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3244035l)).
             setPropName("source").
             setType(PolicyPropertyInfo.PropertyType.REFERENCE).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3244033l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(99).
             setClassName("GbpFloodDomainFromNetworkRTgt").
             setPolicyType(PolicyClassInfo.PolicyClassType.REVERSE_RELATIONSHIP).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        eib = new EnumInfoBuilder();
        eib.setName("RelatorRoleT");
        eib.setEnumValue("resolver",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("source",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("target",new BigInteger(String.valueOf(2)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3276802l)).
             setPropName("role").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        eib = new EnumInfoBuilder();
        eib.setName("RelatorTypeT");
        eib.setEnumValue("direct-association",new BigInteger(String.valueOf(1)));
        eib.setEnumValue("direct-dependency",new BigInteger(String.valueOf(3)));
        eib.setEnumValue("named-association",new BigInteger(String.valueOf(2)));
        eib.setEnumValue("named-dependency",new BigInteger(String.valueOf(4)));
        eib.setEnumValue("reference",new BigInteger(String.valueOf(8)));
        ei = eib.build();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3276801l)).
             setPropName("type").
             setType(PolicyPropertyInfo.PropertyType.ENUM8).
             setEnumInfo(ei).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(100).
             setClassName("GbpFloodDomainToNetworkRRes").
             setPolicyType(PolicyClassInfo.PolicyClassType.RESOLVER).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150826000l)).
             setPropName("DfwEpCounter").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(16l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2150826003l)).
             setPropName("GbpeEpCounter").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(19l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(102).
             setClassName("ObserverEpStatUniverse").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151251977l)).
             setPropName("PlatformConfig").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(9l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151251978l)).
             setPropName("GbpeEpgMapping").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(10l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151252084l)).
             setPropName("PolicySpace").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(116l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(115).
             setClassName("PolicyUniverse").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        ppil = new ArrayList<PolicyPropertyInfo>();
        classKeys = new ArrayList<PolicyPropertyId>();
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(3801089l)).
             setPropName("name").
             setType(PolicyPropertyInfo.PropertyType.STRING).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        ppi = ppib.build();
        ppil.add(ppi);
        classKeys.add(ppi.getPropId());
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284754l)).
             setPropName("GbpeL24Classifier").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(18l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284800l)).
             setPropName("GbpContract").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(64l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284810l)).
             setPropName("GbpRoutingDomain").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(74l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284812l)).
             setPropName("GbpEpGroup").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(76l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284814l)).
             setPropName("GbpSubnets").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(78l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284823l)).
             setPropName("GbpBridgeDomain").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(87l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        ppib = new PolicyPropertyInfoBuilder();
        ppib.setPropId(new PolicyPropertyId(2151284833l)).
             setPropName("GbpFloodDomain").
             setType(PolicyPropertyInfo.PropertyType.COMPOSITE).
             setClassId(97l).
             setPropCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        ppi = ppib.build();
        ppil.add(ppi);
        pcib = new PolicyClassInfoBuilder();
        pcib.setClassId(116).
             setClassName("PolicySpace").
             setPolicyType(PolicyClassInfo.PolicyClassType.LOCAL_ONLY).
             setProperty(ppil).
             setKey(classKeys);
        pci = pcib.build();
        metaData.put(pci.getClassName(), pci);
        classIdToString.put(Long.valueOf(pci.getClassId()), pci.getClassName());

        metaDataMap = Collections.unmodifiableMap(metaData);
        classIdToStringMap = Collections.unmodifiableMap(classIdToString);
	}

	@Override
	public PolicyClassInfo getClass(String name) {
		return metaDataMap.get(name);
	}

	@Override
	public PolicyClassInfo getClass(Long classId) {
		String className = classIdToStringMap.get(classId);
		if (className == null) return null;
		return metaDataMap.get(className);
	}

}
