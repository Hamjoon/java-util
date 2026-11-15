package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toZoneOffset_Test {

    private static Converter converter;

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

//     private static Stream<Arguments> successfulConversionProvider() {
//         return // Direct ZoneOffset instance
//         Stream.// Direct ZoneOffset instance
//         of(// String parsing – standard formats
//         Arguments.of(ZoneOffset.ofHoursMinutes(2, 30), ZoneOffset.ofHoursMinutes(2, 30)), // Number conversion – total seconds
//         Arguments.of("+02:30", ZoneOffset.ofHoursMinutes(2, 30)), // Number conversion – total seconds
//         Arguments.of("-05:00", ZoneOffset.ofHours(-5)), // Number conversion – total seconds
//         Arguments.of("Z", ZoneOffset.UTC), // Map based conversion – key 'zoneOffset' (String)
//         Arguments.of(Integer.valueOf(5400), ZoneOffset.ofHoursMinutes(1, 30)), // Map based conversion – key 'zoneOffset' (String)
//         Arguments.of(Long.valueOf(-18000L), ZoneOffset.ofHours(-5)), // Map based conversion – key 'value' (numeric total‑seconds)
//         Arguments.of(Map.of("zoneOffset", "+03:00"), ZoneOffset.ofHours(3)), // Map based conversion – key '_v' (ZoneOffset instance)
//         Arguments.of(Map.of("value", -18000), ZoneOffset.ofHours(-5)), // ZoneId that is a ZoneOffset
//         Arguments.of(Map.of("_v", ZoneOffset.ofHours(4)), ZoneOffset.ofHours(4)), // Null source handling
//         Arguments.of(ZoneId.of("UTC+02:00"), ZoneOffset.ofHours(2)), // Map with null value returns null
//         Arguments.of(null, null), // CharSequence parsing – StringBuilder
//         Arguments.of(Map.of("zoneOffset", null), null), // CharSequence parsing – CharBuffer
//         Arguments.of(new StringBuilder("+04:15"), ZoneOffset.ofHoursMinutes(4, 15)), // Numeric conversion – Short
//         Arguments.of(CharBuffer.wrap("-03:45"), ZoneOffset.ofHoursMinutes(-3, -45)), // Numeric conversion – Byte
//         Arguments.of(Short.valueOf((short) 7200), ZoneOffset.ofHours(2)), // Numeric conversion – BigInteger
//         Arguments.of(Byte.valueOf((byte) -36), ZoneOffset.ofHours(-1)), // OffsetTime conversion
//         Arguments.of(BigInteger.valueOf(19800L), ZoneOffset.ofHoursMinutes(5, 30)), // OffsetDateTime conversion
//         Arguments.of(OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 45)), ZoneOffset.ofHoursMinutes(5, 45)), // ZonedDateTime conversion (fixed offset ZoneId)
//         Arguments.of(OffsetDateTime.of(2023, 3, 15, 10, 30, 0, 0, ZoneOffset.ofHours(-2)), ZoneOffset.ofHours(-2)), // Map precedence – zoneOffset over value
//         Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC+03:00")), ZoneOffset.ofHours(3)), // Map precedence – value over _v
//         Arguments.of(mapOf("zoneOffset", "+01:00", "value", -7200), ZoneOffset.ofHours(1)), // Map with zoneOffset holding ZoneOffset instance
//         Arguments.of(mapOf("value", "+02:00", "_v", ZoneOffset.ofHours(5)), ZoneOffset.ofHours(2)), // Map with value holding String offset
//         Arguments.of(Map.of("zoneOffset", ZoneOffset.ofHoursMinutes(3, 30)), ZoneOffset.ofHoursMinutes(3, 30)), Arguments.of(Map.of("value", "-04:00"), ZoneOffset.ofHours(-4)));
//     }

//     private static Stream<Arguments> exceptionConversionProvider() {
//         return // Invalid string format
//         Stream.// Invalid string format
//         of(// Invalid map value type (String that cannot be parsed)
//         Arguments.of("invalid-offset", DateTimeException.class), // CharSequence invalid format
//         Arguments.of(Map.of("value", "not-a-zone"), DateTimeException.class), // String offset out‑of‑range
//         Arguments.of(new StringBuilder("bad-offset"), DateTimeException.class), // String offset out‑of‑range
//         Arguments.of(CharBuffer.wrap("+25:00"), DateTimeException.class), // Numeric out‑of‑range total seconds
//         Arguments.of("+19:00", DateTimeException.class), // Numeric out‑of‑range total seconds
//         Arguments.of("-18:30", DateTimeException.class), // ZoneId non‑offset – exception
//         Arguments.of(Integer.valueOf(100_000), DateTimeException.class), // ZoneId non‑offset – exception
//         Arguments.of(Long.valueOf(-100_000L), DateTimeException.class), // Map without recognized keys
//         Arguments.of(ZoneId.of("America/New_York"), DateTimeException.class), // Map with unsupported value type (collection)
//         Arguments.of(Map.of("foo", "+02:00"), IllegalArgumentException.class), // Unsupported source type
//         Arguments.of(Map.of("value", Arrays.asList("+01:00")), DateTimeException.class), Arguments.of(Boolean.TRUE, IllegalArgumentException.class));
//     }

    /**
     * Helper to build a mutable map with var‑args (key, value, ...).
     * Used for test cases where we need more than two entries.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Verify that a map containing the key {@code "_v"} with a valid offset string
     * is correctly converted to a {@link ZoneOffset}.
     */
//     @Test
//     void toZoneOffset_FromMapWithVKey() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("_v", "+02:00");
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull("Result should not be null for a valid map entry", result);
//         Assert.assertEquals(ZoneOffset.ofHours(2), result);
//     }

    /**
     * Verify that a map containing the key {@code "value"} with a valid offset string
     * is correctly converted.
     */
//     @Test
//     void toZoneOffset_FromMapWithValueKey() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", "-05:30");
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), result);
//     }

    /**
     * Verify that a map containing the key {@code "zoneOffset"} with a valid offset string
     * is correctly converted.
     */
