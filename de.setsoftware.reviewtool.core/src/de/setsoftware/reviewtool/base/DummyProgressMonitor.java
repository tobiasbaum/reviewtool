package de.setsoftware.reviewtool.base;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A singleton {@link IProgressMonitor} implementation doing nothing.
 */
public class DummyProgressMonitor implements IProgressMonitor {

    private static final DummyProgressMonitor INSTANCE = new DummyProgressMonitor();

    /**
     * Returns the singleton instance of this class.
     */
    public static DummyProgressMonitor getInstance() {
        return INSTANCE;
    }

    @Override
    public void beginTask(final String name, final int totalWork) {
    }

    @Override
    public void done() {
    }

    @Override
    public void internalWorked(final double work) {
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setCanceled(final boolean value) {
    }

    @Override
    public void setTaskName(final String name) {
    }

    @Override
    public void subTask(final String name) {
    }

    @Override
    public void worked(final int work) {
    }
}
