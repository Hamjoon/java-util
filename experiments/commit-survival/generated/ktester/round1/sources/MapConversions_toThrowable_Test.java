package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toThrowable_Test {

    /**
     * Simple cause exception with only (String) ctor.
     */
    public static class TestCauseException extends Exception {

        public TestCauseException(String message) {
            super(message);
        }
    }

    /**
     * Simple exception with (String) and (String, Throwable) constructors.
     */
    public static class TestException extends Exception {

        public TestException(String message) {
            super(message);
        }

        public TestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception class that lacks a (String) constructor.
     */
    public static class NoStringCtorMainException extends Exception {

        public NoStringCtorMainException() {
            super();
        }
    }

    /**
     * Cause class that lacks a (String) constructor.
     */
    public static class NoStringCtorException extends Exception {

        public NoStringCtorException() {
            super();
        }
    }

    /**
     * Exception with only a (String) constructor – no (String,Throwable).
     */
    public static class MainOnlyStringCtorException extends Exception {

        public MainOnlyStringCtorException(String message) {
            super(message);
        }
    }

    /**
     * Exception with only a (String,Throwable) constructor – no (String) ctor.
     */
    public static class MainOnlyStringThrowableCtorException extends Exception {

        public MainOnlyStringThrowableCtorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception whose (String) constructor deliberately throws an exception.
     */
    public static class ThrowingConstructorException extends Exception {

        public ThrowingConstructorException(String message) {
            super(message);
            throw new RuntimeException("Constructor failure");
        }
    }

    /**
     * Cause whose (String) constructor deliberately throws an exception.
     */
    public static class ThrowingCauseException extends Exception {

        public ThrowingCauseException(String message) {
            super(message);
            throw new RuntimeException("Cause constructor failure");
        }
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(// 1. No cause supplied (simple IllegalArgumentException)
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "simple message"), IllegalArgumentException.class, "simple message", null, null, "Creates an exception when no cause information is supplied"), // 2. With cause (IllegalArgumentException caused by NullPointerException)
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "outer exception", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner cause"), IllegalArgumentException.class, "outer exception", NullPointerException.class, "inner cause", "Creates an exception with a cause"), // 3. Empty cause string is treated as no cause
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", // empty string
        MapConversions.CAUSE, // empty string
        ""), IllegalArgumentException.class, "msg", null, null, "Empty cause string is treated as no cause"), // 4. MainOnlyStringThrowableCtorException with valid cause
        Arguments.of(mapOf(MapConversions.CLASS, MainOnlyStringThrowableCtorException.class.getName(), MapConversions.MESSAGE, "outer", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), MainOnlyStringThrowableCtorException.class, "outer", NullPointerException.class, "inner", "Main exception provides only (String,Throwable) ctor with a valid cause"), // 5. Null message for main exception, valid cause
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, null, MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), IllegalArgumentException.class, null, NullPointerException.class, "inner", "Null message for main exception should be accepted"), // 6. Cause class identical to main exception class
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "outer", MapConversions.CAUSE, IllegalArgumentException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), IllegalArgumentException.class, "outer", IllegalArgumentException.class, "inner", "Cause class same as main exception class"));
    }

    private static Stream<Arguments> failureCases() {
        return Stream.of(// 1. Cause class without (String) ctor
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, NoStringCtorException.class.getName(), MapConversions.CAUSE_MESSAGE, "ignored"), "Cause class lacks a (String) constructor"), // 2. Main exception class lacks (String) ctor
        Arguments.of(mapOf(MapConversions.CLASS, NoStringCtorMainException.class.getName(), MapConversions.MESSAGE, "msg"), "Main exception class lacks a (String) constructor"), // 3. Non‑existent class name
        Arguments.of(mapOf(MapConversions.CLASS, "non.existent.ClassName", MapConversions.MESSAGE, "msg"), "Class name cannot be resolved"), // 4. MainOnlyStringCtorException with a cause (no (String,Throwable) ctor)
        Arguments.of(mapOf(MapConversions.CLASS, MainOnlyStringCtorException.class.getName(), MapConversions.MESSAGE, "outer", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), "Main exception provides only (String) ctor but a cause is supplied"), // 5. Map missing required 'class' key
        Arguments.of(mapOf(MapConversions.MESSAGE, "msg"), "Missing 'class' entry in map"), // 6. 'class' entry is not a String (integer)
        Arguments.of(mapOf(MapConversions.CLASS, 12345, MapConversions.MESSAGE, "msg"), "'class' entry is not a String"), // 7. 'class' entry is explicitly null
        Arguments.of(mapOf(MapConversions.CLASS, null, MapConversions.MESSAGE, "msg"), "'class' entry is null"), // 8. 'cause' entry is not a String (integer)
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, 999, MapConversions.CAUSE_MESSAGE, "inner"), "'cause' entry is not a String"), // 9. 'causeMessage' entry is not a String (integer)
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, 123), "'causeMessage' entry is not a String"), // 10. MainOnlyStringCtorException with cause (duplicate of #4, kept for completeness)
        Arguments.of(mapOf(MapConversions.CLASS, MainOnlyStringCtorException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, NullPointerException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), "MainOnlyStringCtorException with a cause (no (String,Throwable) ctor)"), // 11. Main exception constructor throws an exception
        Arguments.of(mapOf(MapConversions.CLASS, ThrowingConstructorException.class.getName(), MapConversions.MESSAGE, "msg"), "Main exception constructor throws during instantiation"), // 12. Cause constructor throws an exception
        Arguments.of(mapOf(MapConversions.CLASS, IllegalArgumentException.class.getName(), MapConversions.MESSAGE, "msg", MapConversions.CAUSE, ThrowingCauseException.class.getName(), MapConversions.CAUSE_MESSAGE, "inner"), "Cause constructor throws during instantiation"), // 13. 'from' argument is not a Map
        Arguments.of("not a map", "'from' is not a Map"), // 14. 'from' argument is null
        Arguments.of(null, "'from' is null"));
    }

    /* --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void toThrowable_WithCause_ReconstructsException() {
        // GIVEN – a map describing an exception with a cause
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, TestException.class.getName());
        map.put(MapConversions.MESSAGE, "main message");
        map.put(MapConversions.CAUSE, TestCauseException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "cause message");
        // WHEN – the conversion is performed
        Throwable result = MapConversions.toThrowable(map, null);
        // THEN – the correct exception hierarchy is produced
        assertTrue(result instanceof TestException, "Result should be a TestException");
        assertEquals("main message", result.getMessage());
        assertNotNull(result.getCause(), "Cause should not be null");
        assertTrue(result.getCause() instanceof TestCauseException, "Cause should be a TestCauseException");
        assertEquals("cause message", result.getCause().getMessage());
    }

    @Test
    void toThrowable_WithoutCause_ReconstructsException() {
        // GIVEN – a map describing an exception without a cause
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, TestException.class.getName());
        map.put(MapConversions.MESSAGE, "solo message");
        // No CAUSE / CAUSE_MESSAGE entries
        // WHEN
        Throwable result = MapConversions.toThrowable(map, null);
        // THEN
        assertTrue(result instanceof TestException);
        assertEquals("solo message", result.getMessage());
        assertNull(result.getCause(), "Cause should be null when not supplied");
    }

    @Test
    void toThrowable_InvalidClass_ThrowsIllegalArgumentException() {
        // GIVEN – a map with a non‑existent class name
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, "com.nonexistent.FakeException");
        map.put(MapConversions.MESSAGE, "ignored");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, null));
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance"), "Exception message should indicate reconstruction failure");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("successfulCases")
    void toThrowable_SuccessfulReconstruction(Map<String, Object> map, Class<? extends Throwable> expectedClass, String expectedMessage, Class<? extends Throwable> expectedCauseClass, String expectedCauseMessage, String description) {
        // WHEN
        Throwable result = MapConversions.toThrowable(map, null);
        // THEN
        assertNotNull(result, description);
        assertEquals(expectedClass, result.getClass(), description);
        assertEquals(expectedMessage, result.getMessage(), description);
        if (expectedCauseClass == null) {
            assertNull(result.getCause(), description);
        } else {
            assertNotNull(result.getCause(), description);
            assertEquals(expectedCauseClass, result.getCause().getClass(), description);
            assertEquals(expectedCauseMessage, result.getCause().getMessage(), description);
        }
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("failureCases")
    void toThrowable_FailureScenarios(Object from, String description) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(from, null), description);
        assertTrue(ex.getMessage().contains("Unable to reconstruct exception instance"), "Exception message should indicate reconstruction failure");
    }
}
