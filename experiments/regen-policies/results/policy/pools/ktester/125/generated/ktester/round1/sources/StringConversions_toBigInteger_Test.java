package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // A real Converter instance – the method under test does not use it,
        // but we keep it for the cases where a non‑null value is required.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        converter = null;
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Empty and null strings are treated as “no value”.
        Arguments.of("", null, BigInteger.ZERO), Arguments.of(null, null, BigInteger.ZERO), // Plain integer strings.
        Arguments.of("12345", null, new BigInteger("12345")), Arguments.of("-9876", null, new BigInteger("-9876")), // Decimal strings – fractional part is truncated toward zero.
        Arguments.of("12345.678", null, new BigInteger("12345")), Arguments.of("-12345.678", null, new BigInteger("-12345")), // Very large integer.
        Arguments.of("99999999999999999999999999999999999999", null, new BigInteger("99999999999999999999999999999999999999")), // Explicit plus sign.
        Arguments.of("+12345", null, new BigInteger("12345")), // Leading zeros are ignored.
        Arguments.of("0000123", null, new BigInteger("123")), // Zero representations.
        Arguments.of("0", null, BigInteger.ZERO), Arguments.of("-0", null, BigInteger.ZERO), Arguments.of("+0", null, BigInteger.ZERO), // Scientific notation – BigDecimal handles it.
        Arguments.of("1e3", null, new BigInteger("1000")), Arguments.of("1.23E2", null, new BigInteger("123")), Arguments.of("1.23E1", null, new BigInteger("12")), // Fractions less than 1 truncate to zero.
        Arguments.of("0.9999", null, BigInteger.ZERO), Arguments.of("-0.9999", null, BigInteger.ZERO), // Using a non‑null (mocked) Converter – it is ignored.
        Arguments.of("123", mockConverter(), new BigInteger("123")));
    }

    /**
     * Helper that creates a Mockito mock of {@link Converter}.
     * The mock is not used by the method under test, but it allows us to verify that
     * the implementation truly ignores the converter argument.
     */
    private static Converter mockConverter() {
        return org.mockito.Mockito.mock(Converter.class);
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(Arguments.of("not-a-number"), // whitespace only – not considered empty
        Arguments.of("   "), // leading/trailing whitespace not trimmed
        Arguments.of("   123   "), Arguments.of("NaN"), Arguments.of("Infinity"), // suffix not supported
        Arguments.of("12345L"), // hexadecimal not supported
        Arguments.of("0xFF"));
    }

    private static Stream<Arguments> provideNonStringInputs() {
        return Stream.of(Arguments.of(new StringBuilder("123")), // Integer
        Arguments.of(123), Arguments.of(new Object()));
    }

    @Test
    void emptyStringReturnsZero() {
        // GIVEN
        String input = "";
        // WHEN
        BigInteger result = StringConversions.toBigInteger(input, converter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "Empty string should be converted to BigInteger.ZERO");
    }

    @Test
    void validNumericStringIsConverted() {
        // GIVEN
        String input = "123456789012345678901234567890";
        // WHEN
        BigInteger result = StringConversions.toBigInteger(input, converter);
        // THEN
        assertEquals(new BigInteger(input), result, "The method should correctly parse a valid numeric string into a BigInteger");
    }

    @Test
    void invalidStringThrowsIllegalArgumentException() {
        // GIVEN
        String input = "not-a-number";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(input, converter), "An invalid numeric string should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains("not parseable as a BigInteger value"), "Exception message should indicate the value is not parseable");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original parsing exception");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" => {2}")
    @MethodSource("provideValidConversions")
    void toBigInteger_returnsExpectedValue(Object from, Converter conv, BigInteger expected) {
        // WHEN
        BigInteger result = StringConversions.toBigInteger(from, conv);
        // THEN
        assertEquals(expected, result, () -> "Conversion of " + from + " should produce " + expected);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" throws IllegalArgumentException")
    @MethodSource("provideInvalidConversions")
    void toBigInteger_throwsIllegalArgumentException(Object from) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(from, null));
        assertNotNull(ex.getCause(), "The IllegalArgumentException should wrap the original parsing exception");
    }

    @ParameterizedTest(name = "[{index}] from={0} (type {0.getClass().getSimpleName()}) throws ClassCastException")
    @MethodSource("provideNonStringInputs")
    void toBigInteger_nonStringInput_throwsClassCastException(Object from) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toBigInteger(from, null));
    }
}
