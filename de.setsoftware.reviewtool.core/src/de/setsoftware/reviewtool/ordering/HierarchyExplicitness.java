package de.setsoftware.reviewtool.ordering;

/**
 * When to create an explicit nesting level for a match in the hierarchic tour tree.
 */
public enum HierarchyExplicitness {

    /**
     * Never create a hierarchical nesting.
     */
    NONE,

    /**
     * Only create a hierarchical nesting if there is more than one child.
     */
    ONLY_NONTRIVIAL,

    /**
     * Always create a hierarchical nesting.
     */
    ALWAYS

}
