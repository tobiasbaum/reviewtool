package de.setsoftware.reviewtool.telemetry;

import java.util.Arrays;
import java.util.Collection;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configuration for telemetry.
 */
public class TelemetryConfigurator implements IConfigurator {

    private static final String HACKYSTAT_TELEMETRY = "hackystatTelemetry";
    private static final String NO_TELEMETRY = "noTelemetry";

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
                Telemetry.set(new HackystatProtocolTelemetry(xml.getAttribute("dir")));
            } else {
                Logger.info("opting out from telemetry");
                Telemetry.set(new NoTelemetry());
            }
        }
    }

}
