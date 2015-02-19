/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;

/**
 * Interface for interacting with the MIT, regardless of the
 * nature of the actual MIT.
 *
 * @author tbachman
 *
 */
public interface OpflexMit {

	/**
	 * Get a ClassInfo object from the MIT by name
	 *
	 * @param name
	 * @return
	 */
	public PolicyClassInfo getClass(String name);

	/**
	 * Get a ClassInfo object from the MIT by class ID
	 * @param classId
	 * @return
	 */
	public PolicyClassInfo getClass(Long classId);



}
