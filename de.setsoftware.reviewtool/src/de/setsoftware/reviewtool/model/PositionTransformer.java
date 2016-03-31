package de.setsoftware.reviewtool.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

public class PositionTransformer {

    private static final HashMap<String, List<IPath>> cache = new HashMap<>();

    public static Position toPosition(IResource resource, int line) {
        if (resource.getType() != IResource.FILE) {
            return new GlobalPosition();
        }
        final String filename = stripExtension(resource.getName());
        if (filename.isEmpty()) {
            return new GlobalPosition();
        }
        final List<IPath> possiblePaths = getCachedPathsForName(resource, filename);
        if (possiblePaths == null) {
            return new GlobalPosition();
        }
        if (possiblePaths.size() == 1) {
            return createPos(filename, line);
        }
        return createPos(getShortestUniqueName(resource.getFullPath(), possiblePaths), line);
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

    private static synchronized List<IPath> getCachedPathsForName(IResource resource, String filename) {
        final List<IPath> cachedPaths = cache.get(filename);
        if (cachedPaths == null) {
            //entweder noch komplett leer, oder es gibt eine Datei die wir nicht im
            //  Cache haben. In beiden Fällen ein Grund um den Cache neu zu befüllen
            fillCache(resource);
            return cache.get(filename);
        } else {
            return cachedPaths;
        }
    }

    private static synchronized void fillCache(IResource resource) {
        cache.clear();

        final LinkedHashSet<IPath> allRoots = new LinkedHashSet<>();

        fillCacheIfEmpty(resource.getWorkspace());
    }

    private static synchronized void fillCacheIfEmpty(IWorkspace workspace) {
        if (!cache.isEmpty()) {
            return;
        }
        for (final IProject project : workspace.getRoot().getProjects()) {
            //TEST
            System.out.println("project root: " + project.getFullPath().toFile().getAbsolutePath());
            fillCacheRecursive(project.getLocation());
        }
    }

    public static void initializeCacheInBackground() {
        final IWorkspace root = ResourcesPlugin.getWorkspace();
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                fillCacheIfEmpty(root);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void fillCacheRecursive(IPath path) {
        final File[] children = path.toFile().listFiles();
        if (children == null) {
            return;
        }
        for (final File child : children) {
            //TEST
            System.out.println("child: " + child.getAbsolutePath());
            final String childName = child.getName();
            if (childName.startsWith(".") || childName.equals("bin")) {
                continue;
            }
            if (child.isDirectory()) {
                fillCacheRecursive(path.append(childName));
            } else {
                final String childNameWithoutExtension = stripExtension(childName);
                List<IPath> existingPaths = cache.get(childNameWithoutExtension);
                if (existingPaths == null) {
                    existingPaths = new ArrayList<>();
                    cache.put(childNameWithoutExtension, existingPaths);
                }
                existingPaths.add(path.append(childName));
            }
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

    public static IResource toResource(Position pos) {
        final String filename = pos.getShortFileName();
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        if (filename == null) {
            return workspaceRoot;
        }
        final String[] segments = filename.split("/");
        final String filenameWithoutExtension = stripExtension(segments[segments.length - 1]);
        final List<IPath> paths = getCachedPathsForName(workspaceRoot, filenameWithoutExtension);
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
        if (!file.exists()) {
            return workspaceRoot;
        }
        return file;
    }

}
