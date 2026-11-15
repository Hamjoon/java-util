package com.cedarsoftware.util.convert;

import java.time.Year;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toYear_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC-5/-4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static final TimeZone LOS_ANGELES = TimeZone.getTimeZone("America/Los_Angeles");

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions implementation; DefaultConverterOptions is the
        // standard one used throughout the library.
        converter = new Converter(new DefaultConverterOptions());
    }

    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar month is zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Provides successful conversion scenarios.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the source {@link Calendar}</li>
     *   <li>the {@link ZoneId} that the mocked {@link Converter} should expose</li>
     *   <li>the expected {@link Year}</li>
     * </ul>
     */
    private static Stream<Arguments> provideSuccessCases() {
        return Stream.of(// Standard UTC case
        Arguments.of(createCalendar(2023, 5, 17, 10, 30, 0, 0, UTC), ZoneId.of("UTC"), Year.of(2023)), // Near‑year‑boundary in New York, zone matches calendar's zone
        Arguments.of(createCalendar(1999, 12, 31, 23, 59, 59, 0, NEW_YORK), ZoneId.of("America/New_York"), Year.of(1999)), // Exact start of a new year in Tokyo
        Arguments.of(createCalendar(2000, 1, 1, 0, 0, 0, 0, TOKYO), ZoneId.of("Asia/Tokyo"), Year.of(2000)), // Leap‑year date
        Arguments.of(createCalendar(2024, 2, 29, 12, 0, 0, 0, UTC), ZoneId.of("UTC"), Year.of(2024)), // DST gap (America/Los_Angeles switches at 02:00 → 03:00)
        Arguments.of(createCalendar(2021, 3, 14, 2, 30, 0, 0, LOS_ANGELES), ZoneId.of("America/Los_Angeles"), Year.of(2021)), // Calendar instant is early Jan 1 2023 UTC, but converter zone is LA → Dec 31 2022
        Arguments.of(createCalendar(2023, 1, 1, 1, 0, 0, 0, UTC), ZoneId.of("America/Los_Angeles"), Year.of(2022)), // New York late Dec 31 2022, converter zone UTC → Jan 1 2023
        Arguments.of(createCalendar(2022, 12, 31, 23, 0, 0, 0, NEW_YORK), ZoneId.of("UTC"), Year.of(2023)), // Pre‑1970 date
        Arguments.of(createCalendar(1965, 7, 20, 12, 0, 0, 0, UTC), ZoneId.of("UTC"), Year.of(1965)), // Very large (but still within Year.MAX_VALUE) year
        Arguments.of(createCalendar(9999, 12, 31, 23, 59, 59, 0, UTC), ZoneId.of("UTC"), Year.of(9999)));
    }

    /**
     * Provides scenarios that are expected to throw an exception.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the {@code from} object (Calendar, Date, null, etc.)</li>
     *   <li>a {@link Converter} (may be {@code null} or a mock with special behaviour)</li>
     *   <li>the expected exception class</li>
     * </ul>
     */
    private static Stream<Arguments> provideExceptionCases() {
        // Helper for a mock Converter with null options
        Converter converterWithNullOptions = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Helper for a mock Converter whose options return a null ZoneId
        Converter converterWithNullZone = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converterWithNullZone.getOptions().getZoneId()).thenReturn(null);
        // Calendar with a year beyond Year.MAX_VALUE (Year.MAX_VALUE = 999_999_999)
        Calendar outOfRangeCal = createCalendar(1_000_000_000, 1, 1, 0, 0, 0, 0, UTC);
        return Stream.of(// Null calendar – the method casts to Calendar and throws ClassCastException
        Arguments.of(null, new Converter(new DefaultConverterOptions()), ClassCastException.class), // Wrong type (java.util.Date) – also results in ClassCastException
        Arguments.of(new Date(), new Converter(new DefaultConverterOptions()), ClassCastException.class), // Converter with null options → NPE when getOptions().getZoneId()
        Arguments.of(createCalendar(2023, 5, 17, 10, 30, 0, 0, UTC), converterWithNullOptions, NullPointerException.class), // Converter whose options contain a null ZoneId → NPE
        Arguments.of(createCalendar(2023, 5, 17, 10, 30, 0, 0, UTC), converterWithNullZone, NullPointerException.class), // Null converter argument → NPE when calling getOptions()
        Arguments.of(createCalendar(2023, 5, 17, 10, 30, 0, 0, UTC), null, NullPointerException.class), // Year out of range – Year.from(LocalDate) throws DateTimeException
        Arguments.of(outOfRangeCal, new Converter(new DefaultConverterOptions()), DateTimeException.class));
    }

    @Test
    void testToYear_WithVariousTimeZones() {
        // 1888‑01‑02 09:15:30.333 in Paris (UTC+1) → Year 1888
        assertEquals(Year.of(1888), CalendarConversions.toYear(createCalendar(1888, 1, 2, 9, 15, 30, 333, PARIS), converter));
        // 1969‑12‑31 18:45:15.777 in New York (UTC‑5) → Year 1969
        assertEquals(Year.of(1969), CalendarConversions.toYear(createCalendar(1969, 12, 31, 18, 45, 15, 777, NEW_YORK), converter));
        // 1970‑01‑01 06:20:10.111 in Tokyo (UTC+9) → Year 1969 (because instant is still 1969‑12‑31 UTC)
        assertEquals(Year.of(1969), CalendarConversions.toYear(createCalendar(1970, 1, 1, 6, 20, 10, 111, TOKYO), converter));
        // 2023‑06‑15 21:05:55.888 in Paris (UTC+2 during DST) → Year 2023
        assertEquals(Year.of(2023), CalendarConversions.toYear(createCalendar(2023, 6, 15, 21, 5, 55, 888, PARIS), converter));
    }

    @Test
    void testToYear_NullCalendarThrowsException() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toYear(null, converter));
    }

    @ParameterizedTest(name = "[{index}] Calendar {0} with zone {1} → {2}")
    @MethodSource("provideSuccessCases")
    void testToYear_SuccessfulConversions(Calendar calendar, ZoneId zoneId, Year expected) {
        // Mock a Converter that returns the supplied ZoneId via its options
        Converter mockConverter = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(mockConverter.getOptions().getZoneId()).thenReturn(zoneId);
        Year result = CalendarConversions.toYear(calendar, mockConverter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Expect {2} for input {0}")
    @MethodSource("provideExceptionCases")
    void testToYear_ExceptionScenarios(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toYear(from, conv));
    }
}
