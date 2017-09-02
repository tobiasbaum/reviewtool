package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IDelta;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentList;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Default implementation of {@link IFragmentList}.
 */
public class FragmentList implements IFragmentList {

    /**
     * The managed fragments.
     */
    private final List<IFragment> fragments;

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
    public FragmentList(final IFragment fragment) {
        this.fragments = new LinkedList<>();
        this.fragments.add(fragment);
    }

    @Override
    public List<? extends IFragment> getFragments() {
        return Collections.unmodifiableList(this.fragments);
    }

    @Override
    public boolean isEmpty() {
        return this.fragments.isEmpty();
    }

    @Override
    public void addFragment(final IFragment fragment) throws IncompatibleFragmentException {
        final IPositionInText posTo = fragment.getTo();
        final ListIterator<IFragment> it = this.fragments.listIterator();
        while (it.hasNext()) {
            final IFragment oldFragment = it.next();
            if (oldFragment.overlaps(fragment)) {
                throw new IncompatibleFragmentException();
            } else if (posTo.compareTo(oldFragment.getFrom()) <= 0) {
                it.previous();
                it.add(fragment);
                return;
            }
        }
        this.fragments.add(fragment);
    }

    @Override
    public void coalesce() {
        final ListIterator<IFragment> it = this.fragments.listIterator();
        IFragment fragment = null;
        while (it.hasNext()) {
            final IFragment oldFragment = it.next();
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

    @Override
    public void addFragmentList(final IFragmentList fragmentList) throws IncompatibleFragmentException {
        for (final IFragment fragment : fragmentList.getFragments()) {
            this.addFragment(fragment);
        }
    }

    @Override
    public IFragmentList overlayBy(final IFragment fragment) {
        final FragmentList result = new FragmentList();
        try {
            result.addFragment(fragment);
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        final IPositionInText posTo = fragment.getTo();
        final ListIterator<IFragment> it = this.fragments.listIterator();
        while (it.hasNext()) {
            final IFragment oldFragment = it.next();
            if (posTo.compareTo(oldFragment.getFrom()) <= 0) {
                break;
            }

            final ListIterator<IFragment> fIt = result.fragments.listIterator();
            while (fIt.hasNext()) {
                final IFragment f = fIt.next();
                final IFragmentList rest = f.subtract(oldFragment);
                fIt.remove();
                for (final IFragment newFragment : rest.getFragments()) {
                    fIt.add(newFragment);
                }
            }
        }

        try {
            result.addFragmentList(this);
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }

        return result;
    }

    @Override
    public IFragmentList subtract(final IFragment fragment) {
        final FragmentList result = new FragmentList();
        try {
            for (final IFragment oldFragment : this.fragments) {
                if (oldFragment.overlaps(fragment)) {
                    result.addFragmentList(oldFragment.subtract(fragment));
                } else {
                    result.addFragment(oldFragment);
                }
            }
        } catch (final IncompatibleFragmentException e) {
            throw new ReviewtoolException(e);
        }
        return result;
    }

    @Override
    public IFragmentList subtract(final IFragmentList fragmentList) {
        IFragmentList result = this;
        for (final IFragment fragment : fragmentList.getFragments()) {
            result = result.subtract(fragment);
        }
        return result;
    }

    @Override
    public IFragmentList move(final IPositionInText pos, final IDelta delta) {
        final FragmentList result = new FragmentList();
        for (final IFragment fragment : this.fragments) {
            if (fragment.getFrom().compareTo(pos) <= 0) {
                result.fragments.add(fragment);
            } else {
                result.fragments.add(fragment.adjust(delta));
            }
        }
        return result;
    }
}
