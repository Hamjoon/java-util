package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they provide the standard time‑zone and locale handling.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the garbage collector in long‑running test suites.
        converter = null;
    }

    /**
     * Provides a stream of valid inputs together with the expected epoch‑millis.
     * The {@code converter} argument is {@code null} for those cases where the
     * JSON test data specified it, meaning the default static {@link #converter}
     * will be used inside the test method.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 1. UTC OffsetDateTime
        Arguments.of(OffsetDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneOffset.UTC), 1683721845000L), // 2. ZonedDateTime in America/New_York (UTC‑5)
        Arguments.of(ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York")), 1641013199000L), // 3. java.util.Date holding the epoch millis directly
        Arguments.of(new Date(1683721845000L), 1683721845000L), // 4. java.sql.Timestamp representing the same instant as case 1
        Arguments.of(Timestamp.valueOf("2023-05-10 12:30:45.000"), 1683721845000L), // 5. Long containing epoch milliseconds
        Arguments.of(1683721845000L, 1683721845000L), // 6. ISO‑8601 UTC string
        Arguments.of("2023-05-10T12:30:45Z", 1683721845000L));
    }

    /**
     * Supplies inputs that are expected to cause an exception when passed to {@code toLong}.
     * The second argument is the exact {@link Class} of the exception that should be thrown.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null source & null converter → NullPointerException (as per JSON spec)
        Arguments.of(null, null, NullPointerException.class), // Unsupported type Boolean → IllegalArgumentException
        Arguments.of(Boolean.TRUE, converter, IllegalArgumentException.class), // Malformed ISO‑8601 string → DateTimeParseException
        Arguments.of("2023-13-40T99:99:99Z", converter, DateTimeParseException.class), // String without timezone designator → DateTimeParseException
        Arguments.of("2023-05-10 12:30:45", converter, DateTimeParseException.class), // LocalDate (no time) → IllegalArgumentException
        Arguments.of(LocalDate.of(2023, 5, 10), converter, IllegalArgumentException.class), // LocalTime (no date) → IllegalArgumentException
        Arguments.of(LocalTime.of(12, 30, 45), converter, IllegalArgumentException.class), // Arbitrary Object → IllegalArgumentException
        Arguments.of(new Object(), converter, IllegalArgumentException.class), // Very large BigInteger (out of long range) → IllegalArgumentException
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN), converter, IllegalArgumentException.class));
    }

    @Test
    void toLong_fromOffsetDateTime_matchesInstantEpochMilli() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(// 123 ms fraction
        2023, // 123 ms fraction
        5, // 123 ms fraction
        17, // 123 ms fraction
        12, // 123 ms fraction
        34, // 123 ms fraction
        56, // 123 ms fraction
        123_000_000, // +02:30 offset
        ZoneOffset.ofHoursMinutes(2, 30));
        // WHEN
        long result = OffsetDateTimeConversions.toLong(odt, converter);
        // THEN
        long expected = odt.toInstant().toEpochMilli();
        assertEquals(expected, result, "Conversion from OffsetDateTime should produce the same epoch‑millis as its Instant");
    }

    @Test
    void toLong_fromOffsetTime_usesTodayDate() {
        // GIVEN
        // OffsetTime does not contain a date, so the conversion uses the current date
        // in the converter's time‑zone (default system zone). We capture "today" first.
        LocalDate today = LocalDate.now(converter.getOptions().getTimeZone().toZoneId());
        OffsetTime ot = OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(-5));
        // WHEN
        long result = OffsetDateTimeConversions.toLong(ot, converter);
        // THEN
        // Expected instant = today at the given offset‑time.
        Instant expected = ot.atDate(today).toInstant();
        assertEquals(expected.toEpochMilli(), result, "OffsetTime should be interpreted as today’s date in the converter’s time‑zone");
    }

    @Test
    void toLong_fromIsoString_isParsedCorrectly() {
        // GIVEN
        String iso = "2022-12-31T23:59:59.999Z";
        // WHEN
        long result = OffsetDateTimeConversions.toLong(iso, converter);
        // THEN
        Instant expected = Instant.parse(iso);
        assertEquals(expected.toEpochMilli(), result, "ISO‑8601 string should be parsed to the correct epoch milliseconds");
    }

    @Test
    void toLong_fromLong_returnsSameValue() {
        // GIVEN
        // some arbitrary epoch‑millis
        Long source = 1_642_000_000_000L;
        // WHEN
        long result = OffsetDateTimeConversions.toLong(source, converter);
        // THEN
        assertEquals(source.longValue(), result, "A Long source should be returned unchanged");
    }

    @Test
    void toLong_fromNull_returnsZero() {
        // GIVEN
        Object source = null;
        // WHEN
        long result = OffsetDateTimeConversions.toLong(source, converter);
        // THEN
        assertEquals(0L, result, "Converter returns 0 for null when the target type is a primitive long");
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) ➔ {1}")
    @MethodSource("validConversionProvider")
    void toLong_validConversions(Object from, long expected) {
        // WHEN
        long result = OffsetDateTimeConversions.toLong(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Conversion of " + from.getClass().getSimpleName() + " should yield expected epoch millis");
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) throws {2}")
    @MethodSource("exceptionProvider")
    void toLong_invalidInputs_throwException(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> OffsetDateTimeConversions.toLong(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
