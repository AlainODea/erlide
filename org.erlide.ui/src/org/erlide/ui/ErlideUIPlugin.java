/*******************************************************************************
 * Copyright (c) 2004 Eric Merritt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eric Merritt
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.erlide.core.ErlangStatusConstants;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.util.ErlideUtil;
import org.erlide.jinterface.backend.ErlBackend;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.jinterface.util.JRpcUtil;
import org.erlide.runtime.backend.ErlideBackend;
import org.erlide.ui.console.ErlConsoleManager;
import org.erlide.ui.console.ErlangConsolePage;
import org.erlide.ui.internal.folding.ErlangFoldingStructureProviderRegistry;
import org.erlide.ui.util.BackendManagerPopup;
import org.erlide.ui.util.IContextMenuConstants;
import org.erlide.ui.util.ImageDescriptorRegistry;
import org.erlide.ui.util.ProblemMarkerManager;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 * 
 * 
 * @author Eric Merritt [cyberlync at gmail dot com]
 */
public class ErlideUIPlugin extends AbstractUIPlugin {

	/**
	 * The plugin id
	 */
	public static final String PLUGIN_ID = "org.erlide.ui";

	/**
	 * The shared instance.
	 */
	private static ErlideUIPlugin plugin;

	/**
	 * Resource bundle.
	 */
	private ResourceBundle resourceBundle;

	private ImageDescriptorRegistry fImageDescriptorRegistry;

	/**
	 * The extension point registry for the
	 * <code>org.eclipse.jdt.ui.javaFoldingStructureProvider</code> extension
	 * point.
	 * 
	 * @since 3.0
	 */
	private ErlangFoldingStructureProviderRegistry fFoldingStructureProviderRegistry;

	private ProblemMarkerManager fProblemMarkerManager = null;

	private ErlConsoleManager erlConMan;

	/**
	 * The constructor.
	 */
	public ErlideUIPlugin() {
		super();
		plugin = this;

		try {
			resourceBundle = ResourceBundle
					.getBundle("org.erlide.ui.ErlideUIPluginResources");
		} catch (final MissingResourceException x) {
			x.printStackTrace();
			resourceBundle = null;
		}

	}

	/**
	 * This method is called upon plug-in activation
	 * 
	 * @param context
	 *            The context
	 * @throws Exception
	 *             if a problem occurs
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		ErlLogger.debug("Starting UI " + Thread.currentThread());
		super.start(context);

		// set this classloader to be used with erlang rpc
		JRpcUtil.loader = getClass().getClassLoader();

		new InitializeAfterLoadJob().schedule();

		if (ErlideUtil.isDeveloper()) {
			BackendManagerPopup.init();
		}

		ErlLogger.debug("Started UI");

		erlConMan = new ErlConsoleManager();
		if (ErlideUtil.isDeveloper()) {
			erlConMan.runtimeAdded(ErlangCore.getBackendManager()
					.getIdeBackend());
		}

		startPeriodicDump();
	}

	/**
	 * This method is called when the plug-in is stopped
	 * 
	 * @param context
	 *            the context
	 * @throws Exception
	 *             if a problem occurs
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		erlConMan.dispose();

		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return The plugin
	 */
	public static ErlideUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 * 
	 * @param key
	 *            The resource
	 * @return The identified string
	 */
	public static String getResourceString(final String key) {
		final ResourceBundle bundle = ErlideUIPlugin.getDefault()
				.getResourceBundle();
		try {

			final String returnString = bundle != null ? bundle.getString(key)
					: key;
			return returnString;
		} catch (final MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 * 
	 * @return The requested bundle
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated display. If so, this display
	 * is returned. Otherwise the method returns the default display.
	 * 
	 * @return the standard display
	 */
	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}

		return display;
	}

	/**
	 * Creates an image and places it in the image registry.
	 * 
	 * @param id
	 *            The image id
	 * @param baseURL
	 *            The descriptor url
	 */
	protected void createImageDescriptor(final String id, final URL baseURL) {
		URL url = null;
		try {
			url = new URL(baseURL, ErlideUIConstants.ICON_PATH + id);
		} catch (final MalformedURLException e) {
			// ignore exception
		}

		getImageRegistry().put(id, ImageDescriptor.createFromURL(url));
	}

	/**
	 * Returns the image descriptor for the given image PLUGIN_ID. Returns null
	 * if there is no such image.
	 * 
	 * @param id
	 *            The image id
	 * 
	 * @return The image descriptor
	 */
	public ImageDescriptor getImageDescriptor(final String id) {
		final ImageDescriptor returnImageDescriptor = getImageRegistry()
				.getDescriptor(id);
		return returnImageDescriptor;
	}

