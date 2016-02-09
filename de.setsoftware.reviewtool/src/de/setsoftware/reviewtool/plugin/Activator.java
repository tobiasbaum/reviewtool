package de.setsoftware.reviewtool.plugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	private static Activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		this.initializeDefaultPreferences();
	}

	public void initializeDefaultPreferences() {
		final IPreferenceStore store = this.getPreferenceStore();
		store.setDefault(ReviewToolPreferencePage.JIRA_URL, "http://jira:8080");
		store.setDefault(ReviewToolPreferencePage.JIRA_REVIEW_REMARK_FIELD, "Reviewanmerkungen");
		store.setDefault(ReviewToolPreferencePage.JIRA_REVIEW_STATE, "In Review");
		store.setDefault(ReviewToolPreferencePage.JIRA_IMPLEMENTATION_STATE, "In Implementation");
		store.setDefault(ReviewToolPreferencePage.USER, System.getProperty("user.name"));
		store.setDefault(ReviewToolPreferencePage.JIRA_PASSWORD, ""); //$NON-NLS-1$
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
	}

	public static Activator getDefault() {
		return plugin;
	}
}
