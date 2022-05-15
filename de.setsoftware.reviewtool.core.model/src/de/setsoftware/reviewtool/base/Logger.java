package de.setsoftware.reviewtool.base;

import java.util.function.Supplier;

/**
 * Wrapper for the Eclipse logging mechanism.
 */
public abstract class Logger {

    private static Logger instance = new Logger() {
        @Override
        protected void log(int status, String message, Throwable exception) {
        }
        
        @Override
        protected void log(int status, String message) {
        }
    };
    
    private static volatile boolean verbose;

    public static void error(String message, Throwable exception) {
        instance.log(4, message, exception);
    }

    public static void warn(String message, Throwable exception) {
        instance.log(2, message, exception);
    }

    public static void info(String message) {
        instance.log(1, message);
    }

    public static void debug(String message) {
        instance.log(0, message);
    }

    /**
     * Log the string returned by the given supplier iff verbose logging is active.
     */
    public static void verboseDebug(Supplier<String> message) {
        if (verbose) {
            debug(message.get());
        }
    }

    protected abstract void log(int status, String message);

    protected abstract void log(int status, String message, Throwable exception);

    public static void setLogger(Logger logger) {
        instance = logger;
    }

    public static void setVerbose() {
        verbose = true;
    }

}
