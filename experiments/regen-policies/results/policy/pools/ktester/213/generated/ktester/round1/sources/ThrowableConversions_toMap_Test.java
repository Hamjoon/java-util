package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThrowableConversions_toMap_Test {

    @Mock
    private Converter // not used by the method, but required by the signature
    mockConverter;

    /**
     * Provides arguments for groups that only need to verify the content of the map.
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>the {@code Throwable} instance to pass as {@code from}</li>
     *   <li>the {@code Converter} instance (null or a dummy implementation)</li>
     *   <li>expected class name</li>
     *   <li>expected message (may be null)</li>
     *   <li>expected cause class name (or null if no cause)</li>
     *   <li>expected cause message (or null)</li>
     * </ul>
     */
    private static Stream<Arguments> mapContentProvider() {
        return Stream.of(// Throwable without cause
        Arguments.of(new Exception("error"), null, Exception.class.getName(), "error", null, null, "A simple Exception with a non‑null message and no cause should produce a map containing only CLASS and MESSAGE entries."), // Throwable with null message and no cause
        Arguments.of(new Exception((String) null), null, Exception.class.getName(), null, null, null, "An Exception whose getMessage() returns null and has no cause should produce a map containing CLASS and MESSAGE entries, with MESSAGE mapped to null."), // Throwable with cause (both messages non‑null)
        Arguments.of(new Exception("outer", new IllegalArgumentException("inner")), null, Exception.class.getName(), "outer", IllegalArgumentException.class.getName(), "inner", "An Exception with a non‑null cause and both outer and inner messages should produce a map containing CLASS, MESSAGE, CAUSE and CAUSE_MESSAGE entries."), // Throwable with cause (cause message null)
        Arguments.of(new Exception("outer", new IllegalArgumentException((String) null)), null, Exception.class.getName(), "outer", IllegalArgumentException.class.getName(), null, "An Exception whose cause's getMessage() returns null should still include the CAUSE entry and map CAUSE_MESSAGE to null."), // Non‑null converter argument ignored
        Arguments.of(new RuntimeException("runtime"), new Converter() {

            @Override
            public Object convert(Object o) {
                return null;
            }
        }, RuntimeException.class.getName(), "runtime", null, null, "The method does not use the Converter argument; providing a non‑null implementation must not affect the resulting map."), // Throwable subclass without cause (Error subclass)
        Arguments.of(new AssertionError("assertion failed"), null, AssertionError.class.getName(), "assertion failed", null, null, "An Error subclass (AssertionError) with a message and no cause should be handled identically to Exceptions, producing CLASS and MESSAGE entries."));
    }

    @Test
    void testToMapWithoutCause() {
        // GIVEN
        String msg = "top level exception";
        Exception ex = new Exception(msg);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(ex, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain only CLASS and MESSAGE entries");
        assertEquals(ex.getClass().getName(), result.get(CLASS), "CLASS entry should match exception class name");
        assertEquals(msg, result.get(MESSAGE), "MESSAGE entry should match exception message");
        assertFalse(result.containsKey(CAUSE), "CAUSE key must not be present when there is no cause");
        assertFalse(result.containsKey(CAUSE_MESSAGE), "CAUSE_MESSAGE key must not be present when there is no cause");
    }

    @Test
    void testToMapWithCause() {
        // GIVEN
        String causeMsg = "root cause";
        IllegalArgumentException cause = new IllegalArgumentException(causeMsg);
        String topMsg = "wrapper exception";
        Exception wrapper = new Exception(topMsg, cause);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(wrapper, mockConverter);
        // THEN
        assertNotNull(result);
        // Expected four entries: CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE
        assertEquals(4, result.size(), "Map should contain CLASS, MESSAGE, CAUSE and CAUSE_MESSAGE entries");
        assertEquals(wrapper.getClass().getName(), result.get(CLASS), "CLASS entry should be wrapper's class name");
        assertEquals(topMsg, result.get(MESSAGE), "MESSAGE entry should be wrapper's message");
        assertEquals(cause.getClass().getName(), result.get(CAUSE), "CAUSE entry should be cause's class name");
        assertEquals(causeMsg, result.get(CAUSE_MESSAGE), "CAUSE_MESSAGE entry should be cause's message");
    }

    @Test
    void testToMapInvalidSourceThrowsClassCastException() {
        // GIVEN
        Object notAThrowable = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ThrowableConversions.toMap(notAThrowable, mockConverter), "Passing a non‑Throwable should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{6}")
    @MethodSource("mapContentProvider")
    void testToMapVariousScenarios(Throwable from, Converter converter, String expectedClass, String expectedMessage, String expectedCauseClass, String expectedCauseMessage, String description) {
        // GIVEN – arguments are supplied by the method source
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(from, converter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        // Always expect CLASS and MESSAGE entries
        assertEquals(expectedClass, result.get(CLASS), "CLASS entry mismatch");
        assertEquals(expectedMessage, result.get(MESSAGE), "MESSAGE entry mismatch");
        if (expectedCauseClass == null) {
            // No cause expected – map should contain exactly 2 entries
            assertEquals(2, result.size(), "Map should contain only CLASS and MESSAGE when there is no cause");
            assertFalse(result.containsKey(CAUSE), "CAUSE key must be absent");
            assertFalse(result.containsKey(CAUSE_MESSAGE), "CAUSE_MESSAGE key must be absent");
        } else {
            // Cause expected – map should contain 4 entries
            assertEquals(4, result.size(), "Map should contain CLASS, MESSAGE, CAUSE and CAUSE_MESSAGE");
            assertEquals(expectedCauseClass, result.get(CAUSE), "CAUSE entry mismatch");
            assertEquals(expectedCauseMessage, result.get(CAUSE_MESSAGE), "CAUSE_MESSAGE entry mismatch");
        }
    }

    /**
     * Verifies that passing a {@code null} reference as the {@code from} argument
     * results in a {@link NullPointerException}. The method performs an unchecked
     * cast without a null‑check, so the NPE is thrown by the JVM.
     */
    @Test
    void testToMapNullThrowableThrowsNPE() {
        // GIVEN
        Throwable nullThrowable = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ThrowableConversions.toMap(nullThrowable, mockConverter), "Passing a null reference as the 'from' argument should result in a NullPointerException");
    }
}
