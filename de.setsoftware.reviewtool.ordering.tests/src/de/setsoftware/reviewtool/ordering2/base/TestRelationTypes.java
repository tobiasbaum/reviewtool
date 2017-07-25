package de.setsoftware.reviewtool.ordering2.base;

import de.setsoftware.reviewtool.ordering2.base.RelationType;

public enum TestRelationTypes implements RelationType {
    SAME_METHOD,
    CALL_FLOW,
    DATA_FLOW,
    DECLARATION_USE,
    SIMILARITY,
    SAME_FILE,
    LOGICAL_DEPENDENCY,
    OVERRIDES,
    CLASS_REFERENCE,
    GLOBAL_ORDER

}