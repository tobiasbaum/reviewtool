package de.setsoftware.reviewtool.plugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
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
import de.setsoftware.reviewtool.base.Multiset;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.config.ConfigurationInterpreter;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.BasicIrrelevanceFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.BinaryFileFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.FileDeletionFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.pathfilters.FileCountInCommitAndPathFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.pathfilters.FileCountInCommitFilterConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.pathfilters.PathIrrelevanceFilterConfigurator;
import de.setsoftware.reviewtool.lifecycle.IPostInitTask;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.FileReviewDataCache;
import de.setsoftware.reviewtool.model.ISyntaxFixer;
import de.setsoftware.reviewtool.model.ITicketChooser;
import de.setsoftware.reviewtool.model.ITicketConnector;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.IUserInteraction;
import de.setsoftware.reviewtool.model.Mode;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.IChangeClassifier;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.ICreateToursUi;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.ReviewRoundInfo;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.UserSelectedReductions;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.ordering.RelationMatcher;
import de.setsoftware.reviewtool.ordering.StopOrdering;
import de.setsoftware.reviewtool.preferredtransitions.api.IPreferredTransitionStrategy;
import de.setsoftware.reviewtool.preferredtransitions.basicstrategies.PathRegexStrategyConfigurator;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.tourrestructuring.onestop.OneStopPerPartOfFileRestructuring;
import de.setsoftware.reviewtool.ui.api.EndReviewExtension;
import de.setsoftware.reviewtool.ui.api.IStopViewer;
import de.setsoftware.reviewtool.ui.dialogs.CorrectSyntaxDialog;
import de.setsoftware.reviewtool.ui.dialogs.EndReviewDialog;
import de.setsoftware.reviewtool.ui.dialogs.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.dialogs.RemarkMarkers;
import de.setsoftware.reviewtool.ui.dialogs.SelectIrrelevantDialog;
import de.setsoftware.reviewtool.ui.dialogs.SelectTicketDialog;
import de.setsoftware.reviewtool.ui.dialogs.SelectTourStructureDialog;
import de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend.SurveyAtEndConfigurator;
import de.setsoftware.reviewtool.ui.views.CombinedDiffStopViewer;
import de.setsoftware.reviewtool.ui.views.CurrentStop;
import de.setsoftware.reviewtool.ui.views.FullLineHighlighter;
import de.setsoftware.reviewtool.ui.views.ImageCache;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;
import de.setsoftware.reviewtool.ui.views.StopViewConfigurator;
import de.setsoftware.reviewtool.viewtracking.TrackerManager;

/**
 * Plugin that handles the review workflow and ties together the different parts.
 */
public class ReviewPlugin implements IReviewConfigurable {

    /**
     * Implementation of {@link IUserInteraction} delegating to the real dialogs.
     */
    private static final class RealUi implements IUserInteraction, ITicketChooser, ISyntaxFixer {

        @Override
        public ITicketChooser getTicketChooser() {
            return this;
        }

        @Override
        public String choose(
                final ITicketConnector persistence,
                final String ticketKeyDefault,
                final boolean forReview) {
            return SelectTicketDialog.get(persistence, ticketKeyDefault, forReview);
        }

        @Override
        public ISyntaxFixer getSyntaxFixer() {
            return this;
        }

        @Override
        public ReviewData getCurrentReviewDataParsed(
                final ReviewStateManager persistence,
                final IMarkerFactory factory) {
            return CorrectSyntaxDialog.getCurrentReviewDataParsed(persistence, factory);
        }

    }

    /**
     * Implementation of {@link IChangeSourceUi}, delegating the {@link IProgressMonitor} parts.
     */
    private final class ChangeSourceUi implements IChangeSourceUi {

        private final Display display;
        private final IProgressMonitor progressMonitor;
        private final Deque<String> taskStack;
        private String currentSubTask;

        /**
         * Constructor.
         *
         * @param display The display to use.
         * @param progressMonitor The progress monitor to delegate to.
         */
        ChangeSourceUi(final Display display, final IProgressMonitor progressMonitor) {
            this.display = display;
            this.progressMonitor = progressMonitor;
            this.taskStack = new ArrayDeque<>();
        }

