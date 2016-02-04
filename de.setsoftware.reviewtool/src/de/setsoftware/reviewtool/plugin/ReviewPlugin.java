package de.setsoftware.reviewtool.plugin;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

import de.setsoftware.reviewtool.connectors.jira.JiraPersistence;
import de.setsoftware.reviewtool.model.Constants;
import de.setsoftware.reviewtool.model.DummyMarker;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ReviewData;

public class ReviewPlugin {

	public static enum Mode {
		IDLE,
		REVIEWING,
		FIXING
	}

	private static final ReviewPlugin INSTANCE = new ReviewPlugin();
	private final JiraPersistence persistence;
	private Mode mode = Mode.IDLE;
	private final List<WeakReference<ReviewPluginModeService>> modeListeners = new ArrayList<>();

	private ReviewPlugin() {
		final IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
		this.persistence = new JiraPersistence(
				pref.getString("url"),
				pref.getString("reviewRemarkField"),
				pref.getString("reviewState"),
				pref.getString("user"),
				pref.getString("password"));
	}

	public static IReviewPersistence getPersistence() {
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
			return;
		}
		this.clearMarkers();
		final String currentReviewData = this.persistence.getCurrentReviewData();
		if (currentReviewData == null) {
			this.setMode(Mode.IDLE);
			return;
		}
		ReviewData.parse(
				this.persistence,
				new RealMarkerFactory(),
				currentReviewData);
		this.setMode(targetMode);
	}

	private void clearMarkers() throws CoreException {
		ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(
				Constants.REVIEWMARKER_ID, true, IResource.DEPTH_INFINITE);
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
		this.clearMarkers();
		this.setMode(Mode.IDLE);
	}

	public void endFixing() throws CoreException {
		this.clearMarkers();
		this.setMode(Mode.IDLE);
	}

	public boolean hasUnresolvedRemarks() {
		final ReviewData d = ReviewData.parse(
				this.persistence,
				DummyMarker.FACTORY,
				this.persistence.getCurrentReviewData());
		return d.hasUnresolvedRemarks();
	}

	public boolean hasTemporaryMarkers() {
		final ReviewData d = ReviewData.parse(
				this.persistence,
				DummyMarker.FACTORY,
				this.persistence.getCurrentReviewData());
		return d.hasTemporaryMarkers();
	}

	public void refreshMarkers() throws CoreException {
		this.clearMarkers();
		final String currentReviewData = this.persistence.getCurrentReviewData();
		ReviewData.parse(
				this.persistence,
				new RealMarkerFactory(),
				currentReviewData);
	}

}
