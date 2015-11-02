/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.flow;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.Extension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;

import java.util.Comparator;
import java.util.TreeSet;

public class FlowIdUtils {

    private static final String TABLE_ID_PREFIX = "t";
    private static final String FLOWID_SEPARATOR = "|";
    private static final String MATCH_PREFIX = "match[";
    private static final String MATCH_SUFFIX = "]";
    private static final String MATCH_SEPARATOR = ", ";

    // *** flow from FlowTable (abstract parent) ***

    /**
     * For flow without match specified (actually, only "drop all" flow)
     *
     * @param prefix String
     * @return FlowId
     */
    public static FlowId newFlowId(String prefix) {

        return new FlowId(prefix);
    }

    /**
     * FlowId based on match (with prefix like "t2|localL3|")
     *
     * @param tableId Short
     * @param prefix String
     * @param match Match
     * @return FlowId
     */
    public static FlowId newFlowId(Short tableId, String prefix, Match match) {

        return new FlowId((tableId != null ? TABLE_ID_PREFIX + tableId + FLOWID_SEPARATOR : "")
                + prefix + FLOWID_SEPARATOR
                + formatMatch(match));
    }

    private static String formatMatch(Match match) {
        if (match == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder(MATCH_PREFIX);
        boolean first = true;
        if (match.getEthernetMatch() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getEthernetMatch());
        }
        if (match.getIcmpv4Match() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getIcmpv4Match());
        }
        if (match.getIcmpv6Match() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getIcmpv6Match());
        }
        if (match.getInPhyPort() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append("inPhyPort=").append(match.getInPhyPort());
        }
        if (match.getInPort() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append("inPort=").append(match.getInPort());
        }
        if (match.getIpMatch() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getIpMatch());
        }
        if (match.getLayer3Match() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getLayer3Match());
        }
        if (match.getLayer4Match() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getLayer4Match());
        }
        if (match.getMetadata() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getMetadata());
        }
        if (match.getProtocolMatchFields() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getProtocolMatchFields());
        }
        if (match.getTcpFlagMatch() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getTcpFlagMatch());
        }
        if (match.getTunnel() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getTunnel());
        }
        if (match.getVlanMatch() != null) {
            if (first) {
                first = false;
            } else {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append(match.getVlanMatch());
        }

        // only one augmentation is used in Match at the moment;
        // if in the future there will be more of them, similar handling has to be implemented
        GeneralAugMatchNodesNodeTableFlow generalAug = match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        if(generalAug != null && generalAug.getExtensionList() != null) {
            TreeSet<String> extensionAugmentationStrings = new TreeSet<>(new Comparator<String>() {
                @Override
                public int compare(String a, String b) {
                    return Strings.nullToEmpty(a).compareTo(Strings.nullToEmpty(b));
                }
            });
            for (ExtensionList e : generalAug.getExtensionList()) {
                Extension ext = e.getExtension();
                // only one augmentation is used in Extension at the moment;
                // if in the future there will be more of them, similar handling has to be implemented,
                // probing augmentations one by one and adding their toString results to our TreeSet
                // (and every List<> in them needs to be cast to Set<> to avoid non-equivalence
                // due to different element order, and possible element duplication)
                NxAugMatchNodesNodeTableFlow nxAug = ext.getAugmentation(NxAugMatchNodesNodeTableFlow.class);
                if (nxAug != null) {
                    extensionAugmentationStrings.add(nxAug.toString());
                }
            }

            if (!first) {
                builder.append(MATCH_SEPARATOR);
            }
            builder.append("GeneralAugMatchNodesNodeTableFlow[<ExtensionList>=")
                    .append(Joiner.on(", ").skipNulls().join(extensionAugmentationStrings))
                    .append(']');
        }
        builder.append(MATCH_SUFFIX);

        return builder.toString();
    }

}
