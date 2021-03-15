package de.setsoftware.reviewtool.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for telemetry log messages.
 */
public class TelemetryEventBuilder {

    private final AbstractTelemetry telemetry;
    private final String type;
    private final Map<String, String> params;

    TelemetryEventBuilder(AbstractTelemetry telemetry, String type) {
        this.telemetry = telemetry;
        this.type = type;
        this.params = new LinkedHashMap<>();
    }

    /**
     * Add a parameter with the given key and value.
     */
    public TelemetryEventBuilder param(String name, Object value) {
        assert !this.params.containsKey(name);
        this.params.put(name, value.toString());
        return this;
    }

    /**
     * Add a parameter with the given key and value.
     */
    public TelemetryEventBuilder param(String name, long value) {
        return this.param(name, Long.toString(value));
    }

    /**
     * Calls the given param source, which can then add parameters to this event.
     * Can be used to make fluent event creation look nicer.
     */
    public TelemetryEventBuilder params(TelemetryParamSource paramSource) {
        paramSource.addParams(this);
        return this;
    }

    /**
     * End building the message and log it to the telemetry provider.
     */
    public void log() {
        this.telemetry.log(this.type, this.params);
    }

}
