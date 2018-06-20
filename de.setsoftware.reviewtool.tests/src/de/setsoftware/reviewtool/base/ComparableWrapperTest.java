package de.setsoftware.reviewtool.base;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests {@link ComparableWrapper}.
 */
public class ComparableWrapperTest {

    @Test
    public void testEquals() {
        final ComparableWrapper<Long> l1 = ComparableWrapper.wrap(1L);
        final ComparableWrapper<Long> l2 = ComparableWrapper.wrap(2L);
        final ComparableWrapper<Integer> i2 = ComparableWrapper.wrap(2);

        assertThat(l1, is(equalTo(l1)));
        assertThat(l1, is(equalTo(ComparableWrapper.wrap(1L))));

        assertThat(l1, is(not(equalTo(l2))));
        assertThat(l2, is(not(equalTo(l1))));
        assertThat(l2, is(not(equalTo(i2))));
        assertThat(i2, is(not(equalTo(l2))));
    }

    @Test
    public void testHashCode() {
        final ComparableWrapper<Long> l1 = ComparableWrapper.wrap(1L);
        final ComparableWrapper<Integer> i2 = ComparableWrapper.wrap(2);

        assertThat(l1.hashCode(), is(equalTo(l1.getWrappedComparable().hashCode())));
        assertThat(i2.hashCode(), is(equalTo(i2.getWrappedComparable().hashCode())));
    }

    @Test
    public void testToString() {
        final ComparableWrapper<Long> l1 = ComparableWrapper.wrap(1L);
        final ComparableWrapper<Integer> i2 = ComparableWrapper.wrap(2);

        assertThat(l1.toString(), is(equalTo(l1.getWrappedComparable().toString())));
        assertThat(i2.toString(), is(equalTo(i2.getWrappedComparable().toString())));
    }

    @Test
    public void testLessOrEqualOnTotalOrder() {
        final ComparableWrapper<Long> l1 = ComparableWrapper.wrap(1L);
        final ComparableWrapper<Long> l2 = ComparableWrapper.wrap(2L);

        assertThat(l1.le(l1), is(equalTo(true)));
        assertThat(l1.le(l2), is(equalTo(true)));
        assertThat(l2.le(l1), is(equalTo(false)));
        assertThat(l2.le(l2), is(equalTo(true)));
    }
}
