package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuilder_Test {

    @Mock
    private Converter // a mock, we only need it to satisfy the signature
    mockConverter;

    /**
     * Simple holder for parameterised test arguments.
     */
    private static final class ArgumentsHolder {

        final Object from;

        // may be null – the test will substitute mockConverter
        final Converter converter;

        // expected StringBuilder content (or a marker for special checks)
        final String expected;

        ArgumentsHolder(Object from, Converter converter, String expected) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "from=" + (from == null ? "null" : from.getClass().getSimpleName()) + ", converter=" + (converter == null ? "null" : converter.getClass().getSimpleName()) + ", expected=" + expected;
        }
    }

    /**
     * Dummy implementation of Converter – no behaviour needed for these tests.
     */
    private static class DummyConverter implements Converter {
        // Intentionally empty; the method under test does not call any Converter methods.
    }

    /**
     * toString() returns null – triggers a NullPointerException in StringBuilder ctor.
     */
    private static class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * toString() throws an IllegalArgumentException.
     */
    private static class ThrowingToString {

        @Override
        public String toString() {
            throw new IllegalArgumentException("boom");
        }
    }

    /**
     * toString() throws a NullPointerException (not because of a null return).
     */
    private static class NullPointerThrowingToString {

        @Override
        public String toString() {
            throw new NullPointerException("null pointer from toString");
        }
    }

    /**
     * Custom unchecked exception used to verify propagation.
     */
    private static class MyRuntimeException extends RuntimeException {

        MyRuntimeException(String msg) {
            super(msg);
        }
    }

    /**
     * toString() throws the custom unchecked exception.
     */
    private static class CustomRuntimeExceptionToString {

        @Override
        public String toString() {
            throw new MyRuntimeException("MyRuntimeException");
        }
    }

    /**
     * Provides a stream of arguments for normal (non‑exceptional) cases.
     */
    private static Stream<ArgumentsHolder> normalCasesProvider() {
        return Stream.of(// Null Input – already covered, but we keep a reference for completeness
        new ArgumentsHolder(null, null, null), // Simple String
        new ArgumentsHolder("simple string", null, "simple string"), // Integer
        new ArgumentsHolder(Integer.valueOf(123), null, "123"), // Date (epoch 0) – default Date.toString() uses the default locale/timezone.
        // The expected string is based on the UTC timezone used by the JDK.
        new ArgumentsHolder(new Date(0L), null, "Thu Jan 01 00:00:00 UTC 1970"), // Generic Object – we only assert the format, not the exact hashcode.
        new ArgumentsHolder(new Object(), null, "[object]"), // Char array – default toString() yields something like "[C@1a2b3c4d".
        new ArgumentsHolder(new char[] { 'a', 'b', 'c' }, null, "[C@"), // Empty string
        new ArgumentsHolder("", null, ""), // Control characters
        new ArgumentsHolder("\n\t\r", null, "\n\t\r"), // Unicode emojis
        new ArgumentsHolder("😀🚀", null, "😀🚀"));
    }

    /**
     * Provides cases where a non‑null Converter is supplied but must be ignored.
     */
    private static Stream<ArgumentsHolder> converterIgnoredProvider() {
        DummyConverter dummy = new DummyConverter();
        return Stream.of(new ArgumentsHolder("ignore converter", dummy, "ignore converter"), new ArgumentsHolder(Integer.valueOf(456), dummy, "456"));
    }

    @Test
    void toStringBuilder_returnsNullWhenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(input, mockConverter);
        // THEN
        assertNull(result, "Result should be null when the input object is null");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_wrapsObjectToStringInStringBuilder() {
        // GIVEN
        String input = "Hello, World!";
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a non‑null input");
        assertEquals(input, result.toString(), "StringBuilder content must match the input object's toString() value");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_usesObjectToStringEvenIfConverterWouldDoSomethingElse() {
        // GIVEN
        Object input = new Object() {

            @Override
            public String toString() {
                return "customString";
            }
        };
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("customString", result.toString(), "StringBuilder should contain the object's overridden toString() value");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("normalCasesProvider")
    void toStringBuilder_normalCases(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        Converter converter = holder.converter != null ? holder.converter : mockConverter;
        // WHEN
        StringBuilder sb = StringConversions.toStringBuilder(from, converter);
        // THEN
        if (from == null) {
            assertNull(sb, "Result must be null when input is null");
        } else {
            assertNotNull(sb, "Result must not be null for non‑null input");
            String actual = sb.toString();
            // Special handling for cases where the exact string cannot be predetermined
            if ("[object]".equals(holder.expected)) {
                // Expected format: java.lang.Object@<hash>
                assertTrue(Pattern.matches("java\\.lang\\.Object@.+", actual), "Object toString() format should match class@hashcode");
            } else if ("[C@".equals(holder.expected)) {
                // Expected format for a char[] array
                assertTrue(actual.startsWith("[C@"), "Array toString() format should start with [C@");
            } else {
                assertEquals(holder.expected, actual, "StringBuilder content must match expected toString() value");
            }
        }
        // The method never uses the Converter – verify that no interaction occurred.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0} with non‑null converter")
    @MethodSource("converterIgnoredProvider")
    void toStringBuilder_converterArgumentIsIgnored(ArgumentsHolder holder) {
        // GIVEN
        Object from = holder.from;
        // non‑null dummy
        Converter converter = holder.converter;
        // WHEN
        StringBuilder sb = StringConversions.toStringBuilder(from, converter);
        // THEN
        assertNotNull(sb);
        assertEquals(holder.expected, sb.toString(), "Converter argument must be ignored – result should equal from.toString()");
        // Ensure the dummy converter was never called.
        // (DummyConverter does not record interactions, but we can still verify the mock is untouched.)
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides a very large string (10 000 characters) to test that the whole content is copied.
     */
    @Test
    void toStringBuilder_handlesVeryLargeString() {
        // GIVEN
        int length = 10_000;
        StringBuilder source = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            source.append('x');
        }
        String largeString = source.toString();
        // WHEN
        StringBuilder result = StringConversions.toStringBuilder(largeString, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(length, result.length(), "Result length must match the input length");
        assertEquals(largeString, result.toString(), "All characters must be copied without truncation");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_throwsNullPointerExceptionWhenToStringReturnsNull() {
        // GIVEN
        Object from = new NullToString();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toStringBuilder(from, mockConverter), "When toString() returns null, StringBuilder constructor should throw NPE");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_propagatesIllegalArgumentExceptionFromToString() {
        // GIVEN
        Object from = new ThrowingToString();
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toStringBuilder(from, mockConverter), "Exception from toString() must propagate unchanged");
        assertEquals("boom", ex.getMessage());
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_propagatesNullPointerExceptionFromToString() {
        // GIVEN
        Object from = new NullPointerThrowingToString();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toStringBuilder(from, mockConverter), "NullPointerException thrown by toString() must propagate");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuilder_propagatesCustomRuntimeExceptionRegardlessOfConverter() {
        // GIVEN
        Object from = new CustomRuntimeExceptionToString();
        // non‑null, should be ignored
        Converter converter = new DummyConverter();
        // WHEN / THEN
        MyRuntimeException ex = assertThrows(MyRuntimeException.class, () -> StringConversions.toStringBuilder(from, converter), "Custom unchecked exception from toString() must bubble up unchanged");
        assertEquals("MyRuntimeException", ex.getMessage());
        // No interaction with the mock; the dummy converter is not a mock.
        verifyNoInteractions(mockConverter);
    }
}
