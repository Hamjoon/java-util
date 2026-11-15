package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_hasAvailableMemory_Test {

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final long requiredBytes;

        final long freeMemory;

        final boolean expected;

        final String description;

        TestCase(long requiredBytes, long freeMemory, boolean expected, String description) {
            this.requiredBytes = requiredBytes;
            this.freeMemory = freeMemory;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            // JUnit uses this for the display name when no explicit name is given.
            return description;
        }
    }

    /**
     * Supplies all test cases derived from the JSON document.
     */
    private static Stream<TestCase> provideHasAvailableMemoryCases() {
        return Stream.of(// -----------------------------------------------------------------
        // Group: free memory greater than required (including edge cases)
        // -----------------------------------------------------------------
        new TestCase(// requiredBytes = 1 KB
        1024L, // freeMemory = 10 KB
        10 * 1024L, true, "Free memory (10 KB) > required (1 KB) → true"), new TestCase(0L, // any non‑negative free memory
        5 * 1024L, true, "requiredBytes = 0 → always true"), new TestCase(-500L, 1 * 1024L, true, "Negative requiredBytes should be satisfied"), new TestCase(// required = 2 KB
        2048L, // freeMemory exactly 2 KB
        2048L, true, "Free memory equals required (2 KB) → true"), // -----------------------------------------------------------------
        // Group: free memory less than required
        // -----------------------------------------------------------------
        new TestCase(// required = 4 KB
        4096L, // freeMemory = 2 KB
        2 * 1024L, false, "Free memory (2 KB) < required (4 KB) → false"), new TestCase(1L, 0L, false, "Free memory = 0, required > 0 → false"), // -----------------------------------------------------------------
        // Group: zero free memory edge case
        // -----------------------------------------------------------------
        new TestCase(12345L, 0L, false, "Zero free memory, positive required → false"), // -----------------------------------------------------------------
        // Group: extreme boundary values
        // -----------------------------------------------------------------
        new TestCase(Long.MAX_VALUE, Long.MAX_VALUE, true, "Free memory = Long.MAX_VALUE, required = Long.MAX_VALUE → true"), new TestCase(Long.MAX_VALUE, Long.MAX_VALUE - 1, false, "Free memory = Long.MAX_VALUE‑1, required = Long.MAX_VALUE → false"), new TestCase(// negative huge requirement
        Long.MIN_VALUE, 0L, true, "Negative requirement (Long.MIN_VALUE) always satisfied"));
    }

    @Test
    void testHasAvailableMemoryWhenEnoughFreeMemory() {
        // Arrange: free memory (512) >= required (256)
        SystemUtilities.MemoryInfo mockInfo = new SystemUtilities.MemoryInfo(1024L, 512L, 2048L);
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(mockInfo);
            // Act
            boolean result = SystemUtilities.hasAvailableMemory(256L);
            // Assert
            assertTrue(result, "Expected true when free memory is sufficient");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void testHasAvailableMemoryWhenInsufficientFreeMemory() {
        // Arrange: free memory (128) < required (256)
        SystemUtilities.MemoryInfo mockInfo = new SystemUtilities.MemoryInfo(1024L, 128L, 2048L);
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(mockInfo);
            // Act
            boolean result = SystemUtilities.hasAvailableMemory(256L);
            // Assert
            assertFalse(result, "Expected false when free memory is insufficient");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void testHasAvailableMemoryWithZeroRequirement() {
        // Arrange: any free memory is enough for a zero‑byte request
        SystemUtilities.MemoryInfo mockInfo = new SystemUtilities.MemoryInfo(1024L, 0L, 2048L);
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(mockInfo);
            // Act
            boolean result = SystemUtilities.hasAvailableMemory(0L);
            // Assert
            assertTrue(result, "Zero byte requirement should always be satisfied");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void testHasAvailableMemoryWithNegativeRequirement() {
        // Arrange: negative requirement should be treated as satisfied
        SystemUtilities.MemoryInfo mockInfo = new SystemUtilities.MemoryInfo(1024L, 10L, 2048L);
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(mockInfo);
            // Act
            boolean result = SystemUtilities.hasAvailableMemory(-100L);
            // Assert
            assertTrue(result, "Negative byte requirement should be considered satisfied");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideHasAvailableMemoryCases")
    void testHasAvailableMemoryParameterized(TestCase tc) {
        // Arrange: create a MemoryInfo instance with the freeMemory defined in the case.
        // The total and max values are irrelevant for the method under test, so we
        // pick simple values that are >= freeMemory.
        SystemUtilities.MemoryInfo mockInfo = new SystemUtilities.MemoryInfo(// totalMemory (just a placeholder)
        tc.freeMemory + 1_024L, // freeMemory as specified by the case
        tc.freeMemory, // maxMemory (placeholder)
        tc.freeMemory + 2_048L);
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            // Stub the static call.
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(mockInfo);
            // Act
            boolean result = SystemUtilities.hasAvailableMemory(tc.requiredBytes);
            // Assert
            assertEquals(tc.expected, result, () -> "hasAvailableMemory(" + tc.requiredBytes + ") with freeMemory=" + tc.freeMemory + " should be " + tc.expected + ". " + tc.description);
            // Verify that the static method was invoked exactly once.
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }
}
