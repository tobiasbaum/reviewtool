package de.setsoftware.reviewtool.summary;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import raykernel.apps.deltadoc2.DeltaDoc;
import raykernel.apps.deltadoc2.PreProcess;
import raykernel.apps.deltadoc2.hierarchical.CombinePredicates;
import raykernel.apps.deltadoc2.hierarchical.DocNode;
import raykernel.apps.deltadoc2.hierarchical.HierarchicalDoc;
import raykernel.apps.deltadoc2.print.DocPrinter;
import raykernel.apps.deltadoc2.print.DocToPlainText;
import raykernel.apps.deltadoc2.record.RevisionRecord;
import raykernel.io.FileReader;
import raykernel.lang.parse.ClassDeclaration;
import raykernel.lang.parse.EclipseCFGParser;

/**
 * DeltaDoc summary technique.
 */
public class DeltaDocTechnique {
    /**
     * Generate DeltaDoc summary for changed types.
     */
    public static TextWithStyles process(Path previousDir, Path currentDir, Set<Path> previousDirFiles,
            Set<Path> currentDirFiles, ChangePartsModel model) {

        final ArrayList<String> filesBefore = new ArrayList<>();
        final ArrayList<String> filesCurrent = new ArrayList<>();

        for (final Path file : previousDirFiles) {
            filesBefore.add(previousDir.relativize(file).toString());
        }

        for (final Path file : currentDirFiles) {
            filesCurrent.add(currentDir.relativize(file).toString());
        }

        final TextWithStyles text = new TextWithStyles();

        for (final ChangePart type : model.changedParts.getAllTypeParts()) {
            // Path fileFrom = previousDir.resolve(type.getParent().replaceAll("\\.",
            // File.separator))
            // .resolve(type.getName() + ".java");
            // Path fileTo = currentDir.resolve(type.getParent().replaceAll("\\.",
            // File.separator))
            // .resolve(type.getName() + ".java");

            final String parentPath = type.getParent().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
            final String relFilePath = parentPath + File.separator + type.getName() + ".java";

            File f1 = null;
            File f2 = null;
            for (final String file : filesBefore) {
                if (file.contains(relFilePath)) {
                    f1 = new File(previousDir.toFile(), file);
                }
            }
            for (final String file : filesCurrent) {
                if (file.contains(relFilePath)) {
                    f2 = new File(currentDir.toFile(), file);
                }
            }

            if (f1 != null && f2 != null) {
                final DocPrinter print = new DocToPlainText();
                try {
                    // *** read in files ***
                    final String source1 = FileReader.readFile(f1);
                    final String source2 = FileReader.readFile(f2);

                    // *** parse them, create CFGs ***
                    final EclipseCFGParser parser = new EclipseCFGParser();
                    final List<ClassDeclaration> classes1 = parser.parse(source1);
                    final List<ClassDeclaration> classes2 = parser.parse(source2);

                    // *** enumerate paths, do symbolic execution, make change records ***
                    final PreProcess pp = new PreProcess();
                    final RevisionRecord r1 = pp.process(classes1);
                    final RevisionRecord r2 = pp.process(classes2);

                    // *** Discover what changed and needs to be documented ***
                    final DeltaDoc doc = DeltaDoc.computeDelta(r1, r2);

                    // *** format / distill hierarchical documentation ***
                    final DocNode output = HierarchicalDoc.makeDoc(doc);
                    CombinePredicates.process(output);

                    // *** print it ***
                    String out = print.print(output);
                    if (!out.contains("No Appreciable Change")) {
                        out = out.replaceFirst(".*\n", "").replaceFirst(".*\n", "");
                        out = out.replaceAll(".*added method :.*\n", "");
                        out = out.replaceAll(".*removed method :.*\n", "");
                        if (!out.isEmpty()) {
                            text.addItalic("DeltaDoc").addNormal(" for ").add(type.toStyledText()).addNormal("\n");
                            text.addNormal(out + "\n");
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }

            }
        }

        return text;
    }
}
