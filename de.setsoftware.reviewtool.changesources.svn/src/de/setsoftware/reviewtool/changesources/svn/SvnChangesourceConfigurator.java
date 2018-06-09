package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for this package.
 */
public class SvnChangesourceConfigurator implements IConfigurator {

    @Override
    public Set<String> getRelevantElementNames() {
        return Collections.singleton("svnChangeSource");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {

        final List<File> projectDirs = new ArrayList<>();
        final IWorkspace root = ResourcesPlugin.getWorkspace();
        for (final IProject project : root.getRoot().getProjects()) {
            final IPath location = project.getLocation();
            if (location != null) {
                projectDirs.add(location.toFile());
            }
        }
        final String user = xml.getAttribute("user");
        final String pwd = xml.getAttribute("password");
        final String pattern = xml.getAttribute("pattern");
        final String maxTextDiffThreshold = xml.getAttribute("maxTextDiffFileSizeThreshold");
        final String minLogCacheSize = xml.getAttribute("minLogCacheSize");
        configurable.setChangeSource(new SvnChangeSource(
                projectDirs, pattern, user, pwd,
                Long.parseLong(maxTextDiffThreshold),
                minLogCacheSize.isEmpty() ? 1000 : Integer.parseInt(minLogCacheSize)));
    }

}
