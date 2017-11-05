package de.setsoftware.reviewtool.ordering;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;

/**
 * Tests for {@link ChangePart}.
 */
public class ChangePartTest {

    private static ChangePart cp(Stop... stops) {
        return new ChangePart(Arrays.asList(stops));
    }

    private static IRevisionedFile file(String name, int revision) {
        return ChangestructureFactory.createFileInRevision(
                name, ChangestructureFactory.createRepoRevision(revision, StubRepo.INSTANCE));
    }

    private static IRevisionedFile file(
            final String name, final int revision, final String content) {
        return new IRevisionedFile() {

            @Override
            public IPath toLocalPath() {
                throw new UnsupportedOperationException();
            }

            @Override
            public IRevision getRevision() {
                return ChangestructureFactory.createRepoRevision(revision, StubRepo.INSTANCE);
            }

            @Override
            public IRepository getRepository() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getPath() {
                return name;
            }

            @Override
            public byte[] getContents() throws Exception {
                return content.getBytes("UTF-8");
            }

            @Override
            public IResource determineResource() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Stop binaryStop(String filename) {
        return new Stop(
                ChangestructureFactory.createBinaryChange(file(filename, 1), file(filename, 3), false, true),
                file(filename, 4));
    }

    private static Stop singleLineStop(IRevisionedFile file, int lineNumber) throws Exception {
        final IPositionInText posFrom = ChangestructureFactory.createPositionInText(lineNumber, 1);
        final IPositionInText posTo = ChangestructureFactory.createPositionInText(lineNumber + 1, 1);
        return new Stop(
                ChangestructureFactory.createTextualChangeHunk(
                        ChangestructureFactory.createFragment(file(file.getPath(), 1), posFrom, posTo),
                        ChangestructureFactory.createFragment(file, posFrom, posTo),
                        false,
                        true),
                ChangestructureFactory.createFragment(file, posFrom, posTo));
    }

    @Test
    public void testGroupEmpty() {
        assertEquals(Collections.emptyList(), ChangePart.groupToMinimumGranularity(Collections.<Stop>emptyList()));
    }

    @Test
    public void testGroupSingle() {
        assertEquals(
                Arrays.asList(cp(binaryStop("A.java"))),
                ChangePart.groupToMinimumGranularity(Arrays.asList(binaryStop("A.java"))));
    }

    @Test
    public void testGroupDifferentFiles() {
        assertEquals(
                Arrays.asList(
                        cp(binaryStop("A.java")),
                        cp(binaryStop("B.java")),
                        cp(binaryStop("C.java"))),
                ChangePart.groupToMinimumGranularity(Arrays.asList(
                        binaryStop("A.java"),
                        binaryStop("B.java"),
                        binaryStop("C.java"))));
    }

    @Test
    public void testGroupDifferentMethods() throws Exception {
        final IRevisionedFile file = file("Testklasse.java", 4,
                "package x.y.z;\r\n"
                + "\r\n"
                + "public class Testclass {\r\n"
                + "    public void foo() {\r\n"
                + "        System.out.println(\"foo\");\r\n"
                + "    }\r\n"
                + "    public void bar() {\r\n"
                + "        System.out.println(\"bar\");\r\n"
                + "    }\r\n"
                + "}\r\n");
        final Stop s1 = singleLineStop(file, 5);
        final Stop s2 = singleLineStop(file, 8);
        assertEquals(
                Arrays.asList(
                        cp(s1),
                        cp(s2)),
                ChangePart.groupToMinimumGranularity(Arrays.asList(
                        s1,
                        s2)));
    }

    @Test
    public void testGroupSameMethod() throws Exception {
        final IRevisionedFile file = file("Testklasse.java", 4,
                "package x.y.z;\r\n"
                + "\r\n"
                + "public class Testclass {\r\n"
                + "    public void foo() {\r\n"
                + "        System.out.println(\"foo\");\r\n"
                + "    }\r\n"
                + "    public void bar() {\r\n"
                + "        System.out.println(\"bar\");\r\n"
                + "    }\r\n"
                + "}\r\n");
        final Stop s1 = singleLineStop(file, 7);
        final Stop s2 = singleLineStop(file, 8);
        assertEquals(
                Arrays.asList(
                        cp(s1, s2)),
                ChangePart.groupToMinimumGranularity(Arrays.asList(
                        s1,
                        s2)));
    }

    @Test
    public void testGroupVariousSituations() throws Exception {
        final IRevisionedFile file = file("Testklasse.java", 4,
                "package x.y.z;\r\n"
                + "\r\n"
                + "import some.package.Class1;\r\n"
                + "import some.package.Class2;\r\n"
                + "\r\n"
                + "/**\r\n"
                + " * A javadoc for the class.\r\n"
                + " */\r\n"
                + "public class Testclass {\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Javadoc for att1\r\n"
                + "     */\r\n"
                + "    private Class1 att1;\r\n"
                + "    private Class2 att2;\r\n"
                + "    //a single line comment\r\n"
                + "    //  that is part of a comment block\r\n"
                + "    private Class2 att3;\r\n"
                + "\r\n"
                + "    private Class2 anAttributeInitializedOverMultipleLines =\r\n"
                + "        new Class2(\r\n"
                + "            1,\r\n"
                + "            2,\r\n"
                + "            new int[] {4, 5, 6});\r\n"
                + "\r\n"
                + "    public void foo() {\r\n"
                + "        System.out.println(\"foo\");\r\n"
                + "    }\r\n"
                + "\r\n"
                + "    /**\r\n"
                + "     * Javadoc for bar.\r\n"
                + "     */\r\n"
                + "    public void bar() {\r\n"
                + "        System.out.println(\"bar\");\r\n"
                + "    }\r\n"
                + "}\r\n");
        final Stop s1 = singleLineStop(file, 1);
        final Stop s2 = singleLineStop(file, 2);
        final Stop s3 = singleLineStop(file, 3);
        final Stop s4 = singleLineStop(file, 4);
        final Stop s5 = singleLineStop(file, 5);
        final Stop s6 = singleLineStop(file, 6);
        final Stop s7 = singleLineStop(file, 7);
        final Stop s8 = singleLineStop(file, 8);
        final Stop s9 = singleLineStop(file, 9);
        final Stop s10 = singleLineStop(file, 10);
        final Stop s11 = singleLineStop(file, 11);
        final Stop s12 = singleLineStop(file, 12);
        final Stop s13 = singleLineStop(file, 13);
        final Stop s14 = singleLineStop(file, 14);
        final Stop s15 = singleLineStop(file, 15);
        final Stop s16 = singleLineStop(file, 16);
        final Stop s17 = singleLineStop(file, 17);
        final Stop s18 = singleLineStop(file, 18);
        final Stop s19 = singleLineStop(file, 19);
        final Stop s20 = singleLineStop(file, 20);
        final Stop s21 = singleLineStop(file, 21);
        final Stop s22 = singleLineStop(file, 22);
        final Stop s23 = singleLineStop(file, 23);
        final Stop s24 = singleLineStop(file, 24);
        final Stop s25 = singleLineStop(file, 25);
        final Stop s26 = singleLineStop(file, 26);
        final Stop s27 = singleLineStop(file, 27);
        final Stop s28 = singleLineStop(file, 28);
        final Stop s29 = singleLineStop(file, 29);
        final Stop s30 = singleLineStop(file, 30);
        final Stop s31 = singleLineStop(file, 31);
        final Stop s32 = singleLineStop(file, 32);
        final Stop s33 = singleLineStop(file, 33);
        final Stop s34 = singleLineStop(file, 34);
        final Stop s35 = singleLineStop(file, 35);
        final Stop s36 = singleLineStop(file, 36);
        assertEquals(
                Arrays.asList(
                        cp(s1),
                        cp(s2),
                        cp(s3),
                        cp(s4),
                        cp(s5),
                        cp(s6, s7, s8, s9),
                        cp(s10),
                        cp(s11, s12, s13, s14),
                        cp(s15),
                        cp(s16, s17, s18),
                        cp(s19),
                        cp(s20, s21, s22, s23, s24),
                        cp(s25),
                        cp(s26, s27, s28),
                        cp(s29),
                        cp(s30, s31, s32, s33, s34, s35),
                        cp(s36)),
                ChangePart.groupToMinimumGranularity(Arrays.asList(
                        s1,
                        s2,
                        s3,
                        s4,
                        s5,
                        s6,
                        s7,
                        s8,
                        s9,
                        s10,
                        s11,
                        s12,
                        s13,
                        s14,
                        s15,
                        s16,
                        s17,
                        s18,
                        s19,
                        s20,
                        s21,
                        s22,
                        s23,
                        s24,
                        s25,
                        s26,
                        s27,
                        s28,
                        s29,
                        s30,
                        s31,
                        s32,
                        s33,
                        s34,
                        s35,
                        s36)));
    }

}
