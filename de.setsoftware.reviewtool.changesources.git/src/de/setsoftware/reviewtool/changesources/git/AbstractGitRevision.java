package de.setsoftware.reviewtool.changesources.git;

///**
// * Common behaviour for {@link GitRevision} implementations.
// */
//abstract class AbstractGitRevision implements GitRevision {
//
//    /**
//     * Filters out all changed paths that do not belong to passed working copy.
//     *
//     * @param paths The paths to filter.
//     * @param wc The working copy.
//     * @return A map of path entries that belong to passed working copy.
//     */
//    protected final SortedMap<String, CachedLogEntryPath> filterPaths(final GitWorkingCopy wc) {
//
//        final SortedMap<String, CachedLogEntryPath> result = new TreeMap<>();
//        final Iterator<Map.Entry<String, CachedLogEntryPath>> it = this.getChangedPaths().entrySet().iterator();
//
//        while (it.hasNext()) {
//            final Map.Entry<String, CachedLogEntryPath> entry = it.next();
//            if (wc.toAbsolutePathInWc(entry.getKey()) != null) {
//                result.put(entry.getKey(), entry.getValue());
//            }
//        }
//
//        return result;
//    }
//}
