package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toString_Test {

    /**
     * A dummy {@link Converter} instance – the method under test does not use it.
     */
    private Converter dummyConverter;

    /**
     * Class whose {@code toString()} returns a custom non‑null string.
     */
    private static final class CustomObject {

        @Override
        public String toString() {
            return "custom-toString";
        }
    }

    /**
     * Class whose {@code toString()} returns the empty string.
     */
    private static final class CustomEmptyString {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * Base class for objects whose {@code toString()} throws a specific throwable.
     */
    private abstract static class ThrowingToString {

        @Override
        public abstract String toString();
    }

    private static final class ThrowsIllegalStateException extends ThrowingToString {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class ThrowsNullPointerException extends ThrowingToString {

        @Override
        public String toString() {
            throw new NullPointerException("np");
        }
    }

    private static final class ThrowsIllegalArgumentException extends ThrowingToString {

        @Override
        public String toString() {
            throw new IllegalArgumentException("bad arg");
        }
    }

    private static final class ThrowsRuntimeException extends ThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("runtime");
        }
    }

    private static final class ThrowsAssertionError extends ThrowingToString {

        @Override
        public String toString() {
            throw new AssertionError("assert");
        }
    }

    private static final class ReturnsNullString extends ThrowingToString {

        @Override
        public String toString() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor requires a ConverterOptions instance.
        // We can mock the options because the method under test never touches the Converter.
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getConverterOverrides()).thenReturn(java.util.Collections.emptyMap());
        dummyConverter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        dummyConverter = null;
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// null input – already covered in the base test class, but kept for completeness
        Arguments.of(null, "null input", null), // plain String (converter ignored)
        Arguments.of("hello", "plain string", "hello"), // Integer (Number) – relies on Integer.toString()
        Arguments.of(123, "integer number", "123"), // StringBuilder – overrides toString()
        Arguments.of(new StringBuilder("abc"), "StringBuilder instance", "abc"), // CustomObject – overridden toString()
        Arguments.of(new CustomObject(), "custom object with toString()", "custom-toString"), // Boolean true – Boolean.toString()
        Arguments.of(Boolean.TRUE, "boolean true", "true"), // StringBuffer – overrides toString()
        Arguments.of(new StringBuffer("buf"), "StringBuffer instance", "buf"), // Enum constant – Enum.toString() returns name
        Arguments.of(TestEnum.VALUE1, "enum constant", "VALUE1"), // Object whose toString() returns null – should propagate null
        Arguments.of(new ReturnsNullString(), "toString returns null", null), // CustomEmptyString – returns empty string
        Arguments.of(new CustomEmptyString(), "toString returns empty string", ""), // Unicode snowman character
        Arguments.of("\u2603", "unicode snowman", "\u2603"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of(new ThrowsIllegalStateException(), IllegalStateException.class, "toString throws IllegalStateException"), Arguments.of(new ThrowsNullPointerException(), NullPointerException.class, "toString throws NullPointerException"), Arguments.of(new ThrowsIllegalArgumentException(), IllegalArgumentException.class, "toString throws IllegalArgumentException"), Arguments.of(new ThrowsRuntimeException(), RuntimeException.class, "toString throws RuntimeException"), Arguments.of(new ThrowsAssertionError(), AssertionError.class, "toString throws AssertionError"));
    }

    @Test
    void toString_ReturnsNullWhenInputIsNull() {
        // given
        Object input = null;
        // when
        String result = StringConversions.toString(input, dummyConverter);
        // then
        assertNull(result, "toString should return null when the input object is null");
    }

    @Test
    void toString_DelegatesToObjectToString() {
        // given
        Object input = new Object() {

            @Override
            public String toString() {
                return "customString";
            }
        };
        // when
        String result = StringConversions.toString(input, dummyConverter);
        // then
        assertEquals("customString", result, "toString should return the result of the object's toString() method");
    }

    @Test
    void toString_UsesStandardToStringForCommonTypes() {
        // given
        Integer number = 42;
        // epoch
        java.util.Date date = new java.util.Date(0L);
        // when
        String numberStr = StringConversions.toString(number, dummyConverter);
        String dateStr = StringConversions.toString(date, dummyConverter);
        // then
        assertEquals("42", numberStr, "Integer.toString() should be used");
        assertEquals(date.toString(), dateStr, "Date.toString() should be used");
    }

    @Test
    void toString_IgnoresConverterParameter() {
        // given
        Object input = "plainString";
        // Create a mock Converter that would throw if any method were called.
        Converter mockConverter = mock(Converter.class);
        // No stubbing – any unexpected interaction will cause a test failure.
        // when
        String result = StringConversions.toString(input, mockConverter);
        // then
        assertEquals("plainString", result);
        // Verify that no interaction with the mock occurred.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("successCases")
    void toString_SuccessfulConversions(Object input, String description, String expected) {
        // when
        String result = StringConversions.toString(input, dummyConverter);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCases")
    void toString_PropagatesExceptions(Object input, Class<? extends Throwable> expectedException, String description) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.toString(input, dummyConverter), description);
        // Verify that the exception message matches the one we defined in the throwing class.
        // This is not strictly required but adds confidence that the exact exception is propagated.
        assertTrue(thrown.getMessage().contains(expectedException.getSimpleName().replaceAll("Exception|Error", "").toLowerCase()) || thrown.getMessage().contains(expectedException.getSimpleName()), "Exception message should be preserved");
    }

    @Test
    void toString_IgnoresConverterEvenWhenNull() {
        // given
        String input = "no-converter";
        // when
        String result = StringConversions.toString(input, null);
        // then
        assertEquals("no-converter", result);
    }

    @Test
    void toString_IgnoresMockedConverter() {
        // given
        String input = "mocked";
        // no stubbing – any call would fail the test
        Converter mockConverter = mock(Converter.class);
        // when
        String result = StringConversions.toString(input, mockConverter);
        // then
        assertEquals("mocked", result);
        verifyNoInteractions(mockConverter);
    }
}
