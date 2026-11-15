package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toOffsetDateTime_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    @Mock
    private TimeZone timeZone;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
        // Default stubbing – can be overridden per‑test
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(timeZone);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// UTC
        Arguments.of(Instant.parse("2023-01-01T12:34:56Z"), TimeZone.getTimeZone("UTC"), OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC)), // Fixed positive offset +02:00
        Arguments.of(Instant.parse("2023-06-15T08:00:00Z"), TimeZone.getTimeZone("GMT+02:00"), OffsetDateTime.of(2023, 6, 15, 10, 0, 0, 0, ZoneOffset.ofHours(2))), // Fixed negative offset -05:00
        Arguments.of(Instant.parse("2023-12-01T15:30:00Z"), TimeZone.getTimeZone("GMT-05:00"), OffsetDateTime.of(2023, 12, 1, 10, 30, 0, 0, ZoneOffset.ofHours(-5))), // Non‑hourly offset +05:30 (Asia/Kolkata)
        Arguments.of(Instant.parse("2023-03-20T00:00:00Z"), TimeZone.getTimeZone("Asia/Kolkata"), OffsetDateTime.of(2023, 3, 20, 5, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 30))), // Epoch UTC
        Arguments.of(Instant.ofEpochSecond(0), TimeZone.getTimeZone("UTC"), OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)), // Epoch with +02:00
        Arguments.of(Instant.ofEpochSecond(0), TimeZone.getTimeZone("GMT+02:00"), OffsetDateTime.of(1970, 1, 1, 2, 0, 0, 0, ZoneOffset.ofHours(2))), // Negative epoch UTC
        Arguments.of(Instant.ofEpochSecond(-1), TimeZone.getTimeZone("UTC"), OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), // Non‑hourly offset +05:45 (GMT+05:45)
        Arguments.of(Instant.parse("2023-04-01T00:00:00Z"), TimeZone.getTimeZone("GMT+05:45"), OffsetDateTime.of(2023, 4, 1, 5, 45, 0, 0, ZoneOffset.ofHoursMinutes(5, 45))), // Invalid TimeZone ID falls back to GMT (offset 0)
        Arguments.of(Instant.parse("2023-07-01T10:15:30Z"), TimeZone.getTimeZone("Invalid/Zone"), OffsetDateTime.of(2023, 7, 1, 10, 15, 30, 0, ZoneOffset.UTC)), // Precision – nanoseconds preserved
        Arguments.of(Instant.ofEpochSecond(0, 123_456_789), TimeZone.getTimeZone("UTC"), OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 123_456_789, ZoneOffset.UTC)));
    }

    /**
     * Verifies that the method correctly applies the ZoneOffset derived from the
     * Converter's TimeZone to the supplied Instant.
     */
    @Test
    void testToOffsetDateTime_WithNegativeOffset() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        // Simulate a timezone with a -05:00 offset (e.g., America/New_York in standard time)
        // -5 hours in milliseconds
        long offsetMillis = -5 * 60L * 60 * 1000;
        when(timeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-5);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the -05:00 zone offset");
    }

    /**
     * Verifies handling of a positive offset (e.g., +02:00).
     */
    @Test
    void testToOffsetDateTime_WithPositiveOffset() {
        // GIVEN
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        // +2 hours in milliseconds
        long offsetMillis = 2 * 60L * 60 * 1000;
        when(timeZone.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the +02:00 zone offset");
    }

    /**
     * Ensures that a ClassCastException is thrown when the supplied object is not an Instant.
     */
    @Test
    void testToOffsetDateTime_WithInvalidSourceThrowsException() {
        // GIVEN
        Object notAnInstant = "2023-01-01T00:00:00Z";
        // offset does not matter here
        when(timeZone.getOffset(anyLong())).thenReturn(0L);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> InstantConversions.toOffsetDateTime(notAnInstant, converter), "Providing a non‑Instant source should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index} ⇒ instant={0}, tz={1.id}")
    @MethodSource("normalConversionProvider")
    void testToOffsetDateTime_NormalScenarios(Instant instant, TimeZone tz, OffsetDateTime expected) {
        // GIVEN
        when(options.getTimeZone()).thenReturn(tz);
        // The method uses timeZone.getOffset(System.currentTimeMillis()).
        // For real TimeZone instances this returns the correct offset for the *current*
        // system clock.  To make the test deterministic we compute the offset ourselves
        // and stub the call.
        long offsetMillis = tz.getOffset(instant.toEpochMilli());
        when(tz.getOffset(anyLong())).thenReturn(offsetMillis);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void testToOffsetDateTime_NullConverterThrowsNPE() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(instant, null), "Passing a null Converter should throw NullPointerException");
    }

    @Test
    void testToOffsetDateTime_ConverterWithNullOptionsThrowsNPE() {
        // GIVEN
        Instant instant = Instant.now();
        // options = null
        when(converter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(instant, converter), "Null Options should cause NPE when getTimeZone() is invoked");
    }

    @Test
    void testToOffsetDateTime_OptionsWithNullTimeZoneThrowsNPE() {
        // GIVEN
        Instant instant = Instant.now();
        // timeZone = null
        when(options.getTimeZone()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(instant, converter), "Null TimeZone should cause NPE when getOffset(...) is called");
    }

    @Test
    void testToOffsetDateTime_NullSourceObjectThrowsNPE() {
        // GIVEN
        when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // The method will attempt to cast null to Instant → null, then call atOffset → NPE
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> InstantConversions.toOffsetDateTime(null, converter), "Null source object should result in NullPointerException");
    }

    @Test
    void testToOffsetDateTime_TimeZoneOffsetBeyondLimitsThrowsDateTimeException() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        // Mock a TimeZone that reports +20:00 offset
        when(options.getTimeZone()).thenReturn(timeZone);
        // +20 hours in ms
        when(timeZone.getOffset(anyLong())).thenReturn(20 * 60L * 60 * 1000);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> InstantConversions.toOffsetDateTime(instant, converter), "Offset beyond +18:00 should cause DateTimeException");
    }

    @Test
    void testToOffsetDateTime_DstBehavior_NewYorkMockedOffset() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-15T12:00:00Z");
        // New York in winter is UTC‑5, but the description assumes DST (‑04:00).
        // We explicitly stub the offset to -04:00 to make the test deterministic.
        when(options.getTimeZone()).thenReturn(timeZone);
        // -4 hours in ms
        when(timeZone.getOffset(anyLong())).thenReturn(-4 * 60L * 60 * 1000);
        // WHEN
        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, converter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-4);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        assertEquals(expected, result);
    }
}
