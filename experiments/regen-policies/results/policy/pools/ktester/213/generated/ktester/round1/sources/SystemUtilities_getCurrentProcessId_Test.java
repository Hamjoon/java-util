package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getCurrentProcessId_Test {

    /**
     * Supplies the different JVM name scenarios together with the expected PID.
     *
     * @return a stream of arguments for {@link #testGetCurrentProcessId(String, long, String)}
     */
    private static Stream<Arguments> provideJvmNames() {
        return Stream.of(// Valid PID format – numeric part before '@' should be returned.
        Arguments.of("12345@myhost", 12345L, "Valid JVM name format \"12345@hostname\" – the method should parse the numeric part before '@' and return it as the process id."), // Missing '@' separator – method should return 0.
        Arguments.of("hostnameOnly", 0L, "JVM name without the '@' separator (e.g., \"hostnameOnly\") – index of '@' is -1, so the method should return 0."), // Non‑numeric PID – parsing fails, method should return 0.
        Arguments.of("abcde@host", 0L, "JVM name with '@' but a non‑numeric pid prefix (e.g., \"abcde@host\") – parsing throws NumberFormatException, caught and the method returns 0."));
    }

    @Test
    void testValidPidIsReturned() {
        // Arrange – mock the static call to ManagementFactory
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
            mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            when(mockBean.getName()).thenReturn("12345@myhost");
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(12345L, pid, "The PID extracted from a valid name should be returned.");
        }
    }

    @Test
    void testMissingAtSymbolReturnsZero() {
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
            mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Name without '@' – index will be -1
            when(mockBean.getName()).thenReturn("no-at-symbol");
            long pid = SystemUtilities.getCurrentProcessId();
            assertEquals(0L, pid, "When the name does not contain '@', the method should return 0.");
        }
    }

    @Test
    void testNonNumericPidReturnsZero() {
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
            mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Name with non‑numeric part before '@'
            when(mockBean.getName()).thenReturn("abcde@host");
            long pid = SystemUtilities.getCurrentProcessId();
            assertEquals(0L, pid, "When the PID portion cannot be parsed as a number, the method should return 0.");
        }
    }

    /**
     * Parameterized test that covers all cases described in the JSON test data.
     *
     * @param jvmName   the value that {@link RuntimeMXBean#getName()} should return
     * @param expected  the PID the method is expected to produce
     * @param description a human‑readable description of the scenario (used only for readability)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideJvmNames")
    void testGetCurrentProcessId(String jvmName, long expected, String description) {
        // Arrange – mock the static call to ManagementFactory.getRuntimeMXBean()
        try (MockedStatic<ManagementFactory> mockedFactory = Mockito.mockStatic(ManagementFactory.class)) {
            RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
            mockedFactory.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            when(mockBean.getName()).thenReturn(jvmName);
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(expected, pid, () -> "For JVM name \"" + jvmName + "\": " + description);
        }
    }
}
