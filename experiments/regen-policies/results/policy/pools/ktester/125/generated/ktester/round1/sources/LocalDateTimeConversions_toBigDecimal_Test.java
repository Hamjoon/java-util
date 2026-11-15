package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toBigDecimal_Test {

    /**
     * Shared {@link Converter} used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a default ConverterOptions instance (no custom overrides needed for these tests)
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up; method kept for symmetry with the template
        converter = null;
    }

    // ---------- Instant based inputs ----------
    static Stream<Arguments> instantInputs() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(12_345L, 0), BigDecimal.valueOf(12_345)), Arguments.of(Instant.ofEpochSecond(10L, 500_000_000L), new BigDecimal("10.5")));
    }

    // ---------- Date and Calendar ----------
    static Stream<Arguments> dateAndCalendarInputs() {
        // Date representing 2021‑01‑01T00:00:00Z
        Date date = new Date(1_609_459_200_000L);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_609_459_200_000L);
        return Stream.of(Arguments.of(date, BigDecimal.valueOf(1_609_459_200L)), Arguments.of(cal, BigDecimal.valueOf(1_609_459_200L)));
    }

    // ---------- Timestamp and SQL Date ----------
    static Stream<Arguments> sqlInputs() {
        Timestamp ts = new Timestamp(1_609_459_200_000L);
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-01-01");
        return Stream.of(Arguments.of(ts, BigDecimal.valueOf(1_609_459_200L)), Arguments.of(sqlDate, BigDecimal.valueOf(1_609_459_200L)));
    }

    /**
     * Verify that when an {@link Instant} is supplied, the method simply forwards the
     * conversion to {@link InstantConversions#toBigDecimal(Instant, Converter)}.
     */
    @Test
    void toBigDecimal_WithInstant_DelegatesToInstantConversions() {
        Instant instant = Instant.parse("2023-01-01T12:34:56.789Z");
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "BigDecimal result should match the Instant conversion");
    }

    /**
     * Verify that a {@link LocalDateTime} is first converted to an {@link Instant}
     * and then to {@link BigDecimal} in the same way as a direct {@link Instant} input.
     */
    @Test
    void toBigDecimal_WithLocalDateTime_ConvertsViaInstant() {
        // 789 ms
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000);
        // Convert the LocalDateTime to an Instant using the same internal logic
        Instant expectedInstant = LocalDateTimeConversions.toInstant(ldt, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ldt, converter);
        assertEquals(expected, actual, "BigDecimal from LocalDateTime should equal conversion via its Instant");
    }

    /**
     * Verify that a {@code null} source value results in {@code null} output,
     * matching the behaviour of {@link InstantConversions#toBigDecimal(Instant, Converter)}.
     */
    @Test
    void toBigDecimal_WithNull_ReturnsNull() {
        assertNull(LocalDateTimeConversions.toBigDecimal(null, converter), "Converting null should return null");
    }

    @ParameterizedTest(name = "[{index}] Instant {0} → {1}")
    @MethodSource("instantInputs")
    void toBigDecimal_InstantInputs(Instant from, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {0.getClass().getSimpleName__} → {1}")
    @MethodSource("dateAndCalendarInputs")
    void toBigDecimal_DateAndCalendar(Object from, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {0.getClass().getSimpleName__} → {1}")
    @MethodSource("sqlInputs")
    void toBigDecimal_SqlTypes(Object from, BigDecimal expected) {
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(from, converter);
        assertEquals(expected, actual);
    }

    // ---------- LocalDateTime ----------
    @Test
    void toBigDecimal_LocalDateTimeInput() {
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 12, 30, 15);
        BigDecimal expected = BigDecimal.valueOf(1_609_504_215L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ldt, converter);
        assertEquals(expected, actual);
    }

    // ---------- LocalDate ----------
    @Test
    void toBigDecimal_LocalDateInput() {
        LocalDate ld = LocalDate.of(2021, 1, 2);
        BigDecimal expected = BigDecimal.valueOf(1_609_545_600L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ld, converter);
        assertEquals(expected, actual);
    }

    // ---------- ZonedDateTime ----------
    @Test
    void toBigDecimal_ZonedDateTimeInput() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 29, 23, 59, 59, 0, ZoneOffset.UTC);
        BigDecimal expected = BigDecimal.valueOf(1_582_934_399L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(zdt, converter);
        assertEquals(expected, actual);
    }

    // ---------- OffsetDateTime ----------
    @Test
    void toBigDecimal_OffsetDateTimeInput() {
        OffsetDateTime odt = OffsetDateTime.of(1999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        BigDecimal expected = BigDecimal.valueOf(946_702_799L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(odt, converter);
        assertEquals(expected, actual);
    }

    // ---------- AtomicLong ----------
    @Test
    void toBigDecimal_AtomicLongInput() {
        AtomicLong al = new AtomicLong(1_609_459_200_000L);
        BigDecimal expected = BigDecimal.valueOf(1_609_459_200L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(al, converter);
        assertEquals(expected, actual);
    }

    // ---------- BigInteger ----------
    @Test
    void toBigDecimal_BigIntegerInput() {
        BigInteger bi = new BigInteger("1609459200000");
        BigDecimal expected = BigDecimal.valueOf(1_609_459_200L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(bi, converter);
        assertEquals(expected, actual);
    }

    // ---------- Null and unsupported types ----------
    @Test
    void toBigDecimal_NullInput_ThrowsNPE() {
        // According to the JSON spec this should throw NPE (the internal toInstant cannot handle null)
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toBigDecimal(null, converter));
    }

    @Test
    void toBigDecimal_UnsupportedString_ThrowsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal("2021-01-01T00:00:00", converter));
    }

    @Test
    void toBigDecimal_UnsupportedBigDecimal_ThrowsConversionException() {
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal(BigDecimal.valueOf(123), converter));
    }

    // ---------- Custom Converter that parses ISO‑8601 string ----------
    @Test
    void toBigDecimal_CustomConverter_ParsesStringToInstant() {
        Converter custom = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // Simple parser for ISO‑8601 strings to Instant
                return target.cast(Instant.parse((String) src));
            }
        };
        String iso = "2021-01-01T12:34:56Z";
        BigDecimal expected = BigDecimal.valueOf(1_609_500_896L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(iso, custom);
        assertEquals(expected, actual);
    }

    // ---------- Custom Converter that modifies parsed Instant ----------
    @Test
    void toBigDecimal_CustomConverter_ModifiesInstant() {
        Converter custom = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                Instant base = Instant.parse((String) src);
                // Add one hour
                return target.cast(base.plusSeconds(3600));
            }
        };
        String iso = "2021-01-01T00:00:00Z";
        BigDecimal expected = BigDecimal.valueOf(1_609_462_800L);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(iso, custom);
        assertEquals(expected, actual);
    }

    // ---------- Converter that throws ConversionException ----------
    @Test
    void toBigDecimal_ConverterThrowsConversionException_Propagates() {
        Converter bad = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new ConversionException("forced failure");
            }
        };
        assertThrows(ConversionException.class, () -> LocalDateTimeConversions.toBigDecimal("any", bad));
    }

    // ---------- Overflow / DateTimeException scenarios ----------
    @Test
    void toBigDecimal_DateWithLongMaxValue_ThrowsDateTimeException() {
        Date overflowDate = new Date(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toBigDecimal(overflowDate, converter));
    }

    @Test
    void toBigDecimal_CalendarWithLongMaxValue_ThrowsDateTimeException() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toBigDecimal(cal, converter));
    }

    @Test
    void toBigDecimal_AtomicLongWithLongMaxValue_ThrowsDateTimeException() {
        AtomicLong al = new AtomicLong(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toBigDecimal(al, converter));
    }
}
