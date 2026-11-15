package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLong_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor gives a
        // fully‑functional converter with all built‑in conversions registered.
        converter = new Converter(new ConverterOptions());
        // {@code DefaultConverter} is the concrete implementation used by the library.
        converter = new DefaultConverter();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
        converter = null;
    }

    @Test
    void toLong_fromMapWithIntegerValue() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // Integer
        map.put("value", 123);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(123L, result);
    }

    @Test
    void toLong_fromMapWithStringValue() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // String that can be parsed as a number
        map.put("value", "456");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(456L, result);
    }

    @Test
    void toLong_fromAlreadyLong() {
        // GIVEN
        Long source = 789L;
        // WHEN
        Long result = MapConversions.toLong(source, converter);
        // THEN
        assertSame(source, result);
    }

    @Test
    void toLong_fromNullReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Long result = MapConversions.toLong(source, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toLong_fromEmptyMapReturnsNull() {
        // GIVEN
        Map<String, Object> emptyMap = new HashMap<>();
        // WHEN
        Long result = MapConversions.toLong(emptyMap, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toLong_fromNullSource_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Long result = MapConversions.toLong(source, converter);
        // THEN
        assertNull(result, "A null source should produce a null result");
    }

    @Test
    void toLong_fromMapWithLongValue_keyValue() {
        // GIVEN
        Map<String, Object> map = Map.of("value", 123L);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(123L, result);
    }

    @Test
    void toLong_fromMapWithLongValue_keyAltV() {
        // GIVEN
        Map<String, Object> map = Map.of("_v", "456");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(456L, result);
    }

    @Test
    void toLong_fromMapWithStringNumber() {
        // GIVEN
        Map<String, Object> map = Map.of("value", "789");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(789L, result);
    }

    @Test
    void toLong_fromMapWithFloatingPointNumber_truncates() {
        // GIVEN
        Map<String, Object> map = Map.of("value", 12.34);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(12L, result);
    }

    @Test
    void toLong_fromMapWithBooleanTrue_convertsToOne() {
        // GIVEN
        Map<String, Object> map = Map.of("value", true);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(1L, result);
    }

    @Test
    void toLong_fromMapWithAtomicLong_unwraps() {
        // GIVEN
        Map<String, Object> map = Map.of("value", new AtomicLong(555L));
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(555L, result);
    }

    @Test
    void toLong_fromMapWithBigInteger_withinRange() {
        // GIVEN
        Map<String, Object> map = Map.of("value", new BigInteger("999999999999999999"));
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(999999999999999999L, result);
    }

    @Test
    void toLong_fromMapWithDate_convertsToEpochMillis() {
        // GIVEN
        // 2021‑01‑01T00:00:00Z
        Date date = new Date(1609459200000L);
        Map<String, Object> map = Map.of("value", date);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(1609459200000L, result);
    }

    @Test
    void toLong_fromMapWithInstant_convertsToEpochMillis() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(10);
        Map<String, Object> map = Map.of("value", instant);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(10_000L, result);
    }

    @Test
    void toLong_fromMapWithInvalidString_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", "notANumber");
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter), "Non‑numeric strings must cause NumberFormatException");
    }

    @Test
    void toLong_fromMapWithUnknownKey_returnsNull() {
        // GIVEN
        Map<String, Object> map = Map.of("unknown", 42);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertNull(result, "Map without recognised keys should result in null");
    }

    @Test
    void toLong_fromMapWithEpochMillisKey_longValue() {
        // GIVEN
        Map<String, Object> map = Map.of("epochMillis", 1622548800000L);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(1622548800000L, result);
    }

    @Test
    void toLong_fromMapWithEpochMillisKey_dateValue() {
        // GIVEN
        Date date = new Date(1622548800000L);
        Map<String, Object> map = Map.of("epochMillis", date);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(1622548800000L, result);
    }

    @Test
    void toLong_fromMapWithNanosKey_stringValue() {
        // GIVEN
        Map<String, Object> map = Map.of("nanos", "123456789");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(123456789L, result);
    }

    @Test
    void toLong_fromMapWithBigDecimal_truncatesFraction() {
        // GIVEN
        Map<String, Object> map = Map.of("value", new BigDecimal("9876543210.99"));
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(9876543210L, result);
    }

    @Test
    void toLong_fromMapWithCalendar_returnsNull() {
        // GIVEN
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Map<String, Object> map = Map.of("value", cal);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertNull(result, "Unsupported Calendar type should yield null");
    }

    @Test
    void toLong_fromMapWithNullValue_returnsNull() {
        // GIVEN
        Map<String, Object> map = Map.of("value", null);
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toLong_fromMapWithWhitespaceString_trimsAndParses() {
        // GIVEN
        Map<String, Object> map = Map.of("value", "   42   ");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(42L, result);
    }

    @Test
    void toLong_fromMapWithBigIntegerOverflow_throwsNumberFormatException() {
        // GIVEN
        // Long.MAX_VALUE + 1
        Map<String, Object> map = Map.of("value", new BigInteger("9223372036854775808"));
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter), "BigInteger exceeding Long.MAX_VALUE must cause NumberFormatException");
    }

    @Test
    void toLong_precedence_valueOverAltV() {
        // GIVEN
        Map<String, Object> map = Map.of("value", 111L, "_v", "222");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(111L, result, "\"value\" key should take precedence over \"_v\"");
    }

    @Test
    void toLong_ignoresUnknownKeysWhenValuePresent() {
        // GIVEN
        Map<String, Object> map = Map.of("unknown", 77, "value", "333");
        // WHEN
        Long result = MapConversions.toLong(map, converter);
        // THEN
        assertEquals(333L, result);
    }

    @Test
    void toLong_nullConverter_throwsNullPointerException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", 123L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toLong(map, null), "Passing a null Converter must result in NPE");
    }

    @Test
    void toLong_nonMapSource_string_throwsClassCastException() {
        // GIVEN
        String source = "just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toLong(source, converter), "Non‑Map source should cause ClassCastException");
    }

    @Test
    void toLong_nonMapSource_list_throwsClassCastException() {
        // GIVEN
        List<Integer> source = List.of(1, 2, 3);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toLong(source, converter), "Non‑Map source (List) should cause ClassCastException");
    }

    @Test
    void toLong_emptyStringValue_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", "");
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_malformedNumericString_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", "12abc34");
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_bigDecimalOutOfRange_throwsNumberFormatException() {
        // GIVEN
        // > Long.MAX_VALUE
        Map<String, Object> map = Map.of("value", new BigDecimal("9223372036854775808"));
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_epochMillisNonNumericString_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("epochMillis", "notANumber");
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_nanValue_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", Double.NaN);
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_positiveInfinity_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", Double.POSITIVE_INFINITY);
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }

    @Test
    void toLong_negativeInfinityFloat_throwsNumberFormatException() {
        // GIVEN
        Map<String, Object> map = Map.of("value", Float.NEGATIVE_INFINITY);
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLong(map, converter));
    }
}
