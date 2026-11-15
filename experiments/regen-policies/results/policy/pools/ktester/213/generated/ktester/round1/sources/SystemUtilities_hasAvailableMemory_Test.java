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
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_hasAvailableMemory_Test {

    /**
     * A reusable {@link SystemUtilities.MemoryInfo} instance for stubbing.
     */
    private static SystemUtilities.MemoryInfo stubInfo;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a stub MemoryInfo with deterministic values.
        // total = 1 000, free = 200, max = 1 000 (all in bytes for simplicity)
        stubInfo = new SystemUtilities.MemoryInfo(1_000L, 200L, 1_000L);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the stub reference.
        stubInfo = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideStandardCases() {
        return Stream.of(// free > required → true
        arguments(1_024L, 2_048L, true), // free == required → true (>= condition)
        arguments(2_048L, 2_048L, true), // free < required → false
        arguments(4_096L, 2_048L, false), // zero required → always true
        arguments(0L, 0L, true), // negative required → always true
        arguments(-1L, 0L, true));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideAdditionalCases() {
        return Stream.of(// free = 0, required = 1 → false
        arguments(1L, 0L, false), // free = 0, required = 0 → true
        arguments(0L, 0L, true), // free = Long.MAX_VALUE, required = Long.MAX_VALUE → true
        arguments(Long.MAX_VALUE, Long.MAX_VALUE, true), // free = Long.MAX_VALUE - 1, required = Long.MAX_VALUE → false
        arguments(Long.MAX_VALUE, Long.MAX_VALUE - 1, false), // free = Long.MAX_VALUE, required = Long.MAX_VALUE - 1 → true
        arguments(Long.MAX_VALUE - 1, Long.MAX_VALUE, true), // negative required (Long.MIN_VALUE) with any non‑negative free → true
        arguments(Long.MIN_VALUE, 0L, true), // free = 1, required = 1 → true
        arguments(1L, 1L, true), // free = Long.MAX_VALUE, required = 1 → true
        arguments(1L, Long.MAX_VALUE, true));
    }

    @Test
    void hasAvailableMemory_returnsTrueWhenFreeMemoryIsSufficient() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            // Stub the static helper that the method under test relies on.
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(stubInfo);
            // requiredBytes (150) < freeMemory (200) → should be true
            assertTrue(SystemUtilities.hasAvailableMemory(150L), "Expected true when required bytes are less than free memory.");
            // Verify that getMemoryInfo() was invoked exactly once.
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void hasAvailableMemory_returnsFalseWhenFreeMemoryIsInsufficient() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(stubInfo);
            // requiredBytes (250) > freeMemory (200) → should be false
            assertFalse(SystemUtilities.hasAvailableMemory(250L), "Expected false when required bytes exceed free memory.");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void hasAvailableMemory_trueWhenRequiredIsZero() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(stubInfo);
            assertTrue(SystemUtilities.hasAvailableMemory(0L), "Zero required bytes should always be satisfied.");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void hasAvailableMemory_treatsNegativeRequirementAsSatisfied() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(stubInfo);
            // Negative values are logically always satisfied because freeMemory >= negative
            assertTrue(SystemUtilities.hasAvailableMemory(-100L), "Negative required bytes should be considered satisfied.");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void hasAvailableMemory_handlesVeryLargeRequirementGracefully() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(stubInfo);
            // A value far larger than any realistic free memory should simply return false
            long huge = Long.MAX_VALUE;
            assertFalse(SystemUtilities.hasAvailableMemory(huge), "A requirement larger than any possible free memory must return false.");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    /**
     * Parameterized test covering the basic scenarios described in the JSON
     * test group {@code hasAvailableMemory}.
     *
     * @param requiredBytes the value passed to {@code hasAvailableMemory}
     * @param freeMemory     the free memory we stub via {@code getMemoryInfo()}
     * @param expected       the expected boolean result
     */
    @ParameterizedTest(name = "[{index}] required={0}, free={1} → expected={2}")
    @MethodSource("provideStandardCases")
    void hasAvailableMemory_standardScenarios(long requiredBytes, long freeMemory, boolean expected) {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            // Stub getMemoryInfo() with the supplied free memory.
            SystemUtilities.MemoryInfo info = new SystemUtilities.MemoryInfo(/*total*/
            freeMemory, /*free*/
            freeMemory, /*max*/
            freeMemory);
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(info);
            boolean actual = SystemUtilities.hasAvailableMemory(requiredBytes);
            assertEquals(expected, actual, () -> String.format("hasAvailableMemory(%d) with freeMemory=%d", requiredBytes, freeMemory));
            // Verify that the static helper was called exactly once.
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    /**
     * Parameterized test covering the additional edge‑case scenarios from the
     * JSON group {@code hasAvailableMemory_additional}.
     *
     * @param requiredBytes the required bytes argument
     * @param freeMemory    the free memory we stub
     * @param expected      expected result
     */
    @ParameterizedTest(name = "[{index}] required={0}, free={1} → expected={2}")
    @MethodSource("provideAdditionalCases")
    void hasAvailableMemory_additionalScenarios(long requiredBytes, long freeMemory, boolean expected) {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            SystemUtilities.MemoryInfo info = new SystemUtilities.MemoryInfo(/*total*/
            freeMemory, /*free*/
            freeMemory, /*max*/
            freeMemory);
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(info);
            boolean actual = SystemUtilities.hasAvailableMemory(requiredBytes);
            assertEquals(expected, actual, () -> String.format("hasAvailableMemory(%d) with freeMemory=%d", requiredBytes, freeMemory));
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }

    @Test
    void hasAvailableMemory_throwsNPEWhenMemoryInfoIsNull() {
        try (MockedStatic<SystemUtilities> mocked = mockStatic(SystemUtilities.class)) {
            // Stub getMemoryInfo() to return null.
            mocked.when(SystemUtilities::getMemoryInfo).thenReturn(null);
            assertThrows(NullPointerException.class, () -> SystemUtilities.hasAvailableMemory(1L), "Expected NullPointerException when getMemoryInfo() returns null.");
            mocked.verify(SystemUtilities::getMemoryInfo, times(1));
        }
    }
}
