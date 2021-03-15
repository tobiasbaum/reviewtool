package de.setsoftware.reviewtool.diffalgorithms;

import de.setsoftware.reviewtool.model.api.IDiffAlgorithm;

/**
 * A factory for diff algorithms.
 * Allows the change sources to be decoupled from the concrete diff algorithm used.
 */
public class DiffAlgorithmFactory {

    /**
     * Creates the default diff algorithm.
     */
    public static IDiffAlgorithm createDefault() {
        return new MyersSourceDiffAlgorithm();
    }

}
