package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.time.DayOfWeek;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

// kept from the template; Mockito not used here
@ExtendWith(MockitoExtension.class)
class Converter_getShortName_Test {

    /* ----------------------------------------------------------------------
     *  Helper types used only by this test class
     * ---------------------------------------------------------------------- */
    private static class Sample {
        /* empty – just a distinct class for testing */
    }

    private static class SubDate extends Date {
        /* empty – subclass of java.util.Date */
    }

    /**
     * Supplies a stream of arguments: each entry contains the input {@code Class<?>}
     * and the expected result (either a {@code String} or the name of an exception class).
     */
    private static Stream<Map.Entry<Class<?>, Object>> shortNameProvider() {
        return Stream.of(// Non‑Date classes
        entry(String.class, "String"), entry(java.sql.Date.class, "Date"), entry(Converter.class, "Converter"), // Primitive types
        entry(int.class, "int"), entry(boolean.class, "boolean"), // Array types
        entry(String[].class, "String[]"), entry(int[][].class, "int[][]"), // Nested interface
        entry(Map.Entry.class, "Entry"), // Enum
        entry(DayOfWeek.class, "DayOfWeek"), // Interface
        entry(java.util.List.class, "List"), // Abstract class
        entry(java.util.Calendar.class, "Calendar"), // Subclass of java.util.Date (java.sql.Timestamp)
        entry(java.sql.Timestamp.class, "Timestamp"), // java.time class (non‑Date)
        entry(java.time.LocalDate.class, "LocalDate"), // Anonymous class – simple name is empty string
        entry(new Object() {
        }.getClass(), ""), // SecurityException case – we will handle it specially in the test method
        entry(Date.class, SecurityException.class));
    }

    /**
     * Convenience method to create a Map.Entry without pulling in external libraries.
     */
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @Test
    void returnsFullyQualifiedNameWhenTypeIsExactlyDate() {
        // GIVEN
        Class<?> type = Date.class;
        // WHEN
        String result = Converter.getShortName(type);
        // THEN
        // For java.util.Date the method returns the *full* class name, not the simple name
        assertEquals(Date.class.getName(), result, "When the type is exactly java.util.Date the method must return the fully qualified name");
    }

    @Test
    void returnsSimpleNameForNonDateClasses() {
        // GIVEN
        Class<?> stringClass = String.class;
        Class<?> integerClass = Integer.class;
        Class<?> customClass = Sample.class;
        // WHEN / THEN
        assertEquals("String", Converter.getShortName(stringClass), "Non‑Date classes should yield their simple name");
        assertEquals("Integer", Converter.getShortName(integerClass), "Non‑Date classes should yield their simple name");
        assertEquals("Sample", Converter.getShortName(customClass), "Non‑Date classes should yield their simple name");
    }

    @Test
    void returnsSimpleNameWhenClassIsSubclassOfDate() {
        // GIVEN
        // extends java.util.Date
        Class<?> subDateClass = SubDate.class;
        // WHEN
        String result = Converter.getShortName(subDateClass);
        // THEN
        // The method checks for equality with Date.class, so a subclass should fall back to simple name
        assertEquals("SubDate", result, "A subclass of java.util.Date must not be treated as Date.class itself");
    }

    @Test
    void throwsNullPointerExceptionWhenNullIsPassed() {
        // GIVEN
        Class<?> nullClass = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Converter.getShortName(nullClass), "Passing null should result in a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("shortNameProvider")
    void shortNameVariousCases(Map.Entry<Class<?>, Object> entry) {
        Class<?> input = entry.getKey();
        Object expected = entry.getValue();
        // SecurityException case needs a custom SecurityManager
        if (expected == SecurityException.class) {
            SecurityManager original = System.getSecurityManager();
            try {
                System.setSecurityManager(new SecurityManager() {

                    @Override
                    public void checkPermission(Permission perm) {
                        // Allow everything except when the call originates from Converter.getShortName
                        // (the stack will contain this method name)
                        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                        for (StackTraceElement e : stack) {
                            if (e.getClassName().equals(Converter.class.getName()) && e.getMethodName().equals("getShortName")) {
                                throw new SecurityException("Denied for test");
                            }
                        }
                    }
                });
                assertThrows(SecurityException.class, () -> Converter.getShortName(input), "A SecurityManager that forbids class metadata access should cause a SecurityException");
            } finally {
                // restore original manager
                System.setSecurityManager(original);
            }
            return;
        }
        // Normal expected string result
        String result = Converter.getShortName(input);
        assertEquals(expected, result, () -> "Expected short name for " + input + " to be '" + expected + "'");
    }
}
