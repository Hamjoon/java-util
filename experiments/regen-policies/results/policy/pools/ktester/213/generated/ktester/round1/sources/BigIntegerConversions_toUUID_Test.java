package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toUUID_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter can be constructed with default options – no special configuration needed.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    /**
     * Provides all normal (non‑exceptional) test cases described in the JSON specification.
     */
    private static Stream<Arguments> validBigIntegerProvider() {
        return Stream.of(// zero – nil UUID
        Arguments.of(BigInteger.ZERO, "00000000-0000-0000-0000-000000000000", "Zero value should produce the nil UUID"), // small number (1) – requires full left padding
        Arguments.of(BigInteger.ONE, "00000000-0000-0000-0000-000000000001", "Single‑digit hex value should be padded to 32 characters"), // small number (123456789) – hex "75bcd15"
        Arguments.of(new BigInteger("123456789"), "00000000-0000-0000-0000-000000075bcd15", "Decimal 123456789 (hex 75bcd15) padded correctly"), // exact 128‑bit max value (2^128‑1) – all f's
        Arguments.of(new BigInteger("340282366920938463463374607431768211455"), "ffffffff-ffff-ffff-ffff-ffffffffffff", "Maximum unsigned 128‑bit value should yield all‑f UUID"), // 2^128 – 33‑digit hex, method keeps first 32 digits
        Arguments.of(new BigInteger("340282366920938463463374607431768211456"), "10000000-0000-0000-0000-000000000000", "Value 2^128 should be truncated to first 32 hex digits"), // 31‑digit hex (one leading zero needed)
        Arguments.of(new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16), "0fffffff-ffff-ffff-ffff-ffffffffffff", "31‑digit hex requires a single leading zero padding"), // half‑size (16 hex digits) – padded with 16 leading zeros
        Arguments.of(new BigInteger("1234567890abcdef", 16), "00000000-0000-0000-1234-567890abcdef", "16‑digit hex value padded to full 32‑digit UUID"), // just over half (17 hex digits) – padded with 15 leading zeros
        Arguments.of(new BigInteger("1234567890abcdef1", 16), "00000000-0000-0001-2345-67890abcdef1", "17‑digit hex value padded correctly across hyphen boundaries"), // larger than 128 bits (33 hex digits) – first 32 characters used
        Arguments.of(new BigInteger("200000000000000000000000000000000", 16), "20000000-0000-0000-0000-000000000000", "33‑digit hex (beyond 128 bits) truncated to first 32 digits"));
    }

    @Test
    void toUUID_positiveBigInteger_returnsExpectedUuid() {
        // GIVEN
        // 0x1234567890abcdef1234567890abcdef (128‑bit value)
        BigInteger bigInt = new BigInteger("1234567890abcdef1234567890abcdef", 16);
        String expectedUuidString = "12345678-90ab-cdef-1234-567890abcdef";
        UUID expected = UUID.fromString(expectedUuidString);
        // WHEN
        UUID actual = BigIntegerConversions.toUUID(bigInt, converter);
        // THEN
        assertEquals(expected, actual, "The UUID produced from the BigInteger does not match the expected value.");
    }

    @Test
    void toUUID_zero_returnsNilUuid() {
        // GIVEN
        BigInteger zero = BigInteger.ZERO;
        // nil UUID
        UUID expected = new UUID(0L, 0L);
        // WHEN
        UUID actual = BigIntegerConversions.toUUID(zero, converter);
        // THEN
        assertEquals(expected, actual, "Conversion of zero should yield the nil UUID.");
    }

    @Test
    void toUUID_smallNumber_isPaddedCorrectly() {
        // GIVEN
        // Only a single hex digit; the method must left‑pad to 32 characters.
        BigInteger one = BigInteger.ONE;
        String expectedUuidString = "00000000-0000-0000-0000-000000000001";
        UUID expected = UUID.fromString(expectedUuidString);
        // WHEN
        UUID actual = BigIntegerConversions.toUUID(one, converter);
        // THEN
        assertEquals(expected, actual, "The UUID string should be padded with leading zeros.");
    }

    @Test
    void toUUID_negativeBigInteger_throwsIllegalArgumentException() {
        // GIVEN
        BigInteger negative = new BigInteger("-1");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toUUID(negative, converter));
        assertTrue(ex.getMessage().contains("Cannot convert a negative number"), "Exception message should indicate that negative numbers are not allowed.");
    }

    /**
     * Tests a variety of valid {@link BigInteger} inputs and checks that the resulting
     * {@link UUID} matches the expected string representation.
     *
     * @param from               the {@code BigInteger} to convert
     * @param expectedUuidString the expected UUID formatted as a string
     * @param description        a short description of the test case (used only for readability)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validBigIntegerProvider")
    void toUUID_validInputs(BigInteger from, String expectedUuidString, String description) {
        // GIVEN – the test data is supplied by the method source
        // WHEN
        UUID actual = BigIntegerConversions.toUUID(from, /*converter=*/
        null);
        // THEN
        UUID expected = UUID.fromString(expectedUuidString);
        assertEquals(expected, actual, () -> "UUID mismatch for case: " + description);
    }

    @Test
    void toUUID_withNonNullConverter_ignoresConverter() {
        // GIVEN
        // decimal 12345 = 0x3039
        BigInteger value = new BigInteger("12345");
        String expectedUuidString = "00000000-0000-0000-0000-000000003039";
        UUID expected = UUID.fromString(expectedUuidString);
        // WHEN
        UUID actual = BigIntegerConversions.toUUID(value, converter);
        // THEN
        assertEquals(expected, actual, "Method should ignore the provided Converter and produce the correct UUID.");
    }

    @Test
    void toUUID_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toUUID(nullFrom, null), "Passing null as the 'from' argument should result in NullPointerException.");
    }

    @Test
    void toUUID_nonBigIntegerInput_throwsClassCastException() {
        // GIVEN
        Object notABigInteger = "notABigInteger";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toUUID(notABigInteger, null), "Providing an object that is not a BigInteger should cause ClassCastException.");
    }
}
