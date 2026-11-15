package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuilder_Test {

    @Mock
    private Converter // Converter is not used by the method, but we provide a mock
    mockConverter;

    /**
     * An object whose {@code toString()} deliberately returns {@code null}.
     */
    private static final class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * An object whose {@code toString()} throws an {@link IllegalStateException}.
     */
    private static final class BadToString {

        @Override
        public String toString() {
            throw new IllegalStateException("Bad toString!");
        }
    }

    /**
     * Simple holder for parameterized test data.
     */
    private static final class SimpleCase {

        final Object input;

        final String expected;

        SimpleCase(Object input, String expected) {
            this.input = input;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.valueOf(input);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method never interacts with the converter
        verifyNoInteractions(mockConverter);
    }

    private static Stream<SimpleCase> simpleCasesProvider() {
        return Stream.of(new SimpleCase("hello world", "hello world"), new SimpleCase(123, "123"));
    }

    @Test
    void toStringBuilder_NullInput_ReturnsNull() {
        StringBuilder result = StringConversions.toStringBuilder(null, mockConverter);
        assertNull(result, "Result should be null when the source object is null");
    }

    @Test
    void toStringBuilder_StringInput_ReturnsMatchingStringBuilder() {
        String source = "Hello, World!";
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(result, "Result should not be null for non‑null source");
        assertEquals(source, result.toString(), "StringBuilder content must match source string");
    }

    @Test
    void toStringBuilder_IntegerInput_ReturnsStringBuilderWithIntegerString() {
        Integer source = 42;
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(result);
        assertEquals("42", result.toString(), "StringBuilder should contain the integer's toString value");
    }

    @Test
    void toStringBuilder_CustomObject_UsesItsToString() {
        Object source = new Object() {

            @Override
            public String toString() {
                return "CustomObjectString";
            }
        };
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(result);
        assertEquals("CustomObjectString", result.toString(), "StringBuilder should contain the custom object's toString value");
    }

    @Test
    void toStringBuilder_EmptyString_ReturnsEmptyStringBuilder() {
        String source = "";
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(result, "Result must not be null for empty string input");
        assertEquals(0, result.length(), "StringBuilder should be empty for empty string input");
    }

    @Test
    void toStringBuilder_LargeContent_IsFullyTransferred() {
        // Build a string > 1000 characters (here 1500 'a's)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            sb.append('a');
        }
        String largeString = sb.toString();
        StringBuilder result = StringConversions.toStringBuilder(largeString, mockConverter);
        assertNotNull(result, "Result must not be null for large string input");
        assertEquals(largeString, result.toString(), "StringBuilder must contain the full large string without truncation");
        assertEquals(1500, result.length(), "Length of the resulting StringBuilder should be 1500");
    }

    @Test
    void toStringBuilder_ConverterArgumentIsIgnored() {
        String source = "ignore me";
        // Pass the mockConverter explicitly – the method should ignore it.
        StringBuilder result = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(result);
        assertEquals(source, result.toString(), "Result must reflect the source string regardless of the converter argument");
    }

    @Test
    void toStringBuilder_MultipleCallsReturnDistinctInstances() {
        String source = "repeat";
        StringBuilder first = StringConversions.toStringBuilder(source, mockConverter);
        StringBuilder second = StringConversions.toStringBuilder(source, mockConverter);
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(source, first.toString());
        assertEquals(source, second.toString());
        assertNotSame(first, second, "Each call must return a distinct StringBuilder instance");
    }

    @Test
    void toStringBuilder_ToStringReturnsNull_ThrowsNullPointerException() {
        Object source = new NullToString();
        // The constructor of StringBuilder throws NPE when passed a null argument.
        assertThrows(NullPointerException.class, () -> StringConversions.toStringBuilder(source, mockConverter), "When source.toString() returns null, a NullPointerException should be thrown");
    }

    @Test
    void toStringBuilder_ToStringThrowsException_PropagatesException() {
        Object source = new BadToString();
        // The IllegalStateException from toString() must propagate unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.toStringBuilder(source, mockConverter), "Exception from source.toString() should propagate");
        assertEquals("Bad toString!", ex.getMessage());
    }

    @ParameterizedTest(name = "toStringBuilder({0}) should produce \"{1}\"")
    @MethodSource("simpleCasesProvider")
    void toStringBuilder_SimpleTypes_VariousInputs(Object input, String expected) {
        StringBuilder result = StringConversions.toStringBuilder(input, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), "StringBuilder content must match the expected string representation");
    }
}
