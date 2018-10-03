package com.ge.predix.audit.sdk.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.logging.Level;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { LoggerUtils.class })
public class LoggerUtilsTest {

    private static final CustomLogger aLog = LoggerUtils.getLogger("a");
    private static final CustomLogger bLog = LoggerUtils.getLogger("b");
    private static final String DEBUG = "AUDIT_DEBUG_ENABLED";

    @Test
    public void getLogger() {
        assertNotEquals(aLog, bLog);
        assertEquals(Level.WARNING, aLog.getLogLevel());
        assertEquals(Level.WARNING, aLog.getLogLevel());
    }

    @Test
    public void isDebugLogLevelDebugLevelIsFalse() {
        LoggerUtils.setLoggersLogLevel(Level.FINE);
        assertEquals(Level.FINE, aLog.getLogLevel());

        assertFalse(LoggerUtils.isDebugLogLevel());
    }

    @Test
    public void isDebugLogLevelDebugLevelIsTrue() {
        LoggerUtils.setLoggersLogLevel(Level.INFO);
        assertEquals(Level.INFO, aLog.getLogLevel());

        assertTrue(LoggerUtils.isDebugLogLevel());
    }

    @Test
    public void setLoggersLogLevel() {
        LoggerUtils.setLoggersLogLevel(Level.SEVERE);
        assertEquals(Level.SEVERE, aLog.getLogLevel());
    }

    @Test
    public void setLogLevelFromVcapDebugIsEnabled() {
        LoggerUtils.setLoggersLogLevel(Level.SEVERE);
        assertEquals(Level.SEVERE, aLog.getLogLevel());
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(DEBUG)).thenReturn("true");

        LoggerUtils.setLogLevelFromVcap();

        assertEquals(Level.INFO, aLog.getLogLevel());
        assertEquals(Level.INFO, aLog.getLogLevel());
    }

    @Test
    public void setLogLevelFromVcapDebugIsDisabled() {
        LoggerUtils.setLoggersLogLevel(Level.SEVERE);
        assertEquals(Level.SEVERE, aLog.getLogLevel());
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getenv(DEBUG)).thenReturn("false");

        LoggerUtils.setLogLevelFromVcap();

        assertEquals(Level.WARNING, aLog.getLogLevel());
        assertEquals(Level.WARNING, aLog.getLogLevel());
    }

    @Test
    public void nullArgsFailNothing() {
        aLog.logWithPrefix(Level.WARNING, null, null);
        aLog.logWithPrefix(Level.WARNING, null, null, null);
    }

}