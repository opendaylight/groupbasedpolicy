package org.opendaylight.groupbasedpolicy.renderer.ofoverlay.sf;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.groupbasedpolicy.api.sf.EtherTypeClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ClassifierDefinitionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.renderer.rev151103.renderers.renderer.capabilities.SupportedClassifierDefinition;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ClassifierDefinitionListenerTest {

    private ClassifierDefinitionListener listener;
    private DataObjectModification<ClassifierDefinition> rootNode;
    private Set<DataTreeModification<ClassifierDefinition>> changes;

    private DataBroker dataProvider;

    private InstanceIdentifier<ClassifierDefinition> rootIdentifier;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {

        dataProvider = mock(DataBroker.class);

        listener = spy(new ClassifierDefinitionListener(dataProvider));

        ClassifierDefinitionKey key = mock(ClassifierDefinitionKey.class);

        rootNode = mock(DataObjectModification.class);
        rootIdentifier = InstanceIdentifier.builder(SubjectFeatureDefinitions.class)
                .child(ClassifierDefinition.class, key)
                .build();
        DataTreeIdentifier<ClassifierDefinition> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);

        DataTreeModification<ClassifierDefinition> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        ClassifierDefinition def =
                new ClassifierDefinitionBuilder().setId(EtherTypeClassifierDefinition.ID).build();

        when(rootNode.getDataBefore()).thenReturn(def);
        when(rootNode.getDataAfter()).thenReturn(def);
    }

    @Test
    public void testOnDataTreeChanged_Write() {
        when(rootNode.getModificationType()).thenReturn(
                DataObjectModification.ModificationType.WRITE);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(SupportedClassifierDefinition.class), eq(true));
    }

    @Test
    public void testOnDataTreeChanged_SubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(
                DataObjectModification.ModificationType.SUBTREE_MODIFIED);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt).put(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class),
                any(SupportedClassifierDefinition.class), eq(true));
    }

    @Test
    public void testOnDataTreeChanged_Delete() {
        when(rootNode.getModificationType()).thenReturn(
                DataObjectModification.ModificationType.DELETE);

        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(wt).delete(eq(LogicalDatastoreType.OPERATIONAL), any(InstanceIdentifier.class));
    }

    private WriteTransaction resetTransaction() {
        WriteTransaction wt = mock(WriteTransaction.class);
        CheckedFuture checkedFuture = mock(CheckedFuture.class);
        when(wt.submit()).thenReturn(checkedFuture);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wt);
        return wt;
    }

}
