package de.setsoftware.reviewtool.telemetry;

/**
 * Simple functional interface for strategies that determine event parameters.
 */
public interface TelemetryParamSource {

    /**
     * Add the parameters to the given event.
     */
    public abstract void addParams(TelemetryEventBuilder event);

}
