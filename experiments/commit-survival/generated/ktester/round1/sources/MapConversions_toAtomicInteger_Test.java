package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If ConverterOptions has no public no‑arg constructor in the real library,
        // replace this with an appropriate way of obtaining a default options object.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – helps the GC and makes the test state explicit.
        converter = null;
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(7), 7), // truncates to int
        Arguments.of(Long.valueOf(8L), 8), // whole number double
        Arguments.of(Double.valueOf(9.0), 9));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("42", 42), Arguments.of("   13   ", 13), Arguments.of("+15", 15), Arguments.of("-8", -8), Arguments.of("0", 0));
    }

    private static Stream<Arguments> mapWithKnownKeysProvider() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("value", 10);
        Map<String, Object> map2 = new HashMap<>();
        // alternative key with String
        map2.put(MapConversions.V, "20");
        CompactLinkedMap<String, Object> compact = new CompactLinkedMap<>();
        compact.put("value", 30);
        compact.put("extra", "ignored");
        return Stream.of(Arguments.of(map1, 10), Arguments.of(map2, 20), Arguments.of(compact, 30));
    }

    private static Stream<Arguments> edgeNumericProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE), // Long value exceeds int range – lower 32 bits are kept (overflow behavior)
        Arguments.of(Long.valueOf(((long) Integer.MAX_VALUE) + 1L), (int) ((long) Integer.MAX_VALUE + 1L)));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(new Object()), Arguments.of(Arrays.asList(1, 2, 3)), Arguments.of(new Date()));
    }

    private static Stream<Arguments> malformedMapProvider() {
        Map<String, Object> nullValueMap = new HashMap<>();
        nullValueMap.put("value", null);
        Map<String, Object> emptyStringMap = new HashMap<>();
        emptyStringMap.put("_v", "");
        CompactLinkedMap<String, Object> badCompact = new CompactLinkedMap<>();
        badCompact.put("value", "abc");
        return Stream.of(Arguments.of(nullValueMap), Arguments.of(emptyStringMap), Arguments.of(badCompact));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123"), 123), // fractional part truncated
        Arguments.of(new BigDecimal("123.456"), 123));
    }

    @Test
    void testToAtomicIntegerFromMapWithValueKey() {
        // GIVEN a map that contains the "value" key with an Integer
        Map<String, Object> source = new HashMap<>();
        source.put("value", 42);
        // WHEN converting the map to an AtomicInteger
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // THEN the result is a non‑null AtomicInteger with the expected value
        assertNotNull(result, "Result should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the mapped value");
    }

    @Test
    void testToAtomicIntegerFromMapWithAlternativeKey() {
        // GIVEN a map that contains the "_v" key (alternative key used by MapConversions)
        Map<String, Object> source = new HashMap<>();
        // MapConversions.V == "_v"
        source.put(MapConversions.V, 7);
        // WHEN converting the map to an AtomicInteger
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // THEN the result reflects the value from the alternative key
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void testToAtomicIntegerFromNull() {
        // GIVEN a null source
        // WHEN converting
        AtomicInteger result = MapConversions.toAtomicInteger(null, converter);
        // THEN the conversion returns null (consistent with Converter contract)
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void testToAtomicIntegerFromDirectNumber() {
        // GIVEN a plain Number (not a map)
        Integer source = 123;
        // WHEN converting
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // THEN the result should contain the numeric value
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void testToAtomicIntegerFromString() {
        // GIVEN a String that can be parsed as an integer
        String source = "256";
        // WHEN converting
        AtomicInteger result = MapConversions.toAtomicInteger(source, converter);
        // THEN the result should contain the parsed integer value
        assertNotNull(result);
        assertEquals(256, result.get());
    }

    @Test
    void testDirectAtomicIntegerIsReturnedUnchanged() {
        AtomicInteger src = new AtomicInteger(5);
        AtomicInteger result = MapConversions.toAtomicInteger(src, converter);
        assertNotNull(result);
        // The conversion should return the same instance (no new object creation).
        assertSame(src, result, "AtomicInteger should be returned unchanged");
        assertEquals(5, result.get());
    }

    @ParameterizedTest(name = "{index}: from={0} → {1}")
    @MethodSource("numericWrapperProvider")
    void testNumericWrappers(Object from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("stringParsingProvider")
    void testStringParsing(String from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{index}: map={0} → {1}")
    @MethodSource("mapWithKnownKeysProvider")
    void testMapWithKnownKeys(Map<String, Object> map, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void testMapPrefersAlternativeKeyWhenBothPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 55);
        // should be ignored because "_v" is checked first
        map.put("value", 99);
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertNotNull(result);
        assertEquals(55, result.get());
    }

    @Test
    void testMapWithUnrecognizedKeyThrows() {
        // not a recognized key
        Map<String, Object> map = Map.of("v", 77);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(map, converter), "Unrecognized map key should cause IllegalArgumentException");
    }

    @Test
    void testNullSourceReturnsNull() {
        assertNull(MapConversions.toAtomicInteger(null, converter), "Converting a null source should return null");
    }

    @Test
    void testEmptyMapThrows() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(Collections.emptyMap(), converter), "Empty map should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: from={0} → {1}")
    @MethodSource("edgeNumericProvider")
    void testEdgeNumericValues(Object from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void testNaNThrows() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(Double.NaN, converter), "NaN cannot be converted to an AtomicInteger");
    }

    @Test
    void testInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(Double.POSITIVE_INFINITY, converter), "Infinity cannot be converted to an AtomicInteger");
    }

    @ParameterizedTest(name = "{index}: invalid source={0}")
    @MethodSource("invalidSourceProvider")
    void testInvalidSourceTypes(Object from) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(from, converter), "Unsupported source type should raise IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: map={0}")
    @MethodSource("malformedMapProvider")
    void testMalformedMapEntries(Map<String, Object> map) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicInteger(map, converter), "Map entry with null or non‑numeric value should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: BigDecimal {0} → {1}")
    @MethodSource("bigDecimalProvider")
    void testBigDecimalConversion(BigDecimal bd, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(bd, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }
}
