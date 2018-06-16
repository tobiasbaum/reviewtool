package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * A stub implementation of {@link IWorkingCopy} for use by tests.
 */
public final class StubWorkingCopy extends AbstractWorkingCopy {

    public static StubWorkingCopy INSTANCE = new StubWorkingCopy();

    @Override
    public IRepository getRepository() {
        return StubRepo.INSTANCE;
    }

    @Override
    public File getLocalRoot() {
        return new File("/");
    }

    @Override
    public String getRelativePath() {
        return null;
    }

    @Override
    public String toAbsolutePathInWc(final String absolutePathInRepo) {
        return absolutePathInRepo;
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return null;
    }
}
