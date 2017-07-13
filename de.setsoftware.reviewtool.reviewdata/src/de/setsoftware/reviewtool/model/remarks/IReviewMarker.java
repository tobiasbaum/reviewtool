package de.setsoftware.reviewtool.model.remarks;

/**
 * A marker for a review remark in the UI.
 */
public interface IReviewMarker {

    public abstract void delete() throws ReviewRemarkException;

    public abstract void setMessage(String newText) throws ReviewRemarkException;

    public abstract String getMessage() throws ReviewRemarkException;

    public abstract void setAttribute(String attributeName, int value) throws ReviewRemarkException;

    public abstract void setAttribute(String attributeName, String value) throws ReviewRemarkException;

    public abstract String getAttribute(String attributeName, String defaultValue) throws ReviewRemarkException;

    public abstract void setLineNumber(int line);

    public abstract void setSeverityInfo();

    public abstract void setSeverityWarning();

}
