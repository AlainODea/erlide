/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     QNX Software System
 *******************************************************************************/
package org.erlide.ui.internal.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.ISourceRange;
import org.erlide.core.erlang.ISourceReference;
import org.erlide.core.erlang.IErlElement.Kind;
import org.erlide.ui.ErlideUIPlugin;

/**
 * 
 */

class ErlNode extends DocumentRangeNode implements ITypedElement {

	private final ErlNode fParent;

	private final Kind fType;

	private ErlNode(final ErlNode parent, final Kind type, final String id,
			final IDocument doc, final int start, final int length) {
		super(type.hashCode(), id, doc, start, length);
		fParent = parent;
		fType = type;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	public ErlNode(final ErlNode parent, final Kind type, final String id,
			final int start, final int length) {
		this(parent, type, id, parent.getDocument(), start, length);
	}

	public static ErlNode createErlNode(final ErlNode parent,
			final IErlElement element, final Document doc) {
		int start = 0, length = 0;
		if (element instanceof IErlModule) {
			length = doc.getLength();
		} else if (element instanceof ISourceReference) {
			final ISourceReference sourceReference = (ISourceReference) element;
			ISourceRange sr;
			try {
				sr = sourceReference.getSourceRange();
				start = sr.getOffset();
				length = sr.getLength();
			} catch (final ErlModelException e) {
				e.printStackTrace();
			}
		}
		final Kind kind = element.getKind();
		return new ErlNode(parent, kind, ErlangCompareUtilities
				.getJavaElementID(element), doc, start, length);
	}

	/**
	 * @see ITypedInput#getParent
	 */
	public ErlNode getParent() {
		return fParent;
	}

	/**
	 * @see ITypedInput#getNodeType
	 */
	public Kind getNodeType() {
		return fType;
	}

	/**
	 * @see ITypedInput#getName
	 */
	public String getName() {
		return getId();
	}

	/**
	 * @see ITypedInput#getType
	 */
	public String getType() {
		return ".erl";
	}

	/**
	 * @see ITypedInput#getImage
	 */
	public Image getImage() {
		final ImageDescriptor descriptor = ErlideUIPlugin.getDefault()
				.getImageDescriptor("erl");
		return ErlideUIPlugin.getImageDescriptorRegistry().get(descriptor);
	}
}
