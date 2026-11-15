package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDate_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared default Converter instance used by most tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – this is the standard behaviour
        // exercised by NumberConversions for the majority of supported source types.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clean‑up reference for good measure.
        converter = null;
    }

    /**
     * Argument source for {@link #toDate_successfulDefault(Object, long, String)}.
     */
    private static Stream<Arguments> successfulDefaultConversions() {
        // Calendar in UTC with the same epoch millis
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1622548800000L);
        return Stream.of(Arguments.of(new Date(1622548800000L), 1622548800000L, "Date instance should be returned unchanged"), Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00"), 1622548800000L, "Timestamp should be converted using its underlying millis"), Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "Instant should be converted to its epoch millis"), Arguments.of(Long.valueOf(1622548800000L), 1622548800000L, "Long value is interpreted directly as epoch millis"), Arguments.of("1622548800000", 1622548800000L, "Numeric String should be parsed to long"), Arguments.of(utcCal, 1622548800000L, "Calendar (UTC) should provide its timeInMillis"));
    }

    private static Stream<Arguments> additionalSuccessfulConversions() {
        return Stream.of(// java.sql.Date extends java.util.Date – same behaviour as Date
        Arguments.of(new java.sql.Date(1622548800000L), 1622548800000L, "java.sql.Date should be treated like java.util.Date"), // LocalDate with a custom converter (start of day UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), 1622505600000L, "LocalDate with custom converter should map to start‑of‑day UTC"), // ZonedDateTime already in UTC
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), 1622548800000L, "ZonedDateTime UTC should yield correct epoch millis"), // OffsetDateTime with UTC offset
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), 1622548800000L, "OffsetDateTime UTC should yield correct epoch millis"), // Integer interpreted as raw milliseconds
        Arguments.of(Integer.valueOf(1622548800), 1622548800L, "Integer should be taken as milliseconds directly"), // Double – fractional part ignored
        Arguments.of(Double.valueOf(1622548800000.0), 1622548800000L, "Double should be converted via long cast"), // BigDecimal
        Arguments.of(BigDecimal.valueOf(1622548800000L), 1622548800000L, "BigDecimal should be converted via longValueExact"), // BigInteger
        Arguments.of(BigInteger.valueOf(1622548800000L), 1622548800000L, "BigInteger should be converted via longValueExact"), // AtomicLong
        Arguments.of(new AtomicLong(1622548800000L), 1622548800000L, "AtomicLong value should be used as epoch millis"), // AtomicInteger
        Arguments.of(new AtomicInteger(1622548800), 1622548800L, "AtomicInteger value should be used as epoch millis"));
    }

    private static Stream<Arguments> exceptionScenarios() {
        // custom converter that throws a runtime exception
        Converter throwingConverter = new Converter() {

            @Override
            public long convert(Object o) {
                throw new IllegalStateException("custom failure");
            }
        };
        return Stream.of(// non‑numeric string
        Arguments.of("not-a-number", defaultConverter, NumberFormatException.class, "Non‑numeric String should cause NumberFormatException"), // overflow string > Long.MAX_VALUE
        Arguments.of("9223372036854775808", defaultConverter, NumberFormatException.class, "Overflow numeric String should cause NumberFormatException"), // unsupported array type
        Arguments.of(new int[] { 1, 2, 3 }, defaultConverter, IllegalArgumentException.class, "Array source should trigger IllegalArgumentException"), // Double.NaN
        Arguments.of(Double.NaN, defaultConverter, IllegalArgumentException.class, "NaN Double should trigger IllegalArgumentException"), // null converter
        Arguments.of(new Date(0L), null, NullPointerException.class, "Null converter argument should cause NullPointerException"), // custom converter that throws IllegalStateException
        Arguments.of(new Object(), throwingConverter, IllegalStateException.class, "Converter throwing RuntimeException should propagate"));
    }

    @Test
    void toDateFromLong() {
        // arbitrary epoch value
        long epochMillis = 1_600_000_000_000L;
        Date result = NumberConversions.toDate(epochMillis, defaultConverter);
        assertEquals(new Date(epochMillis), result, "Date created from a Long should match the supplied epoch milliseconds");
    }

    @Test
    void toDateFromDate() {
        Date now = new Date();
        Date result = NumberConversions.toDate(now, defaultConverter);
        assertEquals(now, result, "Date created from an existing Date should be equal to the original");
    }

    @Test
    void toDateFromStringNumber() {
        // represents a valid epoch millisecond value
        String millisString = "1622548800000";
        Date expected = new Date(Long.parseLong(millisString));
        Date result = NumberConversions.toDate(millisString, defaultConverter);
        assertEquals(expected, result, "String containing a numeric epoch should be converted to the correct Date");
    }

    @Test
    void toDateFromNull() {
        Date result = NumberConversions.toDate(null, defaultConverter);
        assertEquals(new Date(0L), result, "Null source should be interpreted as 0 milliseconds (epoch start)");
    }

    /**
     * Parameterised test covering all successful conversions that do not require a
     * custom {@link Converter}. Each argument supplies:
     * <ul>
     *   <li>the source object</li>
     *   <li>the expected epoch‑millis value of the resulting {@link Date}</li>
     *   <li>a short description (used only for readability)</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulDefaultConversions")
    void toDate_successfulDefault(Object from, long expectedEpochMillis, String description) {
        Date result = NumberConversions.toDate(from, defaultConverter);
        assertEquals(new Date(expectedEpochMillis), result, () -> "Conversion failed for case: " + description);
    }

    /**
     * Tests that a {@code null} source is interpreted as {@code 0L} (epoch start).
     * This behaviour matches the original test suite.
     */
    @Test
    void toDate_fromNull_returnsEpochStart() {
        Date result = NumberConversions.toDate(null, defaultConverter);
        assertEquals(new Date(0L), result, "Null source should be interpreted as 0 milliseconds (epoch start)");
    }

    /**
     * Tests conversion of a {@link Boolean} – an unsupported type – which must
     * result in an {@link IllegalArgumentException}.
     */
    @Test
    void toDate_fromBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toDate(Boolean.TRUE, defaultConverter), "Boolean source should trigger IllegalArgumentException");
    }

    /**
     * Tests conversion of a {@link LocalDateTime} using a custom {@link Converter}
     * that knows how to turn the value into epoch milliseconds.
     */
    @Test
    void toDate_fromLocalDateTime_withCustomConverter() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 0);
        Converter custom = new Converter() {

            @Override
            public long convert(Object o) {
                return ((LocalDateTime) o).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
            }
        };
        Date result = NumberConversions.toDate(ldt, custom);
        assertEquals(new Date(1622548800000L), result, "Custom converter should correctly convert LocalDateTime to Date");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalSuccessfulConversions")
    void toDate_successfulAdditional(Object from, long expectedEpochMillis, String description) {
        Date result = NumberConversions.toDate(from, defaultConverter);
        assertEquals(new Date(expectedEpochMillis), result, () -> "Conversion failed for case: " + description);
    }

    /**
     * LocalDate conversion requires a custom {@link Converter} because the default
     * {@code Converter} does not understand {@code LocalDate}. This test supplies the
     * converter inline and verifies the result.
     */
    @Test
    void toDate_localDate_withCustomConverter() {
        LocalDate ld = LocalDate.of(2021, 6, 1);
        Converter custom = new Converter() {

            @Override
            public long convert(Object o) {
                return ((LocalDate) o).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            }
        };
        Date result = NumberConversions.toDate(ld, custom);
        assertEquals(new Date(1622505600000L), result, "Custom LocalDate converter should produce start‑of‑day UTC Date");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionScenarios")
    void toDate_exceptional(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toDate(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
