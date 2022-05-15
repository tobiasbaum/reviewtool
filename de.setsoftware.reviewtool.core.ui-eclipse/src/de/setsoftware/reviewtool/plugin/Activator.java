package de.setsoftware.reviewtool.plugin;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.api.IStopViewer;
import de.setsoftware.reviewtool.ui.dialogs.DialogHelper;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;
import de.setsoftware.reviewtool.ui.views.ViewDataSource;

/**
 * Main class (i.e. "Activator") for the plugin.
 */
public class Activator extends AbstractUIPlugin {

    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        plugin = this;
        Logger.setLogger(new Logger() {
            @Override
            protected void log(int status, String message) {
                Activator.this.getLog().log(
                        new Status(status, Activator.this.getBundle().getSymbolicName(), message));
            }

            @Override
            protected void log(int status, String message, Throwable exception) {
                Activator.this.getLog().log(
                        new Status(status, Activator.this.getBundle().getSymbolicName(), message, exception));
            }
        });
        BackgroundJobExecutor.setInstance(new EclipseBackgroundJobExecutor());
        DialogHelper.setPreferenceStore((IPersistentPreferenceStore) this.getPreferenceStore());
        ViewDataSource.setInstance(new ViewDataSource() {
            @Override
            public void registerListener(ReviewModeListener l) {
                ReviewPlugin.getInstance().registerAndNotifyModeListener(l);
            }

            @Override
            public ToursInReview getToursInReview() {
                return ReviewPlugin.getInstance().getTours();
            }

            @Override
            public IStopViewer getStopViewer() {
                return ReviewPlugin.getInstance().getStopViewer();
            }
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        DialogHelper.setPreferenceStore(null);
        Logger.setLogger(null);
    }

    public static Activator getDefault() {
        return plugin;
    }
}
