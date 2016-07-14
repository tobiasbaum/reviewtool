package de.setsoftware.reviewtool.telemetry;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Interface to send telemetry data.
 */
public abstract class AbstractTelemetry {

    private String currentTicketKey;
    private String currentUser;

    /**
     * Sets the ticket key and user that is used for the following events.
     */
    public void registerTicketAndUser(String ticketKey, String user) {
        this.currentTicketKey = ticketKey;
        this.currentUser = user;
    }

    /**
     * Sends the event that a review has been started at the current moment in time.
     */
    public final void reviewStarted(String ticketKey, String reviewer, int round,
            int numberOfTours, int numberOfStops, int numberOfFragments,
            int numberOfAddedLines, int numberOfRemovedLines) {
        this.currentTicketKey = ticketKey;
        this.currentUser = reviewer;
        this.putData(
                "reviewStarted",
                ticketKey,
                reviewer,
                map("round", Integer.toString(round),
                    "cntTours", Integer.toString(numberOfTours),
                    "cntStops", Integer.toString(numberOfStops),
                    "cntFragments", Integer.toString(numberOfFragments),
                    "cntAddedLines", Integer.toString(numberOfAddedLines),
                    "cntRemovedLines", Integer.toString(numberOfRemovedLines)));
    }

    /**
     * Sends the event that a remark fixing has been started at the current moment in time.
     */
    public final void fixingStarted(String ticketKey, String reviewer, int round) {
        this.currentTicketKey = ticketKey;
        this.currentUser = reviewer;
        this.putData(
                "fixingStarted",
                ticketKey,
                reviewer,
                map("round", Integer.toString(round)));
    }

    /**
     * Sends the event that a review has been finished at the current moment in time.
     */
    public final void reviewEnded(String ticketKey, String reviewer, int round, String endTransition) {
        this.putData(
                "reviewEnded",
                ticketKey,
                reviewer,
                map(
                        "round", Integer.toString(round),
                        "endTransition", endTransition));
    }

    /**
     * Sends the event that a remark fixing has been finished at the current moment in time.
     */
    public final void fixingEnded(String ticketKey, String reviewer, int round) {
        this.putData(
                "fixingEnded",
                ticketKey,
                reviewer,
                map("round", Integer.toString(round)));
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

    public void toursMerged(List<Integer> mergedTourIndices, int numberOfTours, int numberOfStops) {
        this.putData(
                "toursMerged",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "mergedTourIndices", mergedTourIndices.toString(),
                    "newNumberOfTours", Integer.toString(numberOfTours),
                    "newNumberOfStops", Integer.toString(numberOfStops)));
    }

    public void tourActivated(int index) {
        this.putData(
                "tourActivated",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "tourIndex", Integer.toString(index)));
    }

    public void jumpedTo(String resource, int line) {
        this.putData(
                "jumpedTo",
                this.currentTicketKey,
                this.currentUser,
                map(
                    "resource", resource,
                    "line", Integer.toString(line)));
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
