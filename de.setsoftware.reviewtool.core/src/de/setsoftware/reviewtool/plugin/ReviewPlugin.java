package de.setsoftware.reviewtool.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.config.ConfigurationInterpreter;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.connectors.file.FilePersistence;
import de.setsoftware.reviewtool.connectors.file.FileTicketConnectorConfigurator;
import de.setsoftware.reviewtool.connectors.jira.JiraConnectorConfigurator;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.ImportChangeFilter;
import de.setsoftware.reviewtool.irrelevancestrategies.basicfilters.WhitespaceChangeFilter;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ISyntaxFixer;
import de.setsoftware.reviewtool.model.ITicketChooser;
import de.setsoftware.reviewtool.model.IUserInteraction;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.ICreateToursUi;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
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
import de.setsoftware.reviewtool.ui.views.CurrentStop;
import de.setsoftware.reviewtool.ui.views.ImageCache;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;
import de.setsoftware.reviewtool.ui.views.SeparateDiffsStopViewer;
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

    private static final ReviewPlugin INSTANCE = new ReviewPlugin();
    private final ReviewStateManager persistence;
    private IChangeSource changeSource;
    private ToursInReview toursInReview;
    private Mode mode = Mode.IDLE;
    private final WeakListeners<ReviewModeListener> modeListeners = new WeakListeners<>();
    private final ConfigurationInterpreter configInterpreter = new ConfigurationInterpreter();
    private ILaunchesListener launchesListener;
    private IResourceChangeListener changeListener;
    List<EndReviewExtension> endReviewExtensions = new ArrayList<>();
    private IStopViewer stopViewer = new SeparateDiffsStopViewer();


    private ReviewPlugin() {
        this.persistence = new ReviewStateManager(new FilePersistence(new File("."), "please configure"), new RealUi());

        final Version bundleVersion = Activator.getDefault().getBundle().getVersion();
        this.configInterpreter.addConfigurator(new FileTicketConnectorConfigurator());
        this.configInterpreter.addConfigurator(new JiraConnectorConfigurator());
        this.configInterpreter.addConfigurator(new TelemetryConfigurator(bundleVersion));
        this.configInterpreter.addConfigurator(new VersionChecker(bundleVersion));
        this.configInterpreter.addConfigurator(new SurveyAtEndConfigurator());
        this.configInterpreter.addConfigurator(new StopViewConfigurator());
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
        this.changeSource = null;
        this.endReviewExtensions.clear();

        try {
            final Document config = ConfigurationInterpreter.load(
                    Activator.getDefault().getPreferenceStore().getString(ReviewToolPreferencePage.TEAM_CONFIG_FILE));
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
    public void startReview() throws CoreException {
        if (this.invalidMode(Mode.IDLE)) {
            return;
        }
        this.loadReviewData(Mode.REVIEWING);
        if (this.mode == Mode.REVIEWING) {
            this.switchToReviewPerspective();

            Telemetry.event("reviewStarted")
                    .param("round", this.persistence.getCurrentRound())
                    .params(Tour.determineSize(this.toursInReview.getTours()))
                    .log();

            this.registerGlobalTelemetryListeners();
            TrackerManager.get().startTracker();
        }
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
    public void startFixing() throws CoreException {
        if (this.invalidMode(Mode.IDLE)) {
            return;
        }
        this.loadReviewData(Mode.FIXING);
        if (this.mode == Mode.FIXING) {
            Telemetry.event("fixingStarted")
                .param("round", this.persistence.getCurrentRound())
                .log();
            this.registerGlobalTelemetryListeners();
        }
    }

    private void loadReviewData(Mode targetMode) throws CoreException {
        this.persistence.resetKey();
        final boolean forReview = targetMode == Mode.REVIEWING;
        final boolean ok = this.persistence.selectTicket(forReview);
        if (!ok) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
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
            final boolean toursOk = this.loadToursAndCreateMarkers();
            if (!toursOk) {
                this.setMode(Mode.IDLE);
                this.toursInReview = null;
                return;
            }
        }

        final ReviewData currentReviewData = CorrectSyntaxDialog.getCurrentReviewDataParsed(
                this.persistence, new RealMarkerFactory());
        if (currentReviewData == null) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
            return;
        }
        if (forReview) {
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
        final EndTransition typeOfEnd = EndReviewDialog.selectTypeOfEnd(
                this.persistence, this.getCurrentReviewDataParsed(), this.endReviewExtensions);
        if (typeOfEnd == null) {
            return;
        }
        if (typeOfEnd.getType() != EndTransition.Type.PAUSE
                && this.getCurrentReviewDataParsed().hasTemporaryMarkers()) {
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
        this.leaveReviewMode();

        //TODO is this the the right time to clear the image cache?
        ImageCache.dispose();
    }

    private void leaveReviewMode() throws CoreException {
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
        Telemetry.event("fixingEnded")
            .param("round", this.persistence.getCurrentRound())
            .log();
        this.persistence.changeStateToReadyForReview();
        this.leaveReviewMode();
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
        this.clearMarkers();
        this.loadToursAndCreateMarkers();
        RemarkMarkers.loadRemarks(this.persistence);
    }

    private boolean loadToursAndCreateMarkers() {
        final String ticketKey = this.persistence.getTicketKey();
        if (ticketKey == null) {
            return false;
        }
        final IChangeSourceUi sourceUi = new IChangeSourceUi() {
            @Override
            public boolean handleLocalWorkingCopyOutOfDate(String detailInfo) {
                return MessageDialog.openQuestion(
                        null, "Working copy out of date",
                        "The working copy (" + detailInfo
                        + ") does not contain all relevant changes. Perform an update?");
            }
        };
        final ICreateToursUi createUi = new ICreateToursUi() {
            @Override
            public List<? extends Tour> selectInitialTours(
                    List<? extends Pair<String, List<? extends Tour>>> choices) {
                if (choices.size() == 1) {
                    return choices.get(0).getSecond();
                }
                return SelectTourStructureDialog.selectStructure(choices);
            }

            @Override
            public List<? extends Pair<String, Set<? extends Change>>> selectIrrelevant(
                    List<? extends Pair<String, Set<? extends Change>>> choices) {
                if (choices.isEmpty()) {
                    return Collections.emptyList();
                }
                return SelectIrrelevantDialog.selectIrrelevant(choices);
            }
        };
        this.toursInReview = ToursInReview.create(
                this.changeSource,
                sourceUi,
                Arrays.asList(new WhitespaceChangeFilter(), new ImportChangeFilter()),
                Arrays.asList(new OneStopPerPartOfFileRestructuring()),
                createUi,
                ticketKey);
        if (this.toursInReview == null) {
            return false;
        }
        this.toursInReview.createMarkers(new RealMarkerFactory());
        return true;
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

        this.changeListener = new IResourceChangeListener() {
            @Override
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    this.logFirstAffectedFile(event.getDelta());
                } catch (final Exception e) {
                    Logger.error("error while sending telemetry events", e);
                }
            }

            private boolean logFirstAffectedFile(IResourceDelta delta) {
                if (delta.getResource().isDerived()) {
                    return false;
                }
                if (delta.getResource() instanceof IFile) {
                    if ((delta.getFlags() & IResourceDelta.CONTENT) == 0) {
                        return false;
                    }
                    Telemetry.event("fileChanged")
                        .param("path", delta.getFullPath())
                        .param("kind", delta.getKind())
                        .log();
                    return true;
                } else {
                    final int kindMask = IResourceDelta.ADDED | IResourceDelta.CHANGED | IResourceDelta.REMOVED;
                    for (final IResourceDelta d : delta.getAffectedChildren(kindMask, IResource.FILE)) {
                        final boolean alreadyLogged = this.logFirstAffectedFile(d);
                        if (alreadyLogged) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this.changeListener, IResourceChangeEvent.POST_CHANGE);
    }

    private void unregisterGlobalTelemetryListeners() {
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this.launchesListener);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this.changeListener);
    }

    @Override
    public void addEndReviewExtension(EndReviewExtension extension) {
        this.endReviewExtensions.add(extension);
    }

    @Override
    public void setStopViewer(IStopViewer stopViewer) {
        this.stopViewer = stopViewer;
    }

    public IStopViewer getStopViewer() {
        return this.stopViewer;
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
}
