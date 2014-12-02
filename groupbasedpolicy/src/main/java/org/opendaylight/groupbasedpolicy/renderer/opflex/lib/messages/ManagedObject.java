/*
 * Copyright (C) 2014 Cisco Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Thomas Bachman
 */
package org.opendaylight.groupbasedpolicy.renderer.opflex.lib.messages;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class ManagedObject {

	public static class Property {
        private String name;
        private JsonNode data;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public JsonNode getData() {
            return data;
        }
        public void setData(JsonNode data) {
            this.data = data;
        }
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((data == null) ? 0 : data.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			Property other = (Property) obj;
			if (data == null) {
				if (other.data != null)
					return false;
			} else if (!data.asText().equals(other.getData().asText()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

    }

    private String subject;
    private Uri uri;
    private List<Property> properties;
    private String parent_subject;
    private Uri parent_uri;
    private String parent_relation;
    private List<Uri> children;

	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public Uri getUri() {
		return uri;
	}
	public void setUri(Uri uri) {
		this.uri = uri;
	}
	public List<Property> getProperties() {
		return properties;
	}
	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
	public String getParent_subject() {
		return parent_subject;
	}
	public void setParent_subject(String parent_subject) {
		this.parent_subject = parent_subject;
	}
	public Uri getParent_uri() {
		return parent_uri;
	}
	public void setParent_uri(Uri parent_uri) {
		this.parent_uri = parent_uri;
	}
	public String getParent_relation() {
		return parent_relation;
	}
	public void setParent_relation(String parent_relation) {
		this.parent_relation = parent_relation;
	}
	public List<Uri> getChildren() {
		return children;
	}
	public void setChildren(List<Uri> children) {
		this.children = children;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((children == null) ? 0 : children.hashCode());
		result = prime * result
				+ ((parent_relation == null) ? 0 : parent_relation.hashCode());
		result = prime * result
				+ ((parent_subject == null) ? 0 : parent_subject.hashCode());
		result = prime * result
				+ ((parent_uri == null) ? 0 : parent_uri.hashCode());
		result = prime * result
				+ ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
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
		ManagedObject other = (ManagedObject) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (parent_relation == null) {
			if (other.parent_relation != null)
				return false;
		} else if (!parent_relation.equals(other.parent_relation))
			return false;
		if (parent_subject == null) {
			if (other.parent_subject != null)
				return false;
		} else if (!parent_subject.equals(other.parent_subject))
			return false;
		if (parent_uri == null) {
			if (other.parent_uri != null)
				return false;
		} else if (!parent_uri.equals(other.parent_uri))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
}
