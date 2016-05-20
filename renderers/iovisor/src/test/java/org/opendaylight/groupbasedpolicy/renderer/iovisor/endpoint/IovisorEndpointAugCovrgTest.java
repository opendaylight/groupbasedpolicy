package org.opendaylight.groupbasedpolicy.renderer.iovisor.endpoint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.groupbasedpolicy.api.EpRendererAugmentationRegistry;
import org.opendaylight.groupbasedpolicy.renderer.iovisor.test.GbpIovisorDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.endpoint.rev140421.RegisterEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.iovisor.rev151030.IovisorModuleAugmentationInput;

public class IovisorEndpointAugCovrgTest extends GbpIovisorDataBrokerTest {

    private EpRendererAugmentationRegistry epRAR;
    private IovisorEndpointAug aug;

    @Before
    public void init() {
        epRAR = mock(EpRendererAugmentationRegistry.class);

        aug = new IovisorEndpointAug(epRAR);
    }

    @Test
    public void testClose() throws Exception {
        IovisorEndpointAug other = new IovisorEndpointAug(epRAR);
        other.close();

        verify(epRAR).unregister(any(IovisorEndpointAug.class));
    }

    @Test
    public void testBuildEndpointAugmentation() {
        assertNull(aug.buildEndpointAugmentation(null));
    }

    @Test
    public void testBuildEndpointL3Augmentation() {
        RegisterEndpointInput input = mock(RegisterEndpointInput.class);
        IovisorModuleAugmentationInput iomAugInput = mock(IovisorModuleAugmentationInput.class);
        when(input.getAugmentation(IovisorModuleAugmentationInput.class)).thenReturn(iomAugInput);
        assertNotNull(aug.buildEndpointL3Augmentation(input));
    }

    @Test
    public void testBuildEndpointL3Augmentation_Null() {
        RegisterEndpointInput input = mock(RegisterEndpointInput.class);
        when(input.getAugmentation(IovisorModuleAugmentationInput.class)).thenReturn(null);
        assertNull(aug.buildEndpointL3Augmentation(input));
    }

    @Test
    public void testBuildL3PrefixEndpointAugmentation() {
        assertNull(aug.buildL3PrefixEndpointAugmentation(null));
    }

}
