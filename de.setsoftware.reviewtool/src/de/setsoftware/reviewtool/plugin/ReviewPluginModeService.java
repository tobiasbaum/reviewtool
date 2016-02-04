package de.setsoftware.reviewtool.plugin;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

public class ReviewPluginModeService extends AbstractSourceProvider {

	private final static String MODE_VAR = "de.setsoftware.reviewtool.mode";

	public ReviewPluginModeService() {
		ReviewPlugin.getInstance().registerModeListener(this);
	}

	void notifyModeChanged() {
		this.fireSourceChanged(ISources.WORKBENCH, MODE_VAR, this.getMode());
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

}
