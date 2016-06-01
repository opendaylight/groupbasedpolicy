package org.opendaylight.groupbasedpolicy.renderer.vpp.util;

import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.Renderer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.RendererNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.renderer.nodes.RendererNodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for {@link VppNodeWriter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class VppNodeWriterTest {

    private static final String RENDERER_NAME = "vpp-renderer";

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction wTx;
    @Captor
    private ArgumentCaptor<InstanceIdentifier<RendererNodes>> rendererNodesPathCpt;
    @Captor
    private ArgumentCaptor<RendererNodes> rendererNodesCpt;

    private InOrder inOrder;

    private VppNodeWriter nodeWriter;

    @Before
    public void setUp() throws Exception {
        nodeWriter = new VppNodeWriter();
        Mockito.when(dataBroker.newWriteOnlyTransaction()).thenReturn(wTx);
        Mockito.when(wTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
    }

    @After
    public void tearDown() throws Exception {
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testCommitToDatastore_with_node() throws Exception {
        final RendererNode node = new RendererNodeBuilder().build();
        nodeWriter.cache(node);
        nodeWriter.commitToDatastore(dataBroker);

        commonChecks();

        final RendererNodes rendererNodes = rendererNodesCpt.getValue();
        Assert.assertEquals(1, rendererNodes.getRendererNode().size());
    }

    @Test
    public void testCommitToDatastore_empty() throws Exception {
        nodeWriter.commitToDatastore(dataBroker);

        commonChecks();

        final RendererNodes rendererNodes = rendererNodesCpt.getValue();
        Assert.assertEquals(0, rendererNodes.getRendererNode().size());
    }

    private void commonChecks() {
        inOrder = Mockito.inOrder(dataBroker, wTx);
        inOrder.verify(dataBroker).newWriteOnlyTransaction();
        inOrder.verify(wTx).merge(Matchers.eq(LogicalDatastoreType.OPERATIONAL), rendererNodesPathCpt.capture(),
                rendererNodesCpt.capture(), Matchers.eq(true));
        inOrder.verify(wTx).submit();

        final InstanceIdentifier<RendererNodes> rendererNodesPath = rendererNodesPathCpt.getValue();
        Assert.assertEquals(RENDERER_NAME, extractRendererName(rendererNodesPath));
    }

    private String extractRendererName(final InstanceIdentifier<RendererNodes> rendererNodesPath) {
        return rendererNodesPath.firstKeyOf(Renderer.class).getName().getValue();
    }
}
