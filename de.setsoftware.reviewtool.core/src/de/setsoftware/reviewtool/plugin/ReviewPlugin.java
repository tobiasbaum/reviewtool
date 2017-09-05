package de.setsoftware.reviewtool.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.config.ConfigurationInterpreter;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.connectors.file.FilePersistence;
import de.setsoftware.reviewtool.connectors.file.FileTicketConnectorConfigurator;
import de.setsoftware.reviewtool.connectors.jira.JiraConnectorConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.BasicIrrelevanceFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.pathfilters.PathIrrelevanceFilterConfigurator;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.FileReviewDataCache;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ISyntaxFixer;
import de.setsoftware.reviewtool.model.ITicketChooser;
import de.setsoftware.reviewtool.model.IUserInteraction;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.ICreateToursUi;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.preferredtransitions.api.IPreferredTransitionStrategy;
import de.setsoftware.reviewtool.preferredtransitions.basicstrategies.PathRegexStrategyConfigurator;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.tourrestructuring.onestop.OneStopPerPartOfFileRestructuring;
import de.setsoftware.reviewtool.ui.IStopViewer;
import de.setsoftware.reviewtool.ui.dialogs.CorrectSyntaxDialog;
import de.setsoftware.reviewtool.ui.dialogs.EndReviewDialog;
import de.setsoftware.reviewtool.ui.dialogs.EndReviewExtension;
import de.setsoftware.reviewtool.ui.dialogs.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.dialogs.RemarkMarkers;
import de.setsoftware.reviewtool.ui.dialogs.SelectIrrelevantDialog;
import de.setsoftware.reviewtool.ui.dialogs.SelectTicketDialog;
import de.setsoftware.reviewtool.ui.dialogs.SelectTourStructureDialog;
import de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend.SurveyAtEndConfigurator;
import de.setsoftware.reviewtool.ui.views.CombinedDiffStopViewer;
import de.setsoftware.reviewtool.ui.views.CurrentStop;
import de.setsoftware.reviewtool.ui.views.ImageCache;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;
import de.setsoftware.reviewtool.ui.views.StopViewConfigurator;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;

/**
 * Plugin that handles the review workflow and ties together the different parts.
 */
public class ReviewPlugin implements IReviewConfigurable {

    /**
     * The plugin can be in one of three modes: Inactive/Idle, Reviewing or Fixing of review remarks.
     */
    public static enum Mode {
        IDLE,
        REVIEWING,
        FIXING
    }

    /**
     * Implementation of {@link IUserInteraction} delegating to the real dialogs.
     */
    private static final class RealUi implements IUserInteraction, ITicketChooser, ISyntaxFixer {

        @Override
        public ITicketChooser getTicketChooser() {
            return this;
        }

        @Override
        public String choose(IReviewPersistence persistence, String ticketKeyDefault, boolean forReview) {
            return SelectTicketDialog.get(persistence, ticketKeyDefault, forReview);
        }

        @Override
        public ISyntaxFixer getSyntaxFixer() {
            return this;
        }

