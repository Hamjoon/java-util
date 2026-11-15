package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuffer_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    class Dummy {

        @Override
        public String toString() {
            return "DummyObject";
        }
    }

    /**
     * toString() returns an empty string.
     */
    static class EmptyToString {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * toString() throws a RuntimeException.
     */
    static class ThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * toString() returns {@code null}.
     */
    static class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * toString() returns a Unicode string containing emojis.
     */
    static class UnicodeToString {

        @Override
        public String toString() {
            return "😀🚀";
        }
    }

    /**
     * toString() returns a long string of 200 'x' characters.
     */
    static class LongString {

        @Override
        public String toString() {
            return "x".repeat(200);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the mock before each test (MockitoExtension does this automatically,
        // but the explicit field is kept for clarity).
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Provides a stream of arguments for the parameterized test.
     * Each argument consists of:
     *   - the source object,
     *   - the expected content of the resulting StringBuffer (null when an exception is expected),
     *   - the expected exception class (null when no exception is expected),
     *   - a description for readability.
     */
    static Stream<Arguments> toStringBufferCases() {
        return Stream.of(// Integer 42
        Arguments.of(Integer.valueOf(42), "42", null, "Integer should be converted via its toString()"), // Empty string from overridden toString()
        Arguments.of(new EmptyToString(), "", null, "Object with empty toString() should yield empty StringBuffer"), // Throwing toString()
        Arguments.of(new ThrowingToString(), null, RuntimeException.class, "Exception from toString() must propagate"), // Boolean TRUE
        Arguments.of(Boolean.TRUE, "true", null, "Boolean true should become \"true\""), // Double 3.14
        Arguments.of(Double.valueOf(3.14), "3.14", null, "Double should be converted via its toString()"), // StringBuilder instance
        Arguments.of(new StringBuilder("builder"), "builder", null, "StringBuilder's toString() result should be used"), // Null-returning toString()
        Arguments.of(new NullToString(), null, NullPointerException.class, "toString() returning null should cause NullPointerException"), // Unicode string
        Arguments.of(new UnicodeToString(), "😀🚀", null, "Unicode characters must be preserved"), // Long string (200 'x')
        Arguments.of(new LongString(), "x".repeat(200), null, "Long string should be handled without truncation"));
    }

    @Test
    void toStringBuffer_NullInput_ReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
        // Verify that the converter was never touched.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_StringInput_ReturnsStringBufferWithSameContent() {
        // GIVEN
        String source = "Hello, World!";
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a non‑null source");
        assertEquals(source, result.toString(), "StringBuffer content should match the source string");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_CustomObject_UsesObjectToString() {
        // GIVEN
        class Dummy {

            @Override
            public String toString() {
                return "DummyObject";
            }
        }
        Dummy source = new Dummy();
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a non‑null source");
        assertEquals("DummyObject", result.toString(), "StringBuffer should contain the value returned by the object's toString()");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("toStringBufferCases")
    void toStringBuffer_AdditionalCases(Object source, String expectedContent, Class<? extends Throwable> expectedException, String description) {
        // GIVEN – source object already created by the provider
        // WHEN / THEN
        if (expectedException != null) {
            // Expecting an exception during conversion
            Throwable thrown = assertThrows(expectedException, () -> StringConversions.toStringBuffer(source, mockConverter), "Expected exception was not thrown for case: " + description);
            // For RuntimeException case we also verify that the original cause is preserved
            if (expectedException == RuntimeException.class && source instanceof ThrowingToString) {
                assertEquals("boom", thrown.getMessage(), "Exception message should match the one thrown by toString()");
            }
        } else {
            StringBuffer result = StringConversions.toStringBuffer(source, mockConverter);
            assertNotNull(result, "Result should not be null for case: " + description);
            assertEquals(expectedContent, result.toString(), "StringBuffer content mismatch for case: " + description);
        }
        // The method must never interact with the Converter.
        verifyNoInteractions(mockConverter);
    }
}
