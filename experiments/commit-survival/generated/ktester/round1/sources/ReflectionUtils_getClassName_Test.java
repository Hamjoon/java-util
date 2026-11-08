package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class Custom {
    }

    /**
     * Simple user‑defined class for testing custom class name handling.
     */
    static class CustomClass {
    }

    /**
     * Subclass of {@link CustomClass} to verify that the subclass name is returned.
     */
    static class SubCustomClass extends CustomClass {
    }

    /**
     * Provides a stream of arguments for {@link #getClassName_variousInputs(Object, String, boolean)}.
     * Each argument consists of:
     * <ul>
     *   <li>the input object</li>
     *   <li>the expected class name (or a substring that must be present)</li>
     *   <li>a flag indicating whether the expected value is a substring check</li>
     * </ul>
     */
    static Stream<Arguments> classNameProvider() {
        // custom classes defined below
        CustomClass custom = new CustomClass();
        SubCustomClass subCustom = new SubCustomClass();
        // anonymous inner class
        Runnable anonymousRunnable = new Runnable() {

            @Override
            public void run() {
            }
        };
        // lambda expression (synthetic class name contains $$Lambda$)
        Runnable lambda = (Runnable) () -> {
        };
        // dynamic proxy (class name contains $Proxy)
        Object proxy = Proxy.newProxyInstance(ReflectionUtils.class.getClassLoader(), new Class[] { Serializable.class }, (p, m, args) -> null);
        return Stream.of(// null input
        Arguments.of(null, "null", false), // boxed primitive
        Arguments.of(Integer.valueOf(42), Integer.class.getName(), false), // regular string
        Arguments.of("sample text", String.class.getName(), false), // primitive array
        Arguments.of(new int[] { 1, 2, 3 }, int[].class.getName(), false), // custom class instance
        Arguments.of(custom, CustomClass.class.getName(), false), // enum constant
        Arguments.of(DayOfWeek.MONDAY, DayOfWeek.class.getName(), false), // anonymous inner class
        Arguments.of(anonymousRunnable, anonymousRunnable.getClass().getName(), false), // boxed long
        Arguments.of(Long.valueOf(123456789L), Long.class.getName(), false), // array of reference types (String[])
        Arguments.of(new String[] { "a", "b" }, String[].class.getName(), false), // multi‑dimensional primitive array (int[][])
        Arguments.of(new int[][] { { 1, 2 }, { 3, 4 } }, int[][].class.getName(), false), // standard collection implementation
        Arguments.of(new ArrayList<>(), ArrayList.class.getName(), false), // standard map implementation
        Arguments.of(new HashMap<>(), HashMap.class.getName(), false), // Class object itself
        Arguments.of(String.class, Class.class.getName(), false), // lambda – we only assert that the generated name contains "$$Lambda$"
        Arguments.of(lambda, "$$Lambda$", true), // dynamic proxy – name contains "$Proxy"
        Arguments.of(proxy, "$Proxy", true), // subclass instance
        Arguments.of(subCustom, SubCustomClass.class.getName(), false));
    }

    @Test
    @DisplayName("getClassName(null) should return the literal string \"null\"")
    void getClassName_returnsLiteralNullForNullArgument() {
        // Given
        Object input = null;
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals("null", result, "When the argument is null the method must return the literal string \"null\"");
    }

    @Test
    @DisplayName("getClassName(Object) should return the object's fully‑qualified class name")
    void getClassName_returnsClassNameForNonNullObject() {
        // Given
        String input = "sample";
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(String.class.getName(), result, "The returned class name must match the object's runtime class");
    }

    @Test
    @DisplayName("getClassName works with custom class instances")
    void getClassName_returnsClassNameForCustomObject() {
        // Given
        class Custom {
        }
        Custom custom = new Custom();
        // When
        String result = ReflectionUtils.getClassName(custom);
        // Then
        assertEquals(Custom.class.getName(), result);
    }

    @Test
    @DisplayName("getClassName works with array objects")
    void getClassName_returnsClassNameForArray() {
        // Given
        int[] array = new int[5];
        // When
        String result = ReflectionUtils.getClassName(array);
        // Then
        assertEquals(int[].class.getName(), result);
    }

    @ParameterizedTest(name = "[{index}] getClassName({0})")
    @MethodSource("classNameProvider")
    @DisplayName("ReflectionUtils.getClassName – various inputs")
    void getClassName_variousInputs(Object input, String expected, boolean containsCheck) {
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        if (containsCheck) {
            assertTrue(result.contains(expected), () -> "Result [" + result + "] should contain expected substring [" + expected + "]");
        } else {
            assertEquals(expected, result, () -> "Result [" + result + "] should equal expected class name [" + expected + "]");
        }
    }

    /**
     * Verifies that a {@link SecurityException} thrown from {@code Class#getName()}
     * propagates out of {@code ReflectionUtils.getClassName}.
     *
     * <p>We install a custom {@link SecurityManager} that throws a {@link SecurityException}
     * for any permission check. When {@code getClassName} attempts to retrieve the class
     * name, the security manager intervenes and the exception should be re‑thrown.</p>
     *
     * <p>Note: The test is disabled on Java runtimes that no longer support a
     * {@code SecurityManager} (e.g., when the security manager is ignored).</p>
     */
    @Test
    @DisplayName("ReflectionUtils.getClassName propagates SecurityException from Class#getName")
    @DisabledOnJre(value = { JRE.JAVA_17, JRE.JAVA_21 }, disabledReason = "SecurityManager is deprecated/ignored on newer JDKs")
    void getClassName_propagatesSecurityException() {
        // Arrange: install a security manager that always throws
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Throw for any permission request
                throw new SecurityException("Access denied by test SecurityManager");
            }
        });
        try {
            Object dummy = new Object();
            // Act & Assert
            assertThrows(SecurityException.class, () -> ReflectionUtils.getClassName(dummy), "Expected a SecurityException to be propagated from getClassName");
        } finally {
            // Restore original security manager to avoid side effects
            System.setSecurityManager(original);
        }
    }
}
