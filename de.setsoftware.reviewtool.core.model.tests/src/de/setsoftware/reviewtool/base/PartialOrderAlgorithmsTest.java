package de.setsoftware.reviewtool.base;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests algorithms operating on partially ordered collections.
 */
public class PartialOrderAlgorithmsTest {

    /**
     * Represents numbers partially ordered by divisor ordering.
     */
    private final class MyNumber implements IPartiallyComparable<MyNumber> {
        private final long value;

        MyNumber(final long value) {
            this.value = value;
        }

        @Override
        public boolean le(final MyNumber other) {
            return other.value % this.value == 0;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof MyNumber) {
                final MyNumber o = (MyNumber) obj;
                return this.value == o.value;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.value);
        }

        @Override
        public String toString() {
            return Long.toString(this.value);
        }
    }

    @Test
    public void testGetSomeMinimum() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n1, n2, n3, n4));
        assertThat(PartialOrderAlgorithms.getSomeMinimum(numbers), is(equalTo(n1)));

        numbers.remove(n1);
        assertThat(PartialOrderAlgorithms.getSomeMinimum(numbers), anyOf(
                is(equalTo(n2)),
                is(equalTo(n3))));

        numbers.remove(n3);
        assertThat(PartialOrderAlgorithms.getSomeMinimum(numbers), is(equalTo(n2)));

        numbers.remove(n2);
        assertThat(PartialOrderAlgorithms.getSomeMinimum(numbers), is(equalTo(n4)));

        numbers.remove(n4);
        assertThat(PartialOrderAlgorithms.getSomeMinimum(numbers), is(nullValue()));
    }

    @Test
    public void testGetSomeMaximum() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n1, n2, n3, n4));
        assertThat(PartialOrderAlgorithms.getSomeMaximum(numbers), is(equalTo(n4)));

        numbers.remove(n4);
        assertThat(PartialOrderAlgorithms.getSomeMaximum(numbers), anyOf(
                is(equalTo(n2)),
                is(equalTo(n3))));

        numbers.remove(n3);
        assertThat(PartialOrderAlgorithms.getSomeMaximum(numbers), is(equalTo(n2)));

        numbers.remove(n2);
        assertThat(PartialOrderAlgorithms.getSomeMaximum(numbers), is(equalTo(n1)));

        numbers.remove(n1);
        assertThat(PartialOrderAlgorithms.getSomeMaximum(numbers), is(nullValue()));
    }

    @Test
    public void testGetAllMinimalElements() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);
        final MyNumber n5 = new MyNumber(5L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n1, n2, n3, n4, n5));
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Arrays.asList(n1))));

        numbers.remove(n1);
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Arrays.asList(n2, n3, n5))));

        numbers.remove(n2);
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Arrays.asList(n3, n4, n5))));

        numbers.remove(n3);
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Arrays.asList(n4, n5))));

        numbers.remove(n4);
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Arrays.asList(n5))));

        numbers.remove(n5);
        assertThat(PartialOrderAlgorithms.getAllMinimalElements(numbers), is(equalTo(
                Collections.<MyNumber>emptyList())));
    }

    @Test
    public void testGetAllMaximalElements() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);
        final MyNumber n5 = new MyNumber(5L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n1, n2, n3, n4, n5));
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Arrays.asList(n5, n4, n3))));

        numbers.remove(n5);
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Arrays.asList(n4, n3))));

        numbers.remove(n4);
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Arrays.asList(n3, n2))));

        numbers.remove(n3);
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Arrays.asList(n2))));

        numbers.remove(n2);
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Arrays.asList(n1))));

        numbers.remove(n1);
        assertThat(PartialOrderAlgorithms.getAllMaximalElements(numbers), is(equalTo(
                Collections.<MyNumber>emptyList())));
    }

    @Test
    public void testTopoSortSortedSet() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n1, n2, n3, n4));
        assertThat(PartialOrderAlgorithms.topoSort(numbers), is(equalTo(numbers)));
    }

    @Test
    public void testTopoSortUnsortedSetWithLeastElement() {
        final MyNumber n1 = new MyNumber(1L);
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n4, n3, n2, n1));
        assertThat(PartialOrderAlgorithms.topoSort(numbers), anyOf(
                is(equalTo(Arrays.asList(n1, n2, n3, n4))),
                is(equalTo(Arrays.asList(n1, n2, n4, n3))),
                is(equalTo(Arrays.asList(n1, n3, n2, n4)))));
    }

    @Test
    public void testTopoSortUnsortedSetWithNoLeastElement() {
        final MyNumber n2 = new MyNumber(2L);
        final MyNumber n3 = new MyNumber(3L);
        final MyNumber n4 = new MyNumber(4L);

        final List<MyNumber> numbers = new ArrayList<>();
        numbers.addAll(Arrays.asList(n4, n3, n2));
        assertThat(PartialOrderAlgorithms.topoSort(numbers), anyOf(
                is(equalTo(Arrays.asList(n2, n3, n4))),
                is(equalTo(Arrays.asList(n2, n4, n3))),
                is(equalTo(Arrays.asList(n3, n2, n4)))));
    }
}
