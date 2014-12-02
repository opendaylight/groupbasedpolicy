/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.groupbasedpolicy.renderer.opflex.mit;


/**
 * Class containing metadata used to describe
 * properties contained by an OpFlex Managed Object
 *
 * @author tbachman
 *
 */
public interface PolicyPropertyInfo {
	/**
	 * enum that represents possible Property types
	 *
	 * @author tbachman
	 *
	 */
	static public enum PropertyType {
		COMPOSITE("composite"),
		REFERENCE("reference"),
		STRING("string"),
		S64("s64"),
		U64("u64"),
		MAC("mac"),
		ENUM8("enum8"),
		ENUM16("enum16"),
		ENUM32("enum32"),
		ENUM64("enum64");

		private final String type;

		PropertyType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return this.type;
		}
	}

	/**
	 * enum that represents the possible cardinalities of a Property
	 *
	 * @author tbachman
	 *
	 */
	static public enum PropertyCardinality {
		SCALAR("scalar"),
		VECTOR("vector");

		private final String cardinality;

		PropertyCardinality(String cardinality) {
			this.cardinality = cardinality;
		}

		@Override
		public String toString() {
			return this.cardinality;
		}
	}

	/**
	 * The unique local ID assigned to this Property
	 *
	 * @author tbachman
	 *
	 */
	static public class PolicyPropertyId {
		private final long propertyId;
		public PolicyPropertyId(long propertyId) {
			this.propertyId = propertyId;
		}
		public long getPropertyId() {
			return propertyId;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (propertyId ^ (propertyId >>> 32));
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
			PolicyPropertyId other = (PolicyPropertyId) obj;
			if (propertyId != other.propertyId)
				return false;
			return true;
		}

	}

	public static class PolicyPropertyInfoBuilder {
		private long classId;
		private PropertyType type;
		private PolicyPropertyId propId;
		private PropertyCardinality propCardinality;
		private String propName;
		private EnumInfo enumInfo;

		public PolicyPropertyInfoBuilder setClassId(long classId) {
			this.classId = classId;
			return this;
		}

		public PolicyPropertyInfoBuilder setType(PropertyType type) {
			this.type = type;
			return this;
		}

		public PolicyPropertyInfoBuilder setPropId(PolicyPropertyId propId) {
			this.propId = propId;
			return this;
		}

		public PolicyPropertyInfoBuilder setPropCardinality(PropertyCardinality propCardinality) {
			this.propCardinality = propCardinality;
			return this;
		}

		public PolicyPropertyInfoBuilder setPropName(String propName) {
			this.propName = propName;
			return this;
		}

		public PolicyPropertyInfoBuilder setEnumInfo(EnumInfo enumInfo) {
			this.enumInfo = enumInfo;
			return this;
		}

		public PolicyPropertyInfo build() {
			return new PolicyPropertyInfoImpl(this);
		}

		private static final class PolicyPropertyInfoImpl implements PolicyPropertyInfo {
			/*
			 * The classId is only used in COMPOSITE properties
			 */
			private final long classId;
			private final PropertyType type;
			private final PolicyPropertyId propId;
			private final PropertyCardinality propCardinality;
			private final String propName;
			private final EnumInfo enumInfo;

			public PolicyPropertyInfoImpl(PolicyPropertyInfoBuilder builder) {
				this.classId = builder.classId;
				this.type = builder.type;
				this.propId = builder.propId;
				this.propCardinality = builder.propCardinality;
				this.propName = builder.propName;
				this.enumInfo = builder.enumInfo;

			}
			@Override
			public long getClassId() {
				return classId;
			}

			@Override
			public PropertyType getType() {
				return type;
			}

			@Override
			public PolicyPropertyId getPropId() {
				return propId;
			}

			@Override
			public PropertyCardinality getPropCardinality() {
				return propCardinality;
			}

			@Override
			public String getPropName() {
				return propName;
			}

			@Override
			public EnumInfo getEnumInfo() {
				return enumInfo;
			}
		}

	}

	/**
	 * Get the class of the {@link PolicyPropertyInfo} object
	 *
	 * @return
	 */
	public long getClassId();

	/**
	 * Get the type of the {@link PolicyPropertyInfo} object
	 *
	 * @return
	 */
	public PropertyType getType();

	/**
	 * Get the ID of the {@link PolicyPropertyInfo} object
	 *
	 * @return
	 */
	public PolicyPropertyId getPropId();

	/**
	 * Get the cardinality of the {@link PolicyPropertyInfo} object
	 *
	 * @return
	 */
	public PropertyCardinality getPropCardinality();

	/**
	 * Get the name of the {@link PolicyPropertyInfo} object
	 *
	 * @return
	 */
	public String getPropName();

	/**
	 * Get the {@link EnumInfo} object for the {@link PolicyPropertyInfo}
	 * object, if present
	 *
	 * @return
	 */
	public EnumInfo getEnumInfo();

}
