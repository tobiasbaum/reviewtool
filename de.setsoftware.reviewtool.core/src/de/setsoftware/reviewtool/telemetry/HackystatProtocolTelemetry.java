package de.setsoftware.reviewtool.telemetry;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Map;

import org.osgi.framework.Version;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.unihannover.se.hackybuffer.Hackybuffer;
import de.unihannover.se.hackybuffer.HackybufferException;

/**
 * Send telemetry data using the Hackystat protocol.
 *
 * <p>The Hackystat project is more or less dead, but we need to use some protocol anyway, so why not use the one from
 * Hackystat, so that some existing tools can be reused.
 */
public class HackystatProtocolTelemetry extends AbstractTelemetry {

    private final Hackybuffer hacky;
    private final Version version;

    public HackystatProtocolTelemetry(String dir, Version pluginVersion) {
        try {
            this.hacky = new Hackybuffer(new File(dir));
            this.version = pluginVersion;
        } catch (final FileNotFoundException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    protected void putData(
            String eventType,
            String ticketKey,
            String user,
            Map<String, String> furtherProperties) {

        try {
            //TODO use a more specific tool name
            this.hacky.writeData(
                    new Date(),
                    "Code Review Tool " + this.version,
                    eventType,
                    ticketKey,
                    obfuscate(user),
                    furtherProperties);
        } catch (final HackybufferException e1) {
            throw new ReviewtoolException(e1);
        }
    }

}
