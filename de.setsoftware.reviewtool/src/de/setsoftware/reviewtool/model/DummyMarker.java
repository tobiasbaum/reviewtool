package de.setsoftware.reviewtool.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class DummyMarker implements IMarker {

    public static final IMarkerFactory FACTORY = new IMarkerFactory() {
        @Override
        public IMarker createMarker(Position pos) {
            return new DummyMarker("");
        }
    };

    private final HashMap<String, Object> attributes = new HashMap<>();
    private final String type;

    public DummyMarker(String type) {
        this.type = type;
    }

    @Override
    public Object getAdapter(Class adapter) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void delete() throws CoreException {
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public Object getAttribute(String attributeName) throws CoreException {
        return this.attributes.get(attributeName);
    }

    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        return this.attributes.containsKey(attributeName)
                ? (Integer) this.attributes.get(attributeName)
                        : defaultValue;
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        return this.attributes.containsKey(attributeName)
                ? (String) this.attributes.get(attributeName)
                        : defaultValue;
    }

    @Override
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        return this.attributes.containsKey(attributeName)
                ? (Boolean) this.attributes.get(attributeName)
                        : defaultValue;
    }

    @Override
    public Map<String, Object> getAttributes() throws CoreException {
        return this.attributes;
    }

    @Override
    public Object[] getAttributes(String[] attributeNames) throws CoreException {
        final Object[] ret = new Object[attributeNames.length];
        for (int i = 0; i < attributeNames.length; i++) {
            ret[i] = this.attributes.get(attributeNames[i]);
        }
        return ret;
    }

    @Override
    public void setAttribute(String attributeName, int value) throws CoreException {
        this.attributes.put(attributeName, value);
    }

    @Override
    public void setAttribute(String attributeName, Object value) throws CoreException {
        this.attributes.put(attributeName, value);
    }

    @Override
    public void setAttribute(String attributeName, boolean value) throws CoreException {
        this.attributes.put(attributeName, value);
    }

    @Override
    public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
        for (int i = 0; i < attributeNames.length; i++) {
            this.attributes.put(attributeNames[i], values[i]);
        }
    }

    @Override
    public void setAttributes(Map<String, ? extends Object> attributes) throws CoreException {
        this.attributes.putAll(attributes);
    }

    @Override
    public long getCreationTime() throws CoreException {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public long getId() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public IResource getResource() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public String getType() throws CoreException {
        return this.type;
    }

    @Override
    public boolean isSubtypeOf(String superType) throws CoreException {
        throw new RuntimeException("not yet implemented");
    }

}
