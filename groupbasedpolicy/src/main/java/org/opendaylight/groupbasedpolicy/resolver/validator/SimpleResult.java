/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.resolver.validator;

/**
 * Class that represents simple result (e.g. SUCCESS/FAILURE and description of
 * that result). Provide option to save custom result (state/code), which can be
 * evaluated by other processes.
 *
 * @see SimpleResult#code
 *
 */
public class SimpleResult {

    /**
     * Code of result.
     */
    private final int code;
    /**
     * Description of result.
     */
    private final String description;

    /**
     * Construct new {@link SimpleResult}.
     *
     * @param result State of result ({@code true} on success and {@code false}
     * on failure).
     * @param description Description of result.
     */
    public SimpleResult(boolean result, String description) {
        this.code = result ? 0 : 1;
        this.description = description;
    }

    /**
     * Construct new {@link SimpleResult} with empty description.
     *
     * @param result State of result ({@code true} on SUCCESS and {@code false}
     * on FAILURE).
     */
    public SimpleResult(boolean result) {
        this(result, null);
    }

    /**
     * Construct new {@link SimpleResult}.
     *
     * @param code Code of result, where 0 is expected as SUCCESS and everything
     * else as FAILURE.
     * @param description Description of result.
     */
    public SimpleResult(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Construct new {@link SimpleResult} with empty description.
     *
     * @param code Code of result, where 0 is expected as SUCCESS and everything
     * else as FAILURE.
     */
    public SimpleResult(int code) {
        this(code, null);
    }

    /**
     * Returns code of result.
     *
     * @return Code of result
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns {@code true} if code of result IS {@code 0}, otherwise returns
     * {@code false}.
     *
     * @return {@code true} or {@code false} based on value of {@code code}
     */
    public boolean isSuccess() {
        return getCode() == 0;
    }

    /**
     * Returns {@code true} if code of result IS NOT {@code 0}, otherwise
     * returns {@code false}.
     *
     * @return {@code true} or {@code false} based on value of {@code code}
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Returns saved description of result or empty string.
     *
     * @return Description of result.
     */
    public String getDescription() {
        if (description == null) {
            return "";
        } else {
            return description;
        }
    }

}
