package de.setsoftware.reviewtool.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.IStopViewer;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IChangeClassifier;
import de.setsoftware.reviewtool.preferredtransitions.api.IPreferredTransitionStrategy;
import de.setsoftware.reviewtool.ui.dialogs.extensions.EndReviewExtension;

/**
 * Tests for {@link ConfigurationInterpreter}.
 */
public class ConfigurationInterpreterTest {

    /**
     * A stub implementation of {@link IConfigurator} that accumulates
     * the configuration data for a set of configured element names.
     */
    private static final class TestConfigurator implements IConfigurator {
        private final Set<String> elementNames;
        private final List<String> result = new ArrayList<>();

        public TestConfigurator(String... elementNames) {
            this.elementNames = new HashSet<>(Arrays.asList(elementNames));
        }

        @Override
        public Set<String> getRelevantElementNames() {
            return this.elementNames;
        }

        @Override
        public void configure(Element xml, IReviewConfigurable configurable) {
            this.addAllAttributesToResult(xml);
        }

        private void addAllAttributesToResult(Element xml) {
            final NamedNodeMap atts = xml.getAttributes();
            for (int i = 0; i < atts.getLength(); i++) {
                final Attr att = (Attr) atts.item(i);
                this.result.add(xml.getNodeName() + "." + att.getName() + "=" + att.getValue());
            }

            final NodeList children = xml.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (child instanceof Element) {
                    this.addAllAttributesToResult((Element) child);
                }
            }
        }

        public List<String> getResult() {
            return this.result;
        }

    }

    /**
     * A stub implementation of {@link IReviewConfigurable}.
     */
    private static final class TestConfigurable implements IReviewConfigurable {

        @Override
        public void setPersistence(IReviewPersistence persistence) {
        }

        @Override
        public void addChangeSource(IChangeSource changeSource) {
        }

        @Override
        public void addEndReviewExtension(EndReviewExtension extension) {
        }

        @Override
        public void setStopViewer(IStopViewer stopViewer) {
        }

        @Override
        public void addPostInitTask(Runnable r) {
        }

        @Override
        public void addPreferredTransitionStrategy(IPreferredTransitionStrategy strategy) {
        }

        @Override
        public void addClassificationStrategy(IChangeClassifier strategy) {
        }

    }

    private static Document load(String xml) throws Exception {
        final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    @Test
    public void testGetUserSpecificParamNames() throws Exception {
        final Set<String> paramNames = new ConfigurationInterpreter().getUserSpecificParamNames(load(
                "<reviewToolConfig>"
                        + "  <someElement att1=\"abc\" att2=\"${def}\" />"
                        + "  <anotherElement att1=\"a$$bc\" att2=\"xy${p1}z${p2}\" />"
                        + "  <aThirdElement>"
                        + "    <withAChildElement att3=\"${def}\" att4=\"${foo}\" />"
                        + "  </aThirdElement>"
                        + "</reviewToolConfig>"));

        assertEquals(new LinkedHashSet<>(Arrays.asList("User-ID", "def", "p1", "p2", "foo")),
                paramNames);
    }

    @Test
    public void testConfigure() throws Exception {
        final Document config = load(
                "<reviewToolConfig>"
                        + "  <someElement att1=\"abc\" att2=\"${def}\" />"
                        + "  <anotherElement att1=\"a$$bc\" att2=\"xy${p1}z${p2}\" />"
                        + "  <aThirdElement>"
                        + "    <withAChildElement att3=\"${def}\" att4=\"${foo}\" />"
                        + "  </aThirdElement>"
                        + "</reviewToolConfig>");

        final Map<String, String> paramValues = new HashMap<>();
        paramValues.put("def", "Wert 1");
        paramValues.put("p1", "X");
        paramValues.put("p2", "Y");
        paramValues.put("foo", "bar");

        final TestConfigurator tc1 = new TestConfigurator("someElement");
        final TestConfigurator tc2 = new TestConfigurator("anotherElement");
        final TestConfigurator tc3 = new TestConfigurator("aThirdElement");
        final TestConfigurator tc4 = new TestConfigurator("someElement", "aThirdElement");

        final ConfigurationInterpreter i = new ConfigurationInterpreter();
        i.addConfigurator(tc1);
        i.addConfigurator(tc2);
        i.addConfigurator(tc3);
        i.addConfigurator(tc4);
        i.configure(config, paramValues, new TestConfigurable());

        assertEquals(Arrays.asList("someElement.att1=abc", "someElement.att2=Wert 1"),
                tc1.getResult());
        assertEquals(Arrays.asList("anotherElement.att1=a$bc", "anotherElement.att2=xyXzY"),
                tc2.getResult());
        assertEquals(Arrays.asList("withAChildElement.att3=Wert 1", "withAChildElement.att4=bar"),
                tc3.getResult());
        assertEquals(Arrays.asList("someElement.att1=abc", "someElement.att2=Wert 1",
                "withAChildElement.att3=Wert 1", "withAChildElement.att4=bar"),
                tc4.getResult());
    }

    @Test
    public void testExceptionOnMissingValue() throws Exception {
        final Document config = load(
                "<reviewToolConfig>"
                        + "  <someElement att1=\"abc\" att2=\"${def}\" />"
                        + "</reviewToolConfig>");

        final Map<String, String> paramValues = new HashMap<>();

        final ConfigurationInterpreter i = new ConfigurationInterpreter();
        try {
            i.configure(config, paramValues, new TestConfigurable());
            fail("Exception expected because value for param def is missing");
        } catch (final ReviewtoolException e) {
            assertTrue("wrong exception message: " + e.getMessage(), e.getMessage().contains("def"));
        }
    }

}
