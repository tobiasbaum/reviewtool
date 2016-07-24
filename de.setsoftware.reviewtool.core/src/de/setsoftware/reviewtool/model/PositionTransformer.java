package de.setsoftware.reviewtool.model;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Is able to transform short filenames into paths and positions with short filenames into positions
 * for Eclipse. Reverse transformation is also supported.
 */
public class PositionTransformer {

    private static final long STALE_LIMIT_MS = 20L * 1000;
    private static final long REALLY_OLD_LIMIT_MS = 60L * 60 * 1000;

    private static AtomicBoolean refreshRunning = new AtomicBoolean();
    private static volatile ConcurrentHashMap<String, PathChainNode> cache = null;
    private static volatile long cacheRefreshTime;

    private static final IProgressMonitor NO_CANCEL_MONITOR = new IProgressMonitor() {
        @Override
        public void worked(int work) {
        }

        @Override
        public void subTask(String name) {
        }

        @Override
        public void setTaskName(String name) {
        }

        @Override
        public void setCanceled(boolean value) {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void internalWorked(double work) {
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
    public static Position toPosition(IPath path, int line, IWorkspace workspace) {
        if (path.toFile().isDirectory()) {
            return new GlobalPosition();
        }
        final String filename = stripExtension(path.lastSegment());
        if (filename.isEmpty()) {
            return new GlobalPosition();
        }
        final List<IPath> possiblePaths = getCachedPathsForName(workspace, filename);
        if (possiblePaths == null) {
            return new GlobalPosition();
        }
        if (possiblePaths.size() == 1) {
            return createPos(filename, line);
        }
        return createPos(getShortestUniqueName(path, possiblePaths), line);
    }

    private static String getShortestUniqueName(IPath resourcePath, List<IPath> possiblePaths) {
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

    private static int countWithSameSuffix(IPath resourcePath, List<IPath> possiblePaths, int suffixLength) {
        int count = 0;
        for (final IPath path : possiblePaths) {
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

    private static synchronized List<IPath> getCachedPathsForName(IWorkspace workspace, String filename) {
        if (cache == null) {
            //should normally have already been initialized, but it wasn't, so take
            //  the last chance to do so and do it synchronously
            try {
                fillCache(workspace, NO_CANCEL_MONITOR);
            } catch (final InterruptedException e) {
                throw new AssertionError(e);
            }
        }
        final List<IPath> cachedPaths = toList(cache.get(filename));
        if ((cachedPaths == null && cacheMightBeStale()) || cacheIsReallyOld()) {
            //Perhaps the resource exists but the cache was stale => trigger refresh
            refreshCacheInBackground(workspace);
        }
        return cachedPaths;
    }

    private static List<IPath> toList(PathChainNode startNode) {
        if (startNode == null) {
            return null;
        }
        final List<IPath> ret = new ArrayList<>();
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

    private static void fillCache(IWorkspace workspace, IProgressMonitor monitor)
            throws InterruptedException {

        if (refreshRunning.compareAndSet(false, true)) {
            final ForkJoinPool pool = new ForkJoinPool((Runtime.getRuntime().availableProcessors() + 1) / 2);
            final List<ForkJoinTask<Void>> tasks = new ArrayList<>();
            final ConcurrentHashMap<String, PathChainNode> newCache = new ConcurrentHashMap<>();
            for (final IPath path : determineRootPaths(workspace.getRoot().getProjects())) {
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

    private static Set<IPath> determineRootPaths(IProject[] projects) {
        final Set<IPath> ret = new LinkedHashSet<>();
        //paths that are not included as a project but part of the scm repo should be included, too
        //  as a simple hack all parents of the project directories are added, which works when all projects
        //  are checked out together and which scans too much (but hopefully is ok too) when the projects are
        //  checked out one by one
        for (final IProject project : projects) {
            ret.add(project.getLocation().removeLastSegments(1));
        }
        return ret;
    }

    private static void fillCacheIfEmpty(IWorkspace workspace, IProgressMonitor monitor)
            throws InterruptedException {

        if (cache != null) {
            return;
        }
        fillCache(workspace, monitor);
    }

    /**
     * Starts a new job that initializes the cache in the background.
     * If the cache has already been initialized, it does nothing.
     */
    public static void initializeCacheInBackground() {
        final IWorkspace root = ResourcesPlugin.getWorkspace();
        final Job job = Job.create("Review resource cache init", new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                try {
                    fillCacheIfEmpty(root, monitor);
                    return Status.OK_STATUS;
                } catch (final InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
            }

        });
        job.schedule();
    }

    /**
     * Starts a new job that refreshes the cache in the background.
     */
    public static void refreshCacheInBackground(final IWorkspace root) {
        final Job job = Job.create("Review resource cache refresh", new IJobFunction() {
            @Override
            public IStatus run(IProgressMonitor monitor) {
                try {
                    fillCache(root, monitor);
                    return Status.OK_STATUS;
                } catch (final InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
            }

        });
        job.schedule();
    }

    /**
     * A linked list node in the cache.
     */
    private static final class PathChainNode {
        private final PathChainNode next;
        private final IPath path;

        public PathChainNode(IPath path2, PathChainNode oldNode) {
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

        private final IPath path;
        private final ConcurrentHashMap<String, PathChainNode> sharedMap;

        public FillCacheAction(IPath directory, ConcurrentHashMap<String, PathChainNode> sharedMap) {
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

        private void addToMap(String childNameWithoutExtension, IPath path) {
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
    public static IResource toResource(Position pos) {
        return toResource(pos.getShortFileName());
    }

    /**
     * Returns the resource for the given short filename.
     * Returns the workspace root if no file resource could be found.
     */
    public static IResource toResource(String filename) {
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        if (filename == null) {
            return workspaceRoot;
        }
        final String[] segments = filename.split("/");
        final String filenameWithoutExtension = stripExtension(segments[segments.length - 1]);
        final List<IPath> paths = getCachedPathsForName(workspaceRoot.getWorkspace(), filenameWithoutExtension);
        if (paths == null) {
            return workspaceRoot;
        }
        if (segments.length == 1 && paths.size() == 1) {
            return getResourceForPath(workspaceRoot, paths.get(0));
        }
        final IPath fittingPath = findFittingPath(segments, paths);
        if (fittingPath == null) {
            return workspaceRoot;
        }
        return getResourceForPath(workspaceRoot, fittingPath);
    }

    private static IPath findFittingPath(String[] segments, List<IPath> paths) {
        for (final IPath path : paths) {
            if (sameSuffix(segments, path.segments(), segments.length)) {
                return path;
            }
        }
        return null;
    }

    private static IResource getResourceForPath(IWorkspaceRoot workspaceRoot, IPath fittingPath) {
        final IFile file = workspaceRoot.getFileForLocation(fittingPath);
        if (file == null || !file.exists()) {
            return workspaceRoot;
        }
        return file;
    }

}
