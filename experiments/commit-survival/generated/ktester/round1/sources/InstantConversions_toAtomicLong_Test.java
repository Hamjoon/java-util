package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toAtomicLong_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        // If the library provides a no‑arg constructor for ConverterOptions we use it,
        // otherwise we create an empty options object.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Long
        Arguments.of(123L, 123L, "Long value should be passed directly to toLong and wrapped"), // Integer
        Arguments.of(Integer.valueOf(456), 456L, "Integer is converted to long by toLong and then wrapped"), // Short
        Arguments.of(Short.valueOf((short) 78), 78L, "Short is promoted to long"), // Byte
        Arguments.of(Byte.valueOf((byte) 9), 9L, "Byte is promoted to long"), // Double (truncated)
        Arguments.of(Double.valueOf(12.0), 12L, "Floating‑point numbers are truncated to long before wrapping"), // Float (truncated)
        Arguments.of(Float.valueOf(34.0f), 34L, "Float is truncated to long"), // BigInteger
        Arguments.of(new BigInteger("1234567890123456789"), 1234567890123456789L, "BigInteger should be converted to long and wrapped"), // BigDecimal (truncated)
        Arguments.of(new BigDecimal("98765.4321"), 98765L, "BigDecimal is truncated to long before wrapping"), // Instant epoch millis
        Arguments.of(Instant.ofEpochMilli(1_609_459_200_123L), 1_609_459_200_123L, "Instant epoch milliseconds are used as the long value"), // java.util.Date
        Arguments.of(new Date(1_620_000_000_000L), 1_620_000_000_000L, "java.util.Date milliseconds since epoch are converted to long"), // Null
        Arguments.of(null, 0L, "Null input should be treated as zero by toLong"), // Numeric String
        Arguments.of("12345", 12345L, "String containing a numeric value should be parsed to long"), // AtomicLong input (unwrapped and re‑wrapped)
        Arguments.of(new AtomicLong(555L), 555L, "AtomicLong input should be unwrapped and re‑wrapped unchanged"));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Non‑numeric string
        Arguments.of("notANumber", NumberFormatException.class, "Non‑numeric string should cause a NumberFormatException"), // Empty string
        Arguments.of("", NumberFormatException.class, "Empty string cannot be parsed to a number, should throw NumberFormatException"), // Whitespace only string
        Arguments.of("   ", NumberFormatException.class, "String with only whitespace is not a valid numeric representation, expecting NumberFormatException"), // Collection type
        Arguments.of(new ArrayList<>(), IllegalArgumentException.class, "A collection type is unsupported for conversion to long, should raise IllegalArgumentException"), // Map type
        Arguments.of(new HashMap<>(), IllegalArgumentException.class, "A map type is unsupported for conversion to long, should raise IllegalArgumentException"), // java.sql.Time (unsupported)
        Arguments.of(new Time(0L), IllegalArgumentException.class, "java.sql.Time is not explicitly handled; conversion should fail with IllegalArgumentException"), // java.time.Duration (unsupported)
        Arguments.of(Duration.ofSeconds(10), IllegalArgumentException.class, "Unsupported java.time type (Duration) should trigger IllegalArgumentException"), // Array type
        Arguments.of(new Object[] { 1, 2, 3 }, IllegalArgumentException.class, "Array input is not supported, should result in IllegalArgumentException"));
    }

    @Test
    void toAtomicLong_givenLong_returnsAtomicLongWithSameValue() {
        // given
        Long source = 123L;
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(123L, result.get(), "AtomicLong should contain the original long value");
    }

    @Test
    void toAtomicLong_givenInteger_returnsAtomicLongWithConvertedValue() {
        // given
        Integer source = 42;
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(42L, result.get(), "Integer should be converted to the same numeric long value");
    }

    @Test
    void toAtomicLong_givenDate_returnsAtomicLongWithEpochMillis() {
        // given
        // a fixed epoch millisecond value
        Date source = new Date(1_600_000_000_000L);
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(1_600_000_000_000L, result.get(), "Date should be converted to its epoch millisecond representation");
    }

    @Test
    void toAtomicLong_givenNull_returnsAtomicLongWithZero() {
        // given
        Object source = null;
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.get(), "Null input should be converted to zero for primitive numeric targets");
    }

    @Test
    void toAtomicLong_givenShort_returnsAtomicLongWithConvertedValue() {
        // given
        Short source = 7;
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(7L, result.get());
    }

    @Test
    void toAtomicLong_givenByte_returnsAtomicLongWithConvertedValue() {
        // given
        Byte source = 9;
        // when
        AtomicLong result = InstantConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result);
        assertEquals(9L, result.get());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toAtomicLong_validConversions(Object from, long expected, String description) {
        // when
        AtomicLong result = InstantConversions.toAtomicLong(from, converter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionConversionProvider")
    void toAtomicLong_invalidConversions(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> InstantConversions.toAtomicLong(from, converter), description);
    }
}
