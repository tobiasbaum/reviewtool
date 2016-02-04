package de.setsoftware.reviewtool.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewRound {

	public static final String POSITIVE_HEADER = "positiv";
	public static final String MUST_FIX_HEADER = "wichtig";
	public static final String CAN_FIX_HEADER = "optional / weniger wichtig";
	public static final String ALREADY_FIXED_HEADER = "direkt eingepflegt";
	public static final String TEMPORARY_HEADER = "temporärer Marker";

	private final int nbr;
	private final List<ReviewRemark> remarks = new ArrayList<>();

	public ReviewRound(int nbr) {
		this.nbr = nbr;
	}

	public int getNumber() {
		return this.nbr;
	}

	public void merge(ReviewRemark reviewRemark) {
		final int index = this.findSimilar(reviewRemark);
		if (index >= 0) {
			this.remarks.set(index, reviewRemark);
		} else {
			this.remarks.add(reviewRemark);
		}
	}

	private int findSimilar(ReviewRemark reviewRemark) {
		int i = 0;
		for (final ReviewRemark r : this.remarks) {
			if (r.hasSameTextAndPositionAs(reviewRemark)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public void add(ReviewRemark reviewRemark) {
		this.remarks.add(reviewRemark);
	}

	public boolean isEmpty() {
		return this.remarks.isEmpty();
	}

	public String serialize() {
		final StringBuilder ret = new StringBuilder();
		ret.append("Review ").append(this.nbr).append(":\n");
		this.serializeRemarksWithType(POSITIVE_HEADER, ret, RemarkType.POSITIVE);
		this.serializeRemarksWithType(MUST_FIX_HEADER, ret, RemarkType.MUST_FIX);
		this.serializeRemarksWithType(CAN_FIX_HEADER, ret, RemarkType.CAN_FIX);
		this.serializeRemarksWithType(ALREADY_FIXED_HEADER, ret, RemarkType.ALREADY_FIXED);
		this.serializeRemarksWithType(TEMPORARY_HEADER, ret, RemarkType.TEMPORARY);
		return ret.toString();
	}

	private void serializeRemarksWithType(String title, StringBuilder ret, RemarkType type) {
		boolean titleWritten = false;
		for (final ReviewRemark remark : this.remarks) {
			if (remark.getRemarkType() == type) {
				if (!titleWritten) {
					ret.append("* ").append(title).append("\n");
					titleWritten = true;
				}
				ret.append(remark.serialize());
			}
		}
	}

	public static RemarkType parseType(String string) {
		switch (string) {
		case ALREADY_FIXED_HEADER:
			return RemarkType.ALREADY_FIXED;
		case CAN_FIX_HEADER:
			return RemarkType.CAN_FIX;
		case MUST_FIX_HEADER:
			return RemarkType.MUST_FIX;
		case POSITIVE_HEADER:
			return RemarkType.POSITIVE;
		case TEMPORARY_HEADER:
			return RemarkType.TEMPORARY;
		default:
			throw new RuntimeException("parse exception: " + string);
		}
	}

	public boolean hasUnresolvedRemarks() {
		for (final ReviewRemark r : this.remarks) {
			if (r.isOpen()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasTemporaryMarkers() {
		for (final ReviewRemark r : this.remarks) {
			if (r.getRemarkType() == RemarkType.TEMPORARY) {
				return true;
			}
		}
		return false;
	}

	public void deleteRemark(ReviewRemark reviewRemark) {
		final int i = this.findSimilar(reviewRemark);
		if (i >= 0) {
			this.remarks.remove(i);
		}
	}

}
