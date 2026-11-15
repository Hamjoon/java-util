package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toAtomicLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    // --------------------------------------------------------------------- //
    static final class CustomNumber {

        private final long value;

        CustomNumber(long value) {
            this.value = value;
        }

        long getValue() {
            return value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // built‑in conversions we need in the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the garbage collector in long‑running test suites.
        converter = null;
    }

    static Stream<Arguments> numericConversions() {
        return Stream.of(arguments("Direct conversion from a Long instance should produce an AtomicLong with the same value.", 123L, 123L), arguments("An Integer value should be widened to long and wrapped in an AtomicLong.", 42, 42L), arguments("A numeric String should be parsed and converted to an AtomicLong.", "987", 987L), arguments("A String containing whitespace and a negative number should be trimmed, parsed and converted.", "   -15  ", -15L), arguments("A Double value should be cast to long (truncating the fractional part) before wrapping.", 3.14d, 3L), arguments("BigInteger should be converted to its long value and wrapped.", new BigInteger("1234567890123456789"), 1234567890123456789L), arguments("BigDecimal should be truncated to its long representation before wrapping.", new BigDecimal("12345.678"), 12345L));
    }

    static Stream<Arguments> dateTimeConversions() {
        return Stream.of(arguments("java.util.Date should be converted to its epoch‑millis representation.", new Date(1609459200000L), 1609459200000L), arguments("Instant should be converted to epoch milliseconds (seconds * 1000).", Instant.ofEpochSecond(1620000000L), 1620000000000L), arguments("OffsetDateTime should be converted to epoch milliseconds based on its instant.", OffsetDateTime.of(2022, 5, 10, 12, 30, 0, 0, ZoneOffset.UTC), 1652181000000L), arguments("LocalDateTime (assumed system default zone) should be converted to epoch milliseconds.", LocalDateTime.of(2022, 5, 10, 12, 30, 0), // compute using the shared converter to avoid zone‑dependent hard‑coding
        converter.convert(LocalDateTime.of(2022, 5, 10, 12, 30, 0), Long.class)));
    }

    static Stream<Arguments> boundaryNumericConversions() {
        return Stream.of(arguments("Maximum long value should be preserved when wrapped in an AtomicLong.", Long.MAX_VALUE, Long.MAX_VALUE), arguments("Minimum long value should be preserved when wrapped in an AtomicLong.", Long.MIN_VALUE, Long.MIN_VALUE));
    }

    static Stream<Arguments> stringParsingEdgeCases() {
        return Stream.of(arguments("String with an explicit plus sign should be parsed as a positive long.", "+12345", 12345L), arguments("String containing only whitespace and zero should be trimmed and parsed correctly.", "  0  ", 0L), arguments("Scientific notation string should be parsed to a double first, then truncated to long.", "1.23e3", 1230L));
    }

    @Test
    void toAtomicLong_nullInput_returnsZero() {
        // GIVEN
        Object from = null;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result, "Resulting AtomicLong must never be null");
        assertEquals(0L, result.get(), "Null source should be converted to 0");
    }

    @Test
    void toAtomicLong_longInput_isPreserved() {
        // GIVEN
        Long from = 123L;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertEquals(123L, result.get(), "Long value should be preserved");
    }

    @Test
    void toAtomicLong_stringInput_isParsed() {
        // GIVEN
        String from = "456";
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertEquals(456L, result.get(), "String representing a number should be parsed");
    }

    @Test
    void toAtomicLong_offsetDateTimeInput_matchesConverterResult() {
        // GIVEN
        OffsetDateTime from = OffsetDateTime.now();
        // Expected value calculated via the public Converter API.
        Long expected = converter.convert(from, Long.class);
        long expectedValue = expected != null ? expected : 0L;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertEquals(expectedValue, result.get(), "Conversion of OffsetDateTime must be consistent with Converter.convert(..., Long.class)");
    }

    @Test
    void toAtomicLong_doubleInput_isTruncated() {
        // GIVEN
        // Converter should truncate/round to long (implementation uses long cast)
        Double from = 78.9;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        // Use the Converter to obtain the canonical long value.
        Long expected = converter.convert(from, Long.class);
        long expectedValue = expected != null ? expected : 0L;
        assertEquals(expectedValue, result.get(), "Double should be converted to long following the Converter's rules");
    }

    @Test
    void toAtomicLong_mapWithValueKey_isConverted() {
        // GIVEN: a simple map that the Converter recognises as a numeric holder.
        // The generic conversion logic looks for a "value" key for many types.
        java.util.Map<String, Object> from = java.util.Map.of("value", "99");
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        Long expected = converter.convert(from, Long.class);
        long expectedValue = expected != null ? expected : 0L;
        assertEquals(expectedValue, result.get(), "Map containing a numeric 'value' entry should be converted correctly");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericConversions")
    void numericConversions(String description, Object input, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(input, converter);
        assertEquals(expected, result.get(), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeConversions")
    void dateTimeConversions(String description, Object input, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(input, converter);
        assertEquals(expected, result.get(), description);
    }

    // --------------------------------------------------------------------- //
    @Test
    void nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toAtomicLong(null, converter), "Passing a null source object should result in a NullPointerException.");
    }

    @Test
    void unsupportedType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(new Object(), converter), "An unsupported type should cause an IllegalArgumentException.");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryNumericConversions")
    void boundaryNumericConversions(String description, Object input, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(input, converter);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void bigIntegerOverflow_throwsArithmeticException() {
        assertAll(() -> assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toAtomicLong(new BigInteger("9223372036854775808"), converter), "BigInteger larger than Long.MAX_VALUE should cause an overflow exception."), () -> assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toAtomicLong(new BigInteger("-9223372036854775809"), converter), "BigInteger smaller than Long.MIN_VALUE should cause an overflow exception."));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingEdgeCases")
    void stringParsingEdgeCases(String description, String input, long expected) {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(input, converter);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void emptyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toAtomicLong("", converter), "Empty string cannot be parsed to a number and should raise a NumberFormatException.");
    }

    @Test
    void nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toAtomicLong("abc123", converter), "Non‑numeric string should cause a NumberFormatException.");
    }

    // --------------------------------------------------------------------- //
    @Test
    void calendarUtc_convertedToEpochMillis() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200000L);
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(cal, converter);
        assertEquals(1609459200000L, result.get(), "Calendar instance (UTC) should be converted to its epoch‑millis value.");
    }

    @Test
    void zonedDateTimeConversion() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 10, 12, 30, 0, 0, ZoneId.of("America/New_York"));
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(zdt, converter);
        assertEquals(1652195400000L, result.get(), "ZonedDateTime should be converted using its instant (accounting for zone offset).");
    }

    // --------------------------------------------------------------------- //
    @Test
    void localTimeConversion() {
        LocalTime lt = LocalTime.of(12, 30, 15);
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(lt, converter);
        assertEquals(45_015_000L, result.get(), "LocalTime should be interpreted as time‑of‑day on the epoch date (1970‑01‑01) and converted to milliseconds.");
    }

    @Test
    void offsetTimeConversion() {
        OffsetTime ot = OffsetTime.of(12, 30, 15, 0, ZoneOffset.ofHours(2));
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(ot, converter);
        // The library normalises to UTC before extracting the time‑of‑day.
        assertEquals(45_015_000L, result.get(), "OffsetTime is normalized to UTC before computing epoch milliseconds of the time‑of‑day.");
    }

    @Test
    void localDateConversion() {
        LocalDate ld = LocalDate.of(2022, 5, 10);
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(ld, converter);
        long expected = converter.convert(ld, Long.class);
        assertEquals(expected, result.get(), "LocalDate should be treated as start of day in the system default zone and converted to epoch milliseconds.");
    }

    @Test
    void monthDayInvalid_throwsIllegalArgumentException() {
        // Feb 29 without a year – conversion logic rejects it.
        MonthDay md = MonthDay.of(2, 29);
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(md, converter), "MonthDay representing an invalid date should be rejected.");
    }

    // --------------------------------------------------------------------- //
    @Test
    void sqlDateConversion() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2022-05-10");
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(sqlDate, converter);
        long expected = converter.convert(sqlDate, Long.class);
        assertEquals(expected, result.get(), "java.sql.Date should be converted to epoch milliseconds at start of the day.");
    }

    @Test
    void timestampConversion() {
        Timestamp ts = Timestamp.valueOf("2022-05-10 12:30:15.123");
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(ts, converter);
        long expected = converter.convert(ts, Long.class);
        assertEquals(expected, result.get(), "java.sql.Timestamp should retain its nanosecond precision when converted to epoch milliseconds.");
    }

    @Test
    void customConverterUsage() {
        // Arrange a mock Converter that knows how to extract a long from CustomNumber.
        Converter customConverter = Mockito.mock(Converter.class);
        CustomNumber source = new CustomNumber(777L);
        Mockito.when(customConverter.convert(source, Long.class)).thenReturn(777L);
        // Act
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(source, customConverter);
        // Assert
        assertEquals(777L, result.get(), "When a non‑numeric source object is supplied, a provided Converter that knows how to extract a long should be used.");
    }

    // --------------------------------------------------------------------- //
    @Test
    void doubleNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(Double.NaN, converter), "NaN cannot be represented as a long and should trigger an IllegalArgumentException.");
    }

    @Test
    void doublePositiveInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toAtomicLong(Double.POSITIVE_INFINITY, converter), "Infinite double values are not convertible to a finite long and should raise an IllegalArgumentException.");
    }

    // --------------------------------------------------------------------- //
    @Test
    void stringOverflow_throwsNumberFormatException() {
        assertAll(() -> assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toAtomicLong("9223372036854775808", converter), "String representing a number larger than Long.MAX_VALUE should cause NumberFormatException."), () -> assertThrows(NumberFormatException.class, () -> OffsetDateTimeConversions.toAtomicLong("-9223372036854775809", converter), "String representing a number smaller than Long.MIN_VALUE should cause NumberFormatException."));
    }

    @Test
    void bigDecimalOverflow_throwsArithmeticException() {
        assertAll(() -> assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toAtomicLong(new BigDecimal("9223372036854775808"), converter), "BigDecimal value exceeding Long.MAX_VALUE should trigger an overflow ArithmeticException."), () -> assertThrows(ArithmeticException.class, () -> OffsetDateTimeConversions.toAtomicLong(new BigDecimal("-9223372036854775809"), converter), "BigDecimal value below Long.MIN_VALUE should trigger an overflow ArithmeticException."));
    }
}
