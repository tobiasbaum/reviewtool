package de.setsoftware.reviewtool.plugin;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.dialogs.MessageDialog;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * A pseudo configurator that checks whether the current plugin satisfies a certain minimum version configured
 * in the config. Can be used to ensure that the whole team updates when updates cannot be performed automatically.
 */
public class VersionChecker implements IConfigurator {

    private final Version currentVersion;

    public VersionChecker(Version currentVersion) {
        this.currentVersion = currentVersion;
    }

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singletonList("versionCheck");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final Version minVersion = Version.parseVersion(xml.getAttribute("minVersion"));
        if (this.currentVersion.compareTo(minVersion) < 0) {
            MessageDialog.openWarning(null, "CoRT version out of date",
                    String.format(
                            "The installed version of CoRT is too old. Configured minimum is %s,"
                            + " installed is %s. Please update.",
                            minVersion, this.currentVersion));
        }
    }

}
