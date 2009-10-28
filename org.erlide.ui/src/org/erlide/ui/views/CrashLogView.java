package org.erlide.ui.views;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
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
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
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
	TableViewer viewer;
	private Action openFileaction;
	private Action action2;
	Action doubleClickAction;

	public static class CrashLog {
		List<LogItem> items;

		public CrashLog(String newInput) throws IOException {
			items = new ArrayList<LogItem>();
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
						items.add(new LogItem(item));
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

		public LogItem[] getItems() {
			return items.toArray(new LogItem[items.size()]);
		}

	}

	public static class LogItem {
		Date time;
		List<OtpErlangObject> processes;
		List<OtpErlangObject> ets;
		List<OtpErlangObject> memory;
		List<OtpErlangObject> stats;

		public LogItem(String item) {
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
					c.set(b.getInt("Y"), b.getInt("Mo"), b.getInt("D"), b.getInt("H"), b.getInt("M"), b.getInt("S"));
					time = c.getTime();
				} catch (ParserException e) {
					e.printStackTrace();
				} catch (OtpErlangException e) {
					e.printStackTrace();
				}
			} else if ("processes".equals(key)) {

			} else if ("ets".equals(key)) {

			} else if ("memory".equals(key)) {

			} else if ("stats".equals(key)) {

			}
		}
	}

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	static class ViewContentProvider implements IStructuredContentProvider {
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
	}

	static class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		
		@Override
		public String getText(Object element) {
			if(element instanceof LogItem){
				LogItem item = (LogItem) element;
				return DateFormat.getDateTimeInstance().format(item.time);
			}
			return super.getText(element);
		}
		
		public String getColumnText(final Object obj, final int index) {
			return getText(obj);
		}

		public Image getColumnImage(final Object obj, final int index) {
			return getImage(obj);
		}

		@Override
		public Image getImage(final Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(
					ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	static class NameSorter extends ViewerSorter {
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
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
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
