package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IDelta;
import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentList;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Default implementation of {@link IFileDiff}.
 */
public final class FileDiff implements IFileDiff {

    /**
     * The hunks this FileDiff object is made of. Later hunks depend on earlier hunks, i.e. the source/target fragment
     * positions of later hunks take the deltas of earlier hunks into consideration.
     */
    private final List<IHunk> hunks;
    private final IRevisionedFile fromRevision;
    private IRevisionedFile toRevision;

    /**
     * Creates an empty FileDiff object that will be filled with hunks.
     */
    public FileDiff(final IRevisionedFile revision) {
        this.hunks = new ArrayList<>();
        this.fromRevision = revision;
        this.toRevision = revision;
    }

    /**
     * Creates a FileDiff object that will be filled with hunks.
     */
    public FileDiff(final IRevisionedFile fromRevision, final IRevisionedFile toRevision) {
        this.hunks = new ArrayList<>();
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    /**
     * Creates a fully specified FileDiff object.
     * The hunks are transferred into a newly created list, such that the original list can be modified independently
     * later on.
     */
    private FileDiff(final IRevisionedFile fromRevision, final IRevisionedFile toRevision,
            final List<? extends IHunk> hunks) {
        this.hunks = new ArrayList<>(hunks);
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    @Override
    public List<? extends IHunk> getHunks() {
        return Collections.unmodifiableList(this.hunks);
    }

    @Override
    public IRevisionedFile getFrom() {
        return this.fromRevision;
    }

    @Override
    public IRevisionedFile getTo() {
        return this.toRevision;
    }

    @Override
    public IFileDiff setTo(final IRevisionedFile newTo) {
        return new FileDiff(this.fromRevision, newTo, this.hunks);
    }

    @Override
    public IFragment traceFragment(final IFragment source) {
        try {
            return this.createCombinedFragment(source).setFile(this.toRevision);
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public List<? extends IHunk> getHunksWithTargetChangesInOneOf(final Collection<? extends IFragment> fragments) {
        final List<IHunk> result = new ArrayList<>();
        for (final IHunk hunk : this.hunks) {
            if (hunk.getTarget().containsChangeInOneOf(fragments)) {
                result.add(hunk);
            }
        }
        return result;
    }

    @Override
    public IFileDiff merge(final IHunk hunkToMerge) throws IncompatibleFragmentException {
        final FileDiff result = new FileDiff(this.fromRevision, hunkToMerge.getTarget().getFile());
        final List<IHunk> stashedHunks = new ArrayList<>();
        final IDelta hunkDelta = hunkToMerge.getDelta();
        final int hunkStartLine = hunkToMerge.getSource().getFrom().getLine();
        boolean hunkCreated = false;
        for (final IHunk hunk : this.hunks) {
            if (hunk.getTarget().overlaps(hunkToMerge.getSource())) {
                stashedHunks.add(hunk);
            } else if (hunk.getTarget().getTo().compareTo(hunkToMerge.getSource().getFrom()) <= 0) {
                result.hunks.add(hunk.adjustTargetFile(result.toRevision));
            } else if (hunkCreated) {
                result.hunks.add(
                        hunk.adjustTarget(hunkDelta.ignoreColumnOffset(
                                hunk.getTarget().getFrom().getLine() != hunkStartLine))
                        .adjustTargetFile(result.toRevision));
            } else {
                result.hunks.add(this.createCombinedHunk(stashedHunks, hunkToMerge)
                        .adjustSourceFile(this.fromRevision)
                        .adjustTargetFile(result.toRevision));
                result.hunks.add(
                        hunk.adjustTarget(hunkDelta.ignoreColumnOffset(
                                hunk.getTarget().getFrom().getLine() != hunkStartLine))
                        .adjustTargetFile(result.toRevision));
                hunkCreated = true;
            }
        }
        if (!hunkCreated) {
            result.hunks.add(this.createCombinedHunk(stashedHunks, hunkToMerge)
                    .adjustSourceFile(this.fromRevision)
                    .adjustTargetFile(result.toRevision));
        }
        return result;
    }

    @Override
    public IFileDiff merge(final Collection<? extends IHunk> hunksToMerge) throws IncompatibleFragmentException {
        IFileDiff result = this;
        IDelta delta = new Delta();
        int lastLine = 0;
        for (IHunk hunk : hunksToMerge) {
            delta = delta.ignoreColumnOffset(hunk.getSource().getFrom().getLine() != lastLine);
            result = result.merge(hunk.adjustSource(delta));
            delta = delta.plus(hunk.getDelta());
            lastLine = hunk.getSource().getTo().getLine();
        }
        return result;
    }

    @Override
    public IFileDiff merge(final IFileDiff diff) throws IncompatibleFragmentException {
        return this.merge(diff.getHunks()).setTo(diff.getTo());
    }

    /**
     * Combines a list of neighbour hunks with a new hunk to be merged.
     *
     * @param hunks The neighbour hunks.
     * @param hunkToMerge The hunk to be merged.
     * @return The combined hunk.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the hunk list
     *      or if the resulting parts cannot be combined into one hunk.
     */
    private IHunk createCombinedHunk(final Collection<? extends IHunk> hunks, final IHunk hunkToMerge)
            throws IncompatibleFragmentException {
        final IFragmentList sources = new FragmentList();
        try {
            for (final IHunk hunk : hunks) {
                sources.addFragment(hunk.getSource());
            }
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        final IFragmentList targets = new FragmentList();
        try {
            for (final IHunk hunk : hunks) {
                targets.addFragment(hunk.getTarget());
            }
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        return new Hunk(
                this.combineSources(hunkToMerge, sources, targets),
                this.combineTargets(hunkToMerge, targets));
    }

    /**
     * Combines a list of neighbour hunks with a fragment to be merged.
     *
     * @param hunks The neighbour hunks.
     * @param fragment The fragment to be merged.
     * @return The combined fragment.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the hunk list
     *      or if the resulting parts cannot be combined into one fragment.
     */
    private IFragment createCombinedFragment(final IFragment fragment)
            throws IncompatibleFragmentException {

        final IFragmentList result = new FragmentList();
        IFragment fragmentRest = fragment;
        IDelta delta = new Delta();
        int lastLine = 0;

        for (final IHunk hunk : this.hunks) {
            final IFragment source = hunk.getSource();
            if (source.overlaps(fragment)) {
                final IFragment target = hunk.getTarget();
                result.addFragment(target);

                if (fragmentRest != null) {
                    final IFragmentList pieces = fragmentRest.subtract(source);
                    fragmentRest = null;
                    for (final IFragment piece : pieces.getFragments()) {
                        if (piece.getTo().compareTo(source.getFrom()) <= 0) {
                            delta = delta.ignoreColumnOffset(piece.getFrom().getLine() != lastLine);
                            result.addFragment(piece.adjust(delta));
                        } else {
                            fragmentRest = piece;
                        }
                    }
                }
            } else if (fragment.getTo().compareTo(source.getFrom()) <= 0) {
                break;
            }

            delta = delta.ignoreColumnOffset(hunk.getSource().getFrom().getLine() != lastLine);
            delta = delta.plus(hunk.getDelta());
            lastLine = hunk.getSource().getTo().getLine();
        }

        if (fragmentRest != null) {
            delta = delta.ignoreColumnOffset(fragmentRest.getFrom().getLine() != lastLine);
            result.addFragment(fragmentRest.adjust(delta));
        }

        result.coalesce();
        if (result.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return result.getFragments().get(0);
    }

    /**
     * Combines the source fragments of neighbour hunks with the source fragment of a hunk to be merged. The result
     * needs to be a single fragment.
     *
     * @param hunkToMerge The hunk to be merged.
     * @param sources The source fragments of neighbour hunks.
     * @param targets The target fragments of neighbour hunks.
     * @return A new fragment containing the combined sources.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the FileDiff object
     *              or if the resulting source parts cannot be combined into one fragment.
     */
    private IFragment combineSources(
            final IHunk hunkToMerge,
            final IFragmentList sources,
            final IFragmentList targets) throws IncompatibleFragmentException {
        final IFragmentList combinedSources = new FragmentList();
        combinedSources.addFragmentList(sources);

        for (final IFragment fragment : hunkToMerge.getSource().subtract(targets).getFragments()) {
            combinedSources.addFragment(fragment.adjust(
                    this.computeDeltaViaTargetFragmentUpTo(fragment.getFrom()).negate()));
        }

        combinedSources.coalesce();
        if (combinedSources.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return combinedSources.getFragments().get(0);
    }

    /**
     * Combines the target fragments of neighbour hunks with the target fragment of a hunk to be merged. The result
     * needs to be a single fragment.
     *
     * @param hunkToMerge The hunk to be merged.
     * @param targets The target fragments of neighbour hunks.
     * @return A new fragment containing the combined targets.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the FileDiff object
     *              or if the resulting target parts cannot be combined into one fragment.
     */
    private IFragment combineTargets(final IHunk hunkToMerge, final IFragmentList targets)
            throws Error, IncompatibleFragmentException {

        final IFragmentList adjustedTargets = new FragmentList();
        final IDelta hunkDelta = hunkToMerge.getDelta();
        final IFragment hunkTarget = hunkToMerge.getTarget();
        final IPositionInText hunkTargetStart = hunkTarget.getFrom();
        final Set<IFragment> hunkOrigins = new LinkedHashSet<>();
        hunkOrigins.add(hunkTarget);
        try {
            for (final IFragment curTarget : targets.getFragments()) {
                if (curTarget.overlaps(hunkToMerge.getSource())) {
                    hunkOrigins.add(curTarget);
                    final IFragmentList pieces = curTarget.subtract(hunkToMerge.getSource());
                    for (final IFragment piece : pieces.getFragments()) {
                        if (piece.getTo().compareTo(hunkTargetStart) <= 0) {
                            adjustedTargets.addFragment(piece);
                        } else {
                            adjustedTargets.addFragment(piece.adjust(hunkDelta));
                        }
                    }
                } else if (curTarget.getTo().compareTo(hunkTargetStart) <= 0) {
                    adjustedTargets.addFragment(curTarget);
                } else {
                    adjustedTargets.addFragment(curTarget.adjust(hunkDelta));
                }
            }
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        final IFragment newHunkTarget = new Fragment(
                hunkTarget.getFile(),
                hunkTarget.getFrom(),
                hunkTarget.getTo(),
                hunkOrigins);
        final IFragmentList combinedTargets = adjustedTargets.overlayBy(newHunkTarget);

        combinedTargets.coalesce();
        if (combinedTargets.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return combinedTargets.getFragments().get(0);
    }

    /**
     * Returns the target line delta to be added given a target {@link IPositionInText} because of hunks applied
     * before this line. It is computed as the number of lines earlier hunks added minus the number of lines
     * earlier hunks deleted.
     *
     * @param pos The position in the target file.
     * @return The line delta.
     */
    private IDelta computeDeltaViaTargetFragmentUpTo(final IPositionInText pos) {
        IDelta delta = new Delta();
        int lastLine = 0;
        for (final IHunk hunk : this.hunks) {
            if (hunk.getTarget().getTo().compareTo(pos) <= 0) {
                delta = delta.ignoreColumnOffset(hunk.getTarget().getFrom().getLine() != lastLine);
                delta = delta.plus(hunk.getDelta());
                lastLine = hunk.getTarget().getTo().getLine();
            } else {
                break;
            }
        }
        return delta.ignoreColumnOffset(pos.getLine() != lastLine);
    }
}
