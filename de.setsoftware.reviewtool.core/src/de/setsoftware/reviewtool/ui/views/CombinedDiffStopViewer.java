package de.setsoftware.reviewtool.ui.views;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileDiff;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Hunk;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;

/**
 * Displays all differences of a {@link Stop} combined in a single window.
 */
public class CombinedDiffStopViewer extends AbstractStopViewer {

    @Override
    public void createStopView(final ViewPart view, final Composite scrollContent, final Stop stop) {
        final ToursInReview tours = ViewDataSource.get().getToursInReview();
        if (tours == null) {
            return;
        }

        final Map<FileInRevision, FileInRevision> changes = stop.getHistory();
        final List<FileInRevision> sortedRevs = FileInRevision.sortByRevision(changes.keySet());
        final FileInRevision firstRevision = sortedRevs.get(0);
        final FileInRevision lastRevision = changes.get(sortedRevs.get(sortedRevs.size() - 1));

        final FileHistoryNode node = tours.getFileHistoryNode(lastRevision);
        if (node != null) {
            if (stop.isBinaryChange()) {
                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        new ArrayList<Fragment>(), new ArrayList<Fragment>(),
                        new ArrayList<Position>(), new ArrayList<Position>());
            } else {
                final FileHistoryNode ancestor = tours.getFileHistoryNode(firstRevision);
                final FileDiff diff = node.buildHistory(ancestor);

                final List<Fragment> origins = new ArrayList<>();
                for (final FileInRevision file : changes.keySet()) {
                    for (final Hunk hunk : stop.getContentFor(file)) {
                        origins.addAll(hunk.getTarget().getOrigins());
                    }
                }
                final List<Hunk> relevantHunks = diff.getHunksWithTargetChangesInOneOf(origins);

                final LineSequence oldContents;
                final LineSequence newContents;
                try {
                    oldContents = fileToLineSequence(firstRevision);
                    newContents = fileToLineSequence(lastRevision);
                } catch (final Exception e) {
                    throw new ReviewtoolException(e);
                }

                final List<Position> oldPositions = new ArrayList<>();
                final List<Position> newPositions = new ArrayList<>();

                for (final Hunk hunk : relevantHunks) {
                    final Fragment sourceFragment = hunk.getSource();
                    final Fragment targetFragment = hunk.getTarget();

                    final int oldStartOffset =
                            oldContents.getStartPositionOfLine(sourceFragment.getFrom().getLine() - 1)
                                    + (sourceFragment.getFrom().getColumn() - 1);
                    final int oldEndOffset =
                            oldContents.getStartPositionOfLine(sourceFragment.getTo().getLine() - 1)
                                    + (sourceFragment.getTo().getColumn() - 1);
                    final int newStartOffset =
                            newContents.getStartPositionOfLine(targetFragment.getFrom().getLine() - 1)
                                    + (targetFragment.getFrom().getColumn() - 1);
                    final int newEndOffset =
                            newContents.getStartPositionOfLine(targetFragment.getTo().getLine() - 1)
                                    + (targetFragment.getTo().getColumn() - 1);

                    oldPositions.add(new Position(oldStartOffset, oldEndOffset - oldStartOffset));
                    newPositions.add(new Position(newStartOffset, newEndOffset - newStartOffset));
                }

                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        Arrays.asList(createFragmentForWholeFile(firstRevision, oldContents)),
                        Arrays.asList(createFragmentForWholeFile(lastRevision, newContents)),
                        oldPositions,
                        newPositions);
            }
        }
    }

    private Fragment createFragmentForWholeFile(final FileInRevision revision, final LineSequence contents) {
        final int numLines = contents.getNumberOfLines();
        final Fragment fragment = ChangestructureFactory.createFragment(revision,
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(numLines + 1, 1));
        return fragment;
    }

    private static LineSequence fileToLineSequence(final FileInRevision file) throws Exception {
        final byte[] data = file.getContents();

        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data));
            return new LineSequence(data, "UTF-8");
        } catch (final CharacterCodingException e) {
            return new LineSequence(data, "ISO-8859-1");
        }
    }
}
