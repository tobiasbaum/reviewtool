package de.setsoftware.reviewtool.plugin;

import java.util.Arrays;
import java.util.Collection;

import org.osgi.framework.Version;
import org.w3c.dom.Element;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.telemetry.HackystatProtocolTelemetry;
import de.setsoftware.reviewtool.telemetry.NoTelemetry;
import de.setsoftware.reviewtool.telemetry.Telemetry;

/**
 * Configuration for telemetry.
 */
public class TelemetryConfigurator implements IConfigurator {

    private static final String HACKYSTAT_TELEMETRY = "hackystatTelemetry";
    private static final String NO_TELEMETRY = "noTelemetry";

    private final Version version;

    public TelemetryConfigurator(Version pluginVersion) {
        this.version = pluginVersion;
    }

    @Override
    public Collection<String> getRelevantElementNames() {
        return Arrays.asList(HACKYSTAT_TELEMETRY, NO_TELEMETRY);
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        if (xml.getNodeName().equals(NO_TELEMETRY)) {
            Telemetry.set(new NoTelemetry());
        } else {
            final String optOut = xml.getAttribute("optOut");
            if (optOut.isEmpty()) {
                Telemetry.set(new HackystatProtocolTelemetry(xml.getAttribute("dir"), this.version));
            } else {
                Logger.info("opting out from telemetry");
                Telemetry.set(new NoTelemetry());
            }
        }
    }

}
