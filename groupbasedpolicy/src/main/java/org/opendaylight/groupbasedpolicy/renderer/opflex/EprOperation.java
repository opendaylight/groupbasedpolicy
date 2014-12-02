/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex;

import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;

/**
 * Interface for managing operations with the Endpoint Registry.
 * An operation is the smallest granularity of interaction with
 * the Endpoint Registry. Put (create/update), Delete, and Read
 * operations are supported.
 *
 * @author thbachma
 *
 */
public interface EprOperation {
	/**
	 * Callback interface used to provide notifications
	 * to other objects.
	 *
	 * @author thbachma
	 *
	 */
    public interface EprOpCallback {
        public void callback(EprOperation op);
    }

    /**
     * Perform a PUT operation, which can be either a
     * creation or update of an element of the Endpoint
     * Registry
     *
     * @param wt
     */
	public void put(WriteTransaction wt);

	/**
	 * Perform a DELETE operation for the requested Endpoint.
	 *
	 * @param wt
	 */
	public void delete(WriteTransaction wt);

	/**
	 * Return the data associated with the requested Endpoint.
	 *
	 * @param rot
	 * @param executor
	 */
	public void read(ReadOnlyTransaction rot,
			ScheduledExecutorService executor);

	/**
	 * Set the callback notification
	 *
	 * @param callback
	 */
	public void setCallback(EprOpCallback callback);
}
