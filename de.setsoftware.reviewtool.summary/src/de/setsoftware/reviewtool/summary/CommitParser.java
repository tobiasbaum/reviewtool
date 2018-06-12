package de.setsoftware.reviewtool.summary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.summary.ChangePart.Kind;

/**
 * This class generates default representation of a commits for the summary and
 * creates temporary file copies of changed files that can be used by other
 * summarize techniques.
 */
public class CommitParser {
    public static final String MEMBER_SEPARATOR = ".";
    public static final String ANONYMOUS_SUFFIX = "$";

    // 10 5-character words equals 1 method call
    public static final double CODE_LENGHT_RELEVANCE_FACTOR = 0.02;

    private final ICommit commit;

    private Path tmpDir;
    public Path previousDir;
    public Path currentDir;

    public Set<Path> previousDirFiles = new HashSet<>();
    public Set<Path> currentDirFiles = new HashSet<>();

    public String[] previousDirFilesArray;
    public String[] currentDirFilesArray;

    private final List<ChangePart> previousMethods = new ArrayList<>();
    private final Map<ChangePart, String> previousMethodsCode = new HashMap<>();

    private final List<ChangePart> currentMethods = new ArrayList<>();
    private final Map<ChangePart, String> currentMethodsCode = new HashMap<>();

    private final List<ChangePart> previousTypes = new ArrayList<>();
    private final Map<ChangePart, String> previousTypesCode = new HashMap<>();

    private final List<ChangePart> currentTypes = new ArrayList<>();
    private final Map<ChangePart, String> currentTypesCode = new HashMap<>();

    private final ChangePartsModel model;

    public CommitParser(ICommit commit, ChangePartsModel model) {
        this.commit = commit;
        this.model = model;
    }

    /**
     * Parse commit, populate model with detected change parts, compute change parts
     * relevance and create temporary file copies of changed files.
     */
    public void processCommit() throws Exception {
        this.tmpDir = Files.createTempDirectory("CORT");
        this.previousDir = this.tmpDir.resolve("prev");
        this.currentDir = this.tmpDir.resolve("cur");

        for (final IChange change : this.commit.getChanges()) {
            if (this.isSourceFile(change)) {
                this.writeTmpFileFrom(change);
                this.writeTmpFileTo(change);
            } else {
                this.processNonSourceChange(change);
            }
        }

        this.previousDirFilesArray = new String[this.previousDirFiles.size()];
        final Iterator<Path> i1 = this.previousDirFiles.iterator();
        for (int i = 0; i < this.previousDirFiles.size(); i++) {
            this.previousDirFilesArray[i] = i1.next().toString();
        }

        this.currentDirFilesArray = new String[this.currentDirFiles.size()];
        final Iterator<Path> i2 = this.currentDirFiles.iterator();
        for (int i = 0; i < this.currentDirFiles.size(); i++) {
            this.currentDirFilesArray[i] = i2.next().toString();
        }

        this.parseTmpFilesFrom();
        this.parseTmpFilesTo();
        this.processParsedData();

        this.parseTmpFilesFromForRelevance();
        this.parseTmpFilesToForRelevance();
        this.addCodeLengthToRelevance();
    }

    private boolean isSourceFile(IChange change) throws Exception {
        if (this.getRelPathFrom(change).toString().replaceAll(".*\\.", "").equals("java")) {
            return true;
        }
        return false;
    }

    private Path getRelPathFrom(IChange change) {
        final Path root = this.commit.getWorkingCopy().getLocalRoot().toPath();
        final Path file = change.getFrom().toLocalPath(this.commit.getWorkingCopy()).toFile().toPath();
        return root.relativize(file);
    }

    private Path getRelPathTo(IChange change) {
        final Path root = this.commit.getWorkingCopy().getLocalRoot().toPath();
        final Path file = change.getTo().toLocalPath(this.commit.getWorkingCopy()).toFile().toPath();
        return root.relativize(file);
    }

