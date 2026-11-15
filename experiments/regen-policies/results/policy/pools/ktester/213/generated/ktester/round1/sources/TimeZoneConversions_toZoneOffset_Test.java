package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimeZoneConversions_toZoneOffset_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a no‑arg options class; we use the default configuration.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /* --------------------------------------------------------------------- */
    private static ZoneOffset invokeToZoneOffset(TimeZone tz) {
        return TimeZoneConversions.toZoneOffset(tz, converter);
    }

    /**
     * Valid offsets – whole hours, hour‑minute, and custom SimpleTimeZone.
     */
    private static Stream<Arguments> validOffsetsProvider() {
        return Stream.of(// Zero offset (UTC)
        Arguments.of(TimeZone.getTimeZone("UTC"), "Z", "Zero offset – UTC should produce ZoneOffset 'Z'"), // Positive whole‑hour
        Arguments.of(TimeZone.getTimeZone("GMT+02:00"), "+02:00", "Positive whole‑hour offset – GMT+02:00"), // Negative whole‑hour
        Arguments.of(TimeZone.getTimeZone("GMT-05:00"), "-05:00", "Negative whole‑hour offset – GMT-05:00"), // Positive hour‑minute (Asia/Kolkata)
        Arguments.of(TimeZone.getTimeZone("Asia/Kolkata"), "+05:30", "Positive half‑hour offset – Asia/Kolkata"), // Positive non‑standard offset created via SimpleTimeZone (+13:45)
        Arguments.of(new SimpleTimeZone(13 * 3600 * 1000 + 45 * 60 * 1000, "Custom+13:45"), "+13:45", "Positive non‑standard offset (+13:45) via SimpleTimeZone"), // Maximum positive offset (+18:00)
        Arguments.of(new SimpleTimeZone(18 * 3600 * 1000, "Max+18"), "+18:00", "Maximum allowed positive offset (+18:00)"), // Maximum negative offset (‑18:00)
        Arguments.of(new SimpleTimeZone(-18 * 3600 * 1000, "Max-18"), "-18:00", "Maximum allowed negative offset (‑18:00)"));
    }

    /**
     * Edge offsets that appear in real‑world time zones.
     */
    private static Stream<Arguments> edgeOffsetsProvider() {
        return Stream.of(Arguments.of(TimeZone.getTimeZone("GMT+14:00"), "+14:00", "Maximum positive real‑world offset (+14:00)"), Arguments.of(TimeZone.getTimeZone("GMT-12:00"), "-12:00", "Maximum negative real‑world offset (‑12:00)"), Arguments.of(TimeZone.getTimeZone("GMT+00:45"), "+00:45", "Positive non‑hour offset (+00:45)"), Arguments.of(TimeZone.getTimeZone("GMT-00:30"), "-00:30", "Negative non‑hour offset (‑00:30)"), // America/New_York raw offset is -5 h (DST ignored)
        Arguments.of(TimeZone.getTimeZone("America/New_York"), "-05:00", "Region with DST – raw offset -5 h should be '-05:00'"));
    }

    /**
     * Sub‑second offsets (exactly one second positive / negative).
     */
    private static Stream<Arguments> subSecondOffsetsProvider() {
        return Stream.of(Arguments.of(new SimpleTimeZone(1_000, "PlusOneSecond"), "+00:00:01", "Offset of exactly one second (+00:00:01)"), Arguments.of(new SimpleTimeZone(-1_000, "MinusOneSecond"), "-00:00:01", "Offset of exactly minus one second (-00:00:01)"));
    }

    /**
     * Invalid inputs that must raise an exception.
     */
    private static Stream<Arguments> invalidOffsetsProvider() {
        return Stream.of(// Offset beyond +18 h – should trigger DateTimeException
        Arguments.of(new SimpleTimeZone(19 * 3600 * 1000, "Invalid+19"), java.time.DateTimeException.class, "Offset beyond +18 h is illegal"), // Null source object – NullPointerException during cast
        Arguments.of(null, NullPointerException.class, "Null source object"), // Object not a TimeZone – ClassCastException
        Arguments.of(new Object(), ClassCastException.class, "Non‑TimeZone source object"));
    }

    /**
     * Invalid sub‑second offsets that exceed the ±18 h limit.
     */
    private static Stream<Arguments> invalidSubSecondProvider() {
        return Stream.of(Arguments.of(new SimpleTimeZone((18 * 3600 + 1) * 1_000, "Invalid+18:00:01"), java.time.DateTimeException.class, "Offset +18:00:01 exceeds allowed range"), Arguments.of(new SimpleTimeZone(-(18 * 3600 + 1) * 1_000, "Invalid-18:00:01"), java.time.DateTimeException.class, "Offset -18:00:01 exceeds allowed range"));
    }

    /**
     * Fallback handling for unknown zone IDs – should resolve to GMT (zero offset).
     */
    private static Stream<Arguments> fallbackIdProvider() {
        return Stream.of(Arguments.of(TimeZone.getTimeZone("Foo/Bar"), "Z", "Unknown zone ID falls back to GMT (zero offset)"));
    }

    /**
     * Non‑null converter handling – the method should ignore the converter.
     */
    private static Stream<Arguments> nonNullConverterProvider() {
        // Anonymous Converter that does nothing; we only need a non‑null instance.
        Converter dummy = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                return null;
            }
        };
        return Stream.of(Arguments.of(TimeZone.getTimeZone("UTC"), dummy, "Z", "Non‑null Converter supplied – still returns zero offset"));
    }

    @Test
    @DisplayName("toZoneOffset returns UTC for GMT (zero offset)")
    void testZeroOffset() {
        // given
        TimeZone tz = TimeZone.getTimeZone("GMT");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.UTC, result, "Expected UTC ZoneOffset for GMT time zone");
    }

    @Test
    @DisplayName("toZoneOffset correctly converts positive hour offset")
    void testPositiveHourOffset() {
        // given
        // +2 hours
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.ofHours(2), result, "Expected +02:00 ZoneOffset");
    }

    @Test
    @DisplayName("toZoneOffset correctly converts negative hour offset")
    void testNegativeHourOffset() {
        // given
        // -5 hours
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.ofHours(-5), result, "Expected -05:00 ZoneOffset");
    }

    @Test
    @DisplayName("toZoneOffset correctly converts positive hour‑minute offset")
    void testPositiveHourMinuteOffset() {
        // given
        // +5 hours 30 minutes
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), result, "Expected +05:30 ZoneOffset");
    }

    @Test
    @DisplayName("toZoneOffset correctly converts negative hour‑minute offset")
    void testNegativeHourMinuteOffset() {
        // given
        // -3 hours 45 minutes
        TimeZone tz = TimeZone.getTimeZone("GMT-03:45");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.ofHoursMinutes(-3, -45), result, "Expected -03:45 ZoneOffset");
    }

    @Test
    @DisplayName("toZoneOffset works with custom TimeZone created via ID")
    void testCustomTimeZoneId() {
        // given: a custom ID that resolves to the same offset as GMT+01:00
        TimeZone tz = TimeZone.getTimeZone("GMT+01:00");
        // when
        ZoneOffset result = invokeToZoneOffset(tz);
        // then
        assertEquals(ZoneOffset.ofHours(1), result, "Expected +01:00 ZoneOffset for custom ID");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("validOffsetsProvider")
    void testValidOffsets(TimeZone tz, String expected, String description) {
        ZoneOffset result = invokeToZoneOffset(tz);
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("edgeOffsetsProvider")
    void testEdgeOffsets(TimeZone tz, String expected, String description) {
        ZoneOffset result = invokeToZoneOffset(tz);
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("subSecondOffsetsProvider")
    void testSubSecondOffsets(TimeZone tz, String expected, String description) {
        ZoneOffset result = invokeToZoneOffset(tz);
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidOffsetsProvider")
    void testInvalidOffsets(Object from, Class<? extends Throwable> expectedException, String description) {
        // The focal method expects Object, so we can pass any type here.
        assertThrows(expectedException, () -> invokeToZoneOffset(from, null), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidSubSecondProvider")
    void testInvalidSubSecondOffsets(TimeZone tz, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> invokeToZoneOffset(tz, null), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("fallbackIdProvider")
    void testFallbackId(TimeZone tz, String expected, String description) {
        ZoneOffset result = invokeToZoneOffset(tz);
        assertEquals(expected, result.toString(), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nonNullConverterProvider")
    void testNonNullConverter(TimeZone tz, Converter customConverter, String expected, String description) {
        ZoneOffset result = invokeToZoneOffset(tz, customConverter);
        assertEquals(expected, result.toString(), description);
    }
}
