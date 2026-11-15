package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toCalendar_Test {

    /**
     * Shared Converter used by all tests
     */
    private static Converter sharedConverter;

    /**
     * Preserve the original default time‑zone so we can restore it after the suite
     */
    private static TimeZone originalDefaultTZ;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic time‑zone for the conversion logic
        originalDefaultTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Build a real Converter – no mocking needed for the public conversion API
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone
        TimeZone.setDefault(originalDefaultTZ);
    }

    /**
     * Provides a stream of valid conversion scenarios.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the source object (the {@code from} parameter)</li>
     *   <li>a {@link Converter} instance (null means use {@link #sharedConverter})</li>
     *   <li>the expected epoch‑millis value that the resulting {@link Calendar} must contain</li>
     *   <li>a short description used only for readability</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct long
        Arguments.of(1622548800000L, null, 1622548800000L, "Long value"), // Numeric string
        Arguments.of("1622548800000", null, 1622548800000L, "String containing a numeric epoch millis"), // java.util.Date
        Arguments.of(new Date(1622548800000L), null, 1622548800000L, "java.util.Date"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), null, 1622548800000L, "Instant"), // LocalDateTime interpreted as UTC (default zone forced to UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), null, LocalDateTime.of(2021, 6, 1, 12, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), "LocalDateTime (UTC)"), // ZonedDateTime with America/New_York (UTC‑4 at that date)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), null, ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")).toInstant().toEpochMilli(), "ZonedDateTime America/New_York"), // LocalDate (start of day in default zone – UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), null, LocalDate.of(2021, 6, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), "LocalDate"), // LocalTime (combined with today’s date in default zone)
        Arguments.of(LocalTime.of(12, 30, 15), null, LocalDate.now(ZoneOffset.UTC).atTime(12, 30, 15).toInstant(ZoneOffset.UTC).toEpochMilli(), "LocalTime (today)"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00.123"), null, Timestamp.valueOf("2021-06-01 12:00:00.123").getTime(), "Timestamp with millis"), // java.sql.Date (midnight UTC)
        Arguments.of(java.sql.Date.valueOf("2021-06-01"), null, java.sql.Date.valueOf("2021-06-01").getTime(), "java.sql.Date"), // BigDecimal holding a whole number
        Arguments.of(new BigDecimal("1622548800000"), null, 1622548800000L, "BigDecimal whole number"), // BigInteger holding a whole number
        Arguments.of(new BigInteger("1622548800000"), null, 1622548800000L, "BigInteger whole number"), // AtomicLong
        Arguments.of(new AtomicLong(1622548800000L), null, 1622548800000L, "AtomicLong"), // Map with key "epoch" holding a Long (real Converter can extract it)
        Arguments.of(Collections.singletonMap("epoch", 1622548800000L), null, 1622548800000L, "Map with epoch key"), // Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, null, Long.MAX_VALUE, "Long.MAX_VALUE"), // Long.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, null, Long.MIN_VALUE, "Long.MIN_VALUE"), // Zero epoch
        Arguments.of(0L, null, 0L, "Zero epoch (long)"), // Date at epoch start
        Arguments.of(new Date(0L), null, 0L, "Date at epoch start"), // OffsetDateTime with -04:00 offset
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)), null, OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)).toInstant().toEpochMilli(), "OffsetDateTime -04:00"), // ZonedDateTime with UTC+02:00 (custom stub – we use a mock converter)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC+02:00")), createMockConverterReturning(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC+02:00")).toInstant().toEpochMilli()), ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC+02:00")).toInstant().toEpochMilli(), "ZonedDateTime UTC+02:00 with mock converter"));
    }

    /**
     * Helper that creates a Mockito mock {@link Converter} which, when asked to
     * convert anything to {@code Long.class}, simply returns the supplied {@code epochMillis}.
     *
     * <p>This is sufficient for the test cases where we need a custom converter
     * to return a known epoch value for a temporal type that the real {@link Converter}
     * cannot handle without a zone.
     */
    private static Converter createMockConverterReturning(long epochMillis) {
        Converter mock = mock(Converter.class);
        // The real Converter API contains a method like: <T> T convert(Object source, Class<T> target)
        // We stub any conversion to Long.class to return the desired epoch millis.
        when(mock.convert(any(), eq(Long.class))).thenReturn(epochMillis);
        // For completeness, also stub conversion to Calendar (which is what toCalendar ultimately uses)
        when(mock.convert(any(), eq(Calendar.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            // Defer to the real conversion logic via the static method under test
            return InstantConversions.toCalendar(src, mock);
        });
        return mock;
    }

    @Test
    void toCalendar_fromLong_returnsCorrectCalendar() {
        // given
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        // when
        Calendar cal = InstantConversions.toCalendar(epochMillis, sharedConverter);
        // then
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(epochMillis, cal.getTimeInMillis(), "Calendar time should match the supplied epoch milliseconds");
    }

    @Test
    void toCalendar_fromDate_returnsCorrectCalendar() {
        // given
        // same instant as above
        Date date = new Date(1_640_995_200_000L);
        // when
        Calendar cal = InstantConversions.toCalendar(date, sharedConverter);
        // then
        assertNotNull(cal);
        assertEquals(date.getTime(), cal.getTimeInMillis(), "Calendar time should equal the Date's epoch milliseconds");
    }

    @Test
    void toCalendar_fromInstant_returnsCorrectCalendar() {
        // given
        Instant instant = Instant.ofEpochMilli(1_640_995_200_000L);
        // when
        Calendar cal = InstantConversions.toCalendar(instant, sharedConverter);
        // then
        assertNotNull(cal);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis(), "Calendar time should equal the Instant's epoch milliseconds");
    }

    @Test
    void toCalendar_fromLocalDateTime_returnsCorrectCalendar() {
        // given – LocalDateTime interpreted as UTC (the default zone is forced to UTC in @BeforeAll)
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 0, 0);
        long expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        // when
        Calendar cal = InstantConversions.toCalendar(ldt, sharedConverter);
        // then
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar time should match the LocalDateTime interpreted in UTC");
    }

    @Test
    void toCalendar_fromStringNumber_returnsCorrectCalendar() {
        // given – the converter can turn a numeric string into a long
        // same epoch millis as above
        String numericString = "1640995200000";
        long expected = Long.parseLong(numericString);
        // when
        Calendar cal = InstantConversions.toCalendar(numericString, sharedConverter);
        // then
        assertNotNull(cal);
        assertEquals(expected, cal.getTimeInMillis(), "Calendar time should be derived from the numeric string");
    }

    @Test
    void toCalendar_fromNull_returnsEpochStartCalendar() {
        // given
        Object nullObj = null;
        // when
        Calendar cal = InstantConversions.toCalendar(nullObj, sharedConverter);
        // then
        assertNotNull(cal);
        assertEquals(0L, cal.getTimeInMillis(), "Null input should produce a Calendar at epoch start (0 ms)");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversions(Object from, Converter converter, long expectedMillis, String description) {
        // Resolve the converter – null means use the shared real converter
        Converter usedConverter = (converter == null) ? sharedConverter : converter;
        // When
        Calendar cal = InstantConversions.toCalendar(from, usedConverter);
        // Then
        assertNotNull(cal, "Calendar should never be null");
        assertEquals(expectedMillis, cal.getTimeInMillis(), () -> "Unexpected epoch millis for case: " + description);
    }

    @Test
    void toCalendar_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> InstantConversions.toCalendar(null, sharedConverter), "Null source should result in NullPointerException");
    }

    @Test
    void toCalendar_unsupportedBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(Boolean.TRUE, sharedConverter), "Boolean source is unsupported and should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_unsupportedObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(new Object(), sharedConverter), "Arbitrary Object source is unsupported and should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_malformedNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> InstantConversions.toCalendar("not-a-number", sharedConverter), "Non‑numeric string should raise NumberFormatException");
    }

    @Test
    void toCalendar_nonIntegralBigDecimal_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(new BigDecimal("123.456"), sharedConverter), "BigDecimal with fractional part should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_bigIntegerOverflow_throwsArithmeticException() {
        // Value larger than Long.MAX_VALUE
        // Long.MAX_VALUE + 1
        BigInteger overflow = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> InstantConversions.toCalendar(overflow, sharedConverter), "Overflowing BigInteger should raise ArithmeticException");
    }

    @Test
    void toCalendar_localDateTimeWithoutConverter_throwsIllegalArgumentException() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(ldt, null), "LocalDateTime without a Converter should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_zonedDateTimeWithoutConverter_throwsIllegalArgumentException() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("America/New_York"));
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(zdt, null), "ZonedDateTime without a Converter should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_mapWithoutSuitableConverter_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap("epoch", "not-a-long");
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toCalendar(map, sharedConverter), "Map with non‑numeric value and no custom converter should raise IllegalArgumentException");
    }

    @Test
    void toCalendar_converterThrowsRuntimeException_propagatesException() {
        // Mock converter that throws on any conversion attempt
        Converter explodingConverter = mock(Converter.class);
        when(explodingConverter.convert(any(), any())).thenThrow(new RuntimeException("converter failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> InstantConversions.toCalendar("1622548800000", explodingConverter), "Exception from the Converter should be propagated");
        assertEquals("converter failure", ex.getMessage());
    }
}
