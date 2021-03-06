/*******************************************************************************
 * Copyright (c) 2009 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.jinterface;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.UIJob;
import org.erlide.jinterface.backend.BackendException;
import org.erlide.jinterface.rpc.RpcException;
import org.erlide.jinterface.rpc.RpcFuture;
import org.erlide.ui.ErlideUIPlugin;

public abstract class AsyncCaller<T> implements Runnable {
	long interval;

	public AsyncCaller() {
		this(100);
	}

	public AsyncCaller(long interval) {
		this.interval = interval;
	}

	protected abstract T prepare();

	protected abstract RpcFuture call() throws BackendException;

	protected abstract void handleResult(T context, RpcFuture result);

	public void run() {
		final T context = prepare();
		try {
			final RpcFuture result = call();
			if (result == null) {
				return;
			}
			Job job = new UIJob("async call updater") {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						if (result.get(1) == null) {
							schedule(interval);
						}
					} catch (RpcException e) {
						e.printStackTrace();
					}
					handleResult(context, result);
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					return new Status(IStatus.OK, ErlideUIPlugin.PLUGIN_ID,
							"done");
				}
			};
			job.schedule(interval);
		} catch (BackendException e) {
			e.printStackTrace();
		}

	}

}
