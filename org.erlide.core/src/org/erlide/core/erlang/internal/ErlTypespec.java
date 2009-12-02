/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.erlang.internal;

import org.erlide.core.erlang.IErlTypespec;

import com.ericsson.otp.erlang.OtpErlangObject;

/**
 * 
 * 
 * @author Vlad Dumitrescu
 */
public class ErlTypespec extends ErlMember implements IErlTypespec {

	private final OtpErlangObject fValue;
	private final String fExtra;

	/**
	 * @param parent
	 * @param name
	 */
	protected ErlTypespec(final ErlElement parent, final String name,
			final OtpErlangObject value, final String extra) {
		super(parent, name);
		fValue = value;
		fExtra = extra;
	}

	/**
	 * @see org.erlide.core.erlang.IErlElement#getKind()
	 */
	public Kind getKind() {
		return Kind.TYPESPEC;
	}

	public OtpErlangObject getValue() {
		return fValue;
	}

	// @Override
	// public OtpErlangObject getParseTree() {
	// // TODO Auto-generated method stub
	// return null;
	// }

	@Override
	public String toString() {
		if (fValue != null) {
			return getName() + ": " + fValue.toString(); // pp(fValue);
		} else if (fExtra != null) {
			return fExtra;
		}
		return "";
	}
}
