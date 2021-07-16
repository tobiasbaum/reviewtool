package de.setsoftware.reviewtool.ticketconnectors.jira;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.EndTransition.Type;
import de.setsoftware.reviewtool.model.ITicketConnector;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.model.TicketLinkSettings;

/**
 * Persists review comments in a special field of JIRA tickets.
 */
public class JiraPersistence implements ITicketConnector {

    /**
     * Wrapper for the JSON data of a JIRA ticket.
     */
    private final class JiraTicket implements ITicketData {

        private final JsonObject ticket;

        public JiraTicket(JsonObject object) {
            this.ticket = object;
        }

        @Override
        public String getReviewData() {
            final JsonValue reviewData =
                    this.ticket.get("fields").asObject().get(JiraPersistence.this.getReviewFieldId());
            if (reviewData == null || reviewData.isNull()) {
                return "";
            } else {
                return reviewData.asString();
            }
        }

        @Override
        public String getReviewerForRound(int number) {
            final JsonArray histories = JiraPersistence.this.getHistories(this.ticket);
            int count = 0;
            for (final JsonValue v : histories) {
                if (JiraPersistence.this.isToReview(v)) {
                    count++;
                    if (count == number) {
                        return JiraPersistence.this.getToUser(v).toUpperCase();
                    }
                }
            }
            return JiraPersistence.this.user.toUpperCase();
        }

        @Override
        public Date getEndTimeForRound(int number) {
            final JsonArray histories = JiraPersistence.this.getHistories(this.ticket);
            int count = 0;
            for (final JsonValue v : histories) {
                if (JiraPersistence.this.isToReview(v)) {
                    count++;
                    if (count == number) {
                        return JiraPersistence.this.getTimeOfHistoryItem(v);
                    }
                }
            }
            return new Date();
        }

