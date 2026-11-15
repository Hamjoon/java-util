package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toAtomicLong_Test {

    private static Converter converter;

    /**
     * Mock that returns a fixed {@code Long} value – used for the “myConverter” case.
     */
    @Mock
    private static Converter myConverter;

    /**
     * Mock that throws a {@code RuntimeException} when invoked – used for the “throwingConverter” case.
     */
    @Mock
    private static Converter throwingConverter;

    /**
     * Mock that returns a {@code String} instead of a {@code Number} – used for the “converterReturningString” case.
     */
    @Mock
    private static Converter converterReturningString;

    /**
     * Mock that returns {@code null} – used for the “converterReturningNull” case.
     */
    @Mock
    private static Converter converterReturningNull;

    @BeforeAll
    static void setupBeforeAll() {
        // The library’s default configuration.
        converter = new Converter(new ConverterOptions());
        // Stubbing behaviour for the custom converters.
        // myConverter → returns 555L for any conversion request.
        // throwingConverter → throws RuntimeException.
        // converterReturningString → returns a String (invalid for long conversion).
        // converterReturningNull → returns null.
        // The stubbing is performed in a static block because the @Mock fields are
        // initialised by MockitoExtension before each test method runs.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock interactions before each test.
        reset(myConverter, throwingConverter, converterReturningString, converterReturningNull);
        when(myConverter.convert(any(), eq(Long.class))).thenReturn(555L);
        when(throwingConverter.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("Converter failure"));
        when(converterReturningString.convert(any(), eq(Long.class))).thenReturn("not-a-long");
        when(converterReturningNull.convert(any(), eq(Long.class))).thenReturn(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> conversionTestCases() {
        return Stream.of(// Long
        Arguments.of("Long value", 123L, null, 123L), // String numeric
        Arguments.of("String numeric", "456", null, 456L), // BigInteger
        Arguments.of("BigInteger", new BigInteger("789"), null, 789L), // BigDecimal (no fraction)
        Arguments.of("BigDecimal (no fraction)", new BigDecimal("101112"), null, 101112L), // Instant
        Arguments.of("Instant epoch milli", Instant.ofEpochMilli(131415000L), null, 131415000L), // java.util.Date
        Arguments.of("java.util.Date", Date.from(Instant.ofEpochMilli(161718000L)), null, 161718000L), // java.sql.Timestamp
        Arguments.of("Timestamp", Timestamp.valueOf("2023-01-01 12:34:56"), null, Timestamp.valueOf("2023-01-01 12:34:56").getTime()), // LocalDateTime (system default zone)
        Arguments.of("LocalDateTime", LocalDateTime.parse("2023-02-03T04:05:06"), null, LocalDateTime.parse("2023-02-03T04:05:06").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), // Null input → 0
        Arguments.of("null input", null, null, 0L), // Double (truncates)
        Arguments.of("Double value", 123.456d, null, 123L), // Float (truncates)
        Arguments.of("Float value", new Float(987.65f), null, 987L), // BigDecimal with fraction (truncates)
        Arguments.of("BigDecimal with fraction", new BigDecimal("123.999"), null, 123L), // String with whitespace
        Arguments.of("String with whitespace", "   42  ", null, 42L), // String with leading plus
        Arguments.of("String with leading plus", "+77", null, 77L), // ZonedDateTime
        Arguments.of("ZonedDateTime UTC", ZonedDateTime.of(2023, 3, 4, 5, 6, 7, 0, ZoneId.of("UTC")), null, ZonedDateTime.of(2023, 3, 4, 5, 6, 7, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()), // OffsetDateTime
        Arguments.of("OffsetDateTime -5", OffsetDateTime.of(2023, 3, 4, 5, 6, 7, 0, ZoneOffset.ofHours(-5)), null, OffsetDateTime.of(2023, 3, 4, 5, 6, 7, 0, ZoneOffset.ofHours(-5)).toInstant().toEpochMilli()), // Calendar (UTC)
        Arguments.of("Calendar UTC", Calendar.getInstance(TimeZone.getTimeZone("UTC")), null, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()), // java.sql.Date
        Arguments.of("java.sql.Date", java.sql.Date.valueOf("2023-04-05"), null, java.sql.Date.valueOf("2023-04-05").getTime()), // Custom converter that returns 555L
        Arguments.of("Custom converter returning 555L", "any", myConverter, 555L));
    }

    private static Stream<Arguments> exceptionTestCases() {
        return Stream.of(// Invalid numeric string → NumberFormatException
        Arguments.of("Invalid numeric string", "not-a-number", null, NumberFormatException.class), // Object with no conversion rule → IllegalArgumentException
        Arguments.of("Unsupported object type", new Object(), null, IllegalArgumentException.class), // Boolean → IllegalArgumentException
        Arguments.of("Boolean value", Boolean.TRUE, null, IllegalArgumentException.class), // Number out of range (greater than Long.MAX_VALUE) → NumberFormatException
        Arguments.of("String > Long.MAX_VALUE", "9223372036854775808", null, NumberFormatException.class), // Number out of range (less than Long.MIN_VALUE) → NumberFormatException
        Arguments.of("String < Long.MIN_VALUE", "-9223372036854775809", null, NumberFormatException.class), // Converter returns String instead of Number → ClassCastException
        Arguments.of("Converter returns String", "123", converterReturningString, ClassCastException.class), // Converter returns null → NullPointerException
        Arguments.of("Converter returns null", "456", converterReturningNull, NullPointerException.class), // Converter throws RuntimeException → RuntimeException
        Arguments.of("Converter throws", "any", throwingConverter, RuntimeException.class), // Map without expected DATE/TIME keys → IllegalArgumentException
        Arguments.of("Map without date/time keys", Collections.singletonMap("unexpected", "value"), null, IllegalArgumentException.class));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        Long source = 123L;
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumeric_returnsParsedValue() {
        String source = "456";
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromDate_returnsDateTimeInMillis() {
        Date now = new Date();
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(now, converter);
        assertNotNull(result);
        assertEquals(now.getTime(), result.get());
    }

    @Test
    void toAtomicLong_fromZonedDateTime_returnsEpochMilli() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L), ZoneId.of("UTC"));
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(zdt, converter);
        assertNotNull(result);
        // ZonedDateTime conversion to long is expected to be the epoch millisecond value.
        assertEquals(zdt.toInstant().toEpochMilli(), result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_multipleCallsReturnDistinctInstances() {
        Long source = 10L;
        AtomicLong first = ZonedDateTimeConversions.toAtomicLong(source, converter);
        AtomicLong second = ZonedDateTimeConversions.toAtomicLong(source, converter);
        assertNotSame(first, second, "Each call must produce a new AtomicLong instance");
        assertEquals(first.get(), second.get(), "Values must be equal");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("conversionTestCases")
    void toAtomicLong_conversionTests(String description, Object from, Converter suppliedConverter, long expectedValue) {
        AtomicLong result = ZonedDateTimeConversions.toAtomicLong(from, suppliedConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedValue, result.get(), () -> "Unexpected value for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionTestCases")
    void toAtomicLong_exceptionTests(String description, Object from, Converter suppliedConverter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toAtomicLong(from, suppliedConverter), () -> "Expected exception not thrown for case: " + description);
    }
}
