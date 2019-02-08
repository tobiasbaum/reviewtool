package de.setsoftware.reviewtool.ticketconnectors.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.model.TicketLinkSettings;

/**
 * A simple review persistence layer based on a shared directory.
 */
public class FilePersistence implements IReviewPersistence {

    private static final String REVIEW_DATA_TXT = "reviewData.txt";
    private static final String REVIEW_HISTORY_TXT = "reviewHistory.txt";
    private static final String STATE_PREFIX = "state.";
    private static final String IN_IMPLEMENTATION = "inImplementation";
    private static final String REJECTED = "rejected";
    private static final String IN_REVIEW = "inReview";
    private static final String READY_FOR_REVIEW = "readyForReview";
    private static final String DONE = "done";

    /**
     * The directory containing the data for a ticket.
     */
    private final class TicketDir implements ITicketData {

        private final File ticketDir;

        public TicketDir(File file) {
            this.ticketDir = file;
            if (!this.ticketDir.exists()) {
                try {
                    this.createDummyDir();
                } catch (final IOException e) {
                    throw new ReviewtoolException(e);
                }
            }
        }

        private void createDummyDir() throws IOException {
            this.ticketDir.mkdir();
            Files.write(this.ticketDir.toPath().resolve("ticket.properties"),
                    ("description=Testticket " + (int) (Math.random() * 1000) + "\ncomponent=Main").getBytes());
            Files.createFile(this.ticketDir.toPath().resolve("state.readyForReview"));
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
                throw new ReviewtoolException(e);
            }
        }

        @Override
        public String getReviewerForRound(int number) {
            final List<String> lines = this.readReviewHistory();
            if (number > lines.size() || number <= 0) {
                return FilePersistence.this.defaultReviewer;
            } else {
                return lines.get(number - 1);
            }
        }

        @Override
        public Date getEndTimeForRound(int number) {
            //not implemented
            return new Date();
        }

        @Override
        public int getCurrentRound() {
            return this.readReviewHistory().size();
        }

        private List<String> readReviewHistory() {
            final Path historyFile = this.getHistoryFile();
            if (!historyFile.toFile().exists()) {
                return Collections.emptyList();
            }
            try {
                return Files.readAllLines(historyFile, Charset.forName("UTF-8"));
            } catch (final IOException e) {
                throw new ReviewtoolException(e);
            }
        }

        private Path getHistoryFile() {
            return this.ticketDir.toPath().resolve(REVIEW_HISTORY_TXT);
        }

        @Override
        public TicketInfo getTicketInfo() {
            return FilePersistence.this.createTicketInfo(this.ticketDir);
        }

        @Override
        public String getId() {
            return this.ticketDir.getName();
        }

    }

    private final File rootDir;
    private final String defaultReviewer;

    public FilePersistence(File rootDir, String defaultReviewer) {
        this.rootDir = rootDir;
        this.defaultReviewer = defaultReviewer;
    }

    @Override
    public Set<String> getFilterNamesForReview() {
        return Collections.singleton("Reviewable");
    }

    @Override
    public Set<String> getFilterNamesForFixing() {
        return Collections.singleton("Fixable");
    }

    @Override
    public List<TicketInfo> getTicketsForFilter(String filterName) {
        if (filterName.equals("Reviewable")) {
            return this.getTicketsWithState(READY_FOR_REVIEW, IN_REVIEW);
        } else if (filterName.equals("Fixable")) {
            return this.getTicketsWithState(REJECTED, IN_IMPLEMENTATION);
        }
        return Collections.emptyList();
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
            throw new ReviewtoolException(e);
        }
        return new TicketInfo(
                child.getName(),
                ticketProperties.getProperty("description", ""),
                this.getState(child),
                "?",
                ticketProperties.getProperty("component", ""),
                ticketProperties.getProperty("parentSummary"),
                new LinkedHashSet<>(new TicketDir(child).readReviewHistory()),
                this.determineWaitingSince(child));
    }

    private Date determineWaitingSince(File child) {
        final File stateFile = this.getStateFile(child);
        return stateFile == null ? new Date() : new Date(stateFile.lastModified());
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

    private File getStateFile(File child) {
        for (final File file : child.listFiles()) {
            if (file.getName().startsWith(STATE_PREFIX)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public void saveReviewData(String ticketKey, String newData) {
        try {
            Files.write(this.rootDir.toPath().resolve(ticketKey).resolve(REVIEW_DATA_TXT),
                    newData.getBytes("UTF-8"));
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public TicketDir loadTicket(String ticketKey) {
        return new TicketDir(this.getTicketDir(ticketKey));
    }

    private File getTicketDir(String ticketKey) {
        return new File(this.rootDir, ticketKey);
    }

    private void checkRoot() {
        if (!this.rootDir.exists()) {
            throw new ReviewtoolException("Das Verzeichnis " + this.rootDir + " existiert nicht.");
        }
        if (!this.rootDir.isDirectory()) {
            throw new ReviewtoolException(this.rootDir + " ist kein Verzeichnis.");
        }
    }

    @Override
    public void startReviewing(String ticketKey) {
        final boolean changeSuccess = this.changeState(ticketKey, READY_FOR_REVIEW, IN_REVIEW);
        if (changeSuccess) {
            this.addUserToReviewHistory(ticketKey, this.defaultReviewer);
        }
    }

    private void addUserToReviewHistory(String ticketKey, String user) {
        final TicketDir ticketDir = this.loadTicket(ticketKey);
        final List<String> lines = new ArrayList<>(ticketDir.readReviewHistory());
        lines.add(user);
        try {
            Files.write(ticketDir.getHistoryFile(), lines, Charset.forName("UTF-8"));
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public void startFixing(String ticketKey) {
        this.changeState(ticketKey, REJECTED, IN_IMPLEMENTATION);
    }

    private boolean changeState(String ticketKey, String from, String to) {
        final File ticketDir = this.getTicketDir(ticketKey);
        return new File(ticketDir, STATE_PREFIX + from).renameTo(
                new File(ticketDir, STATE_PREFIX + to));
    }

    @Override
    public void changeStateToReadyForReview(String ticketKey) {
        this.changeState(ticketKey, IN_REVIEW, READY_FOR_REVIEW);
        this.changeState(ticketKey, IN_IMPLEMENTATION, READY_FOR_REVIEW);
    }

    @Override
    public List<EndTransition> getPossibleTransitionsForReviewEnd(String ticketKey) {
        return Arrays.asList(
                new EndTransition("OK", DONE, EndTransition.Type.OK),
                new EndTransition("OK, aber Zweitreview nötig", READY_FOR_REVIEW, EndTransition.Type.OK),
                new EndTransition("Rückläufer", REJECTED, EndTransition.Type.REJECTION));
    }

    @Override
    public void changeStateAtReviewEnd(String ticketKey, EndTransition transition) {
        this.changeState(ticketKey, this.getState(this.getTicketDir(ticketKey)), transition.getInternalName());
    }

    @Override
    public TicketLinkSettings getLinkSettings() {
        return new TicketLinkSettings(
                "file:///" + this.rootDir.toString().replace('\\', '/') + "/%s",
                "Open ticket dir");
    }

}
