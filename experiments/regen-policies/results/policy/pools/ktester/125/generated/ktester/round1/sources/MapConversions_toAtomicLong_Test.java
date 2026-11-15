package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicLong_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */
    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return m;
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> primitiveSimpleProvider() {
        return Stream.of(// Direct Long instance
        Arguments.of(123L, 123L, "Direct Long instance should be converted to an AtomicLong holding the same long value."), // Integer instance (numeric widening)
        Arguments.of(42, 42L, "Integer instance is accepted and converted to AtomicLong via numeric widening."), // String containing a parsable decimal number
        Arguments.of("9876543210", 9876543210L, "String containing a parsable decimal number is converted to AtomicLong."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> mapBasedProvider() {
        return Stream.of(Arguments.of(map("value", 5555), 5555L, "Map with key \"value\" holding a Number should be extracted."), Arguments.of(map("value", "7777"), 7777L, "Map with key \"value\" holding a numeric String should be parsed."), Arguments.of(map("value", "-12345"), -12345L, "Negative numeric string in map is correctly parsed."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> malformedProvider() {
        return Stream.of(Arguments.of("not-a-number", NumberFormatException.class, "String that cannot be parsed as a number should cause a NumberFormatException."), Arguments.of(map("value", "abc123"), NumberFormatException.class, "Map containing a non‑numeric string for the \"value\" key should raise a conversion exception."), Arguments.of(map("foo", 123), IllegalArgumentException.class, "Map without the expected \"value\" key cannot be converted and should throw IllegalArgumentException."), Arguments.of(new Object(), ClassCastException.class, "An object of an unrelated type should result in a ClassCastException."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(null, null, "When the source object is null, toAtomicLong should return null."), Arguments.of(map("value", null), NullPointerException.class, "Map contains a null for the \"value\" key; converting null to long should raise NPE."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> otherNumericProvider() {
        return Stream.of(Arguments.of(new BigInteger("1234567890123"), 1234567890123L, "BigInteger instance is accepted and converted."), Arguments.of(new BigDecimal("9876543210.0"), 9876543210L, "BigDecimal with zero fractional part is converted."), Arguments.of(3.14d, 3L, "Double value is truncated to its long representation."), Arguments.of(42.0f, 42L, "Float value with no fractional part is converted."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> whitespaceSignProvider() {
        return Stream.of(Arguments.of(map("value", "   55  "), 55L, "String with surrounding whitespace is trimmed before parsing."), Arguments.of(map("value", "+66"), 66L, "String with an explicit plus sign is parsed correctly."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> emptyStringProvider() {
        return Stream.of(Arguments.of("", NumberFormatException.class, "Empty string cannot be parsed as a number."), Arguments.of("   ", NumberFormatException.class, "String containing only whitespace is not a valid numeric representation."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> specialFloatProvider() {
        return Stream.of(Arguments.of(Double.NaN, NumberFormatException.class, "NaN is not a valid long value."), Arguments.of(Double.POSITIVE_INFINITY, NumberFormatException.class, "Infinity cannot be represented as a long."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> unsupportedMapValueProvider() {
        return Stream.of(Arguments.of(map("value", true), ClassCastException.class, "Boolean cannot be interpreted as a numeric value."), Arguments.of(map("value", LocalDate.now()), ClassCastException.class, "LocalDate object is not a supported numeric source."));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nonMapCollectionProvider() {
        return Stream.of(Arguments.of(new long[] { 1L, 2L, 3L }, ClassCastException.class, "Array is not a Map; should throw ClassCastException."), Arguments.of(Arrays.asList(42L), ClassCastException.class, "List is not a Map; conversion should fail with ClassCastException."));
    }

    @Test
    void toAtomicLong_WithMapContainingVKey_ShouldConvertStringValue() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // the special key used by MapConversions
        source.put("_v", "12345");
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(12345L, result.get(), "AtomicLong should contain the converted long value");
    }

    @Test
    void toAtomicLong_WithMapContainingValueKey_ShouldConvertLongValue() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // alternative key used by MapConversions
        source.put("value", 9876L);
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(9876L, result.get(), "AtomicLong should contain the converted long value");
    }

    @Test
    void toAtomicLong_WithNullSource_ShouldReturnNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toAtomicLong_WithNonMapSource_ShouldConvertDirectly() {
        // GIVEN
        // a plain Long (the Converter can handle direct conversion)
        Long source = 555L;
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(555L, result.get(), "AtomicLong should contain the same long value as the source");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("primitiveSimpleProvider")
    void toAtomicLong_PrimitiveOrSimple(Object from, long expected, String description) {
        AtomicLong result = MapConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapBasedProvider")
    void toAtomicLong_MapBased(Map<String, Object> from, long expected, String description) {
        AtomicLong result = MapConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("malformedProvider")
    void toAtomicLong_Malformed(Object from, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> MapConversions.toAtomicLong(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullHandlingProvider")
    void toAtomicLong_NullHandling(Object from, Object expectedOrException, String description) {
        if (expectedOrException == null) {
            assertNull(MapConversions.toAtomicLong(from, converter), description);
        } else {
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expectedOrException;
            assertThrows(ex, () -> MapConversions.toAtomicLong(from, converter), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("otherNumericProvider")
    void toAtomicLong_OtherNumeric(Object from, long expected, String description) {
        AtomicLong result = MapConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_AlternativeKeyV() {
        Map<String, Object> source = map("_v", 7777);
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(7777L, result.get());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("whitespaceSignProvider")
    void toAtomicLong_WhitespaceAndSign(Map<String, Object> from, long expected, String description) {
        AtomicLong result = MapConversions.toAtomicLong(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_MapWithExtraKeys() {
        Map<String, Object> source = map("value", 1111, "extra", "ignored");
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1111L, result.get());
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_SourceAlreadyAtomicLong() {
        AtomicLong original = new AtomicLong(999L);
        AtomicLong result = MapConversions.toAtomicLong(original, converter);
        assertNotNull(result);
        assertEquals(999L, result.get());
        // Ensure a new instance is returned (the implementation creates a new AtomicLong)
        assertNotSame(original, result);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_BigIntegerOverflow() {
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong(tooBig, converter), "BigInteger value larger than Long.MAX_VALUE cannot be represented as an AtomicLong.");
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_HexStringNotSupported() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong("0xFF", converter), "Hexadecimal notation is not accepted by the numeric parser.");
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_NullConverter() {
        assertThrows(NullPointerException.class, () -> MapConversions.toAtomicLong(123L, null), "Passing a null Converter should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("emptyStringProvider")
    void toAtomicLong_EmptyOrWhitespaceString(String input, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> MapConversions.toAtomicLong(input, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_MalformedNumericString() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong("123abc", converter), "String with numeric prefix followed by non‑digit characters cannot be parsed.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialFloatProvider")
    void toAtomicLong_SpecialFloatingPoint(Object from, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> MapConversions.toAtomicLong(from, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_BigIntegerUnderflow() {
        // Long.MIN_VALUE - 1
        BigInteger tooSmall = new BigInteger("-9223372036854775809");
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong(tooSmall, converter), "BigInteger smaller than Long.MIN_VALUE cannot be converted to AtomicLong.");
    }

    // -----------------------------------------------------------------
    @Test
    void toAtomicLong_AmbiguousMapKeys() {
        Map<String, Object> source = map("value", 100, "_v", 200);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicLong(source, converter), "Map contains both \"value\" and the alternative key \"_v\" with different values; conversion should reject the ambiguity.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unsupportedMapValueProvider")
    void toAtomicLong_UnsupportedMapValue(Map<String, Object> source, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> MapConversions.toAtomicLong(source, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonMapCollectionProvider")
    void toAtomicLong_NonMapCollection(Object from, Class<? extends Throwable> exception, String description) {
        assertThrows(exception, () -> MapConversions.toAtomicLong(from, converter), description);
    }
}
