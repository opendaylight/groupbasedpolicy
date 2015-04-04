/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.groupbasedpolicy.renderer.opflex.mit.PolicyPropertyInfo.PolicyPropertyId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

/**
 * This is a policy object instance, used to provide an abstraction
 * between OpFlex messaging and the back-end policy.
 *
 * @author tbachman
 */
public class PolicyObjectInstance {

    /**
     * An object used as a key for the property hash map. It
     * consists of the tuple: { {@link PolicyPropertyId}, {@link PolicyPropertyInfo.PropertyType},
     * {@link PolicyPropertyInfo.PropertyCardinality}
     *
     * @author tbachman
     */
    public static class PropertyKey {

        private final PolicyPropertyId propId;
        private final PolicyPropertyInfo.PropertyType type;
        private final PolicyPropertyInfo.PropertyCardinality cardinality;

        public PropertyKey(PolicyPropertyId propId, PolicyPropertyInfo.PropertyType type,
                PolicyPropertyInfo.PropertyCardinality cardinality) {
            this.propId = propId;
            this.type = type;
            this.cardinality = cardinality;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((cardinality == null) ? 0 : cardinality.hashCode());
            result = prime * result + ((propId == null) ? 0 : propId.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PropertyKey other = (PropertyKey) obj;
            if (cardinality != other.cardinality)
                return false;
            if (propId == null) {
                if (other.propId != null)
                    return false;
            } else if (!propId.equals(other.propId))
                return false;
            if (type != other.type)
                return false;
            return true;
        }

    }

    /**
     * Class that contains a value held by the {@link PolicyObjectInstance}.
     * The value can be scalar or vector in nature.
     *
     * @author tbachman
     */
    public static class Value {

        private Object v;
        private List<Object> vl;
        private PolicyPropertyInfo.PropertyType type;
        private PolicyPropertyInfo.PropertyCardinality cardinality;

        public void setPropertyType(PolicyPropertyInfo.PropertyType type) {
            this.type = type;
        }

        public void setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality cardinality) {
            this.cardinality = cardinality;
        }

        public void setValue(Object v) {
            this.v = v;
        }

        public void setValue(List<Object> vl) {
            this.vl = vl;
        }

        public Object getValue(int index) {
            if (this.cardinality == PolicyPropertyInfo.PropertyCardinality.VECTOR) {
                return this.vl.get(index);
            } else {
                return this.v;
            }
        }

        public PolicyPropertyInfo.PropertyType getType() {
            return this.type;
        }
    }

    /**
     * Class used as a reference to another PolicyObject.
     *
     * @author tbachman
     */
    public static class PolicyReference {

        private final Uri uri;
        private final long classId;

        public PolicyReference(long classId, Uri uri) {
            this.uri = uri;
            this.classId = classId;
        }

        public Uri getUri() {
            return this.uri;
        }

        public long getClassId() {
            return this.classId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (classId ^ (classId >>> 32));
            result = prime * result + ((uri == null) ? 0 : uri.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PolicyReference other = (PolicyReference) obj;
            if (classId != other.classId)
                return false;
            if (uri == null) {
                if (other.uri != null)
                    return false;
            } else if (!uri.equals(other.uri))
                return false;
            return true;
        }

    }

    private final long classId;
    private Uri uri;
    private Uri parentUri;
    private String parentSubject;
    private String parentRelation;
    private final List<Uri> children = new ArrayList<>();
    private final Map<PropertyKey, Value> propertyMap = new HashMap<>();

    private PolicyPropertyInfo.PropertyType normalize(PolicyPropertyInfo.PropertyType type) {
        switch (type) {
            case ENUM8:
            case ENUM16:
            case ENUM32:
            case ENUM64:
                return PolicyPropertyInfo.PropertyType.U64;
            default:
                return type;
        }
    }

    public PolicyObjectInstance(long classId) {
        this.classId = classId;
    }

