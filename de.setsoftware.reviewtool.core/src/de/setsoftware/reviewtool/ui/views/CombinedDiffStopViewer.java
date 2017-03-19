package de.setsoftware.reviewtool.ui.views;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.LineSequence;
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
        final FileHistoryNode node = tours.getFileHistoryNode(stop.getLastFileRevision());
        if (node != null) {
            final List<FileInRevision> history = stop.getHistory();
            final FileInRevision firstRevision = history.get(0);
            final FileInRevision lastRevision = history.get(history.size() - 1);

            final List<Fragment> fragmentsFirst = stop.getContentFor(firstRevision);
            final List<Fragment> fragmentsLast = stop.getContentFor(lastRevision);
            if (fragmentsFirst == null || fragmentsLast == null) { // binary change
                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision, fragmentsFirst, fragmentsLast,
                        null, null);
            } else { // textual change
                final FileHistoryNode ancestor = tours.getFileHistoryNode(firstRevision);
                final FileDiff diff = node.buildHistory(ancestor);
                final List<Hunk> hunks = diff.getHunksForTargets(fragmentsLast);

                final Fragment firstSourceFragment = Hunk.getSources(hunks).getFragments().get(0);
                final Fragment firstTargetFragment = Hunk.getTargets(hunks).getFragments().get(0);

                final LineSequence oldContents = fileToLineSequence(firstRevision);
                final LineSequence newContents = fileToLineSequence(lastRevision);

                final int oldStartOffset =
                        oldContents.getStartPositionOfLine(firstSourceFragment.getFrom().getLine() - 1);
                final int oldEndOffset =
                        oldContents.getStartPositionOfLine(firstSourceFragment.getTo().getLine() - 1);
                final int newStartOffset =
                        newContents.getStartPositionOfLine(firstTargetFragment.getFrom().getLine() - 1);
                final int newEndOffset =
                        newContents.getStartPositionOfLine(firstTargetFragment.getTo().getLine() - 1);

                final int oldNumLines = oldContents.getNumberOfLines();
                final Fragment oldFragment = ChangestructureFactory.createFragment(firstRevision,
                        ChangestructureFactory.createPositionInText(1, 1, 0),
                        ChangestructureFactory.createPositionInText(oldNumLines + 1, 0,
                                oldContents.getStartPositionOfLine(oldNumLines)),
                        oldContents.getLinesConcatenated(0, oldNumLines));

                final int newNumLines = newContents.getNumberOfLines();
                final Fragment newFragment = ChangestructureFactory.createFragment(lastRevision,
                        ChangestructureFactory.createPositionInText(1, 1, 0),
                        ChangestructureFactory.createPositionInText(newNumLines + 1, 0,
                                newContents.getStartPositionOfLine(newNumLines)),
                        newContents.getLinesConcatenated(0, newNumLines));

                this.createDiffViewer(view, scrollContent, firstRevision, lastRevision,
                        Arrays.asList(new Fragment[] { oldFragment }),
                        Arrays.asList(new Fragment[] { newFragment }),
                        new Position(oldStartOffset, oldEndOffset - oldStartOffset),
                        new Position(newStartOffset, newEndOffset - newStartOffset));
            }
        }
    }

    private static LineSequence fileToLineSequence(final FileInRevision file) {
        final byte[] data = file.getContents();
        if (data == null) {
            return null;
        }

        try {
            try {
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data));
                return new LineSequence(data, "UTF-8");
            } catch (final CharacterCodingException e) {
                return new LineSequence(data, "ISO-8859-1");
            }
        } catch (final IOException e) {
            return null;
        }
    }
}
