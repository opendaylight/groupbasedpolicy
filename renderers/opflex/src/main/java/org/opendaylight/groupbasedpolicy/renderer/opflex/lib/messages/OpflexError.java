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

public class OpflexError {

	public static enum ErrorCode {
		ERROR("ERROR"),
		EUNSUPPORTED("EUNSUPPORTED"),
		ESTATE("ESTATE"),
		EPROTO("EPROTO"),
		EDOMAIN("EDOMAIN");

		private String errorCode;

		ErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}

		@Override
		public String toString() {
			return this.errorCode;
		}
	}

	private String code;
	private String message;
	private String trace;
	private String data;
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getTrace() {
		return trace;
	}
	public void setTrace(String trace) {
		this.trace = trace;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
}