    private void processNonSourceChange(IChange change) throws Exception {
        if (this.isNewFile(change)) {
            this.model.newParts.addPart(
                    new ChangePart(this.getRelPathTo(change).toString(), "", Kind.NON_SOURCE_FILE));
        } else if (this.isDeletedFile(change)) {
            this.model.deletedParts.addPart(
                    new ChangePart(this.getRelPathFrom(change).toString(), "", Kind.NON_SOURCE_FILE));
        } else {
            this.model.changedParts.addPart(
                    new ChangePart(this.getRelPathFrom(change).toString(), "", Kind.NON_SOURCE_FILE));
        }
    }

    private boolean isNewFile(IChange change) throws Exception {
        final IRevisionedFile revFileFrom = change.getFrom();
        final IRevisionedFile revFileTo = change.getTo();
        return (revFileFrom.getContents().length == 0 && revFileTo.getContents().length != 0);
    }

    private boolean isDeletedFile(IChange change) throws Exception {
        final IRevisionedFile revFileFrom = change.getFrom();
        final IRevisionedFile revFileTo = change.getTo();
        return (revFileFrom.getContents().length != 0 && revFileTo.getContents().length == 0);
    }

    private void writeTmpFileFrom(IChange change) throws Exception {
        final IRevisionedFile revFile = change.getFrom();
        final Path file = this.previousDir.resolve(this.getRelPathFrom(change));
        if (revFile.getContents().length != 0 && this.previousDirFiles.add(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, revFile.getContents());
        }
    }

    private void writeTmpFileTo(IChange change) throws Exception {
        final IRevisionedFile revFile = change.getTo();
        final Path file = this.currentDir.resolve(this.getRelPathFrom(change));
        if (revFile.getContents().length != 0 && this.currentDirFiles.add(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, revFile.getContents());
        }
    }

    private String getName(IMethodBinding node) {
        String parameters = "(";
        for (final ITypeBinding parameter : node.getParameterTypes()) {
            if (parameters.equals("(")) {
                parameters = parameters + parameter.getName();
            } else {
                parameters = parameters + ", " + parameter.getName();
            }
        }
        parameters = parameters + ")";
        // replace generics
        return node.getName() + parameters.replaceAll("\\<.*\\>", "");
    }

    private String getName(ITypeBinding node) {
        return node.getName().toString();
    }

    private String getParent(IMethodBinding method) {
        final ITypeBinding parent = method.getDeclaringClass();
        if (parent.isAnonymous()) {
            return this.getParent(parent.getDeclaringClass()) + MEMBER_SEPARATOR + parent.getSuperclass().getName()
                    + ANONYMOUS_SUFFIX;
        }
        return parent.getPackage().getName() + MEMBER_SEPARATOR + parent.getName();
    }

    private String getParent(ITypeBinding type) {
        if (type.isAnonymous()) {
            return this.getParent(type.getDeclaringClass()) + MEMBER_SEPARATOR + type.getSuperclass().getName()
                    + ANONYMOUS_SUFFIX;
        }
        return type.getPackage().getName();
    }

    private void parseTmpFilesFrom() throws IOException {
        final ASTParser parser = this.makeAstParser(new String[0]);
        final FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                final ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        final String name = CommitParser.this.getName(node.resolveBinding());
                        final String parent = CommitParser.this.getParent(node.resolveBinding());
                        final ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        CommitParser.this.previousMethods.add(part);
                        CommitParser.this.previousMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        final String name = CommitParser.this.getName(node.resolveBinding());
                        final String parent = CommitParser.this.getParent(node.resolveBinding());
                        final ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        CommitParser.this.previousTypes.add(part);
                        CommitParser.this.previousTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(this.previousDirFilesArray, null, new String[0], requestor, null);
    }

    private void parseTmpFilesTo() throws IOException {
        final ASTParser parser = this.makeAstParser(new String[0]);
        final FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                final ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        final String name = CommitParser.this.getName(node.resolveBinding());
                        final String parent = CommitParser.this.getParent(node.resolveBinding());
                        final ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        CommitParser.this.currentMethods.add(part);
                        CommitParser.this.currentMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        final String name = CommitParser.this.getName(node.resolveBinding());
                        final String parent = CommitParser.this.getParent(node.resolveBinding());
                        final ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        CommitParser.this.currentTypes.add(part);
                        CommitParser.this.currentTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(this.currentDirFilesArray, null, new String[0], requestor, null);
    }

