package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class UUIDConversions_toBigDecimal_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special configuration for the
        // conversions used in these tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Normal conversions
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// UUID whose 128‑bit value is 1 → BigDecimal.ONE
        Arguments.of(UUID.fromString("00000000-0000-0000-0000-000000000001"), converter, BigDecimal.ONE, "UUID with value 1 should become BigDecimal.ONE"), // Numeric String "12345"
        Arguments.of("12345", converter, new BigDecimal(new BigInteger("12345")), "String \"12345\" should be parsed to the corresponding BigDecimal"), // Direct BigInteger, null converter
        Arguments.of(new BigInteger("9876543210"), null, new BigDecimal(new BigInteger("9876543210")), "Passing a BigInteger directly should return an equivalent BigDecimal"));
    }

    /* --------------------------------------------------------------
     *  Null input handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(// from == null, non‑null converter
        Arguments.of(null, converter, NullPointerException.class, "Null `from` with a non‑null Converter should throw NPE"), // both null
        Arguments.of(null, null, NullPointerException.class, "Both `from` and `converter` null should throw NPE"));
    }

    @Test
    void toBigDecimal_givenBigInteger_returnsEquivalentBigDecimal() {
        // GIVEN
        BigInteger source = new BigInteger("9876543210123456789");
        // WHEN
        BigDecimal result = UUIDConversions.toBigDecimal(source, converter);
        // THEN
        assertEquals(new BigDecimal(source), result, "BigDecimal result should be created from the supplied BigInteger");
    }

    @Test
    void toBigDecimal_givenUuid_returnsBigDecimalRepresentingUuid() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // Build the expected BigInteger representation of the UUID.
        // This mirrors the typical conversion: 128‑bit unsigned integer.
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        BigInteger expected = new BigInteger(1, buffer.array());
        // WHEN
        BigDecimal result = UUIDConversions.toBigDecimal(uuid, converter);
        // THEN
        assertEquals(new BigDecimal(expected), result, "BigDecimal should represent the UUID as an unsigned 128‑bit integer");
    }

    @Test
    void toBigDecimal_givenNull_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(source, converter), "Passing null should result in a NullPointerException from BigDecimal constructor");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConversionProvider")
    void toBigDecimal_normalConversions(Object from, Converter conv, BigDecimal expected, String description) {
        // WHEN
        BigDecimal result = UUIDConversions.toBigDecimal(from, conv);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullInputProvider")
    void toBigDecimal_nullInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> UUIDConversions.toBigDecimal(from, conv), description);
    }

    /* --------------------------------------------------------------
     *  Unsupported type conversion
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_unsupportedType_throwsIllegalArgumentException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> UUIDConversions.toBigDecimal(unsupported, converter), "Converting an unsupported type should throw IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Invalid numeric string
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_invalidNumericString_throwsNumberFormatException() {
        // GIVEN
        String badNumber = "abc123";
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> UUIDConversions.toBigDecimal(badNumber, converter), "A non‑numeric string should cause NumberFormatException");
    }

    /* --------------------------------------------------------------
     *  Missing converter for required conversion (Date → BigInteger)
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_missingConverterForDate_throwsNullPointerException() {
        // GIVEN
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(date, null), "A null Converter required for Date conversion should cause NPE");
    }

    /* --------------------------------------------------------------
     *  Null result from toBigInteger (e.g., empty map)
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_toBigIntegerReturnsNull_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> emptyMap = new LinkedHashMap<>();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(emptyMap, converter), "If toBigInteger returns null (e.g., for an empty map) the BigDecimal ctor throws NPE");
    }
}
