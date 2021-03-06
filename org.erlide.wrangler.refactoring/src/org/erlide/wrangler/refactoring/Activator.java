package org.erlide.wrangler.refactoring;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.jinterface.backend.BackendException;
import org.erlide.jinterface.backend.ErlangCode;
import org.erlide.jinterface.rpc.RpcResult;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.runtime.backend.ErlideBackend;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	/**
	 * The plug-in ID.
	 */
	public static final String PLUGIN_ID = "org.erlide.wrangler.refactoring";

	// The shared instance
	private static Activator plugin;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/**
	 * Loads the necessary *.ebin files to the Erlang node for the plug-in.
	 * 
	 * @throws CoreException
	 *             detailed exception about the loading process errors
	 */
	private void initWrangler() throws CoreException {
		try {
			Path pluginPath = getPluginPath();
			IPath wranglerRootPath = pluginPath.append("wrangler");
			String wranglerEbinPath = wranglerRootPath.append("ebin")
					.toOSString();
			// FIXME: grant that the source is shipped with the release or move
			// the necessary files somewhere else

			String wranglerSrcPath = wranglerRootPath.append("erl")
					.toOSString();

			ErlLogger
					.debug("Wrangler beam files found at: " + wranglerEbinPath);

			ErlideBackend mb = ErlangCore.getBackendManager().getIdeBackend();

			ErlLogger.debug("Managed backend found:" + mb.getJavaNodeName());

			ErlangCode.addPathA(mb, wranglerEbinPath);
			ErlangCode.addPathA(mb, wranglerSrcPath);
			ErlLogger.debug("Wrangler path has been added.");
			// ErlangCode.addPathA(mb, wranglerRootPath.toOSString());

			RpcResult res = mb.call_noexception("code", "load_file", "a",
					"wrangler");
			res = mb.call_noexception("code", "load_file", "a", "refac_util");
			ErlLogger.debug("Wrangler's path is added to Erlang with result:"
					+ res.isOk() + "\t raw:" + res);

			mb.call("application", "load", "a", "wrangler_app");
			// application:start(wrangler_app)
			mb.call("application", "start", "a", "wrangler_app");

			ErlLogger.debug("Wrangler app started:\n" + res);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
					"Could not load the ebin files!"));
		} catch (BackendException e) {
			e.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
					"Could not reach the erlang node!"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Path getPluginPath() throws IOException {
		URL url;
		Bundle b = getDefault().getBundle();
		url = FileLocator.find(b, new Path(""), null);
		url = FileLocator.resolve(url);

		ErlLogger.debug("Wrangler installation found at: " + url);

		Path pluginPath = new Path(url.getPath());
		return pluginPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		initWrangler();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

}
