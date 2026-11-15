package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toBigInteger_Test {

    /**
     * Converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Helper that computes the expected BigInteger for a given input
     *  according to the rules described in the JSON test specification.
     * ------------------------------------------------------------- */
    private static BigInteger expectedBigInteger(Object from) {
        if (from == null) {
            return null;
        }
        if (from instanceof Instant instant) {
            return BigInteger.valueOf(instant.getEpochSecond());
        }
        if (from instanceof LocalDateTime ldt) {
            // Assume UTC as the reference zone (matches JSON expectations)
            return BigInteger.valueOf(ldt.toInstant(ZoneOffset.UTC).getEpochSecond());
        }
        if (from instanceof LocalDate ld) {
            return BigInteger.valueOf(ld.atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond());
        }
        if (from instanceof LocalTime lt) {
            // Seconds since start of the epoch day (1970‑01‑01)
            return BigInteger.valueOf(lt.toSecondOfDay());
        }
        if (from instanceof ZonedDateTime zdt) {
            return BigInteger.valueOf(zdt.toInstant().getEpochSecond());
        }
        if (from instanceof OffsetDateTime odt) {
            return BigInteger.valueOf(odt.toInstant().getEpochSecond());
        }
        if (from instanceof Date date) {
            return BigInteger.valueOf(date.getTime());
        }
        if (from instanceof Timestamp ts) {
            return BigInteger.valueOf(ts.getTime());
        }
        if (from instanceof Calendar cal) {
            return BigInteger.valueOf(cal.getTimeInMillis());
        }
        if (from instanceof Long l) {
            return BigInteger.valueOf(l);
        }
        if (from instanceof AtomicLong al) {
            return BigInteger.valueOf(al.get());
        }
        if (from instanceof String s) {
            // ISO‑8601 string – parse as an Instant
            Instant i = Instant.parse(s);
            return BigInteger.valueOf(i.getEpochSecond());
        }
        // For any other type we deliberately do not compute an expectation.
        throw new IllegalArgumentException("Unsupported type for expected value calculation: " + from.getClass());
    }

    private static Stream<Arguments> validConversionProvider() {
        // LocalDateTime
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30, 45);
        // java.util.Date
        Date date = Date.from(Instant.ofEpochMilli(1672500000123L));
        // java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2022-01-15 08:20:30.123");
        // Instant (epoch‑second)
        Instant instant = Instant.ofEpochSecond(1609459200L);
        // Long (epoch‑millis)
        Long epochMillisLong = 1625097600000L;
        // ISO‑8601 string
        String isoString = "2024-03-01T14:45:00Z";
        // Additional conversions
        LocalDate ld = LocalDate.of(2020, 2, 29);
        LocalTime lt = LocalTime.of(13, 45, 30);
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(2));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200123L);
        AtomicLong atomicLong = new AtomicLong(123456789L);
        return Stream.of(Arguments.of("LocalDateTime → BigInteger (epoch‑second)", ldt, expectedBigInteger(ldt)), Arguments.of("java.util.Date → BigInteger (epoch‑millis)", date, expectedBigInteger(date)), Arguments.of("java.sql.Timestamp → BigInteger (epoch‑millis)", ts, expectedBigInteger(ts)), Arguments.of("Instant → BigInteger (epoch‑second)", instant, expectedBigInteger(instant)), Arguments.of("Long (epoch‑millis) → BigInteger", epochMillisLong, expectedBigInteger(epochMillisLong)), Arguments.of("ISO‑8601 String → BigInteger (epoch‑second)", isoString, expectedBigInteger(isoString)), Arguments.of("LocalDate → BigInteger (epoch‑second at start‑of‑day UTC)", ld, expectedBigInteger(ld)), Arguments.of("LocalTime → BigInteger (seconds of day)", lt, expectedBigInteger(lt)), Arguments.of("ZonedDateTime (offset -05:00) → BigInteger (epoch‑second)", zdt, expectedBigInteger(zdt)), Arguments.of("OffsetDateTime (offset +02:00) → BigInteger (epoch‑second)", odt, expectedBigInteger(odt)), Arguments.of("Calendar (UTC) → BigInteger (epoch‑millis)", cal, expectedBigInteger(cal)), Arguments.of("AtomicLong → BigInteger (epoch‑millis)", atomicLong, expectedBigInteger(atomicLong)));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(Arguments.of("null source → NullPointerException", null, NullPointerException.class), Arguments.of("Non‑ISO‑8601 string → IllegalArgumentException", "2023/04/05 10:20:30", IllegalArgumentException.class), Arguments.of("MonthDay (no year) → IllegalArgumentException", MonthDay.of(12, 25), IllegalArgumentException.class), Arguments.of("BigDecimal → IllegalArgumentException", new BigDecimal("9876543210"), IllegalArgumentException.class), Arguments.of("Arbitrary Object → IllegalArgumentException", new Object(), IllegalArgumentException.class), Arguments.of("Optional.empty() → IllegalArgumentException", Optional.empty(), IllegalArgumentException.class));
    }

    /**
     * Verify that a {@link LocalDateTime} is correctly converted to a {@link BigInteger}
     * representing the epoch‑millisecond value of the corresponding {@link Instant}.
     */
    @Test
    void testToBigIntegerFromLocalDateTime() {
        // GIVEN
        // 123 ms
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000);
        // The expected Instant in UTC.
        Instant expectedInstant = ldt.toInstant(ZoneOffset.UTC);
        // Expected BigInteger – InstantConversions uses epoch‑millis for BigInteger conversion.
        BigInteger expected = BigInteger.valueOf(expectedInstant.toEpochMilli());
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(ldt, converter);
        // THEN
        assertEquals(expected, actual, () -> "BigInteger conversion of LocalDateTime should equal epoch milliseconds of the corresponding Instant");
    }

    /**
     * Verify that a {@code null} source yields {@code null} as the result.
     */
    @Test
    void testToBigIntegerFromNull() {
        // GIVEN
        Object source = null;
        // WHEN
        BigInteger result = LocalDateTimeConversions.toBigInteger(source, converter);
        // THEN
        assertNull(result, "Conversion of null should return null");
    }

    /* -------------------------------------------------------------
     *  Parameterized test for all successful conversion scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void testToBigIntegerValid(String description, Object from, BigInteger expected) {
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, actual, () -> "Failed conversion: " + description);
    }

    /* -------------------------------------------------------------
     *  Parameterized test for exception‑throwing scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionConversionProvider")
    void testToBigIntegerExceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toBigInteger(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
