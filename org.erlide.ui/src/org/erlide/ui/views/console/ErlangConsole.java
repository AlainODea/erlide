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
package org.erlide.ui.views.console;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.backend.BackendShell;
import org.erlide.jinterface.backend.BackendShellListener;
import org.erlide.jinterface.backend.IDisposable;
import org.erlide.jinterface.backend.console.IoRequest;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.runtime.backend.ErlideBackend;

public class ErlangConsole extends IOConsole implements BackendShellListener,
		IDisposable {

	private Backend backend;
	private final IOConsoleOutputStream stdout;
	private final IOConsoleOutputStream stderr;
	private final IOConsoleOutputStream output;
	private final BackendShell shell;

	// private final ErlangConsolePartitioner partitioner;

	public ErlangConsole(ErlideBackend backend, String name,
			ImageDescriptor descriptor) {
		super(name, descriptor);

		// partitioner = new ErlangConsolePartitioner();
		// partitioner.connect(getDocument());

		this.backend = backend;
		shell = backend.getShell("main");
		shell.addListener(this);

		stdout = newOutputStream();
		stdout.setColor(new Color(Display.getCurrent(), 0, 120, 0));
		stderr = newOutputStream();
		stderr.setColor(new Color(Display.getCurrent(), 120, 0, 0));
		output = newOutputStream();
		output.setColor(new Color(Display.getCurrent(), 0, 0, 120));

		IOConsoleInputStream inputStream = getInputStream();
		inputStream.setColor(new Color(Display.getCurrent(), 0, 0, 0));

		InputReadJob readJob = new InputReadJob(inputStream);
		readJob.setSystem(true);
		readJob.schedule();
	}

	@Override
	public IPageBookViewPage createPage(IConsoleView view) {
		return new ErlangConsolePage(this, view);
	}

	public void show() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		IConsoleView view;
		try {
			view = (IConsoleView) page.showView(id);
			view.display(this);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// @Override
	// protected IConsoleDocumentPartitioner getPartitioner() {
	// return partitioner;
	// }
	//
	// @Override
	// public void clearConsole() {
	// if (partitioner != null) {
	// // partitioner.clearBuffer();
	// }
	// super.clearConsole();
	// }

	public void shellEvent(BackendShell aShell, IoRequest req) {
		if (aShell != shell || req == null) {
			return;
		}
		try {
			switch (req.getKind()) {
			case HEADER:
			case PROMPT:
			case OUTPUT:
				output.write(req.getMessage());
				break;
			case STDOUT:
				stdout.write(req.getMessage());
				break;
			case STDERR:
				stderr.write(req.getMessage());
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void dispose() {
		try {
			stdout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			stderr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		backend = null;
	}

	private class InputReadJob extends Job {

		private final IOConsoleInputStream stream;

		InputReadJob(IOConsoleInputStream inputStream) {
			super("Process Console Input Job"); //$NON-NLS-1$
			this.stream = inputStream;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				byte[] b = new byte[1024];
				int read = 0;
				while (stream != null && read >= 0) {
					read = stream.read(b);
					if (read > 0) {
						String s = new String(b, 0, read);
						shell.input(s);
						shell.send(s);
					}
				}
			} catch (IOException e) {
				ErlLogger.error(e);
			}
			return Status.OK_STATUS;
		}
	}

	public BackendShell getShell() {
		return shell;
	}

}
