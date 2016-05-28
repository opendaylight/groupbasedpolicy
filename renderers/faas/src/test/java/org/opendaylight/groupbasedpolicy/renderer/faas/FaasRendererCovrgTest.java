package org.opendaylight.groupbasedpolicy.renderer.faas;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FaasRendererCovrgTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testConstructor() throws Exception {
        DataBroker dataProvider = mock(DataBroker.class);
        EpRendererAugmentationRegistry epRendererAugmentationRegistry = mock(EpRendererAugmentationRegistry.class);

        WriteTransaction wTx = mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> futureVoid = mock(CheckedFuture.class);
        when(wTx.submit()).thenReturn(futureVoid);
        doNothing().when(wTx).put(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(DataObject.class));
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wTx);

        FaasRenderer renderer = new FaasRenderer(dataProvider, epRendererAugmentationRegistry);
        renderer.close();
    }
}
