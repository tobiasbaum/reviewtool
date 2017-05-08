package de.setsoftware.reviewtool.model.api;

import java.util.List;

/**
 * A fragment list is a sorted collection of fragments.
 */
public interface IFragmentList {

    /**
     * @return A read-only view on the fragments in this fragment list.
     */
    public abstract List<? extends IFragment> getFragments();

    /**
     * @return {@code true} if this fragment list is empty.
     */
    public abstract boolean isEmpty();

    /**
     * Adds a fragment to the fragment list.
     * @param fragment The fragment to add.
     * @throws IncompatibleFragmentException if the fragment overlaps some other fragment in this fragment list.
     */
    public abstract void addFragment(IFragment fragment) throws IncompatibleFragmentException;

    /**
     * Merges adjacent fragments.
     */
    public abstract void coalesce();

    /**
     * Adds the fragments of some other fragment list to this fragment list.
     * @param fragmentList The other fragment list.
     * @throws IncompatibleFragmentException if some fragment in the other fragment list overlaps some other fragment
     *                                          in this fragment list.
     */
    public abstract void addFragmentList(IFragmentList fragmentList) throws IncompatibleFragmentException;

    /**
     * Overlays this fragment list by some {@link IFragment}. That means that parts that overlap are taken
     * from the fragment passed.
     * @param fragment The fragment.
     * @return A fragment list storing the overlay result.
     */
    public abstract IFragmentList overlayBy(IFragment fragment);

    /**
     * Subtracts some {@link IFragment} from this fragment list.
     * @param fragment The fragment to subtract.
     * @return A list of remaining fragments.
     */
    public abstract IFragmentList subtract(IFragment fragment);

    /**
     * Subtracts some other fragment list from this fragment list.
     * @param fragmentList The fragment list to subtract.
     * @return A list of remaining fragments.
     */
    public abstract IFragmentList subtract(IFragmentList fragmentList);

    /**
     * Moves fragments starting at a given position.
     * @param pos The start position.
     * @param delta The delta to apply.
     */
    public abstract IFragmentList move(IPositionInText pos, IDelta delta);

}
