package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneOffsetConversions_toTimeZone_Test {

    // not used by the method but required by signature
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a real Converter instance – the method under test does not depend on its behaviour
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        converter = null;
    }

    /* --------------------------------------------------------------
     *  UTC offset handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> utcOffsetProvider() {
        return Stream.of(// ZoneOffset.UTC with a null converter
        Arguments.of(ZoneOffset.UTC, null, "GMT"), // Zero offset created via ofHours(0) – should be treated as UTC
        Arguments.of(ZoneOffset.ofHours(0), null, "GMT"));
    }

    /* --------------------------------------------------------------
     *  Positive offset handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> positiveOffsetProvider() {
        return Stream.of(// +05:30
        Arguments.of(ZoneOffset.ofHoursMinutes(5, 30), null, "GMT+05:30"), // +01:02:03 (hours, minutes, seconds)
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(1, 2, 3), null, "GMT+01:02:03"));
    }

    /* --------------------------------------------------------------
     *  Negative offset handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> negativeOffsetProvider() {
        return Stream.of(// -02:00
        Arguments.of(ZoneOffset.ofHours(-2), null, "GMT-02:00"), // -03:45
        Arguments.of(ZoneOffset.ofHoursMinutes(-3, -45), null, "GMT-03:45"));
    }

    /* --------------------------------------------------------------
     *  Invalid input handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Non‑ZoneOffset object
        Arguments.of("not a ZoneOffset", null, ClassCastException.class), // Null source object
        Arguments.of(null, null, NullPointerException.class));
    }

    /* --------------------------------------------------------------
     *  Maximum offset handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> maximumOffsetProvider() {
        return Stream.of(Arguments.of(ZoneOffset.ofHours(18), null, "GMT+18:00"), Arguments.of(ZoneOffset.ofHours(-18), null, "GMT-18:00"));
    }

    /* --------------------------------------------------------------
     *  Zero offset variations
     * -------------------------------------------------------------- */
    private static Stream<Arguments> zeroOffsetVariationsProvider() {
        return Stream.of(// Zero offset via ofHoursMinutes(0,0)
        Arguments.of(ZoneOffset.ofHoursMinutes(0, 0), null, "GMT"), // Zero offset via ofHoursMinutesSeconds(0,0,0)
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(0, 0, 0), null, "GMT"), // Positive minute‑only offset (+00:45)
        Arguments.of(ZoneOffset.ofHoursMinutes(0, 45), null, "GMT+00:45"), // Negative minute‑only offset (-00:30)
        Arguments.of(ZoneOffset.ofHoursMinutes(0, -30), null, "GMT-00:30"));
    }

    /* --------------------------------------------------------------
     *  Mixed sign minutes/seconds handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> mixedSignProvider() {
        return Stream.of(// +02:00:30
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(2, 0, 30), null, "GMT+02:00:30"), // -05:30:15
        Arguments.of(ZoneOffset.ofHoursMinutesSeconds(-5, -30, -15), null, "GMT-05:30:15"));
    }

    /* --------------------------------------------------------------
     *  Non‑null converter handling
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nonNullConverterProvider() {
        // Use a freshly created Converter instance (different from the field 'converter')
        return Stream.of(Arguments.of(ZoneOffset.ofHours(3), new Converter(new ConverterOptions()), "GMT+03:00"));
    }

    @Test
    void utcOffsetReturnsGMT() {
        // given
        ZoneOffset utc = ZoneOffset.UTC;
        // when
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, converter);
        // then
        assertNotNull(tz, "Resulting TimeZone should not be null");
        assertEquals("GMT", tz.getID(), "UTC offset must be represented as plain \"GMT\"");
        assertEquals(0, tz.getRawOffset(), "GMT should have a raw offset of 0 milliseconds");
    }

    @Test
    void positiveOffsetIsConvertedCorrectly() {
        // given
        // +05:30
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        // when
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // then
        assertNotNull(tz);
        assertEquals("GMT+05:30", tz.getID(), "Positive offset should be prefixed with \"GMT+\"");
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @Test
    void negativeOffsetIsConvertedCorrectly() {
        // given
        // -04:00
        ZoneOffset offset = ZoneOffset.of("-04:00");
        // when
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // then
        assertNotNull(tz);
        assertEquals("GMT-04:00", tz.getID(), "Negative offset should be prefixed with \"GMT-\"");
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @Test
    void wholeHourPositiveOffset() {
        // given
        // +02:00
        ZoneOffset offset = ZoneOffset.ofHours(2);
        // when
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // then
        assertNotNull(tz);
        assertEquals("GMT+02:00", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    void wholeHourNegativeOffset() {
        // given
        // -07:00
        ZoneOffset offset = ZoneOffset.ofHours(-7);
        // when
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // then
        assertNotNull(tz);
        assertEquals("GMT-07:00", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @ParameterizedTest(name = "[{index}] UTC offset {0} → {2}")
    @MethodSource("utcOffsetProvider")
    void utcOffsetHandling(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz, "Resulting TimeZone must not be null");
        assertEquals(expectedId, tz.getID(), "UTC offset must be represented as plain \"GMT\"");
        assertEquals(0, tz.getRawOffset(), "GMT should have a raw offset of 0 milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Positive offset {0} → {2}")
    @MethodSource("positiveOffsetProvider")
    void positiveOffsetHandling(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Negative offset {0} → {2}")
    @MethodSource("negativeOffsetProvider")
    void negativeOffsetHandling(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Invalid input {0} throws {2}")
    @MethodSource("invalidInputProvider")
    void invalidInputHandling(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ZoneOffsetConversions.toTimeZone(from, conv), "Expected exception when passing invalid input");
    }

    @ParameterizedTest(name = "[{index}] Maximum offset {0} → {2}")
    @MethodSource("maximumOffsetProvider")
    void maximumOffsetHandling(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Zero/Minute variation {0} → {2}")
    @MethodSource("zeroOffsetVariationsProvider")
    void zeroOffsetVariations(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Mixed sign offset {0} → {2}")
    @MethodSource("mixedSignProvider")
    void mixedSignMinutesSecondsHandling(ZoneOffset from, Converter conv, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, conv);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }

    @ParameterizedTest(name = "[{index}] Non‑null converter with offset {0} → {2}")
    @MethodSource("nonNullConverterProvider")
    void nonNullConverterHandling(ZoneOffset from, Converter nonNullConverter, String expectedId) {
        TimeZone tz = ZoneOffsetConversions.toTimeZone(from, nonNullConverter);
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
        assertEquals(from.getTotalSeconds() * 1000L, tz.getRawOffset(), "Raw offset must match the ZoneOffset in milliseconds");
    }
}
