package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThrowableConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    private static Stream<Arguments> withoutCauseProvider() {
        return Stream.of(Arguments.of("Plain Exception with non‑null message", new Exception("test message"), Map.of(CLASS, Exception.class.getName(), MESSAGE, "test message")), Arguments.of("Exception with null message", new Exception((String) null), Map.of(CLASS, Exception.class.getName(), MESSAGE, null)), Arguments.of("AssertionError subclass with message", new AssertionError("assert"), Map.of(CLASS, AssertionError.class.getName(), MESSAGE, "assert")));
    }

    private static Stream<Arguments> withCauseProvider() {
        return Stream.of(Arguments.of("Exception wrapping RuntimeException (both messages non‑null)", new Exception("outer", new RuntimeException("inner")), Map.of(CLASS, Exception.class.getName(), MESSAGE, "outer", CAUSE, RuntimeException.class.getName(), CAUSE_MESSAGE, "inner")), Arguments.of("Exception wrapping IllegalArgumentException with null cause message", new Exception("outer", new IllegalArgumentException()), Map.of(CLASS, Exception.class.getName(), MESSAGE, "outer", CAUSE, IllegalArgumentException.class.getName(), CAUSE_MESSAGE, null)));
    }

    private static Stream<Arguments> errorSubclassProvider() {
        return Stream.of(Arguments.of("OutOfMemoryError with non‑null message", new OutOfMemoryError("oom"), Map.of(CLASS, OutOfMemoryError.class.getName(), MESSAGE, "oom")), Arguments.of("AssertionError with null message", new AssertionError(), Map.of(CLASS, AssertionError.class.getName(), MESSAGE, null)));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("String input", "just a string"));
    }

    @Test
    void testToMap_NoCause() {
        // GIVEN
        String message = "top level exception";
        Exception ex = new Exception(message);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(ex, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain only class and message entries");
        assertEquals(ex.getClass().getName(), result.get(CLASS), "Class entry should match throwable class name");
        assertEquals(message, result.get(MESSAGE), "Message entry should match throwable message");
        assertFalse(result.containsKey(CAUSE), "Map should not contain CAUSE when there is no cause");
        assertFalse(result.containsKey(CAUSE_MESSAGE), "Map should not contain CAUSE_MESSAGE when there is no cause");
    }

    @Test
    void testToMap_WithCause() {
        // GIVEN
        String causeMessage = "root cause";
        IllegalArgumentException cause = new IllegalArgumentException(causeMessage);
        RuntimeException ex = new RuntimeException("wrapper exception", cause);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(ex, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(4, result.size(), "Map should contain class, message, cause class and cause message entries");
        assertEquals(ex.getClass().getName(), result.get(CLASS), "Class entry should match throwable class name");
        assertEquals("wrapper exception", result.get(MESSAGE), "Message entry should match throwable message");
        assertEquals(cause.getClass().getName(), result.get(CAUSE), "Cause entry should match cause class name");
        assertEquals(causeMessage, result.get(CAUSE_MESSAGE), "Cause message entry should match cause message");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("withoutCauseProvider")
    void testToMap_WithoutCause(String description, Throwable throwable, Map<String, Object> expected) {
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(throwable, null);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(expected.size(), result.size(), "Map size should match expected number of entries");
        expected.forEach((k, v) -> assertEquals(v, result.get(k), () -> "Map entry for key '" + k + "' should match expected value"));
    }

    @Test
    void testToMap_NestedCause_IgnoresDeepCause() {
        // GIVEN: outer exception -> RuntimeException (inner) -> IllegalArgumentException (deep)
        Throwable deep = new IllegalArgumentException("deep");
        Throwable inner = new RuntimeException("inner", deep);
        Throwable outer = new Exception("outer", inner);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(outer, null);
        // THEN
        assertEquals(4, result.size(), "Map should contain only the immediate cause information");
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertEquals("outer", result.get(MESSAGE));
        assertEquals(RuntimeException.class.getName(), result.get(CAUSE));
        assertEquals("inner", result.get(CAUSE_MESSAGE));
        // Deep cause must not appear
        assertFalse(result.containsKey("deep"), "Deep cause should not be present");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("errorSubclassProvider")
    void testToMap_ErrorSubclass(String description, Throwable throwable, Map<String, Object> expected) {
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(throwable, null);
        // THEN
        assertEquals(expected.size(), result.size(), "Map size should match expected number of entries for Error subclass");
        expected.forEach((k, v) -> assertEquals(v, result.get(k), () -> "Map entry for key '" + k + "' should match expected value"));
    }

    @Test
    void testToMap_SpecialCharactersInMessage() {
        // GIVEN
        // newline + Unicode snowman
        String special = "line1\nline2\u2603";
        Throwable ex = new Exception(special);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(ex, null);
        // THEN
        assertEquals(2, result.size());
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertEquals(special, result.get(MESSAGE));
    }

    @Test
    void testToMap_ConverterParameterIgnored() {
        // GIVEN: a dummy Converter implementation
        Converter dummy = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                return "should not be used";
            }
        };
        Throwable ex = new IllegalStateException("state");
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(ex, dummy);
        // THEN
        assertEquals(2, result.size());
        assertEquals(IllegalStateException.class.getName(), result.get(CLASS));
        assertEquals("state", result.get(MESSAGE));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidInputProvider")
    void testToMap_InvalidInput_ThrowsClassCastException(String description, Object invalidInput) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ThrowableConversions.toMap(invalidInput, null), "A non‑Throwable input must cause ClassCastException");
    }

    @Test
    void testToMap_NullFrom_ThrowsNullPointerException() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ThrowableConversions.toMap(null, null), "Passing null as the 'from' argument should result in NullPointerException");
    }
}
