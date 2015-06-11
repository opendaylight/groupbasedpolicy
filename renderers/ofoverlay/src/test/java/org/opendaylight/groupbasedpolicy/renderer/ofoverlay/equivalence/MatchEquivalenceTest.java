/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Tunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;

public class MatchEquivalenceTest {

    private MatchEquivalence equivalence;

    private Match matchA;
    private Match matchB;

    private EthernetMatch ethernetMatch;
    private Icmpv4Match icmpv4Match;
    private Icmpv6Match icmpv6Match;
    private NodeConnectorId inPhyPort;
    private NodeConnectorId inPort;
    private IpMatch ipMatch;
    private Layer3Match layer3Match;
    private Layer4Match layer4Match;
    private Metadata metadata;
    private ProtocolMatchFields protocolMatchFields;
    private TcpFlagMatch tcpFlagMatch;
    private Tunnel tunnel;
    private VlanMatch vlanMatch;

    private GeneralAugMatchNodesNodeTableFlow generalAugMatchA;
    private GeneralAugMatchNodesNodeTableFlow generalAugMatchB;
    private ExtensionList extensionListA;
    private ExtensionList extensionListB;
    private List<ExtensionList> setA;
    private List<ExtensionList> setB;

    @Before
    public void initialise() {
        equivalence = new MatchEquivalence();

        matchA = mock(Match.class);
        matchB = mock(Match.class);

        ethernetMatch = mock(EthernetMatch.class);
        when(matchA.getEthernetMatch()).thenReturn(ethernetMatch);
        icmpv4Match = mock(Icmpv4Match.class);
        when(matchA.getIcmpv4Match()).thenReturn(icmpv4Match);
        icmpv6Match = mock(Icmpv6Match.class);
        when(matchA.getIcmpv6Match()).thenReturn(icmpv6Match);
        inPhyPort = mock(NodeConnectorId.class);
        when(matchA.getInPhyPort()).thenReturn(inPhyPort);
        inPort = mock(NodeConnectorId.class);
        when(matchA.getInPort()).thenReturn(inPort);
        ipMatch = mock(IpMatch.class);
        when(matchA.getIpMatch()).thenReturn(ipMatch);
        layer3Match = mock(Layer3Match.class);
        when(matchA.getLayer3Match()).thenReturn(layer3Match);
        layer4Match = mock(Layer4Match.class);
        when(matchA.getLayer4Match()).thenReturn(layer4Match);
        metadata = mock(Metadata.class);
        when(matchA.getMetadata()).thenReturn(metadata);
        protocolMatchFields = mock(ProtocolMatchFields.class);
        when(matchA.getProtocolMatchFields()).thenReturn(protocolMatchFields);
        tcpFlagMatch = mock(TcpFlagMatch.class);
        when(matchA.getTcpFlagMatch()).thenReturn(tcpFlagMatch);
        tunnel = mock(Tunnel.class);
        when(matchA.getTunnel()).thenReturn(tunnel);
        vlanMatch = mock(VlanMatch.class);
        when(matchA.getVlanMatch()).thenReturn(vlanMatch);

        generalAugMatchA = mock(GeneralAugMatchNodesNodeTableFlow.class);
        when(matchA.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(generalAugMatchA);
        generalAugMatchB = mock(GeneralAugMatchNodesNodeTableFlow.class);
        extensionListA = mock(ExtensionList.class);
        extensionListB = mock(ExtensionList.class);
        setA = Arrays.asList(extensionListA);
        setB = Arrays.asList(extensionListB);
        when(generalAugMatchA.getExtensionList()).thenReturn(setA);
    }

    @Test
    public void doEquivalentdoHashTest() {
        Assert.assertTrue(equivalence.doEquivalent(matchA, matchA));
        Assert.assertEquals(equivalence.doHash(matchA), equivalence.doHash(matchA));

        when(matchB.getEthernetMatch()).thenReturn(mock(EthernetMatch.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getEthernetMatch()).thenReturn(ethernetMatch);
        when(matchB.getIcmpv4Match()).thenReturn(mock(Icmpv4Match.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getIcmpv4Match()).thenReturn(icmpv4Match);
        when(matchB.getIcmpv6Match()).thenReturn(mock(Icmpv6Match.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getIcmpv6Match()).thenReturn(icmpv6Match);
        when(matchB.getInPhyPort()).thenReturn(mock(NodeConnectorId.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getInPhyPort()).thenReturn(inPhyPort);
        when(matchB.getInPort()).thenReturn(mock(NodeConnectorId.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getInPort()).thenReturn(inPort);
        when(matchB.getIpMatch()).thenReturn(mock(IpMatch.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getIpMatch()).thenReturn(ipMatch);
        when(matchB.getLayer3Match()).thenReturn(mock(Layer3Match.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getLayer3Match()).thenReturn(layer3Match);
        when(matchB.getLayer4Match()).thenReturn(mock(Layer4Match.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getLayer4Match()).thenReturn(layer4Match);
        when(matchB.getMetadata()).thenReturn(mock(Metadata.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getMetadata()).thenReturn(metadata);
        when(matchB.getProtocolMatchFields()).thenReturn(mock(ProtocolMatchFields.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getProtocolMatchFields()).thenReturn(protocolMatchFields);
        when(matchB.getTcpFlagMatch()).thenReturn(mock(TcpFlagMatch.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getTcpFlagMatch()).thenReturn(tcpFlagMatch);
        when(matchB.getTunnel()).thenReturn(mock(Tunnel.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getTunnel()).thenReturn(tunnel);
        when(matchB.getVlanMatch()).thenReturn(mock(VlanMatch.class));
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getVlanMatch()).thenReturn(vlanMatch);
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(generalAugMatchB.getExtensionList()).thenReturn(setB);
        when(matchB.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(generalAugMatchB);
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchA.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(null);
        Assert.assertFalse(equivalence.doEquivalent(matchA, matchB));
        Assert.assertNotEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchB.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(null);
        Assert.assertTrue(equivalence.doEquivalent(matchA, matchB));
        Assert.assertEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));

        when(matchA.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(generalAugMatchA);
        when(matchB.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class)).thenReturn(generalAugMatchB);
        when(generalAugMatchB.getExtensionList()).thenReturn(setA);
        Assert.assertTrue(equivalence.doEquivalent(matchA, matchB));
        Assert.assertEquals(equivalence.doHash(matchA), equivalence.doHash(matchB));
    }
}
