package org.opendaylight.groupbasedpolicy.neutron.mapper.mapping;

/**
 * HTTP status codes
 */
public final class StatusCode {

    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;

    private StatusCode() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }
}
