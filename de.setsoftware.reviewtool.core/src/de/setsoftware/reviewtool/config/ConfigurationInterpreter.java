package de.setsoftware.reviewtool.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Class to interpret a configuration file.
 */
public class ConfigurationInterpreter {

    /**
     * Parameter name for the user ID that always has to be set.
     */
    public static final String USER_PARAM_NAME = "User-ID";

    private final List<IConfigurator> configurators = new ArrayList<>();

    public void addConfigurator(IConfigurator configurator) {
        this.configurators.add(configurator);
    }

    /**
     * Determines the placeholders used for user specific values in the given
     * configuration document.
     */
    public Set<String> getUserSpecificParamNames(Document configuration) {
        final Set<String> ret = new LinkedHashSet<>();
        ret.add(USER_PARAM_NAME);
        this.getUserSpecificParamNamesRec(configuration.getDocumentElement(), ret);
        return ret;
    }

    private void getUserSpecificParamNamesRec(Element element, Set<String> nameBuffer) {
        final NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node n = nodes.item(i);
            final NamedNodeMap atts = n.getAttributes();
            if (atts != null) {
                for (int j = 0; j < atts.getLength(); j++) {
                    this.parseParamNamesFromValue(atts.item(j).getNodeValue(), nameBuffer);
                }
            }
            if (n instanceof Element) {
                this.getUserSpecificParamNamesRec((Element) n, nameBuffer);
            }
        }
    }

    private void parseParamNamesFromValue(String nodeValue, Set<String> ret) {
        final List<String> parts = this.parseValue(nodeValue);
        for (int i = 1; i < parts.size(); i += 2) {
            ret.add(parts.get(i));
        }
    }

    /**
     * States for the parser that separates values into parameter names and normal text.
     */
    private static enum ParseState {
        OUT,
        FIRST,
        IN
    }

    /**
     * Splits the given attribute value into parts so that every element in the
     * returned list with an even index is text and every element with an odd index
     * is a parameter name.
     */
    private List<String> parseValue(String value) {
        final List<String> ret = new ArrayList<>();
        final StringBuilder curPart = new StringBuilder();
        ParseState state = ParseState.OUT;
        for (final char ch : value.toCharArray()) {
            switch (state) {
            case OUT:
                if (ch == '$') {
                    state = ParseState.FIRST;
                } else {
                    curPart.append(ch);
                }
                break;
            case FIRST:
                if (ch == '$') {
                    //escaped dollar sign
                    curPart.append('$');
                    state = ParseState.OUT;
                } else if (ch == '{') {
                    //begin of a parameter
                    ret.add(curPart.toString());
                    curPart.setLength(0);
                    state = ParseState.IN;
                } else {
                    throw new ReviewtoolException("Syntax error $" + ch + " in " + value);
                }
                break;
            case IN:
                if (ch == '}') {
                    ret.add(curPart.toString());
                    curPart.setLength(0);
                    state = ParseState.OUT;
                } else {
                    curPart.append(ch);
                }
                break;
            default:
                throw new AssertionError();
            }
        }
        if (state != ParseState.OUT) {
            throw new ReviewtoolException("Open parameter name in " + value);
        }
        ret.add(curPart.toString());
        return ret;
    }

    /**
     * Configures the given configurable based on the given configuration.
     */
    public void configure(Document configuration, Map<String, String> paramValues, IReviewConfigurable configurable) {
        final Element withParamsReplaced = (Element) configuration.getDocumentElement().cloneNode(true);
        this.replaceParams(withParamsReplaced, paramValues);
        this.callConfigurators(configurable, withParamsReplaced);
    }

    private void replaceParams(Element element, Map<String, String> paramValues) {
        final NamedNodeMap atts = element.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
            final Attr att = (Attr) atts.item(i);
            att.setValue(this.replaceParamsInValue(att.getValue(), paramValues));
        }

        final NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child instanceof Element) {
                this.replaceParams((Element) child, paramValues);
            }
        }
    }

    private String replaceParamsInValue(String value, Map<String, String> paramValues) {
        final StringBuilder ret = new StringBuilder();
        final List<String> parts = this.parseValue(value);
        for (int i = 0; i < parts.size(); i++) {
            if (i % 2 == 0) {
                ret.append(parts.get(i));
            } else {
                final String paramName = parts.get(i);
                final String paramValue = paramValues.get(paramName);
                if (paramValue == null) {
                    throw new ReviewtoolException("Value for user specific parameter " + paramName + " missing.");
                }
                ret.append(paramValue);
            }
        }
        return ret.toString();
    }

    private void callConfigurators(IReviewConfigurable configurable, final Element rootElement) {
        final NodeList children = rootElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            for (final IConfigurator configurator : this.configurators) {
                if (configurator.getRelevantElementNames().contains(child.getNodeName())) {
                    final Element clone = (Element) child.cloneNode(true);
                    configurator.configure(clone, configurable);
                }
            }
        }
    }

    /**
     * Helper method to load an XML file.
     */
    public static Document load(String filename) throws IOException, SAXException, ParserConfigurationException {
        final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        try (InputStream stream = new FileInputStream(filename)) {
            return f.newDocumentBuilder().parse(new InputSource(stream));
        }
    }

}
