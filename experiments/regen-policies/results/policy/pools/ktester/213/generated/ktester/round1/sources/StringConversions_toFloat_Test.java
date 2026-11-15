package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toFloat_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideValidFloatConversions() {
        Converter dummyConverter = new Converter() {
        };
        return Stream.of(// Empty string → 0.0F
        org.junit.jupiter.params.provider.Arguments.of("", null, 0.0f), // Null input → 0.0F
        org.junit.jupiter.params.provider.Arguments.of(null, null, 0.0f), // Simple decimal
        org.junit.jupiter.params.provider.Arguments.of("123.45", null, 123.45f), // Integer string
        org.junit.jupiter.params.provider.Arguments.of("10", null, 10.0f), // Scientific notation
        org.junit.jupiter.params.provider.Arguments.of("1e3", null, 1000.0f), // Explicit plus sign
        org.junit.jupiter.params.provider.Arguments.of("+5.5", null, 5.5f), // Special values
        org.junit.jupiter.params.provider.Arguments.of("NaN", null, Float.NaN), org.junit.jupiter.params.provider.Arguments.of("Infinity", null, Float.POSITIVE_INFINITY), org.junit.jupiter.params.provider.Arguments.of("-Infinity", null, Float.NEGATIVE_INFINITY), // Negative zero
        org.junit.jupiter.params.provider.Arguments.of("-0.0", null, -0.0f), // Overflow – results in positive infinity
        org.junit.jupiter.params.provider.Arguments.of("1e40", null, Float.POSITIVE_INFINITY), // Non‑null converter is ignored
        org.junit.jupiter.params.provider.Arguments.of("42.42", dummyConverter, 42.42f));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideInvalidFloatConversions() {
        return Stream.of(// Non‑numeric string
        org.junit.jupiter.params.provider.Arguments.of("abc", null, IllegalArgumentException.class), // Whitespace‑only string – Float.valueOf does not trim, so it fails
        org.junit.jupiter.params.provider.Arguments.of("   ", null, IllegalArgumentException.class), // String with leading/trailing spaces
        org.junit.jupiter.params.provider.Arguments.of(" 12.3 ", null, IllegalArgumentException.class), // Hexadecimal floating‑point literal (unsupported)
        org.junit.jupiter.params.provider.Arguments.of("0x1.0p0", null, IllegalArgumentException.class), // Underscore in numeric literal (unsupported)
        org.junit.jupiter.params.provider.Arguments.of("1_000.0", null, IllegalArgumentException.class), // Non‑String object – cast to String fails
        org.junit.jupiter.params.provider.Arguments.of(5, null, ClassCastException.class));
    }

    @Test
    @DisplayName("Valid numeric string is converted to Float")
    void testToFloatWithValidString() {
        // given
        String source = "123.45";
        // when
        Float result = StringConversions.toFloat(source, mockConverter);
        // then
        assertEquals(123.45f, result);
    }

    @Test
    @DisplayName("Empty string returns 0.0F")
    void testToFloatWithEmptyString() {
        // given
        String source = "";
        // when
        Float result = StringConversions.toFloat(source, mockConverter);
        // then
        assertEquals(0.0f, result);
    }

    @Test
    @DisplayName("Null source returns 0.0F")
    void testToFloatWithNull() {
        // given
        Object source = null;
        // when
        Float result = StringConversions.toFloat(source, mockConverter);
        // then
        assertEquals(0.0f, result);
    }

    @Test
    @DisplayName("Non‑numeric string throws IllegalArgumentException")
    void testToFloatWithInvalidString() {
        // given
        String source = "not-a-number";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toFloat(source, mockConverter));
        assertTrue(ex.getMessage().contains("not-a-number"));
        assertNotNull(ex.getCause());
    }

    @Test
    @DisplayName("Non‑String object causes ClassCastException")
    void testToFloatWithNonStringObject() {
        // given
        Integer source = 42;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toFloat(source, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={2}")
    @MethodSource("provideValidFloatConversions")
    @DisplayName("Valid inputs are converted to the expected Float")
    void testToFloatValid(Object from, Converter converter, Float expected) {
        // when
        Float result = StringConversions.toFloat(from, converter);
        // then
        if (Float.isNaN(expected)) {
            assertTrue(Float.isNaN(result), "Result should be NaN");
        } else if (expected.equals(Float.POSITIVE_INFINITY)) {
            assertEquals(Float.POSITIVE_INFINITY, result);
        } else if (expected.equals(Float.NEGATIVE_INFINITY)) {
            assertEquals(Float.NEGATIVE_INFINITY, result);
        } else if (expected.equals(-0.0f)) {
            // Distinguish -0.0 from +0.0 via bit pattern
            assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(result));
        } else {
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {2}")
    @MethodSource("provideInvalidFloatConversions")
    @DisplayName("Invalid inputs cause the expected exception")
    void testToFloatInvalid(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.toFloat(from, converter));
        // Additional sanity checks for IllegalArgumentException
        if (expectedException == IllegalArgumentException.class) {
            assertNotNull(thrown.getCause(), "Cause should be present for IllegalArgumentException");
        }
    }
}
