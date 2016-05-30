/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.RendererName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Maps;

public class RendererUtils {

    public static @Nonnull ImmutableMultimap<InstanceIdentifier<?>, RendererName> resolveRenderersByNodes(
            @Nullable List<Renderer> renderers) {
        if (renderers == null) {
            return ImmutableMultimap.of();
        }
        Builder<InstanceIdentifier<?>, RendererName> renderersByNodeBuilder = ImmutableMultimap.builder();
        for (Renderer renderer : renderers) {
            if (renderer.getRendererNodes() == null) {
                continue;
            }
            List<RendererNode> rendererNodes = renderer.getRendererNodes().getRendererNode();
            if (rendererNodes == null) {
                continue;
            }
            for (RendererNode rendererNode : rendererNodes) {
                if (rendererNode.getNodePath() != null) {
                    renderersByNodeBuilder.put(rendererNode.getNodePath(), renderer.getName());
                }
            }
        }
        return renderersByNodeBuilder.build();
    }

    public static @Nonnull ImmutableMap<RendererName, Renderer> resolveRendererByName(
            @Nullable List<Renderer> renderers) {
        if (renderers == null) {
            return ImmutableMap.of();
        }
        return Maps.uniqueIndex(renderers, Renderer::getName);
    }
}
