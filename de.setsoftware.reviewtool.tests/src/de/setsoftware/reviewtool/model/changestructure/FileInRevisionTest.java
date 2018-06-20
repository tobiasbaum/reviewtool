package de.setsoftware.reviewtool.model.changestructure;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Tests {@link FileInRevision}.
 */
public class FileInRevisionTest {

    @Test
    public void testTotalOrder() {
        final IRevision u = ChangestructureFactory.createUnknownRevision(StubRepo.INSTANCE);
        final IRevision r1 = ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(1), StubRepo.INSTANCE);
        final IRevision r2 = ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(2), StubRepo.INSTANCE);
        final IRevision l = ChangestructureFactory.createLocalRevision(StubWorkingCopy.INSTANCE);

        final IRevisionedFile fU_a = ChangestructureFactory.createFileInRevision("/a", u);
        final IRevisionedFile fU_b = ChangestructureFactory.createFileInRevision("/b", u);
        final IRevisionedFile fR1_a = ChangestructureFactory.createFileInRevision("/a", r1);
        final IRevisionedFile fR1_b = ChangestructureFactory.createFileInRevision("/b", r1);
        final IRevisionedFile fR2_a = ChangestructureFactory.createFileInRevision("/a", r2);
        final IRevisionedFile fR2_b = ChangestructureFactory.createFileInRevision("/b", r2);
        final IRevisionedFile fL_a = ChangestructureFactory.createFileInRevision("/a", l);
        final IRevisionedFile fL_b = ChangestructureFactory.createFileInRevision("/b", l);

        assertThat(fU_a.le(fU_a), is(equalTo(true)));
        assertThat(fU_a.le(fU_b), is(equalTo(true)));
        assertThat(fU_a.le(fR1_a), is(equalTo(true)));
        assertThat(fU_a.le(fR1_b), is(equalTo(true)));
        assertThat(fU_a.le(fR2_a), is(equalTo(true)));
        assertThat(fU_a.le(fR2_b), is(equalTo(true)));
        assertThat(fU_a.le(fL_a), is(equalTo(true)));
        assertThat(fU_a.le(fL_b), is(equalTo(true)));

        assertThat(fU_b.le(fU_a), is(equalTo(false)));
        assertThat(fU_b.le(fU_b), is(equalTo(true)));
        assertThat(fU_b.le(fR1_a), is(equalTo(true)));
        assertThat(fU_b.le(fR1_b), is(equalTo(true)));
        assertThat(fU_b.le(fR2_a), is(equalTo(true)));
        assertThat(fU_b.le(fR2_b), is(equalTo(true)));
        assertThat(fU_b.le(fL_a), is(equalTo(true)));
        assertThat(fU_b.le(fL_b), is(equalTo(true)));

        assertThat(fR1_a.le(fU_a), is(equalTo(false)));
        assertThat(fR1_a.le(fU_b), is(equalTo(false)));
        assertThat(fR1_a.le(fR1_a), is(equalTo(true)));
        assertThat(fR1_a.le(fR1_b), is(equalTo(true)));
        assertThat(fR1_a.le(fR2_a), is(equalTo(true)));
        assertThat(fR1_a.le(fR2_b), is(equalTo(true)));
        assertThat(fR1_a.le(fL_a), is(equalTo(true)));
        assertThat(fR1_a.le(fL_b), is(equalTo(true)));

        assertThat(fR1_b.le(fU_a), is(equalTo(false)));
        assertThat(fR1_b.le(fU_b), is(equalTo(false)));
        assertThat(fR1_b.le(fR1_a), is(equalTo(false)));
        assertThat(fR1_b.le(fR1_b), is(equalTo(true)));
        assertThat(fR1_b.le(fR2_a), is(equalTo(true)));
        assertThat(fR1_b.le(fR2_b), is(equalTo(true)));
        assertThat(fR1_b.le(fL_a), is(equalTo(true)));
        assertThat(fR1_b.le(fL_b), is(equalTo(true)));

        assertThat(fR2_a.le(fU_a), is(equalTo(false)));
        assertThat(fR2_a.le(fU_b), is(equalTo(false)));
        assertThat(fR2_a.le(fR1_a), is(equalTo(false)));
        assertThat(fR2_a.le(fR1_b), is(equalTo(false)));
        assertThat(fR2_a.le(fR2_a), is(equalTo(true)));
        assertThat(fR2_a.le(fR2_b), is(equalTo(true)));
        assertThat(fR2_a.le(fL_a), is(equalTo(true)));
        assertThat(fR2_a.le(fL_b), is(equalTo(true)));

        assertThat(fR2_b.le(fU_a), is(equalTo(false)));
        assertThat(fR2_b.le(fU_b), is(equalTo(false)));
        assertThat(fR2_b.le(fR1_a), is(equalTo(false)));
        assertThat(fR2_b.le(fR1_b), is(equalTo(false)));
        assertThat(fR2_b.le(fR2_a), is(equalTo(false)));
        assertThat(fR2_b.le(fR2_b), is(equalTo(true)));
        assertThat(fR2_b.le(fL_a), is(equalTo(true)));
        assertThat(fR2_b.le(fL_b), is(equalTo(true)));

        assertThat(fL_a.le(fU_a), is(equalTo(false)));
        assertThat(fL_a.le(fU_b), is(equalTo(false)));
        assertThat(fL_a.le(fR1_a), is(equalTo(false)));
        assertThat(fL_a.le(fR1_b), is(equalTo(false)));
        assertThat(fL_a.le(fR2_a), is(equalTo(false)));
        assertThat(fL_a.le(fR2_b), is(equalTo(false)));
        assertThat(fL_a.le(fL_a), is(equalTo(true)));
        assertThat(fL_a.le(fL_b), is(equalTo(true)));

        assertThat(fL_b.le(fU_a), is(equalTo(false)));
        assertThat(fL_b.le(fU_b), is(equalTo(false)));
        assertThat(fL_b.le(fR1_a), is(equalTo(false)));
        assertThat(fL_b.le(fR1_b), is(equalTo(false)));
        assertThat(fL_b.le(fR2_a), is(equalTo(false)));
        assertThat(fL_b.le(fR2_b), is(equalTo(false)));
        assertThat(fL_b.le(fL_a), is(equalTo(false)));
        assertThat(fL_b.le(fL_b), is(equalTo(true)));
    }

    @Test
    public void testPartialOrder() {
        final IRevision u = ChangestructureFactory.createUnknownRevision(PartiallyOrderedRepo.INSTANCE);
        final IRevision r1 = ChangestructureFactory.createRepoRevision(new PartiallyOrderedID("abcd"),
                PartiallyOrderedRepo.INSTANCE);
        final IRevision r2 = ChangestructureFactory.createRepoRevision(new PartiallyOrderedID("efgh"),
                PartiallyOrderedRepo.INSTANCE);
        final IRevision l = ChangestructureFactory.createLocalRevision(PartiallyOrderedWorkingCopy.INSTANCE);

        final IRevisionedFile fU_a = ChangestructureFactory.createFileInRevision("/a", u);
        final IRevisionedFile fU_b = ChangestructureFactory.createFileInRevision("/b", u);
        final IRevisionedFile fR1_a = ChangestructureFactory.createFileInRevision("/a", r1);
        final IRevisionedFile fR1_b = ChangestructureFactory.createFileInRevision("/b", r1);
        final IRevisionedFile fR2_a = ChangestructureFactory.createFileInRevision("/a", r2);
        final IRevisionedFile fR2_b = ChangestructureFactory.createFileInRevision("/b", r2);
        final IRevisionedFile fL_a = ChangestructureFactory.createFileInRevision("/a", l);
        final IRevisionedFile fL_b = ChangestructureFactory.createFileInRevision("/b", l);

        assertThat(fU_a.le(fU_a), is(equalTo(true)));
        assertThat(fU_a.le(fU_b), is(equalTo(true)));
        assertThat(fU_a.le(fR1_a), is(equalTo(true)));
        assertThat(fU_a.le(fR1_b), is(equalTo(true)));
        assertThat(fU_a.le(fR2_a), is(equalTo(true)));
        assertThat(fU_a.le(fR2_b), is(equalTo(true)));
        assertThat(fU_a.le(fL_a), is(equalTo(true)));
        assertThat(fU_a.le(fL_b), is(equalTo(true)));

        assertThat(fU_b.le(fU_a), is(equalTo(false)));
        assertThat(fU_b.le(fU_b), is(equalTo(true)));
        assertThat(fU_b.le(fR1_a), is(equalTo(true)));
        assertThat(fU_b.le(fR1_b), is(equalTo(true)));
        assertThat(fU_b.le(fR2_a), is(equalTo(true)));
        assertThat(fU_b.le(fR2_b), is(equalTo(true)));
        assertThat(fU_b.le(fL_a), is(equalTo(true)));
        assertThat(fU_b.le(fL_b), is(equalTo(true)));

        assertThat(fR1_a.le(fU_a), is(equalTo(false)));
        assertThat(fR1_a.le(fU_b), is(equalTo(false)));
        assertThat(fR1_a.le(fR1_a), is(equalTo(true)));
        assertThat(fR1_a.le(fR1_b), is(equalTo(true)));
        assertThat(fR1_a.le(fR2_a), is(equalTo(false)));
        assertThat(fR1_a.le(fR2_b), is(equalTo(false)));
        assertThat(fR1_a.le(fL_a), is(equalTo(true)));
        assertThat(fR1_a.le(fL_b), is(equalTo(true)));

        assertThat(fR1_b.le(fU_a), is(equalTo(false)));
        assertThat(fR1_b.le(fU_b), is(equalTo(false)));
        assertThat(fR1_b.le(fR1_a), is(equalTo(false)));
        assertThat(fR1_b.le(fR1_b), is(equalTo(true)));
        assertThat(fR1_b.le(fR2_a), is(equalTo(false)));
        assertThat(fR1_b.le(fR2_b), is(equalTo(false)));
        assertThat(fR1_b.le(fL_a), is(equalTo(true)));
        assertThat(fR1_b.le(fL_b), is(equalTo(true)));

        assertThat(fR2_a.le(fU_a), is(equalTo(false)));
        assertThat(fR2_a.le(fU_b), is(equalTo(false)));
        assertThat(fR2_a.le(fR1_a), is(equalTo(false)));
        assertThat(fR2_a.le(fR1_b), is(equalTo(false)));
        assertThat(fR2_a.le(fR2_a), is(equalTo(true)));
        assertThat(fR2_a.le(fR2_b), is(equalTo(true)));
        assertThat(fR2_a.le(fL_a), is(equalTo(true)));
        assertThat(fR2_a.le(fL_b), is(equalTo(true)));

        assertThat(fR2_b.le(fU_a), is(equalTo(false)));
        assertThat(fR2_b.le(fU_b), is(equalTo(false)));
        assertThat(fR2_b.le(fR1_a), is(equalTo(false)));
        assertThat(fR2_b.le(fR1_b), is(equalTo(false)));
        assertThat(fR2_b.le(fR2_a), is(equalTo(false)));
        assertThat(fR2_b.le(fR2_b), is(equalTo(true)));
        assertThat(fR2_b.le(fL_a), is(equalTo(true)));
        assertThat(fR2_b.le(fL_b), is(equalTo(true)));

        assertThat(fL_a.le(fU_a), is(equalTo(false)));
        assertThat(fL_a.le(fU_b), is(equalTo(false)));
        assertThat(fL_a.le(fR1_a), is(equalTo(false)));
        assertThat(fL_a.le(fR1_b), is(equalTo(false)));
        assertThat(fL_a.le(fR2_a), is(equalTo(false)));
        assertThat(fL_a.le(fR2_b), is(equalTo(false)));
        assertThat(fL_a.le(fL_a), is(equalTo(true)));
        assertThat(fL_a.le(fL_b), is(equalTo(true)));

        assertThat(fL_b.le(fU_a), is(equalTo(false)));
        assertThat(fL_b.le(fU_b), is(equalTo(false)));
        assertThat(fL_b.le(fR1_a), is(equalTo(false)));
        assertThat(fL_b.le(fR1_b), is(equalTo(false)));
        assertThat(fL_b.le(fR2_a), is(equalTo(false)));
        assertThat(fL_b.le(fR2_b), is(equalTo(false)));
        assertThat(fL_b.le(fL_a), is(equalTo(false)));
        assertThat(fL_b.le(fL_b), is(equalTo(true)));
    }
}
