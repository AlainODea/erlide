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

package org.erlide.gunit.internal.model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.erlide.core.erlang.IErlProject;
import org.erlide.gunit.internal.Messages;
import org.erlide.gunit.internal.launcher.GUnitLaunchConfigurationConstants;
import org.erlide.gunit.internal.launcher.ITestKind;
import org.erlide.gunit.internal.ui.GUnitMessages;
import org.erlide.gunit.internal.ui.GUnitPlugin;
import org.erlide.gunit.model.ITestElement;
import org.erlide.gunit.model.ITestElementContainer;
import org.erlide.gunit.model.ITestRunSession;
import org.erlide.gunit.model.TestElement;
import org.erlide.gunit.model.TestRoot;
import org.erlide.gunit.model.TestSuiteElement;
import org.erlide.gunit.model.TestElement.Status;

/**
 * A test run session holds all information about a test run, i.e. launch
 * configuration, launch, test tree (including results).
 */
public class TestRunSession implements ITestRunSession {

	/**
	 * The launch, or <code>null</code> iff this session was run externally.
	 */
	private final ILaunch fLaunch;

	private final String fTestRunName;

	/**
	 * Java project, or <code>null</code>.
	 */
	private final IErlProject fProject;

	private final ITestKind fTestRunnerKind;

	/**
	 * Test runner client or <code>null</code>.
	 */
	private RemoteTestRunnerClient fTestRunnerClient;

	private final ListenerList/* <ITestSessionListener> */fSessionListeners;

	/**
	 * The model root, or <code>null</code> if swapped to disk.
	 */
	private TestRoot fTestRoot;

	/**
	 * The test run session's cached result, or <code>null</code> if
	 * <code>fTestRoot != null</code>.
	 */
	private Result fTestResult;

	/**
	 * Map from testId to testElement.
	 */
	private HashMap<String, ITestElement> fIdToTest;

	/**
	 * The TestSuites for which additional children are expected.
	 */
	private List<IncompleteTestSuite> fIncompleteTestSuites;

	/**
	 * Suite for unrooted test case elements, or <code>null</code>.
	 */
	private TestSuiteElement fUnrootedSuite;

	/**
	 * Number of tests started during this test run.
	 */
	volatile int fStartedCount;

	/**
	 * Number of tests ignored during this test run.
	 */
	volatile int fIgnoredCount;

	/**
	 * Number of errors during this test run.
	 */
	volatile int fErrorCount;

	/**
	 * Number of failures during this test run.
	 */
	volatile int fFailureCount;

	/**
	 * Total number of tests to run.
	 */
	volatile int fTotalCount;

	/**
	 * Start time in millis.
	 */
	volatile long fStartTime;

	volatile boolean fIsRunning;

	volatile boolean fIsStopped;

	/**
	 * @param testRunName
	 * @param project
	 *            may be <code>null</code>
	 */
	public TestRunSession(final String testRunName, final IErlProject project) {
		// TODO: check assumptions about non-null fields

		this.fLaunch = null;
		this.fProject = null; // TODO

		Assert.isNotNull(testRunName);
		this.fTestRunName = testRunName;
		this.fTestRunnerKind = ITestKind.NULL; // TODO

		this.fTestRoot = new TestRoot(this);
		this.fIdToTest = new HashMap<String, ITestElement>();

		this.fTestRunnerClient = null;

		this.fSessionListeners = new ListenerList();
	}

