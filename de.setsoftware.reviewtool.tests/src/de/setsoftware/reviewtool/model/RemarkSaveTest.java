package de.setsoftware.reviewtool.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.FilePosition;
import de.setsoftware.reviewtool.model.remarks.GlobalPosition;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.RemarkType;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;

/**
 * Tests for saving/serialising review remarks.
 */
public class RemarkSaveTest {

    //  Review 1 (Std.rev 12345):
    //  * muss
    //  *# (Testklasse, 1234) das ist bl�d
    //  *#* stimmt gar nicht
    //  *#* stimmt doch
    //  *# das ist auch bl�d
    //  * weniger wichtig
    //  ** asdf jkl�
    //
    //  Review 2:
    //  - hier wurde was vergessen

    private static IReviewMarker newMarker() {
        return new DummyMarker();
    }

    private static GlobalPosition global() {
        return new GlobalPosition();
    }

    @Test
    public void testSaveNewRemarkInEmptyReviewData() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r =
                ReviewRemark.create(newMarker(), "TB", global(), "globale Review-Anmerkung", RemarkType.MUST_FIX);
        p.saveRemark(r);

        assertEquals(
                "Review 1:\n"
                        + "* muss\n"
                        + "*# globale Review-Anmerkung\n",
                        p.getCurrentReviewData());
    }

    @Test
    public void testWithMultilineRemark() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r =
                ReviewRemark.create(newMarker(), "TB", global(), "globale\nReview-Anmerkung", RemarkType.MUST_FIX);
        p.saveRemark(r);

        assertEquals(
                "Review 1:\n"
                        + "* muss\n"
                        + "*# globale\n"
                        + "Review-Anmerkung\n",
                        p.getCurrentReviewData());
    }

    @Test
    public void testSaveTwoRemarksInEmptyReviewData() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 =
                ReviewRemark.create(newMarker(), "TB", global(), "globale Review-Anmerkung", RemarkType.MUST_FIX);
        p.saveRemark(r1);
        final ReviewRemark r2 =
                ReviewRemark.create(newMarker(), "TB", global(), "zweite Anmerkung", RemarkType.MUST_FIX);
        p.saveRemark(r2);

        assertEquals(
                "Review 1:\n"
                        + "* muss\n"
                        + "*# globale Review-Anmerkung\n"
                        + "*# zweite Anmerkung\n",
                        p.getCurrentReviewData());
    }

    @Test
    public void testSaveRemarksWithDifferentTypes() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "Anm A", RemarkType.CAN_FIX);
        p.saveRemark(r1);
        final ReviewRemark r2 = ReviewRemark.create(newMarker(), "TB", global(), "Anm B", RemarkType.ALREADY_FIXED);
        p.saveRemark(r2);
        final ReviewRemark r3 = ReviewRemark.create(newMarker(), "TB", global(), "Anm C", RemarkType.CAN_FIX);
        p.saveRemark(r3);
        final ReviewRemark r4 = ReviewRemark.create(newMarker(), "TB", global(), "Anm D", RemarkType.POSITIVE);
        p.saveRemark(r4);
        final ReviewRemark r5 = ReviewRemark.create(newMarker(), "TB", global(), "Anm E", RemarkType.MUST_FIX);
        p.saveRemark(r5);
        final ReviewRemark r6 = ReviewRemark.create(newMarker(), "TB", global(), "Anm F", RemarkType.CAN_FIX);
        p.saveRemark(r6);
        final ReviewRemark r7 = ReviewRemark.create(newMarker(), "TB", global(), "Anm G", RemarkType.MUST_FIX);
        p.saveRemark(r7);

        assertEquals(
                "Review 1:\n"
                        + "* positiv\n"
                        + "*# Anm D\n"
                        + "* muss\n"
                        + "*# Anm E\n"
                        + "*# Anm G\n"
                        + "* kann\n"
                        + "*# Anm A\n"
                        + "*# Anm C\n"
                        + "*# Anm F\n"
                        + "* direkt eingepflegt\n"
                        + "*# Anm B (/)\n",
                        p.getCurrentReviewData());
    }

    @Test
    public void testWithDifferentReviewRounds() throws Exception {
        final PersistenceStub stubPersistence = new PersistenceStub();
        final ReviewStateManager p = new ReviewStateManager(stubPersistence, stubTicketChooser());
        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "Anm A", RemarkType.MUST_FIX);
        p.saveRemark(r1);
        final ReviewRemark r2 = ReviewRemark.create(newMarker(), "TB", global(), "Anm B", RemarkType.MUST_FIX);
        p.saveRemark(r2);

        stubPersistence.setReviewRound(2);

        final ReviewRemark r3 = ReviewRemark.create(newMarker(), "TB", global(), "Anm C", RemarkType.MUST_FIX);
        p.saveRemark(r3);
        final ReviewRemark r4 = ReviewRemark.create(newMarker(), "TB", global(), "Anm D", RemarkType.MUST_FIX);
        p.saveRemark(r4);

        assertEquals(
                "Review 2:\n"
                        + "* muss\n"
                        + "*# Anm C\n"
                        + "*# Anm D\n"
                        + "\n"
                        + "Review 1:\n"
                        + "* muss\n"
                        + "*# Anm A\n"
                        + "*# Anm B\n",
                        p.getCurrentReviewData());
    }

    @Test
    public void testWithComments() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 =
                ReviewRemark.create(newMarker(), "TB", global(), "stimmt nicht", RemarkType.MUST_FIX);
        r1.addComment("XY", "stimmt doch");
        r1.addComment("VW", "gar nicht");
        p.saveRemark(r1);

        assertEquals("Review 1:\n"
                + "* muss\n"
                + "*# stimmt nicht\n"
                + "*#* XY: stimmt doch\n"
                + "*#* VW: gar nicht\n",
                p.getCurrentReviewData());
    }

    @Test
    public void testWithCommentsAndMultipleSaves() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 =
                ReviewRemark.create(newMarker(), "TB", global(), "stimmt nicht", RemarkType.MUST_FIX);
        p.saveRemark(r1);
        r1.addComment("XY", "stimmt doch");
        p.saveRemark(r1);
        r1.addComment("VW", "gar nicht");
        p.saveRemark(r1);

        assertEquals("Review 1:\n"
                + "* muss\n"
                + "*# stimmt nicht\n"
                + "*#* XY: stimmt doch\n"
                + "*#* VW: gar nicht\n",
                p.getCurrentReviewData());
    }

    @Test
    public void testResolutionsAtRemark() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "asdf", RemarkType.MUST_FIX);
        r1.setResolution(ResolutionType.FIXED);
        p.saveRemark(r1);
        final ReviewRemark r2 = ReviewRemark.create(newMarker(), "TB", global(), "jkl�", RemarkType.MUST_FIX);
        r2.setResolution(ResolutionType.WONT_FIX);
        p.saveRemark(r2);
        final ReviewRemark r3 = ReviewRemark.create(newMarker(), "TB", global(), "qwer", RemarkType.MUST_FIX);
        r3.setResolution(ResolutionType.QUESTION);
        p.saveRemark(r3);

        assertEquals("Review 1:\n"
                + "* muss\n"
                + "*# asdf (/)\n"
                + "*# jkl� (x)\n"
                + "*# qwer (?)\n",
                p.getCurrentReviewData());
    }

    @Test
    public void testResolutionsAtComment() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "asdf", RemarkType.MUST_FIX);
        r1.addComment("NN", "blabla");
        r1.setResolution(ResolutionType.FIXED);
        p.saveRemark(r1);
        final ReviewRemark r2 = ReviewRemark.create(newMarker(), "TB", global(), "jkl�", RemarkType.MUST_FIX);
        r2.addComment("NN", "blablub");
        r2.setResolution(ResolutionType.WONT_FIX);
        p.saveRemark(r2);
        final ReviewRemark r3 = ReviewRemark.create(newMarker(), "TB", global(), "qwer", RemarkType.MUST_FIX);
        r3.addComment("NN", "blablo");
        r3.setResolution(ResolutionType.QUESTION);
        p.saveRemark(r3);

        assertEquals("Review 1:\n"
                + "* muss\n"
                + "*# asdf\n"
                + "*#* (/) NN: blabla\n"
                + "*# jkl�\n"
                + "*#* (x) NN: blablub\n"
                + "*# qwer\n"
                + "*#* (?) NN: blablo\n",
                p.getCurrentReviewData());
    }


    @Test
    public void testWithFilePosition() throws Exception {
        final ReviewStateManager p = createPersistence();
        final ReviewRemark r1 =
                ReviewRemark.create(newMarker(), "TB", new FilePosition("xyz.java"), "asdf", RemarkType.MUST_FIX);
        r1.addComment("NN", "blabla");
        p.saveRemark(r1);
        r1.addComment("MM", "blablub");
        p.saveRemark(r1);

        assertEquals("Review 1:\n"
                + "* muss\n"
                + "*# (xyz.java) asdf\n"
                + "*#* NN: blabla\n"
                + "*#* MM: blablub\n",
                p.getCurrentReviewData());
    }

    private static ReviewStateManager createPersistence() {
        return new ReviewStateManager(new PersistenceStub(), stubTicketChooser());
    }

    private static IUserInteraction stubTicketChooser() {
        return new StubUi("TEST-1234");
    }

    @Test
    public void testDeleteRemark() throws Exception {
        final ReviewStateManager p = createPersistence();
        p.saveCurrentReviewData("Review 2:\n"
                + "* muss\n"
                + "*# Anm C\n"
                + "*# Anm D\n"
                + "\n"
                + "Review 1:\n"
                + "* muss\n"
                + "*# Anm A\n"
                + "*# (DateiX) Anm A\n"
                + "*# Anm B\n");
        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "Anm A", RemarkType.MUST_FIX);
        p.deleteRemark(r1);

        assertEquals("Review 2:\n"
                + "* muss\n"
                + "*# Anm C\n"
                + "*# Anm D\n"
                + "\n"
                + "Review 1:\n"
                + "* muss\n"
                + "*# (DateiX) Anm A\n"
                + "*# Anm B\n",
                p.getCurrentReviewData());
    }

    @Test
    public void testAddComment() throws Exception {
        final ReviewStateManager p = createPersistence();
        p.saveCurrentReviewData("Review 2:\n"
                + "* muss\n"
                + "*# Anm C\n"
                + "*# Anm D\n"
                + "\n"
                + "Review 1:\n"
                + "* muss\n"
                + "*# Anm A\n"
                + "*# Anm B\n");

        final ReviewRemark r1 = ReviewRemark.create(newMarker(), "TB", global(), "Anm A", RemarkType.MUST_FIX);
        r1.addComment("AA", "ein Kommentar fuer A");
        r1.setResolution(ResolutionType.QUESTION);
        p.saveRemark(r1);

        final ReviewRemark r2 = ReviewRemark.create(newMarker(), "XX", global(), "Anm D", RemarkType.MUST_FIX);
        r2.addComment("DD", "ein Kommentar fuer D");
        r2.setResolution(ResolutionType.QUESTION);
        p.saveRemark(r2);

        assertEquals("Review 2:\n"
                + "* muss\n"
                + "*# Anm C\n"
                + "*# Anm D\n"
                + "*#* (?) DD: ein Kommentar fuer D\n"
                + "\n"
                + "Review 1:\n"
                + "* muss\n"
                + "*# Anm A\n"
                + "*#* (?) AA: ein Kommentar fuer A\n"
                + "*# Anm B\n",
                p.getCurrentReviewData());
    }

}
