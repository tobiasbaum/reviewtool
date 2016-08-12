package de.setsoftware.reviewtool.telemetry;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Interface to send telemetry data.
 */
public abstract class AbstractTelemetry {

    private String currentTicketKey;
    private String currentUser;

    public AbstractTelemetry() {
        Logger.debug("create telemetry " + this.toString());
    }

    /**
     * Sets the ticket key and user that is used for the following events.
     */
    public void registerTicketAndUser(String ticketKey, String user) {
        Logger.debug("registerTicketAndUser " + user + " at " + this);
        if (user == null) {
            throw new AssertionError("user is null for ticket key " + ticketKey);
        }
        this.currentTicketKey = ticketKey;
        this.currentUser = user;
    }

    /**
     * Sends the event that a review remark has been created at the current moment in time.
     */
    public final void remarkCreated(String remarkType, String resource, int line) {
        this.putData(
                "remarkCreated",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "remarkType", remarkType,
                    "resource", resource,
                    "line", Integer.toString(line)));

    }

    public final void resolutionComment(String resource, int line) {
        this.logResolution("resolutionComment", resource, line);
    }

    public final void resolutionDelete(String resource, int line) {
        this.logResolution("resolutionDelete", resource, line);
    }

    public final void resolutionFixed(String resource, int line) {
        this.logResolution("resolutionFixed", resource, line);
    }

    public final void resolutionQuestion(String resource, int line) {
        this.logResolution("resolutionQuestion", resource, line);
    }

    public final void resolutionWontFix(String resource, int line) {
        this.logResolution("resolutionWontFix", resource, line);
    }

    private void logResolution(String resolutionType, String resource, int line) {
        this.putData(
                resolutionType,
                this.currentTicketKey,
                this.currentUser,
                map(
                    "resource", resource,
                    "line", Integer.toString(line)));
    }

    public void tourActivated(int index) {
        this.putData(
                "tourActivated",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "tourIndex", Integer.toString(index)));
    }

    /**
     * Sends the event that the user used the reviewtool's "jump to stop" function.
     */
    public void jumpedTo(String resource, int line, String typeOfJump) {
        this.putData(
                "jumpedTo",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "resource", resource,
                    "line", Integer.toString(line),
                    "type", typeOfJump));
    }

    /**
     * Sends the event that the active file(s) have changed. Is also called when inactivity ends.
     */
    public void activeFilesChanged(List<File> activeFiles) {
        this.putData(
                "activeFilesChanged",
                this.currentTicketKey,
                this.currentUser,
                map("files", activeFiles.toString()));
    }

    /**
     * Sends the event that the user is possibly inactive because there was no change for some time.
     */
    public void possibleInactivity(long timeWithoutChange) {
        this.putData(
                "possibleInactivity",
                this.currentTicketKey,
                this.currentUser,
                map("sinceMs", Long.toString(timeWithoutChange)));
    }

    /**
     * Sends the event that a launch (for example of JUnit tests) has occurred at the current moment in time.
     */
    public void launchOccured(String launchMode, String launchConfigName) {
        this.putData(
                "launch",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "mode", launchMode,
                    "config", launchConfigName));
    }

    /**
     * Sends the event that a file has been changed (and saved).
     */
    public void fileChanged(String path, int changeKind) {
        this.putData(
                "fileChanged",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "path", path,
                    "kind", Integer.toString(changeKind)));
    }

    /**
     * Generic log method.
     * Instead of using this method, its better to use a {@link TelemetryEventBuilder} (created by
     * {@link de.setsoftware.reviewtool.telemetry.Telemetry.#event(type)}).
     */
    public void log(String eventType, Map<String, String> params) {
        this.putData(
                eventType,
                this.currentTicketKey,
                this.currentUser,
                params);
    }

    /**
     * Hashes the given string so that the real value is not directly visible.
     */
    protected static String obfuscate(String data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data.getBytes("UTF-8"));
            final byte[] mdbytes = md.digest();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
                final int b = 0xFF & mdbytes[i];
                if (b < 16) {
                    sb.append('0').append(Integer.toHexString(b));
                } else {
                    sb.append(Integer.toHexString(b));
                }
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new ReviewtoolException(e);
        }
    }

    private static Map<String, String> map(String... keysAndValues) {
        assert keysAndValues.length % 2 == 0;
        final LinkedHashMap<String, String> ret = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            ret.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return ret;
    }

    /**
     * Sends a telemetry event.
     */
    protected abstract void putData(
            String eventType,
            String ticketKey,
            String user,
            Map<String, String> furtherProperties);

}
