package de.setsoftware.reviewtool.plugin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.changestructure.SlicesInReview;
import de.setsoftware.reviewtool.ui.views.ReviewModeListener;

/**
 * Services that provides and allows listening to the current review mode.
 */
public class ReviewPluginModeService extends AbstractSourceProvider implements ReviewModeListener {

    private static final String MODE_VAR = "de.setsoftware.reviewtool.mode";

    public ReviewPluginModeService() {
        ReviewPlugin.getInstance().registerModeListener(this);
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] {MODE_VAR};
    }

    @Override
    public Map<Object, Object> getCurrentState() {
        final Map<Object, Object> map = new HashMap<>(1);
        map.put(MODE_VAR, this.getMode());
        return map;
    }

    private String getMode() {
        return ReviewPlugin.getInstance().getMode().name();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, SlicesInReview slices) {
        this.notifyModeChanged();
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        this.notifyModeChanged();
    }

    @Override
    public void notifyIdle() {
        this.notifyModeChanged();
    }

    private void notifyModeChanged() {
        this.fireSourceChanged(ISources.WORKBENCH, MODE_VAR, this.getMode());
    }

}
