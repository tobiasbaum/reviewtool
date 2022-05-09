package de.setsoftware.reviewtool.telemetry;

import java.util.Map;

/**
 * Telemetry implementation that does not send telemetry data.
 */
public class NoTelemetry extends AbstractTelemetry {

    @Override
    protected void putData(String eventType, String ticketKey, String user, Map<String, String> furtherProperties) {
    }

}
