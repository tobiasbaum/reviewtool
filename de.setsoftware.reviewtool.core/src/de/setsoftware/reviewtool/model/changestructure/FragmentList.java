package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A fragment list is a sorted collection of fragments.
 */
public class FragmentList {

    /**
     * The managed fragments.
     */
    private final List<Fragment> fragments;

    /**
     * Creates an empty fragment list.
     */
    public FragmentList() {
        this.fragments = new LinkedList<>();
    }

    /**
     * Creates a singleton fragment list.
     * @param fragment The fragment to add.
     */
    public FragmentList(final Fragment fragment) {
        this.fragments = new LinkedList<>();
        this.fragments.add(fragment);
    }

    /**
     * @return A read-only view on the fragments in this fragment list.
     */
    public List<Fragment> getFragments() {
        return Collections.unmodifiableList(this.fragments);
    }

    /**
     * Adds a fragment to the fragment list.
     * @param fragment The fragment to add.
     * @throws IncompatibleFragmentException if the fragment overlaps some other fragment in this fragment list.
     */
    public void addFragment(final Fragment fragment) throws IncompatibleFragmentException {
        final PositionInText posTo = fragment.getTo();
        final ListIterator<Fragment> it = this.fragments.listIterator();
        while (it.hasNext()) {
            final Fragment oldFragment = it.next();
            if (oldFragment.overlaps(fragment)) {
                throw new IncompatibleFragmentException();
            } else if (posTo.compareTo(oldFragment.getFrom()) < 0) {
                it.previous();
                it.add(fragment);
                return;
            }
        }
        this.fragments.add(fragment);
    }

    /**
     * Merges adjacent fragments.
     */
    public void coalesce() {
        final ListIterator<Fragment> it = this.fragments.listIterator();
        Fragment fragment = null;
        while (it.hasNext()) {
            final Fragment oldFragment = it.next();
            if (fragment == null) {
                fragment = oldFragment;
                it.remove();
            } else if (oldFragment.isAdjacentTo(fragment)) {
                it.remove();
                fragment = oldFragment.adjoin(fragment);
            } else {
                it.remove();
                it.add(fragment);
                fragment = oldFragment;
            }
        }
        if (fragment != null) {
            this.fragments.add(fragment);
        }
    }

    /**
     * Adds the fragments of some other fragment list to this fragment list.
     * @param fragmentList The other fragment list.
     * @throws IncompatibleFragmentException if some fragment in the other fragment list overlaps some other fragment
     *                                          in this fragment list.
     */
    public void addFragmentList(final FragmentList fragmentList) throws IncompatibleFragmentException {
        for (final Fragment fragment : fragmentList.fragments) {
            this.addFragment(fragment);
        }
    }

    /**
     * Overlays this fragment list by some {@link Fragment}. That means that parts that overlap are taken from the
     * fragment passed.
     * @param fragment The fragment.
     * @return A fragment list storing the overlay result.
     */
    public FragmentList overlayBy(final Fragment fragment) {
        final FragmentList result = new FragmentList();
        try {
            result.addFragment(fragment);
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }

        final PositionInText posTo = fragment.getTo();
        final ListIterator<Fragment> it = this.fragments.listIterator();
        while (it.hasNext()) {
            final Fragment oldFragment = it.next();
            if (posTo.lessThan(oldFragment.getFrom())) {
                break;
            }

            final ListIterator<Fragment> fIt = result.fragments.listIterator();
            while (fIt.hasNext()) {
                final Fragment f = fIt.next();
                final FragmentList rest = f.subtract(oldFragment);
                fIt.remove();
                for (final Fragment newFragment : rest.fragments) {
                    fIt.add(newFragment);
                }
            }
        }

        try {
            result.addFragmentList(this);
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }

        return result;
    }

    /**
     * Subtracts some {@link Fragment} from this fragment list.
     * @param fragment The fragment to subtract.
     * @return A list of remaining fragments.
     */
    public FragmentList subtract(final Fragment fragment) {
        final FragmentList result = new FragmentList();
        try {
            for (final Fragment oldFragment : this.fragments) {
                if (oldFragment.overlaps(fragment)) {
                    result.addFragmentList(oldFragment.subtract(fragment));
                } else {
                    result.addFragment(oldFragment);
                }
            }
        } catch (final IncompatibleFragmentException e) {
            throw new Error(e);
        }
        return result;
    }

    /**
     * Subtracts some other fragment list from this fragment list.
     * @param fragmentList The fragment list to subtract.
     * @return A list of remaining fragments.
     */
    public FragmentList subtract(final FragmentList fragmentList) {
        FragmentList result = this;
        for (final Fragment fragment : fragmentList.getFragments()) {
            result = result.subtract(fragment);
        }
        return result;
    }
}
