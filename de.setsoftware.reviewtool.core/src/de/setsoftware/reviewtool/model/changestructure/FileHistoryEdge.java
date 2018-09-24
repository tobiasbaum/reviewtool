package de.setsoftware.reviewtool.model.changestructure;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Implementation of a {@link IMutableFileHistoryEdge}.
 */
public final class FileHistoryEdge extends ProxyableFileHistoryEdge {

    private static final long serialVersionUID = 2504351095940964539L;

    private final FileHistoryGraph graph;
    private final ProxyableFileHistoryNode ancestor;
    private final ProxyableFileHistoryNode descendant;
    private Type type;
    private IFileDiff diff;

    /**
     * Constructor.
     * @param ancestor The ancestor node of the edge.
     * @param descendant The descendant node of the edge.
     * @param type The type of the edge.
     */
    FileHistoryEdge(
            final FileHistoryGraph graph,
            final ProxyableFileHistoryNode ancestor,
            final ProxyableFileHistoryNode descendant,
            final Type type) {
        this.graph = graph;
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.type = type;
    }

    @Override
    public FileHistoryGraph getGraph() {
        return this.graph;
    }

    @Override
    public ProxyableFileHistoryNode getAncestor() {
        return this.ancestor;
    }

    @Override
    public ProxyableFileHistoryNode getDescendant() {
        return this.descendant;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    void setType(final Type type) {
        this.type = type;
    }

    @Override
    public IFileDiff getDiff() {
        if (this.diff == null) {
            this.diff = this.computeDiff();
        }
        return this.diff;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof FileHistoryEdge) {
            final FileHistoryEdge other = (FileHistoryEdge) o;
            return this.ancestor.equals(other.ancestor)
                    && this.descendant.equals(other.descendant)
                    && this.type.equals(other.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.ancestor.hashCode() ^ this.descendant.hashCode();
    }

    /**
     * Computes the difference between the file contents of ancestor and descendant by using the diff algorithm
     * associated with our {@link IFileHistoryGraph}.
     * @throws ReviewtoolException if an error occurred while loading file contents or computing difference.
     */
    private IFileDiff computeDiff() {
        final byte[] oldFileContents;
        final byte[] newFileContents;

        try {
            oldFileContents = this.ancestor.getFile().getContents();
            newFileContents = this.descendant.getFile().getContents();
        } catch (final Exception e) {
            // if loading old or new file contents failed, we cannot do anything reasonable
            throw new ReviewtoolException(e);
        }

        final List<Pair<IFragment, IFragment>> textChanges = this.graph.getDiffAlgorithm().determineDiff(
                this.ancestor.getFile(),
                oldFileContents,
                this.descendant.getFile(),
                newFileContents,
                this.guessEncoding(oldFileContents, newFileContents));

        final List<IHunk> hunks = new ArrayList<>();
        for (final Pair<IFragment, IFragment> pos : textChanges) {
            hunks.add(ChangestructureFactory.createHunk(pos.getFirst(), pos.getSecond()));
        }

        try {
            return new FileDiff(this.ancestor.getFile(), this.descendant.getFile()).merge(hunks);
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }
    }

    private String guessEncoding(final byte[] oldFileContent, final byte[] newFileContent) {
        if (this.isValidUtf8(oldFileContent) && this.isValidUtf8(newFileContent)) {
            return "UTF-8";
        } else {
            return "ISO-8859-1";
        }
    }

    /**
     * Returns true iff the given bytes are syntactically valid UTF-8.
     */
    private boolean isValidUtf8(final byte[] content) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content));
            return true;
        } catch (final CharacterCodingException e) {
            return false;
        }
    }

    /**
     * Replaces this object by a proxy when serializing.
     */
    protected final Object writeReplace() {
        return new FileHistoryEdgeProxy(this.graph, this.ancestor.getFile(), this.descendant.getFile(), this.type);
    }
}
