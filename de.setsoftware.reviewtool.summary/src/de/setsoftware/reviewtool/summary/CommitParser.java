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
    public static final Double CODE_LENGHT_RELEVANCE_FACTOR = 0.02;

    private ICommit commit;

    private Path tmpDir;
    public Path previousDir;
    public Path currentDir;

    public Set<Path> previousDirFiles = new HashSet<>();
    public Set<Path> currentDirFiles = new HashSet<>();

    public String[] previousDirFilesArray;
    public String[] currentDirFilesArray;

    private List<ChangePart> previousMethods = new ArrayList<>();
    private Map<ChangePart, String> previousMethodsCode = new HashMap<>();

    private List<ChangePart> currentMethods = new ArrayList<>();
    private Map<ChangePart, String> currentMethodsCode = new HashMap<>();

    private List<ChangePart> previousTypes = new ArrayList<>();
    private Map<ChangePart, String> previousTypesCode = new HashMap<>();

    private List<ChangePart> currentTypes = new ArrayList<>();
    private Map<ChangePart, String> currentTypesCode = new HashMap<>();

    private ChangePartsModel model;

    public CommitParser(ICommit commit, ChangePartsModel model) {
        this.commit = commit;
        this.model = model;
    }

    /**
     * Parse commit, populate model with detected change parts, compute change parts
     * relevance and create temporary file copies of changed files.
     */
    public void processCommit() throws Exception {
        tmpDir = Files.createTempDirectory("CORT");
        previousDir = tmpDir.resolve("prev");
        currentDir = tmpDir.resolve("cur");

        for (IChange change : commit.getChanges()) {
            if (isSourceFile(change)) {
                writeTmpFileFrom(change);
                writeTmpFileTo(change);
            } else {
                processNonSourceChange(change);
            }
        }

        previousDirFilesArray = new String[previousDirFiles.size()];
        Iterator<Path> i1 = previousDirFiles.iterator();
        for (int i = 0; i < previousDirFiles.size(); i++) {
            previousDirFilesArray[i] = i1.next().toString();
        }

        currentDirFilesArray = new String[currentDirFiles.size()];
        Iterator<Path> i2 = currentDirFiles.iterator();
        for (int i = 0; i < currentDirFiles.size(); i++) {
            currentDirFilesArray[i] = i2.next().toString();
        }

        parseTmpFilesFrom();
        parseTmpFilesTo();
        processParsedData();

        parseTmpFilesFromForRelevance();
        parseTmpFilesToForRelevance();
        addCodeLengthToRelevance();
    }

    private boolean isSourceFile(IChange change) throws Exception {
        if (getRelPathFrom(change).toString().replaceAll(".*\\.", "").equals("java")) {
            return true;
        }
        return false;
    }

    private Path getRelPathFrom(IChange change) {
        Path root = commit.getRevision().getRepository().getLocalRoot().toPath();
        Path file = change.getFrom().toLocalPath().toFile().toPath();
        return root.relativize(file);
    }

    private Path getRelPathTo(IChange change) {
        Path root = commit.getRevision().getRepository().getLocalRoot().toPath();
        Path file = change.getTo().toLocalPath().toFile().toPath();
        return root.relativize(file);
    }

    private void processNonSourceChange(IChange change) throws Exception {
        if (isNewFile(change)) {
            model.newParts.addPart(new ChangePart(getRelPathTo(change).toString(), "", Kind.NON_SOURCE_FILE));
        } else if (isDeletedFile(change)) {
            model.deletedParts.addPart(new ChangePart(getRelPathFrom(change).toString(), "", Kind.NON_SOURCE_FILE));
        } else {
            model.changedParts.addPart(new ChangePart(getRelPathFrom(change).toString(), "", Kind.NON_SOURCE_FILE));
        }
    }

    private boolean isNewFile(IChange change) throws Exception {
        IRevisionedFile revFileFrom = change.getFrom();
        IRevisionedFile revFileTo = change.getTo();
        return (revFileFrom.getContents().length == 0 && revFileTo.getContents().length != 0);
    }

    private boolean isDeletedFile(IChange change) throws Exception {
        IRevisionedFile revFileFrom = change.getFrom();
        IRevisionedFile revFileTo = change.getTo();
        return (revFileFrom.getContents().length != 0 && revFileTo.getContents().length == 0);
    }

    private void writeTmpFileFrom(IChange change) throws Exception {
        IRevisionedFile revFile = change.getFrom();
        Path file = previousDir.resolve(getRelPathFrom(change));
        if (revFile.getContents().length != 0 && previousDirFiles.add(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, revFile.getContents());
        }
    }

    private void writeTmpFileTo(IChange change) throws Exception {
        IRevisionedFile revFile = change.getTo();
        Path file = currentDir.resolve(getRelPathFrom(change));
        if (revFile.getContents().length != 0 && currentDirFiles.add(file)) {
            Files.createDirectories(file.getParent());
            Files.write(file, revFile.getContents());
        }
    }

    private String getName(IMethodBinding node) {
        String parameters = "(";
        for (ITypeBinding parameter : node.getParameterTypes()) {
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
        ITypeBinding parent = method.getDeclaringClass();
        if (parent.isAnonymous()) {
            return getParent(parent.getDeclaringClass()) + MEMBER_SEPARATOR + parent.getSuperclass().getName()
                    + ANONYMOUS_SUFFIX;
        }
        return parent.getPackage().getName() + MEMBER_SEPARATOR + parent.getName();
    }

    private String getParent(ITypeBinding type) {
        if (type.isAnonymous()) {
            return getParent(type.getDeclaringClass()) + MEMBER_SEPARATOR + type.getSuperclass().getName()
                    + ANONYMOUS_SUFFIX;
        }
        return type.getPackage().getName();
    }

    private void parseTmpFilesFrom() throws IOException {
        ASTParser parser = makeAstParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = getName(node.resolveBinding());
                        String parent = getParent(node.resolveBinding());
                        ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        previousMethods.add(part);
                        previousMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        String name = getName(node.resolveBinding());
                        String parent = getParent(node.resolveBinding());
                        ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        previousTypes.add(part);
                        previousTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(previousDirFilesArray, null, new String[0], requestor, null);
    }

    private void parseTmpFilesTo() throws IOException {
        ASTParser parser = makeAstParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = getName(node.resolveBinding());
                        String parent = getParent(node.resolveBinding());
                        ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        currentMethods.add(part);
                        currentMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        String name = getName(node.resolveBinding());
                        String parent = getParent(node.resolveBinding());
                        ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        currentTypes.add(part);
                        currentTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(currentDirFilesArray, null, new String[0], requestor, null);
    }

    private ASTParser makeAstParser(String[] sourceFolders) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setEnvironment(new String[0], sourceFolders, null, true);
        return parser;
    }

    private void processParsedData() {
        processParsedMethodData();
        processParsedTypesData();
    }

    private void processParsedMethodData() {
        for (ChangePart part : previousMethods) {
            if (currentMethods.contains(part)) {
                if (!previousMethodsCode.get(part).equals(currentMethodsCode.get(part))
                        && !model.newParts.methods.contains(part)) {
                    model.changedParts.addPart(part);
                }
                currentMethods.remove(part);
            } else {
                if(!model.newParts.methods.contains(part)) {
                    model.deletedParts.addPart(part);
                } else {
                    model.newParts.methods.remove(part);
                }
            }
        }
        for (ChangePart part : currentMethods) {
            model.newParts.addPart(part);
        }
    }

    private void processParsedTypesData() {
        for (ChangePart part : previousTypes) {
            if (currentTypes.contains(part)) {
                if (!previousTypesCode.get(part).equals(currentTypesCode.get(part))
                        && !model.newParts.types.contains(part)) {
                    model.changedParts.addPart(part);
                }
                currentTypes.remove(part);
            } else {
                if(!model.newParts.types.contains(part)) {
                    model.deletedParts.addPart(part);
                } else {
                    model.newParts.types.remove(part);
                }
            }
        }
        for (ChangePart part : currentTypes) {
            model.newParts.addPart(part);
        }
    }

    /**
     * Remove temporary files.
     */
    public void clean() throws IOException {
        Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
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
        ASTParser parser = makeAstParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        IMethodBinding method = node.resolveMethodBinding();
                        if (method != null) {
                            String name = getName(method);
                            String parent = getParent(method);
                            ChangePart part = new ChangePart(name, parent, Kind.METHOD);

                            int i = model.deletedParts.methods.indexOf(part);
                            if (i != -1) {
                                model.deletedParts.methods.get(i).relevance++;
                            }
                        }
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(previousDirFilesArray, null, new String[0], requestor, null);
    }

    private void parseTmpFilesToForRelevance() throws IOException {
        ASTParser parser = makeAstParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        IMethodBinding method = node.resolveMethodBinding();
                        if (method != null) {
                            String name = getName(method);
                            String parent = getParent(method);
                            ChangePart part = new ChangePart(name, parent, Kind.METHOD);

                            int i1 = model.changedParts.methods.indexOf(part);
                            if (i1 != -1) {
                                model.changedParts.methods.get(i1).relevance++;
                            }

                            int i2 = model.newParts.methods.indexOf(part);
                            if (i2 != -1) {
                                model.newParts.methods.get(i2).relevance++;
                            }
                        }
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        parser.createASTs(currentDirFilesArray, null, new String[0], requestor, null);
    }

    private void addCodeLengthToRelevance() {
        for (ChangePart part : model.deletedParts.methods) {
            part.relevance = (int) ((part.relevance
                    + previousMethodsCode.get(part).length() * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
        for (ChangePart part : model.changedParts.methods) {
            part.relevance = (int) ((part.relevance
                    + currentMethodsCode.get(part).length() * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
        for (ChangePart part : model.newParts.methods) {
            part.relevance = (int) ((part.relevance
                    + currentMethodsCode.get(part).length() * CODE_LENGHT_RELEVANCE_FACTOR) / 2);
        }
    }
}
