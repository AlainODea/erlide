/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.properties;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.erlide.core.ErlangPlugin;
import org.erlide.core.erlang.IErlModule;
import org.erlide.ui.editors.erl.IErlangHelpContextIds;
import org.erlide.ui.launch.DebugTab;
import org.erlide.ui.launch.DebugTab.DebugTreeItem;
import org.erlide.ui.launch.DebugTab.TreeContentProvider;
import org.erlide.ui.launch.DebugTab.TreeLabelProvider;

/**
 * Property page used to set the project's edoc location
 */
public class ErlTracingPropertyPage extends PropertyPage implements
		IPreferenceChangeListener, IPropertyChangeListener {

	public static final String PROP_ID = "org.eclipse.jdt.ui.propertyPages.EdocConfigurationPropertyPage"; //$NON-NLS-1$

	// private boolean fIsValidElement;

	// private IPath fContainerPath;
	@SuppressWarnings("unused")
	private URL fInitialLocation;

	private final ArrayList<IErlModule> tracedModules = null;

	private CheckboxTreeViewer checkboxTreeViewer;

	public ErlTracingPropertyPage() {
	}

	private IEclipsePreferences getNode() {
		final IAdaptable prj = getElement();
		final IProject project = (IProject) prj.getAdapter(IProject.class);
		final IEclipsePreferences node = new ProjectScope(project)
				.getNode(ErlangPlugin.PLUGIN_ID);
		return node;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);
		setDescription("Specify the location of the generated edoc (in HTML format).");
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(),
				IErlangHelpContextIds.EDOC_CONFIGURATION_PROPERTY_PAGE);
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(final Composite parent) {
		// create composite
		final Composite comp = parent;

		final GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);

		final Group tracedModulesGroup = new Group(comp, SWT.NONE);
		tracedModulesGroup.setText("Traced modules");
		final GridData gd_interpretedModulesGroup = new GridData();
		tracedModulesGroup.setLayoutData(gd_interpretedModulesGroup);
		tracedModulesGroup.setLayout(new GridLayout());

		checkboxTreeViewer = new CheckboxTreeViewer(tracedModulesGroup,
				SWT.BORDER);
		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
			@SuppressWarnings("synthetic-access")
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final DebugTab.DebugTreeItem dti = (DebugTreeItem) event
						.getElement();
				checkboxTreeViewer.setGrayed(dti, false);
				final boolean checked = event.getChecked();
				setSubtreeChecked(dti, checked, tracedModules,
						checkboxTreeViewer);
				DebugTab.checkUpwards(checkboxTreeViewer, dti, checked, false);
			}

		});
		checkboxTreeViewer.setLabelProvider(new TreeLabelProvider());
		checkboxTreeViewer.setContentProvider(new TreeContentProvider());
		final Tree tree = checkboxTreeViewer.getTree();
		final GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd_tree.minimumWidth = 250;
		gd_tree.minimumHeight = 120;
		gd_tree.widthHint = 256;
		gd_tree.heightHint = 220;
		tree.setLayoutData(gd_tree);

		applyDialogFont(comp);
		return comp;
	}

	public static void setSubtreeChecked(final DebugTreeItem dti,
			final boolean checked, final ArrayList<IErlModule> traceModules,
			final CheckboxTreeViewer checkboxTreeViewer) {
		final List<DebugTreeItem> children = dti.getChildren();
		if (children == null || children.size() == 0) {
			traceOrNotTrace(dti, checked, traceModules);
			return;
		}
		for (final DebugTreeItem i : children) {
			checkboxTreeViewer.setChecked(i, checked);
			setSubtreeChecked(i, checked, traceModules, checkboxTreeViewer);
		}
	}

	public static void traceOrNotTrace(final DebugTreeItem dti,
			final boolean checked, final ArrayList<IErlModule> traceModules) {
		final IErlModule m = (IErlModule) dti.getItem();
		if (checked) {
			traceModules.add(m);
		} else {
			traceModules.remove(m);
		}
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		;
		;
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		;
		;
		return true;
	}

	public void preferenceChange(final PreferenceChangeEvent event) {
		// TODO Auto-generated method stub

	}

	public void propertyChange(final PropertyChangeEvent event) {
		// TODO Auto-generated method stub

	}

}
