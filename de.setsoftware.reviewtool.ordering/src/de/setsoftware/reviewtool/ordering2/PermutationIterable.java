package de.setsoftware.reviewtool.ordering2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class PermutationIterable<S> implements Iterable<List<S>> {

    private static final class PermutationIterator<S> implements Iterator<List<S>> {

        private int[] next = null;

        private final int n;
        private int[] perm;
        private int[] dirs;
        private final S[] set;

        public PermutationIterator(Set<S> stops) {
            this.set = (S[]) stops.toArray();
            this.n = stops.size();
            if (this.n <= 0) {
                this.perm = (this.dirs = null);
            } else {
                this.perm = new int[this.n];
                this.dirs = new int[this.n];
                for(int i = 0; i < this.n; i++) {
                    this.perm[i] = i;
                    this.dirs[i] = -1;
                }
                this.dirs[0] = 0;
            }

            this.next = this.perm;
        }

        @Override
        public List<S> next() {
            final int[] r = this.makeNext();
            this.next = null;
            final List<S> ret = new ArrayList<>(this.set.length);
            for (int i = 0; i < r.length; i++) {
                ret.add(this.set[r[i]]);
            }
            return ret;
        }

        @Override
        public boolean hasNext() {
            return (this.makeNext() != null);
        }

        private int[] makeNext() {
            if (this.next != null) {
                return this.next;
            }
            if (this.perm == null) {
                return null;
            }

            // find the largest element with != 0 direction
            int i = -1, e = -1;
            for(int j = 0; j < this.n; j++) {
                if ((this.dirs[j] != 0) && (this.perm[j] > e)) {
                    e = this.perm[j];
                    i = j;
                }
            }

            if (i == -1) {
                return (this.next = (this.perm = (this.dirs = null))); // no more permutations
            }

            // swap with the element in its direction
            final int k = i + this.dirs[i];
            swap(i, k, this.dirs);
            swap(i, k, this.perm);
            // if it's at the start/end or the next element in the direction
            // is greater, reset its direction.
            if ((k == 0) || (k == this.n-1) || (this.perm[k + this.dirs[k]] > e)) {
                this.dirs[k] = 0;
            }

            // set directions to all greater elements
            for(int j = 0; j < this.n; j++) {
                if (this.perm[j] > e) {
                    this.dirs[j] = (j < k) ? +1 : -1;
                }
            }

            this.next = this.perm;
            return this.next;
        }

        protected static void swap(int i, int j, int[] arr) {
            final int v = arr[i];
            arr[i] = arr[j];
            arr[j] = v;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private final Set<S> stops;

    public PermutationIterable(Set<S> stops) {
        this.stops = stops;
    }

    @Override
    public Iterator<List<S>> iterator() {
        return new PermutationIterator<>(this.stops);
    }

}
