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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICortPath;
import de.setsoftware.reviewtool.model.api.ICortResource;
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
    private static Supplier<ICortWorkspace> workspaceSupplier;
    private static Consumer<ICortWorkspace> refreshAction;
    
    public static void init(Supplier<ICortWorkspace> ws, Consumer<ICortWorkspace> ra) {
        workspaceSupplier = ws;
        refreshAction = ra;
    }

    /**
     * Creates a position identifying the given line in the given resource.
     */
    public static Position toPosition(ICortPath path, int line, ICortWorkspace workspace) {
        if (path.toFile().isDirectory()) {
            return new GlobalPosition();
        }
        final String filename = stripExtension(path.lastSegment());
        if (filename.isEmpty()) {
            return new GlobalPosition();
        }
        final List<ICortPath> possiblePaths = getCachedPathsForName(workspace, filename);
        if (possiblePaths == null) {
            return new GlobalPosition();
        }
        if (possiblePaths.size() == 1) {
            return createPos(path.lastSegment(), line);
        }
        return createPos(getShortestUniqueName(path, possiblePaths), line);
    }

    private static String getShortestUniqueName(ICortPath resourcePath, List<ICortPath> possiblePaths) {
        final String[] segments = resourcePath.segments();
        int suffixLength = 1;
        while (suffixLength < segments.length) {
            final int count = countWithSameSuffix(resourcePath, possiblePaths, suffixLength);
            if (count == 1) {
                break;
            }
            suffixLength++;
        }
        return implodePath(segments, suffixLength);
    }

    private static int countWithSameSuffix(ICortPath resourcePath, List<ICortPath> possiblePaths, int suffixLength) {
        int count = 0;
        for (final ICortPath path : possiblePaths) {
            if (sameSuffix(resourcePath.segments(), path.segments(), suffixLength)) {
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

    private static synchronized List<ICortPath> getCachedPathsForName(ICortWorkspace workspace, String filename) {
        while (cache == null) {
            //should normally have already been initialized, but it wasn't, so take
            //  the last chance to do so and do it synchronously
            try {
                fillCache(workspace, () -> false);
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
        final List<ICortPath> cachedPaths = toList(cache.get(filename));
        if ((cachedPaths == null && cacheMightBeStale()) || cacheIsReallyOld()) {
            //Perhaps the resource exists but the cache was stale => trigger refresh
            refreshAction.accept(workspace);
        }
        return cachedPaths;
    }

    private static List<ICortPath> toList(PathChainNode startNode) {
        if (startNode == null) {
            return null;
        }
        final List<ICortPath> ret = new ArrayList<>();
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

    public static void fillCache(ICortWorkspace workspace, BooleanSupplier isCanceled)
            throws InterruptedException {

        if (refreshRunning.compareAndSet(false, true)) {
            final ForkJoinPool pool = new ForkJoinPool((Runtime.getRuntime().availableProcessors() + 1) / 2);
            final List<ForkJoinTask<Void>> tasks = new ArrayList<>();
            final ConcurrentHashMap<String, PathChainNode> newCache = new ConcurrentHashMap<>();
            for (final ICortPath path : determineRootPaths(workspace.getProjectPaths())) {
                if (isCanceled.getAsBoolean()) {
                    throw new InterruptedException();
                }
                tasks.add(pool.submit(new FillCacheAction(path, newCache)));
            }
            for (final ForkJoinTask<Void> task : tasks) {
                if (isCanceled.getAsBoolean()) {
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

    private static Set<ICortPath> determineRootPaths(List<? extends ICortPath> projects) {
        final Set<ICortPath> ret = new LinkedHashSet<>();
        //paths that are not included as a project but part of the scm repo should be included, too
        for (final ICortPath project : projects) {
            ret.add(getWorkingCopyRoot(project));
        }
        //with nested projects, the set now contains a parent as well as its children. remove the children
        final Set<ICortPath> childProjects = new HashSet<>();
        for (final ICortPath cur : ret) {
            if (ret.contains(cur.removeLastSegments(1))) {
                childProjects.add(cur);
            }
        }
        ret.removeAll(childProjects);
        return ret;
    }

    private static ICortPath getWorkingCopyRoot(ICortPath location) {
        final IChangeSource[] cs = changeSources;
        final File dir = location.toFile();
        for (final IChangeSource c : cs) {
            try {
                final File root = c.determineWorkingCopyRoot(dir);
                if (root != null) {
                    return toPath(root);
                }
            } catch (final ChangeSourceException e) {
                Logger.warn("exception from changesource", e);
            }
        }
        return location;
    }

    public static void fillCacheIfEmpty(ICortWorkspace workspace, BooleanSupplier isCanceled)
            throws InterruptedException {

        if (cache != null) {
            return;
        }
        fillCache(workspace, isCanceled);
    }

    /**
     * A linked list node in the cache.
     */
    private static final class PathChainNode {
        private final PathChainNode next;
        private final ICortPath path;

        public PathChainNode(ICortPath path2, PathChainNode oldNode) {
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

        private final ICortPath path;
        private final ConcurrentHashMap<String, PathChainNode> sharedMap;

        public FillCacheAction(ICortPath directory, ConcurrentHashMap<String, PathChainNode> sharedMap) {
            this.path = directory;
            this.sharedMap = sharedMap;
        }

        @Override
        protected void compute() {
            final File[] children = this.path.toFile().listFiles();
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
                    subActions.add(new FillCacheAction(this.path.append(childName), this.sharedMap));
                } else {
                    final String childNameWithoutExtension = stripExtension(childName);
                    this.addToMap(childNameWithoutExtension, this.path.append(childName));
                }
            }
            invokeAll(subActions);
        }

        private void addToMap(String childNameWithoutExtension, ICortPath path) {
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
     * Returns the workspace root if no file resource could be found.
     */
    public static ICortResource toResource(Position pos) {
        return toResource(pos.getShortFileName());
    }

    /**
     * Returns the resource for the given short filename.
     * Returns the workspace root if no file resource could be found.
     */
    public static ICortResource toResource(String filename) {
        final ICortPath path = toPath(filename);
        final ICortWorkspace workspace = workspaceSupplier.get();
        return path == null ? workspace.getRoot() : workspace.getResourceForPath(path);
    }

    /**
     * Returns the resource for the file from the given position.
     * Returns null if no file could be found.
     */
    public static ICortPath toPath(Position pos) {
        return toPath(pos.getShortFileName());
    }

    /**
     * Returns the path for the given short filename.
     * Returns null if no file could be found.
     */
    public static ICortPath toPath(String filename) {
        if (filename == null) {
            return null;
        }
        final String[] segments = filename.split("/");
        final String filenameWithoutExtension = stripExtension(segments[segments.length - 1]);
        final List<ICortPath> paths = getCachedPathsForName(workspaceSupplier.get(), filenameWithoutExtension);
        if (paths == null) {
            return null;
        }
        if (segments.length == 1 && paths.size() == 1) {
            return paths.get(0);
        }
        final ICortPath fittingPath = findFittingPath(segments, paths);
        if (fittingPath == null) {
            return null;
        }
        return fittingPath;
    }

    /**
     * Returns the path for the given file.
     */
    public static ICortPath toPath(File absolutePathInWc) {
        return workspaceSupplier.get().createPath(absolutePathInWc);
    }

    private static ICortPath findFittingPath(String[] segments, List<ICortPath> paths) {
        ICortPath result = null;
        for (final ICortPath path : paths) {
            final String[] pathSegments = path.segments();
            if (sameSuffix(segments, pathSegments, segments.length)
                    && (result == null || pathSegments.length < result.segments().length)) {
                result = path;
            }
        }
        return result;
    }

    public static void setChangeSources(List<IChangeSource> list) {
        changeSources = list.toArray(new IChangeSource[list.size()]);
    }

}
