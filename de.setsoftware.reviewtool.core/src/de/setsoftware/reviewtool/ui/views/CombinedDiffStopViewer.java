package de.setsoftware.reviewtool.ui.views;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
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

        final Map<IRevisionedFile, IRevisionedFile> changes = stop.getHistory();
        final List<? extends IRevisionedFile> sortedRevs = FileInRevision.sortByRevision(changes.keySet());
        final IRevisionedFile firstRevision = sortedRevs.get(0);
        final IRevisionedFile lastRevision = changes.get(sortedRevs.get(sortedRevs.size() - 1));

        final IFileHistoryNode node = tours.getFileHistoryNode(lastRevision);
        if (node != null) {
            if (stop.isBinaryChange()) {
                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        new ArrayList<IFragment>(), new ArrayList<IFragment>(),
                        new ArrayList<Position>(), new ArrayList<Position>());
            } else {
                final IFileHistoryNode ancestor = tours.getFileHistoryNode(firstRevision);
                final Set<? extends IFileDiff> diffs = node.buildHistories(ancestor);
                // TODO: we currently cowardly refuse to display any history beyond the first merge parent
                // TODO: it happened to me once that "diffs" was empty here; fix here or somewhere else?
                final IFileDiff diff = diffs.iterator().next();

                final List<IFragment> origins = new ArrayList<>();
                for (final IRevisionedFile file : changes.keySet()) {
                    for (final IHunk hunk : stop.getContentFor(file)) {
                        origins.addAll(hunk.getTarget().getOrigins());
                    }
                }
                final List<? extends IHunk> relevantHunks = diff.getHunksWithTargetChangesInOneOf(origins);

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

                for (final IHunk hunk : relevantHunks) {
                    final IFragment sourceFragment = hunk.getSource();
                    final IFragment targetFragment = hunk.getTarget();

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
                        Arrays.asList(this.createFragmentForWholeFile(firstRevision, oldContents)),
                        Arrays.asList(this.createFragmentForWholeFile(lastRevision, newContents)),
                        oldPositions,
                        newPositions);
            }
        }
    }

    private IFragment createFragmentForWholeFile(final IRevisionedFile revision, final LineSequence contents) {
        final int numLines = contents.getNumberOfLines();
        final IFragment fragment = ChangestructureFactory.createFragment(revision,
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(numLines + 1, 1));
        return fragment;
    }

    private static LineSequence fileToLineSequence(final IRevisionedFile file) throws Exception {
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
