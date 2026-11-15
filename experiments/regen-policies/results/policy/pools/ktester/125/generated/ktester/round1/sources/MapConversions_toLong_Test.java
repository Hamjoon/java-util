package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Fresh {@link Converter} instance for each test (if needed).
     */
    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default constructor.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a new instance per test to avoid cross‑test state (e.g., custom conversions).
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear static reference.
        sharedConverter = null;
    }

    /**
     * Interprets the {@code expected} value from the JSON matrix.
     *
     * @param expected either a concrete value (e.g., {@link Long}) or a
     *                 {@link String} naming an exception class.
     * @return {@code Optional} containing the expected value or {@code null}
     *         if an exception is expected.
     */
    private static Object expectedValue(Object expected) {
        return expected instanceof String ? null : expected;
    }

    /**
     * Determines whether the {@code expected} entry denotes an exception.
     *
     * @param expected the JSON “expected” field.
     * @return {@code true} if the entry is a string naming an exception.
     */
    private static boolean expectsException(Object expected) {
        return expected instanceof String;
    }

    /**
     * Maps the exception name from the JSON matrix to the concrete {@link Class}
     * that should be thrown by {@link MapConversions#toLong(Object, Converter)}.
     */
    private static Class<? extends Throwable> exceptionClassFor(String name) {
        return switch(name) {
            case "IllegalArgumentException" ->
                IllegalArgumentException.class;
            case "NumberFormatException" ->
                NumberFormatException.class;
            case "ArithmeticException" ->
                ArithmeticException.class;
            // fallback – should never happen in our data
            default ->
                RuntimeException.class;
        };
    }

    static Stream<Arguments> nullAndDirectTypes() {
        return Stream.of(// null source
        Arguments.of(null, null, "When the source object is null the conversion should return null."), // already a Long
        Arguments.of(1234567890123L, 1234567890123L, "If the source object is already a Long it should be returned unchanged."), // Integer widening
        Arguments.of(42, 42L, "An Integer should be converted to a Long via numeric widening."), // Double truncation
        Arguments.of(3.14, 3L, "A Double should be truncated to its long integral part."), // Boolean unsupported
        Arguments.of(true, "IllegalArgumentException", "Boolean values are not supported for Long conversion and should cause an exception."));
    }

    static Stream<Arguments> stringParsing() {
        return Stream.of(Arguments.of("9876543210", 9876543210L, "A numeric string should be parsed into a Long."), Arguments.of("   12345   ", 12345L, "String with surrounding whitespace should be trimmed and parsed."), Arguments.of("12.34", "NumberFormatException", "A non‑integral numeric string cannot be parsed to Long and must raise an exception."), Arguments.of("notANumber", "NumberFormatException", "A completely non‑numeric string should cause a NumberFormatException."), // Additional edge cases
        Arguments.of("-12345", -12345L, "A numeric string with a leading minus sign should be parsed to a negative Long."), Arguments.of("+6789", 6789L, "A numeric string with an explicit plus sign should be parsed correctly."), Arguments.of("0xFF", "NumberFormatException", "Hexadecimal notation is not supported; parsing should fail with NumberFormatException."), Arguments.of("   ", "NumberFormatException", "A string containing only whitespace cannot be parsed to a Long."), Arguments.of("", "NumberFormatException", "An empty string should cause a NumberFormatException."), Arguments.of("1e3", "NumberFormatException", "Scientific notation is not supported for Long parsing and should result in NumberFormatException."), Arguments.of("+", "NumberFormatException", "A string containing only a plus sign cannot be parsed to a Long and must raise NumberFormatException."), Arguments.of("1_000", "NumberFormatException", "Underscore separators are not allowed in numeric strings for this conversion; parsing should fail with NumberFormatException."));
    }

    static Stream<Arguments> mapKeyExtraction() {
        Map<String, Object> mapValue = new HashMap<>();
        mapValue.put("value", 5555);
        Map<String, Object> mapV = new HashMap<>();
        mapV.put("_v", "7777");
        Map<String, Object> mapEpoch = new HashMap<>();
        mapEpoch.put("epochMillis", 1_609_459_200_000L);
        Map<String, Object> mapUuid = new HashMap<>();
        mapUuid.put("mostSigBits", 0L);
        mapUuid.put("leastSigBits", 12_345L);
        Map<String, Object> mapUnknown = new HashMap<>();
        mapUnknown.put("unknownKey", 999);
        return Stream.of(Arguments.of(mapValue, 5555L, "Map containing the key \"value\" with a numeric value should be extracted and converted."), Arguments.of(mapV, 7777L, "Map containing the alternate key \"_v\" with a numeric string should be extracted and parsed."), Arguments.of(mapEpoch, 1_609_459_200_000L, "Map with the key \"epochMillis\" should be treated as a timestamp in milliseconds."), Arguments.of(mapUuid, 12_345L, "When both MOST_SIG_BITS and LEAST_SIG_BITS are present, the method should combine them into a UUID‑derived long (here simplified to the least‑sig bits for test purposes)."), Arguments.of(mapUnknown, "IllegalArgumentException", "A map that does not contain any recognised key should cause an exception."));
    }

    static Stream<Arguments> dateAndTimeTypes() {
        // Real objects (the library also accepts map representations, but objects are simpler)
        Date date = new Date(1_622_505_600_000L);
        Instant instant = Instant.ofEpochSecond(1_622_505_600L);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_622_505_600_000L);
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 0, 0, 0, 0);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
        return Stream.of(Arguments.of(date, 1_622_505_600_000L, "A java.util.Date should be converted to its epoch‑millis representation."), Arguments.of(instant, 1_622_505_600_000L, "An Instant should be converted to milliseconds since the epoch."), Arguments.of(calendar, 1_622_505_600_000L, "A Calendar instance should be converted to its time‑in‑millis value."), Arguments.of(zdt, 1_622_505_600_000L, "A LocalDateTime (with an explicit zone) should be converted to epoch millis."));
    }

    static Stream<Arguments> atomicAndBigTypes() {
        AtomicLong atomicLong = new AtomicLong(8_888L);
        // fractional part will be truncated
        BigDecimal bigDecimal = new BigDecimal("12_345.67");
        BigInteger bigInteger = new BigInteger("999999999999999999");
        return Stream.of(Arguments.of(atomicLong, 8_888L, "AtomicLong should expose its current long value."), Arguments.of(bigDecimal, 12345L, "BigDecimal should be converted via its longValue() (truncating the fractional part)."), Arguments.of(bigInteger, 999_999_999_999_999_999L, "BigInteger should be converted directly to a Long when it fits."));
    }

    static Stream<Arguments> errorAndEdgeCases() {
        Map<String, Object> emptyMap = new HashMap<>();
        Map<String, Object> mapMultiple = new HashMap<>();
        mapMultiple.put("value", 1);
        mapMultiple.put("epochMillis", 2);
        Map<String, Object> mapOverflow = new HashMap<>();
        // > Long.MAX_VALUE
        mapOverflow.put("value", "9223372036854775808");
        return Stream.of(Arguments.of(emptyMap, "IllegalArgumentException", "An empty map provides no data for conversion and must raise an exception."), Arguments.of(mapMultiple, 1L, "When multiple recognised keys exist, the method should prioritize the first key in its internal order (here assumed to be \"value\")."), Arguments.of(mapOverflow, "ArithmeticException", "A numeric string exceeding Long.MAX_VALUE should cause an overflow exception."));
    }

    static Stream<Arguments> additionalNumericTypes() {
        Short shortVal = 32767;
        Byte byteVal = 127;
        Float floatVal = 2.9f;
        AtomicInteger atomicInt = new AtomicInteger(77);
        AtomicBoolean atomicBool = new AtomicBoolean(true);
        return Stream.of(Arguments.of(shortVal, 32767L, "A Short value should be widened to Long."), Arguments.of(byteVal, 127L, "A Byte value should be widened to Long."), Arguments.of(floatVal, 2L, "A Float (or Double) with a fractional part should be truncated to its integral Long part."), Arguments.of(atomicInt, 77L, "AtomicInteger should expose its current int value as a Long."), Arguments.of(atomicBool, "IllegalArgumentException", "AtomicBoolean is not a supported source type for Long conversion and must raise an exception."));
    }

    static Stream<Arguments> mapEdgeCases() {
        Map<String, Object> epochString = new HashMap<>();
        epochString.put("epochMillis", "1622505600000");
        Map<String, Object> epochSecNano = new HashMap<>();
        epochSecNano.put("epochSecond", 1_622_505_600L);
        epochSecNano.put("nano", 5_000_000);
        Map<String, Object> valueNull = new HashMap<>();
        valueNull.put("value", null);
        Map<String, Object> valueBoolean = new HashMap<>();
        valueBoolean.put("value", true);
        Map<String, Object> unknownClass = new HashMap<>();
        unknownClass.put("class", "java.lang.Unknown");
        unknownClass.put("value", 123);
        Map<String, Object> overflowString = new HashMap<>();
        overflowString.put("value", "9223372036854775808");
        return Stream.of(Arguments.of(epochString, 1_622_505_600_000L, "Map with epochMillis as a numeric string should be parsed to Long."), Arguments.of(epochSecNano, 1_622_505_600_005L, "Map with epochSecond and nano should be converted to epoch milliseconds (seconds * 1000 + nano/1_000_000)."), Arguments.of(valueNull, "IllegalArgumentException", "A map containing a recognized key with a null value must raise an exception."), Arguments.of(valueBoolean, "IllegalArgumentException", "A map where the 'value' key holds a Boolean is unsupported and should cause an exception."), Arguments.of(unknownClass, "IllegalArgumentException", "A map specifying an unknown class should be rejected."), Arguments.of(overflowString, "ArithmeticException", "String representation of a number larger than Long.MAX_VALUE must cause overflow exception."));
    }

    static Stream<Arguments> overflowCases() {
        Map<String, Object> bigIntMap = new HashMap<>();
        bigIntMap.put("class", "java.math.BigInteger");
        bigIntMap.put("value", "9223372036854775808");
        Map<String, Object> bigDecMap = new HashMap<>();
        bigDecMap.put("class", "java.math.BigDecimal");
        bigDecMap.put("value", "9223372036854775808.99");
        return Stream.of(Arguments.of(bigIntMap, "ArithmeticException", "BigInteger exceeding Long.MAX_VALUE should raise ArithmeticException."), Arguments.of(bigDecMap, "ArithmeticException", "BigDecimal exceeding Long.MAX_VALUE should raise ArithmeticException, even though fractional part is present."));
    }

    static Stream<Arguments> unsupportedTypes() throws Exception {
        UUID uuid = new UUID(0L, 1L);
        URL url = new URL("http://example.com");
        Locale locale = new Locale("en", "US");
        Duration duration = Duration.ofSeconds(60);
        return Stream.of(Arguments.of(uuid, 1L, "A UUID object (or map mimicking it) should be converted to its least‑significant bits (here 1)."), Arguments.of(url, "IllegalArgumentException", "URL is not a convertible source for Long and must raise an exception."), Arguments.of(locale, "IllegalArgumentException", "Locale objects cannot be converted to Long."), Arguments.of(duration, "IllegalArgumentException", "Duration is not directly convertible to Long by this method and should cause an exception."));
    }

    @Test
    void toLong_ShouldConvertSimpleNumber() {
        // given
        Long expected = 123L;
        // when
        Long actual = MapConversions.toLong(expected, converter);
        // then
        assertEquals(expected, actual, "toLong should return the same Long instance when the source is already a Long");
    }

    @Test
    void toLong_ShouldConvertStringRepresentingNumber() {
        // given
        String source = "456";
        Long expected = 456L;
        // when
        Long actual = MapConversions.toLong(source, converter);
        // then
        assertEquals(expected, actual, "String containing a numeric value should be converted to Long");
    }

    @Test
    void toLong_ShouldExtractValueFromMapUsingDefaultKey() {
        // given
        // The default key used by MapConversions for simple values is "_v"
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        // "_v" -> "789"
        map.put(MapConversions.V, "789");
        Long expected = 789L;
        // when
        Long actual = MapConversions.toLong(map, converter);
        // then
        assertEquals(expected, actual, "Map with key \"_v\" should be converted to Long");
    }

    @Test
    void toLong_ShouldExtractValueFromMapUsingExplicitValueKey() {
        // given
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(MapConversions.VALUE, 321L);
        Long expected = 321L;
        // when
        Long actual = MapConversions.toLong(map, converter);
        // then
        assertEquals(expected, actual, "Map with key \"value\" should be converted to Long");
    }

    @Test
    void toLong_ShouldReturnNullWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        Long actual = MapConversions.toLong(source, converter);
        // then
        assertNull(actual, "Converting a null source should return null for object target types");
    }

    @Test
    void toLong_ShouldHandleAtomicLongSource() {
        // given
        java.util.concurrent.atomic.AtomicLong atomic = new java.util.concurrent.atomic.AtomicLong(555L);
        Long expected = 555L;
        // when
        Long actual = MapConversions.toLong(atomic, converter);
        // then
        assertEquals(expected, actual, "AtomicLong should be converted to its long value");
    }

    @Test
    void toLong_ShouldConvertDateToEpochMillis() {
        // given
        // specific epoch millis
        java.util.Date date = new java.util.Date(1_600_000_000_000L);
        Long expected = date.getTime();
        // when
        Long actual = MapConversions.toLong(date, converter);
        // then
        assertEquals(expected, actual, "Date should be converted to its epoch milliseconds");
    }

    @Test
    void toLong_ShouldConvertCalendarToEpochMillis() {
        // given
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_600_000_123_456L);
        Long expected = cal.getTimeInMillis();
        // when
        Long actual = MapConversions.toLong(cal, converter);
        // then
        assertEquals(expected, actual, "Calendar should be converted to its epoch milliseconds");
    }

    @Test
    void toLong_ShouldConvertMapWithEpochMillisKey() {
        // given
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1_600_000_999_999L);
        Long expected = 1_600_000_999_999L;
        // when
        Long actual = MapConversions.toLong(map, converter);
        // then
        assertEquals(expected, actual, "Map containing \"epochMillis\" should be converted to Long");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullAndDirectTypes")
    void testNullAndDirectTypes(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringParsing")
    void testStringParsing(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapKeyExtraction")
    void testMapKeyExtraction(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("dateAndTimeTypes")
    void testDateAndTimeTypes(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("atomicAndBigTypes")
    void testAtomicAndBigTypes(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorAndEdgeCases")
    void testErrorAndEdgeCases(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalNumericTypes")
    void testAdditionalNumericTypes(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapEdgeCases")
    void testMapEdgeCases(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowCases")
    void testOverflowCases(Object from, Object expected, String description) {
        Class<? extends Throwable> ex = exceptionClassFor((String) expected);
        assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedTypes")
    void testUnsupportedTypes(Object from, Object expected, String description) {
        if (expectsException(expected)) {
            Class<? extends Throwable> ex = exceptionClassFor((String) expected);
            assertThrows(ex, () -> MapConversions.toLong(from, converter), description);
        } else {
            Long actual = MapConversions.toLong(from, converter);
            assertEquals(expected, actual, description);
        }
    }
}
