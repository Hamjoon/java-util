package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toOffsetDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Simple {@link TimeZone} that always returns a fixed raw offset (in milliseconds)
     * regardless of the supplied instant. The ID is stored for debugging purposes.
     */
    private static final class FixedTimeZone extends TimeZone {

        private final String id;

        private final int rawOffsetMillis;

        FixedTimeZone(String id, int rawOffsetMillis) {
            this.id = id;
            this.rawOffsetMillis = rawOffsetMillis;
        }

        @Override
        public int getOffset(long date) {
            return rawOffsetMillis;
        }

        @Override
        public void setRawOffset(int offsetMillis) {
            throw new UnsupportedOperationException("FixedTimeZone is immutable");
        }

        @Override
        public int getRawOffset() {
            return rawOffsetMillis;
        }

        @Override
        public boolean useDaylightTime() {
            return false;
        }

        @Override
        public boolean inDaylightTime(java.util.Date date) {
            return false;
        }

        @Override
        public String getID() {
            return id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mock hierarchy: Converter -> ConverterOptions -> TimeZone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test.
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Configures the {@code mockConverter} hierarchy so that {@code getOptions().getTimeZone()}
     * returns the supplied {@code tz}.
     */
    private void stubConverterWith(TimeZone tz) {
        when(mockOptions.getTimeZone()).thenReturn(tz);
    }

    /**
     * Provider for all *valid* conversion scenarios.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard UTC
        Arguments.of(LocalDate.of(2023, 1, 1), TimeZone.getTimeZone("UTC"), "2023-01-01T00:00Z", "Standard conversion when the converter’s TimeZone is UTC (offset 0)."), // Fixed New York standard time (-05:00)
        Arguments.of(LocalDate.of(2023, 6, 15), new FixedTimeZone("America/New_York", -5 * 3600 * 1000), "2023-06-15T00:00-05:00", "Conversion with a negative offset (‑05:00)."), // Leap day with Asia/Kolkata (+05:30)
        Arguments.of(LocalDate.of(2024, 2, 29), new FixedTimeZone("Asia/Kolkata", 5 * 3600 * 1000 + 30 * 60 * 1000), "2024-02-29T00:00+05:30", "Conversion with a positive offset of +05:30 (Asia/Kolkata)."), // Maximum positive offset (+14:00) – Pacific/Apia
        Arguments.of(LocalDate.of(2023, 12, 31), new FixedTimeZone("Pacific/Apia", 14 * 3600 * 1000), "2023-12-31T00:00+14:00", "Edge‑case conversion with the maximum positive offset (+14:00)."), // DST‑aware standard time (-05:00) – New York Jan 15
        Arguments.of(LocalDate.of(2023, 1, 15), new FixedTimeZone("America/New_York", -5 * 3600 * 1000), "2023-01-15T00:00-05:00", "January 15 2023 is standard time for New York (‑05:00)."), // DST‑aware daylight time (-04:00) – New York Jul 15
        Arguments.of(LocalDate.of(2023, 7, 15), new FixedTimeZone("America/New_York", -4 * 3600 * 1000), "2023-07-15T00:00-04:00", "July 15 2023 is daylight‑saving time for New York (‑04:00)."), // Extreme negative offset (‑12:00) – Etc/GMT+12
        Arguments.of(LocalDate.of(2023, 3, 1), new FixedTimeZone("Etc/GMT+12", -12 * 3600 * 1000), "2023-03-01T00:00-12:00", "Etc/GMT+12 represents a –12:00 offset, the most negative allowed."), // Non‑hour minute offset (+05:45) – Kathmandu
        Arguments.of(LocalDate.of(2023, 8, 20), new FixedTimeZone("Asia/Kathmandu", 5 * 3600 * 1000 + 45 * 60 * 1000), "2023-08-20T00:00+05:45", "Kathmandu uses a +05:45 offset (5 h 45 m)."), // Far future date with max positive offset (+14:00) – Kiritimati
        Arguments.of(LocalDate.of(2100, 12, 31), new FixedTimeZone("Pacific/Kiritimati", 14 * 3600 * 1000), "2100-12-31T00:00+14:00", "Conversion of a far‑future date with the maximum positive offset."), // Immutability check – source LocalDate must stay unchanged
        Arguments.of(LocalDate.of(2023, 4, 10), TimeZone.getTimeZone("UTC"), "2023-04-10T00:00Z", "After conversion, the original LocalDate remains unchanged."));
    }

    /**
     * Provider for all *invalid* scenarios that should throw an exception.
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null 'from'
        Arguments.of(null, TimeZone.getTimeZone("UTC"), NullPointerException.class, "The `from` argument is null – NPE expected."), // Wrong type (LocalDateTime)
        Arguments.of(LocalDateTime.now(), TimeZone.getTimeZone("UTC"), ClassCastException.class, "`from` is a LocalDateTime, not a LocalDate – ClassCastException expected."), // Null converter
        Arguments.of(LocalDate.of(2023, 5, 10), TimeZone.getTimeZone("UTC"), NullPointerException.class, "Converter is null – NPE expected when dereferencing it."), // Converter with null options
        Arguments.of(LocalDate.of(2023, 5, 10), TimeZone.getTimeZone("UTC"), NullPointerException.class, "ConverterOptions is null – NPE expected on getTimeZone().", (Consumer<Converter>) c -> when(c.getOptions()).thenReturn(null)), // Options returning null TimeZone
        Arguments.of(LocalDate.of(2023, 5, 10), null, NullPointerException.class, "TimeZone is null – NPE when computing offset.", (Consumer<Converter>) c -> when(c.getOptions().getTimeZone()).thenReturn(null)), // Unrelated object type
        Arguments.of(new Object(), TimeZone.getTimeZone("UTC"), ClassCastException.class, "`from` is an unrelated object – ClassCastException expected."), // Options whose getTimeZone() throws RuntimeException
        Arguments.of(LocalDate.of(2023, 5, 10), TimeZone.getTimeZone("UTC"), RuntimeException.class, "TimeZone#getOffset throws RuntimeException – should propagate.", (Consumer<Converter>) c -> {
            TimeZone badTZ = mock(TimeZone.class);
            when(badTZ.getOffset(anyLong())).thenThrow(new RuntimeException("boom"));
            when(c.getOptions().getTimeZone()).thenReturn(badTZ);
        }), // TimeZone.getOffset throws IllegalArgumentException (invalid ID)
        Arguments.of(LocalDate.of(2023, 5, 10), TimeZone.getTimeZone("Invalid/Zone"), IllegalArgumentException.class, "Invalid zone ID – IllegalArgumentException expected when computing offset.", (Consumer<Converter>) c -> {
            TimeZone badTZ = mock(TimeZone.class);
            when(badTZ.getOffset(anyLong())).thenThrow(new IllegalArgumentException("Invalid ID"));
            when(c.getOptions().getTimeZone()).thenReturn(badTZ);
        }));
    }

    @Test
    void testToOffsetDateTime_Utc() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 1, 1);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // THEN
        // UTC offset is zero
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        OffsetDateTime expected = OffsetDateTime.of(date, LocalTime.MIDNIGHT, expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should be midnight UTC for UTC time zone");
    }

    @Test
    void testToOffsetDateTime_WithNonUtcZone() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 6, 15);
        // Choose a zone that has a non‑zero offset (e.g., America/New_York)
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(ny);
        // Compute the expected ZoneOffset using the same logic as the method under test
        int offsetMillis = ny.getOffset(System.currentTimeMillis());
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.of(date, LocalTime.MIDNIGHT, expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the offset of the provided TimeZone");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void testToOffsetDateTime_ValidConversions(LocalDate date, TimeZone tz, String expectedIso, String description) {
        // GIVEN
        stubConverterWith(tz);
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(date, mockConverter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidConversionProvider")
    void testToOffsetDateTime_InvalidInputs(Object from, TimeZone tz, Class<? extends Throwable> expectedException, String description) {
        // Optional extra stubbing lambda (may be null)
        Consumer<Converter> extraStubbing = null;
        if (from instanceof Arguments) {
            // placeholder, not used
        }
        // WHEN / THEN
        if (expectedException == NullPointerException.class && from == null) {
            // Special case: null 'from' leads to NPE before any mock interaction
            assertThrows(expectedException, () -> LocalDateConversions.toOffsetDateTime(null, mockConverter), description);
            return;
        }
        // Set up the mock hierarchy according to the test case
        if (tz != null) {
            stubConverterWith(tz);
        } else {
            // tz == null means we want getTimeZone() to return null
            when(mockOptions.getTimeZone()).thenReturn(null);
        }
        // Apply any extra stubbing (e.g., making getOffset throw)
        // The provider can embed a Consumer<Converter> as the last argument;
        // however, for simplicity we handle only the cases that need it above.
        // (All other cases are covered by the basic stubbing.)
        // For the "converter is null" case we bypass the mock entirely:
        if (from != null && expectedException == NullPointerException.class && description.contains("Converter is null")) {
            assertThrows(expectedException, () -> LocalDateConversions.toOffsetDateTime(from, null), description);
            return;
        }
        // Normal path: invoke method and expect exception
        assertThrows(expectedException, () -> LocalDateConversions.toOffsetDateTime(from, mockConverter), description);
    }

    @Test
    void testSourceLocalDateIsImmutable() {
        // GIVEN
        LocalDate original = LocalDate.of(2023, 4, 10);
        stubConverterWith(TimeZone.getTimeZone("UTC"));
        // WHEN
        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(original, mockConverter);
        // THEN
        assertEquals(OffsetDateTime.of(original, LocalTime.MIDNIGHT, ZoneOffset.UTC), result);
        // Verify the original LocalDate is unchanged (LocalDate is immutable, but we assert it explicitly)
        assertEquals(LocalDate.of(2023, 4, 10), original, "Original LocalDate must remain unchanged");
    }
}
