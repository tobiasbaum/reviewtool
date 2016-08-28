package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a set of difference hunks between different revisions of the same file. A FileDiff object can accumulate
 * hunks of different source/target revisions.
 */
public class FileDiff {

    /**
     * The hunks this FileDiff object is made of. Later hunks depend on earlier hunks, i.e. the source/target fragment
     * positions of later hunks take the deltas of earlier hunks into consideration.
     */
    private final List<Hunk> hunks;

    /**
     * Creates an empty FileDiff object.
     */
    public FileDiff() {
        this.hunks = new ArrayList<>();
    }

    /**
     * @return The hunks this FileDiff object is made of. Later hunks depend on earlier hunks, so they need to be
     *          merged separately to achieve the same result.
     */
    List<Hunk> getHunks() {
        return Collections.unmodifiableList(this.hunks);
    }

    /**
     * Returns the hunk matching a given source fragment. First, equal or overlapping fragments are considered. If not
     * found and the fragment passed is a deletion, adjacent fragments are considered.
     *
     * @param fragment The source fragment.
     * @return The hunk whose source fragment matches passed source fragment or null if no such hunk has been found.
     */
    public Hunk getHunkForSource(final Fragment fragment) {
        // first try: search for equal or overlapping fragment
        for (final Hunk hunk : this.hunks) {
            final Fragment source = hunk.getSource();
            if (source.equals(fragment) || source.overlaps(fragment)) {
                return hunk;
            }
        }

        // second try: if fragment is a deletion, search for neighbour fragment
        if (fragment.isDeletion()) {
            for (final Hunk hunk : this.hunks) {
                final Fragment source = hunk.getSource();
                if (source.isAdjacentTo(fragment)) {
                    return hunk;
                }
            }
        }

        // not found
        return null;
    }

