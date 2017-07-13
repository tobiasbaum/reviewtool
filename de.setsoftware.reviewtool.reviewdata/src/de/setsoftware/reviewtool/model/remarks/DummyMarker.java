package de.setsoftware.reviewtool.model.remarks;

import java.util.HashMap;

/**
 * A dummy marker that is not represented in Eclipse.
 */
public class DummyMarker implements IReviewMarker {

    public static final IMarkerFactory FACTORY = new IMarkerFactory() {
        @Override
        public IReviewMarker createMarker(Position pos) {
            return new DummyMarker();
        }

        @Override
        public IReviewMarker createMarker(IReviewResource resource) {
            return new DummyMarker();
        }
    };

    private final HashMap<String, Object> attributes = new HashMap<>();

    public DummyMarker() {
    }

    @Override
    public void delete() throws ReviewRemarkException {
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        return this.attributes.containsKey(attributeName)
                ? (String) this.attributes.get(attributeName)
                        : defaultValue;
    }

    @Override
    public void setAttribute(String attributeName, int value) {
        this.attributes.put(attributeName, value);
    }

    @Override
    public void setAttribute(String attributeName, String value) {
        this.attributes.put(attributeName, value);
    }

    @Override
    public void setMessage(String newText) throws ReviewRemarkException {
        this.setAttribute("__message", newText);
    }

    @Override
    public String getMessage() throws ReviewRemarkException {
        return this.getAttribute("__message", "");
    }

    @Override
    public void setLineNumber(int line) {
        this.setAttribute("__line", line);
    }

    @Override
    public void setSeverityInfo() {
        this.setAttribute("__severity", "info");
    }

    @Override
    public void setSeverityWarning() {
        this.setAttribute("__severity", "warning");
    }

}
