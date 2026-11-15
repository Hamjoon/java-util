package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class StringConversions_toFloat_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required by the signature of toFloat, even though the method
        // does not use it.  Create a default instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
        converter = null;
    }

    private static Stream<Arguments> emptyOrNullProvider() {
        return Stream.of(// empty string, null converter
        Arguments.of("", null), // null string, null converter
        Arguments.of(null, null));
    }

    private static Stream<Arguments> validNumberProvider() {
        return Stream.of(Arguments.of("123", 123.0F), Arguments.of("-45.67", -45.67F), Arguments.of("+8.9", 8.9F), Arguments.of("0", 0.0F), // Float.MAX_VALUE = 3.4028235E38
        Arguments.of("3.4028235e38", Float.MAX_VALUE));
    }

    private static Stream<Arguments> specialFloatProvider() {
        return Stream.of(Arguments.of("NaN", Float.NaN), Arguments.of("Infinity", Float.POSITIVE_INFINITY), Arguments.of("-Infinity", Float.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> invalidNumberProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of("1.2.3"), Arguments.of(" "), Arguments.of("--5"));
    }

    private static Stream<Arguments> nonStringProvider() {
        return Stream.of(// Integer
        Arguments.of(123), // Arbitrary Object
        Arguments.of(new Object()));
    }

    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(// leading space
        Arguments.of(" 123.45"), // trailing space + tab
        Arguments.of("-0.1 \t"));
    }

    @Test
    void toFloat_EmptyString_ReturnsZero() {
        // given
        String empty = "";
        // when
        Float result = StringConversions.toFloat(empty, converter);
        // then
        assertEquals(0.0F, result);
    }

    @Test
    void toFloat_NullString_ReturnsZero() {
        // given
        String nullStr = null;
        // when
        Float result = StringConversions.toFloat(nullStr, converter);
        // then
        assertEquals(0.0F, result);
    }

    @Test
    void toFloat_ValidNumberString_ReturnsParsedFloat() {
        // given
        String number = "123.45";
        // when
        Float result = StringConversions.toFloat(number, converter);
        // then
        assertEquals(123.45F, result);
    }

    @Test
    void toFloat_InvalidNumberString_ThrowsIllegalArgumentException() {
        // given
        String bad = "not-a-float";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toFloat(bad, converter));
        assertTrue(ex.getMessage().contains("not parseable as a float"));
        assertNotNull(ex.getCause());
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("emptyOrNullProvider")
    void toFloat_EmptyOrNull_ReturnsZero(Object from, Converter conv) {
        Float result = StringConversions.toFloat(from, conv);
        assertEquals(0.0F, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from=\"{0}\"")
    @MethodSource("validNumberProvider")
    void toFloat_ValidNumberStrings(String from, Float expected) {
        Float result = StringConversions.toFloat(from, converter);
        assertEquals(expected, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from=\"{0}\"")
    @MethodSource("specialFloatProvider")
    void toFloat_SpecialValues(String from, Float expected) {
        Float result = StringConversions.toFloat(from, converter);
        if (expected.isNaN()) {
            assertTrue(result.isNaN(), "Result should be NaN");
        } else if (expected.isInfinite()) {
            assertTrue(result.isInfinite() && result.equals(expected), "Result should be " + expected);
        } else {
            assertEquals(expected, result);
        }
    }

    // --------------------------------------------------------------------- //
    @Test
    void toFloat_Overflow_ReturnsInfinity() {
        String from = "1e40";
        Float result = StringConversions.toFloat(from, converter);
        assertTrue(result.isInfinite() && result > 0, "Result should be positive infinity for overflow input");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from=\"{0}\"")
    @MethodSource("invalidNumberProvider")
    void toFloat_InvalidNumbers_ThrowsIllegalArgumentException(String from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toFloat(from, converter));
        assertNotNull(ex.getCause());
        assertTrue(ex.getMessage().contains("not parseable as a float"));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("nonStringProvider")
    void toFloat_NonStringInput_ThrowsClassCastException(Object from) {
        assertThrows(ClassCastException.class, () -> StringConversions.toFloat(from, converter));
    }

    // --------------------------------------------------------------------- //
    @Test
    void toFloat_WithCustomConverter_IgnoresIt() {
        // could be a mock
        Converter dummy = new Converter(new ConverterOptions());
        Float result = StringConversions.toFloat("10.5", dummy);
        assertEquals(10.5F, result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "{index}: from=\"{0}\"")
    @MethodSource("whitespaceProvider")
    void toFloat_WhitespaceStrings_ThrowsIllegalArgumentException(String from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toFloat(from, converter));
        assertNotNull(ex.getCause());
    }
}
