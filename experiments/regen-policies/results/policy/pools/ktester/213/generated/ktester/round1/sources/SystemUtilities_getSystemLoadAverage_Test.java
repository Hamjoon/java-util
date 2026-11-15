package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getSystemLoadAverage_Test {

    /**
     * Holds the static mock for {@link ManagementFactory} when needed.
     */
    private MockedStatic<ManagementFactory> managementFactoryMock;

    /**
     * Mocked OperatingSystemMXBean used in the tests.
     */
    private OperatingSystemMXBean osMxBeanMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid cross‑test contamination.
        osMxBeanMock = mock(OperatingSystemMXBean.class);
        managementFactoryMock = Mockito.mockStatic(ManagementFactory.class);
        // By default the static method returns our mock bean.
        managementFactoryMock.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(osMxBeanMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so that other test classes are not affected.
        if (managementFactoryMock != null) {
            managementFactoryMock.close();
        }
    }

    @Test
    void testGetSystemLoadAverage_ForwardsCallAndReturnsValue() {
        // GIVEN – the MXBean will return a specific load average.
        double expectedLoad = 1.23;
        when(osMxBeanMock.getSystemLoadAverage()).thenReturn(expectedLoad);
        // WHEN – the utility method is invoked.
        double actualLoad = SystemUtilities.getSystemLoadAverage();
        // THEN – the value from the MXBean is returned unchanged.
        assertEquals(expectedLoad, actualLoad, "SystemUtilities should return the exact value provided by the MXBean");
        // Verify that the MXBean method was indeed called exactly once.
        verify(osMxBeanMock, times(1)).getSystemLoadAverage();
    }

    @Test
    void testGetSystemLoadAverage_WhenMxBeanReturnsMinusOne() {
        // GIVEN – some platforms report -1 when the load average is unavailable.
        when(osMxBeanMock.getSystemLoadAverage()).thenReturn(-1.0);
        // WHEN
        double load = SystemUtilities.getSystemLoadAverage();
        // THEN – the wrapper propagates the sentinel value unchanged.
        assertEquals(-1.0, load, "SystemUtilities should propagate -1.0 when the MXBean reports it");
    }

    @Test
    void testGetSystemLoadAverage_HandlesUnexpectedExceptionGracefully() {
        // GIVEN – the MXBean throws a RuntimeException (unlikely but possible).
        when(osMxBeanMock.getSystemLoadAverage()).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN – the exception propagates; this test ensures the method does not
        // swallow it silently (the contract is to return whatever the MXBean returns).
        RuntimeException ex = assertThrows(RuntimeException.class, SystemUtilities::getSystemLoadAverage, "Unexpected exceptions should not be caught inside the utility method");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testGetSystemLoadAverage_PropagatesRuntimeExceptionFromMxBean() {
        // GIVEN – the MXBean throws a RuntimeException (e.g., IllegalStateException).
        when(osMxBeanMock.getSystemLoadAverage()).thenThrow(new IllegalStateException("bean failure"));
        // WHEN / THEN – the exception propagates unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, SystemUtilities::getSystemLoadAverage, "The wrapper must not swallow RuntimeExceptions from the MXBean");
        assertEquals("bean failure", ex.getMessage());
    }

    @Test
    void testGetSystemLoadAverage_HandlesSecurityExceptionFromManagementFactory() {
        // GIVEN – a SecurityException is thrown when trying to obtain the MXBean.
        // close previous mock
        managementFactoryMock.close();
        managementFactoryMock = Mockito.mockStatic(ManagementFactory.class);
        managementFactoryMock.when(ManagementFactory::getOperatingSystemMXBean).thenThrow(new SecurityException("access denied"));
        // WHEN / THEN – the SecurityException propagates.
        SecurityException ex = assertThrows(SecurityException.class, SystemUtilities::getSystemLoadAverage, "SecurityException should propagate from ManagementFactory");
        assertEquals("access denied", ex.getMessage());
    }

    @Test
    void testGetSystemLoadAverage_WhenManagementFactoryReturnsNull() {
        // GIVEN – ManagementFactory returns null (should never happen in practice).
        managementFactoryMock.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(null);
        // WHEN / THEN – a NullPointerException (or IllegalStateException) is thrown.
        // The exact type is not defined by the contract, so we accept either.
        Exception ex = assertThrows(Exception.class, SystemUtilities::getSystemLoadAverage, "Calling getSystemLoadAverage with a null MXBean should fail");
        assertTrue(ex instanceof NullPointerException || ex instanceof IllegalStateException, "Expected NullPointerException or IllegalStateException, but got " + ex);
    }

    @Test
    void testGetSystemLoadAverage_UnexpectedRuntimeExceptionFromMxBean() {
        // GIVEN – the MXBean throws a generic RuntimeException.
        when(osMxBeanMock.getSystemLoadAverage()).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN – the exception propagates; the method does not swallow it.
        RuntimeException ex = assertThrows(RuntimeException.class, SystemUtilities::getSystemLoadAverage, "Unexpected exceptions should not be caught inside the utility method");
        assertEquals("boom", ex.getMessage());
    }
}
