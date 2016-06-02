/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.vpp.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public abstract class DtoChangeEvent<T extends DataObject> {

    protected final InstanceIdentifier<T> iid;
    protected final Optional<T> before;
    protected final Optional<T> after;

    public static enum DtoModificationType {
        CREATED, UPDATED, DELETED;
    }

    public DtoChangeEvent(@Nonnull InstanceIdentifier<T> iid, @Nullable T before, @Nullable T after) {
        this.iid = Preconditions.checkNotNull(iid);
        Preconditions.checkArgument(!(before == null && after == null),
                "before and after cannot be null at the same time");
        this.before = Optional.fromNullable(before);
        this.after = Optional.fromNullable(after);
    }

    public @Nonnull InstanceIdentifier<T> getIid() {
        return iid;
    }

    public Optional<T> getBefore() {
        return before;
    }

    public Optional<T> getAfter() {
        return after;
    }

    /**
     * Returns:<br>
     * {@link DtoModificationType#CREATED} - when {@link #isDtoCreated()} is {@code true}<br>
     * {@link DtoModificationType#UPDATED} - when {@link #isDtoUpdated()} is {@code true}<br>
     * {@link DtoModificationType#DELETED} - when {@link #isDtoDeleted()} is {@code true}
     * 
     * @return DtoModificationType
     */
    public @Nonnull DtoModificationType getDtoModificationType() {
        if (isDtoCreated()) {
            return DtoModificationType.CREATED;
        }
        if (isDtoUpdated()) {
            return DtoModificationType.UPDATED;
        }
        if (isDtoDeleted()) {
            return DtoModificationType.DELETED;
        }
        throw new IllegalStateException("Unknown DTO modification type.");
    }

    /**
     * Checks if {@link #getBefore()} is NOT present and if {@link #getAfter()} is present
     * 
     * @return {@code true} if DTO is created; {@code false} otherwise
     */
    public boolean isDtoCreated() {
        return !before.isPresent() && after.isPresent();
    }

    /**
     * Checks if {@link #getBefore()} is present and if {@link #getAfter()} is present
     * 
     * @return {@code true} if DTO is updated; {@code false} otherwise
     */
    public boolean isDtoUpdated() {
        return before.isPresent() && after.isPresent();
    }

    /**
     * Checks if {@link #getBefore()} is present and if {@link #getAfter()} is NOT present
     * 
     * @return {@code true} if DTO is deleted; {@code false} otherwise
     */
    public boolean isDtoDeleted() {
        return before.isPresent() && !after.isPresent();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((after == null) ? 0 : after.hashCode());
        result = prime * result + ((before == null) ? 0 : before.hashCode());
        result = prime * result + ((iid == null) ? 0 : iid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof DtoChangeEvent))
            return false;
        DtoChangeEvent other = (DtoChangeEvent) obj;
        if (after == null) {
            if (other.after != null)
                return false;
        } else if (!after.equals(other.after))
            return false;
        if (before == null) {
            if (other.before != null)
                return false;
        } else if (!before.equals(other.before))
            return false;
        if (iid == null) {
            if (other.iid != null)
                return false;
        } else if (!iid.equals(other.iid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DtoChangeEvent [iid=" + iid + ", before=" + before + ", after=" + after + "]";
    }

}