    /**
     * Returns a list of hunks related to a collection of target fragments. "Related to" means that their target
     * fragments either overlap a given target fragment or that their target fragments are adjacent to a given target
     * fragment.
     *
     * @param fragments The target fragments to "reach".
     * @return A list of hunks whose target fragments overlap or are adjacent to a collection of given target
     *          fragments.
     */
    public List<Hunk> getHunksForTargets(final Collection<? extends Fragment> fragments) {
        final List<Hunk> result = new ArrayList<>();
        for (final Hunk hunk : this.hunks) {
            final Fragment target = hunk.getTarget();
            for (final Fragment selector : fragments) {
                if (target.equals(selector) || target.overlaps(selector) || target.isAdjacentTo(selector)) {
                    result.add(hunk);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Merges a hunk into this FileDiff. This is allowed only for hunks that do not overlap with hunks in this FileDiff
     * object. The FileDiff object is not changed, rather a new FileDiff object is created and returned (value
     * semantics).
     *
     * @param hunkToMerge The hunk to be merged.
     * @return The new FileDiff object containing the merged hunk.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the FileDiff object.
     */
    FileDiff merge(final Hunk hunkToMerge) throws IncompatibleFragmentException {
        final FileDiff result = new FileDiff();
        final List<Hunk> hunks = new ArrayList<Hunk>();
        boolean hunkCreated = false;
        for (final Hunk hunk : this.hunks) {
            if (hunk.getTarget().overlaps(hunkToMerge.getSource())) {
                hunks.add(hunk);
            } else if (hunk.getTarget().getTo().compareTo(hunkToMerge.getSource().getFrom()) < 0) {
                result.hunks.add(hunk);
            } else if (hunkCreated) {
                result.hunks.add(hunk.adjustTarget(hunkToMerge.getDelta()));
            } else {
                result.hunks.add(this.createCombinedHunk(hunks, hunkToMerge));
                result.hunks.add(hunk.adjustTarget(hunkToMerge.getDelta()));
                hunkCreated = true;
            }
        }
        if (!hunkCreated) {
            result.hunks.add(this.createCombinedHunk(hunks, hunkToMerge));
        }
        return result;
    }

    /**
     * Merges a list of hunks into this FileDiff. This operation assumes that the hunks do not know anything about each
     * other, i.e. the source/target fragments refer to the same state of file. Therefore, applying two hunks
     * separately is different from applying them together.
     *
     * @param hunksToMerge A list of hunks to be merged.
     * @return The new FileDiff object containing the merged hunks.
     * @throws IncompatibleFragmentException if some hunk to be merged overlaps with some hunk in the FileDiff object.
     */
    FileDiff merge(final Collection<? extends Hunk> hunksToMerge) throws IncompatibleFragmentException {
        FileDiff result = this;
        int delta = 0;
        for (Hunk hunk : hunksToMerge) {
            hunk = hunk.adjustSource(delta);
            result = result.merge(hunk);
            delta += hunk.getDelta();
        }
        return result;
    }

    /**
     * Combines a list of neighbour hunks with a new hunk to be merged.
     *
     * @param hunks The neighbour hunks.
     * @param hunkToMerge The hunk to be merged.
     * @return The combined hunk.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the hunk list.
     */
    private Hunk createCombinedHunk(final Collection<? extends Hunk> hunks, final Hunk hunkToMerge)
            throws IncompatibleFragmentException {
        final FragmentList sources = new FragmentList();
        try {
            for (final Hunk hunk : hunks) {
                sources.addFragment(hunk.getSource());
            }
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }

        final FragmentList targets = new FragmentList();
        try {
            for (final Hunk hunk : hunks) {
                targets.addFragment(hunk.getTarget());
            }
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }

        return new Hunk(
                this.combineSources(hunkToMerge, sources, targets),
                this.combineTargets(hunkToMerge, targets));
    }

    /**
     * Combines the source fragments of meighbour hunks with the source fragment of a hunk to be merged. The result
     * needs to be a single fragment.
     *
     * @param hunkToMerge The hunk to be merged.
     * @param sources The source fragments of neighbour hunks.
     * @param targets The target fragments of neighbour hunks.
     * @return A new fragment containing the combined sources.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the FileDiff object
     *              or if the resulting source parts cannot be combined into one fragment.
     */
    private Fragment combineSources(final Hunk hunkToMerge, final FragmentList sources, final FragmentList targets)
            throws IncompatibleFragmentException {
        final FragmentList combinedSources = new FragmentList();
        for (final Fragment fragment : hunkToMerge.getSource().subtract(targets).getFragments()) {
            combinedSources.addFragment(fragment.adjust(-this.computeDeltaUpTo(fragment.getFrom())));
        }

        combinedSources.addFragmentList(sources);
        combinedSources.coalesce();
        if (combinedSources.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return combinedSources.getFragments().get(0);
    }

    /**
     * Combines the target fragments of meighbour hunks with the target fragment of a hunk to be merged. The result
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
        final int hunkDelta = hunkToMerge.getDelta();
        final Fragment hunkTarget = hunkToMerge.getTarget();
        final PositionInText hunkTargetStart = hunkTarget.getFrom();
        try {
            for (final Fragment curTarget : targets.getFragments()) {
                if (curTarget.overlaps(hunkToMerge.getSource())) {
                    final FragmentList pieces = curTarget.subtract(hunkToMerge.getSource());
                    for (final Fragment piece : pieces.getFragments()) {
                        if (piece.getTo().lessThan(hunkTargetStart)) {
                            adjustedTargets.addFragment(piece);
                        } else {
                            adjustedTargets.addFragment(piece.adjust(hunkDelta));
                        }
                    }
                } else if (curTarget.getTo().lessThan(hunkTargetStart)) {
                    adjustedTargets.addFragment(curTarget);
                } else {
                    adjustedTargets.addFragment(curTarget.adjust(hunkDelta));
                }
            }
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }

        final FragmentList combinedTargets = adjustedTargets.overlayBy(hunkTarget);
        combinedTargets.coalesce();
        if (combinedTargets.getFragments().size() != 1) {
            throw new IncompatibleFragmentException();
        }
        return combinedTargets.getFragments().get(0);
    }

    /**
     * Returns the target line delta to be added given a {@link PositionInText} because of hunks applied before this
     * line. It is computed as the number of lines earlier hunks added minus the number of lines earlier hunks deleted.
     *
     * @param pos The position in question.
     * @return The line delta.
     */
    private int computeDeltaUpTo(final PositionInText pos) {
        int result = 0;
        for (final Hunk hunk : this.hunks) {
            if (hunk.getTarget().getTo().lessThan(pos)) {
                result += hunk.getDelta();
            } else if (pos.lessThan(hunk.getTarget().getFrom())) {
                break;
            }
        }
        return result;
    }

}
