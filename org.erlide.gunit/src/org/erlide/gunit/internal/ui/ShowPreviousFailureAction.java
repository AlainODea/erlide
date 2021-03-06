/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.gunit.internal.ui;

import org.eclipse.jface.action.Action;

class ShowPreviousFailureAction extends Action {

	private final TestRunnerViewPart fPart;

	public ShowPreviousFailureAction(final TestRunnerViewPart part) {
		super(GUnitMessages.ShowPreviousFailureAction_label);
		setDisabledImageDescriptor(GUnitPlugin
				.getImageDescriptor("dlcl16/select_prev.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(GUnitPlugin
				.getImageDescriptor("elcl16/select_prev.gif")); //$NON-NLS-1$
		setImageDescriptor(GUnitPlugin
				.getImageDescriptor("elcl16/select_prev.gif")); //$NON-NLS-1$
		setToolTipText(GUnitMessages.ShowPreviousFailureAction_tooltip);
		this.fPart = part;
	}

	@Override
	public void run() {
		this.fPart.selectPreviousFailure();
	}
}
