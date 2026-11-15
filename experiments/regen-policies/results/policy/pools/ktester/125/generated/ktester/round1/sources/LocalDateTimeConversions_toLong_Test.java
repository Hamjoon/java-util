package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLong_Test {

    /**
     * The Converter used by all test cases.
     */
    private static Converter converter;

    /**
     * Simple holder class used in a custom‑converter test case.
     */
    private static final class MyInstantHolder {

        final long epochMilli;

        MyInstantHolder(long epochMilli) {
            this.epochMilli = epochMilli;
        }
    }

    /**
     * --------------------------------------------------------------------- *
     *  Global test‑class life‑cycle                                            *
     *  ---------------------------------------------------------------------
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are fine for our tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * --------------------------------------------------------------------- *
     *  Per‑test life‑cycle                                                    *
     *  ---------------------------------------------------------------------
     */
    @BeforeEach
    void setupBeforeEach() {
        // Ensure the JVM runs in UTC for deterministic results unless a test changes it.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any zone changes made by individual tests.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Supplies all test cases that expect a successful conversion to a long value.
     */
    private static Stream<TestCase> successCases() {
        // -----------------------------------------------------------------
        // Built‑in types
        // -----------------------------------------------------------------
        return Stream.of(new TestCase(Instant.ofEpochMilli(1672576496789L), null, 1672576496789L, "Instant input should be returned unchanged as epoch‑millis."), new TestCase(new Date(1672576496789L), null, 1672576496789L, "java.util.Date should be converted to epoch‑millis."), new TestCase(createUtcCalendar(1672576496789L), null, 1672576496789L, "Calendar (UTC) should be interpreted as the same instant."), new TestCase(Long.valueOf(1672576496789L), null, 1672576496789L, "A numeric Long representing epoch‑millis is returned directly."), new TestCase("2023-01-01T12:34:56.789Z", null, 1672576496789L, "ISO‑8601 UTC string is parsed to an Instant."), new TestCase(LocalDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000), null, 1672576496789L, "LocalDateTime interpreted in system default zone (UTC) yields epoch‑millis."), new TestCase(LocalDate.of(2023, 1, 1), null, 1672531200000L, "LocalDate is treated as start‑of‑day UTC."), new TestCase(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.UTC), null, 1672576496789L, "ZonedDateTime with UTC offset yields the reference instant."), new TestCase(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.UTC), null, 1672576496789L, "OffsetDateTime with UTC offset is converted correctly."), new TestCase(new Timestamp(1672576496789L), null, 1672576496789L, "java.sql.Timestamp should be converted to the same epoch‑millis."), new TestCase(new java.sql.Date(1672576496789L), null, 1672576496789L, "java.sql.Date (date‑only) is interpreted as midnight UTC."), new TestCase(OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.ofHours(2)), null, 1672569296789L, "OffsetDateTime with +02:00 offset is converted to UTC."), new TestCase(ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneId.of("America/New_York")), null, 1672594496789L, "ZonedDateTime in America/New_York (UTC‑05) is shifted to UTC."), new TestCase("2023-01-01T12:34:56.789", null, 1672576496789L, "ISO‑8601 string without explicit zone is interpreted in UTC."), new TestCase(Integer.valueOf(1672576496789), null, 1672576496789L, "Integer representing epoch‑millis is accepted."), new TestCase(Double.valueOf(1672576496789.0), null, 1672576496789L, "Double value is truncated to long."), new TestCase(new BigDecimal("1672576496789"), null, 1672576496789L, "BigDecimal containing a whole number is converted."), new TestCase(new BigInteger("1672576496789"), null, 1672576496789L, "BigInteger is converted to long without loss."), new TestCase(new AtomicLong(1672576496789L), null, 1672576496789L, "AtomicLong holding epoch‑millis is extracted."));
    }

    /**
     * Supplies test cases that are expected to throw an exception.
     */
    private static Stream<TestCase> exceptionCases() {
        return Stream.of(new TestCase(Boolean.TRUE, null, IllegalArgumentException.class, "Unsupported type (Boolean) should cause IllegalArgumentException."), new TestCase(new MyInstantHolder(1672576496789L), createCustomConverterForMyInstantHolder(), 1672576496789L, "Custom converter handles MyInstantHolder."), new TestCase(new java.sql.Time(1672576496789L), null, IllegalArgumentException.class, "java.sql.Time represents only a time‑of‑day; conversion unsupported."), new TestCase(LocalTime.of(12, 34, 56, 789_000_000), null, IllegalArgumentException.class, "LocalTime lacks a date component; conversion unsupported."), new TestCase("01/01/2023 12:34:56.789", null, IllegalArgumentException.class, "Non‑ISO string format should cause IllegalArgumentException."), new TestCase(null, null, IllegalArgumentException.class, "Null source object must result in IllegalArgumentException."), new TestCase("", null, IllegalArgumentException.class, "Empty string cannot be parsed to an instant."), new TestCase(Collections.singletonMap("epochMilli", 1672576496789L), null, IllegalArgumentException.class, "Map without a recognised conversion path should trigger IllegalArgumentException."), new TestCase("2023-13-01T00:00:00Z", null, IllegalArgumentException.class, "Invalid month in ISO‑8601 string should cause parsing failure."), new TestCase("2023-01-01T12:34:56.789+25:00", null, IllegalArgumentException.class, "Out‑of‑range zone offset should be rejected."), new TestCase(Float.valueOf(1672576496789f), null, IllegalArgumentException.class, "Float values are not part of the supported numeric conversions."), new TestCase(Short.valueOf((short) 12345), null, IllegalArgumentException.class, "Short values are unsupported for instant conversion."), new TestCase(Byte.valueOf((byte) 10), null, IllegalArgumentException.class, "Byte values are unsupported for instant conversion."), new TestCase("trigger", createConverterThatThrows(), RuntimeException.class, "Custom converter throwing RuntimeException should propagate."), new TestCase(Integer.valueOf(1672576496789), createCustomConverterForInteger(), 1672576496789L, "Custom converter translates Integer to Instant."));
    }

    /**
     * Helper to create a UTC Calendar pre‑populated with the given epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /**
     * Custom Converter that knows how to turn {@link MyInstantHolder} into an {@link Instant}.
     */
    private static Converter createCustomConverterForMyInstantHolder() {
        return new Converter(new ConverterOptions()) {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object from, Class<T> toClass) {
                if (from instanceof MyInstantHolder && toClass == Instant.class) {
                    return (T) Instant.ofEpochMilli(((MyInstantHolder) from).epochMilli);
                }
                return super.convert(from, toClass);
            }
        };
    }

    /**
     * Custom Converter that knows how to turn an {@link Integer} into an {@link Instant}.
     */
    private static Converter createCustomConverterForInteger() {
        return new Converter(new ConverterOptions()) {

            @Override
            @SuppressWarnings("unchecked")
            public <T> T convert(Object from, Class<T> toClass) {
                if (from instanceof Integer && toClass == Instant.class) {
                    return (T) Instant.ofEpochMilli(((Integer) from).longValue());
                }
                return super.convert(from, toClass);
            }
        };
    }

    /**
     * Custom Converter that throws a RuntimeException for any conversion attempt.
     */
    private static Converter createConverterThatThrows() {
        return new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                throw new RuntimeException("forced failure");
            }
        };
    }

    @Test
    void toLong_givenDate_returnsDateEpochMilli() {
        // 2021‑01‑01T00:00:00Z in epoch milliseconds
        long expected = 1_609_459_200_000L;
        Date date = new Date(expected);
        long actual = LocalDateTimeConversions.toLong(date, converter);
        assertEquals(expected, actual, "Conversion of java.util.Date should yield its epoch‑millisecond value");
    }

    @Test
    void toLong_givenLongString_returnsParsedLong() {
        // same instant as above, expressed as a String
        String source = "1609459200000";
        long expected = 1_609_459_200_000L;
        long actual = LocalDateTimeConversions.toLong(source, converter);
        assertEquals(expected, actual, "String containing a numeric epoch‑millisecond value should be parsed correctly");
    }

    @Test
    void toLong_givenInstant_returnsInstantEpochMilli() {
        Instant instant = Instant.ofEpochMilli(1_609_459_200_000L);
        long expected = instant.toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(instant, converter);
        assertEquals(expected, actual, "Conversion of java.time.Instant should return its epoch‑millisecond value");
    }

    @Test
    void toLong_givenCalendar_returnsCalendarTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_609_459_200_000L);
        long expected = cal.getTimeInMillis();
        long actual = LocalDateTimeConversions.toLong(cal, converter);
        assertEquals(expected, actual, "Conversion of java.util.Calendar should return its time in epoch milliseconds");
    }

    @Test
    void toLong_givenLocalDateTime_usesSystemDefaultZone() {
        // The conversion implementation uses LocalDateTimeConversions.toInstant(...),
        // which in turn treats a LocalDateTime as being in the system default zone.
        // To make the test deterministic we set the default zone to UTC for the duration
        // of this test.
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            // Force the JVM to re‑read the property
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
            long expected = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
            long actual = LocalDateTimeConversions.toLong(ldt, converter);
            assertEquals(expected, actual, "LocalDateTime should be interpreted as UTC (system default) and converted to epoch millis");
        } finally {
            // Restore original zone
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    @Test
    void toLong_givenZonedDateTime_returnsCorrectEpochMilli() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, // UTC‑05:00
        ZoneId.of("America/New_York"));
        long expected = zdt.toInstant().toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(zdt, converter);
        assertEquals(expected, actual, "ZonedDateTime should be converted to its instant and then to epoch milliseconds");
    }

    @Test
    void toLong_givenOffsetDateTime_returnsCorrectEpochMilli() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, // UTC‑05:00
        ZoneOffset.ofHours(-5));
        long expected = odt.toInstant().toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(odt, converter);
        assertEquals(expected, actual, "OffsetDateTime should be converted to its instant and then to epoch milliseconds");
    }

    @Test
    void toLong_givenNull_returnsZero() {
        // According to Converter’s contract, a null source yields the default value for the
        // target primitive type – for long this is 0.
        long actual = LocalDateTimeConversions.toLong(null, converter);
        assertEquals(0L, actual, "Null source should be converted to 0L");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameterised success tests                                            *
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void toLong_successfulConversions(TestCase tc) {
        // Use the custom converter if supplied, otherwise the default one.
        Converter used = tc.customConverter() != null ? tc.customConverter() : converter;
        long actual = LocalDateTimeConversions.toLong(tc.from(), used);
        assertEquals(tc.expected(), actual, tc.description());
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameterised exception tests                                         *
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void toLong_exceptionalConversions(TestCase tc) {
        Converter used = tc.customConverter() != null ? tc.customConverter() : converter;
        if (tc.expected() instanceof Class<?> expectedEx) {
            // Expected an exception.
            assertThrows(expectedEx, () -> LocalDateTimeConversions.toLong(tc.from(), used), tc.description());
        } else {
            // Expected a successful conversion (used for the custom MyInstantHolder case).
            long actual = LocalDateTimeConversions.toLong(tc.from(), used);
            assertEquals(tc.expected(), actual, tc.description());
        }
    }
}
