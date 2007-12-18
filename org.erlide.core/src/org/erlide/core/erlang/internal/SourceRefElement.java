/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.core.erlang.internal;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IOpenable;
import org.erlide.core.erlang.IParent;
import org.erlide.core.erlang.ISourceManipulation;
import org.erlide.core.erlang.ISourceRange;
import org.erlide.core.erlang.ISourceReference;
import org.erlide.core.erlang.util.IBuffer;
import org.erlide.core.erlang.util.Util;

/**
 * Abstract class for Erlang elements which implement ISourceReference.
 */
abstract class SourceRefElement extends ErlElement implements ISourceReference {

	protected int fSourceRangeStart;

	protected int fSourceRangeEnd;

	protected int lineStart, lineEnd;

	protected SourceRefElement(IErlElement parent, String name) {
		super(parent, name);
	}

	/**
	 * This element is being closed. Do any necessary cleanup.
	 */
	@Override
	protected void closing(Object info) throws ErlModelException {
		// Do any necessary cleanup
	}

	/**
	 * Returns a new element info for this element.
	 */
	protected Object createElementInfo() {
		return null; // not used for source ref elements
	}

	/**
	 * @see ISourceManipulation
	 */
	public void copy(IErlElement container, IErlElement sibling, String rename,
			boolean force, IProgressMonitor monitor) throws ErlModelException {
		if (container == null) {
			throw new IllegalArgumentException(Util
					.bind("operation.nullContainer")); //$NON-NLS-1$
		}
		final IErlElement[] elements = new IErlElement[] { this };
		final IErlElement[] containers = new IErlElement[] { container };
		IErlElement[] siblings = null;
		if (sibling != null) {
			siblings = new IErlElement[] { sibling };
		}
		String[] renamings = null;
		if (rename != null) {
			renamings = new String[] { rename };
		}
		getModel().copy(elements, containers, siblings, renamings, force,
				monitor);
	}

	/**
	 * @see ISourceManipulation
	 */
	public void delete(boolean force, IProgressMonitor monitor)
			throws ErlModelException {
		final IErlElement[] elements = new IErlElement[] { this };
		getModel().delete(elements, force, monitor);
	}

	/*
	 * @see ErlElement#generateInfos
	 */
	protected void open(IProgressMonitor pm) throws ErlModelException {
		final Openable openableParent = (Openable) getOpenableParent();
		if (openableParent == null) {
			return;
		}

		final ErlElement openableParentInfo = (ErlElement) ErlangCore
				.getModelManager().getInfo(openableParent);
		if (openableParentInfo == null) {
			openableParent.open(pm);
		}
	}

	/**
	 * @see IMember
	 */
	@Override
	public IErlModule getModule() {
		return ((ErlElement) getParent()).getModule();
	}

	/**
	 * Elements within compilation units and class files have no corresponding
	 * resource.
	 * 
	 * @see IErlElement
	 */
	public IResource getCorrespondingResource() throws ErlModelException {
		if (!exists()) {
			throw newNotPresentException();
		}
		return null;
	}

	/**
	 * Return the first instance of IOpenable in the hierarchy of this type
	 * (going up the hierarchy from this type);
	 */
	@Override
	public IOpenable getOpenableParent() {
		IErlElement current = getParent();
		while (current != null) {
			if (current instanceof IOpenable) {
				return (IOpenable) current;
			}
			current = current.getParent();
		}
		return null;
	}

	/*
	 * @see IErlElement
	 */
	public IResource getResource() {
		return this.getParent().getResource();
	}

	/**
	 * @see ISourceReference
	 */
	public String getSource() throws ErlModelException {
		final IOpenable openable = getOpenableParent();
		final IBuffer buffer = openable.getBuffer();
		if (buffer == null) {
			return null;
		}
		final ISourceRange range = getSourceRange();
		final int offset = range.getOffset();
		final int length = range.getLength();
		if (offset == -1 || length == 0) {
			return null;
		}
		try {
			return buffer.getText(offset, length);
		} catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * @see ISourceReference
	 */
	public ISourceRange getSourceRange() throws ErlModelException {
		return new SourceRange(fSourceRangeStart, fSourceRangeEnd
				- fSourceRangeStart + 1);
	}

	/**
	 * @see IErlElement
	 */
	public IResource getUnderlyingResource() throws ErlModelException {
		if (!exists()) {
			throw newNotPresentException();
		}
		return getParent().getUnderlyingResource();
	}

	/**
	 * @see IParent
	 */
	@Override
	public boolean hasChildren() {
		return getChildren().length > 0;
	}

	/**
	 * @see ISourceManipulation
	 */
	public void move(IErlElement container, IErlElement sibling, String rename,
			boolean force, IProgressMonitor monitor) throws ErlModelException {
		if (container == null) {
			throw new IllegalArgumentException(Util
					.bind("operation.nullContainer")); //$NON-NLS-1$
		}
		final IErlElement[] elements = new IErlElement[] { this };
		final IErlElement[] containers = new IErlElement[] { container };
		IErlElement[] siblings = null;
		if (sibling != null) {
			siblings = new IErlElement[] { sibling };
		}
		String[] renamings = null;
		if (rename != null) {
			renamings = new String[] { rename };
		}
		getModel().move(elements, containers, siblings, renamings, force,
				monitor);
	}

	/**
	 * @see ISourceManipulation
	 */
	public void rename(String newName, boolean force, IProgressMonitor monitor)
			throws ErlModelException {
		if (newName == null) {
			throw new IllegalArgumentException(Util.bind("element.nullName")); //$NON-NLS-1$
		}
		final IErlElement[] elements = new IErlElement[] { this };
		final IErlElement[] dests = new IErlElement[] { this.getParent() };
		final String[] renamings = new String[] { newName };
		getModel().rename(elements, dests, renamings, force, monitor);
	}

	/**
	 */
	public int getSourceRangeEnd() {
		return fSourceRangeEnd;
	}

	/**
	 */
	public int getSourceRangeStart() {
		return fSourceRangeStart;
	}

	protected void setSourceRangeEnd(int end) {
		fSourceRangeEnd = end;
	}

	protected void setSourceRangeStart(int start) {
		fSourceRangeStart = start;
	}

	protected void setLineStart(int lineStart) {
		this.lineStart = lineStart;
	}

	public int getLineStart() {
		return this.lineStart;
	}

	public int getLineEnd() {
		return this.lineEnd;
	}

	protected void setLineEnd(int lineEnd) {
		this.lineEnd = lineEnd;
	}

}
