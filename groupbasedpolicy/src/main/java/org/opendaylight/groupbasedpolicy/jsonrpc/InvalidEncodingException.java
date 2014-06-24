/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.groupbasedpolicy.jsonrpc;

public class InvalidEncodingException extends RuntimeException {

    private static final long serialVersionUID = -2241512201890075052L;
    private final String actual;

    public InvalidEncodingException(String actual, String message) {
          super(message);
          this.actual = actual;
      }

    public String getActual() {
        return actual;
    }
}
