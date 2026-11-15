import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBigDecimal_Test {

    /**
     * The Converter used by all test cases.
     */
    private Converter converter;

    /**
     * The Converter used by most test cases (default implementation).
     */
    private Converter defaultConverter;

    /**
     * Simple custom number class used in a custom‑converter test.
     */
    class CustomNumber {

        final int value;

        CustomNumber(int v) {
            this.value = v;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter for each test to avoid cross‑test state.
        // The library requires a ConverterOptions instance for construction.
        converter = new Converter(new ConverterOptions());
        defaultConverter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> nullInputHandling() {
        assertThrows(expectedException, () -> MapConversions.toBigDecimal(from, conv), description);
    }

    private static Stream<Arguments> numericSourceTypes() {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result, "Result should not be null");
        assertEquals(0, expected.compareTo(result), description);
    }

    private static Stream<Arguments> stringSourceTypes() {
        if (expected instanceof Class) {
            assertThrows(((Class<? extends Throwable>) expected), () -> MapConversions.toBigDecimal(from, defaultConverter), description);
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
            assertNotNull(result, "Result should not be null");
            assertEquals(0, ((BigDecimal) expected).compareTo(result), description);
        }
    }

    private static Stream<Arguments> mapSourceWithValueKeys() {
        if (expected instanceof Class) {
            assertThrows(((Class<? extends Throwable>) expected), () -> MapConversions.toBigDecimal(source, defaultConverter), description);
        } else {
            BigDecimal result = MapConversions.toBigDecimal(source, defaultConverter);
            assertNotNull(result, "Result should not be null");
            assertEquals(0, ((BigDecimal) expected).compareTo(result), description);
        }
    }

    private static Stream<Arguments> unsupportedSourceTypes() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(from, defaultConverter), description);
    }

    private static Stream<Arguments> boundaryNumericValues() {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    private static Stream<Arguments> stringScientificNotation() {
        BigDecimal result = MapConversions.toBigDecimal(source, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    private static Stream<Arguments> mapWithNullOrMissingValue() {
        if (expected == null) {
            assertNull(MapConversions.toBigDecimal(source, defaultConverter), description);
        } else {
            assertThrows(((Class<? extends Throwable>) expected), () -> MapConversions.toBigDecimal(source, defaultConverter), description);
        }
    }

    private static Stream<Arguments> unsupportedPrimitiveWrappers() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(from, defaultConverter), description);
    }

    private static Stream<Arguments> emptyAndWhitespaceStrings() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    private static Stream<Arguments> numericPrimitiveWrappers() {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    private static Stream<Arguments> specialNumericValues() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    private static Stream<Arguments> mapWithUnsupportedValueType() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    private static Stream<Arguments> unsupportedCharSequence() {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    @Test
    void toBigDecimal_FromMapWithValueKey_String() {
        Map<String, Object> source = new HashMap<>();
        source.put("value", "123.45");
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(new BigDecimal("123.45"), result, "String value should be converted to BigDecimal");
    }

    @Test
    void toBigDecimal_FromMapWithVKey_String() {
        Map<String, Object> source = new HashMap<>();
        source.put("_v", "67.89");
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(new BigDecimal("67.89"), result, "Short key '_v' should be converted to BigDecimal");
    }

    @Test
    void toBigDecimal_FromBigDecimal_ReturnsSameValue() {
        BigDecimal original = new BigDecimal("42.00");
        BigDecimal result = MapConversions.toBigDecimal(original, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(original, result, "Passing a BigDecimal should return an equal BigDecimal");
    }

    @Test
    void toBigDecimal_FromDouble_Number() {
        Double source = 10.5;
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(BigDecimal.valueOf(10.5), result, "Double should be converted to BigDecimal preserving the value");
    }

    @Test
    void toBigDecimal_FromNull_ReturnsNull() {
        Object source = null;
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNull(result, "Converting null should yield null");
    }

    /* -------------------------------------------------------------
     Group: Null input handling
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullInputHandling")
    void nullInputHandling(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toBigDecimal(from, converter), description);
    }

    /* -------------------------------------------------------------
     Group: Numeric source types
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericSourceTypes")
    void numericSourceTypes(String description, Object from, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result, "Result should not be null");
        assertEquals(0, expected.compareTo(result), description);
    }

    /* -------------------------------------------------------------
     Group: String source types
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringSourceTypes")
    void stringSourceTypes(String description, Object from, Object expected) {
        if (expected instanceof Class) {
            // Expected exception
            assertThrows(((Class<? extends Throwable>) (expected)), () -> MapConversions.toBigDecimal(from, defaultConverter), description);
        } else {
            // Expected BigDecimal
            BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
            assertNotNull(result, "Result should not be null");
            assertEquals(0, ((BigDecimal) (expected)).compareTo(result), description);
        }
    }

    /* -------------------------------------------------------------
     Group: Map source with VALUE_KEYS
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapSourceWithValueKeys")
    void mapSourceWithValueKeys(String description, Map<String, Object> source, Object expected) {
        if (expected instanceof Class) {
            assertThrows(((Class<? extends Throwable>) (expected)), () -> MapConversions.toBigDecimal(source, defaultConverter), description);
        } else {
            BigDecimal result = MapConversions.toBigDecimal(source, defaultConverter);
            assertNotNull(result, "Result should not be null");
            assertEquals(0, ((BigDecimal) (expected)).compareTo(result), description);
        }
    }

    /* -------------------------------------------------------------
     Group: Unsupported source types
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedSourceTypes")
    void unsupportedSourceTypes(String description, Object from) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(from, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Fallback to custom Converter
    -------------------------------------------------------------
     */
    @Test
    void fallbackToCustomConverter() {
        CustomNumber source = new CustomNumber(555);
        Converter customConverter = Mockito.mock(Converter.class);
        Mockito.when(customConverter.convert(eq(source), eq(BigDecimal.class), any())).thenReturn(new BigDecimal("555"));
        BigDecimal result = MapConversions.toBigDecimal(source, customConverter);
        assertNotNull(result);
        assertEquals(new BigDecimal("555"), result);
    }

    /* -------------------------------------------------------------
     Group: Boundary numeric values
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryNumericValues")
    void boundaryNumericValues(String description, Object from, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    /* -------------------------------------------------------------
     Group: String scientific notation and signs
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringScientificNotation")
    void stringScientificNotation(String description, String source, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(source, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    /* -------------------------------------------------------------
     Group: BigDecimal identity conversion
    -------------------------------------------------------------
     */
    @Test
    void bigDecimalIdentityConversion() {
        BigDecimal source = new BigDecimal("12345.6789");
        BigDecimal result = MapConversions.toBigDecimal(source, defaultConverter);
        assertNotNull(result);
        assertEquals(0, source.compareTo(result));
    }

    /* -------------------------------------------------------------
     Group: Map with null or missing value
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapWithNullOrMissingValue")
    void mapWithNullOrMissingValue(String description, Map<String, Object> source, Object expected) {
        if (expected == null) {
            assertNull(MapConversions.toBigDecimal(source, defaultConverter), description);
        } else {
            assertThrows(((Class<? extends Throwable>) (expected)), () -> MapConversions.toBigDecimal(source, defaultConverter), description);
        }
    }

    /* -------------------------------------------------------------
     Group: Null converter handling
    -------------------------------------------------------------
     */
    @Test
    void nullConverterHandling() {
        assertThrows(NullPointerException.class, () -> MapConversions.toBigDecimal("123", null), "If the supplied Converter instance is null, dispatch should throw NPE before attempting conversion.");
    }

    /* -------------------------------------------------------------
     Group: Unsupported primitive wrapper types
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedPrimitiveWrappers")
    void unsupportedPrimitiveWrappers(String description, Object from) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(from, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Empty and whitespace‑only strings
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyAndWhitespaceStrings")
    void emptyAndWhitespaceStrings(String description, String source) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Numeric primitive wrappers other than Integer/Long
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericPrimitiveWrappers")
    void numericPrimitiveWrappers(String description, Object from, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(from, defaultConverter);
        assertNotNull(result);
        assertEquals(0, expected.compareTo(result), description);
    }

    /* -------------------------------------------------------------
     Group: Special numeric values (NaN, Infinity)
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("specialNumericValues")
    void specialNumericValues(String description, Number source) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Map with unsupported value type
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapWithUnsupportedValueType")
    void mapWithUnsupportedValueType(String description, Map<String, Object> source) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Map with whitespace‑only string value
    -------------------------------------------------------------
     */
    @Test
    void mapWithWhitespaceOnlyStringValue() {
        Map<String, Object> source = Map.of("value", "   ");
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), "Map value is a string that becomes empty after trimming; parsing should fail with a conversion exception.");
    }

    /* -------------------------------------------------------------
     Group: Unsupported CharSequence implementations
    -------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedCharSequence")
    void unsupportedCharSequence(String description, CharSequence source) {
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(source, defaultConverter), description);
    }

    /* -------------------------------------------------------------
     Group: Byte array source
    -------------------------------------------------------------
     */
    @Test
    void byteArraySource() {
        byte[] bytes = "12345".getBytes(StandardCharsets.UTF_8);
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(bytes, defaultConverter), "A raw byte[] representing numeric characters is not a supported source type for BigDecimal conversion.");
    }

    /* -------------------------------------------------------------
     Group: CharBuffer source
    -------------------------------------------------------------
     */
    @Test
    void charBufferSource() {
        CharBuffer buffer = CharBuffer.wrap("6789");
        assertThrows(ConversionException.class, () -> MapConversions.toBigDecimal(buffer, defaultConverter), "CharBuffer is not directly convertible to BigDecimal; the method should raise a conversion exception.");
    }
}
