package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalDateTime_Test {

    /**
     * Mocked Converter used for all test cases.
     */
    @Mock
    Converter mockConverter;

    /**
     * Holds a sample ZonedDateTime that will be returned by the mock.
     */
    private static ZonedDateTime sampleZonedDateTime;

    /**
     * Expected LocalDateTime extracted from {@link #sampleZonedDateTime}.
     */
    private static LocalDateTime expectedLocalDateTime;

    @BeforeAll
    static void setupBeforeAll() {
        // Choose a deterministic ZonedDateTime so that the expected LocalDateTime is known.
        sampleZonedDateTime = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        17, // hour, minute, second, nano
        14, // hour, minute, second, nano
        35, // hour, minute, second, nano
        50, // hour, minute, second, nano
        123_000_000, // zone
        ZoneId.of("Europe/Paris"));
        expectedLocalDateTime = sampleZonedDateTime.toLocalDateTime();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure mock interactions do not leak between tests.
        clearInvocations(mockConverter);
    }

    /**
     * Supplies test data for the parameterised end‑to‑end conversion tests.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>the source object to convert</li>
     *   <li>the expected {@link LocalDateTime} result (or {@code null})</li>
     *   <li>a description for readability in test reports</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> conversionProvider() {
        // Helper to create a UTC Calendar set to the same instant as the others.
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.clear();
        utcCal.set(2023, Calendar.MAY, 15, 13, 45, 30);
        utcCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(// ISO_LOCAL_DATE_TIME string – should be parsed in the system default zone.
        org.junit.jupiter.params.provider.Arguments.of("2023-05-15T13:45:30", LocalDateTime.of(2023, 5, 15, 13, 45, 30), "Valid ISO_LOCAL_DATE_TIME string"), // java.sql.Timestamp
        org.junit.jupiter.params.provider.Arguments.of(Timestamp.valueOf("2023-05-15 13:45:30"), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "Timestamp conversion"), // java.util.Date from Instant
        org.junit.jupiter.params.provider.Arguments.of(Date.from(Instant.parse("2023-05-15T13:45:30Z")), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "Date (Instant) conversion"), // ZonedDateTime input
        org.junit.jupiter.params.provider.Arguments.of(ZonedDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneId.of("Europe/Paris")), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "ZonedDateTime input"), // Invalid date string – conversion should yield null
        org.junit.jupiter.params.provider.Arguments.of("invalid-date", null, "Unparseable string"), // Non‑date type (Integer) – should result in null
        org.junit.jupiter.params.provider.Arguments.of(12345, null, "Non‑date numeric input"), // ISO_OFFSET_DATE_TIME string with offset
        org.junit.jupiter.params.provider.Arguments.of("2023-05-15T13:45:30+02:00", LocalDateTime.of(2023, 5, 15, 13, 45, 30), "Offset date‑time string"), // OffsetDateTime instance
        org.junit.jupiter.params.provider.Arguments.of(OffsetDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneOffset.ofHours(2)), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "OffsetDateTime input"), // LocalDateTime input – should be returned unchanged
        org.junit.jupiter.params.provider.Arguments.of(LocalDateTime.of(2023, 5, 15, 13, 45, 30), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "LocalDateTime input"), // Instant input – uses default zone (UTC for tests)
        org.junit.jupiter.params.provider.Arguments.of(Instant.parse("2023-05-15T13:45:30Z"), LocalDateTime.of(2023, 5, 15, 13, 45, 30), "Instant input"), // LocalDate input – start of day
        org.junit.jupiter.params.provider.Arguments.of(LocalDate.of(2023, 5, 15), LocalDateTime.of(2023, 5, 15, 0, 0, 0), "LocalDate input"), // Calendar (UTC) input
        org.junit.jupiter.params.provider.Arguments.of(utcCal, LocalDateTime.of(2023, 5, 15, 13, 45, 30), "UTC Calendar input"), // YearMonth input – first day of month at midnight
        org.junit.jupiter.params.provider.Arguments.of(YearMonth.of(2023, 5), LocalDateTime.of(2023, 5, 1, 0, 0, 0), "YearMonth input"), // Year input – first day of year at midnight
        org.junit.jupiter.params.provider.Arguments.of(Year.of(2023), LocalDateTime.of(2023, 1, 1, 0, 0, 0), "Year input"), // LocalTime alone – cannot be mapped, expect null
        org.junit.jupiter.params.provider.Arguments.of(LocalTime.of(13, 45, 30), null, "LocalTime alone"), // Date‑only string – conversion to ZonedDateTime fails, expect null
        org.junit.jupiter.params.provider.Arguments.of("2023-05-15", null, "Date‑only string"));
    }

    /**
     * Verifies that a non‑null {@link ZonedDateTime} returned by the internal conversion
     * results in the corresponding {@link LocalDateTime}.
     */
    @Test
    void toLocalDateTime_returnsLocalDateTime_whenZonedDateTimeIsPresent() {
        // GIVEN – the mock Converter returns a predefined ZonedDateTime for any input.
        when(mockConverter.convert(any(), eq(ZonedDateTime.class))).thenReturn(sampleZonedDateTime);
        // WHEN – the focal method is invoked.
        LocalDateTime result = StringConversions.toLocalDateTime("any‑value", mockConverter);
        // THEN – the result matches the LocalDateTime part of the sample ZonedDateTime.
        assertNotNull(result, "Result should not be null when ZonedDateTime is provided");
        assertEquals(expectedLocalDateTime, result, "The LocalDateTime extracted from the ZonedDateTime is incorrect");
        // Verify that the converter was asked exactly once for a ZonedDateTime conversion.
        verify(mockConverter, times(1)).convert(any(), eq(ZonedDateTime.class));
    }

    /**
     * Verifies that a {@code null} {@link ZonedDateTime} leads to a {@code null}
     * {@link LocalDateTime} result.
     */
    @Test
    void toLocalDateTime_returnsNull_whenZonedDateTimeIsNull() {
        // GIVEN – the mock Converter returns null for the ZonedDateTime conversion.
        when(mockConverter.convert(any(), eq(ZonedDateTime.class))).thenReturn(null);
        // WHEN – the focal method is called.
        LocalDateTime result = StringConversions.toLocalDateTime("any‑value", mockConverter);
        // THEN – the method should propagate the null.
        assertNull(result, "Result should be null when ZonedDateTime conversion returns null");
        verify(mockConverter, times(1)).convert(any(), eq(ZonedDateTime.class));
    }

    /**
     * Verifies the behaviour when the source object itself is {@code null}.
     * The internal conversion should receive {@code null} and, per the contract
     * of {@link Converter}, return {@code null}, which the method under test
     * propagates.
     */
    @Test
    void toLocalDateTime_returnsNull_whenSourceIsNull() {
        // GIVEN – the mock Converter returns null when asked to convert a null source.
        when(mockConverter.convert(isNull(), eq(ZonedDateTime.class))).thenReturn(null);
        // WHEN – the focal method receives a null source object.
        LocalDateTime result = StringConversions.toLocalDateTime(null, mockConverter);
        // THEN – the result must be null.
        assertNull(result, "Result should be null when the source object is null");
        verify(mockConverter, times(1)).convert(isNull(), eq(ZonedDateTime.class));
    }

    /**
     * End‑to‑end conversion test using a real {@link Converter}.
     *
     * @param source      the object to be converted
     * @param expected    the expected {@link LocalDateTime} (or {@code null})
     * @param description textual description for the test case
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("conversionProvider")
    void toLocalDateTime_endToEndConversion(Object source, LocalDateTime expected, String description) {
        // Use a real Converter implementation (assumes a public no‑arg constructor).
        Converter realConverter = new Converter();
        // The method under test will internally call toZonedDateTime, which
        // performs the actual conversion based on the source type.
        LocalDateTime result = StringConversions.toLocalDateTime(source, realConverter);
        assertEquals(expected, result, () -> "Conversion of [" + source + "] did not produce the expected LocalDateTime");
    }
}
