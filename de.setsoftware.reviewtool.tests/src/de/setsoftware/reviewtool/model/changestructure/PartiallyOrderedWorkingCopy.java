package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepository;

final class PartiallyOrderedWorkingCopy extends AbstractWorkingCopy {

    public static PartiallyOrderedWorkingCopy INSTANCE = new PartiallyOrderedWorkingCopy();

    @Override
    public IRepository getRepository() {
        return PartiallyOrderedRepo.INSTANCE;
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