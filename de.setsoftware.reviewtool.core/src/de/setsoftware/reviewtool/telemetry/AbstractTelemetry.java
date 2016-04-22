package de.setsoftware.reviewtool.telemetry;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Interface to send telemetry data.
 */
public abstract class AbstractTelemetry {

    /**
     * Sends the event that a review remark has been created at the current moment in time.
     */
    public final void remarkCreated(String ticketKey, String reviewer, String remarkType, String resource, int line) {
        this.putData(
                "remarkCreated",
                ticketKey,
                reviewer,
                map(
                        "remarkType", remarkType,
                        "resource", obfuscate(resource),
                        "line", Integer.toString(line)));

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
