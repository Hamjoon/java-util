package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toCalendar_Test {

    /**
     * The mock Converter that will be supplied to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * The mock ConverterOptions returned by the mock Converter.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy so that mockConverter.getOptions()
        // returns mockOptions and mockOptions.getTimeZone() returns a known zone.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockConverter, mockOptions);
    }

    /**
     * Creates a fresh {@link Converter} mock that returns the supplied {@link TimeZone}
     * from {@code getOptions().getTimeZone()}.
     *
     * @param tz the time‑zone to be returned (may be {@code null})
     * @return a mocked {@code Converter}
     */
    private static Converter mockConverterWith(TimeZone tz) {
        Converter conv = mock(Converter.class);
        ConverterOptions opts = mock(ConverterOptions.class);
        when(conv.getOptions()).thenReturn(opts);
        when(opts.getTimeZone()).thenReturn(tz);
        return conv;
    }

    private static Stream<Arguments> provideValidConversions() {
        TimeZone defaultTZ = TimeZone.getDefault();
        return Stream.of(Arguments.of(BigInteger.valueOf(1_500_000_000_000L), defaultTZ, 1_500_000L, "Standard positive nanosecond value"), Arguments.of(BigInteger.ZERO, defaultTZ, 0L, "Zero nanoseconds (epoch start)"), Arguments.of(BigInteger.valueOf(-2_500_000_000L), defaultTZ, -2_500L, "Negative nanosecond value"), Arguments.of(new BigInteger("9223372036854775807000000"), defaultTZ, Long.MAX_VALUE, "Very large value that maps to Long.MAX_VALUE"));
    }

    private static Stream<Arguments> provideTimezoneConversions() {
        return Stream.of(Arguments.of(BigInteger.valueOf(3_600_000_000L), TimeZone.getTimeZone("GMT+02:00"), 3_600_000L, "GMT+02:00", "GMT+2 time‑zone handling"), Arguments.of(BigInteger.ZERO, TimeZone.getTimeZone("UTC"), 0L, "UTC", "UTC time‑zone handling"), Arguments.of(BigInteger.ZERO, TimeZone.getTimeZone("GMT+05:30"), 0L, "GMT+05:30", "Half‑hour offset handling"));
    }

    private static Stream<Arguments> provideRemainderCases() {
        return Stream.of(Arguments.of(BigInteger.valueOf(1_234_567_890L), 1_234L, "Positive remainder truncates toward zero"), Arguments.of(BigInteger.valueOf(-1_234_567_890L), -1L, "Negative remainder truncates toward zero"), Arguments.of(BigInteger.valueOf(-9_999_999L), -9L, "Negative remainder just under 10 ms"));
    }

    private static Stream<Arguments> provideExactLongBoundaries() {
        return Stream.of(Arguments.of(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000L)), Long.MAX_VALUE, "Maximum long value after conversion"), Arguments.of(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000L)), Long.MIN_VALUE, "Minimum long value after conversion"));
    }

    private static Stream<Arguments> provideErrorCases() {
        // Converter that returns a null TimeZone
        Converter convNullTZ = mockConverterWith(null);
        return Stream.of(Arguments.of(null, mockConverterWith(TimeZone.getDefault()), NullPointerException.class, "Null 'from' argument should cause NPE"), Arguments.of(Integer.valueOf(123), mockConverterWith(TimeZone.getDefault()), ClassCastException.class, "'from' not a BigInteger should cause CCE"), Arguments.of(BigInteger.ONE, convNullTZ, NullPointerException.class, "Null TimeZone from ConverterOptions should cause NPE"), Arguments.of(BigInteger.ONE, null, NullPointerException.class, "Null Converter argument should cause NPE"));
    }

    @Test
    void toCalendar_convertsEpochNanosToCalendarWithCorrectMillisAndTimeZone() {
        // GIVEN: a BigInteger representing epoch nanoseconds
        // 1,500,000,000 ns = 1,500 ms
        BigInteger epochNanos = BigInteger.valueOf(1_500_000_000L);
        // WHEN: the conversion method is invoked
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN: the Calendar should contain the expected millisecond value
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(1_500L, result.getTimeInMillis(), "Calendar timeInMillis should be epochNanos / 1_000_000");
        // AND: the Calendar should be created with the Converter's time‑zone (UTC)
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar time zone must match the Converter's configured time zone");
    }

    @Test
    void toCalendar_handlesNegativeEpochNanos() {
        // GIVEN: a negative epoch nanosecond value
        // -2,000,000,000 ns = -2,000 ms
        BigInteger epochNanos = BigInteger.valueOf(-2_000_000_000L);
        // WHEN: conversion is performed
        Calendar result = BigIntegerConversions.toCalendar(epochNanos, mockConverter);
        // THEN: the Calendar reflects the negative millisecond offset
        assertNotNull(result);
        assertEquals(-2_000L, result.getTimeInMillis(), "Negative epoch nanos should be correctly converted to negative milliseconds");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Time zone must still be the one supplied by the Converter");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideValidConversions")
    void toCalendar_validConversions(BigInteger from, TimeZone tz, long expectedMillis, String description) {
        // GIVEN
        Converter conv = mockConverterWith(tz);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(from, conv);
        // THEN
        assertNotNull(cal, "Resulting Calendar must not be null");
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Incorrect milliseconds after conversion");
        assertEquals(tz, cal.getTimeZone(), "Calendar time‑zone does not match the Converter's time‑zone");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideTimezoneConversions")
    void toCalendar_timezoneHandling(BigInteger from, TimeZone tz, long expectedMillis, String expectedTZId, String description) {
        // GIVEN
        Converter conv = mockConverterWith(tz);
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(from, conv);
        // THEN
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Milliseconds must be calculated independently of the time‑zone");
        assertEquals(expectedTZId, cal.getTimeZone().getID(), "Calendar must be created with the supplied time‑zone");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideRemainderCases")
    void toCalendar_remainderHandling(BigInteger from, long expectedMillis, String description) {
        // GIVEN
        Converter conv = mockConverterWith(TimeZone.getDefault());
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(from, conv);
        // THEN
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Incorrect truncation of nanoseconds to milliseconds");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideExactLongBoundaries")
    void toCalendar_exactLongBoundaries(BigInteger from, long expectedMillis, String description) {
        // GIVEN
        Converter conv = mockConverterWith(TimeZone.getDefault());
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(from, conv);
        // THEN
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Boundary value conversion failed");
    }

    @Test
    void toCalendar_calendarFieldCorrectness_oneDay() {
        // GIVEN: 86 400 000 000 000 ns = exactly one day
        BigInteger oneDayNanos = new BigInteger("86400000000000");
        Converter conv = mockConverterWith(TimeZone.getDefault());
        // WHEN
        Calendar cal = BigIntegerConversions.toCalendar(oneDayNanos, conv);
        // THEN: verify the instant and the broken‑down fields
        assertEquals(86_400_000L, cal.getTimeInMillis(), "Milliseconds for one day are incorrect");
        assertEquals(1970, cal.get(Calendar.YEAR), "Year should be 1970");
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH), "Month should be January (0)");
        assertEquals(2, cal.get(Calendar.DAY_OF_MONTH), "Day of month should be 2 (1970‑01‑02)");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideErrorCases")
    void toCalendar_errorHandling(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toCalendar(from, converter), description);
    }
}
