package de.setsoftware.reviewtool.model;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IMarker;
import org.junit.Test;

public class RemarkSaveTest {

	//	Review 1 (Std.rev 12345):
	//	* wichtig
	//	*# (Testklasse, 1234) das ist bl�d
	//	*#* stimmt gar nicht
	//	*#* stimmt doch
	//	*# das ist auch bl�d
	//	* weniger wichtig
	//	** asdf jkl�
	//
	//	Review 2:
	//	- hier wurde was vergessen

	private static IMarker newMarker() {
		return new DummyMarker("marker.type.id");
	}

	private static GlobalPosition global() {
		return new GlobalPosition();
	}

	@Test
	public void testSaveNewRemarkInEmptyReviewData() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r =
				ReviewRemark.create(p, newMarker(), "TB", global(), "globale Review-Anmerkung", RemarkType.MUST_FIX);
		r.save();

		assertEquals(
				"Review 1:\n"
						+ "* wichtig\n"
						+ "*# globale Review-Anmerkung\n",
						p.getCurrentReviewData());
	}

	@Test
	public void testWithMultilineRemark() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r =
				ReviewRemark.create(p, newMarker(), "TB", global(), "globale\nReview-Anmerkung", RemarkType.MUST_FIX);
		r.save();

		assertEquals(
				"Review 1:\n"
						+ "* wichtig\n"
						+ "*# globale\n"
						+ "Review-Anmerkung\n",
						p.getCurrentReviewData());
	}

	@Test
	public void testSaveTwoRemarksInEmptyReviewData() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 =
				ReviewRemark.create(p, newMarker(), "TB", global(), "globale Review-Anmerkung", RemarkType.MUST_FIX);
		r1.save();
		final ReviewRemark r2 =
				ReviewRemark.create(p, newMarker(), "TB", global(), "zweite Anmerkung", RemarkType.MUST_FIX);
		r2.save();

		assertEquals(
				"Review 1:\n"
						+ "* wichtig\n"
						+ "*# globale Review-Anmerkung\n"
						+ "*# zweite Anmerkung\n",
						p.getCurrentReviewData());
	}

	@Test
	public void testSaveRemarksWithDifferentTypes() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm A", RemarkType.CAN_FIX);
		r1.save();
		final ReviewRemark r2 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm B", RemarkType.ALREADY_FIXED);
		r2.save();
		final ReviewRemark r3 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm C", RemarkType.CAN_FIX);
		r3.save();
		final ReviewRemark r4 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm D", RemarkType.POSITIVE);
		r4.save();
		final ReviewRemark r5 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm E", RemarkType.MUST_FIX);
		r5.save();
		final ReviewRemark r6 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm F", RemarkType.CAN_FIX);
		r6.save();
		final ReviewRemark r7 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm G", RemarkType.MUST_FIX);
		r7.save();

		assertEquals(
				"Review 1:\n"
						+ "* positiv\n"
						+ "*# Anm D\n"
						+ "* wichtig\n"
						+ "*# Anm E\n"
						+ "*# Anm G\n"
						+ "* optional / weniger wichtig\n"
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
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm A", RemarkType.MUST_FIX);
		r1.save();
		final ReviewRemark r2 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm B", RemarkType.MUST_FIX);
		r2.save();

		stubPersistence.setReviewRound(2);

		final ReviewRemark r3 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm C", RemarkType.MUST_FIX);
		r3.save();
		final ReviewRemark r4 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm D", RemarkType.MUST_FIX);
		r4.save();

		assertEquals(
				"Review 2:\n"
						+ "* wichtig\n"
						+ "*# Anm C\n"
						+ "*# Anm D\n"
						+ "\n"
						+ "Review 1:\n"
						+ "* wichtig\n"
						+ "*# Anm A\n"
						+ "*# Anm B\n",
						p.getCurrentReviewData());
	}

	@Test
	public void testWithComments() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "stimmt nicht", RemarkType.MUST_FIX);
		r1.addComment("XY", "stimmt doch");
		r1.addComment("VW", "gar nicht");
		r1.save();

		assertEquals("Review 1:\n"
				+ "* wichtig\n"
				+ "*# stimmt nicht\n"
				+ "*#* XY: stimmt doch\n"
				+ "*#* VW: gar nicht\n",
				p.getCurrentReviewData());
	}

	@Test
	public void testWithCommentsAndMultipleSaves() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "stimmt nicht", RemarkType.MUST_FIX);
		r1.save();
		r1.addComment("XY", "stimmt doch");
		r1.save();
		r1.addComment("VW", "gar nicht");
		r1.save();

		assertEquals("Review 1:\n"
				+ "* wichtig\n"
				+ "*# stimmt nicht\n"
				+ "*#* XY: stimmt doch\n"
				+ "*#* VW: gar nicht\n",
				p.getCurrentReviewData());
	}

	@Test
	public void testResolutionsAtRemark() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "asdf", RemarkType.MUST_FIX);
		r1.setResolution(ResolutionType.FIXED);
		r1.save();
		final ReviewRemark r2 = ReviewRemark.create(p, newMarker(), "TB", global(), "jkl�", RemarkType.MUST_FIX);
		r2.setResolution(ResolutionType.WONT_FIX);
		r2.save();
		final ReviewRemark r3 = ReviewRemark.create(p, newMarker(), "TB", global(), "qwer", RemarkType.MUST_FIX);
		r3.setResolution(ResolutionType.QUESTION);
		r3.save();

		assertEquals("Review 1:\n"
				+ "* wichtig\n"
				+ "*# asdf (/)\n"
				+ "*# jkl� (x)\n"
				+ "*# qwer (?)\n",
				p.getCurrentReviewData());
	}

	@Test
	public void testResolutionsAtComment() throws Exception {
		final ReviewStateManager p = createPersistence();
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "asdf", RemarkType.MUST_FIX);
		r1.addComment("NN", "blabla");
		r1.setResolution(ResolutionType.FIXED);
		r1.save();
		final ReviewRemark r2 = ReviewRemark.create(p, newMarker(), "TB", global(), "jkl�", RemarkType.MUST_FIX);
		r2.addComment("NN", "blablub");
		r2.setResolution(ResolutionType.WONT_FIX);
		r2.save();
		final ReviewRemark r3 = ReviewRemark.create(p, newMarker(), "TB", global(), "qwer", RemarkType.MUST_FIX);
		r3.addComment("NN", "blablo");
		r3.setResolution(ResolutionType.QUESTION);
		r3.save();

		assertEquals("Review 1:\n"
				+ "* wichtig\n"
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
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", new FilePosition("xyz.java"), "asdf", RemarkType.MUST_FIX);
		r1.addComment("NN", "blabla");
		r1.save();
		r1.addComment("MM", "blablub");
		r1.save();

		assertEquals("Review 1:\n"
				+ "* wichtig\n"
				+ "*# (xyz.java) asdf\n"
				+ "*#* NN: blabla\n"
				+ "*#* MM: blablub\n",
				p.getCurrentReviewData());
	}

	private static ReviewStateManager createPersistence() {
		return new ReviewStateManager(new PersistenceStub(), stubTicketChooser());
	}

	private static ITicketChooser stubTicketChooser() {
		return new ITicketChooser() {
			@Override
			public String choose(IReviewPersistence persistence, String ticketKeyDefault, boolean forReview) {
				return "TEST-1234";
			}
		};
	}

	@Test
	public void testDeleteRemark() throws Exception {
		final ReviewStateManager p = createPersistence();
		p.saveCurrentReviewData("Review 2:\n"
				+ "* wichtig\n"
				+ "*# Anm C\n"
				+ "*# Anm D\n"
				+ "\n"
				+ "Review 1:\n"
				+ "* wichtig\n"
				+ "*# Anm A\n"
				+ "*# (DateiX) Anm A\n"
				+ "*# Anm B\n");
		final ReviewRemark r1 = ReviewRemark.create(p, newMarker(), "TB", global(), "Anm A", RemarkType.MUST_FIX);
		r1.delete();

		assertEquals("Review 2:\n"
				+ "* wichtig\n"
				+ "*# Anm C\n"
				+ "*# Anm D\n"
				+ "\n"
				+ "Review 1:\n"
				+ "* wichtig\n"
				+ "*# (DateiX) Anm A\n"
				+ "*# Anm B\n",
				p.getCurrentReviewData());
	}
}
