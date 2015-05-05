/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.equivalence;

import com.google.common.base.Equivalence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Custom Equivalence for Match
 *
 * @see FlowEquivalence
 */
public class MatchEquivalence extends Equivalence<Match> {

    MatchEquivalence() {
    }

    @Override
    protected boolean doEquivalent(Match a, Match b) {

        if (!Objects.equals(a.getEthernetMatch(), b.getEthernetMatch())) {
            return false;
        }
        if (!Objects.equals(a.getIcmpv4Match(), b.getIcmpv4Match())) {
            return false;
        }
        if (!Objects.equals(a.getIcmpv6Match(), b.getIcmpv6Match())) {
            return false;
        }
        if (!Objects.equals(a.getInPhyPort(), b.getInPhyPort())) {
            return false;
        }
        if (!Objects.equals(a.getInPort(), b.getInPort())) {
            return false;
        }
        if (!Objects.equals(a.getIpMatch(), b.getIpMatch())) {
            return false;
        }
        if (!Objects.equals(a.getLayer3Match(), b.getLayer3Match())) {
            return false;
        }
        if (!Objects.equals(a.getLayer4Match(), b.getLayer4Match())) {
            return false;
        }
        if (!Objects.equals(a.getMetadata(), b.getMetadata())) {
            return false;
        }
        if (!Objects.equals(a.getProtocolMatchFields(), b.getProtocolMatchFields())) {
            return false;
        }
        if (!Objects.equals(a.getTcpFlagMatch(), b.getTcpFlagMatch())) {
            return false;
        }
        if (!Objects.equals(a.getTunnel(), b.getTunnel())) {
            return false;
        }
        if (!Objects.equals(a.getVlanMatch(), b.getVlanMatch())) {
            return false;
        }
        GeneralAugMatchNodesNodeTableFlow generalAugMatchA =
                a.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        GeneralAugMatchNodesNodeTableFlow generalAugMatchB =
                b.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);

        if (generalAugMatchA != null && generalAugMatchB != null) {
            // if both have GeneralAugMatchNodesNodeTableFlow augmentation
            // create sets of ExtentionList type (not a List/Collection at all, as of yet)
            Set<ExtensionList> setA = new HashSet<>();
            Set<ExtensionList> setB = new HashSet<>();
            if (generalAugMatchA.getExtensionList() != null) {
                setA = new HashSet<>(generalAugMatchA.getExtensionList());
            }
            if (generalAugMatchB.getExtensionList() != null) {
                setB = new HashSet<>(generalAugMatchB.getExtensionList());
            }
            if (!setA.equals(setB)) {
                return false;
            }

        } else if ((generalAugMatchA == null && generalAugMatchB != null)
                || generalAugMatchA != null) {
            // if only one has GeneralAugMatchNodesNodeTableFlow augmentation, they are not equal
            return false;
        } // if no-one has GeneralAugMatchNodesNodeTableFlow augmentation, continue matching

        return true;
    }

    @Override
    protected int doHash(Match m) {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m.getEthernetMatch() == null) ? 0 : m.getEthernetMatch().hashCode());
        result = prime * result + ((m.getIcmpv4Match() == null) ? 0 : m.getIcmpv4Match().hashCode());
        result = prime * result + ((m.getIcmpv6Match() == null) ? 0 : m.getIcmpv6Match().hashCode());
        result = prime * result + ((m.getInPhyPort() == null) ? 0 : m.getInPhyPort().hashCode());
        result = prime * result + ((m.getInPort() == null) ? 0 : m.getInPort().hashCode());
        result = prime * result + ((m.getIpMatch() == null) ? 0 : m.getIpMatch().hashCode());
        result = prime * result + ((m.getLayer3Match() == null) ? 0 : m.getLayer3Match().hashCode());
        result = prime * result + ((m.getLayer4Match() == null) ? 0 : m.getLayer4Match().hashCode());
        result = prime * result + ((m.getMetadata() == null) ? 0 : m.getMetadata().hashCode());
        result = prime * result + ((m.getProtocolMatchFields() == null) ? 0 : m.getProtocolMatchFields().hashCode());
        result = prime * result + ((m.getTcpFlagMatch() == null) ? 0 : m.getTcpFlagMatch().hashCode());
        result = prime * result + ((m.getTunnel() == null) ? 0 : m.getTunnel().hashCode());
        result = prime * result + ((m.getVlanMatch() == null) ? 0 : m.getVlanMatch().hashCode());

        GeneralAugMatchNodesNodeTableFlow generalAugMatch =
                m.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        if (generalAugMatch != null) {
            List<ExtensionList> augMatchExtensionList = generalAugMatch.getExtensionList();
            Set<ExtensionList> extensionListSet = new HashSet<>();
            if (augMatchExtensionList != null) {
                extensionListSet =
                        new HashSet<>(augMatchExtensionList);
            }
            result = prime * result + extensionListSet.hashCode();
        }
        return result;
    }
}
