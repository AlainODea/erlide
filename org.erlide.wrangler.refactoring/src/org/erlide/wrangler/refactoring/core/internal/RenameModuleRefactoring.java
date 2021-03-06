package org.erlide.wrangler.refactoring.core.internal;



import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.erlide.wrangler.refactoring.backend.ChangedFile;
import org.erlide.wrangler.refactoring.backend.IRefactoringRpcMessage;
import org.erlide.wrangler.refactoring.backend.WranglerBackendManager;
import org.erlide.wrangler.refactoring.core.SimpleOneStepWranglerRefactoring;
import org.erlide.wrangler.refactoring.selection.IErlSelection;
import org.erlide.wrangler.refactoring.util.GlobalParameters;

public class RenameModuleRefactoring extends SimpleOneStepWranglerRefactoring {

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// since any selection contains a module, it can be applied
		return new RefactoringStatus();
	}

	@Override
	public String getName() {
		return "Rename module";
	}

	@Override
	public IRefactoringRpcMessage run(IErlSelection sel) {
		return WranglerBackendManager.getRefactoringBackend().call(
				"rename_mod_eclipse", "ssxi", sel.getFilePath(), userInput,
				sel.getSearchPath(), GlobalParameters.getTabWidth());
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {

		CompositeChange c = (CompositeChange) super.createChange(pm);

		for (ChangedFile f : changedFiles) {
			if (f.isNameChanged()) {
				IPath p = f.getIPath();
				String s = f.getNewName();
				RenameResourceChange rch = new RenameResourceChange(p, s);
				c.add(rch);
			}
		}

		return c;
	}

}
