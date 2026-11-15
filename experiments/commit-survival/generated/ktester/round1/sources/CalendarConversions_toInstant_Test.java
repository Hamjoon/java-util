package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toInstant_Test {

    /**
     * Mocked Converter – the method under test does not use it, but a non‑null instance is required.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A Calendar instance fixed at a known epoch millisecond for deterministic testing.
     */
    private Calendar calendar;

    /**
     * The expected Instant derived from the Calendar above.
     */
    private Instant expectedInstant;

    @BeforeEach
    void setupBeforeEach() {
        // Use UTC to avoid timezone‑related offsets.
        calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // Choose an arbitrary, but fixed, epoch millisecond.
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        calendar.setTimeInMillis(epochMilli);
        expectedInstant = Instant.ofEpochMilli(epochMilli);
    }

    /**
     * Provides Calendar instances that should be converted successfully to the given Instant.
     */
    private static Stream<Arguments> provideCalendarsForSuccessfulConversion() {
        return Stream.of(// 1. Valid GregorianCalendar in UTC
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("UTC"), 2023, Calendar.JANUARY, 15, 10, 30, 45, 123), Instant.ofEpochMilli(1673782245123L), "Valid GregorianCalendar in UTC"), // 2. GregorianCalendar with America/New_York zone
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("America/New_York"), 2023, Calendar.DECEMBER, 31, 23, 59, 59, 0), Instant.ofEpochMilli(1704067199000L), "America/New_York zone should map to UTC correctly"), // 3. Custom Calendar subclass overriding toInstant()
        Arguments.of(new GregorianCalendar() {

            @Override
            public Instant toInstant() {
                return Instant.parse("2000-01-01T00:00:00Z");
            }
        }, Instant.parse("2000-01-01T00:00:00Z"), "Custom Calendar subclass with overridden toInstant"), // 4. Epoch start in UTC
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("UTC"), 1970, Calendar.JANUARY, 1, 0, 0, 0, 0), Instant.ofEpochMilli(0L), "Epoch start (1970‑01‑01T00:00:00Z)"), // 5. Maximum 32‑bit signed Unix time (2038‑01‑19T03:14:07Z)
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("UTC"), 2038, Calendar.JANUARY, 19, 3, 14, 7, 0), Instant.ofEpochMilli(2_147_483_647_000L), "Maximum 32‑bit signed Unix time"), // 6. Calendar in Asia/Kolkata near month‑end boundary
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"), 2022, Calendar.FEBRUARY, 28, 23, 59, 59, 999), Instant.ofEpochMilli(1_646_092_799_999L), "Asia/Kolkata zone near month‑end"), // 7. Calendar representing Long.MAX_VALUE epoch milli
        Arguments.of(createGregorianCalendarWithMillis(TimeZone.getTimeZone("UTC"), Long.MAX_VALUE), Instant.ofEpochMilli(Long.MAX_VALUE), "Maximum long epoch millisecond"), // 8. Calendar representing Long.MIN_VALUE epoch milli
        Arguments.of(createGregorianCalendarWithMillis(TimeZone.getTimeZone("UTC"), Long.MIN_VALUE), Instant.ofEpochMilli(Long.MIN_VALUE), "Minimum long epoch millisecond"), // 9. Leap‑day date (2020‑02‑29)
        Arguments.of(createGregorianCalendar(TimeZone.getTimeZone("UTC"), 2020, Calendar.FEBRUARY, 29, 12, 0, 0, 0), Instant.ofEpochMilli(1_582_987_200_000L), "Leap‑day handling"));
    }

    /**
     * Provides inputs that are expected to result in an exception.
     */
    private static Stream<Arguments> provideCalendarsForException() {
        return Stream.of(// 1. Null `from` argument
        Arguments.of(null, NullPointerException.class, "Null `from` should cause NullPointerException"), // 2. Non‑Calendar object (java.util.Date)
        Arguments.of(new java.util.Date(), ClassCastException.class, "Passing a Date should cause ClassCastException"), // 3. Calendar subclass whose toInstant() throws RuntimeException
        Arguments.of(new GregorianCalendar() {

            @Override
            public Instant toInstant() {
                throw new RuntimeException("custom failure");
            }
        }, RuntimeException.class, "Exception thrown by overridden toInstant should propagate"));
    }

    /**
     * Creates a {@link GregorianCalendar} with the supplied fields and time‑zone.
     */
    private static Calendar createGregorianCalendar(TimeZone tz, int year, int month, int day, int hour, int minute, int second, int milli) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    /**
     * Creates a {@link GregorianCalendar} that represents the given epoch milli.
     */
    private static Calendar createGregorianCalendarWithMillis(TimeZone tz, long epochMilli) {
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toInstant_returnsCalendarInstant() {
        // GIVEN – a Calendar (already prepared in @BeforeEach) and a mock Converter.
        // WHEN
        Instant actual = CalendarConversions.toInstant(calendar, mockConverter);
        // THEN
        assertEquals(expectedInstant, actual, "CalendarConversions.toInstant should return the same Instant as Calendar.toInstant()");
    }

    @Test
    void toInstant_invalidObject_throwsClassCastException() {
        // GIVEN – an object that is NOT a Calendar.
        Object notACalendar = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toInstant(notACalendar, mockConverter), "Passing a non‑Calendar object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideCalendarsForSuccessfulConversion")
    void toInstant_convertsCalendarsCorrectly(Calendar calendar, Instant expected, String description) {
        // The method does not use the Converter, so we pass null as per the JSON test data.
        Instant actual = CalendarConversions.toInstant(calendar, null);
        assertEquals(expected, actual, () -> "Failed conversion for case: " + description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideCalendarsForException")
    void toInstant_propagatesExceptions(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CalendarConversions.toInstant(from, null), () -> "Expected exception not thrown for case: " + description);
    }
}
