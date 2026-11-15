package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

// No mocks are actually used, but kept for consistency with the template
@ExtendWith(MockitoExtension.class)
class SystemUtilities_getAvailableProcessors_Test {

    /**
     * Holds the value reported by the JVM before any test runs.
     */
    private static int originalProcessorCount;

    @BeforeAll
    static void setupBeforeAll() {
        // Capture the baseline processor count once for possible future assertions.
        originalProcessorCount = Runtime.getRuntime().availableProcessors();
    }

    @Test
    @DisplayName("SystemUtilities.getAvailableProcessors() returns a sensible value")
    void testGetAvailableProcessors() {
        // given: the current runtime processor count (captured in @BeforeAll)
        int runtimeProcessors = originalProcessorCount;
        // when: the utility method is invoked
        int available = SystemUtilities.getAvailableProcessors();
        // then: the result must be at least 1 and never exceed the runtime value
        assertTrue(available >= 1, "The number of available processors should never be less than 1");
        assertTrue(available <= runtimeProcessors, "The utility should never report more processors than the JVM reports");
    }

    /**
     * Parameterized test that verifies the method enforces a minimum of {@code 1}
     * and otherwise returns the exact value reported by {@link Runtime}.
     *
     * @param mockedValue the value we force {@code Runtime.availableProcessors()} to return
     * @param expected    the expected result from {@code SystemUtilities.getAvailableProcessors()}
     */
    @ParameterizedTest(name = "[{index}] Runtime reports {0} → expected {1}")
    @CsvSource({ // Simulated edge‑cases and a typical multi‑core scenario
    // Runtime reports 0 → method should clamp to 1
    "0, 1", // Exactly 1 processor → returned unchanged
    "1, 1", // Typical multi‑core value
    "4, 4", // Negative value → still clamped to 1
    "-2, 1" })
    @DisplayName("SystemUtilities.getAvailableProcessors() respects Runtime value with minimum 1")
    void testGetAvailableProcessors_withVariousRuntimeValues(int mockedValue, int expected) {
        // given: a static mock of Runtime that returns the desired processor count
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.availableProcessors()).thenReturn(mockedValue);
            // when: the utility method is invoked
            int actual = SystemUtilities.getAvailableProcessors();
            // then: the result matches the expected clamped value
            assertEquals(expected, actual, () -> "When Runtime reports " + mockedValue + " processors, " + "SystemUtilities should return " + expected);
        }
    }

    /**
     * Non‑parameterized sanity test that verifies the method never returns a value
     * less than 1 and never exceeds the real runtime processor count.
     *
     * This test runs against the actual JVM environment (no mocking) and
     * complements the mocked scenarios above.
     */
    @Test
    @DisplayName("SystemUtilities.getAvailableProcessors() returns a sensible value in real JVM")
    void testGetAvailableProcessors_realRuntime() {
        // given: the actual processor count reported by the JVM
        int runtimeProcessors = originalProcessorCount;
        // when: the utility method is called without any mocking
        int available = SystemUtilities.getAvailableProcessors();
        // then: result is at least 1 and never greater than the JVM's count
        assertTrue(available >= 1, "The number of available processors should never be less than 1");
        assertTrue(available <= runtimeProcessors, "The utility should never report more processors than the JVM reports");
    }
}