        @Override
        public Boolean handleLocalWorkingIncomplete(final String detailInfo) {
            return ReviewPlugin.this.callUiFromBackgroundJob(
                    null,
                    this.display,
                    new Callback<Boolean, Void>() {
                        @Override
                        public Boolean run(final Void unused) {
                            final MessageDialog dg = new MessageDialog(
                                    null,
                                    "Working copy incomplete",
                                    null,
                                    detailInfo,
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
            this.currentSubTask = name;
            this.progressMonitor.subTask(this.buildSubTaskName(this.currentSubTask));
        }

        @Override
        public void worked(final int work) {
            this.progressMonitor.worked(work);
        }

        @Override
        public void increaseTaskNestingLevel() {
            this.taskStack.push(this.currentSubTask);
            this.currentSubTask = "";
        }

        @Override
        public void decreaseTaskNestingLevel() {
            this.currentSubTask = this.taskStack.pop();
            this.progressMonitor.subTask(this.buildSubTaskName(this.currentSubTask));
        }

        private String buildSubTaskName(final String name) {
            final StringBuilder sb = new StringBuilder();
            for (final String task : this.taskStack) {
                sb.append(" ");
                sb.append(task);
            }
            sb.append(" ");
            sb.append(name);
            return sb.toString().trim();
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
    private final ChangeManager changeManager = new ChangeManager(true);
    private ToursInReview toursInReview;
    private Mode mode = Mode.IDLE;
    private final WeakListeners<ReviewModeListener> modeListeners = new WeakListeners<>();
    private final ConfigurationInterpreter configInterpreter = new ConfigurationInterpreter();
    private ILaunchesListener launchesListener;
    private final List<IChangeClassifier> relevanceFilters = new ArrayList<>();
    private final List<EndReviewExtension> endReviewExtensions = new ArrayList<>();
    private final List<IPreferredTransitionStrategy> preferredTransitionStrategies = new ArrayList<>();
    private final List<RelationMatcher> relationTypes = new ArrayList<>();
    private IStopViewer stopViewer = new CombinedDiffStopViewer();
    private final List<Runnable> postInitTasks = new ArrayList<>();
    private final List<IChangeSource> changeSources = new ArrayList<>();


    private ReviewPlugin() {
        this.persistence = new ReviewStateManager(
                new FileReviewDataCache(Activator.getDefault().getStateLocation().toFile()),
                new DummyPersistence(),
                new RealUi());

        final Version bundleVersion = Activator.getDefault().getBundle().getVersion();
        this.configInterpreter.addConfigurator(new TelemetryConfigurator(bundleVersion));
        this.configInterpreter.addConfigurator(new VersionChecker(bundleVersion));
        this.configInterpreter.addConfigurator(new SurveyAtEndConfigurator());
        this.configInterpreter.addConfigurator(new StopViewConfigurator());
        this.configInterpreter.addConfigurator(new PathRegexStrategyConfigurator());
        this.configInterpreter.addConfigurator(new BasicIrrelevanceFilterConfigurator());
        this.configInterpreter.addConfigurator(new PathIrrelevanceFilterConfigurator());
        this.configInterpreter.addConfigurator(new FileDeletionFilterConfigurator());
        this.configInterpreter.addConfigurator(new BinaryFileFilterConfigurator());
        this.configInterpreter.addConfigurator(new FileCountInCommitFilterConfigurator());
        this.configInterpreter.addConfigurator(new FileCountInCommitAndPathFilterConfigurator());
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
            public void propertyChange(final PropertyChangeEvent event) {
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

        this.changeSources.clear();
        this.changeManager.setChangeSources(null);
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


        //configure relation types for tour ordering; they are user specific, not team specific
        this.relationTypes.clear();
        this.relationTypes.addAll(
                RelationMatcherPreferences.load(Activator.getDefault().getPreferenceStore()).createMatchers());
    }
    
    public final void configureWith(Object strategy) {
        boolean handled = false;
        if (strategy instanceof ITicketConnector) {
            this.setPersistence((ITicketConnector) strategy);
            handled = true;
        }
        if (strategy instanceof IChangeSource) {
            this.addChangeSource((IChangeSource) strategy);
            handled = true;
        }
        if (strategy instanceof EndReviewExtension) {
            this.addEndReviewExtension((EndReviewExtension) strategy);
            handled = true;
        }
        if (strategy instanceof IStopViewer) {
            this.setStopViewer((IStopViewer) strategy);
            handled = true;
        }
        if (strategy instanceof IPostInitTask) {
            this.addPostInitTask((IPostInitTask) strategy);
            handled = true;
        }
        if (strategy instanceof IPreferredTransitionStrategy) {
            this.addPreferredTransitionStrategy((IPreferredTransitionStrategy) strategy);
            handled = true;
        }
        if (strategy instanceof IChangeClassifier) {
            this.addClassificationStrategy((IChangeClassifier) strategy);
            handled = true;
        }
        if (!handled) {
            throw new AssertionError("unhandled configuration result: " + strategy);
        }
    }

    public void addPostInitTask(final Runnable r) {
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
            this.switchToPerspective("de.setsoftware.reviewtool.ui.perspective.reviewPerspective");
            FullLineHighlighter.registerHighlighters();

            Telemetry.event("reviewStarted")
                    .param("round", this.persistence.getCurrentRound())
                    .params(Tour.determineSize(
                            this.toursInReview.getTopmostTours(), this.toursInReview.getIrrelevantCategories()))
                    .log();

            this.registerGlobalTelemetryListeners();
            TrackerManager.get().startTracker();
        }
    }

    private boolean openTasksForFixingExist() {
        return !this.persistence.getTicketsForFilter("", false).isEmpty();
    }

    private void switchToPerspective(final String perspectiveId) {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            PlatformUI.getWorkbench().showPerspective(perspectiveId, window);
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
            this.switchToPerspective("de.setsoftware.reviewtool.ui.perspective.fixingPerspective");
            Telemetry.event("fixingStarted")
                    .param("round", this.persistence.getCurrentRound())
                    .log();
            this.registerGlobalTelemetryListeners();
        }
    }

    private void checkConfigured() {
        if (!this.changeManager.isConfigured()) {
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

    private void setMode(final Mode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            this.notifyModeListeners();
        }
        if (mode == Mode.IDLE) {
            this.toursInReview = null;
        }
    }

    private void notifyModeListeners() {
        this.modeListeners.notifyListeners(this::notifyModeListener);
    }

    private void notifyModeListener(final ReviewModeListener s) {
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

    public void registerModeListener(final ReviewModeListener reviewPluginModeService) {
        this.modeListeners.add(reviewPluginModeService);
    }

    public void registerAndNotifyModeListener(final ReviewModeListener reviewPluginModeService) {
        this.modeListeners.add(reviewPluginModeService);
        this.notifyModeListener(reviewPluginModeService);
    }

    /**
     * Ends review or fixing, depending on the current mode.
     */
    public void endReviewOrFixing() throws CoreException {
        if (this.mode == Mode.FIXING) {
            this.endFixing();
        } else {
            this.endReview();
        }
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

    private List<String> determinePreferredEndTransitions(final EndTransition.Type type) {
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

    private boolean invalidMode(final Mode expectedMode) {
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
                    new Status(IStatus.WARNING, "CoRT", "CoRT was interrupted while " + action + ".", e),
                    StatusManager.LOG);
        } catch (final InvocationTargetException e) {
            this.logException(e);
            StatusManager.getManager().handle(
                    new Status(IStatus.ERROR, "CoRT", "An error occurred while " + action + ".", e),
                    StatusManager.LOG);
        }
    }

    private boolean doLoadToursAndCreateMarkers(
            final Display display,
            final IProgressMonitor progressMonitor,
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
                                    final List<? extends Pair<String, List<? extends Tour>>> choices) {
                                return SelectTourStructureDialog.selectStructure(choices);
                            }
                        });
            }

            @Override
            public UserSelectedReductions selectIrrelevant(
                    final List<? extends ICommit> changes,
                    final Multiset<IClassification> strategyResults,
                    final List<ReviewRoundInfo> reviewRounds) {
                return ReviewPlugin.this.callUiFromBackgroundJob(
                        null,
                        display,
                        new Callback<UserSelectedReductions, Void>() {
                            @Override
                            public UserSelectedReductions run(final Void v) {
                                return SelectIrrelevantDialog.show(
                                        changes, strategyResults, reviewRounds);
                            }
                        });
            }
        };

        sourceUi.beginTask(ticketKey + ": Please wait while " + action + "...", IProgressMonitor.UNKNOWN);
        try {
            sourceUi.subTask("Determining relevant changes...");
            final IChangeData changes;
            try {
                changes = this.getChanges(ticketKey, sourceUi);
                if (changes.getMatchedCommits().isEmpty()) {
                    final boolean ok = ReviewPlugin.this.callUiFromBackgroundJob(
                            null,
                            display,
                            new Callback<Boolean, Void>() {
                                @Override
                                public Boolean run(final Void input) {
                                    return MessageDialog.openConfirm(
                                            display.getActiveShell(),
                                            "No commits found, continue review?",
                                            String.format("No commits have been found for ticket key %s.\n\n"
                                                    + "If this is unexpected, please abort the review and look into "
                                                    + "the Error log whether there were problems accessing the "
                                                    + "repository. Otherwise confirm to continue the review.",
                                                    ticketKey));
                                }
                            });

                    if (!ok) {
                        return false;
                    }
                }
            } catch (final ChangeSourceException e) {
                Logger.error("Problem while determining relevant changes", e);
                return false;
            }

            sourceUi.subTask("Creating tours...");
            this.toursInReview = ToursInReview.create(
                    this.changeManager,
                    sourceUi,
                    this.relevanceFilters,
                    Arrays.asList(
                            new OneStopPerPartOfFileRestructuring()),
                    new StopOrdering(this.relationTypes),
                    createUi,
                    changes,
                    this.getReviewRounds(this.persistence.getCurrentTicketData()));
            if (this.toursInReview == null) {
                return false;
            }

            sourceUi.subTask("Creating stop markers...");
            this.toursInReview.createMarkers(new RealMarkerFactory(), () -> {
                if (sourceUi.isCanceled()) {
                    throw new OperationCanceledException();
                }
            });
            return true;
        } finally {
            sourceUi.done();
        }
    }

    private IChangeData getChanges(String ticketKey, IChangeSourceUi sourceUi) throws ChangeSourceException {
        final List<ICommit> commits = new ArrayList<>();
        for (final IChangeSource src : this.changeManager.getChangeSources()) {
            commits.addAll(src.getRepositoryChanges(ticketKey, sourceUi).getMatchedCommits());
        }
        return ChangestructureFactory.createChangeData(commits);
    }

    private List<ReviewRoundInfo> getReviewRounds(final ITicketData ticket) {
        final List<ReviewRoundInfo> ret = new ArrayList<>();
        for (int round = 1; round <= ticket.getCurrentRound(); round++) {
            ret.add(new ReviewRoundInfo(
                    round,
                    ticket.getEndTimeForRound(round),
                    ticket.getReviewerForRound(round)));
        }
        return ret;
    }

    public void setPersistence(final ITicketConnector newPersistence) {
        this.persistence.setPersistence(newPersistence);
    }

    public void addChangeSource(final IChangeSource changeSource) {
        this.changeSources.add(changeSource);
        this.changeManager.setChangeSources(this.changeSources);
        PositionTransformer.setChangeSources(this.changeSources);
    }

    public ToursInReview getTours() {
        return this.toursInReview;
    }

    private void registerGlobalTelemetryListeners() {
        this.launchesListener = new ILaunchesListener() {
            @Override
            public void launchesRemoved(final ILaunch[] launches) {
            }

            @Override
            public void launchesChanged(final ILaunch[] launches) {
            }

            @Override
            public void launchesAdded(final ILaunch[] launches) {
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
    }

    private void unregisterGlobalTelemetryListeners() {
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this.launchesListener);
    }

    public void addClassificationStrategy(final IChangeClassifier strategy) {
        this.relevanceFilters.add(strategy);
    }

    public void addEndReviewExtension(final EndReviewExtension extension) {
        this.endReviewExtensions.add(extension);
    }

    public void addPreferredTransitionStrategy(final IPreferredTransitionStrategy strategy) {
        this.preferredTransitionStrategies.add(strategy);
    }

    public void setStopViewer(final IStopViewer stopViewer) {
        this.stopViewer = stopViewer;
    }

    public IStopViewer getStopViewer() {
        return this.stopViewer;
    }

    /**
     * Logs the given exception. Swallows all follow-up exceptions.
     */
    public void logException(final Throwable t) {
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

    public ChangeManager getChangeManager() {
        return this.changeManager;
    }

}
