package de.setsoftware.reviewtool.connectors.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.TicketInfo;

public class FilePersistence implements IReviewPersistence {

	private final class TicketDir implements ITicketData {

		private final File ticketDir;

		public TicketDir(File file) {
			this.ticketDir = file;
		}

		@Override
		public String getReviewData() {
			final Path reviewDataFile = this.ticketDir.toPath().resolve(REVIEW_DATA_TXT);
			if (!reviewDataFile.toFile().exists()) {
				return "";
			}
			try {
				return new String(Files.readAllBytes(reviewDataFile), "UTF-8");
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String getReviewerForRound(int number) {
			final List<String> lines = this.readReviewHistory();
			if (number > lines.size()) {
				return null;
			} else {
				return lines.get(number - 1);
			}
		}

		@Override
		public int getCurrentRound() {
			return this.readReviewHistory().size();
		}

		private List<String> readReviewHistory() {
			final Path historyFile = this.ticketDir.toPath().resolve(REVIEW_HISTORY_TXT);
			if (!historyFile.toFile().exists()) {
				return Collections.emptyList();
			}
			try {
				return Files.readAllLines(historyFile, Charset.forName("UTF-8"));
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private static final String REVIEW_DATA_TXT = "reviewData.txt";
	private static final String REVIEW_HISTORY_TXT = "reviewHistory.txt";
	private static final String STATE_PREFIX = "state.";
	private final File rootDir;

	public FilePersistence(File rootDir) {
		this.rootDir = rootDir;
	}

	@Override
	public List<TicketInfo> getReviewableTickets() {
		return this.getTicketsWithState("readyForReview", "inReview");
	}

	@Override
	public List<TicketInfo> getFixableTickets() {
		return this.getTicketsWithState("rejected", "inImplementation");
	}

	private List<TicketInfo> getTicketsWithState(String... states) {
		this.checkRoot();
		final List<TicketInfo> ret = new ArrayList<>();
		for (final File child : this.rootDir.listFiles()) {
			if (!this.isTicketDir(child)) {
				continue;
			}
			if (this.hasAnyOfStates(child, states)) {
				ret.add(this.createTicketInfo(child));
			}
		}
		return ret;
	}

	private boolean hasAnyOfStates(File child, String[] states) {
		for (final String state : states) {
			if (this.hasState(child, state)) {
				return true;
			}
		}
		return false;
	}

	private boolean isTicketDir(File child) {
		return child.isDirectory() && !child.getName().startsWith(".");
	}

	private TicketInfo createTicketInfo(File child) {
		final Properties ticketProperties = new Properties();
		try (FileInputStream stream = new FileInputStream(new File(child, "ticket.properties"))) {
			ticketProperties.load(stream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return new TicketInfo(
				child.getName(),
				ticketProperties.getProperty("description", ""),
				this.getState(child),
				ticketProperties.getProperty("component", ""));
	}

	private boolean hasState(File child, String state) {
		return new File(child, STATE_PREFIX + state).exists();
	}

	private String getState(File child) {
		for (final String filename : child.list()) {
			if (filename.startsWith(STATE_PREFIX)) {
				return filename.substring(STATE_PREFIX.length());
			}
		}
		return "unknown";
	}

	@Override
	public void saveReviewData(String ticketKey, String newData) {
		try {
			Files.write(this.rootDir.toPath().resolve(ticketKey).resolve(REVIEW_DATA_TXT),
					newData.getBytes("UTF-8"));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ITicketData loadTicket(String ticketKey) {
		return new TicketDir(new File(this.rootDir, ticketKey));
	}

	private void checkRoot() {
		if (!this.rootDir.exists()) {
			throw new RuntimeException("Das Verzeichnis " + this.rootDir + " existiert nicht.");
		}
		if (!this.rootDir.isDirectory()) {
			throw new RuntimeException(this.rootDir + " ist kein Verzeichnis.");
		}
	}

}
