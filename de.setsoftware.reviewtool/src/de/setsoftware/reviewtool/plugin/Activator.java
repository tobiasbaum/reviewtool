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
		store.setDefault("url", "http://jira.intranet.set.de:8080");
		store.setDefault("reviewRemarkField", "Reviewanmerkungen");
		store.setDefault("reviewState", "In Review");
		store.setDefault("user", "");
		store.setDefault("password", ""); //$NON-NLS-1$
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
	}

	public static Activator getDefault() {
		return plugin;
	}
}