    private ASTParser makeAstParser(String[] sourceFolders) {
        final ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        final Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(new String[0], sourceFolders, null, true);
        return parser;
    }

    private void processParsedData() {
        this.processParsedMethodData();
        this.processParsedTypesData();
    }

    private void processParsedMethodData() {
        for (final ChangePart part : this.previousMethods) {
            if (this.currentMethods.contains(part)) {
                if (!this.previousMethodsCode.get(part).equals(this.currentMethodsCode.get(part))
                        && !this.model.newParts.methods.contains(part)) {
                    this.model.changedParts.addPart(part);
                }
                this.currentMethods.remove(part);
            } else {
                if (!this.model.newParts.methods.contains(part)) {
                    this.model.deletedParts.addPart(part);
                } else {
                    this.model.newParts.methods.remove(part);
                }
            }
        }
        for (final ChangePart part : this.currentMethods) {
            this.model.newParts.addPart(part);
        }
    }

    private void processParsedTypesData() {
        for (final ChangePart part : this.previousTypes) {
            if (this.currentTypes.contains(part)) {
                if (!this.previousTypesCode.get(part).equals(this.currentTypesCode.get(part))
                        && !this.model.newParts.types.contains(part)) {
                    this.model.changedParts.addPart(part);
                }
                this.currentTypes.remove(part);
            } else {
                if (!this.model.newParts.types.contains(part)) {
                    this.model.deletedParts.addPart(part);
                } else {
                    this.model.newParts.types.remove(part);
                }
            }
        }
        for (final ChangePart part : this.currentTypes) {
            this.model.newParts.addPart(part);
        }
    }

    /**
     * Remove temporary files.
     */
    public void clean() throws IOException {
        Files.walkFileTree(this.tmpDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw e;
                }
            }
        });
    }

    private void parseTmpFilesFromForRelevance() throws IOException {
        final ASTParser parser = this.makeAstParser(new String[0]);
        final FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                final ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        final IMethodBinding method = node.resolveMethodBinding();
                        if (method != null) {
                            final String name = CommitParser.this.getName(method);
                            final String parent = CommitParser.this.getParent(method);
                            final ChangePart part = new ChangePart(name, parent, Kind.METHOD);

                            final int i = CommitParser.this.model.deletedParts.methods.indexOf(part);
                            if (i != -1) {
                                CommitParser.this.model.deletedParts.methods.get(i).relevance++;
                            }
                        }
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(this.previousDirFilesArray, null, new String[0], requestor, null);
    }

    private void parseTmpFilesToForRelevance() throws IOException {
        final ASTParser parser = this.makeAstParser(new String[0]);
        final FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                final ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        final IMethodBinding method = node.resolveMethodBinding();
                        if (method != null) {
                            final String name = CommitParser.this.getName(method);
                            final String parent = CommitParser.this.getParent(method);
                            final ChangePart part = new ChangePart(name, parent, Kind.METHOD);

                            final int i1 = CommitParser.this.model.changedParts.methods.indexOf(part);
                            if (i1 != -1) {
                                CommitParser.this.model.changedParts.methods.get(i1).relevance++;
                            }

                            final int i2 = CommitParser.this.model.newParts.methods.indexOf(part);
                            if (i2 != -1) {
                                CommitParser.this.model.newParts.methods.get(i2).relevance++;
                            }
                        }
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(this.currentDirFilesArray, null, new String[0], requestor, null);
    }

    private void addCodeLengthToRelevance() {
        for (final ChangePart part : this.model.deletedParts.methods) {
            part.relevance = (int) ((part.relevance
                    + getLengthIfExists(this.previousMethodsCode, part) * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
        for (final ChangePart part : this.model.changedParts.methods) {
            part.relevance = (int) ((part.relevance
                    + getLengthIfExists(this.currentMethodsCode, part) * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
        for (final ChangePart part : this.model.newParts.methods) {
            part.relevance = (int) ((part.relevance
                    + getLengthIfExists(this.currentMethodsCode, part) * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
    }

    private static int getLengthIfExists(Map<ChangePart, String> map, ChangePart key) {
        final String value = map.get(key);
        return value == null ? 0 : value.length();
    }
}
