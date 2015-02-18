/*
 * Copyright (c) 2015 Juniper Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.oc;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * Invoked to format the UUID if UUID is not in correct format.
     *
     * @param String
     *            An instance of UUID string.
     *
     * @return Correctly formated UUID string.
     */
    public static String uuidFormater(String uuid) {
        String uuidPattern = null;
        String id1 = uuid.substring(0, 8);
        String id2 = uuid.substring(8, 12);
        String id3 = uuid.substring(12, 16);
        String id4 = uuid.substring(16, 20);
        String id5 = uuid.substring(20, 32);
        uuidPattern = (id1 + "-" + id2 + "-" + id3 + "-" + id4 + "-" + id5);
        return uuidPattern;
    }

    /**
     * Invoked to check the UUID if UUID is not a valid hexa-decimal number.
     *
     * @param String
     *            An instance of UUID string.
     *
     * @return boolean value.
     */
    public static boolean isValidHexNumber(String uuid) {
        try {
            Pattern hex = Pattern.compile("^[0-9a-f]+$");
            uuid = uuid.replaceAll("-", "");
            boolean valid = hex.matcher(uuid).matches();
            if (uuid.length() != 32) {
                return false;
            }
            if (valid) {
                return true;
            } else {
                return false;
            }
        } catch (NumberFormatException ex) {
            LOGGER.error("Exception :  " + ex);
            return false;
        }
    }

    /**
     * Invoked to format the UUID/Name in correct format.
     *
     * @param String
     *            An instance of UUID/Name string.
     *
     * @return Correctly formated UUID/name string.
     */

    public static String uuidNameFormat(String value){
    	String[] pattern = value.split("=");
    	value = pattern[1].replace("]", "");
    	return value;
    }
}