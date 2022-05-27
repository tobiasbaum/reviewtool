package de.setsoftware.reviewtool.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.BackgroundJobExecutor;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICortProgressMonitor;
import de.setsoftware.reviewtool.model.remarks.FileLinePosition;
import de.setsoftware.reviewtool.model.remarks.FilePosition;
import de.setsoftware.reviewtool.model.remarks.GlobalPosition;
import de.setsoftware.reviewtool.model.remarks.Position;

/**
 * Is able to transform short filenames into paths and positions with short filenames into positions
 * for Eclipse. Reverse transformation is also supported.
 *
 * <p>In older versions of CoRT, shortening the filename could also mean stripping its extension. This is still
 * supported for convenience and to be able to transform old positions, but will not be generated for new positions.
 */
public class PositionTransformer {

    private static final long STALE_LIMIT_MS = 20L * 1000;
    private static final long REALLY_OLD_LIMIT_MS = 60L * 60 * 1000;

    private static AtomicBoolean refreshRunning = new AtomicBoolean();
    private static volatile ConcurrentHashMap<String, PathChainNode> cache = null;
    private static volatile long cacheRefreshTime;
    private static volatile IChangeSource[] changeSources = new IChangeSource[0];
    private static volatile Supplier<Set<File>> projectPathSupplier;

