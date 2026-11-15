package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.nio.CharBuffer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuffer_Test {

    @Mock
    private Converter // a mock – the method under test should never use it
    mockConverter;

    /**
     * Simple class whose toString() returns a configurable value.
     */
    private static final class CustomToString {

        private final String value;

        CustomToString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * toString() returns null – triggers NPE in StringBuffer constructor.
     */
    private static final class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * toString() returns empty string.
     */
    private static final class EmptyToString {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * toString() returns a Unicode string.
     */
    private static final class UnicodeToString {

        private final String payload;

        UnicodeToString(String payload) {
            this.payload = payload;
        }

        @Override
        public String toString() {
            return payload;
        }
    }

    /**
     * toString() throws a supplied throwable when invoked.
     * Used to verify that the method under test does not swallow exceptions.
     */
    private static final class ExceptionToString {

        private final RuntimeException exception;

        ExceptionToString(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public String toString() {
            throw exception;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mocked Converter was never interacted with
        verifyNoInteractions(mockConverter);
        // Ensure the method truly ignores the Converter argument
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides the stream of test cases for the successful conversions.
     */
    private static Stream<TestCase> successCases() {
        return Stream.of(new TestCase(123, "123", null, "Non‑null Integer should be converted via toString()"), new TestCase("Hello World", "Hello World", null, "String instance should be wrapped unchanged"), new TestCase(new CustomToString("custom-value"), "custom-value", null, "Custom object with overridden toString()"), new TestCase("", "", null, "Empty String should produce an empty StringBuffer"), new TestCase(new StringBuilder("builder"), "builder", null, "StringBuilder – toString() result must be wrapped"), new TestCase(new StringBuffer("buf"), "buf", null, "StringBuffer – a new instance must be created"), new TestCase(45.67, "45.67", null, "Double value should be converted via toString()"), new TestCase(Boolean.FALSE, "false", null, "Boolean false should be converted via toString()"), new TestCase(new EmptyToString(), "", null, "Custom object whose toString() returns empty string"), new TestCase(new UnicodeToString("こんにちは"), "こんにちは", null, "Unicode characters must be preserved"), new TestCase(new Timestamp(0L), "1970-01-01 00:00:00.0", null, "Timestamp epoch representation"), new TestCase(CharBuffer.wrap("charbuf"), "charbuf", null, "CharBuffer's toString() returns its contents"));
    }

    /**
     * Provides the stream of test cases that are expected to throw.
     */
    private static Stream<TestCase> exceptionCases() {
        return Stream.of(new TestCase(new NullToString(), null, NullPointerException.class, "toString() returns null – StringBuffer ctor throws NPE"), new TestCase(new ExceptionToString(new IllegalStateException()), null, IllegalStateException.class, "toString() throws IllegalStateException – must propagate"), new TestCase(new ExceptionToString(new IllegalArgumentException()), null, IllegalArgumentException.class, "toString() throws IllegalArgumentException – must propagate"), new TestCase(new ExceptionToString(new AssertionError()), null, AssertionError.class, "toString() throws AssertionError – must propagate unchanged"));
    }

    @Test
    void toStringBuffer_ReturnsNull_WhenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNull(result, "Result should be null when the input object is null");
    }

    @Test
    void toStringBuffer_ReturnsStringBuffer_WithObjectToString() {
        // GIVEN
        Object input = new Object() {

            @Override
            public String toString() {
                return "customString";
            }
        };
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a non‑null input");
        assertEquals("customString", result.toString(), "StringBuffer content must match the object's toString() value");
    }

    @Test
    void toStringBuffer_ReturnsStringBuffer_ForStandardTypes() {
        // GIVEN
        Integer input = 12345;
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("12345", result.toString(), "StringBuffer should contain the integer's string representation");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void toStringBuffer_ConvertsSuccessfully(TestCase tc) {
        // GIVEN
        Object input = tc.from();
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result, tc.description());
        assertEquals(tc.expectedContent(), result.toString(), "StringBuffer content does not match expected for case: " + tc.description());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void toStringBuffer_PropagatesExceptions(TestCase tc) {
        // GIVEN
        Object input = tc.from();
        // WHEN / THEN
        Throwable thrown = assertThrows(tc.expectedException(), () -> StringConversions.toStringBuffer(input, mockConverter), "Expected exception not thrown for case: " + tc.description());
        // Optional: verify the exception message matches the one we set (if any)
        if (tc.expectedException() == NullPointerException.class) {
            // NPE from StringBuffer constructor has a standard message
            assertTrue(thrown.getMessage() == null || thrown.getMessage().isEmpty(), "NullPointerException from StringBuffer should have no message");
        }
    }
}
