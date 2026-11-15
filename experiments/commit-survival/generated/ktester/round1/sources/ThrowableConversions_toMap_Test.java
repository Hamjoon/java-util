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
    private Converter // not used by the method but required by the signature
    converter;

    /**
     * Simple user‑defined subclass of {@link Exception}.
     */
    static class MyError extends Exception {

        MyError(String msg) {
            super(msg);
        }
    }

    /**
     * Exception that throws when {@code getMessage()} is invoked.
     */
    static class ThrowingMessageException extends Exception {

        @Override
        public String getMessage() {
            throw new RuntimeException("Message failure");
        }
    }

    /**
     * Exception that throws when {@code getCause()} is invoked.
     */
    static class ThrowingCauseException extends Exception {

        @Override
        public synchronized Throwable getCause() {
            throw new RuntimeException("Cause failure");
        }
    }

    /**
     * Provides the basic functionality test data.
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Exception without a cause
        Arguments.of(new Exception("top level"), new Expected("java.lang.Exception", "top level", null, null)), // RuntimeException with an IllegalArgumentException cause
        Arguments.of(new RuntimeException("runtime", new IllegalArgumentException("illegal")), new Expected("java.lang.RuntimeException", "runtime", "java.lang.IllegalArgumentException", "illegal")), // NullPointerException with null message and no cause
        Arguments.of(new NullPointerException(), new Expected("java.lang.NullPointerException", null, null, null)), // IllegalStateException with a cause that has a null message
        Arguments.of(new IllegalStateException("state", new Exception()), new Expected("java.lang.IllegalStateException", "state", "java.lang.Exception", null)));
    }

    @Test
    void toMap_withoutCause_returnsClassAndMessageOnly() {
        // GIVEN
        String msg = "simple error";
        Throwable t = new IllegalArgumentException(msg);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(t, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries when there is no cause");
        assertEquals(t.getClass().getName(), result.get(CLASS), "Class name should be stored under key CLASS");
        assertEquals(msg, result.get(MESSAGE), "Message should be stored under key MESSAGE");
        assertFalse(result.containsKey(CAUSE), "Map must not contain CAUSE key when there is no cause");
        assertFalse(result.containsKey(CAUSE_MESSAGE), "Map must not contain CAUSE_MESSAGE key when there is no cause");
    }

    @Test
    void toMap_withCause_includesCauseInformation() {
        // GIVEN
        String causeMsg = "root cause";
        Throwable cause = new NullPointerException(causeMsg);
        String topMsg = "higher level failure";
        Throwable top = new RuntimeException(topMsg, cause);
        // WHEN
        Map<String, Object> result = ThrowableConversions.toMap(top, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        // Expect four entries: CLASS, MESSAGE, CAUSE, CAUSE_MESSAGE
        assertEquals(4, result.size(), "Map should contain four entries when a cause is present");
        assertEquals(top.getClass().getName(), result.get(CLASS), "Class key should contain top throwable class");
        assertEquals(topMsg, result.get(MESSAGE), "Message key should contain top throwable message");
        assertEquals(cause.getClass().getName(), result.get(CAUSE), "CAUSE key should contain cause class");
        assertEquals(causeMsg, result.get(CAUSE_MESSAGE), "CAUSE_MESSAGE key should contain cause message");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityProvider")
    void toMap_basicFunctionality(Object throwable, Expected expected) {
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = ThrowableConversions.toMap(throwable, converter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        // The map always contains CLASS and MESSAGE; CAUSE/CAUSE_MESSAGE are optional.
        int expectedSize = (expected.cause() == null) ? 2 : 4;
        assertEquals(expectedSize, result.size(), "Unexpected number of entries in the result map");
        assertEquals(expected.clazz(), result.get(CLASS), "Incorrect CLASS entry");
        assertEquals(expected.message(), result.get(MESSAGE), "Incorrect MESSAGE entry");
        if (expected.cause() != null) {
            assertEquals(expected.cause(), result.get(CAUSE), "Incorrect CAUSE entry");
            assertEquals(expected.causeMessage(), result.get(CAUSE_MESSAGE), "Incorrect CAUSE_MESSAGE entry");
        } else {
            assertFalse(result.containsKey(CAUSE), "CAUSE key should be absent");
            assertFalse(result.containsKey(CAUSE_MESSAGE), "CAUSE_MESSAGE key should be absent");
        }
    }

    @Test
    @DisplayName("toMap(null, ...) throws NullPointerException")
    void toMap_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ThrowableConversions.toMap(null, converter), "Calling toMap with a null source should throw NPE");
    }

    @Test
    @DisplayName("toMap(non‑Throwable, ...) throws ClassCastException")
    void toMap_nonThrowableSource_throwsClassCastException() {
        String notAThrowable = "just a string";
        assertThrows(ClassCastException.class, () -> ThrowableConversions.toMap(notAThrowable, converter), "Providing a non‑Throwable argument should result in a ClassCastException");
    }

    @Test
    @DisplayName("toMap captures only the immediate cause when chain is deeper")
    void toMap_deeperCauseChain_capturesImmediateOnly() {
        // top -> mid -> root
        Throwable root = new IllegalArgumentException("root");
        Throwable mid = new RuntimeException("mid", root);
        Throwable top = new Exception("top", mid);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = ThrowableConversions.toMap(top, converter);
        assertEquals(4, result.size(), "Map should contain exactly four entries");
        assertEquals(Exception.class.getName(), result.get(CLASS));
        assertEquals("top", result.get(MESSAGE));
        assertEquals(RuntimeException.class.getName(), result.get(CAUSE));
        assertEquals("mid", result.get(CAUSE_MESSAGE));
        // The root cause must NOT appear in the map
        assertNotEquals(IllegalArgumentException.class.getName(), result.get(CAUSE));
    }

    @Test
    @DisplayName("toMap handles a custom Throwable subclass")
    void toMap_customThrowableSubclass() {
        MyError custom = new MyError("custom error");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = ThrowableConversions.toMap(custom, converter);
        assertEquals(2, result.size(), "Custom throwable without cause should have two entries");
        assertEquals(MyError.class.getName(), result.get(CLASS), "Class name should match custom subclass");
        assertEquals("custom error", result.get(MESSAGE));
        assertFalse(result.containsKey(CAUSE), "No CAUSE entry expected");
        assertFalse(result.containsKey(CAUSE_MESSAGE), "No CAUSE_MESSAGE entry expected");
    }

    @Test
    @DisplayName("toMap works identically when a non‑null Converter is supplied")
    void toMap_nonNullConverterArgument() {
        // Using a real (but empty) Converter instance
        Converter realConverter = new Converter() {
        };
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = ThrowableConversions.toMap(ex, realConverter);
        assertEquals(2, result.size());
        assertEquals(IllegalArgumentException.class.getName(), result.get(CLASS));
        assertEquals("bad arg", result.get(MESSAGE));
        assertFalse(result.containsKey(CAUSE));
        assertFalse(result.containsKey(CAUSE_MESSAGE));
    }

    @Test
    @DisplayName("toMap propagates RuntimeException from getMessage()")
    void toMap_throwingMessageException_propagatesRuntimeException() {
        ThrowingMessageException ex = new ThrowingMessageException();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ThrowableConversions.toMap(ex, converter), "RuntimeException from getMessage() should propagate");
        assertEquals("Message failure", thrown.getMessage());
    }

    @Test
    @DisplayName("toMap propagates RuntimeException from getCause()")
    void toMap_throwingCauseException_propagatesRuntimeException() {
        ThrowingCauseException ex = new ThrowingCauseException();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ThrowableConversions.toMap(ex, converter), "RuntimeException from getCause() should propagate");
        assertEquals("Cause failure", thrown.getMessage());
    }
}
