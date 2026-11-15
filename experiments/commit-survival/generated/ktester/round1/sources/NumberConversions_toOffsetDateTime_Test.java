package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toOffsetDateTime_Test {

    /**
     * A mock {@link Converter} that can be passed to the static methods.
     */
    @Mock
    Converter mockConverter;

    /**
     * Holds a single test case for the successful‑conversion parameterized test.
     */
    private static final class ArgumentsHolder {

        final Object input;

        final OffsetDateTime expected;

        final String description;

        ArgumentsHolder(Object input, OffsetDateTime expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Provides a stream of arguments for successful conversions.
     */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        // java.util.Date
        Date date = Date.from(Instant.parse("2023-03-15T10:20:30Z"));
        OffsetDateTime expectedFromDate = ZonedDateTime.ofInstant(date.toInstant(), systemZone).toOffsetDateTime();
        // java.sql.Timestamp
        Timestamp ts = Timestamp.from(Instant.parse("2022-12-01T05:45:00Z"));
        OffsetDateTime expectedFromTimestamp = ZonedDateTime.ofInstant(ts.toInstant(), systemZone).toOffsetDateTime();
        // java.time.Instant
        Instant instant = Instant.parse("2021-07-04T14:00:00Z");
        OffsetDateTime expectedFromInstant = ZonedDateTime.ofInstant(instant, systemZone).toOffsetDateTime();
        // java.time.LocalDateTime (no zone – interpreted in system default)
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
        OffsetDateTime expectedFromLDT = ldt.atZone(systemZone).toOffsetDateTime();
        // java.time.ZonedDateTime (preserve offset)
        ZonedDateTime zdt = ZonedDateTime.of(2019, 5, 20, 8, 30, 0, 0, ZoneId.of("Europe/Moscow"));
        OffsetDateTime expectedFromZDT = zdt.toOffsetDateTime();
        // java.time.OffsetDateTime (should be returned unchanged)
        OffsetDateTime odt = OffsetDateTime.of(2018, 11, 11, 22, 15, 0, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime expectedFromODT = odt;
        // java.util.Calendar (system default zone)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(systemZone));
        cal.set(2024, Calendar.FEBRUARY, 29, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        Date calDate = cal.getTime();
        OffsetDateTime expectedFromCalendar = ZonedDateTime.ofInstant(calDate.toInstant(), systemZone).toOffsetDateTime();
        // ISO‑8601 String (parsed as an instant, then interpreted in system zone)
        String isoString = "2025-12-31T23:59:59Z";
        Instant isoInstant = Instant.parse(isoString);
        OffsetDateTime expectedFromString = ZonedDateTime.ofInstant(isoInstant, systemZone).toOffsetDateTime();
        return Stream.of(new ArgumentsHolder(date, expectedFromDate, "java.util.Date"), new ArgumentsHolder(ts, expectedFromTimestamp, "java.sql.Timestamp"), new ArgumentsHolder(instant, expectedFromInstant, "java.time.Instant"), new ArgumentsHolder(ldt, expectedFromLDT, "java.time.LocalDateTime"), new ArgumentsHolder(zdt, expectedFromZDT, "java.time.ZonedDateTime"), new ArgumentsHolder(odt, expectedFromODT, "java.time.OffsetDateTime"), new ArgumentsHolder(cal, expectedFromCalendar, "java.util.Calendar"), new ArgumentsHolder(isoString, expectedFromString, "ISO‑8601 String"));
    }

    /**
     * Provides a stream of arguments for unsupported‑type failures.
     */
    private static Stream<Object> unsupportedTypeProvider() {
        return Stream.of(// Integer
        12345, // LocalDate
        LocalDate.of(2022, 5, 17), // Year
        Year.of(2021), // LocalTime
        LocalTime.of(14, 30, 15), // OffsetTime
        OffsetTime.of(14, 30, 0, 0, ZoneOffset.UTC), // Boolean
        Boolean.TRUE);
    }

    /**
     * Verify that {@code toOffsetDateTime} delegates to {@code toZonedDateTime}
     * and returns the {@link OffsetDateTime} produced by that delegate.
     *
     * The static {@code toZonedDateTime} method is mocked to return a known
     * {@link ZonedDateTime}. The test then asserts that the result of
     * {@code toOffsetDateTime} is exactly the {@code OffsetDateTime}
     * representation of the mocked {@code ZonedDateTime}.
     */
    @Test
    void toOffsetDateTime_usesToZonedDateTime_andReturnsOffset() {
        // GIVEN
        ZonedDateTime expectedZdt = ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 0, ZoneId.of("UTC+02:00"));
        // Mock the static method inside NumberConversions.
        try (MockedStatic<NumberConversions> mocked = mockStatic(NumberConversions.class, MockedStatic.VerificationMode.CALLS_REAL_METHODS)) {
            mocked.when(() -> NumberConversions.toZonedDateTime(any(), any())).thenReturn(expectedZdt);
            // WHEN
            OffsetDateTime result = NumberConversions.toOffsetDateTime("anyInput", mockConverter);
            // THEN
            assertEquals(expectedZdt.toOffsetDateTime(), result, "The OffsetDateTime should be derived from the mocked ZonedDateTime");
            // Verify that the static method was indeed called once with the supplied arguments.
            mocked.verify(() -> NumberConversions.toZonedDateTime(eq("anyInput"), eq(mockConverter)), times(1));
        }
    }

    /**
     * End‑to‑end conversion test using a real {@link ZonedDateTime} as the source.
     *
     * The {@code Converter} is mocked because the conversion of a {@code ZonedDateTime}
     * to another {@code ZonedDateTime} does not need any conversion logic – the method
     * simply returns the argument (or a copy). This keeps the test lightweight while
     * still exercising the real implementation path.
     */
    @Test
    void toOffsetDateTime_withRealZonedDateTime_returnsCorrectOffset() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, // offset -05:00 (or -04:00 depending on DST)
        ZoneId.of("America/New_York"));
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting OffsetDateTime must not be null");
        assertEquals(source.toOffsetDateTime(), result, "OffsetDateTime should match the source ZonedDateTime's offset representation");
    }

    @ParameterizedTest(name = "toOffsetDateTime converts {0}")
    @MethodSource("validConversionProvider")
    void toOffsetDateTime_convertsSupportedTypes(ArgumentsHolder holder) {
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(holder.input, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null for " + holder.description);
        assertEquals(holder.expected, result, () -> "Converted OffsetDateTime does not match expected for " + holder.description);
    }

    @Test
    void toOffsetDateTime_nullInput_returnsNull() {
        // WHEN
        OffsetDateTime result = NumberConversions.toOffsetDateTime(null, mockConverter);
        // THEN
        assertNull(result, "A null source should result in a null OffsetDateTime");
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNPE() {
        // any non‑null source
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> NumberConversions.toOffsetDateTime(date, null), "Passing a null Converter must throw NullPointerException");
    }

    @ParameterizedTest(name = "toOffsetDateTime throws for unsupported type: {0}")
    @MethodSource("unsupportedTypeProvider")
    void toOffsetDateTime_unsupportedTypeThrows(Object unsupported) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toOffsetDateTime(unsupported, mockConverter), "Unsupported type " + unsupported.getClass().getSimpleName() + " should cause IllegalArgumentException");
    }

    @Test
    void toOffsetDateTime_malformedIsoString_throwsDateTimeParseException() {
        String badIso = "2023-13-40T25:61:61Z";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> NumberConversions.toOffsetDateTime(badIso, mockConverter), "A malformed ISO‑8601 string must cause DateTimeParseException");
    }

    @Test
    void toOffsetDateTime_nonDateString_throwsDateTimeParseException() {
        String notADate = "not-a-date";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> NumberConversions.toOffsetDateTime(notADate, mockConverter), "A non‑date string must cause DateTimeParseException");
    }
}
