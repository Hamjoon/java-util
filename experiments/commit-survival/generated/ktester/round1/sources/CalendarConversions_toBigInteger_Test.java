package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigInteger_Test {

    /**
     * A mock Converter – the implementation of {@code toBigInteger} does not use it,
     *  but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A Calendar instance representing the Unix epoch (1970‑01‑01T00:00:00Z).
     */
    private static Calendar epochCalendar;

    /**
     * A Calendar instance with a known non‑zero millisecond value.
     */
    private static Calendar sampleCalendar;

    @BeforeAll
    static void setupBeforeAll() {
        // Epoch calendar – 0 milliseconds since the epoch.
        epochCalendar = new GregorianCalendar();
        epochCalendar.setTimeInMillis(0L);
        epochCalendar.setLenient(false);
        // Sample calendar – 1 234 567 890 milliseconds after the epoch.
        sampleCalendar = new GregorianCalendar();
        sampleCalendar.setTimeInMillis(1_234_567_890L);
        sampleCalendar.setLenient(false);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Null out static references to help GC (optional).
        epochCalendar = null;
        sampleCalendar = null;
    }

    /**
     * Provides {@link Calendar} instances together with the expected {@link BigInteger}
     * result and a human‑readable description.
     */
    private static Stream<Arguments> provideCalendarsForBigInteger() {
        // Helper to create a GregorianCalendar with a specific epoch milli and optional timezone.
        java.util.function.BiFunction<Long, TimeZone, Calendar> calendarFactory = (epochMilli, tz) -> {
            Calendar cal = (tz == null) ? new GregorianCalendar() : new GregorianCalendar(tz);
            cal.setTimeInMillis(epochMilli);
            cal.setLenient(false);
            return cal;
        };
        // Normal conversion – 2023‑01‑01T00:00:00Z
        Calendar c2023Utc = calendarFactory.apply(1_672_531_200_000L, null);
        BigInteger exp2023Utc = BigInteger.valueOf(1_672_531_200_000L * 1_000_000L);
        // Zero epoch – 1970‑01‑01T00:00:00Z
        Calendar cEpoch = calendarFactory.apply(0L, null);
        BigInteger expEpoch = BigInteger.ZERO;
        // Negative epoch – 1960‑01‑01T00:00:00Z
        Calendar c1960 = calendarFactory.apply(-315_619_200_000L, null);
        BigInteger exp1960 = BigInteger.valueOf(-315_619_200_000L * 1_000_000L);
        // Timezone handling – same instant as 2023‑01‑01T00:00:00 in GMT+02:00
        Calendar c2023GmtPlus2 = calendarFactory.apply(1_672_524_000_000L, TimeZone.getTimeZone("GMT+02:00"));
        BigInteger exp2023GmtPlus2 = BigInteger.valueOf(1_672_524_000_000L * 1_000_000L);
        // Millisecond precision – 2023‑03‑15T10:20:30.123Z
        Calendar cPrecise = calendarFactory.apply(1_678_875_630_123L, null);
        BigInteger expPrecise = BigInteger.valueOf(1_678_875_630_123L * 1_000_000L);
        // Upper‑bound without overflow – Long.MAX_VALUE / 1_000_000
        // 9_223_372_036_854
        long upperBound = Long.MAX_VALUE / 1_000_000L;
        Calendar cUpper = calendarFactory.apply(upperBound, null);
        BigInteger expUpper = BigInteger.valueOf(upperBound * 1_000_000L);
        // Lower‑bound without overflow – Long.MIN_VALUE / 1_000_000
        // -9_223_372_036_854
        long lowerBound = Long.MIN_VALUE / 1_000_000L;
        Calendar cLower = calendarFactory.apply(lowerBound, null);
        BigInteger expLower = BigInteger.valueOf(lowerBound * 1_000_000L);
        // Just‑below overflow – Long.MAX_VALUE / 1_000_000 - 1
        // 9_223_372_036_853
        long justBelow = (Long.MAX_VALUE / 1_000_000L) - 1;
        Calendar cJustBelow = calendarFactory.apply(justBelow, null);
        BigInteger expJustBelow = BigInteger.valueOf(justBelow * 1_000_000L);
        return Stream.of(Arguments.of(c2023Utc, exp2023Utc, "Normal conversion: 2023‑01‑01T00:00:00Z → microseconds"), Arguments.of(cEpoch, expEpoch, "Zero epoch: 1970‑01‑01T00:00:00Z → 0"), Arguments.of(c1960, exp1960, "Negative epoch: 1960‑01‑01T00:00:00Z → negative microseconds"), Arguments.of(c2023GmtPlus2, exp2023GmtPlus2, "Timezone handling: 2023‑01‑01T00:00:00 in GMT+02:00 → same microseconds as UTC instant"), Arguments.of(cPrecise, expPrecise, "Millisecond precision: includes .123 ms component"), Arguments.of(cUpper, expUpper, "Upper‑bound without overflow (Long.MAX_VALUE/1_000_000)"), Arguments.of(cLower, expLower, "Lower‑bound without overflow (Long.MIN_VALUE/1_000_000)"), Arguments.of(cJustBelow, expJustBelow, "Just‑below overflow threshold"));
    }

    @Test
    void toBigInteger_givenEpochCalendar_returnsZero() {
        // GIVEN
        Calendar from = epochCalendar;
        // WHEN
        BigInteger result = CalendarConversions.toBigInteger(from, mockConverter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "Converting the epoch (0 ms) should yield a BigInteger value of 0");
    }

    @Test
    void toBigInteger_givenNonZeroCalendar_returnsCorrectMicroseconds() {
        // GIVEN
        Calendar from = sampleCalendar;
        long expectedMicros = from.getTime().getTime() * 1_000_000L;
        BigInteger expected = BigInteger.valueOf(expectedMicros);
        // WHEN
        BigInteger result = CalendarConversions.toBigInteger(from, mockConverter);
        // THEN
        assertEquals(expected, result, "The conversion should multiply the millisecond epoch by 1,000,000 to obtain microseconds");
    }

    @Test
    void toBigInteger_givenNonCalendarObject_throwsClassCastException() {
        // GIVEN
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigInteger(notACalendar, mockConverter), "Passing a non‑Calendar object should result in a ClassCastException");
    }

    @Test
    void toBigInteger_givenNullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigInteger(nullObj, mockConverter), "Casting null to Calendar and invoking getTime() should throw NPE");
    }

    /**
     * Parameterized test that covers a wide range of epoch‑millisecond values,
     * including positive, zero, negative, timezone‑shifted, and boundary values.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideCalendarsForBigInteger")
    void toBigInteger_variousCalendars(Calendar calendar, BigInteger expected, String description) {
        // WHEN
        BigInteger result = CalendarConversions.toBigInteger(calendar, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    /**
     * Specific test for the overflow scenario where the multiplication
     * {@code epochMilli * 1_000_000L} exceeds the range of {@code long}
     * and wraps around. The method uses the overflowed {@code long} value
     * when constructing the {@link BigInteger}.
     */
    @Test
    void toBigInteger_overflowBehaviour_matchesLongOverflow() {
        // epochMilli chosen so that epochMilli * 1_000_000 overflows long
        // 9_223_372_036_855
        long overflowEpochMilli = Long.MAX_VALUE / 1_000_000L + 1;
        Calendar overflowCal = new GregorianCalendar();
        overflowCal.setTimeInMillis(overflowEpochMilli);
        overflowCal.setLenient(false);
        // Compute the overflowed long value exactly as Java would do
        // wraps around
        long overflowed = overflowEpochMilli * 1_000_000L;
        BigInteger expected = BigInteger.valueOf(overflowed);
        // WHEN
        BigInteger result = CalendarConversions.toBigInteger(overflowCal, mockConverter);
        // THEN
        assertEquals(expected, result, "Overflow handling: the result should reflect Java's long overflow semantics");
    }
}