    private static final ICortProgressMonitor NO_CANCEL_MONITOR = new ICortProgressMonitor() {
        @Override
        public void subTask(String name) {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void done() {
        }

        @Override
        public void beginTask(String name, int totalWork) {
        }
    };

    /**
     * Creates a position identifying the given line in the given resource.
     */
    public static Position toPosition(File path, int line) {
        if (path.isDirectory()) {
            return new GlobalPosition();
        }
        final String filename = stripExtension(path.getName());
        if (filename.isEmpty()) {
            return new GlobalPosition();
        }
        final List<File> possiblePaths = getCachedPathsForName(filename);
        if (possiblePaths == null) {
            return new GlobalPosition();
        }
        if (possiblePaths.size() == 1) {
            return createPos(path.getName(), line);
        }
        return createPos(getShortestUniqueName(path, possiblePaths), line);
    }
    
    private static String getShortestUniqueName(File resourcePath, List<File> possiblePaths) {
        final String[] segments = segments(resourcePath);
        int suffixLength = 1;
        while (suffixLength < segments.length) {
            final int count = countWithSameSuffix(segments, possiblePaths, suffixLength);
            if (count == 1) {
                break;
            }
            suffixLength++;
        }
        return implodePath(segments, suffixLength);
    }

    private static int countWithSameSuffix(String[] resourcePathSegments, List<File> possiblePaths, int suffixLength) {
        int count = 0;
        for (final File path : possiblePaths) {
            if (sameSuffix(resourcePathSegments, segments(path), suffixLength)) {
                count++;
            }
        }
        return count;
    }

    private static boolean sameSuffix(String[] resourcePath, String[] otherPath, int suffixLength) {
        if (otherPath.length < suffixLength) {
            return false;
        }
        for (int i = suffixLength; i >= 1; i--) {
            final String seg1 = resourcePath[resourcePath.length - i];
            final String seg2 = otherPath[otherPath.length - i];
            if (!seg1.equals(seg2)) {
                return false;
            }
        }
        return true;
    }

    private static String implodePath(String[] segments, int suffixLength) {
        final StringBuilder ret = new StringBuilder();
        for (int i = segments.length - suffixLength; i < segments.length; i++) {
            if (i > segments.length - suffixLength) {
                ret.append('/');
            }
            ret.append(segments[i]);
        }
        return ret.toString();
    }

    private static synchronized List<File> getCachedPathsForName(String filename) {
        while (cache == null) {
            //should normally have already been initialized, but it wasn't, so take
            //  the last chance to do so and do it synchronously
            try {
                fillCache(NO_CANCEL_MONITOR);
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
            if (cache == null) {
                //when the cache is still null here, it is currently being initialized. Wait a little
                //  to make the busy waiting a bit less busy.
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        final List<File> cachedPaths = toList(cache.get(filename));
        if ((cachedPaths == null && cacheMightBeStale()) || cacheIsReallyOld()) {
            //Perhaps the resource exists but the cache was stale => trigger refresh
            refreshCacheInBackground();
        }
        return cachedPaths;
    }

    private static List<File> toList(PathChainNode startNode) {
        if (startNode == null) {
            return null;
        }
        final List<File> ret = new ArrayList<>();
        PathChainNode curNode = startNode;
        do {
            ret.add(curNode.path);
            curNode = curNode.next;
        } while (curNode != null);
        return ret;
    }

    private static boolean cacheMightBeStale() {
        return System.currentTimeMillis() - cacheRefreshTime > STALE_LIMIT_MS;
    }

    private static boolean cacheIsReallyOld() {
        return System.currentTimeMillis() - cacheRefreshTime > REALLY_OLD_LIMIT_MS;
    }

    private static void fillCache(ICortProgressMonitor monitor)
            throws InterruptedException {

        if (refreshRunning.compareAndSet(false, true)) {
            final ForkJoinPool pool = new ForkJoinPool((Runtime.getRuntime().availableProcessors() + 1) / 2);
            final List<ForkJoinTask<Void>> tasks = new ArrayList<>();
            final ConcurrentHashMap<String, PathChainNode> newCache = new ConcurrentHashMap<>();
            for (final File path : determineRootPaths()) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
                tasks.add(pool.submit(new FillCacheAction(path, newCache)));
            }
            for (final ForkJoinTask<Void> task : tasks) {
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
                task.join();
            }
            pool.shutdown();
            cache = newCache;
            cacheRefreshTime = System.currentTimeMillis();
            refreshRunning.set(false);
        }
    }

    private static Set<File> determineRootPaths() {
        final Set<File> ret = new LinkedHashSet<>();
        //paths that are not included as a project but part of the scm repo should be included, too
        for (final File projectPath : projectPathSupplier.get()) {
            ret.add(getWorkingCopyRoot(projectPath));
        }
        //with nested projects, the set now contains a parent as well as its children. remove the children
        final Set<File> childProjects = new HashSet<>();
        for (final File cur : ret) {
            if (ret.contains(cur.getParentFile())) {
                childProjects.add(cur);
            }
        }
        ret.removeAll(childProjects);
        return ret;
    }

    private static File getWorkingCopyRoot(File dir) {
        final IChangeSource[] cs = changeSources;
        for (final IChangeSource c : cs) {
            try {
                final File root = c.determineWorkingCopyRoot(dir);
                if (root != null) {
                    return root;
                }
            } catch (final ChangeSourceException e) {
                Logger.warn("exception from changesource", e);
            }
        }
        return dir;
    }

    private static void fillCacheIfEmpty(ICortProgressMonitor monitor)
            throws InterruptedException {

        if (cache != null) {
            return;
        }
        fillCache(monitor);
    }

    /**
     * Starts a new job that initializes the cache in the background.
     * If the cache has already been initialized, it does nothing.
     */
    public static void initializeCacheInBackground() {
        BackgroundJobExecutor.execute("Review resource cache init",
                (ICortProgressMonitor monitor) -> {
                    try {
                        fillCacheIfEmpty(monitor);
                        return null;
                    } catch (InterruptedException e) {
                        return e;
                    }
                });
    }

    /**
     * Starts a new job that refreshes the cache in the background.
     */
    public static void refreshCacheInBackground() {
        BackgroundJobExecutor.execute("Review resource cache refresh",
                (ICortProgressMonitor monitor) -> {
                    try {
                        fillCache(monitor);
                        return null;
                    } catch (final InterruptedException e) {
                        return e;
                    }
                });
    }

    /**
     * A linked list node in the cache.
     */
    private static final class PathChainNode {
        private final PathChainNode next;
        private final File path;

        public PathChainNode(File path2, PathChainNode oldNode) {
            this.next = oldNode;
            this.path = path2;
        }

    }

    /**
     * Fork-join action to fill the cache. Every action is responsible for a single directory.
     * Does not really join/merge maps to avoid unnecessary copying, uses a ConcurrentHashMap instead.
     */
    private static final class FillCacheAction extends RecursiveAction {

        private static final long serialVersionUID = -6349559047252339690L;

        private final File path;
        private final ConcurrentHashMap<String, PathChainNode> sharedMap;

        public FillCacheAction(File directory, ConcurrentHashMap<String, PathChainNode> sharedMap) {
            this.path = directory;
            this.sharedMap = sharedMap;
        }

        @Override
        protected void compute() {
            final File[] children = this.path.listFiles();
            if (children == null) {
                return;
            }
            final List<FillCacheAction> subActions = new ArrayList<>();
            for (final File child : children) {
                final String childName = child.getName();
                if (childName.startsWith(".") || childName.equals("bin")) {
                    continue;
                }
                if (child.isDirectory()) {
                    subActions.add(new FillCacheAction(new File(this.path, childName), this.sharedMap));
                } else {
                    final String childNameWithoutExtension = stripExtension(childName);
                    this.addToMap(childNameWithoutExtension, new File(this.path, childName));
                }
            }
            invokeAll(subActions);
        }

        private void addToMap(String childNameWithoutExtension, File path) {
            boolean success;
            do {
                final PathChainNode oldNode = this.sharedMap.get(childNameWithoutExtension);
                final PathChainNode newNode = new PathChainNode(path, oldNode);
                if (oldNode == null) {
                    success = this.sharedMap.putIfAbsent(childNameWithoutExtension, newNode) == null;
                } else {
                    success = this.sharedMap.replace(childNameWithoutExtension, oldNode, newNode);
                }
            } while (!success);
        }

    }

    private static String stripExtension(String name) {
        final int dotIndex = name.lastIndexOf('.');
        if (dotIndex >= 0) {
            return name.substring(0, dotIndex);
        } else {
            return name;
        }
    }

    private static Position createPos(String shortName, int line) {
        if (line > 0) {
            return new FileLinePosition(shortName, line);
        } else {
            return new FilePosition(shortName);
        }
    }

    /**
     * Returns the resource for the file from the given position.
     * Returns null if no file could be found.
     */
    public static File toPath(Position pos) {
        return toPath(pos.getShortFileName());
    }

    /**
     * Returns the path for the given short filename.
     * Returns null if no file could be found.
     */
    public static File toPath(String filename) {
        if (filename == null) {
            return null;
        }
        final String[] segments = filename.split("/");
        final String filenameWithoutExtension = stripExtension(segments[segments.length - 1]);
        final List<File> paths = getCachedPathsForName(filenameWithoutExtension);
        if (paths == null) {
            return null;
        }
        if (segments.length == 1 && paths.size() == 1) {
            return paths.get(0);
        }
        final File fittingPath = findFittingPath(segments, paths);
        if (fittingPath == null) {
            return null;
        }
        return fittingPath;
    }

    private static File findFittingPath(String[] segments, List<File> paths) {
        File result = null;
        for (final File path : paths) {
            final String[] pathSegments = segments(path);
            if (sameSuffix(segments, pathSegments, segments.length)
                    && (result == null || pathSegments.length < segments(result).length)) {
                result = path;
            }
        }
        return result;
    }
    
    private static String[] segments(File path) {
        java.nio.file.Path p = path.toPath();
        String[] ret = new String[p.getNameCount()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = p.getName(i).toString();
        }
        return ret;
    }

    public static void setProjectSource(Supplier<Set<File>> projectPathSupplier2) {
        projectPathSupplier = projectPathSupplier2;
    }

    public static void setChangeSources(List<IChangeSource> list) {
        changeSources = list.toArray(new IChangeSource[list.size()]);
    }

}
