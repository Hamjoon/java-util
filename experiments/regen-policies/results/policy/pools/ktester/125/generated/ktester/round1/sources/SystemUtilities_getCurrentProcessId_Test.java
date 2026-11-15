package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getCurrentProcessId_Test {

    /**
     * Provides a set of JVM name strings together with the expected PID result.
     * The expected value follows the contract of {@link SystemUtilities#getCurrentProcessId()}:
     * <ul>
     *   <li>Valid numeric PID → parsed long.</li>
     *   <li>Invalid format / overflow / whitespace → 0.</li>
     *   <li>Negative numbers are allowed and returned as‑is.</li>
     * </ul>
     */
    private static Stream<Arguments> pidParsingProvider() {
        return Stream.of(// normal case
        Arguments.of("12345@myhost", 12345L), // no '@' character – covered by a dedicated test, but kept for completeness
        Arguments.of("no-at-symbol", 0L), // '@' as first character
        Arguments.of("@hostname", 0L), // non‑numeric PID part
        Arguments.of("abcde@myhost", 0L), // overflow (Long.MAX_VALUE + 1)
        Arguments.of("9223372036854775808@host", 0L), // negative PID
        Arguments.of("-42@host", -42L), // leading whitespace before PID (parseLong does not trim)
        Arguments.of(" 123@host", 0L), // PID zero
        Arguments.of("0@host", 0L), // explicit plus sign
        Arguments.of("+123@host", 123L), // multiple '@' characters – only first matters
        Arguments.of("123@host@domain", 123L));
    }

    @Test
    void returnsParsedPidWhenNameIsWellFormed() {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn("12345@myhost");
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(12345L, pid, "Expected PID to be parsed correctly from the MXBean name");
        }
    }

    @Test
    void returnsZeroWhenNameDoesNotContainAtSymbol() {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn("no-at-symbol");
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(0L, pid, "Expected 0 when the MXBean name lacks the '@' delimiter");
        }
    }

    @Test
    void returnsZeroWhenPidPartIsNotANumber() {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn("abcde@myhost");
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(0L, pid, "Expected 0 when the PID part cannot be parsed as a number");
        }
    }

    @ParameterizedTest(name = "JVM name \"{0}\" → expected PID {1}")
    @MethodSource("pidParsingProvider")
    void testPidParsingVariousScenarios(String jvmName, long expectedPid) {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn(jvmName);
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act
            long actual = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(expectedPid, actual, () -> "Unexpected PID for JVM name \"" + jvmName + "\"");
        }
    }

    @Test
    void returnsZeroWhenAtSymbolIsFirstCharacter() {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn("@hostname");
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act
            long pid = SystemUtilities.getCurrentProcessId();
            // Assert
            assertEquals(0L, pid, "Expected 0 when '@' is the first character of the MXBean name");
        }
    }

    @Test
    void throwsNullPointerExceptionWhenJvmNameIsNull() {
        // Arrange
        RuntimeMXBean mockBean = mock(RuntimeMXBean.class);
        when(mockBean.getName()).thenReturn(null);
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenReturn(mockBean);
            // Act & Assert
            assertThrows(NullPointerException.class, SystemUtilities::getCurrentProcessId, "Expected NullPointerException when MXBean name is null");
        }
    }

    @Test
    void propagatesSecurityExceptionFromRuntimeMxBeanAccess() {
        // Arrange: mock static to throw SecurityException when getRuntimeMXBean() is called
        try (MockedStatic<ManagementFactory> mocked = Mockito.mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenThrow(new SecurityException("access denied"));
            // Act & Assert
            assertThrows(SecurityException.class, SystemUtilities::getCurrentProcessId, "Expected SecurityException to propagate when access to RuntimeMXBean is denied");
        }
    }
}
