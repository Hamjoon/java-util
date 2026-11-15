package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getSystemLoadAverage_Test {

    /**
     * Supplies a stream of double values for {@link #testGetSystemLoadAverage_VariousValues(double)}.
     */
    private static Stream<Double> loadAverageProvider() {
        return Stream.of(// zero load
        0.0, // typical positive load
        1.42, // sentinel meaning “not available”
        -1.0);
    }

    /**
     * Verify that {@link SystemUtilities#getSystemLoadAverage()} returns the value
     * provided by the underlying {@link OperatingSystemMXBean}.
     * The static call to {@link ManagementFactory#getOperatingSystemMXBean()} is
     * mocked using Mockito's inline static mocking capability.
     */
    @Test
    void testGetSystemLoadAverage_MockedValue() {
        // Arrange
        double expectedLoad = 2.73;
        OperatingSystemMXBean mockBean = mock(OperatingSystemMXBean.class);
        when(mockBean.getSystemLoadAverage()).thenReturn(expectedLoad);
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            mockedFactory.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(mockBean);
            // Act
            double actualLoad = SystemUtilities.getSystemLoadAverage();
            // Assert
            assertEquals(expectedLoad, actualLoad, "The load average returned by SystemUtilities should match the mocked value.");
        }
    }

    /**
     * Verify the fallback behaviour when the platform does not provide a load average.
     * The JDK specification states that {@code getSystemLoadAverage()} may return
     * {@code -1.0} if the information is unavailable.
     */
    @Test
    void testGetSystemLoadAverage_Default() {
        // Act
        double load = SystemUtilities.getSystemLoadAverage();
        // Assert – the value must be >= -1.0 per the JDK contract
        assertTrue(load >= -1.0, "System load average should be >= -1.0 (‑1 indicates unavailable).");
    }

    /**
     * Parameterized test that checks a variety of legitimate return values,
     * including the platform‑specific “not available” sentinel {@code -1.0}.
     *
     * This covers both the standard positive/zero case and the branch where the
     * MXBean reports that the load average is unavailable.
     */
    @ParameterizedTest
    @MethodSource("loadAverageProvider")
    void testGetSystemLoadAverage_VariousValues(double mockedValue) {
        // Arrange
        OperatingSystemMXBean mockBean = mock(OperatingSystemMXBean.class);
        when(mockBean.getSystemLoadAverage()).thenReturn(mockedValue);
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            mockedFactory.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(mockBean);
            // Act
            double actual = SystemUtilities.getSystemLoadAverage();
            // Assert
            assertEquals(mockedValue, actual, () -> "Expected load average to be " + mockedValue + " as supplied by the mock.");
        }
    }

    /**
     * Verify that when the underlying MXBean throws a {@link RuntimeException},
     * the exception propagates unchanged.  This corresponds to the
     * “error handling branch (hypothetical)” described in the JSON test cases.
     */
    @Test
    void testGetSystemLoadAverage_ExceptionPropagation() {
        // Arrange
        OperatingSystemMXBean mockBean = mock(OperatingSystemMXBean.class);
        RuntimeException expectedException = new RuntimeException("Unexpected failure");
        when(mockBean.getSystemLoadAverage()).thenThrow(expectedException);
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            mockedFactory.when(ManagementFactory::getOperatingSystemMXBean).thenReturn(mockBean);
            // Act & Assert
            RuntimeException actual = assertThrows(RuntimeException.class, SystemUtilities::getSystemLoadAverage, "The method should let RuntimeException from the MXBean propagate.");
            assertSame(expectedException, actual, "The propagated exception should be the same instance thrown by the mock.");
        }
    }
}