        @Override
        public ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory) {
            return CorrectSyntaxDialog.getCurrentReviewDataParsed(persistence, factory);
        }

    }

    /**
     * Implementation of {@link IChangeSourceUi}, delegating the {@link IProgressMonitor} parts.
     */
    private final class ChangeSourceUi implements IChangeSourceUi {

        private final Display display;
        private final IProgressMonitor progressMonitor;

        /**
         * Constructor.
         *
         * @param display The display to use.
         * @param progressMonitor The progress monitor to delegate to.
         */
        ChangeSourceUi(final Display display, final IProgressMonitor progressMonitor) {
            this.display = display;
            this.progressMonitor = progressMonitor;
        }

        @Override
        public Boolean handleLocalWorkingCopyOutOfDate(final String detailInfo) {
            return ReviewPlugin.this.callUiFromBackgroundJob(
                    null,
                    this.display,
                    new Callback<Boolean, Void>() {
                        @Override
                        public Boolean run(final Void unused) {
                            final MessageDialog dg = new MessageDialog(
                                    null,
                                    "Working copy out of date",
                                    null,
                                    "The working copy (" + detailInfo
                                    + ") does not contain all relevant changes. Perform an update?",
                                    MessageDialog.QUESTION_WITH_CANCEL,
                                    new String[]{
                                        IDialogConstants.YES_LABEL,
                                        IDialogConstants.NO_LABEL,
                                        IDialogConstants.CANCEL_LABEL},
                                    0);
                            switch (dg.open()) {
                            case 0:
                                //yes
                                return Boolean.TRUE;
                            case 1:
                                //no
                                return Boolean.FALSE;
                            case 2:
                            default:
                                //cancel
                                return null;
                            }
                        }
                    });
        }

        @Override
        public void beginTask(final String name, final int totalWork) {
            this.progressMonitor.beginTask(name, totalWork);
        }

        @Override
        public void done() {
            this.progressMonitor.done();
        }

        @Override
        public void internalWorked(final double work) {
            this.progressMonitor.internalWorked(work);
        }

        @Override
        public boolean isCanceled() {
            return this.progressMonitor.isCanceled();
        }

        @Override
        public void setCanceled(final boolean value) {
            this.progressMonitor.setCanceled(value);
        }

        @Override
        public void setTaskName(final String name) {
            this.progressMonitor.setTaskName(name);
        }

        @Override
        public void subTask(final String name) {
            this.progressMonitor.subTask(name);
        }

        @Override
        public void worked(final int work) {
            this.progressMonitor.worked(work);
        }
    }

    /**
     * Handles resource changes.
     */
    private final class ResourceChangeListener implements IResourceChangeListener {
        private boolean logged;

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            this.logged = false;
            final List<File> paths = new ArrayList<>();

            try {
                this.handleResourceDelta(event.getDelta(), paths);
                if (!paths.isEmpty()) {
                    ReviewPlugin.this.updateLocalChanges(paths);
                }
            } catch (final Exception e) {
                Logger.error("error while sending telemetry events", e);
            }
        }

        private void handleResourceDelta(final IResourceDelta delta, final List<File> paths) {
            if (delta.getResource().isDerived()) {
                return;
            }
            if (delta.getResource().getType() == IResource.FILE) {
                if (!this.logged && (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
                    Telemetry.event("fileChanged")
                        .param("path", delta.getFullPath())
                        .param("kind", delta.getKind())
                        .log();
                    this.logged = true;
                }

                if (delta.getKind() != IResourceDelta.CHANGED
                        || (delta.getFlags() & (IResourceDelta.CONTENT | IResourceDelta.REPLACED)) != 0) {
                    final File filePath = delta.getResource().getLocation().toFile();
                    paths.add(filePath);
                }
            } else {
                for (final IResourceDelta d : delta.getAffectedChildren()) {
                    this.handleResourceDelta(d, paths);
                }
            }
        }
    }

    /**
     * A {@link IProgressMonitor} implementation doing nothing.
     */
    private static final class DummyProgressMonitor implements IProgressMonitor {

        @Override
        public void beginTask(final String name, final int totalWork) {
        }

        @Override
        public void done() {
        }

        @Override
        public void internalWorked(final double work) {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void setCanceled(final boolean value) {
        }

        @Override
        public void setTaskName(final String name) {
        }

        @Override
        public void subTask(final String name) {
        }

        @Override
        public void worked(final int work) {
        }
    }

    /**
     * Represents a simple callback receiving a single value and returning some value.
     * @param <R> The type of the result returned.
     * @param <T> The type of the value received.
     */
    private interface Callback<R, T> {
        /**
         * Lets the client do something at some point in the future with a provided value.
         */
        public abstract R run(T t);
    }

    /**
     * Represents an asynchronous callback that acquires the UI.
     * @param <T> The type of value received.
     */
    private abstract class UiCallback<T> implements Callback<Void, T> {

        private final Display display;

        /**
         * Constructor.
         * @param display The display to use.
         */
        UiCallback(final Display display) {
            this.display = display;
        }

        @Override
        public final Void run(final T t) {
            this.display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    UiCallback.this.runInUi(t, UiCallback.this.display);
                }
            });
            return null;
        }

        /**
         * Lets the client do something at some point in the future with a provided value and in the context
         * of the UI thread.
         */
        public abstract Void runInUi(T t, Display display);
    }

    private static final ReviewPlugin INSTANCE = new ReviewPlugin();
    private final ReviewStateManager persistence;
    private IChangeSource changeSource;
    private ToursInReview toursInReview;
    private Mode mode = Mode.IDLE;
    private final WeakListeners<ReviewModeListener> modeListeners = new WeakListeners<>();
    private final ConfigurationInterpreter configInterpreter = new ConfigurationInterpreter();
    private ILaunchesListener launchesListener;
    private IResourceChangeListener changeListener;
    private final List<IIrrelevanceDetermination> relevanceFilters = new ArrayList<>();
    private final List<EndReviewExtension> endReviewExtensions = new ArrayList<>();
    private final List<IPreferredTransitionStrategy> preferredTransitionStrategies = new ArrayList<>();
    private IStopViewer stopViewer = new CombinedDiffStopViewer();
    private final List<Runnable> postInitTasks = new ArrayList<>();


    private ReviewPlugin() {
        this.persistence = new ReviewStateManager(
                new FileReviewDataCache(Activator.getDefault().getStateLocation().toFile()),
                new FilePersistence(new File("."), "please configure"),
                new RealUi());

        final Version bundleVersion = Activator.getDefault().getBundle().getVersion();
        this.configInterpreter.addConfigurator(new FileTicketConnectorConfigurator());
        this.configInterpreter.addConfigurator(new JiraConnectorConfigurator());
        this.configInterpreter.addConfigurator(new TelemetryConfigurator(bundleVersion));
        this.configInterpreter.addConfigurator(new VersionChecker(bundleVersion));
        this.configInterpreter.addConfigurator(new SurveyAtEndConfigurator());
        this.configInterpreter.addConfigurator(new StopViewConfigurator());
        this.configInterpreter.addConfigurator(new PathRegexStrategyConfigurator());
        this.configInterpreter.addConfigurator(new BasicIrrelevanceFilterConfigurator());
        this.configInterpreter.addConfigurator(new PathIrrelevanceFilterConfigurator());
        final IExtensionPoint configuratorExtensions =
                Platform.getExtensionRegistry().getExtensionPoint("de.setsoftware.reviewtool.configurator");
        for (final IExtension extension : configuratorExtensions.getExtensions()) {
            for (final IConfigurationElement conf : extension.getConfigurationElements()) {
                Logger.debug("adding configurator " + conf.getAttribute("class"));
                try {
                    this.configInterpreter.addConfigurator((IConfigurator) conf.createExecutableExtension("class"));
                } catch (final CoreException e) {
                    Logger.error("could not load configurator extension " + extension.getUniqueIdentifier(), e);
                }
            }
        }
        this.reconfigure();

        Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().startsWith("dialog")) {
                    return;
                }
                ReviewPlugin.this.reconfigure();
            }
        });
    }

    private void reconfigure() {
        final String configFile =
                Activator.getDefault().getPreferenceStore().getString(ReviewToolPreferencePage.TEAM_CONFIG_FILE);
        if (configFile.isEmpty()) {
            //avoid errors in workspaces where CoRT shall not be used
            return;
        }

        this.changeSource = null;
        this.endReviewExtensions.clear();
        this.preferredTransitionStrategies.clear();
        this.relevanceFilters.clear();

        try {
            final Document config = ConfigurationInterpreter.load(configFile);
            final Map<String, String> userParams = ReviewToolPreferencePage.getUserParams(config, getSecurePrefs());
            this.configInterpreter.configure(config, userParams, this);
        } catch (IOException | SAXException | ParserConfigurationException | ReviewtoolException | StorageException e) {
            Logger.error("error while loading config", e);
            MessageDialog.openError(null, "Error while loading the CoRT configuration.", e.toString());
        }

        if (this.mode != Mode.IDLE) {
            //during reconfiguration, a new telemetry provider was probably created, so we have to re-register
            //  the session. This will create a new session uid, which is not fully correct, but should
            //  hopefully not be problematic during data analysis.
            Telemetry.get().registerSession(
                    this.persistence.getTicketKey(),
                    getUserPref().toUpperCase(),
                    this.mode == Mode.FIXING ? "F" : "R",
                    this.persistence.getCurrentRound());
        }

        this.enqueuePostInitTasks();
    }

    @Override
    public void addPostInitTask(Runnable r) {
        this.postInitTasks.add(r);
    }

    private void enqueuePostInitTasks() {
        for (final Runnable r : this.postInitTasks) {
            Display.getDefault().asyncExec(r);
        }
        this.postInitTasks.clear();
    }

    private static ISecurePreferences getSecurePrefs() throws StorageException {
        return ReviewToolPreferencePage.getSecurePreferences();
    }

    /**
     * Returns the user ID for the current user.
     */
    public static String getUserPref() {
        try {
            return ReviewToolPreferencePage.getUserIdPref();
        } catch (final StorageException e) {
            throw new ReviewtoolException(e);
        }
    }

    public static ReviewStateManager getPersistence() {
        return INSTANCE.persistence;
    }

    public static ReviewPlugin getInstance() {
        return INSTANCE;
    }

    public Mode getMode() {
        return this.mode;
    }

    /**
     * Lets the user select a ticket and starts reviewing for it.
     */
    public void startReview() {
        this.checkConfigured();
        if (this.invalidMode(Mode.IDLE)) {
            return;
        }

        if (this.openTasksForFixingExist()) {
            final boolean startFixingInstead = MessageDialog.openQuestion(null,
                    "Start fixing instead?",
                    "You have open tickets with review remarks. Start fixing instead?");
            if (startFixingInstead) {
                this.startFixing();
                return;
            }
        }

        if (this.checkUnsavedFilesAndCheckCancel()) {
            return;
        }

        try {
            this.loadReviewData(Mode.REVIEWING, new UiCallback<Void>(Display.getCurrent()) {
                @Override
                public Void runInUi(final Void unused, final Display display) {
                    ReviewPlugin.this.startReviewTail();
                    return null;
                }
            }, "starting review");
        } catch (final CoreException e) {
            this.logException(e);
            StatusManager.getManager().handle(e, "CoRT");
        }
    }

    private void startReviewTail() {
        if (this.mode == Mode.REVIEWING) {
            this.switchToReviewPerspective();

            Telemetry.event("reviewStarted")
                    .param("round", this.persistence.getCurrentRound())
                    .params(Tour.determineSize(this.toursInReview.getTopmostTours()))
                    .log();

            this.registerGlobalTelemetryListeners();
            TrackerManager.get().startTracker();
        }
    }

    private boolean openTasksForFixingExist() {
        return !this.persistence.getTicketsForFilter("", false).isEmpty();
    }

    private void switchToReviewPerspective() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            PlatformUI.getWorkbench().showPerspective(
                    "de.setsoftware.reviewtool.ui.perspective.reviewPerspective", window);
        } catch (final WorkbenchException e) {
            Logger.error("could not switch perspective", e);
        }
    }

    /**
     * Lets the user select a ticket and starts fixing for it.
     */
    public void startFixing() {
        this.checkConfigured();
        if (this.invalidMode(Mode.IDLE)) {
            return;
        }

        if (this.checkUnsavedFilesAndCheckCancel()) {
            return;
        }

        try {
            this.loadReviewData(Mode.FIXING, new UiCallback<Void>(Display.getCurrent()) {
                @Override
                public Void runInUi(final Void unused, final Display display) {
                    ReviewPlugin.this.startFixingTail();
                    return null;
                }
            }, "starting fixing");
        } catch (final CoreException e) {
            this.logException(e);
            StatusManager.getManager().handle(e, "CoRT");
        }
    }

    private void startFixingTail() {
        if (this.mode == Mode.FIXING) {
            Telemetry.event("fixingStarted")
                .param("round", this.persistence.getCurrentRound())
                .log();
            this.registerGlobalTelemetryListeners();
        }
    }

    private void checkConfigured() {
        if (this.changeSource == null) {
            MessageDialog.openInformation(null, "Not configured",
                    "CoRT is not configured. Go to the preferences dialog and select a config file.");
        }
    }

    private void loadReviewData(final Mode targetMode, final Callback<?, Void> tail, final String action)
            throws CoreException {

        this.persistence.resetKey();
        final boolean forReview = targetMode == Mode.REVIEWING;
        final boolean ok = this.persistence.selectTicket(forReview);
        if (!ok) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
            tail.run(null);
            return;
        }
        //The startReview event is sent only after all data is loaded. but some user interaction is possible
        //  before. Therefore the ticket key and reviewer is already set here.
        Telemetry.get().registerSession(
                this.persistence.getTicketKey(),
                getUserPref().toUpperCase(),
                forReview ? "R" : "F",
                this.persistence.getCurrentRound());
        this.clearMarkers();

        if (targetMode == Mode.REVIEWING) {
            this.loadToursAndCreateMarkers(
                    new UiCallback<Boolean>(Display.getCurrent()) {
                        @Override
                        public Void runInUi(final Boolean toursOk, final Display display) {
                            if (!toursOk) {
                                ReviewPlugin.this.setMode(Mode.IDLE);
                                ReviewPlugin.this.toursInReview = null;
                            } else {
                                ReviewPlugin.this.loadReviewDataTail(targetMode);
                            }
                            tail.run(null);
                            return null;
                        }
                    },
                    action);
        } else {
            this.loadReviewDataTail(targetMode);
            tail.run(null);
        }
    }

    private void loadReviewDataTail(final Mode targetMode) {
        final ReviewData currentReviewData = CorrectSyntaxDialog.getCurrentReviewDataParsed(
                this.persistence, new RealMarkerFactory());
        if (currentReviewData == null) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
            return;
        }
        if (targetMode == Mode.REVIEWING) {
            this.persistence.startReviewing();
        } else {
            this.persistence.startFixing();
        }
        this.setMode(targetMode);
    }

    private void clearMarkers() throws CoreException {
        RemarkMarkers.clearMarkers();
        if (this.toursInReview != null) {
            this.toursInReview.clearMarkers();
        }
    }

    private void setMode(Mode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            this.notifyModeListeners();
        }
    }

    private void notifyModeListeners() {
        for (final ReviewModeListener l : this.modeListeners) {
            this.notifyModeListener(l);
        }
    }

    private void notifyModeListener(ReviewModeListener s) {
        switch (this.mode) {
        case FIXING:
            s.notifyFixing(this.persistence);
            break;
        case REVIEWING:
            s.notifyReview(this.persistence, this.toursInReview);
            break;
        case IDLE:
            s.notifyIdle();
            break;
        default:
            throw new AssertionError();
        }
    }

    public void registerModeListener(ReviewModeListener reviewPluginModeService) {
        this.modeListeners.add(reviewPluginModeService);
    }

    public void registerAndNotifyModeListener(ReviewModeListener reviewPluginModeService) {
        this.modeListeners.add(reviewPluginModeService);
        this.notifyModeListener(reviewPluginModeService);
    }

    /**
     * Ends the review, after asking the user for confirmation and the type of end
     * transition to use.
     */
    public void endReview() throws CoreException {
        if (this.invalidMode(Mode.REVIEWING)) {
            return;
        }
        if (this.checkUnsavedFilesAndCheckCancel()) {
            return;
        }

        final ReviewData reviewData = this.getCurrentReviewDataParsed();
        final EndTransition.Type preferredEndTransitionType;
        if (reviewData.hasTemporaryMarkers()) {
            preferredEndTransitionType = EndTransition.Type.PAUSE;
        } else if (reviewData.hasUnresolvedRemarks()) {
            preferredEndTransitionType = EndTransition.Type.REJECTION;
        } else {
            preferredEndTransitionType = EndTransition.Type.OK;
        }
        final EndTransition typeOfEnd = EndReviewDialog.selectTypeOfEnd(
                this.persistence,
                reviewData,
                this.endReviewExtensions,
                preferredEndTransitionType,
                this.determinePreferredEndTransitions(preferredEndTransitionType));
        if (typeOfEnd == null) {
            return;
        }
        if (typeOfEnd.getType() != EndTransition.Type.PAUSE
                && reviewData.hasTemporaryMarkers()) {
            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            final boolean yes = MessageDialog.openQuestion(shell, "Open markers",
                    "There are still temporary markers. Finish anyway?");
            if (!yes) {
                return;
            }
        }
        TrackerManager.get().stopTracker();
        Telemetry.event("reviewEnded")
                .param("round", this.persistence.getCurrentRound())
                .param("endTransition", typeOfEnd.getNameForUser())
                .log();
        if (typeOfEnd.getType() != EndTransition.Type.PAUSE) {
            this.persistence.changeStateAtReviewEnd(typeOfEnd);
        }
        this.leaveActiveMode();

        //TODO is this the the right time to clear the image cache?
        ImageCache.dispose();
    }

    private boolean checkUnsavedFilesAndCheckCancel() {
        return !PlatformUI.getWorkbench().saveAllEditors(true);
    }

    private List<String> determinePreferredEndTransitions(EndTransition.Type type) {
        final List<String> ret = new ArrayList<>();
        for (final IPreferredTransitionStrategy strategy : this.preferredTransitionStrategies) {
            ret.addAll(strategy.determinePreferredTransitions(
                    type == EndTransition.Type.OK, this.persistence.getCurrentTicketData(), this.toursInReview));
        }
        return ret;
    }

    private void leaveActiveMode() throws CoreException {
        this.persistence.flushReviewData();
        this.clearMarkers();
        this.setMode(Mode.IDLE);
        this.toursInReview = null;
        CurrentStop.unsetCurrentStop();
        this.unregisterGlobalTelemetryListeners();
    }

    /**
     * Ends fixing and changes the ticket's state accordingly.
     */
    public void endFixing() throws CoreException {
        if (this.invalidMode(Mode.FIXING)) {
            return;
        }
        if (this.hasUnresolvedRemarks()) {
            final boolean yes = MessageDialog.openQuestion(null, "Open remarks",
                    "Some remarks have not been marked as processed. You can mark"
                    + " them with a quick fix on the marker. Finish anyway?");
            if (!yes) {
                return;
            }
        }

        if (this.checkUnsavedFilesAndCheckCancel()) {
            return;
        }

        Telemetry.event("fixingEnded")
            .param("round", this.persistence.getCurrentRound())
            .log();
        this.persistence.changeStateToReadyForReview();
        this.leaveActiveMode();
    }

    private boolean invalidMode(Mode expectedMode) {
        if (this.mode == expectedMode) {
            return false;
        } else {
            MessageDialog.openError(null, "Action not possible in current mode",
                    "The chosen action is not possible in CoRT's current mode. The current mode is "
                    + this.mode + ", the needed mode is " + expectedMode + ".");
            return true;
        }
    }

    public boolean hasUnresolvedRemarks() {
        return this.getCurrentReviewDataParsed().hasUnresolvedRemarks();
    }

    private ReviewData getCurrentReviewDataParsed() {
        return CorrectSyntaxDialog.getCurrentReviewDataParsed(this.persistence, DummyMarker.FACTORY);
    }

    /**
     * Removes all markers, reloads the underlying data and recreates the markers with this new data.
     */
    public void refreshMarkers() throws CoreException {
        final boolean cont = MessageDialog.openQuestion(null, "Really reload?",
                "If you made local changes to the review remarks, these will be lost. Continue reload?");
        if (!cont) {
            return;
        }
        this.persistence.clearLocalReviewData();
        this.clearMarkers();
        this.loadToursAndCreateMarkers(new UiCallback<Boolean>(Display.getCurrent()) {
            @Override
            public Void runInUi(final Boolean toursOk, final Display display) {
                RemarkMarkers.loadRemarks(ReviewPlugin.this.persistence);
                return null;
            }
        }, "refreshing markers");
    }

    /**
     * Saves the local review remarks to the persistence layer.
     */
    public void flushLocalReviewData() {
        this.persistence.flushReviewData();
    }

    private void loadToursAndCreateMarkers(final Callback<?, Boolean> tail, final String action) {
        final IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
        try {
            final Display display = Display.getCurrent();
            progressService.busyCursorWhile(new IRunnableWithProgress() {

                @Override
                public void run(final IProgressMonitor progressMonitor)
                        throws InvocationTargetException, InterruptedException {
                    tail.run(ReviewPlugin.this.doLoadToursAndCreateMarkers(display, progressMonitor, action));
                }
            });
        } catch (final InterruptedException e) {
            StatusManager.getManager().handle(
                    new Status(Status.WARNING, "CoRT", "CoRT was interrupted while " + action + ".", e),
                    StatusManager.LOG);
        } catch (final InvocationTargetException e) {
            this.logException(e);
            StatusManager.getManager().handle(
                    new Status(Status.ERROR, "CoRT", "An error occurred while " + action + ".", e),
                    StatusManager.LOG);
        }
    }

    private boolean doLoadToursAndCreateMarkers(final Display display, final IProgressMonitor progressMonitor,
            final String action) {

        final String ticketKey = this.persistence.getTicketKey();
        if (ticketKey == null) {
            return false;
        }
        final IChangeSourceUi sourceUi = new ChangeSourceUi(display, progressMonitor);
        final ICreateToursUi createUi = new ICreateToursUi() {
            @Override
            public List<? extends Tour> selectInitialTours(
                    final List<? extends Pair<String, List<? extends Tour>>> choices) {
                if (choices.size() == 1) {
                    return choices.get(0).getSecond();
                }
                return ReviewPlugin.this.callUiFromBackgroundJob(
                        choices,
                        display,
                        new Callback<List<? extends Tour>, List<? extends Pair<String, List<? extends Tour>>>>() {
                            @Override
                            public List<? extends Tour> run(
                                    List<? extends Pair<String, List<? extends Tour>>> choices) {
                                return SelectTourStructureDialog.selectStructure(choices);
                            }
                        });
            }

            @Override
            public List<? extends Pair<String, Set<? extends IChange>>> selectIrrelevant(
                    final List<? extends Pair<String, Set<? extends IChange>>> choices) {
                if (choices.isEmpty()) {
                    return Collections.emptyList();
                }
                return ReviewPlugin.this.callUiFromBackgroundJob(
                        choices,
                        display,
                        new Callback<
                                List<? extends Pair<String, Set<? extends IChange>>>,
                                List<? extends Pair<String, Set<? extends IChange>>>>() {
                            @Override
                            public List<? extends Pair<String, Set<? extends IChange>>> run(
                                    List<? extends Pair<String, Set<? extends IChange>>> choices) {
                                return SelectIrrelevantDialog.selectIrrelevant(choices);
                            }
                        });
            }
        };

        sourceUi.beginTask(ticketKey + ": Please wait while " + action + "...", IProgressMonitor.UNKNOWN);
        try {
            sourceUi.subTask("Creating tours...");
            this.toursInReview = ToursInReview.create(
                    this.changeSource,
                    sourceUi,
                    this.relevanceFilters,
                    Arrays.asList(
                            new OneStopPerPartOfFileRestructuring()),
                    createUi,
                    ticketKey);
            if (this.toursInReview == null) {
                return false;
            }
            sourceUi.subTask("Creating stop markers...");
            this.toursInReview.createMarkers(new RealMarkerFactory(), sourceUi);
            return true;
        } finally {
            sourceUi.done();
        }
    }

    @Override
    public void setPersistence(IReviewPersistence newPersistence) {
        this.persistence.setPersistence(newPersistence);
    }

    @Override
    public void setChangeSource(IChangeSource changeSource) {
        this.changeSource = changeSource;
    }

    public ToursInReview getTours() {
        return this.toursInReview;
    }

    private void registerGlobalTelemetryListeners() {
        this.launchesListener = new ILaunchesListener() {
            @Override
            public void launchesRemoved(ILaunch[] launches) {
            }

            @Override
            public void launchesChanged(ILaunch[] launches) {
            }

            @Override
            public void launchesAdded(ILaunch[] launches) {
                try {
                    for (final ILaunch launch : launches) {
                        Telemetry.event("launch")
                            .param("mode", launch.getLaunchMode())
                            .param("config", launch.getLaunchConfiguration().getName())
                            .log();
                    }
                } catch (final Exception e) {
                    Logger.error("error while sending telemetry events", e);
                }
            }
        };
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this.launchesListener);

        this.changeListener = new ResourceChangeListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this.changeListener, IResourceChangeEvent.POST_CHANGE);
    }

    private void unregisterGlobalTelemetryListeners() {
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this.launchesListener);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.changeListener);
    }

    @Override
    public void addIrrelevanceStrategy(IIrrelevanceDetermination strategy) {
        this.relevanceFilters.add(strategy);
    }

    @Override
    public void addEndReviewExtension(EndReviewExtension extension) {
        this.endReviewExtensions.add(extension);
    }

    @Override
    public void addPreferredTransitionStrategy(IPreferredTransitionStrategy strategy) {
        this.preferredTransitionStrategies.add(strategy);
    }

    @Override
    public void setStopViewer(IStopViewer stopViewer) {
        this.stopViewer = stopViewer;
    }

    public IStopViewer getStopViewer() {
        return this.stopViewer;
    }

    private void updateLocalChanges(final List<File> paths) {
        if (this.toursInReview == null) {
            return;
        }

        ReviewPlugin.this.toursInReview.createLocalTour(paths, new DummyProgressMonitor(), new RealMarkerFactory());
    }

    /**
     * Logs the given exception. Swallows all follow-up exceptions.
     */
    public void logException(Throwable t) {
        try {
            final StringWriter w = new StringWriter();
            t.printStackTrace(new PrintWriter(w));
            Telemetry.event("exception")
                .param("exceptionClass", t.getClass().toString())
                .param("details", w.toString())
                .log();
        } catch (final Throwable t2) {
            //swallow possible follow-up exceptions
        }
    }

    private <R, T> R callUiFromBackgroundJob(
            final T input,
            final Display display,
            final Callback<R, T> callback) {
        final Object condition = new Object();
        final ValueWrapper<Boolean> resultSet = new ValueWrapper<>(false);
        final ValueWrapper<R> result = new ValueWrapper<>();
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                final R r = callback.run(input);
                synchronized (condition) {
                    result.setValue(r);
                    resultSet.setValue(Boolean.TRUE);
                    condition.notify();
                }
            }
        });
        synchronized (condition) {
            while (!resultSet.get()) {
                try {
                    condition.wait();
                } catch (final InterruptedException e) {
                    ReviewPlugin.this.logException(e);
                    break;
                }
            }
        }
        return result.get();
    }

}
