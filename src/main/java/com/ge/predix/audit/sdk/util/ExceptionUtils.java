package com.ge.predix.audit.sdk.util;


import com.google.common.base.Throwables;

import java.util.Optional;
import java.util.logging.Level;

public class ExceptionUtils {

    private static CustomLogger log = LoggerUtils.getLogger(ExceptionUtils.class.getName());

    @FunctionalInterface
    public interface ExceptionalCodeBlock {
        void invoke() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static void wrapCheckedException(ExceptionUtils.ExceptionalCodeBlock codeBlock) {
        try {
            codeBlock.invoke();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void swallowException(ExceptionalCodeBlock codeBlock, String errorMsg) {
        try {
            codeBlock.invoke();
        } catch (Throwable t) {
            log.log(Level.WARNING, t,"Exception occurred, Context: %s Error: %s", errorMsg, t.getMessage());
        }
    }

    public static <T> Optional<T> swallowSupplierException(ThrowingSupplier<T> supplier, String errorMsg, Level level) {
        T result = null;
        try {
            result = supplier.get();
        } catch (Throwable t) {
            log.log(level, t,"Exception occurred, Context: %s Error: %s", errorMsg, t.getMessage());
        }
        return Optional.ofNullable(result);
    }

    public static String toString(Throwable t) {
        return String.format("Exception - %s, message: %s Cause: %s Stacktrace: %s",
                t.getClass(), t.getMessage(), t.getCause(), Throwables.getStackTraceAsString(t));
    }
}
