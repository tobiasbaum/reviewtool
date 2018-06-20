package de.setsoftware.reviewtool.model.changestructure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IRevision;

/**
 * Tests revisions.
 */
public class RevisionTest {

    @Test
    public void testTotalOrder() {
        final IRevision u = ChangestructureFactory.createUnknownRevision(StubRepo.INSTANCE);
        final IRevision r1 = ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(1), StubRepo.INSTANCE);
        final IRevision r2 = ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(2), StubRepo.INSTANCE);
        final IRevision l = ChangestructureFactory.createLocalRevision(StubWorkingCopy.INSTANCE);

        assertThat(u.le(u), is(equalTo(true)));
        assertThat(u.le(r1), is(equalTo(true)));
        assertThat(u.le(r2), is(equalTo(true)));
        assertThat(u.le(l), is(equalTo(true)));

        assertThat(r1.le(u), is(equalTo(false)));
        assertThat(r1.le(r1), is(equalTo(true)));
        assertThat(r1.le(r2), is(equalTo(true)));
        assertThat(r1.le(l), is(equalTo(true)));

        assertThat(r2.le(u), is(equalTo(false)));
        assertThat(r2.le(r1), is(equalTo(false)));
        assertThat(r2.le(r2), is(equalTo(true)));
        assertThat(r2.le(l), is(equalTo(true)));

        assertThat(l.le(u), is(equalTo(false)));
        assertThat(l.le(r1), is(equalTo(false)));
        assertThat(l.le(r2), is(equalTo(false)));
        assertThat(l.le(l), is(equalTo(true)));
    }

    @Test
    public void testPartialOrder() {
        final IRevision u = ChangestructureFactory.createUnknownRevision(PartiallyOrderedRepo.INSTANCE);
        final IRevision r1 = ChangestructureFactory.createRepoRevision(new PartiallyOrderedID("abcd"), PartiallyOrderedRepo.INSTANCE);
        final IRevision r2 = ChangestructureFactory.createRepoRevision(new PartiallyOrderedID("efgh"), PartiallyOrderedRepo.INSTANCE);
        final IRevision l = ChangestructureFactory.createLocalRevision(PartiallyOrderedWorkingCopy.INSTANCE);

        assertThat(u.le(u), is(equalTo(true)));
        assertThat(u.le(r1), is(equalTo(true)));
        assertThat(u.le(r2), is(equalTo(true)));
        assertThat(u.le(l), is(equalTo(true)));

        assertThat(r1.le(u), is(equalTo(false)));
        assertThat(r1.le(r1), is(equalTo(true)));
        assertThat(r1.le(r2), is(equalTo(false)));
        assertThat(r1.le(l), is(equalTo(true)));

        assertThat(r2.le(u), is(equalTo(false)));
        assertThat(r2.le(r1), is(equalTo(false)));
        assertThat(r2.le(r2), is(equalTo(true)));
        assertThat(r2.le(l), is(equalTo(true)));

        assertThat(l.le(u), is(equalTo(false)));
        assertThat(l.le(r1), is(equalTo(false)));
        assertThat(l.le(r2), is(equalTo(false)));
        assertThat(l.le(l), is(equalTo(true)));
    }
}
