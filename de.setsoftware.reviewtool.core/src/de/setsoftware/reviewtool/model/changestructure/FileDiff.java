package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Represents a set of difference hunks between different revisions of the same file. A FileDiff object can accumulate
 * hunks of different source/target revisions.
 */
public final class FileDiff {

    /**
     * The hunks this FileDiff object is made of. Later hunks depend on earlier hunks, i.e. the source/target fragment
     * positions of later hunks take the deltas of earlier hunks into consideration.
     */
    private final List<Hunk> hunks;
    private final FileInRevision fromRevision;
    private FileInRevision toRevision;

    /**
     * Creates an empty FileDiff object that will be filled with hunks.
     */
    public FileDiff(final FileInRevision revision) {
        this.hunks = new ArrayList<>();
        this.fromRevision = revision;
        this.toRevision = revision;
    }

    /**
     * Creates a FileDiff object that will be filled with hunks.
     */
    public FileDiff(final FileInRevision fromRevision, final FileInRevision toRevision) {
        this.hunks = new ArrayList<>();
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    /**
     * Creates a fully specified FileDiff object.
     */
    private FileDiff(final FileInRevision fromRevision, final FileInRevision toRevision, final List<Hunk> hunks) {
        this.hunks = new ArrayList<>(hunks);
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
    }

    /**
     * @return The hunks this FileDiff object is made of. Later hunks depend on earlier hunks, so they need to be
     *          merged separately to achieve the same result.
     */
    List<Hunk> getHunks() {
        return Collections.unmodifiableList(this.hunks);
    }

    /**
     * @return The {@link FileInRevision} this diff starts at.
     */
    public FileInRevision getFrom() {
        return this.fromRevision;
    }

    /**
     * @return The {@link FileInRevision} this diff ends at.
     */
    public FileInRevision getTo() {
        return this.toRevision;
    }

    /**
     * Creates a copy of this object with a new target revision. The list of hunks is copied in such a way that
     * both lists can be modified separately after the copy.
     *
     * @param newTo The new target revision.
     * @return The requested copy.
     */
    public FileDiff setTo(final FileInRevision newTo) {
        return new FileDiff(this.fromRevision, newTo, this.hunks);
    }
    /**
     * Traces a source fragment over the recorded hunks to the last known file revision.
     * @param source The source fragment.
     * @return The resulting fragment matching the last known file revision.
     */
    public Fragment traceFragment(final Fragment source) {
        try {
            return this.createCombinedFragment(source).setFile(this.toRevision);
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }
    }

    /**
     * Returns a list of hunks related to a collection of target {@link Fragment}s. A hunk is related if its target
     * fragment originates from some fragment that overlaps or is adjacent to at least one of the fragments passed.
     */
    public List<Hunk> getHunksWithTargetChangesInOneOf(final Collection<Fragment> fragments) {
        final List<Hunk> result = new ArrayList<>();
        for (final Hunk hunk : this.hunks) {
            if (hunk.getTarget().containsChangeInOneOf(fragments)) {
                result.add(hunk);
            }
        }
        return result;
    }

    /**
     * Merges a hunk into this FileDiff. This is allowed only for hunks that do not overlap with hunks in this FileDiff
     * object.
     * <p/>
     * The FileDiff object is not changed, rather a new FileDiff object is created and returned (value semantics).
     *
     * @param hunkToMerge The hunk to be merged.
     * @return The new FileDiff object containing the merged hunk.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the FileDiff object.
     */
    public FileDiff merge(final Hunk hunkToMerge) throws IncompatibleFragmentException {
        final FileDiff result = new FileDiff(this.fromRevision, hunkToMerge.getTarget().getFile());
        final List<Hunk> stashedHunks = new ArrayList<Hunk>();
        final Delta hunkDelta = hunkToMerge.getDelta();
        final int hunkStartLine = hunkToMerge.getSource().getFrom().getLine();
        boolean hunkCreated = false;
        for (final Hunk hunk : this.hunks) {
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

    /**
     * Merges a list of hunks into this FileDiff. This operation assumes that the hunks do not know anything about each
     * other, i.e. the source/target fragments refer to the same state of file. Therefore, applying two hunks
     * separately is different from applying them together.
     * <p/>
     * The FileDiff object is not changed, rather a new FileDiff object is created and returned (value semantics).
     *
     * @param hunksToMerge A list of hunks to be merged.
     * @return The new FileDiff object containing the merged hunks.
     * @throws IncompatibleFragmentException if some hunk to be merged overlaps with some hunk in the FileDiff object.
     */
    public FileDiff merge(final Collection<? extends Hunk> hunksToMerge) throws IncompatibleFragmentException {
        FileDiff result = this;
        Delta delta = new Delta();
        int lastLine = 0;
        for (Hunk hunk : hunksToMerge) {
            delta = delta.ignoreColumnOffset(hunk.getSource().getFrom().getLine() != lastLine);
            result = result.merge(hunk.adjustSource(delta));
            delta = delta.plus(hunk.getDelta());
            lastLine = hunk.getSource().getTo().getLine();
        }
        return result;
    }

    /**
     * Merges a list of hunks of a FileDiff into this FileDiff.
     * <p/>
     * The FileDiff object is not changed, rather a new FileDiff object is created and returned (value semantics).
     *
     * @param diff A FileDiff to merge.
     * @return The new FileDiff object containing the merged hunks.
     * @throws IncompatibleFragmentException if some hunk to be merged overlaps with some hunk in the FileDiff object.
     */
    public FileDiff merge(final FileDiff diff) throws IncompatibleFragmentException {
        return this.merge(diff.hunks).setTo(diff.toRevision);
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
    private Hunk createCombinedHunk(final Collection<? extends Hunk> hunks, final Hunk hunkToMerge)
            throws IncompatibleFragmentException {
        final FragmentList sources = new FragmentList();
        try {
            for (final Hunk hunk : hunks) {
                sources.addFragment(hunk.getSource());
            }
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        final FragmentList targets = new FragmentList();
        try {
            for (final Hunk hunk : hunks) {
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
    private Fragment createCombinedFragment(final Fragment fragment)
            throws IncompatibleFragmentException {

        final FragmentList result = new FragmentList();
        Fragment fragmentRest = fragment;
        Delta delta = new Delta();
        int lastLine = 0;

        for (final Hunk hunk : this.hunks) {
            final Fragment source = hunk.getSource();
            if (source.overlaps(fragment)) {
                final Fragment target = hunk.getTarget();
                result.addFragment(target);

                if (fragmentRest != null) {
                    final FragmentList pieces = fragmentRest.subtract(source);
                    fragmentRest = null;
                    for (final Fragment piece : pieces.getFragments()) {
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
    private Fragment combineSources(
            final Hunk hunkToMerge,
            final FragmentList sources,
            final FragmentList targets) throws IncompatibleFragmentException {
        final FragmentList combinedSources = new FragmentList();
        combinedSources.addFragmentList(sources);

        for (final Fragment fragment : hunkToMerge.getSource().subtract(targets).getFragments()) {
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
    private Fragment combineTargets(final Hunk hunkToMerge, final FragmentList targets)
            throws Error, IncompatibleFragmentException {

        final FragmentList adjustedTargets = new FragmentList();
        final Delta hunkDelta = hunkToMerge.getDelta();
        final Fragment hunkTarget = hunkToMerge.getTarget();
        final PositionInText hunkTargetStart = hunkTarget.getFrom();
        final Set<Fragment> hunkOrigins = new LinkedHashSet<>();
        hunkOrigins.add(hunkTarget);
        try {
            for (final Fragment curTarget : targets.getFragments()) {
                if (curTarget.overlaps(hunkToMerge.getSource())) {
                    hunkOrigins.add(curTarget);
                    final FragmentList pieces = curTarget.subtract(hunkToMerge.getSource());
                    for (final Fragment piece : pieces.getFragments()) {
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

        final Fragment newHunkTarget = new Fragment(
                hunkTarget.getFile(),
                hunkTarget.getFrom(),
                hunkTarget.getTo(),
                hunkOrigins);
        final FragmentList combinedTargets = adjustedTargets.overlayBy(newHunkTarget);

        combinedTargets.coalesce();
        if (combinedTargets.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return combinedTargets.getFragments().get(0);
    }

    /**
     * Returns the target line delta to be added given a target {@link PositionInText} because of hunks applied
     * before this line. It is computed as the number of lines earlier hunks added minus the number of lines
     * earlier hunks deleted.
     *
     * @param pos The position in the target file.
     * @return The line delta.
     */
    private Delta computeDeltaViaTargetFragmentUpTo(final PositionInText pos) {
        Delta delta = new Delta();
        int lastLine = 0;
        for (final Hunk hunk : this.hunks) {
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