    /**
     * Get the class ID for this object instance.
     *
     * @return the class ID
     */
    public long getClassId() {
        return this.classId;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public void addChild(Uri childUri) {
        children.add(childUri);
    }

    public List<Uri> getChildren() {
        return this.children;
    }

    public void setParent(Uri uri) {
        this.parentUri = uri;
    }

    public Uri getParent() {
        return this.parentUri;
    }

    public void setParentSubject(String subject) {
        this.parentSubject = subject;
    }

    public String getParentSubject() {
        return this.parentSubject;
    }

    public void setParentRelation(String relation) {
        this.parentRelation = relation;
    }

    public String getParentRelation() {
        return this.parentRelation;
    }

    /**
     * Check whether the given property is set. If the property is
     * vector-valued, this will return false if the vector is zero length
     *
     * @param propId
     * @param type
     * @param cardinality
     * @return true if set, false if not set or zero length vector
     */
    public boolean isSet(PolicyPropertyInfo.PolicyPropertyId propId, PolicyPropertyInfo.PropertyType type,
            PolicyPropertyInfo.PropertyCardinality cardinality) {
        type = normalize(type);
        PropertyKey key = new PropertyKey(propId, type, cardinality);
        return propertyMap.containsKey(key);
    }

    /**
     * Unset the given property. If it's a vector, the vector is
     * emptied. If its a scalar, the scalar is unset.
     *
     * @param propId
     * @param type
     * @param cardinality
     * @return true if the property was alread set before
     */
    public boolean unset(PolicyPropertyInfo.PolicyPropertyId propId, PolicyPropertyInfo.PropertyType type,
            PolicyPropertyInfo.PropertyCardinality cardinality) {
        type = normalize(type);
        PropertyKey key = new PropertyKey(propId, type, cardinality);
        Value v = propertyMap.remove(key);
        if (v == null)
            return false;
        return true;
    }

    // getters

    /**
     * Get the unsigned 64-bit valued property for prop_name.
     *
     * @param id
     * @return null if not present or {@link BigInteger}
     */
    public BigInteger getUint64(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        return (BigInteger) propertyMap.get(key).getValue(0);
    }

    /**
     * For a vector-valued 64-bit unsigned property, get the specified
     * property value at the specified index
     *
     * @param id
     * @param index
     * @return the property value
     */
    public BigInteger getUint64(PolicyPropertyInfo.PolicyPropertyId id, int index) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        return (BigInteger) propertyMap.get(key).getValue(index);
    }

    /**
     * Get the number of unsigned 64-bit values for the specified
     * property
     *
     * @param id
     * @return the number of elements
     */
    public int getUint64Size(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        return v.vl.size();
    }

    /**
     * Get the signed 64-bit valued property for prop_name.
     *
     * @param id
     * @return the property value
     */
    public long getInt64(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        return (long) propertyMap.get(key).getValue(0);
    }

    /**
     * For a vector-valued 64-bit signed property, get the specified
     * property value at the specified index
     *
     * @param id
     * @param index
     * @return the property value
     */
    public long getInt64(PolicyPropertyInfo.PolicyPropertyId id, int index) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        return (long) propertyMap.get(key).getValue(index);
    }

    /**
     * Get the number of signed 64-bit values for the specified
     * property
     *
     * @param id
     * @return the number of elements
     */
    public int getInt64Size(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        return v.vl.size();
    }

    /**
     * Get the string-valued property for prop_name.
     *
     * @param id
     * @return the property value
     */
    public String getString(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        return (String) propertyMap.get(key).getValue(0);
    }

