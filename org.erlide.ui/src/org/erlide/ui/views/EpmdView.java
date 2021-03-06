package org.erlide.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.jinterface.util.EpmdWatcher;
import org.erlide.jinterface.util.IEpmdListener;

public class EpmdView extends ViewPart implements IEpmdListener {

	TreeViewer treeViewer;

	class TreeContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {

		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(final Object inputElement) {
			model = epmdWatcher.getData();
			if (model == null) {
				return new Object[] {};
			}
			return model.keySet().toArray();
		}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof String) {
				final String host = (String) parentElement;
				final List<String> h = model.get(host);
				final List<String> res = new ArrayList<String>();
				if (h != null) {
					for (String s : h) {
						if (!s.startsWith("jerlide_")) {
							res.add(s);
						}
					}
					return res.toArray();
				}
			}
			return new Object[] {};
		}

		public Object getParent(final Object element) {
			return null;
		}

		public boolean hasChildren(final Object element) {
			return getChildren(element).length > 0;
		}

	}

	class TreeLabelProvider extends LabelProvider {
		@Override
		public String getText(final Object element) {
			return super.getText(element);
		}

		@Override
		public Image getImage(final Object element) {
			return null;
		}
	}

	Map<String, List<String>> model;
	EpmdWatcher epmdWatcher;

	public EpmdView() {
		epmdWatcher = ErlangCore.getBackendManager().getEpmdWatcher();
		epmdWatcher.addEpmdListener(this);
	}

	@Override
	public void createPartControl(final Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.BORDER);
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				treeViewer.refresh();
			}
		});
		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setLabelProvider(new TreeLabelProvider());
		treeViewer.setAutoExpandLevel(2);
		initializeToolBar();

		treeViewer.setInput(this);
	}

	@Override
	public void setFocus() {
	}

	private void initializeToolBar() {
		// IToolBarManager toolBarManager = getViewSite().getActionBars()
		// .getToolBarManager();
	}

	public void updateNodeStatus(final String host,
			final Collection<String> started, final Collection<String> stopped) {
		model = epmdWatcher.getData();
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				treeViewer.setInput(model);
			}
		});
	}
}
