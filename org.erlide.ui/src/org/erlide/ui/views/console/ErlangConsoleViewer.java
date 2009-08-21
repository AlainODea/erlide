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
package org.erlide.ui.views.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.internal.console.IOConsoleViewer;

/**
 * Viewer used to display an Erlang console
 * 
 */
public class ErlangConsoleViewer extends IOConsoleViewer {
	public static final String OUTPUT_PARTITION_TYPE = ConsolePlugin
			.getUniqueIdentifier()
			+ ".io_console_output_partition_type"; //$NON-NLS-1$
	public static final String INPUT_PARTITION_TYPE = ConsolePlugin
			.getUniqueIdentifier()
			+ ".io_console_input_partition_type"; //$NON-NLS-1$

	final ErlangConsoleHistory history;

	public ErlangConsoleViewer(Composite parent, ErlangConsole console) {
		super(parent, console);
		history = new ErlangConsoleHistory();
	}

	@Override
	protected void createControl(Composite parent, int styles) {
		super.createControl(parent, styles);
		Control control = getControl();
		control.addKeyListener(new KeyAdapter() {
			boolean inHistory = false;

			@Override
			public void keyPressed(final KeyEvent e) {
				final IDocument doc = getDocument();
				final boolean isHistoryCommand = ((e.stateMask & SWT.CTRL) == SWT.CTRL)
						&& ((e.keyCode == SWT.ARROW_UP) || (e.keyCode == SWT.ARROW_DOWN));

				System.out.println("-----------" + doc.getLength());
				int i = 0;
				while (i <= doc.getLength()) {
					ITypedRegion p;
					try {
						p = doc.getPartition(i);
						System.out.println("> " + p.getOffset() + ":"
								+ p.getLength() + "=" + p.getType() + " '"
								+ doc.get(p.getOffset(), p.getLength()) + "'");
						if (p.getLength() == 0) {
							break;
						}
						i += p.getLength();
					} catch (BadLocationException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				System.out.println("===========");

				try {
					int length = doc.getLength() - 1;
					ITypedRegion partition = doc.getPartition(length - 1);
					if (e.keyCode == 13) {
						String text = doc.get(partition.getOffset(), partition
								.getLength());
						history.add(text.trim());
						System.out.println("ADDED " + history);
						inHistory = false;
					} else if (isHistoryCommand) {
						if (!inHistory) {
							if (e.keyCode == SWT.ARROW_UP) {
								history.gotoLast();
							} else {
								history.gotoFirst();
							}
						}
						String text = doc.get(partition.getOffset(), partition
								.getLength());
						text = history.size() == 0 ? text : history.get();
						System.out.println("MOVE " + history);
						System.out.println("   - " + text);
						if (partition.getType().equals(INPUT_PARTITION_TYPE)) {
							doc.replace(partition.getOffset(), partition
									.getLength(), text);
						} else {
							doc.replace(length, 0, text);
						}
						if (e.keyCode == SWT.ARROW_UP) {
							history.prev();
						} else if (e.keyCode == SWT.ARROW_DOWN) {
							history.next();
						} else {
						}
						inHistory = true;
						e.doit = false;
					}
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
	}
}