    /**
     * For a vector-valued string property, get the specified property
     * value at the specified index
     *
     * @param id
     * @param index
     * @return the property value
     */
    public String getString(PolicyPropertyInfo.PolicyPropertyId id, int index) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        return (String) propertyMap.get(key).getValue(index);
    }

    /**
     * Get the number of string values for the specified property
     *
     * @param id
     * @return the number of elements
     */
    public int getStringSize(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        return v.vl.size();
    }

    /**
     * Get the reference-valued property for prop_name.
     * 
     * @param id
     * @return the property value
     */
    public PolicyReference getReference(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        return (PolicyReference) propertyMap.get(key).getValue(0);
    }

    /**
     * For a vector-valued reference property, get the specified property
     * value at the specified index
     *
     * @param id
     * @param index
     * @return the property value
     */
    public PolicyReference getReference(PolicyPropertyInfo.PolicyPropertyId id, int index) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        return (PolicyReference) propertyMap.get(key).getValue(index);
    }

    /**
     * Get the number of reference values for the specified property
     *
     * @param id
     * @return the number of elements
     */
    public int getReferenceSize(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        return v.vl.size();
    }

    /**
     * Get the MAC-address-valued property for prop_name.
     *
     * @param id
     * @return the property value
     */
    public MacAddress getMacAddress(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        return (MacAddress) propertyMap.get(key).getValue(0);
    }

    /**
     * For a vector-valued MAC address property, get the specified
     * property value at the specified index
     *
     * @param id
     * @param index
     * @return the property value
     */
    public MacAddress getMacAddress(PolicyPropertyInfo.PolicyPropertyId id, int index) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        return (MacAddress) propertyMap.get(key).getValue(index);
    }

    /**
     * Get the number of MAC address values for the specified
     * property
     *
     * @param id
     * @return the number of elements
     */
    public int getMacAddressSize(PolicyPropertyInfo.PolicyPropertyId id) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        return v.vl.size();
    }

    // setters
    /**
     * Set the uint64-valued parameter to the specified value.
     *
     * @param id
     * @param bi
     */
    public void setUint64(PolicyPropertyInfo.PolicyPropertyId id, BigInteger bi) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.U64);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        v.setValue(bi);
    }

    /**
     * Set the uint64-vector-valued parameter to the specified value.
     *
     * @param id
     * @param bil
     */
    public void setUint64(PolicyPropertyInfo.PolicyPropertyId id, List<BigInteger> bil) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.U64);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        v.setValue(bil);

    }

    /**
     * Set the int64-valued parameter to the specified value.
     *
     * @param id
     * @param li
     */
    public void setInt64(PolicyPropertyInfo.PolicyPropertyId id, long li) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.S64);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        v.setValue(li);

    }

    /**
     * Set the int64-vector-valued parameter to the specified value.
     *
     * @param id
     * @param ll
     */
    public void setInt64(PolicyPropertyInfo.PolicyPropertyId id, List<Long> ll) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.S64);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        v.setValue(ll);

    }

    /**
     * Set the string-valued parameter to the specified value.
     *
     * @param id
     * @param s
     */
    public void setString(PolicyPropertyInfo.PolicyPropertyId id, String s) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.STRING);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        v.setValue(s);

    }

    /**
     * Set the string-vector-valued parameter to the specified vector.
     *
     * @param id
     * @param sl
     */
    public void setString(PolicyPropertyInfo.PolicyPropertyId id, List<String> sl) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.STRING);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        v.setValue(sl);

    }

    /**
     * Set the reference-valued parameter to the specified value.
     *
     * @param id
     * @param pr
     */
    public void setReference(PolicyPropertyInfo.PolicyPropertyId id, PolicyReference pr) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.REFERENCE);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        v.setValue(pr);

    }

    /**
     * Set the reference-vector-valued parameter to the specified
     * vector.
     *
     * @param id
     * @param prl
     */
    public void setReference(PolicyPropertyInfo.PolicyPropertyId id, List<PolicyReference> prl) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.REFERENCE);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        v.setValue(prl);

    }

    /**
     * Set the MAC address-valued parameter to the specified value.
     *
     * @param id
     * @param mac
     */
    public void setMacAddress(PolicyPropertyInfo.PolicyPropertyId id, MacAddress mac) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.SCALAR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.MAC);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.SCALAR);
        v.setValue(mac);

    }

    /**
     * Set the MAC address-vector-valued parameter to the specified value.
     *
     * @param id
     * @param macList
     */
    public void setMacAddress(PolicyPropertyInfo.PolicyPropertyId id, List<MacAddress> macList) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null)
            v = new Value();
        propertyMap.put(key, v);

        v.setPropertyType(PolicyPropertyInfo.PropertyType.MAC);
        v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
        v.setValue(macList);

    }

    /**
     * Add a value to a the specified unsigned 64-bit vector.
     *
     * @param id
     * @param bi
     */
    public void addUint64(PolicyPropertyInfo.PolicyPropertyId id, BigInteger bi) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.U64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null) {
            v = new Value();
            v.setPropertyType(PolicyPropertyInfo.PropertyType.U64);
            v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
            List<BigInteger> bil = new ArrayList<BigInteger>();
            v.setValue(bil);
            propertyMap.put(key, v);
        }
        v.vl.add(bi);
    }

    /**
     * Add a value to a the specified signed 64-bit vector.
     *
     * @param id
     * @param li
     */
    public void addInt64(PolicyPropertyInfo.PolicyPropertyId id, long li) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.S64,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null) {
            v = new Value();
            v.setPropertyType(PolicyPropertyInfo.PropertyType.U64);
            v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
            List<Long> ll = new ArrayList<Long>();
            v.setValue(ll);
            propertyMap.put(key, v);
        }
        v.vl.add(li);
    }

    /**
     * Add a value to a the specified string vector.
     *
     * @param id
     * @param s
     */
    public void addString(PolicyPropertyInfo.PolicyPropertyId id, String s) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.STRING,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null) {
            v = new Value();
            v.setPropertyType(PolicyPropertyInfo.PropertyType.STRING);
            v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
            List<String> sl = new ArrayList<String>();
            v.setValue(sl);
            propertyMap.put(key, v);
        }
        v.vl.add(s);
    }

    /**
     * Add a value to a the specified reference vector.
     *
     * @param id
     * @param pr
     */
    public void addReference(PolicyPropertyInfo.PolicyPropertyId id, PolicyReference pr) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.REFERENCE,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null) {
            v = new Value();
            v.setPropertyType(PolicyPropertyInfo.PropertyType.REFERENCE);
            v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
            List<PolicyReference> prl = new ArrayList<PolicyReference>();
            v.setValue(prl);
            propertyMap.put(key, v);
        }
        v.vl.add(pr);

    }

    /**
     * Add a value to a the specified MAC address vector.
     *
     * @param id
     * @param mac
     */
    public void addMacAddress(PolicyPropertyInfo.PolicyPropertyId id, MacAddress mac) {
        PropertyKey key = new PropertyKey(id, PolicyPropertyInfo.PropertyType.MAC,
                PolicyPropertyInfo.PropertyCardinality.VECTOR);
        Value v = propertyMap.get(key);
        if (v == null) {
            v = new Value();
            v.setPropertyType(PolicyPropertyInfo.PropertyType.MAC);
            v.setPropertyCardinality(PolicyPropertyInfo.PropertyCardinality.VECTOR);
            List<MacAddress> ml = new ArrayList<MacAddress>();
            v.setValue(ml);
            propertyMap.put(key, v);
        }
        v.vl.add(mac);

    }
}