//     @Test
//     void toZoneOffset_FromMapWithZoneOffsetKey() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("zoneOffset", "Z");
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.UTC, result);
//     }

    /**
     * Verify that a plain {@link String} source is converted directly to a {@link ZoneOffset}.
     */
//     @Test
//     void toZoneOffset_FromString() {
//         String source = "+09:15";
//         ZoneOffset result = MapConversions.toZoneOffset(source, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(9, 15), result);
//     }

    /**
     * Verify that a {@code null} source yields {@code null}.
     */
//     @Test
//     void toZoneOffset_NullSource() {
//         ZoneOffset result = MapConversions.toZoneOffset(null, converter);
//         Assert.assertNull("Null source should result in null ZoneOffset", result);
//     }

    /**
     * Verify that an unrecognised source (e.g., an Integer) results in {@code null}
     * rather than throwing an exception.
     */
//     @Test
//     void toZoneOffset_InvalidSource() {
//         Integer invalid = 12345;
//         ZoneOffset result = MapConversions.toZoneOffset(invalid, converter);
//         Assert.assertNull("Invalid source type should result in null ZoneOffset", result);
//     }

    /**
     * Verify that a map with a recognised key but an invalid offset string
     * results in {@code null}.
     */
//     @Test
//     void toZoneOffset_MapWithInvalidValue() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("_v", "not-a-zone");
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNull("Map containing an invalid offset string should return null", result);
//     }

    /**
     * Successful conversion scenarios – the expected result is either a
     * {@link ZoneOffset} instance or {@code null}.
     */
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("successfulConversionProvider")
//     void testToZoneOffset_Success(Object from, ZoneOffset expected) {
//         ZoneOffset actual = MapConversions.toZoneOffset(from, null);
//         if (expected == null) {
//             assertNull(actual, "Expected null result");
//         } else {
//             assertNotNull(actual, "Result should not be null");
//             assertEquals(expected, actual);
//         }
//     }

    /**
     * Scenarios that must throw an exception (DateTimeException,
     * IllegalArgumentException, etc.).
     */
//     @ParameterizedTest(name = "[{index}] from={0}")
//     @MethodSource("exceptionConversionProvider")
//     void testToZoneOffset_Exception(Object from, Class<? extends Throwable> expectedException) {
//         assertThrows(expectedException, () -> MapConversions.toZoneOffset(from, null), "Expected exception: " + expectedException.getSimpleName());
//     }

//     @Test
//     public void toZoneOffset_CharSequence_StringBuilder() {
//         StringBuilder sb = new StringBuilder("+04:15");
//         ZoneOffset result = MapConversions.toZoneOffset(sb, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(4, 15), result);
//     }

