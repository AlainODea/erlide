package org.erlide.ui.views;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.erlide.core.erlang.util.ErlideUtil;
import org.erlide.jinterface.util.Bindings;
import org.erlide.jinterface.util.ErlUtils;
import org.erlide.jinterface.util.ParserException;
import org.erlide.jinterface.util.TermParser;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangException;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class CrashLogView extends ViewPart {

	TreeViewer viewer;
	private Action openFileaction;
	private Action action2;
	Action doubleClickAction;

	public static class CrashLog {
		List<LogEntry> items;

		public CrashLog(String newInput) throws IOException {
			items = new ArrayList<LogEntry>();
			BufferedReader in = new BufferedReader(new FileReader(newInput));
			String line;
			String item = "";
			boolean insideItem = false;
			line = in.readLine();
			while (line != null) {
				if (insideItem) {
					item += line;
					if (item.contains("}.")) {
						insideItem = false;
						items.add(new LogEntry(item));
						item = "";
					}
				} else {
					if (line.contains(" : info: {erlide_monitor,")) {
						insideItem = true;
						item += line;
					}
				}
				line = in.readLine();
			}
		}

		public LogEntry[] getItems() {
			return items.toArray(new LogEntry[items.size()]);
		}

	}

	public static class LogEntry {
		Date time;
		List<OtpErlangObject> processes;
		List<OtpErlangObject> ets;
		List<OtpErlangObject> memory;
		List<OtpErlangObject> stats;

		public LogEntry(String item) {
			int x = item.indexOf("{erlide_monitor,");
			try {
				String str = item.substring(x, item.length() - 1);
				OtpErlangTuple data = (OtpErlangTuple) TermParser.parse(str);
				OtpErlangList elems = (OtpErlangList) data.elementAt(2);
				for (OtpErlangObject elem : elems.elements()) {
					fillData((OtpErlangTuple) elem);
				}
			} catch (ParserException e) {
				System.out.println(e.getMessage());
			}
		}

		private void fillData(OtpErlangTuple elem) {
			OtpErlangAtom akey = (OtpErlangAtom) elem.elementAt(0);
			OtpErlangObject val = elem.elementAt(1);
			String key = akey.atomValue();
			if ("time".equals(key)) {
				try {
					Bindings b = ErlUtils.match("{{Y,Mo,D},{H,M,S}}", val);
					Calendar c = Calendar.getInstance();
					c.set(b.getInt("Y"), b.getInt("Mo"), b.getInt("D"), b
							.getInt("H"), b.getInt("M"), b.getInt("S"));
					time = c.getTime();
				} catch (ParserException e) {
					e.printStackTrace();
				} catch (OtpErlangException e) {
					e.printStackTrace();
				}
			} else if ("processes".equals(key)) {
				OtpErlangObject[] ps = ((OtpErlangList) val).elements();
				processes = Arrays.asList(ps);
			} else if ("ets".equals(key)) {
				OtpErlangObject[] ps = ((OtpErlangList) val).elements();
				ets = Arrays.asList(ps);
			} else if ("memory".equals(key)) {
				OtpErlangObject[] ps = ((OtpErlangList) val).elements();
				memory = Arrays.asList(ps);
			} else if ("stats".equals(key)) {
				OtpErlangObject[] ps = ((OtpErlangList) val).elements();
				stats = Arrays.asList(ps);
			}
		}
	}

	public static class LogItem {

		private final String name;
		private final List<OtpErlangObject> items;

		public LogItem(String string, List<OtpErlangObject> items) {
			name = string;
			this.items = items;
		}

	}

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	static class ViewContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {
		private CrashLog log = null;

		public void inputChanged(final Viewer v, final Object oldInput,
				final Object newInput) {
			if (newInput instanceof String) {
				// a new log is to be loaded
				try {
					log = new CrashLog((String) newInput);
				} catch (IOException e) {
					log = null;
				}
			}
		}

		public void dispose() {
		}

		public Object[] getElements(final Object parent) {
			if (log == null) {
				return new String[] { "No data" };
			}
			return log.getItems();
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof CrashLog) {
				return ((CrashLog) parent).getItems();
			}
			if (parent instanceof LogEntry) {
				LogEntry entry = (LogEntry) parent;
				return new LogItem[] {
						new LogItem("processes", entry.processes),
						new LogItem("ets", entry.ets),
						new LogItem("memory", entry.memory),
						new LogItem("statistics", entry.stats) };
			}
			if (parent instanceof LogItem) {
				LogItem item = (LogItem) parent;
				List<String> parts = new ArrayList<String>();
				for (OtpErlangObject el : item.items) {
					parts.add(el.toString());
				}
				return parts.toArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}
	}

	static class ViewLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof LogEntry) {
				LogEntry item = (LogEntry) element;
				return "Entry @ "
						+ DateFormat.getDateTimeInstance().format(item.time);
			}
			if (element instanceof LogItem) {
				LogItem item = (LogItem) element;
				return item.name;
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(final Object obj) {
			return null;
			// return PlatformUI.getWorkbench().getSharedImages().getImage(
			// ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	/**
	 * The constructor.
	 */
	public CrashLogView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(final Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(getViewSite());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		final MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				CrashLogView.this.fillContextMenu(manager);
			}
		});
		final Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(final IMenuManager manager) {
		manager.add(openFileaction);
		manager.add(new Separator());
		manager.add(action2);
	}

	void fillContextMenu(final IMenuManager manager) {
		manager.add(action2);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(final IToolBarManager manager) {
		manager.add(openFileaction);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void makeActions() {
		openFileaction = new Action() {
			@Override
			public void run() {
				loadLogFile();
			}

		};
		openFileaction.setText("Open a log file");
		openFileaction.setToolTipText("Open a log file");
		openFileaction.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_OBJ_FOLDER));

		action2 = new Action() {
			@Override
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		doubleClickAction = new Action() {
			@Override
			public void run() {
				final ISelection selection = viewer.getSelection();
				final Object obj = ((IStructuredSelection) selection)
						.getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	protected void loadLogFile() {
		FileDialog dlg = new FileDialog(getViewSite().getShell());
		dlg.setFilterPath(ErlideUtil.getReportLocation());
		dlg.setFilterExtensions(new String[] { "*.txt" });
		String name = dlg.open();
		viewer.setInput(name);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	void showMessage(final String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(),
				"CrashLog view", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
