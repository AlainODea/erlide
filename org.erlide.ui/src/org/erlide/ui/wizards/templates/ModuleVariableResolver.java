/*******************************************************************************
 * Copyright (c) 2004 Lukas Larsson and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lukas Larsson
 *******************************************************************************/

package org.erlide.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class ModuleVariableResolver extends TemplateVariableResolver {

	private static final ArrayList<TemplateVariableResolver> fInstances = new ArrayList<TemplateVariableResolver>();

	private String fModule = "<module_name>";

	public ModuleVariableResolver() {
		fInstances.add(this);
	}

	public static ModuleVariableResolver getDefault() {
		if (fInstances.size() == 0) {
			fInstances.add(new ModuleVariableResolver());
		}
		return (ModuleVariableResolver) fInstances.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org
	 * .eclipse.jface.text.templates.TemplateVariable,
	 * org.eclipse.jface.text.templates.TemplateContext)
	 */
	@Override
	public void resolve(final TemplateVariable variable,
			final TemplateContext theContext) {
		variable.setValue(fModule);
	}

	/**
	 * @return Returns the module.
	 */
	public String getModule() {
		return fModule;
	}

	/**
	 * @param module
	 *            The module to set.
	 */
	public void setModule(final String module) {
		for (final Object element0 : fInstances) {
			final ModuleVariableResolver element = (ModuleVariableResolver) element0;
			element.doSetModule(module);
		}
		fModule = module;
	}

	void doSetModule(final String module) {
		fModule = module;
	}

}
