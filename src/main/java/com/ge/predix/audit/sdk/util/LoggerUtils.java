package com.ge.predix.audit.sdk.util;

import java.util.logging.Level;

import com.ge.predix.audit.sdk.*;
import com.ge.predix.audit.sdk.config.vcap.VcapLoaderServiceImpl;
import com.ge.predix.audit.sdk.message.tracing.TracingMessageSenderImpl;
import com.ge.predix.audit.sdk.validator.ValidatorServiceImpl;

public class LoggerUtils {
	
	private static final String AUDIT_DEBUG_ENABLED = "AUDIT_DEBUG_ENABLED";

	private static volatile boolean isDebugLevel = Boolean.FALSE;

	public static void setLogLevelFromVcap() {
		Level level = Level.WARNING;
		String debug = System.getenv(AUDIT_DEBUG_ENABLED);
		
		if(Boolean.valueOf(debug)) { // it doesn't throw an exception. it's TRUE only if isDebugLevel="true", otherwise it's false
			level = Level.INFO;
			isDebugLevel = Boolean.TRUE;
        }
		setLoggersLogLevel(level);
	}

	public static boolean isDebugLogLevel(){
		return isDebugLevel;
	}

	public static void setLoggersLogLevel(Level level) {
		AbstractAuditClientImpl.getLog().setLevel(level);
		AuditClientAsyncImpl.getLog().setLevel(level);
		AuditClientSyncImpl.getLog().setLevel(level);
		DirectMemoryMonitor.getLog().setLevel(level);
		TracingHandlerImpl.getLog().setLevel(level);
		TracingMessageSenderImpl.getLog().setLevel(level);
		ValidatorServiceImpl.getLog().setLevel(level);
		VcapLoaderServiceImpl.getLog().setLevel(level);
	}
}
