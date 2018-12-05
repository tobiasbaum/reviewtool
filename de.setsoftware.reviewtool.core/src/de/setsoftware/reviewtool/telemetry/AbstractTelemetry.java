package de.setsoftware.reviewtool.telemetry;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Interface to send telemetry data.
 */
public abstract class AbstractTelemetry {

    private String currentSessionId;
    private String currentUser;
    private long lastId;

    public AbstractTelemetry() {
        Logger.debug("create telemetry " + this.toString());
    }

    /**
     * Sets the session key for the following events.For each call, a new session ID is generated.
     */
    public void registerSession(String ticketKey, String user, String type, int round) {
        Logger.debug("registerSession " + user + ", " + ticketKey + ", " + type + round + " at " + this);
        if (user == null) {
            throw new AssertionError("user is null for ticket key " + ticketKey);
        }
        this.currentSessionId = ticketKey + "," + type + "," + round + "," + Long.toHexString(this.getSessionUid());
        this.currentUser = user;
    }

    private long getSessionUid() {
        long uid = System.currentTimeMillis();
        if (uid == this.lastId) {
            uid++;
        }
        this.lastId = uid;
        return uid;
    }

    /**
     * Generic log method.
     * Instead of using this method, its better to use a {@link TelemetryEventBuilder} (created by
     * {@link de.setsoftware.reviewtool.telemetry.Telemetry#event(String)}).
     */
    public void log(String eventType, Map<String, String> params) {
        this.putData(
                eventType,
                this.currentSessionId,
                this.currentUser,
                params);
    }

    /**
     * Hashes the given string so that the real value is not directly visible.
     */
    protected static String obfuscate(String data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data == null ? new byte[0] : data.getBytes("UTF-8"));
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

    /**
     * Sends a telemetry event.
     */
    protected abstract void putData(
            String eventType,
            String ticketKey,
            String user,
            Map<String, String> furtherProperties);

}
