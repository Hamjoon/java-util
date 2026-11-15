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
import java.time.*;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigInteger_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Calendar referenceCalendar;

    private static long referenceEpochMilli;

    /**
     * A custom {@link Calendar} subclass that deliberately returns {@code null}
     * from {@link #getTime()}, used to verify the NPE path.
     */
    private static final class CalendarWithNullTime extends GregorianCalendar {

        @Override
        public java.util.Date getTime() {
            // force NPE in CalendarConversions.toBigInteger
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Deterministic instant: 2023‑01‑01T12:34:56.789Z
        referenceCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        referenceCalendar.setTimeInMillis(1672578896789L);
        referenceEpochMilli = referenceCalendar.getTimeInMillis();
    }

    @AfterAll
    static void teardownAfterAll() {
        referenceCalendar = null;
    }

    /**
     * Provides successful conversion scenarios.
     *
     * Each argument consists of:
     * <ul>
     *   <li>a {@link Calendar} instance configured as described in the test case</li>
     *   <li>the expected {@link BigInteger} result (computed at runtime)</li>
     *   <li>a short description for readability</li>
     * </ul>
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// 2023‑01‑01T00:00:00Z  → epoch millis 1672531200000
        Arguments.of(calendarFromInstant(Instant.parse("2023-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC")), BigInteger.valueOf(1672531200000L * 1_000_000L), "Standard UTC calendar conversion"), // Epoch start (1970‑01‑01T00:00:00Z) → 0
        Arguments.of(calendarFromInstant(Instant.EPOCH, TimeZone.getTimeZone("UTC")), BigInteger.ZERO, "Epoch start should produce zero"), // One millisecond before epoch → -1
        Arguments.of(calendarFromInstant(Instant.ofEpochMilli(-1L), TimeZone.getTimeZone("UTC")), BigInteger.valueOf(-1L * 1_000_000L), "Negative epoch millis should yield negative microseconds"), // DST gap in America/New_York: 2023‑03‑12T02:30:00 (non‑existent)
        // The underlying instant is 2023‑03‑12T07:30:00Z
        Arguments.of(calendarFromInstant(Instant.parse("2023-03-12T07:30:00Z"), TimeZone.getTimeZone("America/New_York")), BigInteger.valueOf(Instant.parse("2023-03-12T07:30:00Z").toEpochMilli() * 1_000_000L), "DST gap – conversion must use the stored instant, ignoring the gap"), // Calendar with max long epoch millis (overflow test)
        Arguments.of(calendarFromInstant(Instant.ofEpochMilli(Long.MAX_VALUE), TimeZone.getTimeZone("UTC")), // The multiplication overflows in long arithmetic; we mimic the same behaviour.
        BigInteger.valueOf(Long.MAX_VALUE * 1_000_000L), "Overflow case – long multiplication wraps around"), // Asia/Tokyo local midnight → 2022‑12‑31T15:00:00Z
        Arguments.of(calendarFromInstant(Instant.parse("2022-12-31T15:00:00Z"), TimeZone.getTimeZone("Asia/Tokyo")), BigInteger.valueOf(1672444800000L * 1_000_000L), "Timezone test – calendar stores instant in UTC"), // Generic UTC calendar with arbitrary millis
        Arguments.of(calendarFromInstant(Instant.ofEpochMilli(123_456_789L), TimeZone.getTimeZone("UTC")), BigInteger.valueOf(123_456_789L * 1_000_000L), "Simple positive value test"), // Non‑null Converter (mock) – should be ignored
        Arguments.of(calendarFromInstant(Instant.parse("2023-06-15T12:34:56Z"), TimeZone.getTimeZone("UTC")), BigInteger.valueOf(1686833696000L * 1_000_000L), "Non‑null Converter argument is ignored"), // Lenient = false, date = 2022‑02‑28
        Arguments.of(// months are 0‑based
        lenientCalendar(2022, 1, 28, TimeZone.getTimeZone("UTC")), BigInteger.valueOf(1646006400000L * 1_000_000L), "Lenient‑false calendar conversion"));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Non‑Calendar object
        Arguments.of("2023-01-01", ClassCastException.class, "Passing a String should cause ClassCastException"), // Calendar subclass whose getTime() returns null
        Arguments.of(new CalendarWithNullTime(), NullPointerException.class, "Calendar with null getTime() should cause NPE"));
    }

    /**
     * Creates a {@link Calendar} whose underlying instant is the supplied {@link Instant}
     * and whose {@link TimeZone} is set as requested.
     */
    private static Calendar calendarFromInstant(Instant instant, TimeZone tz) {
        Calendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(instant.toEpochMilli());
        return cal;
    }

    /**
     * Creates a lenient‑false {@link GregorianCalendar} set to the supplied date.
     * Note: month is zero‑based (January = 0) to match {@link Calendar} API.
     */
    private static Calendar lenientCalendar(int year, int monthZeroBased, int day, TimeZone tz) {
        Calendar cal = new GregorianCalendar(tz);
        cal.setLenient(false);
        // clear all fields to avoid residual values
        cal.clear();
        cal.set(year, monthZeroBased, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    @DisplayName("toBigInteger converts Calendar epoch millis to microseconds as BigInteger")
    void testToBigIntegerConversion() {
        // given
        Calendar cal = (Calendar) referenceCalendar.clone();
        // when
        BigInteger result = CalendarConversions.toBigInteger(cal, mockConverter);
        // then
        BigInteger expected = BigInteger.valueOf(referenceEpochMilli * 1_000_000L);
        assertEquals(expected, result, "BigInteger value should be epoch millis multiplied by 1,000,000");
    }

    @Test
    @DisplayName("toBigInteger throws ClassCastException when source is not a Calendar")
    void testToBigIntegerWithInvalidSource() {
        // given
        Object notACalendar = "I am not a calendar";
        // when / then
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigInteger(notACalendar, mockConverter), "Expected ClassCastException when source is not a Calendar");
    }

    @Test
    @DisplayName("toBigInteger throws NullPointerException when source is null")
    void testToBigIntegerWithNullSource() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigInteger(nullSource, mockConverter), "Expected NullPointerException when source is null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideSuccessfulConversions")
    @DisplayName("toBigInteger successful conversion scenarios")
    void testToBigIntegerSuccessfulConversions(Calendar cal, BigInteger expected, String description) {
        // Use the mockConverter for the case that explicitly checks a non‑null converter,
        // otherwise passing null is also fine because the method never touches it.
        Converter converter = (description.contains("Non‑null Converter") ? mockConverter : null);
        BigInteger result = CalendarConversions.toBigInteger(cal, converter);
        assertEquals(expected, result, "Unexpected conversion result for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideExceptionScenarios")
    @DisplayName("toBigInteger exception scenarios")
    void testToBigIntegerExceptions(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CalendarConversions.toBigInteger(source, null), "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
