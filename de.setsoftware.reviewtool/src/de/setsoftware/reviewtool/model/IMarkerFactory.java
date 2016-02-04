package de.setsoftware.reviewtool.model;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

public interface IMarkerFactory {

	public abstract IMarker createMarker(Position pos) throws CoreException;

}