	/**
	 * Returns the image for the given image PLUGIN_ID. Returns null if there is
	 * no such image.
	 * 
	 * @param id
	 *            The image id
	 * 
	 * @return The image
	 */
	public Image getImage(final String id) {
		final Image returnImage = getImageRegistry().get(id);
		return returnImage;
	}

	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(final ImageRegistry reg) {
		super.initializeImageRegistry(reg);

		final URL baseURL = getBundle().getEntry("/");

		createImageDescriptor(ErlideUIConstants.IMG_CONSOLE, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_NEW_PROJECT_WIZARD, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_PROJECT_LABEL, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_PACKAGE_FOLDER_LABEL,
				baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_PACKAGE_LABEL, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_FILE_LABEL, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_FOLDER_LABEL, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_DISABLED_REFRESH, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_REFRESH, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_DISABLED_IMPORT, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_IMPORT, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_DISABLED_EXPORT, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_EXPORT, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_COLLAPSEALL, baseURL);
		createImageDescriptor(ErlideUIConstants.IMG_PROJECT_CLOSED_LABEL,
				baseURL);

		createImageDescriptor(ErlideUIConstants.IMG_ERLANG_LOGO, baseURL);
	}

	/**
	 * @return
	 */
	public static IWorkbenchPage getActivePage() {
		final IWorkbenchWindow w = getActiveWorkbenchWindow();
		if (w != null) {
			return w.getActivePage();
		}
		return null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}

	/**
	 * Returns the active workbench shell or <code>null</code> if none
	 * 
	 * @return the active workbench shell or <code>null</code> if none
	 */
	public static Shell getActiveWorkbenchShell() {
		final IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	public static void log(final Exception e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID,
				ErlangStatusConstants.INTERNAL_ERROR, e.getMessage(), null));
	}

	public static void log(final IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void logErrorMessage(final String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID,
				ErlangStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(final String message, final IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		final MultiStatus multi = new MultiStatus(PLUGIN_ID,
				ErlangStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		log(multi);
	}

	public static void log(final Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID,
				ErlangStatusConstants.INTERNAL_ERROR, "Erlide internal error",
				e));
	}

	public static ImageDescriptorRegistry getImageDescriptorRegistry() {
		return getDefault().internalGetImageDescriptorRegistry();
	}

	private synchronized ImageDescriptorRegistry internalGetImageDescriptorRegistry() {
		if (fImageDescriptorRegistry == null) {
			fImageDescriptorRegistry = new ImageDescriptorRegistry();
		}
		return fImageDescriptorRegistry;
	}

	/**
	 * Returns the registry of the extensions to the
	 * <code>org.erlide.ui.erlangFoldingStructureProvider</code> extension
	 * point.
	 * 
	 * @return the registry of contributed
	 *         <code>IErlangFoldingStructureProvider</code>
	 */
	public synchronized ErlangFoldingStructureProviderRegistry getFoldingStructureProviderRegistry() {
		if (fFoldingStructureProviderRegistry == null) {
			fFoldingStructureProviderRegistry = new ErlangFoldingStructureProviderRegistry();
		}
		return fFoldingStructureProviderRegistry;
	}

	public static void debug(final String message) {
		if (getDefault().isDebugging()) {
			ErlLogger.debug(message);
		}
	}

	public static void createStandardGroups(final IMenuManager menu) {
		if (!menu.isEmpty()) {
			return;
		}
		menu.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
		menu.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		menu.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
	}

	/**
	 * Returns a section in the Java plugin's dialog settings. If the section
	 * doesn't exist yet, it is created.
	 * 
	 * @param name
	 *            the name of the section
	 * @return the section of the given name
	 * @since 3.2
	 */
	public IDialogSettings getDialogSettingsSection(final String name) {
		final IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(name);
		if (section == null) {
			section = dialogSettings.addNewSection(name);
		}
		return section;
	}

	public ProblemMarkerManager getProblemMarkerManager() {
		if (fProblemMarkerManager == null) {
			fProblemMarkerManager = new ProblemMarkerManager();
		}
		return fProblemMarkerManager;
	}

	public static IEclipsePreferences getPrefsNode() {
		final String qualifier = ErlideUIPlugin.PLUGIN_ID;
		final IScopeContext context = new InstanceScope();
		final IEclipsePreferences eclipsePreferences = context
				.getNode(qualifier);
		return eclipsePreferences;
	}

	private final int DUMP_INTERVAL = Integer.parseInt(System.getProperty(
			"erlide.dump.interval", "300000"));

	private ErlangConsolePage fErlangConsolePage;

	private void startPeriodicDump() {
		String env = System.getenv("erlide.internal.coredump");
		if ("true".equals(env)) {
			Job job = new Job("Erlang node info dump") {

				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					try {
						final ErlideBackend ideBackend = ErlangCore
								.getBackendManager().getIdeBackend();
						String info = ErlBackend.getSystemInfo(ideBackend);
						String sep = "\n++++++++++++++++++++++\n";
						ErlLogger.debug(sep + info + sep);
					} finally {
						schedule(DUMP_INTERVAL);
					}
					return Status.OK_STATUS;
				}

			};
			job.setPriority(Job.SHORT);
			job.setSystem(true);
			job.schedule(DUMP_INTERVAL);
		}
	}

	public ErlangConsolePage getConsolePage() {
		return fErlangConsolePage;
	}

	public void setConsolePage(final ErlangConsolePage erlangConsolePage) {
		fErlangConsolePage = erlangConsolePage;
	}
}