        @Override
        public int getCurrentRound() {
            final JsonArray histories = JiraPersistence.this.getHistories(this.ticket);
            int count = 0;
            for (final JsonValue v : histories) {
                if (JiraPersistence.this.isToReview(v)) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public TicketInfo getTicketInfo() {
            return JiraPersistence.this.queryTickets("key=" + this.getId()).get(0);
        }

        @Override
        public String getId() {
            return this.ticket.get("key").asString();
        }

    }

    private final class StateIds {
        private final String reviewStateId;
        private final String implementationStateId;
        private final String readyForReviewStateId;
        private final String rejectedStateId;
        private final String doneStateId;

        public StateIds() {
            final JsonArray states = JiraPersistence.this.loadStates();
            this.reviewStateId = this.getStateId(states, JiraPersistence.this.reviewStateName);
            this.implementationStateId = this.getStateId(states, JiraPersistence.this.implementationStateName);
            this.readyForReviewStateId = this.getStateId(states, JiraPersistence.this.readyForReviewStateName);
            this.rejectedStateId = this.getStateId(states, JiraPersistence.this.rejectedStateName);
            this.doneStateId = this.getStateId(states, JiraPersistence.this.doneStateName);
        }

        private String getStateId(JsonArray states, String stateName) {
            final List<String> possibleNames = new ArrayList<>();
            for (final JsonValue v : states) {
                final String name = v.asObject().get("name").asString();
                if (name.equals(stateName)) {
                    return v.asObject().get("id").asString();
                }
                possibleNames.add(name);
            }
            throw new ReviewtoolException("Status " + stateName + " not found in JIRA."
                    + " Possible values: " + possibleNames);
        }

    }

    private final String url;
    private final String reviewFieldName;
    private final String user;
    private final String password;
    private final String reviewStateName;
    private final String implementationStateName;
    private final String readyForReviewStateName;
    private final String rejectedStateName;
    private final String doneStateName;
    private StateIds stateIds;
    private final Map<String, String> filtersForReview;
    private final Map<String, String> filtersForFixing;

    private String reviewFieldId;
    private final TicketLinkSettings linkSettings;
    private final File cookiesFile;

    private final String oauthIssuer;
    private final String oauthAudience;
    private final String oauthClientID;
    private final String oauthClientSecret;

    public JiraPersistence(
            String url,
            String reviewFieldName,
            String reviewState,
            String implementationState,
            String readyForReviewState,
            String rejectedState,
            String doneState,
            String user,
            String password,
            TicketLinkSettings linkSettings,
            File cookiesFile,
            String oauthIssuer,
            String oauthAudience,
            String oauthClientID,
            String oauthClientSecret) {
        this.url = url;
        this.reviewFieldName = reviewFieldName;
        this.user = user;
        this.password = password;
        this.cookiesFile = cookiesFile;
        this.reviewStateName = reviewState;
        this.implementationStateName = implementationState;
        this.readyForReviewStateName = readyForReviewState;
        this.rejectedStateName = rejectedState;
        this.doneStateName = doneState;
        this.filtersForReview = new LinkedHashMap<>();
        this.filtersForFixing = new LinkedHashMap<>();
        this.linkSettings = linkSettings;

        this.oauthAudience = oauthAudience;
        this.oauthClientID = oauthClientID;
        this.oauthClientSecret = oauthClientSecret;

        if (!oauthAudience.isEmpty() && !oauthClientID.isEmpty() && !oauthClientSecret.isEmpty()) {
            if (oauthIssuer.isEmpty()) {
                this.oauthIssuer = this.ensureSlashAtEnd(url);
            } else {
                this.oauthIssuer = this.ensureSlashAtEnd(oauthIssuer);
            }
        } else {
            this.oauthIssuer = null;
        }
    }

    private String ensureSlashAtEnd(final String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private StateIds sids() {
        if (this.stateIds != null) {
            return this.stateIds;
        }
        synchronized (this) {
            if (this.stateIds == null) {
                this.stateIds = new StateIds();
            }
            return this.stateIds;
        }
    }

    private JsonArray loadStates() {
        final String getUrl = String.format(
                "%s/rest/api/latest/status?%s",
                this.url,
                this.getAuthParams());
        return this.performGet(getUrl).asArray();
    }

    @Override
    public void saveReviewData(String ticketKey, String newData) {
        final String putUrl = String.format(
                "%s/rest/api/latest/issue/%s?%s", this.url, ticketKey, this.getAuthParams());
        final JsonObject fields = new JsonObject();
        fields.add(this.getReviewFieldId(), newData);
        final JsonObject json = new JsonObject();
        json.add("fields", fields);
        this.performPut(putUrl, json);
    }

    private boolean isToReview(JsonValue v) {
        final JsonArray items = v.asObject().get("items").asArray();
        for (final JsonValue item : items) {
            final JsonObject io = item.asObject();
            if (io.get("field").asString().equals("status")
                    && io.get("to").asString().equals(this.sids().reviewStateId)) {
                return true;
            }
        }
        return false;
    }

    private String getToUser(JsonValue v) {
        final JsonValue author = v.asObject().get("author");
        final JsonValue authorName = author == null ? null : author.asObject().get("name");
        return authorName == null ? "" : authorName.toString();
    }

    private JsonArray getHistories(JsonObject ticket) {
        return ticket.get("changelog").asObject().get("histories").asArray();
    }

    @Override
    public ITicketData loadTicket(String ticketKey) {
        final JsonObject object = (JsonObject)
                this.performGet(this.url + "/rest/api/latest/issue/" + ticketKey
                        + "?fields=" + this.getReviewFieldId() + "&expand=changelog"
                        + this.getAuthParams());
        if (object.get("key") == null) {
            return null;
        }
        return new JiraTicket(object);
    }

    @Override
    public Set<String> getFilterNamesForReview() {
        return this.filtersForReview.keySet();
    }

    @Override
    public Set<String> getFilterNamesForFixing() {
        return this.filtersForFixing.keySet();
    }

    @Override
    public List<TicketInfo> getTicketsForFilter(String filterName) {
        if (this.filtersForReview.containsKey(filterName)) {
            return this.queryTickets(this.filtersForReview.get(filterName));
        } else {
            return this.queryTickets(this.filtersForFixing.get(filterName));
        }
    }

    /**
     * Adds a filter to the set of known filters.
     * @param name The filters name.
     * @param jql The JQL select for the filter.
     * @param forReview true iff it is a filter for tickets to review, false iff it is for fixing.
     */
    public void addFilter(String name, String jql, boolean forReview) {
        if (forReview) {
            this.filtersForReview.put(name, jql);
        } else {
            this.filtersForFixing.put(name, jql);
        }
    }

    private List<TicketInfo> queryTickets(final String jql) {
        try {
            final String searchUrl = String.format(
                    "%s/rest/api/latest/search"
                            + "?maxResults=200"
                            + "&fields=summary,components,status,parent"
                            + "&expand=changelog"
                            + "&jql=%s"
                            + "%s",
                            this.url,
                            URLEncoder.encode(jql, "UTF-8"),
                            this.getAuthParams());

            final JsonObject result = this.performGet(searchUrl).asObject();
            final List<TicketInfo> ret = new ArrayList<>();
            for (final JsonValue issue : result.get("issues").asArray()) {
                ret.add(this.mapTicket(issue.asObject()));
            }
            return ret;
        } catch (final UnsupportedEncodingException e) {
            throw new ReviewtoolException(e);
        }
    }

    private TicketInfo mapTicket(JsonObject ticket) {
        final JsonValue parent = ticket.get("fields").asObject().get("parent");
        return new TicketInfo(
                ticket.get("key").asString(),
                ticket.get("fields").asObject().get("summary").asString(),
                ticket.get("fields").asObject().get("status").asObject().get("name").asString(),
                this.getPreviousStatus(ticket),
                this.formatComponents(ticket.get("fields").asObject().get("components").asArray()),
                parent == null ? null : parent.asObject().get("fields").asObject().get("summary").asString(),
                this.getReviewers(ticket),
                this.getTimeOfTransferToCurrentStatus(ticket));
    }

    private Set<String> getReviewers(JsonObject ticket) {
        final LinkedHashSet<String> reviewers = new LinkedHashSet<>();
        final JsonArray histories = JiraPersistence.this.getHistories(ticket);
        for (final JsonValue v : histories) {
            if (this.isToReview(v)) {
                final String reviewer = this.getToUser(v);
                if (!reviewer.isEmpty()) {
                    reviewers.add(reviewer.toUpperCase());
                }
            }
        }
        return reviewers;
    }

    private String getPreviousStatus(JsonObject ticket) {
        String prevStatus = "";
        final JsonArray histories = JiraPersistence.this.getHistories(ticket);
        for (final JsonValue v : histories) {
            final String fromStatus = this.getFromStatus(v);
            if (fromStatus != null) {
                prevStatus = fromStatus;
            }
        }
        return prevStatus;
    }

    private Date getTimeOfTransferToCurrentStatus(JsonObject ticket) {
        Date ret = new Date(0);
        final JsonArray histories = JiraPersistence.this.getHistories(ticket);
        for (final JsonValue v : histories) {
            final String fromStatus = this.getFromStatus(v);
            if (fromStatus != null) {
                ret = Util.max(ret, this.getTimeOfHistoryItem(v));
            }
        }
        return ret;
    }

    private Date getTimeOfHistoryItem(final JsonValue v) {
        final String dateString = v.asObject().get("created").asString();
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(dateString);
        } catch (final java.text.ParseException e) {
            throw new ReviewtoolException(e);
        }
    }

    private String getFromStatus(JsonValue v) {
        final JsonArray items = v.asObject().get("items").asArray();
        for (final JsonValue item : items) {
            final JsonObject io = item.asObject();
            if (io.get("field").asString().equals("status")) {
                return io.get("fromString").asString();
            }
        }
        return null;
    }

    private String formatComponents(JsonArray components) {
        final StringBuilder b = new StringBuilder();
        for (final JsonValue v : components) {
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append(v.asObject().get("name"));
        }
        return b.toString();
    }

    private String getReviewFieldId() {
        if (this.reviewFieldId != null) {
            return this.reviewFieldId;
        }
        this.reviewFieldId = this.getIdForName(this.reviewFieldName);
        return this.reviewFieldId;
    }

    private String getIdForName(final String name) {
        final JsonArray fields = this.performGet(this.url + "/rest/api/latest/field/").asArray();
        for (final JsonValue o : fields) {
            if (o.asObject().get("name").asString().equals(name)) {
                return o.asObject().get("id").asString();
            }
        }
        throw new ReviewtoolException("found no id for name " + name);
    }

    /**
     * Performs an HTTP GET request and returns the resulting JSON data.
     */
    public JsonValue performGet(final String searchUrl) {
        final StringBuilder b = new StringBuilder();
        try {
            this.communicate(searchUrl, "GET", null, (InputStream input) -> {
                try {
                    final InputStreamReader reader = new InputStreamReader(input, "UTF-8"); //$NON-NLS-1$
                    int ch;
                    while ((ch = reader.read()) >= 0) {
                        b.append((char) ch);
                    }
                } catch (final IOException e) {
                    throw new ReviewtoolException(e);
                }
            });
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
        final String data = b.toString();
        try {
            return Json.parse(data);
        } catch (final ParseException e) {
            throw new ReviewtoolException("exception parsing: " + data, e);
        }
    }

    private String getAuthParams() {
        try {
            return String.format("&os_username=%s&os_password=%s",
                    URLEncoder.encode(this.user, "UTF-8"),
                    URLEncoder.encode(this.password, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            throw new ReviewtoolException(e);
        }
    }

    private void performPut(final String putUrl, final JsonObject json) {
        try {
            this.communicate(putUrl, "PUT", json.toString(), (InputStream s) -> { });
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    private void performPost(final String postUrl, final JsonObject json) {
        try {
            this.communicate(postUrl, "POST", json.toString(), (InputStream s) -> { });
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Sends and receives data.
     */
    private void communicate(final String url, final String method, final String data,
            Consumer<InputStream> resultConsumer) throws IOException {
        Logger.debug("communicate to JIRA: " + this.trimArgs(url));
        final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod(method);
        c.addRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
        final String cookies = this.loadCookies();
        if (!cookies.isEmpty()) {
            c.setRequestProperty("Cookie", cookies);
        }
        c.setDoOutput(data != null);
        c.connect();
        if (data != null) {
            final OutputStream outputStream = c.getOutputStream();
            try {
                outputStream.write(data.getBytes("UTF-8")); //$NON-NLS-1$
            } finally {
                outputStream.close();
            }
        }
        for (final Entry<String, List<String>> header : c.getHeaderFields().entrySet()) {
            if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                this.setCookies(header.getValue());
            }
        }
        try {
            final InputStream s = c.getInputStream();
            resultConsumer.accept(s);
            s.close();
        } catch (final IOException e) {
            this.flushErrorStream(c);
            throw e;
        } finally {
            c.disconnect();
        }
    }

    private String trimArgs(String url2) {
        final int end = url2.indexOf('?');
        return end < 0 ? url2 : url2.substring(0, end);
    }

    private void setCookies(List<String> cookies) throws IOException {
        if (this.cookiesFile == null) {
            return;
        }
        final Map<String, String> map = new LinkedHashMap<>();
        if (this.cookiesFile.exists()) {
            Files.lines(this.cookiesFile.toPath(), Charset.forName("UTF-8")).forEach((String line) -> {
                this.putSplitAtEqualsSign(map, line);
            });
        }
        for (final String cookie : cookies) {
            final int semiIndex = cookie.indexOf(';');
            final String keyAndValue;
            if (semiIndex >= 0) {
                keyAndValue = cookie.substring(0, semiIndex);
            } else {
                keyAndValue = cookie;
            }
            this.putSplitAtEqualsSign(map, keyAndValue);
        }
        final StringBuilder content = new StringBuilder();
        for (final Entry<String, String> e : map.entrySet()) {
            content.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        Files.write(this.cookiesFile.toPath(), content.toString().getBytes("UTF-8"));
    }

    private void putSplitAtEqualsSign(final Map<String, String> map, final String keyAndValue) {
        if (keyAndValue.isEmpty()) {
            return;
        }
        final int equalsIndex = keyAndValue.indexOf('=');
        map.put(keyAndValue.substring(0, equalsIndex), keyAndValue.substring(equalsIndex + 1));
    }

    private String loadCookies() throws IOException {
        if (this.cookiesFile == null || !this.cookiesFile.exists()) {
            Logger.debug("using no cookies " + this.cookiesFile);
            return this.createBaererTokenCookie();
        }

        final Stream<String> cookies = Stream.concat(
                Files.lines(this.cookiesFile.toPath(), Charset.forName("UTF-8")),
                Stream.of(this.createBaererTokenCookie()));

        return cookies.collect(Collectors.joining("; "));
    }

    private String createBaererTokenCookie() throws IOException {
        if (this.oauthIssuer == null) {
            Logger.debug("using no bearer token");
            return "";
        }

        Logger.debug("using bearer token from issuer " + this.oauthIssuer);
        final String bearerToken = BearerTokenProvider.createBearerToken(
                this.oauthIssuer,
                this.oauthAudience,
                this.oauthClientID,
                this.oauthClientSecret);

        return "bearertoken=" + bearerToken;
    }

    private void flushErrorStream(final HttpURLConnection c) throws IOException {
        try (final InputStream s = c.getErrorStream()) {
            int r;
            while ((r = s.read()) >= 0) {
                System.err.write(r);
            }
        }
    }

    @Override
    public void startReviewing(String ticketKey) {
        this.performTransitionIfPossible(ticketKey, this.sids().reviewStateId);
    }

    @Override
    public void startFixing(String ticketKey) {
        this.performTransitionIfPossible(ticketKey, this.sids().implementationStateId);
    }

    @Override
    public void changeStateToReadyForReview(String ticketKey) {
        this.performTransitionIfPossible(ticketKey, this.sids().readyForReviewStateId);
    }

    @Override
    public List<EndTransition> getPossibleTransitionsForReviewEnd(String ticket) {
        final String getUrl = String.format(
                "%s/rest/api/latest/issue/%s/transitions?%s",
                this.url,
                ticket,
                this.getAuthParams());

        final JsonObject result = this.performGet(getUrl).asObject();
        final List<EndTransition> ret = new ArrayList<>();
        for (final JsonValue curValue : result.get("transitions").asArray()) {
            final JsonObject transition = curValue.asObject();
            final String transitionTarget = transition.get("to").asObject().get("id").asString();
            ret.add(new EndTransition(
                    transition.get("name").asString(),
                    transition.get("id").asString(),
                    this.determineTransitionType(transitionTarget)));
        }
        return ret;
    }

    private Type determineTransitionType(String transitionTargetId) {
        if (this.sids().doneStateId.equals(transitionTargetId)) {
            return Type.OK;
        } else if (this.sids().rejectedStateId.equals(transitionTargetId)) {
            return Type.REJECTION;
        } else {
            return Type.UNKNOWN;
        }
    }

    @Override
    public void changeStateAtReviewEnd(String ticketKey, EndTransition transition) {
        this.performTransition(ticketKey, transition.getInternalName());
    }

    private void performTransitionIfPossible(String ticketKey, String targetStateId) {
        final TreeSet<String> possibleTransitions = new TreeSet<>();
        final String transition = this.getTransitionId(ticketKey, targetStateId, possibleTransitions);
        if (transition == null) {
            Logger.info("Could not transition " + ticketKey + " to " + targetStateId
                    + ". Possible transitions: " + possibleTransitions);
        } else if (targetStateId.equals(this.getCurrentStatus(ticketKey))) {
            Logger.debug("Did not transition, already in state " + targetStateId);
        } else {
            this.performTransition(ticketKey, transition);
        }
    }

    private String getCurrentStatus(String ticketKey) {
        final String getUrl = String.format(
                "%s/rest/api/latest/issue/%s?fields=status&%s",
                this.url,
                ticketKey,
                this.getAuthParams());

        final JsonObject result = this.performGet(getUrl).asObject();
        return result.get("fields").asObject().get("status").asObject().get("id").asString();
    }

    private void performTransition(final String ticket, final String transitionId) {
        final String postUrl = String.format(
                "%s/rest/api/latest/issue/%s/transitions?%s",
                this.url,
                ticket,
                this.getAuthParams());

        final JsonObject to = new JsonObject();
        to.add("id", transitionId);

        final JsonObject command = new JsonObject();
        command.add("transition", to);

        this.performPost(postUrl, command);
    }

    /**
     * Determines a transition that results in the given state.
     */
    private String getTransitionId(
            final String ticket, String targetStateName, Set<String> possibleTransitions) {
        final String getUrl = String.format(
                "%s/rest/api/latest/issue/%s/transitions?%s",
                this.url,
                ticket,
                this.getAuthParams());

        final JsonObject result = this.performGet(getUrl).asObject();
        for (final JsonValue curValue : result.get("transitions").asArray()) {
            final JsonObject transition = curValue.asObject();
            final String transitionTarget = transition.get("to").asObject().get("id").asString();
            possibleTransitions.add(transitionTarget);
            if (targetStateName.equals(transitionTarget)) {
                return transition.get("id").asString();
            }
        }
        return null;
    }

    @Override
    public TicketLinkSettings getLinkSettings() {
        return this.linkSettings;
    }

}
