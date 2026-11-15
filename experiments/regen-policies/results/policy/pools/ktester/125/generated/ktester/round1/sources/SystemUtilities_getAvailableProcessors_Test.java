package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_getAvailableProcessors_Test {

    /**
     * Holds the value returned by the real JVM for later comparison.
     */
    private static int jvmAvailableProcessors;

    @BeforeAll
    static void setupBeforeAll() {
        // Capture the actual number of processors reported by the JVM once,
        // so that the test can compare the utility method against it.
        jvmAvailableProcessors = Runtime.getRuntime().availableProcessors();
    }

    /**
     * Provides the mocked Runtime values and the expected result from
     * {@link SystemUtilities#getAvailableProcessors()}.
     *
     * <p>Case 1 – JVM reports 4 processors → method should return 4.</p>
     * <p>Case 2 – JVM reports 0 processors → method should return 1 (the safety net).</p>
     */
    private static Stream<Arguments> mockedProcessorValues() {
        return Stream.of(Arguments.of(4, 4, "When Runtime reports 4, the utility should return 4."), Arguments.of(0, 1, "When Runtime reports 0, the utility must fall back to 1."));
    }

    @Test
    void testGetAvailableProcessorsReturnsAtLeastOne() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue(processors >= 1, "SystemUtilities.getAvailableProcessors() should never return less than 1");
    }

    @Test
    void testGetAvailableProcessorsDoesNotExceedRuntimeValue() {
        int processors = SystemUtilities.getAvailableProcessors();
        assertTrue(processors <= jvmAvailableProcessors, "SystemUtilities.getAvailableProcessors() should never exceed Runtime.availableProcessors()");
    }

    @Test
    void testGetAvailableProcessorsMatchesRuntimeWhenRuntimeIsPositive() {
        // When the JVM reports a positive number, the utility method should return the same value,
        // because Math.max(1, x) will return x for any x >= 1.
        assumeTrue(jvmAvailableProcessors > 0, "JVM reports zero processors – unrealistic, test skipped");
        int processors = SystemUtilities.getAvailableProcessors();
        assertEquals(jvmAvailableProcessors, processors, "When Runtime reports a positive number, the utility should return that exact number");
    }

    @Test
    void testConstructorIsPrivate() throws Exception {
        // Verify that the utility class cannot be instantiated.
        java.lang.reflect.Constructor<SystemUtilities> con = SystemUtilities.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(con.getModifiers()), "Constructor should be private");
        con.setAccessible(true);
        assertNotNull(con.newInstance(), "Even though private, the constructor should be invocable via reflection");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mockedProcessorValues")
    void testGetAvailableProcessorsWithMockedRuntime(int mockedRuntimeValue, int expectedResult, String description) {
        // Mockito can mock static methods when the 'mockito-inline' artifact is on the classpath.
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            // Mock Runtime.getRuntime() to return a dummy Runtime instance.
            Runtime dummyRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(dummyRuntime);
            // Stub the call to availableProcessors() on that dummy instance.
            Mockito.when(dummyRuntime.availableProcessors()).thenReturn(mockedRuntimeValue);
            // Call the method under test – it will hit the mocked static path.
            int actual = SystemUtilities.getAvailableProcessors();
            assertEquals(expectedResult, actual, "Unexpected result for mocked Runtime.availableProcessors() = " + mockedRuntimeValue);
        }
    }
}
