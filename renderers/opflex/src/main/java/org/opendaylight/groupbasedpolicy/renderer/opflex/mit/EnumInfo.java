/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * An object that represents an enum in the MIT's
 * PropertyInfo object map
 *
 * @author tbachman
 */
public interface EnumInfo {

    /**
     * Get the name of the enum as it appears in the MIT's
     * PropertyInfo map.
     *
     * @return
     */
    public String getName();

    /**
     * Get the Integer representation of the value by name
     * for the enum
     *
     * @param name
     * @return
     */
    public BigInteger getEnumValue(String name);

    public String getEnumValue(BigInteger value);

    /**
     * Class for building immutable EnumInfo objects
     *
     * @author tbachman
     */
    public static class EnumInfoBuilder {

        private String name;
        private final Map<String, BigInteger> enumValuesByString = new HashMap<>();
        private final Map<BigInteger, String> enumValuesByInt = new HashMap<>();

        public EnumInfoBuilder() {
        }

        /**
         * Set the name of the EnumInfo object
         *
         * @param name
         * @return
         */
        public EnumInfoBuilder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add a name/value pair to the enum, where
         * value is an Integer object
         *
         * @param name
         * @param value
         * @return
         */
        public EnumInfoBuilder setEnumValue(String name, BigInteger value) {
            this.enumValuesByString.put(name, value);
            this.enumValuesByInt.put(value, name);
            return this;
        }

        public EnumInfo build() {
            return new EnumInfoImpl(this);
        }

        public static class EnumInfoImpl implements EnumInfo {

            private final String name;
            private final Map<String, BigInteger> enumValuesByString;
            private final Map<BigInteger, String> enumValuesByInt;

            public EnumInfoImpl(EnumInfoBuilder builder) {
                this.name = builder.name;
                this.enumValuesByString = builder.enumValuesByString;
                this.enumValuesByInt = builder.enumValuesByInt;
            }

            @Override
            public String getName() {
                return this.name;
            }

            @Override
            public BigInteger getEnumValue(String name) {
                return this.enumValuesByString.get(name);
            }

            @Override
            public String getEnumValue(BigInteger value) {
                return this.enumValuesByInt.get(value);
            }
        }
    }
}
