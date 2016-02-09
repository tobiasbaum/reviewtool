package de.setsoftware.reviewtool.plugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class Logger {

	public static void info(String message) {
		Activator.getDefault().getLog().log(
				new Status(IStatus.INFO, Activator.getDefault().getBundle().getSymbolicName(), message));
	}

}
