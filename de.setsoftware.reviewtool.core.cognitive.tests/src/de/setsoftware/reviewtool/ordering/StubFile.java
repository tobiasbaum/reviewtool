package de.setsoftware.reviewtool.ordering;

import org.eclipse.core.runtime.IPath;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;

public class StubFile implements IRevisionedFile {

    private static final long serialVersionUID = 1L;
    private final IRevisionedFile file;
    private final String content;

    public StubFile(String name, int revision, String content) {
        this.file = ChangestructureFactory.createFileInRevision(
                name,
                ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revision), StubRepo.INSTANCE));
        this.content = content;
    }

    @Override
    public IPath toLocalPath(final IWorkingCopy wc) {
        return this.file.toLocalPath(wc);
    }

    @Override
    public IRevision getRevision() {
        return this.file.getRevision();
    }

    @Override
    public IRepository getRepository() {
        return this.file.getRepository();
    }

    @Override
    public String getPath() {
        return this.file.getPath();
    }

    @Override
    public byte[] getContents() throws Exception {
        return this.content.getBytes("UTF-8");
    }

    @Override
    public boolean le(final IRevisionedFile other) {
        return this.file.le(other);
    }
}
