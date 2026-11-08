package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.util.stream.Stream;

class ReflectionUtils_isJavaCompilerAvailable_Test {

    private static final String FORCE_JRE_PROPERTY = "java.util.force.jre";

    private String originalForceJreValue;

    /**
     * Functional interface used to apply scenario‑specific mock configuration.
     */
    @FunctionalInterface
    private interface MockSetup {

        void apply(MockedStatic<Class> classMock);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Preserve the original system‑property value so we can restore it after each test.
        originalForceJreValue = System.getProperty(FORCE_JRE_PROPERTY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original value (or clear it if it was not set).
        if (originalForceJreValue != null) {
            System.setProperty(FORCE_JRE_PROPERTY, originalForceJreValue);
        } else {
            System.clearProperty(FORCE_JRE_PROPERTY);
        }
    }

    /**
     * Supplies the different scenarios described above.
     */
    private static Stream<Arguments> provideScenarios() {
        return Stream.of(// 1. ClassNotFoundException while loading ToolProvider -> false
        Arguments.of("ClassNotFoundException for javax.tools.ToolProvider should result in false", (MockSetup) classMock -> classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenThrow(new ClassNotFoundException("ToolProvider not found")), false), // 2. getSystemJavaCompiler() returns null -> false
        Arguments.of("ToolProvider present but getSystemJavaCompiler() returns null should result in false", (MockSetup) classMock -> {
            Class<?> toolProviderMock = Mockito.mock(Class.class);
            Method methodMock = Mockito.mock(Method.class);
            classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenReturn(toolProviderMock);
            Mockito.when(toolProviderMock.getMethod("getSystemJavaCompiler")).thenReturn(methodMock);
            Mockito.when(methodMock.invoke(null)).thenReturn(null);
        }, false), // 3. getSystemJavaCompiler() returns non‑null -> true
        Arguments.of("ToolProvider present and getSystemJavaCompiler() returns non‑null should result in true", (MockSetup) classMock -> {
            Class<?> toolProviderMock = Mockito.mock(Class.class);
            Method methodMock = Mockito.mock(Method.class);
            classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenReturn(toolProviderMock);
            Mockito.when(toolProviderMock.getMethod("getSystemJavaCompiler")).thenReturn(methodMock);
            Mockito.when(methodMock.invoke(null)).thenReturn(new Object());
        }, // any non‑null object
        true), // 4. getSystemJavaCompiler() throws RuntimeException -> false
        Arguments.of("ToolProvider present but getSystemJavaCompiler() throws RuntimeException should result in false", (MockSetup) classMock -> {
            Class<?> toolProviderMock = Mockito.mock(Class.class);
            Method methodMock = Mockito.mock(Method.class);
            classMock.when(() -> Class.forName("javax.tools.ToolProvider")).thenReturn(toolProviderMock);
            Mockito.when(toolProviderMock.getMethod("getSystemJavaCompiler")).thenReturn(methodMock);
            Mockito.when(methodMock.invoke(null)).thenThrow(new RuntimeException("simulated failure"));
        }, false));
    }

    /**
     * When the system property {@code java.util.force.jre} is set to {@code true},
     * {@link ReflectionUtils#isJavaCompilerAvailable()} must return {@code false}
     * regardless of the actual JDK environment.
     */
    @Test
    void testIsJavaCompilerAvailableWhenForceJreTrue() {
        System.setProperty(FORCE_JRE_PROPERTY, "true");
        assertFalse(ReflectionUtils.isJavaCompilerAvailable(), "Method should report compiler unavailable when forced JRE flag is true");
    }

    /**
     * When the {@code java.util.force.jre} flag is not set, the method should behave
     * exactly like a direct reflective check for {@code javax.tools.ToolProvider}.
     * This test computes the expected result using the same reflective steps and
     * verifies that the utility method returns the same value.
     */
    @Test
    void testIsJavaCompilerAvailableMatchesDirectReflectiveCheck() {
        System.clearProperty(FORCE_JRE_PROPERTY);
        // Compute the expected result using the same logic that the utility method uses.
        boolean expected;
        try {
            Class<?> toolProvider = Class.forName("javax.tools.ToolProvider");
            Object compiler = toolProvider.getMethod("getSystemJavaCompiler").invoke(null);
            expected = compiler != null;
        } catch (Throwable t) {
            expected = false;
        }
        boolean actual = ReflectionUtils.isJavaCompilerAvailable();
        assertEquals(expected, actual, "ReflectionUtils.isJavaCompilerAvailable() should match the direct reflective check");
    }

    /**
     * Parameterized test covering the remaining execution paths:
     * <ul>
     *   <li>ClassNotFoundException when loading ToolProvider.</li>
     *   <li>getSystemJavaCompiler() returns {@code null}.</li>
     *   <li>getSystemJavaCompiler() returns a non‑null object.</li>
     *   <li>getSystemJavaCompiler() throws a RuntimeException.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideScenarios")
    void testIsJavaCompilerAvailableVariousScenarios(String description, MockSetup mockSetup, boolean expected) {
        System.clearProperty(FORCE_JRE_PROPERTY);
        // Mock static calls on java.lang.Class
        try (MockedStatic<Class> classMock = Mockito.mockStatic(Class.class)) {
            // Apply the scenario‑specific mocking behaviour.
            mockSetup.apply(classMock);
            boolean actual = ReflectionUtils.isJavaCompilerAvailable();
            assertEquals(expected, actual, description);
        }
    }
}
