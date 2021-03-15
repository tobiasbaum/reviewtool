package de.setsoftware.reviewtool.diffalgorithms;


/**
 * An implementation of <a href="http://www.cs.arizona.edu/people/gene/">
 * Eugene Myers</a> differencing algorithm, based on the source by Juanco Anez.
 */
public class MyersDiff {

    /**
     * Computes the minimum diffpath that expresses the differences
     * between the original and revised sequences, according
     * to Gene Myers differencing algorithm.
     *
     * @param orig The original sequence.
     * @param rev The revised sequence.
     * @return A minimum {@link PathNode Path} across the differences graph.
     * @throws DifferentiationFailedException if a diff path could not be found.
     */
    public PathNode buildPath(final OneFileView<String> orig, final OneFileView<String> rev) {
        final int commonSuffixLength = this.determineCommonSuffixLength(orig, rev);
        final int n = orig.getItemCount() - commonSuffixLength;
        final int m = rev.getItemCount() - commonSuffixLength;

        final int max = n + m + 1;
        final int size = 1 + 2 * max;
        final int middle = size / 2;
        final PathNode[] diagonal = new PathNode[size];

        diagonal[middle + 1] = new Snake(0, -1, null);
        for (int d = 0; d < max; d++) {
            for (int k = -d; k <= d; k += 2) {
                final int kmiddle = middle + k;
                final int kplus = kmiddle + 1;
                final int kminus = kmiddle - 1;
                PathNode prev = null;

                int i;
                if ((k == -d) || (k != d && diagonal[kminus].getPosOld() < diagonal[kplus].getPosOld())) {
                    i = diagonal[kplus].getPosOld();
                    prev = diagonal[kplus];
                } else {
                    i = diagonal[kminus].getPosOld() + 1;
                    prev = diagonal[kminus];
                }

                diagonal[kminus] = null; // no longer used

                int j = i - k;

                PathNode node = new DiffNode(i, j, prev);

                // orig and rev are zero-based
                // but the algorithm is one-based
                // that's why there's no +1 when indexing the sequences
                while (i < n && j < m && orig.getItem(i).equals(rev.getItem(j))) {
                    i++;
                    j++;
                }
                if (i > node.getPosOld()) {
                    node = new Snake(i, j, node);
                }

                diagonal[kmiddle] = node;

                if (i >= n && j >= m) {
                    return this.addCommonSuffixSnake(diagonal[kmiddle], commonSuffixLength);
                }
            }
            diagonal[middle + d - 1] = null;

        }
        // According to Myers, this cannot happen
        throw new Error("could not find a diff path");
    }

    private PathNode addCommonSuffixSnake(PathNode pathNode, int commonSuffixLength) {
        if (pathNode.isSnake()) {
            pathNode.enlargeBy(commonSuffixLength);
            return pathNode;
        } else {
            return new Snake(
                    pathNode.getPosOld() + commonSuffixLength,
                    pathNode.getPosNew() + commonSuffixLength,
                    pathNode);
        }
    }

    private int determineCommonSuffixLength(OneFileView<String> oldFile, OneFileView<String> newFile) {
        int suffixLength = 0;
        final int max = Math.min(oldFile.getItemCount(), newFile.getItemCount());
        while (suffixLength < max) {
            if (!oldFile.getItemFromEnd(suffixLength).equals(newFile.getItemFromEnd(suffixLength))) {
                return suffixLength;
            }
            suffixLength++;
        }
        return suffixLength;
    }

}
