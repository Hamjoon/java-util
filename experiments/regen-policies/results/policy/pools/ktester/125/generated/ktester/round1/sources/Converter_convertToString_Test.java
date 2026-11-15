package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToString_Test {

    /* --------------------------------------------------------------------- *
     *  Basic types (String, Integer, Boolean, null) – parameterized
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> basicTypesProvider() {
        return Stream.of(Arguments.of("simple string", "simple string"), Arguments.of(123, "123"), Arguments.of(true, "true"), Arguments.of(null, null));
    }

    /* --------------------------------------------------------------------- *
     *  Numeric edge cases
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> numericEdgeProvider() {
        return Stream.of(Arguments.of(0, "0"), Arguments.of(-123, "-123"), Arguments.of(1234567890123456789L, "1234567890123456789"), Arguments.of(0.0, "0.0"), Arguments.of(Double.NaN, "NaN"), Arguments.of(Double.POSITIVE_INFINITY, "Infinity"), Arguments.of(Double.NEGATIVE_INFINITY, "-Infinity"), Arguments.of(new BigDecimal("12345.6789"), "12345.6789"), Arguments.of(new BigInteger("98765432109876543210"), "98765432109876543210"));
    }

    /* --------------------------------------------------------------------- *
     *  Date / Time types
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> dateTimeProvider() {
        return Stream.of(Arguments.of(Instant.ofEpochMilli(0), "1970-01-01T00:00:00Z"), Arguments.of(LocalDate.of(2023, 1, 1), "2023-01-01"), Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56), "2023-01-01T12:34:56"), Arguments.of(ZonedDateTime.parse("2023-01-01T12:34:56+01:00[Europe/Paris]"), "2023-01-01T12:34:56+01:00[Europe/Paris]"), // java.util.Date – we use UTC to make the string deterministic
        Arguments.of(Date.from(Instant.EPOCH), "Thu Jan 01 00:00:00 UTC 1970"));
    }

    /* --------------------------------------------------------------------- *
     *  Collection / Map / Array conversions
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> collectionProvider() {
        return Stream.of(// List – deterministic order
        Arguments.of(List.of(1, 2, 3), "[1, 2, 3]"), // Set – order not guaranteed, we compare as a Set after stripping brackets
        Arguments.of(Set.of("a", "b"), "[a, b]"), // Map – deterministic order for Map.of
        Arguments.of(Map.of("key", "value"), "{key=value}"), // int[] – uses Arrays.toString
        Arguments.of(new int[] { 4, 5, 6 }, "[4, 5, 6]"));
    }

    /* --------------------------------------------------------------------- *
     *  Atomic and wrapper types (including false Boolean, Byte, Short)
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> atomicAndWrapperProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), "true"), Arguments.of(new AtomicInteger(42), "42"), Arguments.of(new AtomicLong(9_999_999_999L), "9999999999"), Arguments.of(Boolean.FALSE, "false"), Arguments.of(Byte.valueOf((byte) 7), "7"), Arguments.of(Short.valueOf((short) -12), "-12"));
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported reference types – expect IllegalArgumentException
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> unsupportedReferenceProvider() {
        return Stream.of(Arguments.of(new AtomicReference<>("test")), Arguments.of(Duration.ofHours(5)), Arguments.of(new File("/tmp/test.txt")), Arguments.of(Map.entry("k", "v")), Arguments.of(Class.forName("java.lang.String")));
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported primitive array types – expect IllegalArgumentException
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> unsupportedArrayProvider() {
        return Stream.of(Arguments.of(new boolean[] { true, false }), Arguments.of(new char[] { 'a', 'b', 'c' }));
    }

    /* --------------------------------------------------------------------- *
     *  Basic null handling
     * --------------------------------------------------------------------- */
    @Test
    void convertNullReturnsNullString() {
        // given
        Object input = null;
        // when
        String result = Converter.convertToString(input);
        // then
        assertNull(result, "Converting null should return null");
    }

    /* --------------------------------------------------------------------- *
     *  Primitive and wrapper types
     * --------------------------------------------------------------------- */
    @Test
    void convertIntegerToString() {
        Integer input = 12345;
        String result = Converter.convertToString(input);
        assertEquals("12345", result);
    }

    @Test
    void convertBooleanToString() {
        Boolean input = Boolean.TRUE;
        String result = Converter.convertToString(input);
        assertEquals("true", result);
    }

    @Test
    void convertCharToString() {
        Character input = 'Z';
        String result = Converter.convertToString(input);
        assertEquals("Z", result);
    }

    /* --------------------------------------------------------------------- *
     *  Number subclasses
     * --------------------------------------------------------------------- */
    @Test
    void convertBigDecimalToString() {
        BigDecimal input = new BigDecimal("12345.6789");
        String result = Converter.convertToString(input);
        assertEquals("12345.6789", result);
    }

    @Test
    void convertBigIntegerToString() {
        BigInteger input = new BigInteger("9876543210");
        String result = Converter.convertToString(input);
        assertEquals("9876543210", result);
    }

    /* --------------------------------------------------------------------- *
     *  Date / Time types
     * --------------------------------------------------------------------- */
    @Test
    void convertLocalDateToString() {
        LocalDate date = LocalDate.of(2023, Month.APRIL, 5);
        String result = Converter.convertToString(date);
        // The exact format is implementation‑dependent; we only verify that it contains the date parts
        assertTrue(result.contains("2023"));
        assertTrue(result.contains("04"));
        assertTrue(result.contains("05"));
    }

    @Test
    void convertLocalDateTimeToString() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 4, 5, 14, 30, 15, 123_000_000);
        String result = Converter.convertToString(dateTime);
        assertTrue(result.contains("2023"));
        assertTrue(result.contains("04"));
        assertTrue(result.contains("05"));
        assertTrue(result.contains("14"));
        assertTrue(result.contains("30"));
        assertTrue(result.contains("15"));
    }

    @Test
    void convertInstantToString() {
        Instant instant = Instant.parse("2023-04-05T14:30:15.123Z");
        String result = Converter.convertToString(instant);
        assertEquals("2023-04-05T14:30:15.123Z", result);
    }

    @Test
    void convertDateToString() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.APRIL, 5, 14, 30, 15);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        String result = Converter.convertToString(date);
        // The default string representation of Date includes the locale‑specific format;
        // we assert that the epoch milliseconds are preserved when parsed back.
        long epochFromResult = Date.parse(result);
        assertEquals(date.getTime(), epochFromResult);
    }

    /* --------------------------------------------------------------------- *
     *  Collection / Map conversions
     * --------------------------------------------------------------------- */
    @Test
    void convertListOfIntegersToString() {
        List<Integer> list = List.of(1, 2, 3);
        String result = Converter.convertToString(list);
        // Expected format: "[1, 2, 3]" (standard List.toString())
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    void convertMapToString() {
        Map<String, Object> map = Map.of("name", "Alice", "age", 30, "active", true);
        String result = Converter.convertToString(map);
        // The map's toString representation is deterministic in insertion order for Map.of
        assertEquals("{name=Alice, age=30, active=true}", result);
    }

    /* --------------------------------------------------------------------- *
     *  ByteBuffer / CharBuffer
     * --------------------------------------------------------------------- */
    @Test
    void convertByteBufferToString() {
        byte[] bytes = "hello".getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        String result = Converter.convertToString(buffer);
        assertEquals("hello", result);
    }

    @Test
    void convertCharBufferToString() {
        CharBuffer buffer = CharBuffer.wrap("world");
        String result = Converter.convertToString(buffer);
        assertEquals("world", result);
    }

    /* --------------------------------------------------------------------- *
     *  UUID conversion
     * --------------------------------------------------------------------- */
    @Test
    void convertUuidToString() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String result = Converter.convertToString(uuid);
        assertEquals(uuid.toString(), result);
    }

    @Test
    void convertEnumToString() {
        Sample e = Sample.TWO;
        String result = Converter.convertToString(e);
        assertEquals("TWO", result);
    }

    @ParameterizedTest(name = "[{index}] convertToString({0}) => {1}")
    @MethodSource("basicTypesProvider")
    void convertBasicTypes(Object input, String expected) {
        String result = Converter.convertToString(input);
        assertEquals(expected, result, "Unexpected conversion result");
    }

    @ParameterizedTest(name = "[{index}] convertToString({0}) => {1}")
    @MethodSource("numericEdgeProvider")
    void convertNumericEdgeCases(Object input, String expected) {
        String result = Converter.convertToString(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convertToString({0})")
    @MethodSource("dateTimeProvider")
    void convertDateTimeTypes(Object input, String expected) {
        String result = Converter.convertToString(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convertToString({0})")
    @MethodSource("collectionProvider")
    void convertCollectionsAndArrays(Object input, String expected) {
        String result = Converter.convertToString(input);
        if (input instanceof Set) {
            // Strip surrounding brackets and split – order may differ
            String stripped = result.substring(1, result.length() - 1);
            Set<String> actual = new HashSet<>(Arrays.asList(stripped.split(", ")));
            Set<String> expectedSet = new HashSet<>(Arrays.asList(expected.substring(1, expected.length() - 1).split(", ")));
            assertEquals(expectedSet, actual);
        } else {
            assertEquals(expected, result);
        }
    }

    @Test
    void convertObjectWithCustomToString() {
        Object custom = new Object() {

            @Override
            public String toString() {
                return "customObject";
            }
        };
        assertEquals("customObject", Converter.convertToString(custom));
    }

    @Test
    void convertEmptyString() {
        assertEquals("", Converter.convertToString(""));
    }

    @Test
    void convertStringWithWhitespaceAndEscapes() {
        String src = " line\nbreak\t tab";
        assertEquals(src, Converter.convertToString(src));
    }

    /* --------------------------------------------------------------------- *
     *  Buffer types (already covered) – malformed ByteBuffer
     * --------------------------------------------------------------------- */
    @Test
    void convertMalformedByteBufferThrows() {
        // 0xC3 0x28 is an illegal UTF‑8 sequence
        ByteBuffer malformed = ByteBuffer.wrap(new byte[] { (byte) 0xC3, (byte) 0x28 });
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(malformed), "Expected IllegalArgumentException for malformed UTF‑8 ByteBuffer");
    }

    @ParameterizedTest(name = "[{index}] convertToString({0}) => {1}")
    @MethodSource("atomicAndWrapperProvider")
    void convertAtomicAndWrapperTypes(Object input, String expected) {
        assertEquals(expected, Converter.convertToString(input));
    }

    @ParameterizedTest(name = "[{index}] convertToString({0}) throws IllegalArgumentException")
    @MethodSource("unsupportedReferenceProvider")
    void convertUnsupportedReferenceTypes(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(input), "Conversion of unsupported reference type should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] convertToString({0}) throws IllegalArgumentException")
    @MethodSource("unsupportedArrayProvider")
    void convertUnsupportedPrimitiveArrays(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToString(input), "Conversion of unsupported primitive array should throw IllegalArgumentException");
    }
}
