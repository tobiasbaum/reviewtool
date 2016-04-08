package de.setsoftware.reviewtool.plugin;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.connectors.file.FilePersistence;
import de.setsoftware.reviewtool.connectors.jira.JiraPersistence;
import de.setsoftware.reviewtool.dialogs.CorrectSyntaxDialog;
import de.setsoftware.reviewtool.dialogs.EndReviewDialog;
import de.setsoftware.reviewtool.dialogs.ReviewInfoDialog;
import de.setsoftware.reviewtool.dialogs.SelectTicketDialog;
import de.setsoftware.reviewtool.fragmenttracers.svn.BasicFragmentTracer;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.DummyMarker;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ISyntaxFixer;
import de.setsoftware.reviewtool.model.ITicketChooser;
import de.setsoftware.reviewtool.model.IUserInteraction;
import de.setsoftware.reviewtool.model.ReviewData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.ISliceSource;
import de.setsoftware.reviewtool.model.changestructure.SlicesInReview;
import de.setsoftware.reviewtool.slicesources.svn.SvnSliceSource;

/**
 * Plugin that handles the review workflow and ties together the different parts.
 */
public class ReviewPlugin {

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
    private final ISliceSource sliceSource;
    private SlicesInReview slicesInReview;
    private Mode mode = Mode.IDLE;
    private final List<WeakReference<ReviewPluginModeService>> modeListeners = new ArrayList<>();

    private ReviewPlugin() {
        final IReviewPersistence persistence = createPersistenceFromPreferences();
        this.sliceSource = createSliceSource();
        this.persistence = new ReviewStateManager(persistence, new RealUi());
        Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (ReviewToolPreferencePage.getAllPreferenceIds().contains(event.getProperty())) {
                    ReviewPlugin.this.persistence.setPersistence(createPersistenceFromPreferences());
                }
            }
        });
    }

    private static IReviewPersistence createPersistenceFromPreferences() {
        final IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
        pref.setDefault(ReviewToolPreferencePage.USER, System.getProperty("user.name"));
        final String user = pref.getString(ReviewToolPreferencePage.USER);
        final IReviewPersistence persistence;
        if (pref.getBoolean(ReviewToolPreferencePage.JIRA_SOURCE)) {
            persistence = new JiraPersistence(
                    pref.getString(ReviewToolPreferencePage.JIRA_URL),
                    pref.getString(ReviewToolPreferencePage.JIRA_REVIEW_REMARK_FIELD),
                    pref.getString(ReviewToolPreferencePage.JIRA_REVIEW_STATE),
                    pref.getString(ReviewToolPreferencePage.JIRA_IMPLEMENTATION_STATE),
                    pref.getString(ReviewToolPreferencePage.JIRA_READY_FOR_REVIEW_STATE),
                    pref.getString(ReviewToolPreferencePage.JIRA_REJECTED_STATE),
                    pref.getString(ReviewToolPreferencePage.JIRA_DONE_STATE),
                    user,
                    pref.getString(ReviewToolPreferencePage.JIRA_PASSWORD));
        } else {
            persistence = new FilePersistence(new File(pref.getString(ReviewToolPreferencePage.FILE_PATH)), user);
        }
        return persistence;
    }

    private static ISliceSource createSliceSource() {
        final List<File> projectDirs = new ArrayList<>();
        final IWorkspace root = ResourcesPlugin.getWorkspace();
        for (final IProject project : root.getRoot().getProjects()) {
            final IPath location = project.getLocation();
            if (location != null) {
                projectDirs.add(location.toFile());
            }
        }
        return new SvnSliceSource(projectDirs, ".*${key}([^0-9].*)?");
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

    public void startReview() throws CoreException {
        this.loadReviewData(Mode.REVIEWING);
    }

    public void startFixing() throws CoreException {
        this.loadReviewData(Mode.FIXING);
    }

    private void loadReviewData(Mode targetMode) throws CoreException {
        this.persistence.resetKey();
        final boolean ok = this.persistence.selectTicket(targetMode == Mode.REVIEWING);
        if (!ok) {
            this.setMode(Mode.IDLE);
            this.slicesInReview = null;
            return;
        }
        this.clearMarkers();

        this.loadSlicesAndCreateMarkers();

        final ReviewData currentReviewData = CorrectSyntaxDialog.getCurrentReviewDataParsed(
                this.persistence, new RealMarkerFactory());
        if (currentReviewData == null) {
            this.setMode(Mode.IDLE);
            this.slicesInReview = null;
            return;
        }
        this.setMode(targetMode);
    }

    private void clearMarkers() throws CoreException {
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.REVIEWMARKER_ID, true, IResource.DEPTH_INFINITE);
        ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
                Constants.FRAGMENTMARKER_ID, true, IResource.DEPTH_INFINITE);
    }

    private void setMode(Mode mode) {
        if (mode != this.mode) {
            this.mode = mode;
            final Iterator<WeakReference<ReviewPluginModeService>> iter = this.modeListeners.iterator();
            while (iter.hasNext()) {
                final ReviewPluginModeService s = iter.next().get();
                if (s != null) {
                    s.notifyModeChanged();
                } else {
                    iter.remove();
                }
            }
        }
    }

    public void registerModeListener(ReviewPluginModeService reviewPluginModeService) {
        this.modeListeners.add(new WeakReference<>(reviewPluginModeService));
    }

    public void endReview() throws CoreException {
        final EndReviewDialog.TypeOfEnd typeOfEnd =
                EndReviewDialog.selectTypeOfEnd(this.persistence, this.getCurrentReviewDataParsed());
        if (typeOfEnd == null) {
            return;
        }
        if (typeOfEnd != EndReviewDialog.TypeOfEnd.PAUSE
                && this.getCurrentReviewDataParsed().hasTemporaryMarkers()) {
            final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            final boolean yes = MessageDialog.openQuestion(shell, "Offene Marker",
                    "Es gibt noch temporäre Marker. Trotzdem abschließen?");
            if (!yes) {
                return;
            }
        }
        switch (typeOfEnd) {
        case ANOTHER_REVIEW:
            this.persistence.changeStateToReadyForReview();
            break;
        case OK:
            this.persistence.changeStateToDone();
            break;
        case REJECTED:
            this.persistence.changeStateToRejected();
            break;
        case PAUSE:
            break;
        default:
            throw new AssertionError();
        }
        this.leaveReviewMode();
    }

    private void leaveReviewMode() throws CoreException {
        this.clearMarkers();
        this.setMode(Mode.IDLE);
        this.slicesInReview = null;
    }

    public void endFixing() throws CoreException {
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
        this.loadSlicesAndCreateMarkers();
        CorrectSyntaxDialog.getCurrentReviewDataParsed(this.persistence, new RealMarkerFactory());
    }

    private void loadSlicesAndCreateMarkers() {
        final String ticketKey = this.persistence.getTicketKey();
        if (ticketKey == null) {
            return;
        }
        this.slicesInReview = SlicesInReview.create(
                this.sliceSource,
                new BasicFragmentTracer(),
                ticketKey);
        this.slicesInReview.createMarkers(new RealMarkerFactory());
        this.slicesInReview.showInfo();
    }

    public void showReviewInfo() {
        ReviewInfoDialog.show(this.persistence);
    }

}
