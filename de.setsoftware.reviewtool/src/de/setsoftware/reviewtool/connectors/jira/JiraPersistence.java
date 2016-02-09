package de.setsoftware.reviewtool.connectors.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.plugin.Logger;

public class JiraPersistence implements IReviewPersistence {

	private final class JiraTicket implements ITicketData {

		private final JsonObject ticket;

		public JiraTicket(JsonObject object) {
			this.ticket = object;
		}

		@Override
		public String getReviewData() {
			final JsonValue reviewData = this.ticket.get("fields").asObject().get(JiraPersistence.this.getReviewFieldId());
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
		public int getCurrentRound() {
			final JsonArray histories = JiraPersistence.this.getHistories(this.ticket);
			int count = 0;
			for (final JsonValue v : histories) {
				if (JiraPersistence.this.isToReview(v)) {
					count++;
				}
			}
			return count > 0 ? count - 1 : 0;
		}

	}

	private final String url;
	private final String reviewFieldName;
	private final String reviewState;
	private final String implementationState;
	private final String user;
	private final String password;

	private String reviewFieldId;

	public JiraPersistence(
			String url, String reviewFieldName, String reviewState, String implementationState,
			String user, String password) {
		this.url = url;
		this.reviewFieldName = reviewFieldName;
		this.reviewState = reviewState;
		this.implementationState = implementationState;
		this.user = user;
		this.password = password;
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
					&& io.get("toString").equals(this.reviewState)) {
				return true;
			}
		}
		return false;
	}

	private String getToUser(JsonValue v) {
		return v.asObject().get("author").asObject().get("name").asString();
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
	public List<TicketInfo> getReviewableTickets() {
		//TODO variable Anteile auslagern
		return this.queryTickets("(status = \"Ready for Review\" and assignee != currentUser()) or (status = \"In Review\" and assignee = currentUser())");
	}

	@Override
	public List<TicketInfo> getFixableTickets() {
		//TODO variable Anteile auslagern
		return this.queryTickets("assignee = currentUser() and (status = Rejected or (status = \"In Progress\" and Reviewanmerkungen is not EMPTY))");
	}

	private List<TicketInfo> queryTickets(final String jql) {
		try {
			final String searchUrl = String.format(
					"%s/rest/api/latest/search" +
							"?maxResults=200" +
							"&fields=summary,components,status" +
							"&jql=%s" +
							"%s",
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
			throw new RuntimeException(e);
		}
	}

	private TicketInfo mapTicket(JsonObject ticket) {
		return new TicketInfo(
				ticket.get("key").asString(),
				ticket.get("fields").asObject().get("summary").asString(),
				ticket.get("fields").asObject().get("status").asObject().get("name").asString(),
				this.formatComponents(ticket.get("fields").asObject().get("components").asArray()));
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
		throw new RuntimeException("found no id for name " + name);
	}

	public JsonValue performGet(final String searchUrl) {
		try (InputStream input = new URL(searchUrl).openStream()) {
			final InputStreamReader reader = new InputStreamReader(input, "UTF-8"); //$NON-NLS-1$
			final StringBuilder b = new StringBuilder();
			int ch;
			while ((ch = reader.read()) >= 0) {
				b.append((char) ch);
			}
			final String data = b.toString();
			try {
				return JsonValue.readFrom(data);
			} catch (final ParseException e) {
				throw new RuntimeException("exception parsing: " + data, e);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getAuthParams() {
		try {
			return String.format("&os_username=%s&os_password=%s",
					URLEncoder.encode(this.user, "UTF-8"),
					URLEncoder.encode(this.password, "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void performPut(final String putUrl, final JsonObject json) {
		try {
			this.communicate(putUrl, "PUT", json.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void performPost(final String postUrl, final JsonObject json) {
		try {
			this.communicate(postUrl, "POST", json.toString());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sendet und empfängt Daten.
	 */
	private void communicate(final String url, final String method, final String data) throws IOException {
		final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
		c.setRequestMethod(method);
		c.addRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
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
		try {
			c.getInputStream().close();
		} catch (final IOException e) {
			this.flushErrorStream(c);
			throw e;
		} finally {
			c.disconnect();
		}
	}

	private void flushErrorStream(final HttpURLConnection c) throws IOException {
		final InputStream s = c.getErrorStream();
		int r;
		while ((r = s.read()) >= 0) {
			System.err.write(r);
		}
	}

	@Override
	public void startReviewing(String ticketKey) {
		this.performTransitionIfPossible(ticketKey, this.reviewState);
	}

	@Override
	public void startFixing(String ticketKey) {
		this.performTransitionIfPossible(ticketKey, this.implementationState);
	}

	private void performTransitionIfPossible(String ticketKey, String reviewState2) {
		final TreeSet<String> possibleTransitions = new TreeSet<>();
		final String transition = this.getTransitionId(ticketKey, this.reviewState, possibleTransitions);
		if (transition == null) {
			Logger.info("Could not transition " + ticketKey + " to " + this.reviewState
					+ ". Possible transitions: " + possibleTransitions);
		} else {
			this.performTransition(ticketKey, transition);
		}
	}

	private void performTransition(final String ticket, final String transition) {
		final String postUrl = String.format(
				"%s/rest/api/latest/issue/%s/transitions?%s",
				this.url,
				ticket,
				this.getAuthParams());

		final JsonObject to = new JsonObject();
		to.add("id", transition);

		final JsonObject command = new JsonObject();
		command.add("transition", to);

		this.performPost(postUrl, command);
	}

	private String getTransitionId(final String ticket, String targetStateName, Set<String> possibleTransitions) {
		final String getUrl = String.format(
				"%s/rest/api/latest/issue/%s/transitions?%s",
				this.url,
				ticket,
				this.getAuthParams());

		final JsonObject result = this.performGet(getUrl).asObject();
		for (final JsonValue curValue : result.get("transitions").asArray()) {
			final JsonObject transition = curValue.asObject();
			final String transitionTarget = transition.get("to").asObject().get("name").asString();
			possibleTransitions.add(transitionTarget);
			if (targetStateName.equals(transitionTarget)) {
				return transition.get("id").asString();
			}
		}
		return null;
	}
}
