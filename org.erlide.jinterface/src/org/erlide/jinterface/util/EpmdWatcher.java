/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.jinterface.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ericsson.otp.erlang.OtpEpmd;

/**
 * Periodically, query epmd to see if there are any new nodes that have been
 * registered.
 * 
 */
public final class EpmdWatcher {

	public EpmdWatcher() {
		try {
			addHost(InetAddress.getLocalHost().getHostName());
		} catch (final UnknownHostException e) {
			addHost("localhost");
		}
	}

	private final List<String> hosts = new ArrayList<String>();
	private final Map<String, List<String>> nodeMap = new HashMap<String, List<String>>();
	private final List<IEpmdListener> listeners = new ArrayList<IEpmdListener>();
	private final Map<String, List<IEpmdMonitor>> monitors = new HashMap<String, List<IEpmdMonitor>>();
	private boolean epmdStarted = false;

	synchronized public void addHost(final String host) {
		if (this.hosts.contains(host)) {
			return;
		}
		this.hosts.add(host);
		this.nodeMap.put(host, new ArrayList<String>());
	}

	synchronized public void removeHost(final String host) {
		this.hosts.remove(host);
		this.nodeMap.remove(host);
	}

	public synchronized void checkEpmd() {
		for (final Entry<String, List<String>> entry : this.nodeMap.entrySet()) {
			try {
				final String host = entry.getKey();
				final List<String> nodes = entry.getValue();

				final String[] names = OtpEpmd.lookupNames(InetAddress
						.getByName(host));
				final List<String> labels = clean(Arrays.asList(names));

				final List<String> started = getDiff(labels, nodes);
				final List<String> stopped = getDiff(nodes, labels);

				if (started.size() > 0 || stopped.size() > 0) {
					for (final IEpmdListener listener : this.listeners) {
						listener.updateNodeStatus(host, started, stopped);
					}
					for (final String s : started) {
						final List<IEpmdMonitor> ms = this.monitors.get(s);
						if (ms != null) {
							for (final IEpmdMonitor m : ms) {
								m.nodeUp(s);
							}
						}
					}
					for (final String s : stopped) {
						final List<IEpmdMonitor> ms = this.monitors.get(s);
						if (ms != null) {
							for (final IEpmdMonitor m : ms) {
								m.nodeDown(s);
							}
						}
					}
				}

				entry.setValue(labels);
				this.epmdStarted = true;
			} catch (final IOException e) {
				if (this.epmdStarted) {
					final String msg = "Erlide warning: epmd daemon went down on host "
							+ entry.getKey() + "...";
					// InterfacePlugin.getDefault().getLog().log(
					// new Status(IStatus.WARNING,
					// InterfacePlugin.PLUGIN_ID, msg));
					ErlLogger.warn(msg);
					this.epmdStarted = false;
				}
			}
		}
	}

	/**
	 * Register interest in all changes of node status
	 * 
	 * @param listener
	 */
	public void addEpmdListener(final IEpmdListener listener) {
		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	/**
	 * Unregister interest in all changes of node status
	 * 
	 * @param listener
	 */
	public void removeEpmdListener(final IEpmdListener listener) {
		this.listeners.remove(listener);
	}

	public static List<String> clean(final List<String> list) {
		final List<String> result = new ArrayList<String>();
		for (final String label : list) {
			if ("".equals(label)) {
				continue;
			}
			// label is "name X at port N"
			final String[] parts = label.split(" ");
			if (parts.length == 5) {
				String alabel = parts[1];
				if (alabel.length() == 0) {
					alabel = "??" + label;
				}
				result.add(alabel);
			}
		}
		return result;
	}

	private List<String> getDiff(final List<String> list1,
			final List<String> list2) {
		final List<String> result = new ArrayList<String>(list1);
		result.removeAll(list2);
		return result;
	}

	public Map<String, List<String>> getData() {
		return this.nodeMap;
	}

	/**
	 * Register interest in the status of a certain node.
	 * 
	 * @param node
	 * @param monitor
	 */
	public void addMonitor(final String node, final IEpmdMonitor monitor) {
		List<IEpmdMonitor> mons = this.monitors.get(node);
		if (mons == null) {
			mons = new ArrayList<IEpmdMonitor>();
		}
		if (mons.contains(monitor)) {
			return;
		}
		mons.add(monitor);
		this.monitors.put(node, mons);
	}

	/**
	 * Unregister interest in the status of a certain node.
	 * 
	 * @param node
	 * @param monitor
	 */
	public void removeMonitor(final String node, final IEpmdMonitor monitor) {
		final List<IEpmdMonitor> mons = this.monitors.get(node);
		if (mons == null) {
			return;
		}
		if (mons.contains(monitor)) {
			mons.remove(monitor);
			this.monitors.put(node, mons);
		}
	}

	public static boolean findRunningNode(final String nodeName) {
		try {
			final String[] names = OtpEpmd.lookupNames();
			final List<String> labels = EpmdWatcher.clean(Arrays.asList(names));
			for (final String name : labels) {
				if (name.equals(nodeName)) {
					return true;
				}
			}
		} catch (final IOException e) {
		}
		return false;
	}

}
