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
    public static String process(Path previousDir, Path currentDir, Set<Path> previousDirFiles,
            Set<Path> currentDirFiles, ChangePartsModel model) {

        ArrayList<String> filesBefore = new ArrayList<>();
        ArrayList<String> filesCurrent = new ArrayList<>();

        for (Path file : previousDirFiles) {
            filesBefore.add(previousDir.relativize(file).toString());
        }

        for (Path file : currentDirFiles) {
            filesCurrent.add(currentDir.relativize(file).toString());
        }

        StringBuilder text = new StringBuilder("");

        for (ChangePart type : model.changedParts.types) {
            // Path fileFrom = previousDir.resolve(type.getParent().replaceAll("\\.",
            // File.separator))
            // .resolve(type.getName() + ".java");
            // Path fileTo = currentDir.resolve(type.getParent().replaceAll("\\.",
            // File.separator))
            // .resolve(type.getName() + ".java");

            String parentPath = type.getParent().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
            String relFilePath = parentPath + File.separator + type.getName() + ".java";

            File f1 = null;
            File f2 = null;
            for (String file : filesBefore) {
                if (file.contains(relFilePath)) {
                    f1 = new File(previousDir.toFile(), file);
                }
            }
            for (String file : filesCurrent) {
                if (file.contains(relFilePath)) {
                    f2 = new File(currentDir.toFile(), file);
                }
            }

            if (f1 != null && f2 != null) {
                DocPrinter print = new DocToPlainText();
                try {
                    // *** read in files ***
                    String source1 = FileReader.readFile(f1);
                    String source2 = FileReader.readFile(f2);

                    // *** parse them, create CFGs ***
                    EclipseCFGParser parser = new EclipseCFGParser();
                    List<ClassDeclaration> classes1 = parser.parse(source1);
                    List<ClassDeclaration> classes2 = parser.parse(source2);

                    // *** enumerate paths, do symbolic execution, make change records ***
                    PreProcess pp = new PreProcess();
                    RevisionRecord r1 = pp.process(classes1);
                    RevisionRecord r2 = pp.process(classes2);

                    // *** Discover what changed and needs to be documented ***
                    DeltaDoc doc = DeltaDoc.computeDelta(r1, r2);

                    // *** format / distill hierarchical documentation ***
                    DocNode output = HierarchicalDoc.makeDoc(doc);
                    CombinePredicates.process(output);

                    // *** print it ***
                    String out = print.print(output);
                    if (!out.contains("No Appreciable Change")) {
                        out = out.replaceFirst(".*\n", "").replaceFirst(".*\n", "");
                        text.append("DeltaDoc for " + type.toString() + "\n");
                        text.append(out + "\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        return text.toString();
    }
}
