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

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

/**
 * Class used for building and extending URIs
 * Returning the URI could be optimized by building this
 * as we go, rather than building it when requested.
 * This counts and levels used in this class are for
 * the level of hierarchy, and do not include any
 * separator characters ("/"). For example, the following
 * URI:
 * /tenants/tenant/51134b1e-6047-4d51-8d07-4135afd3672f
 * has 3 levels.
 *
 * @author tbachman
 */
public class PolicyUri {

    public final static String POLICY_URI_SEP = "/";

    private List<String> uri;

    public PolicyUri() {
        uri = new ArrayList<String>();
    }

    /**
     * Copy Constructor
     *
     * @param policyUri
     */
    public PolicyUri(PolicyUri policyUri) {
        this.uri = new ArrayList<String>(policyUri.uri);
    }

    /**
     * Constructor using a full string, which gets split
     * into it's elements.
     *
     * @param uri
     */
    public PolicyUri(String uri) {
        String[] tmpUri = uri.split(PolicyUri.POLICY_URI_SEP);
        if (tmpUri.length > 0) {
            this.uri = new ArrayList<String>();
            // gets rid of leading empty element
            for (int i = 1; i < tmpUri.length; i++) {
                this.push(tmpUri[i]);
            }
        } else {
            this.uri = null;
        }
    }

    /**
     * Constructor using a list of URI elements, which
     * excludes separator characters
     *
     * @param tokens
     */
    public PolicyUri(List<String> tokens) {
        if (tokens.size() > 0) {
            this.uri = new ArrayList<String>();
            for (String t : tokens) {
                this.push(t);
            }
        }
    }

    /**
     * Return the URI as a Uri object, including
     * separator characters
     *
     * @return
     */
    public Uri getUri() {
        return new Uri(this.toString());
    }

    /**
     * Push a new leaf on to the URI
     *
     * @param leaf
     */
    public void push(String leaf) {
        uri.add(POLICY_URI_SEP);
        uri.add(leaf);
    }

    /**
     * Returns the String representation of parent object URI
     *
     * @return
     */
    public String getParent() {
        if (uri.size() == 0)
            return null;

        PolicyUri parentUri = new PolicyUri(this);
        parentUri.pop();
        return parentUri.toString();

    }

    /**
     * Remove (and return) the leaf of the URI.
     * Never pop off the "/" root element.
     *
     * @return
     */
    public String pop() {
        if (uri.size() <= 0) {
            return null;
        } else {
            // remove the node
            String s = uri.remove(uri.size() - 1);
            // remove the separator
            uri.remove(uri.size() - 1);
            // return just the node
            return s;
        }
    }

    /**
     * Determine if the URI is valid.
     *
     * @return
     */
    public boolean valid() {
        if (uri.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the element at depth "level". Returns
     * null if level exceeds the hierarchy depth.
     *
     * @param level
     * @return
     */
    public String getElement(int level) {
        if ((level * 2 + 1) > this.uri.size())
            return null;

        return this.uri.get(level * 2 + 1);
    }

    /**
     * Return the index of the first instance
     * where the named element is found in the URI,
     * starting the search from the root
     *
     * @param needle
     * @return
     */
    public int whichElement(String needle) {
        return (this.uri.indexOf(needle) / 2);
    }

    /**
     * Return the number of levels in the parsed URI
     * hierarchy
     *
     * @return
     */
    public int totalElements() {
        return (this.uri.size() / 2);
    }

    /**
     * Check to see if the parsed URI contains
     * the named element in the hierarchy
     *
     * @param needle
     * @return
     */
    public boolean contains(String needle) {
        return this.uri.contains(needle);
    }

    public String originalPath() {
        return this.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        PolicyUri other = (PolicyUri) obj;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (uri.size() == 0)
            return "";
        StringBuilder sb = new StringBuilder();

        for (String s : uri) {
            sb.append(s);
        }
        return sb.toString();
    }
}
