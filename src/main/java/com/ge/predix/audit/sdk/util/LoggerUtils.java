package com.ge.predix.audit.sdk.util;

import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerUtils {
	
	private static final String AUDIT_DEBUG_ENABLED = "AUDIT_DEBUG_ENABLED";
	private static volatile Level logLevel = Level.WARNING;
	private static final List<CustomLogger> loggers = Collections.synchronizedList(new ArrayList<>());

	public static CustomLogger getLogger(String name) {
	    Logger log = Logger.getLogger(name);
        log.setLevel(logLevel);
        CustomLogger customLogger = new CustomLogger(log);
        loggers.add(customLogger);
        return  customLogger;
    }

	public static void setLogLevelFromVcap() {
		String debug = System.getenv(AUDIT_DEBUG_ENABLED);
        logLevel = Boolean.valueOf(debug)? Level.INFO : Level.WARNING; //Note that Boolean.valueOf(debug) doesn't throw an exception. it's TRUE only if isDebugLevel="true"
		setLoggersLogLevel(logLevel);
	}

	public static boolean isDebugLogLevel(){
		return logLevel.equals(Level.INFO);
	}

	public synchronized static void setLoggersLogLevel(Level level) {
	    logLevel = level;
	    loggers.forEach(logger -> logger.setLogLevel(level));
	}

	public static String generateLogPrefix(String auditZoneId) {
		return (StringUtil.isNullOrEmpty(auditZoneId)) ? "" : String.format("[azid {%s}]: ",auditZoneId);
	}

}
