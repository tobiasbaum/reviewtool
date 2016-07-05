package de.setsoftware.reviewtool.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
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
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.WeakListeners;
import de.setsoftware.reviewtool.changesources.svn.SvnChangesourceConfigurator;
import de.setsoftware.reviewtool.config.ConfigurationInterpreter;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.connectors.file.FilePersistence;
import de.setsoftware.reviewtool.connectors.file.FileTicketConnectorConfigurator;
import de.setsoftware.reviewtool.connectors.jira.JiraConnectorConfigurator;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.DummyMarker;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ISyntaxFixer;
import de.setsoftware.reviewtool.model.ITicketChooser;
import de.setsoftware.reviewtool.model.IUserInteraction;
import de.setsoftware.reviewtool.model.ReviewData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.slicingalgorithms.OneTourPerCommit;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.CorrectSyntaxDialog;
import de.setsoftware.reviewtool.ui.dialogs.EndReviewDialog;
import de.setsoftware.reviewtool.ui.dialogs.SelectTicketDialog;
import de.setsoftware.reviewtool.ui.views.ImageCache;
import de.setsoftware.reviewtool.ui.views.RealMarkerFactory;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;
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


    private ReviewPlugin() {
        this.persistence = new ReviewStateManager(new FilePersistence(new File("."), "please configure"), new RealUi());

        final Version bundleVersion = Activator.getDefault().getBundle().getVersion();
        this.configInterpreter.addConfigurator(new FileTicketConnectorConfigurator());
        this.configInterpreter.addConfigurator(new JiraConnectorConfigurator());
        this.configInterpreter.addConfigurator(new SvnChangesourceConfigurator());
        this.configInterpreter.addConfigurator(new TelemetryConfigurator(bundleVersion));
        this.configInterpreter.addConfigurator(new VersionChecker(bundleVersion));
        this.reconfigure();

        Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                ReviewPlugin.this.reconfigure();
            }
        });
    }

    private void reconfigure() {
        this.changeSource = null;

        try {
            final IPreferenceStore pref = getPrefs();
            final Document config = ConfigurationInterpreter.load(
                    pref.getString(ReviewToolPreferencePage.TEAM_CONFIG_FILE));
            final Map<String, String> userParams = ReviewToolPreferencePage.getUserParams(config, pref);
            this.configInterpreter.configure(config, userParams, this);
        } catch (IOException | SAXException | ParserConfigurationException | ReviewtoolException e) {
            Logger.error("error while loading config", e);
            MessageDialog.openError(null, "Fehler beim Laden der Konfiguration", e.toString());
        }
    }

    private static IPreferenceStore getPrefs() {
        final IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
        pref.setDefault(ConfigurationInterpreter.USER_PARAM_NAME, System.getProperty("user.name"));
        return pref;
    }

    private static String getUserPref() {
        return getPrefs().getString(ConfigurationInterpreter.USER_PARAM_NAME);
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
        this.loadReviewData(Mode.REVIEWING);
        if (this.mode == Mode.REVIEWING) {
            this.switchToReviewPerspective();
            Telemetry.get().reviewStarted(
                    this.persistence.getTicketKey(),
                    this.persistence.getReviewerForCurrentRound(),
                    this.persistence.getCurrentRound(),
                    this.toursInReview.getNumberOfTours(),
                    this.toursInReview.getNumberOfStops(),
                    this.toursInReview.getNumberOfFragments(),
                    this.toursInReview.getNumberOfAddedLines(),
                    this.toursInReview.getNumberOfRemovedLines());
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
        this.loadReviewData(Mode.FIXING);
        if (this.mode == Mode.FIXING) {
            Telemetry.get().fixingStarted(
                    this.persistence.getTicketKey(),
                    getUserPref(),
                    this.persistence.getCurrentRound());
        }
    }

    private void loadReviewData(Mode targetMode) throws CoreException {
        this.persistence.resetKey();
        final boolean ok = this.persistence.selectTicket(targetMode == Mode.REVIEWING);
        if (!ok) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
            return;
        }
        this.clearMarkers();

        this.loadToursAndCreateMarkers();

        final ReviewData currentReviewData = CorrectSyntaxDialog.getCurrentReviewDataParsed(
                this.persistence, new RealMarkerFactory());
        if (currentReviewData == null) {
            this.setMode(Mode.IDLE);
            this.toursInReview = null;
            return;
        }
        this.setMode(targetMode);
    }

    private void clearMarkers() throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.REVIEWMARKER_ID, true, IResource.DEPTH_INFINITE);
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
        final EndTransition typeOfEnd =
                EndReviewDialog.selectTypeOfEnd(this.persistence, this.getCurrentReviewDataParsed());
        if (typeOfEnd == null) {
            return;
        }
        if (typeOfEnd.getType() != EndTransition.Type.PAUSE
                && this.getCurrentReviewDataParsed().hasTemporaryMarkers()) {
            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            final boolean yes = MessageDialog.openQuestion(shell, "Offene Marker",
                    "Es gibt noch temporäre Marker. Trotzdem abschließen?");
            if (!yes) {
                return;
            }
        }
        TrackerManager.get().stopTracker();
        Telemetry.get().reviewEnded(
                this.persistence.getTicketKey(),
                this.persistence.getReviewerForCurrentRound(),
                this.persistence.getCurrentRound(),
                typeOfEnd.getNameForUser());
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
    }

    /**
     * Ends fixing and changes the ticket's state accordingly.
     */
    public void endFixing() throws CoreException {
        Telemetry.get().fixingEnded(
                this.persistence.getTicketKey(),
                this.persistence.getReviewerForCurrentRound(),
                this.persistence.getCurrentRound());
        this.persistence.changeStateToReadyForReview();
        this.leaveReviewMode();
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
        CorrectSyntaxDialog.getCurrentReviewDataParsed(this.persistence, new RealMarkerFactory());
    }

    private void loadToursAndCreateMarkers() {
        final String ticketKey = this.persistence.getTicketKey();
        if (ticketKey == null) {
            return;
        }
        final IChangeSourceUi sourceUi = new IChangeSourceUi() {
            @Override
            public boolean handleLocalWorkingCopyOutOfDate(String detailInfo) {
                return MessageDialog.openQuestion(
                        null, "Arbeitskopie veraltet",
                        "Die Arbeitskopie (" + detailInfo
                        + ") enthält nicht alle relevanten Änderungen. Soll ein Update durchgeführt werden?");
            }
        };
        this.toursInReview = ToursInReview.create(
                this.changeSource,
                sourceUi,
                new OneTourPerCommit(this.changeSource.createTracer()),
                ticketKey);
        this.toursInReview.createMarkers(new RealMarkerFactory());
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

}
