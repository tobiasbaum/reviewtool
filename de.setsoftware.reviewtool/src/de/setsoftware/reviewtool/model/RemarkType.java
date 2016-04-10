package de.setsoftware.reviewtool.model;

/**
 * The type of review remark.
 * It will be used as clue by the author to know if he needs to take action.
 */
public enum RemarkType {
    MUST_FIX,
    CAN_FIX,
    ALREADY_FIXED,
    POSITIVE,
    TEMPORARY,
    OTHER
}
