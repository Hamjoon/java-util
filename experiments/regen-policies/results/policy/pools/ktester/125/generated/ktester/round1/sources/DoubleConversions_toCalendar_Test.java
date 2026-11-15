package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toCalendar_Test {

    @Mock
    Converter // mock of the Converter passed to the method
    converter;

    @Mock
    ConverterOptions // mock of the ConverterOptions returned by converter.getOptions()
    options;

    @BeforeEach
    void setupBeforeEach() {
        // By default, the converter returns the mocked options instance.
        when(converter.getOptions()).thenReturn(options);
    }

    /**
     * Supplies all test cases described in the JSON document.
     *
     * Each {@link Arguments} entry contains:
     * <ol>
     *   <li>description (String)</li>
     *   <li>from value (Object) – may be Double, String, Boolean, etc.</li>
     *   <li>converter scenario identifier (String) – determines which mock behaviour to apply</li>
     *   <li>expected epoch milliseconds (Long) – {@code null} when an exception is expected</li>
     *   <li>expected time‑zone ID (String) – {@code null} when an exception is expected</li>
     *   <li>expected exception class (or {@code null} for successful conversion)</li>
     * </ol>
     */
    private static Stream<Arguments> provideToCalendarCases() {
        return Stream.of(// 1. whole‑second epoch value, UTC
        Arguments.of("Convert a whole‑second epoch value (2021‑01‑01T00:00:00Z) using a UTC time‑zone.", 1609459200.0, "converterUtc", 1609459200000L, "UTC", null), // 2. fractional seconds, PST
        Arguments.of("Fractional seconds are truncated when cast to long. 1.5 s → 1500 ms. Verify PST zone.", 1.5, "converterPst", 1500L, "America/Los_Angeles", null), // 3. zero seconds, UTC
        Arguments.of("Zero seconds should produce the epoch start regardless of time‑zone.", 0.0, "converterUtc", 0L, "UTC", null), // 4. negative seconds, Tokyo
        Arguments.of("Negative seconds are allowed; compute negative epoch millis and keep supplied zone.", -10.75, "converterTokyo", -10750L, "Asia/Tokyo", null), // 5. double larger than Long.MAX_VALUE – should clamp to Long.MAX_VALUE
        Arguments.of("A double larger than Long.MAX_VALUE is clamped to Long.MAX_VALUE when cast to long.", 9.223372036854776e+18, "converterUtc", Long.MAX_VALUE, "UTC", null), // 6. ConverterOptions returns null TimeZone → NPE from Calendar.getInstance(null)
        Arguments.of("If the Converter's Options return a null TimeZone, Calendar.getInstance(null) throws NPE.", 12345.0, "converterNullTz", null, null, NullPointerException.class), // 7. NaN → 0 ms, UTC
        Arguments.of("NaN is cast to long as 0. Calendar should contain 0 ms and keep UTC.", Double.NaN, "converterUtc", 0L, "UTC", null), // 8. POSITIVE_INFINITY → Long.MAX_VALUE, UTC
        Arguments.of("Positive infinity casts to Long.MAX_VALUE. Verify Calendar stores Long.MAX_VALUE ms and uses UTC.", Double.POSITIVE_INFINITY, "converterUtc", Long.MAX_VALUE, "UTC", null), // 9. NEGATIVE_INFINITY → Long.MIN_VALUE, UTC
        Arguments.of("Negative infinity casts to Long.MIN_VALUE. Calendar must reflect Long.MIN_VALUE ms with UTC zone.", Double.NEGATIVE_INFINITY, "converterUtc", Long.MIN_VALUE, "UTC", null), // 10. sub‑millisecond value 0.0004 → 0 ms, UTC
        Arguments.of("A sub‑millisecond value (0.0004 s → 0.4 ms) truncates to 0 ms.", 0.0004, "converterUtc", 0L, "UTC", null), // 11. sub‑millisecond value 0.0006 → 0 ms, UTC
        Arguments.of("Another sub‑millisecond value (0.0006 s → 0.6 ms) also truncates to 0 ms.", 0.0006, "converterUtc", 0L, "UTC", null), // 12. typical fractional seconds, GMT+02:00
        Arguments.of("Typical fractional seconds with a custom GMT+02:00 zone. Fractional part is dropped after multiplication.", 123456789.123, "converterGmtPlus2", 123456789123L, "GMT+02:00", null), // 13. seconds just above Long.MAX_VALUE/1000 → clamp to Long.MAX_VALUE, UTC
        Arguments.of("Seconds just above Long.MAX_VALUE/1000 cause epochMillis overflow; casting clamps to Long.MAX_VALUE.", 9223372036854.775, "converterUtc", Long.MAX_VALUE, "UTC", null), // 14. negative seconds just below Long.MIN_VALUE/1000 → clamp to Long.MIN_VALUE, UTC
        Arguments.of("Negative seconds just below Long.MIN_VALUE/1000 overflow to Long.MIN_VALUE after casting.", -9223372036854.775, "converterUtc", Long.MIN_VALUE, "UTC", null), // 15. extremely small positive value → 0 ms, UTC
        Arguments.of("An extremely small positive value results in 0 ms after truncation.", 1e-12, "converterUtc", 0L, "UTC", null), // 16. extremely small negative value → 0 ms, UTC
        Arguments.of("An extremely small negative value also truncates to 0 ms.", -1e-12, "converterUtc", 0L, "UTC", null), // 17. non‑numeric String → ClassCastException
        Arguments.of("The 'from' argument is a String, which cannot be cast to double. Expect ClassCastException.", "not-a-number", "converterUtc", null, null, ClassCastException.class), // 18. Boolean true → ClassCastException
        Arguments.of("The 'from' argument is a Boolean, which cannot be cast to double. Expect ClassCastException.", true, "converterUtc", null, null, ClassCastException.class), // 19. null converter → NullPointerException
        Arguments.of("The 'converter' argument itself is null. Invoking converter.getOptions() should raise NPE.", 12345.0, "null", null, null, NullPointerException.class), // 20. converter with null options → NullPointerException
        Arguments.of("Converter instance non‑null but its getOptions() returns null. Accessing getTimeZone() on null must throw NPE.", 12345.0, "converterNullOptions", null, null, NullPointerException.class));
    }

    @Test
    void toCalendar_usesUtcTimeZone_andCalculatesCorrectEpochMillis() {
        // GIVEN
        // 2021‑06‑01T00:00:00Z
        double seconds = 1622548800.0;
        long expectedMillis = (long) (seconds * 1000L);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(options.getTimeZone()).thenReturn(utc);
        // WHEN
        Calendar result = DoubleConversions.toCalendar(seconds, converter);
        // THEN
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar should contain the correct epoch milliseconds");
        assertEquals(utc, result.getTimeZone(), "Calendar should be created with the UTC time zone");
    }

    @Test
    void toCalendar_respectsProvidedTimeZone() {
        // GIVEN
        // epoch start
        double seconds = 0.0;
        long expectedMillis = 0L;
        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        when(options.getTimeZone()).thenReturn(newYork);
        // WHEN
        Calendar result = DoubleConversions.toCalendar(seconds, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTimeInMillis(), "Epoch start should still be 0 milliseconds regardless of zone");
        assertEquals(newYork, result.getTimeZone(), "Calendar should be created with the supplied New York time zone");
    }

    @Test
    void toCalendar_handlesNegativeSeconds() {
        // GIVEN
        // some time before the epoch
        double seconds = -12345.678;
        long expectedMillis = (long) (seconds * 1000L);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(options.getTimeZone()).thenReturn(utc);
        // WHEN
        Calendar result = DoubleConversions.toCalendar(seconds, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTimeInMillis(), "Negative seconds should be correctly converted to negative epoch milliseconds");
        assertEquals(utc, result.getTimeZone(), "Time zone should still be UTC as configured");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideToCalendarCases")
    void toCalendar_parameterised(String description, Object from, String converterScenario, Long expectedMillis, String expectedTimeZoneId, Class<? extends Throwable> expectedException) {
        // -------------------------------------------------------------
        // Arrange – configure the mock according to the scenario name
        // -------------------------------------------------------------
        switch(converterScenario) {
            case "converterUtc":
                when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
                break;
            case "converterPst":
                when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("America/Los_Angeles"));
                break;
            case "converterTokyo":
                when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("Asia/Tokyo"));
                break;
            case "converterGmtPlus2":
                when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("GMT+02:00"));
                break;
            case "converterNullTz":
                when(options.getTimeZone()).thenReturn(null);
                break;
            case "converterNullOptions":
                when(converter.getOptions()).thenReturn(null);
                break;
            case "null":
                // leave converter as null
                converter = null;
                break;
            default:
                // default – use UTC to avoid NPE if the test data is malformed
                when(options.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        }
        // -------------------------------------------------------------
        // Act / Assert
        // -------------------------------------------------------------
        if (expectedException != null) {
            // Expect an exception – the exact type matters
            Throwable thrown = assertThrows(expectedException, () -> DoubleConversions.toCalendar(from, converter), description);
            // For NPE we don't need further assertions
        } else {
            Calendar result = DoubleConversions.toCalendar(from, converter);
            assertNotNull(result, description + " – result must not be null");
            assertEquals(expectedMillis.longValue(), result.getTimeInMillis(), description + " – epoch milliseconds");
            assertEquals(expectedTimeZoneId, result.getTimeZone().getID(), description + " – time‑zone ID");
        }
    }
}
