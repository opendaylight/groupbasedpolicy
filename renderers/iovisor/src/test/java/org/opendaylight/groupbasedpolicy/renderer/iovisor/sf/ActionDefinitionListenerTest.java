package org.opendaylight.groupbasedpolicy.renderer.iovisor.sf;

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
import org.opendaylight.groupbasedpolicy.api.sf.AllowActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.SubjectFeatureDefinitions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.groupbasedpolicy.policy.rev140421.subject.feature.definitions.ActionDefinitionKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ActionDefinitionListenerTest {

    private ActionDefinitionListener listener;
    private DataObjectModification<ActionDefinition> rootNode;
    private Set<DataTreeModification<ActionDefinition>> changes;

    private InstanceIdentifier<ActionDefinition> rootIdentifier;
    private DataBroker dataProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        dataProvider = mock(DataBroker.class);

        listener = spy(new ActionDefinitionListener(dataProvider));

        ActionDefinitionKey key = mock(ActionDefinitionKey.class);
        rootNode = mock(DataObjectModification.class);
        rootIdentifier =
                InstanceIdentifier.builder(SubjectFeatureDefinitions.class).child(ActionDefinition.class, key).build();
        DataTreeIdentifier<ActionDefinition> rootPath =
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, rootIdentifier);

        DataTreeModification<ActionDefinition> change = mock(DataTreeModification.class);

        when(change.getRootNode()).thenReturn(rootNode);
        when(change.getRootPath()).thenReturn(rootPath);

        changes = ImmutableSet.of(change);

        ActionDefinition def = new ActionDefinitionBuilder().setId(AllowActionDefinition.ID).build();

        when(rootNode.getDataBefore()).thenReturn(def);
        when(rootNode.getDataAfter()).thenReturn(def);
    }

    @Test
    public void testOnWrite() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(listener).onSubtreeModified(rootNode, rootIdentifier);
    }

    @Test
    public void testOnDelete() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(listener).onDelete(rootNode, rootIdentifier);
    }

    @Test
    public void testOnSubtreeModified() {
        when(rootNode.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        WriteTransaction wt = resetTransaction();

        listener.onDataTreeChanged(changes);

        verify(listener).onSubtreeModified(rootNode, rootIdentifier);
    }

    private WriteTransaction resetTransaction() {
        WriteTransaction wt = mock(WriteTransaction.class);
        CheckedFuture checkedFuture = mock(CheckedFuture.class);
        when(wt.submit()).thenReturn(checkedFuture);
        when(dataProvider.newWriteOnlyTransaction()).thenReturn(wt);
        return wt;
    }

}
