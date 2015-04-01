/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyId;

/**
 * Class that provides Managed Object metadata. The {@link PolicyClassInfo} object contains metadata
 * about
 * the Managed Object, while the {@link PropertyInfo} list
 * contained by this object provides the metadata about the
 * Managed Object properties.
 * This data must be kept consistent with the agents.
 *
 * @author tbachman
 */
public interface PolicyClassInfo {

    static public enum PolicyClassType {
        POLICY("policy"), REMOTE_ENDPOINT("remote_endpoint"), LOCAL_ENDPOINT("local_endpoint"), OBSERVABLE("observable"), LOCAL_ONLY(
                "local_only"), RESOLVER("resolver"), RELATIONSHIP("relationship"), REVERSE_RELATIONSHIP(
                "reverse_relationship");

        private final String type;

        PolicyClassType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    /**
     * Get the ID for the class
     *
     * @return
     */
    public int getClassId();

    /**
     * Get the type for the class
     *
     * @return
     */
    public PolicyClassType getPolicyType();

    /**
     * Get the name for the class
     *
     * @return
     */
    public String getClassName();

    /**
     * Get a {@link PolicyPropertyInfo} object by name
     * from the class.
     *
     * @param name
     * @return
     */
    public PolicyPropertyInfo getProperty(String name);

    /**
     * Return the entire list of {@link PolicyPropertyInfo} objects for this class.
     *
     * @return
     */
    public List<PolicyPropertyInfo> getProperties();

    /**
     * Get a list of {@link PolicyPropertyId} objects,
     * which are the keys to the {@link PolicyPropertyInfo} objects for this class.
     *
     * @return
     */
    public List<PolicyPropertyId> getKeys();

    /**
     * A builder class for create immutable instances of {@link PolicyClassInfo} objects.
     *
     * @author tbachman
     */
    public static class PolicyClassInfoBuilder {

        private int classId;
        private PolicyClassType policyType;
        private String className;
        private Map<String, PolicyPropertyInfo> properties;
        private List<PolicyPropertyId> keys;

        public PolicyClassInfoBuilder() {
            this.properties = new HashMap<String, PolicyPropertyInfo>();
            this.keys = new ArrayList<PolicyPropertyId>();
        }

        public int getClassId() {
            return classId;
        }

        public PolicyClassInfoBuilder setClassId(int classId) {
            this.classId = classId;
            return this;
        }

        public PolicyClassType getPolicyType() {
            return policyType;
        }

        public PolicyClassInfoBuilder setPolicyType(PolicyClassType policyType) {
            this.policyType = policyType;
            return this;
        }

        public String getClassName() {
            return className;
        }

        public PolicyClassInfoBuilder setClassName(String className) {
            this.className = className;
            return this;
        }

        public PolicyClassInfoBuilder setProperty(List<PolicyPropertyInfo> ppil) {
            for (PolicyPropertyInfo ppi : ppil) {
                this.properties.put(ppi.getPropName(), ppi);
            }
            return this;
        }

        public PolicyClassInfoBuilder setKey(List<PolicyPropertyId> pidl) {
            this.keys.addAll(pidl);
            return this;
        }

        public PolicyClassInfo build() {
            return new PolicyClassInfoImpl(this);
        }

        private static final class PolicyClassInfoImpl implements PolicyClassInfo {

            private final int classId;
            private final PolicyClassType policyType;
            private final String className;
            private final Map<String, PolicyPropertyInfo> properties;
            private List<PolicyPropertyId> keys;

            private PolicyClassInfoImpl(PolicyClassInfoBuilder builder) {
                this.classId = builder.classId;
                this.policyType = builder.policyType;
                this.className = builder.className;
                this.properties = builder.properties;
                this.keys = builder.keys;
            }

            @Override
            public int getClassId() {
                return classId;
            }

            @Override
            public PolicyClassType getPolicyType() {
                return policyType;
            }

            @Override
            public String getClassName() {
                return className;
            }

            @Override
            public PolicyPropertyInfo getProperty(String name) {
                return properties.get(name);
            }

            @Override
            public List<PolicyPropertyInfo> getProperties() {
                return new ArrayList<PolicyPropertyInfo>(properties.values());
            }

            @Override
            public List<PolicyPropertyId> getKeys() {
                return keys;
            }
        }
    }

}