//     @Test
//     public void toZoneOffset_CharSequence_CharBuffer() {
//         CharBuffer cb = CharBuffer.wrap("-03:45");
//         ZoneOffset result = MapConversions.toZoneOffset(cb, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(-3, -45), result);
//     }

//     @Test
//     public void toZoneOffset_Numeric_Short() {
//         Short value = Short.valueOf((short) 7200);
//         ZoneOffset result = MapConversions.toZoneOffset(value, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(2), result);
//     }

//     @Test
//     public void toZoneOffset_Numeric_Byte() {
//         Byte value = Byte.valueOf((byte) -36);
//         ZoneOffset result = MapConversions.toZoneOffset(value, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(-1), result);
//     }

//     @Test
//     public void toZoneOffset_Numeric_BigInteger() {
//         BigInteger value = BigInteger.valueOf(19800L);
//         ZoneOffset result = MapConversions.toZoneOffset(value, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(5, 30), result);
//     }

//     @Test
//     public void toZoneOffset_OffsetTime() {
//         OffsetTime ot = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 45));
//         ZoneOffset result = MapConversions.toZoneOffset(ot, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(5, 45), result);
//     }

//     @Test
//     public void toZoneOffset_OffsetDateTime() {
//         OffsetDateTime odt = OffsetDateTime.of(2023, 3, 15, 10, 30, 0, 0, ZoneOffset.ofHours(-2));
//         ZoneOffset result = MapConversions.toZoneOffset(odt, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(-2), result);
//     }

//     @Test
//     public void toZoneOffset_ZonedDateTime_FixedOffset() {
//         ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC+03:00"));
//         ZoneOffset result = MapConversions.toZoneOffset(zdt, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(3), result);
//     }

//     @Test
//     public void toZoneOffset_MapPrecedence_ZoneOffsetOverValue() {
//         Map<String, Object> map = mapOf("zoneOffset", "+01:00", "value", -7200);
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(1), result);
//     }

//     @Test
//     public void toZoneOffset_MapPrecedence_ValueOverV() {
//         Map<String, Object> map = mapOf("value", "+02:00", "_v", ZoneOffset.ofHours(5));
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(2), result);
//     }

//     @Test
//     public void toZoneOffset_MapWithZoneOffsetHoldingInstance() {
//         Map<String, Object> map = mapOf("zoneOffset", ZoneOffset.ofHoursMinutes(3, 30));
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHoursMinutes(3, 30), result);
//     }

//     @Test
//     public void toZoneOffset_MapWithValueHoldingString() {
//         Map<String, Object> map = mapOf("value", "-04:00");
//         ZoneOffset result = MapConversions.toZoneOffset(map, converter);
//         Assert.assertNotNull(result);
//         Assert.assertEquals(ZoneOffset.ofHours(-4), result);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_invalidString() {
//         MapConversions.toZoneOffset("invalid-offset", converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_mapValueStringNotParseable() {
//         MapConversions.toZoneOffset(mapOf("value", "not-a-zone"), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_charSequenceBadOffset() {
//         MapConversions.toZoneOffset(new StringBuilder("bad-offset"), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_charBufferOutOfRange() {
//         MapConversions.toZoneOffset(CharBuffer.wrap("+25:00"), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_stringOffsetOutOfRange_Positive() {
//         MapConversions.toZoneOffset("+19:00", converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_stringOffsetOutOfRange_Negative() {
//         MapConversions.toZoneOffset("-18:30", converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_numericOutOfRange_Integer() {
//         MapConversions.toZoneOffset(Integer.valueOf(100_000), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_numericOutOfRange_Long() {
//         MapConversions.toZoneOffset(Long.valueOf(-100_000L), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_zoneIdNotOffset() {
//         MapConversions.toZoneOffset(ZoneId.of("America/New_York"), converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void exception_mapMissingKeys() {
//         MapConversions.toZoneOffset(mapOf("foo", "+02:00"), converter);
//     }

//     @Test(expected = DateTimeException.class)
//     public void exception_mapUnsupportedValueType() {
//         MapConversions.toZoneOffset(mapOf("value", Arrays.asList("+01:00")), converter);
//     }

//     @Test(expected = IllegalArgumentException.class)
//     public void exception_unsupportedSourceType() {
//         MapConversions.toZoneOffset(Boolean.TRUE, converter);
//     }
}