	public TestRunSession(final ILaunch launch, final IErlProject project,
			final int port) {
		Assert.isNotNull(launch);

		this.fLaunch = launch;
		this.fProject = project;

		final ILaunchConfiguration launchConfiguration = launch
				.getLaunchConfiguration();
		if (launchConfiguration != null) {
			this.fTestRunName = launchConfiguration.getName();
			this.fTestRunnerKind = GUnitLaunchConfigurationConstants
					.getTestRunnerKind(launchConfiguration);
		} else {
			this.fTestRunName = project.getName();
			this.fTestRunnerKind = ITestKind.NULL;
		}

		this.fTestRoot = new TestRoot(this);
		this.fIdToTest = new HashMap<String, ITestElement>();

		this.fTestRunnerClient = new RemoteTestRunnerClient();
		this.fTestRunnerClient.startListening(
				new ITestRunListener2[] { new TestSessionNotifier() }, port);

		final ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		launchManager.addLaunchListener(new ILaunchesListener2() {
			public void launchesTerminated(final ILaunch[] launches) {
				if (Arrays.asList(launches).contains(
						TestRunSession.this.fLaunch)) {
					if (TestRunSession.this.fTestRunnerClient != null) {
						TestRunSession.this.fTestRunnerClient.stopWaiting();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			public void launchesRemoved(final ILaunch[] launches) {
				if (Arrays.asList(launches).contains(
						TestRunSession.this.fLaunch)) {
					if (TestRunSession.this.fTestRunnerClient != null) {
						TestRunSession.this.fTestRunnerClient.stopWaiting();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			public void launchesChanged(final ILaunch[] launches) {
			}

			public void launchesAdded(final ILaunch[] launches) {
			}
		});

		this.fSessionListeners = new ListenerList();
		addTestSessionListener(new TestRunListenerAdapter(this));
	}

	void reset() {
		this.fStartedCount = 0;
		this.fFailureCount = 0;
		this.fErrorCount = 0;
		this.fIgnoredCount = 0;
		this.fTotalCount = 0;

		this.fTestRoot = new TestRoot(this);
		this.fTestResult = null;
		this.fIdToTest = new HashMap<String, ITestElement>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.ITestRunSession#getProgressState()
	 */
	public ProgressState getProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isStopped()) {
			return ProgressState.STOPPED;
		}
		return ProgressState.COMPLETED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.model.ITestElement#getTestResult(boolean)
	 */
	public Result getTestResult(final boolean includeChildren) {
		if (this.fTestRoot != null) {
			return this.fTestRoot.getTestResult(true);
		} else {
			return this.fTestResult;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.model.ITestElementContainer#getChildren()
	 */
	public ITestElement[] getChildren() {
		return getTestRoot().getChildren();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.model.ITestElement#getFailureTrace()
	 */
	public FailureTrace getFailureTrace() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.model.ITestElement#getParentContainer()
	 */
	public ITestElementContainer getParentContainer() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.erlide.gunit.model.ITestElement#getTestRunSession()
	 */
	public ITestRunSession getTestRunSession() {
		return this;
	}

	public TestRoot getTestRoot() {
		swapIn(); // TODO: TestRoot should stay (e.g. for
		// getTestRoot().getStatus())
		return this.fTestRoot;
	}

	/**
	 * @return the Java project, or <code>null</code>
	 */
	public IErlProject getLaunchedProject() {
		return this.fProject;
	}

	public ITestKind getTestRunnerKind() {
		return this.fTestRunnerKind;
	}

	/**
	 * @return the launch, or <code>null</code> iff this session was run
	 *         externally
	 */
	public ILaunch getLaunch() {
		return this.fLaunch;
	}

	public String getTestRunName() {
		return this.fTestRunName;
	}

	public int getErrorCount() {
		return this.fErrorCount;
	}

	public int getFailureCount() {
		return this.fFailureCount;
	}

	public int getStartedCount() {
		return this.fStartedCount;
	}

	public int getIgnoredCount() {
		return this.fIgnoredCount;
	}

	public int getTotalCount() {
		return this.fTotalCount;
	}

	public long getStartTime() {
		return this.fStartTime;
	}

	/**
	 * @return <code>true</code> iff the session has been stopped or terminated
	 */
	public boolean isStopped() {
		return this.fIsStopped;
	}

	public void addTestSessionListener(final ITestSessionListener listener) {
		swapIn();
		this.fSessionListeners.add(listener);
	}

	public void removeTestSessionListener(final ITestSessionListener listener) {
		this.fSessionListeners.remove(listener);
	}

	public void swapOut() {
		if (this.fTestRoot == null) {
			return;
		}
		if (isRunning() || isStarting() || isKeptAlive()) {
			return;
		}

		final Object[] listeners = this.fSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITestSessionListener registered = (ITestSessionListener) listeners[i];
			if (!registered.acceptsSwapToDisk()) {
				return;
			}
		}

		try {
			final File swapFile = getSwapFile();

			GUnitModel.exportTestRunSession(this, swapFile);
			this.fTestResult = this.fTestRoot.getTestResult(true);
			this.fTestRoot = null;
			this.fTestRunnerClient = null;
			this.fIdToTest = new HashMap<String, ITestElement>();
			this.fIncompleteTestSuites = null;
			this.fUnrootedSuite = null;

		} catch (final IllegalStateException e) {
			GUnitPlugin.log(e);
		} catch (final CoreException e) {
			GUnitPlugin.log(e);
		}
	}

	private boolean isStarting() {
		return getStartTime() == 0 && this.fLaunch != null
				&& !this.fLaunch.isTerminated();
	}

	public void removeSwapFile() {
		final File swapFile = getSwapFile();
		if (swapFile.exists()) {
			swapFile.delete();
		}
	}

	private File getSwapFile() throws IllegalStateException {
		final File historyDir = GUnitPlugin.getHistoryDirectory();
		final String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS").format(new Date(getStartTime())); //$NON-NLS-1$
		final String swapFileName = isoTime + ".xml"; //$NON-NLS-1$
		return new File(historyDir, swapFileName);
	}

	public void swapIn() {
		if (this.fTestRoot != null) {
			return;
		}

		try {
			GUnitModel.importIntoTestRunSession(getSwapFile(), this);
		} catch (final IllegalStateException e) {
			GUnitPlugin.log(e);
			this.fTestRoot = new TestRoot(this);
			this.fTestResult = null;
		} catch (final CoreException e) {
			GUnitPlugin.log(e);
			this.fTestRoot = new TestRoot(this);
			this.fTestResult = null;
		}
	}

	public void stopTestRun() {
		if (isRunning() || !isKeptAlive()) {
			this.fIsStopped = true;
		}
		if (this.fTestRunnerClient != null) {
			this.fTestRunnerClient.stopTest();
		}
	}

	/**
	 * @return <code>true</code> iff the runtime VM of this test session is
	 *         still alive
	 */
	public boolean isKeptAlive() {
		if (this.fTestRunnerClient != null
				&& this.fLaunch != null
				&& this.fTestRunnerClient.isRunning()
				&& ILaunchManager.DEBUG_MODE.equals(this.fLaunch
						.getLaunchMode())) {
			final ILaunchConfiguration config = this.fLaunch
					.getLaunchConfiguration();
			try {
				return config != null
						&& config
								.getAttribute(
										GUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING,
										false);
			} catch (final CoreException e) {
				return false;
			}

		} else {
			return false;
		}
	}

	/**
	 * @return <code>true</code> iff this session has been started, but not
	 *         ended nor stopped nor terminated
	 */
	public boolean isRunning() {
		return this.fIsRunning;
	}

	/**
	 * @param testId
	 * @param className
	 * @param testName
	 * @param launchMode
	 * @return <code>false</code> iff the rerun could not be started
	 * @throws CoreException
	 */
	public boolean rerunTest(final String testId, final String className,
			final String testName, final String launchMode)
			throws CoreException {
		if (isKeptAlive()) {
			final Status status = ((TestCaseElement) getTestElement(testId))
					.getStatus();
			if (status == Status.ERROR) {
				this.fErrorCount--;
			} else if (status == Status.FAILURE) {
				this.fFailureCount--;
			}
			this.fTestRunnerClient.rerunTest(testId, className, testName);
			return true;

		} else if (this.fLaunch != null) {
			// run the selected test using the previous launch configuration
			final ILaunchConfiguration launchConfiguration = this.fLaunch
					.getLaunchConfiguration();
			if (launchConfiguration != null) {

				String name = className;
				if (testName != null) {
					name += "." + testName; //$NON-NLS-1$
				}
				final String configName = Messages.format(
						GUnitMessages.TestRunnerViewPart_configName, name);
				final ILaunchConfigurationWorkingCopy tmp = launchConfiguration
						.copy(configName);
				// fix for bug: 64838 junit view run single test does not use
				// correct class [JUnit]
				// tmp.setAttribute(
				// IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				// className);

				// reset the container
				tmp.setAttribute(
						GUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER,
						""); //$NON-NLS-1$
				if (testName != null) {
					tmp
							.setAttribute(
									GUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
									testName);
					// String args= "-rerun "+testId;
					// tmp.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
					// args);
				}
				tmp.launch(launchMode, null);
				return true;
			}
		}

		return false;
	}

	public TestElement getTestElement(final String id) {
		return (TestElement) this.fIdToTest.get(id);
	}

	private TestElement addTreeEntry(final String treeEntry) {
		// format: testId","testName","isSuite","testcount
		final int index0 = treeEntry.indexOf(',');
		final String id = treeEntry.substring(0, index0);

		final StringBuffer testNameBuffer = new StringBuffer(100);
		final int index1 = scanTestName(treeEntry, index0 + 1, testNameBuffer);
		final String testName = testNameBuffer.toString().trim();

		final int index2 = treeEntry.indexOf(',', index1 + 1);
		final boolean isSuite = treeEntry.substring(index1 + 1, index2).equals(
				"true"); //$NON-NLS-1$

		final int testCount = Integer.parseInt(treeEntry.substring(index2 + 1));

		if (this.fIncompleteTestSuites.isEmpty()) {
			return createTestElement(this.fTestRoot, id, testName, isSuite,
					testCount);
		} else {
			final int suiteIndex = this.fIncompleteTestSuites.size() - 1;
			final IncompleteTestSuite openSuite = this.fIncompleteTestSuites
					.get(suiteIndex);
			openSuite.fOutstandingChildren--;
			if (openSuite.fOutstandingChildren <= 0) {
				this.fIncompleteTestSuites.remove(suiteIndex);
			}
			return createTestElement(openSuite.fTestSuiteElement, id, testName,
					isSuite, testCount);
		}
	}

	public TestElement createTestElement(final TestSuiteElement parent,
			final String id, final String testName, final boolean isSuite,
			final int testCount) {
		TestElement testElement;
		if (isSuite) {
			final TestSuiteElement testSuiteElement = new TestSuiteElement(
					parent, id, testName, testCount);
			testElement = testSuiteElement;
			if (testCount > 0) {
				this.fIncompleteTestSuites.add(new IncompleteTestSuite(
						testSuiteElement, testCount));
			}
		} else {
			testElement = new TestCaseElement(parent, id, testName);
		}
		this.fIdToTest.put(id, testElement);
		return testElement;
	}

	/**
	 * Append the test name from <code>s</code> to <code>testName</code>.
	 * 
	 * @param s
	 *            the string to scan
	 * @param start
	 *            the offset of the first character in <code>s</code>
	 * @param testName
	 *            the result
	 * 
	 * @return the index of the next ','
	 */
	private int scanTestName(final String s, final int start,
			final StringBuffer testName) {
		boolean inQuote = false;
		int i = start;
		for (; i < s.length(); i++) {
			final char c = s.charAt(i);
			if (c == '\\' && !inQuote) {
				inQuote = true;
				continue;
			} else if (inQuote) {
				inQuote = false;
				testName.append(c);
			} else if (c == ',') {
				break;
			} else {
				testName.append(c);
			}
		}
		return i;
	}

	/**
	 * An {@link ITestRunListener2} that listens to events from the
	 * {@link RemoteTestRunnerClient} and translates them into high-level model
	 * events (broadcasted to {@link ITestSessionListener}s).
	 */
	private class TestSessionNotifier implements ITestRunListener2 {

		public void testRunStarted(final int testCount) {
			TestRunSession.this.fIncompleteTestSuites = new ArrayList<IncompleteTestSuite>();

			TestRunSession.this.fStartedCount = 0;
			TestRunSession.this.fIgnoredCount = 0;
			TestRunSession.this.fFailureCount = 0;
			TestRunSession.this.fErrorCount = 0;
			TestRunSession.this.fTotalCount = testCount;

			TestRunSession.this.fStartTime = System.currentTimeMillis();
			TestRunSession.this.fIsRunning = true;

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionStarted();
			}
		}

		public void testRunEnded(final long elapsedTime) {
			TestRunSession.this.fIsRunning = false;

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionEnded(elapsedTime);
			}
		}

		public void testRunStopped(final long elapsedTime) {
			TestRunSession.this.fIsRunning = false;
			TestRunSession.this.fIsStopped = true;

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.sessionStopped(elapsedTime);
			}
		}

		public void testRunTerminated() {
			TestRunSession.this.fIsRunning = false;
			TestRunSession.this.fIsStopped = true;

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionTerminated();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.erlide.gunit.internal.model.ITestRunListener2#testTreeEntry(java
		 * .lang.String)
		 */
		public void testTreeEntry(final String description) {
			final TestElement testElement = addTreeEntry(description);

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testAdded(testElement);
			}
		}

		private TestElement createUnrootedTestElement(final String testId,
				final String testName) {
			final TestSuiteElement unrootedSuite = getUnrootedSuite();
			final TestElement testElement = createTestElement(unrootedSuite,
					testId, testName, false, 1);

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testAdded(testElement);
			}

			return testElement;
		}

		private TestSuiteElement getUnrootedSuite() {
			if (TestRunSession.this.fUnrootedSuite == null) {
				TestRunSession.this.fUnrootedSuite = (TestSuiteElement) createTestElement(
						TestRunSession.this.fTestRoot,
						"-2", GUnitMessages.TestRunSession_unrootedTests, true, 0); //$NON-NLS-1$
			}
			return TestRunSession.this.fUnrootedSuite;
		}

		public void testStarted(final String testId, final String testName) {
			if (TestRunSession.this.fStartedCount == 0) {
				final Object[] listeners = TestRunSession.this.fSessionListeners
						.getListeners();
				for (int i = 0; i < listeners.length; ++i) {
					((ITestSessionListener) listeners[i]).runningBegins();
				}
			}
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;
			setStatus(testCaseElement, Status.RUNNING);

			TestRunSession.this.fStartedCount++;

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.testStarted(testCaseElement);
			}
		}

		public void testEnded(final String testId, final String testName) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (testName.startsWith(MessageIds.IGNORED_TEST_PREFIX)) {
				testCaseElement.setIgnored(true);
				TestRunSession.this.fIgnoredCount++;
			}

			if (testCaseElement.getStatus() == Status.RUNNING) {
				setStatus(testCaseElement, Status.OK);
			}

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.testEnded(testCaseElement);
			}
		}

		@SuppressWarnings("unused")
		public void testFailed(final int status, final String testId,
				final String testName, final String trace) {
			testFailed(status, testId, testName, trace, null, null);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.erlide.gunit.internal.model.ITestRunListener2#testFailed(int,
		 * java.lang.String, java.lang.String, java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		public void testFailed(final int statusCode, final String testId,
				final String testName, final String trace,
				final String expected, final String actual) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
				return;
			}

			final Status status = Status.convert(statusCode);
			registerTestFailed(testElement, status, trace,
					nullifyEmpty(expected), nullifyEmpty(actual));

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testFailed(testElement,
						status, trace, expected, actual);
			}
		}

		private String nullifyEmpty(final String string) {
			final int length = string.length();
			if (length == 0) {
				return null;
			} else if (string.charAt(length - 1) == '\n') {
				return string.substring(0, length - 1);
			} else {
				return string;
			}
		}

		@SuppressWarnings("unused")
		public void testReran(final String testId, final String testClass,
				final String testName, final int status, final String trace) {
			testReran(testId, testClass, testName, status, trace, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.erlide.gunit.internal.model.ITestRunListener2#testReran(java.
		 * lang.String, java.lang.String, java.lang.String, int,
		 * java.lang.String, java.lang.String, java.lang.String)
		 */
		public void testReran(final String testId, final String className,
				final String testName, final int statusCode,
				final String trace, final String expectedResult,
				final String actualResult) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;

			final Status status = Status.convert(statusCode);
			registerTestFailed(testElement, status, trace,
					nullifyEmpty(expectedResult), nullifyEmpty(actualResult));

			final Object[] listeners = TestRunSession.this.fSessionListeners
					.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				// TODO: post old & new status?
				((ITestSessionListener) listeners[i]).testReran(
						testCaseElement, status, trace, expectedResult,
						actualResult);
			}
		}

		private void logUnexpectedTest(final String testId,
				final TestElement testElement) {
			GUnitPlugin
					.log(new Exception(
							"Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class IncompleteTestSuite {
		public TestSuiteElement fTestSuiteElement;

		public int fOutstandingChildren;

		public IncompleteTestSuite(final TestSuiteElement testSuiteElement,
				final int outstandingChildren) {
			this.fTestSuiteElement = testSuiteElement;
			this.fOutstandingChildren = outstandingChildren;
		}
	}

	public void registerTestFailed(final TestElement testElement,
			final Status status, final String trace, final String expected,
			final String actual) {
		testElement.setStatus(status, trace, expected, actual);
		if (status.isError()) {
			this.fErrorCount++;
		} else {
			this.fFailureCount++;
		}
	}

	public void registerTestEnded(final TestElement testElement,
			final boolean completed) {
		if (testElement instanceof TestCaseElement) {
			this.fTotalCount++;
			if (!completed) {
				return;
			}
			this.fStartedCount++;
			if (((TestCaseElement) testElement).isIgnored()) {
				this.fIgnoredCount++;
			}
			if (!testElement.getStatus().isErrorOrFailure()) {
				setStatus(testElement, Status.OK);
			}
		}
	}

	private void setStatus(final TestElement testElement, final Status status) {
		testElement.setStatus(status);
	}

	public TestElement[] getAllFailedTestElements() {
		final List<ITestElement> failures = new ArrayList<ITestElement>();
		addFailures(failures, getTestRoot());
		return failures.toArray(new TestElement[failures.size()]);
	}

	private void addFailures(final List<ITestElement> failures,
			final ITestElement testElement) {
		final Result testResult = testElement.getTestResult(true);
		if (testResult == Result.ERROR || testResult == Result.FAILURE) {
			failures.add(testElement);
		}
		if (testElement instanceof TestSuiteElement) {
			final TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
			final ITestElement[] children = testSuiteElement.getChildren();
			for (int i = 0; i < children.length; i++) {
				addFailures(failures, children[i]);
			}
		}
	}

}
