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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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
    public final static String MEMBER_SEPARATOR = ".";

    private ICommit commit;

    private Path tmpDir;
    public Path previousDir;
    public Path currentDir;

    public Set<Path> previousDirFiles = new HashSet<>();
    public Set<Path> currentDirFiles = new HashSet<>();

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
        parseTmpFilesFrom();
        parseTmpFilesTo();
        processParsedData();
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

    private String getName(MethodDeclaration node) {
        String parameters = "(";
        for (Object parameter : node.parameters()) {
            if (parameters.equals("(")) {
                parameters = parameters + parameter;
            } else {
                parameters = parameters + ", " + parameter;
            }
        }
        // replace parameter names
        parameters = parameters.replaceAll(" .*", "") + ")";
        // replace generics
        return node.getName() + parameters.replaceAll("\\<.*\\>", "");
    }

    private String getName(TypeDeclaration node) {
        return node.getName().toString();
    }

    private String getParent(ASTNode node, String path) {
        // path = path.replaceFirst("\\.java$", "");
        // replace generics
        return getParent2(node, path).replaceAll("\\<.*\\>", "");
    }

    private String getParent2(ASTNode node, String parent) {
        if (node == null) {
            return parent;
        }
        if (node instanceof AbstractTypeDeclaration) {
            String name = ((AbstractTypeDeclaration) node).getName().toString();
            // String fileName = parent.replaceAll(".*/", "").replaceFirst(".java", "");
            // if (name.equals(fileName))
            // return parent;
            // else
            return getParent2(node.getParent(), parent) + MEMBER_SEPARATOR + name;
        }

        if (node instanceof ClassInstanceCreation) {
            String name = ((ClassInstanceCreation) node).getType().toString();
            return getParent2(node.getParent(), parent) + MEMBER_SEPARATOR + name;
        }
        return getParent2(node.getParent(), parent);
    }

    private void parseTmpFilesFrom() throws IOException {
        ASTParser parser = makeASTParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {
                // String path = sourceFilePath.replaceFirst(previousDir.getPath(), "");
                String path = compilationUnit.getPackage().getName().toString();

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = getName(node);
                        String parent = getParent(node.getParent(), path);
                        ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        previousMethods.add(part);
                        previousMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        String name = getName(node);
                        String parent = getParent(node.getParent(), path);
                        ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        previousTypes.add(part);
                        previousTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        String[] filesArray = new String[previousDirFiles.size()];
        Iterator<Path> iterator = previousDirFiles.iterator();
        for (int i = 0; i < previousDirFiles.size(); i++) {
            filesArray[i] = iterator.next().toString();
        }

        parser.createASTs(filesArray, null, new String[0], requestor, null);
    }

    private void parseTmpFilesTo() throws IOException {
        ASTParser parser = makeASTParser(new String[0]);
        FileASTRequestor requestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit compilationUnit) {
                // String path = sourceFilePath.replaceFirst(currentDir.getPath(), "");
                String path = compilationUnit.getPackage().getName().toString();

                ASTVisitor visitor = new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        String name = getName(node);
                        String parent = getParent(node.getParent(), path);
                        ChangePart part = new ChangePart(name, parent, Kind.METHOD);
                        currentMethods.add(part);
                        currentMethodsCode.put(part, node.toString());
                        return true;
                    }

                    @Override
                    public boolean visit(TypeDeclaration node) {
                        String name = node.getName().toString();
                        String parent = getParent(node.getParent(), path);
                        ChangePart part = new ChangePart(name, parent, Kind.TYPE);
                        currentTypes.add(part);
                        currentTypesCode.put(part, node.toString());
                        return true;
                    }
                };
                compilationUnit.accept(visitor);
            }
        };
        String[] filesArray = new String[currentDirFiles.size()];
        Iterator<Path> iterator = currentDirFiles.iterator();
        for (int i = 0; i < currentDirFiles.size(); i++) {
            filesArray[i] = iterator.next().toString();
        }

        parser.createASTs(filesArray, null, new String[0], requestor, null);
    }

    private ASTParser makeASTParser(String[] sourceFolders) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
        parser.setCompilerOptions(options);
        // parser.setResolveBindings(true);
        // parser.setBindingsRecovery(true);
        // parser.setEnvironment(new String[0], sourceFolders, null, true);
        return parser;
    }

    private void processParsedData() {
        processParsedMethodData();
        processParsedTypesData();
    }

    private void processParsedMethodData() {
        for (ChangePart part : previousMethods) {
            if (currentMethods.contains(part)) {
                if (!previousMethodsCode.get(part).equals(currentMethodsCode.get(part))) {
                    model.changedParts.addPart(part);
                }
                currentMethods.remove(part);
            } else {
                model.deletedParts.addPart(part);
            }
        }
        for (ChangePart part : currentMethods) {
            model.newParts.addPart(part);
        }
    }

    private void processParsedTypesData() {
        for (ChangePart part : previousTypes) {
            if (currentTypes.contains(part)) {
                if (!previousTypesCode.get(part).equals(currentTypesCode.get(part))) {
                    model.changedParts.addPart(part);
                }
                currentTypes.remove(part);
            } else {
                model.deletedParts.addPart(part);
            }
        }
        for (ChangePart part : currentTypes) {
            model.newParts.addPart(part);
        }
    }

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
}
