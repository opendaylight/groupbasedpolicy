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
import java.util.List;

import org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages.ManagedObject;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MitLib {

    protected static final Logger LOG = LoggerFactory.getLogger(MitLib.class);
    private ObjectMapper objectMapper;
    private JsonNodeFactory jnf;

    @JsonSerialize
    public static class Reference {

        String subject;
        String reference_uri;

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getReference_uri() {
            return reference_uri;
        }

        public void setReference_uri(String reference_uri) {
            this.reference_uri = reference_uri;
        }

    }

    public MitLib() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jnf = objectMapper.getNodeFactory();
    }

    public static final String REFERENCE_SUBJECT = "subject";
    public static final String REFERENCE_URI = "reference_uri";

    public BigInteger deserializeMoPropertyEnum(JsonNode node, PolicyPropertyInfo ppi) {

        EnumInfo ei = ppi.getEnumInfo();
        return ei.getEnumValue(node.asText());
    }

    public String serializeMoPropertyEnum(PolicyPropertyInfo ppi, PolicyObjectInstance poi) {

        EnumInfo ei = ppi.getEnumInfo();
        return ei.getEnumValue(poi.getUint64(ppi.getPropId()));
    }

    /**
     * Deserialize a REFERENCE property
     *
     * @param node
     * @return
     */
    public PolicyObjectInstance.PolicyReference deserializeMoPropertyRef(JsonNode node, OpflexMit mit) {

        JsonNode sn = node.findValue(REFERENCE_SUBJECT);
        if (sn == null)
            return null;
        JsonNode un = node.findValue(REFERENCE_URI);
        if (un == null)
            return null;

        PolicyClassInfo pci = mit.getClass(sn.asText());
        if (pci == null)
            return null;

        return new PolicyObjectInstance.PolicyReference(pci.getClassId(), new Uri(un.asText()));

    }

    /**
     * Serialize a REFERENCE property
     *
     * @param reference
     * @param mit
     * @return
     */
    public ObjectNode serializeMoPropertyRef(PolicyObjectInstance.PolicyReference reference, OpflexMit mit) {

        ObjectNode on = jnf.objectNode();
        PolicyClassInfo pci = mit.getClass(reference.getClassId());
        if (pci == null)
            return null;
        PolicyUri puri = new PolicyUri(reference.getUri().getValue());
        // walk our way up until we find a valid class
        String identifier = puri.pop();
        while (mit.getClass(identifier) == null) {
            identifier = puri.pop();
        }
        on.put(REFERENCE_SUBJECT, identifier);
        on.put(REFERENCE_URI, reference.getUri().getValue());
        return on;
    }

    /**
     * Take the {@link ManagedObject} and deserialize the properties
     * into a concrete type to be used by the renderer. It also
     * adds URIs for any children that are referenced in the
     * properties to the MO's "children" array.
     *
     * @param mo
     */
    public PolicyObjectInstance deserializeMoProperties(ManagedObject mo, OpflexMit mit) {

        /*
         * The subject gives us the class to use for the schema
         */
        PolicyClassInfo pci = mit.getClass(mo.getSubject());

        // sanity checks
        if (pci == null)
            return null;

        PolicyObjectInstance poi = new PolicyObjectInstance(pci.getClassId());

        if (mo.getProperties() == null)
            return poi;

        for (ManagedObject.Property prop : mo.getProperties()) {
            PolicyPropertyInfo ppi = pci.getProperty(prop.getName());
            if ((ppi == null) || (prop.getData() == null))
                continue;

            JsonNode node = prop.getData();

            boolean vectored = false;
            if (ppi.getPropCardinality().equals(PolicyPropertyInfo.PropertyCardinality.VECTOR)) {
                vectored = true;
            }
            switch (ppi.getType()) {
                case STRING:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<String> sl = new ArrayList<String>();
                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);
                            if (!jn.isTextual())
                                continue;

                            sl.add(jn.asText());
                        }
                        poi.setString(ppi.getPropId(), sl);
                    } else {
                        if (!node.isTextual())
                            continue;

                        poi.setString(ppi.getPropId(), node.asText());
                    }
                    break;

                case U64:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<BigInteger> bil = new ArrayList<BigInteger>();

                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);
                            if (!jn.isBigInteger())
                                continue;

                            bil.add(new BigInteger(jn.asText()));
                        }
                        poi.setUint64(ppi.getPropId(), bil);
                    } else {
                        if (!node.isBigInteger())
                            continue;

                        poi.setUint64(ppi.getPropId(), new BigInteger(node.asText()));
                    }
                    break;

                case S64:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<Long> ll = new ArrayList<Long>();

                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);

                            if (!jn.isBigInteger())
                                continue;

                            ll.add(jn.asLong());
                        }
                        poi.setInt64(ppi.getPropId(), ll);
                    } else {
                        if (!node.isBigInteger())
                            continue;

                        poi.setInt64(ppi.getPropId(), node.asLong());
                    }
                    break;

                case REFERENCE:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<PolicyObjectInstance.PolicyReference> prl = new ArrayList<PolicyObjectInstance.PolicyReference>();

                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);
                            PolicyObjectInstance.PolicyReference pr = deserializeMoPropertyRef(jn, mit);
                            if (pr == null)
                                continue;

                            prl.add(pr);
                        }
                        poi.setReference(ppi.getPropId(), prl);
                    } else {
                        PolicyObjectInstance.PolicyReference pr = deserializeMoPropertyRef(node, mit);
                        if (pr == null)
                            continue;

                        poi.setReference(ppi.getPropId(), pr);
                    }
                    break;

                case ENUM8:
                case ENUM16:
                case ENUM32:
                case ENUM64:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<BigInteger> bil = new ArrayList<BigInteger>();

                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);
                            if (!jn.isTextual())
                                continue;
                            BigInteger bi = deserializeMoPropertyEnum(node, ppi);

                            bil.add(bi);
                        }
                        poi.setUint64(ppi.getPropId(), bil);
                    } else {
                        if (!node.isTextual())
                            continue;

                        BigInteger bi = deserializeMoPropertyEnum(node, ppi);
                        poi.setUint64(ppi.getPropId(), bi);
                    }
                    break;

                case MAC:
                    if (vectored == true) {
                        if (!node.isArray())
                            continue;

                        List<MacAddress> ml = new ArrayList<MacAddress>();

                        for (int i = 0; i < node.size(); i++) {
                            JsonNode jn = node.get(i);

                            if (!jn.isTextual())
                                continue;

                            ml.add(new MacAddress(jn.asText()));
                        }
                        poi.setMacAddress(ppi.getPropId(), ml);
                    } else {
                        if (!node.isTextual())
                            continue;

                        poi.setMacAddress(ppi.getPropId(), new MacAddress(node.asText()));
                    }
                    break;

                case COMPOSITE:

            }

        }

        return poi;
    }

    /**
     * Serialize the properties contained in the {@link PolicyObjectInstance} into the properties
     * field of the {@link ManagedObject}
     *
     * @param pci
     * @param poi
     * @param mo
     * @param mit
     */
    public void serializeMoProperties(PolicyClassInfo pci, PolicyObjectInstance poi, ManagedObject mo, OpflexMit mit) {

        List<PolicyPropertyInfo> ppil = pci.getProperties();
        if (ppil == null)
            return;

        List<ManagedObject.Property> pl = new ArrayList<ManagedObject.Property>();

        /*
         * For serialization of values, we can cheat a bit,
         * as the native "toString" method gives us exactly
         * the formatting we need (including vectors).
         */
        for (PolicyPropertyInfo ppi : ppil) {
            /*
             * Skip any properties that aren't populated for this
             * object instance
             */
            if (ppi.getType() != PolicyPropertyInfo.PropertyType.COMPOSITE
                    && !poi.isSet(ppi.getPropId(), ppi.getType(), ppi.getPropCardinality())) {
                continue;
            }
            ManagedObject.Property p = null;
            boolean scalar = true;

            if (ppi.getPropCardinality() == PolicyPropertyInfo.PropertyCardinality.VECTOR) {
                scalar = false;
            }

            switch (ppi.getType()) {
                case STRING:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        JsonNode jn = jnf.textNode(poi.getString(ppi.getPropId()));
                        p.setData(jn);
                    } else {
                        int len = poi.getStringSize(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            an.add(poi.getString(ppi.getPropId(), i));
                        }
                        p.setData(an);
                    }
                    break;

                case S64:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        JsonNode jn = jnf.numberNode(poi.getInt64(ppi.getPropId()));
                        p.setData(jn);
                    } else {
                        int len = poi.getInt64Size(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            an.add(Long.valueOf(poi.getInt64(ppi.getPropId(), i)));
                        }
                        p.setData(an);
                    }
                    break;

                case ENUM8:
                case ENUM16:
                case ENUM32:
                case ENUM64:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        JsonNode jn = jnf.textNode(serializeMoPropertyEnum(ppi, poi));
                        p.setData(jn);
                    } else {
                        int len = poi.getUint64Size(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            an.add(serializeMoPropertyEnum(ppi, poi));
                        }
                        p.setData(an);
                    }
                    break;

                case U64:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        JsonNode jn = jnf.numberNode(poi.getUint64(ppi.getPropId()));
                        p.setData(jn);
                    } else {
                        int len = poi.getUint64Size(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            an.numberNode(poi.getUint64(ppi.getPropId()));
                        }
                        p.setData(an);
                    }
                    break;

                case MAC:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        MacAddress mac = poi.getMacAddress(ppi.getPropId());
                        JsonNode jn = jnf.textNode(mac.getValue().toString());
                        p.setData(jn);
                    } else {
                        int len = poi.getMacAddressSize(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            MacAddress mac = poi.getMacAddress(ppi.getPropId());
                            an.add(mac.getValue().toString());
                        }
                        p.setData(an);
                    }
                    break;

                case REFERENCE:
                    p = new ManagedObject.Property();
                    p.setName(ppi.getPropName());
                    if (scalar == true) {
                        ObjectNode on = serializeMoPropertyRef(poi.getReference(ppi.getPropId()), mit);
                        p.setData(on);
                    } else {
                        int len = poi.getReferenceSize(ppi.getPropId());
                        ArrayNode an = jnf.arrayNode();
                        for (int i = 0; i < len; i++) {
                            ObjectNode on = serializeMoPropertyRef(poi.getReference(ppi.getPropId(), i), mit);
                            an.add(on);
                        }
                        p.setData(an);
                    }
                    break;

                case COMPOSITE:
                    /*
                     * Get the URI to add to the list of children
                     */
                    break;

                default:

            }
            if (p != null) {
                pl.add(p);
            }
        }

        mo.setProperties(pl);
    }
}
