package com.ge.predix.audit.sdk.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.logging.Level;
import java.util.logging.Logger;

@AllArgsConstructor
@EqualsAndHashCode
public class CustomLogger {

    private final Logger logger;

    public void warning(String s, Object... args) {
        logger.warning(String.format(s, args));
    }

    public void warning(String s) {
        logger.warning(s);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void info(String s, Object... args) {
        logger.info(String.format(s, args));
    }


    public void severe(String s, Object... args) {
        logger.severe(String.format(s, args));
    }

    public void severe(String s, Throwable t) {
        logger.log(Level.SEVERE, s, t);
    }

    public void logWithPrefix(Level level,String prefix, String message, Object... args) {
        logger.log(level, String.format(prefix+message, args));
    }

    public void logWithPrefix(Level level, Throwable t, String prefix, String message, Object... args) {
        logger.log(level, String.format(prefix+message, args), t);
    }

    public void log(Level level, Throwable e, String s) {
        logger.log(level, s, e);
    }

    public void log(Level level, Throwable e, String msg, Object... args) {
        logger.log(level, String.format(msg, args), e);
    }

    public void setLogLevel(Level level) {
        logger.setLevel(level);
    }

    public Level getLogLevel() {
        return logger.getLevel();
    }

}
