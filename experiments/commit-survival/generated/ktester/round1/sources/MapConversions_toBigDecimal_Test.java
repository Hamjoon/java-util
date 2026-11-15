package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBigDecimal_Test {

    /**
     * Mocked {@link Converter} used by the {@code MapConversions} methods.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A simple map that mimics the structure expected by {@code MapConversions}.
     */
    private Map<String, Object> valueMap;

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a map with the standard "value" key.
        valueMap = new HashMap<>();
        valueMap.put(MapConversions.VALUE, "123.45");
        // Configure the mock converter to return a predictable BigDecimal
        // whenever a conversion to BigDecimal is requested.
        when(mockConverter.convert(any(), eq(BigDecimal.class))).thenAnswer(invocation -> {
            Object source = invocation.getArgument(0);
            // The real conversion logic is not under test here; we simply
            // emulate a successful conversion by constructing a BigDecimal
            // from the source's {@code toString()} representation.
            return new BigDecimal(source.toString());
        });
    }

    private static Stream<Arguments> nullAndIdentityProvider() {
        return Stream.of(Arguments.of("Null source with null converter -> null", null, null, null, null), Arguments.of("Null source with non‑null converter -> null", null, mock(Converter.class), null, null), Arguments.of("Identity: already a BigDecimal", new BigDecimal("12345.6789"), null, new BigDecimal("12345.6789"), null));
    }

    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of("Integer → BigDecimal", Integer.valueOf(42), new BigDecimal("42")), Arguments.of("Long → BigDecimal", Long.valueOf(1234567890123L), new BigDecimal("1234567890123")), Arguments.of("Double → BigDecimal (string based)", Double.valueOf(12.34), new BigDecimal("12.34")), Arguments.of("BigInteger → BigDecimal", new BigInteger("98765432109876543210"), new BigDecimal("98765432109876543210")));
    }

    private static Stream<Arguments> stringValuesProvider() {
        return Stream.of(Arguments.of("Negative numeric string", "-12345.6789", new BigDecimal("-12345.6789"), null), Arguments.of("String with surrounding whitespace", "  3.14159  ", new BigDecimal("3.14159"), null), Arguments.of("Non‑numeric string → NumberFormatException", "notANumber", null, NumberFormatException.class), Arguments.of("Plus sign string", "+123.45", new BigDecimal("+123.45"), null), Arguments.of("Scientific notation", "1.23e4", new BigDecimal("1.23E+4"), null), Arguments.of("Only whitespace → NumberFormatException", "   ", null, NumberFormatException.class), Arguments.of("Empty string → NumberFormatException", "", null, NumberFormatException.class));
    }

    private static Stream<Arguments> mapBasedProvider() {
        // Helper maps
        Map<String, Object> mapValueNumeric = Map.of(MapConversions.VALUE, 12345);
        Map<String, Object> mapVString = Map.of(MapConversions.V, "6789.01");
        Map<String, Object> mapNoKeys = new HashMap<String, Object>() {

            {
                put("unused", 1);
                put("another", "2");
            }
        };
        Map<String, Object> mapValueNonNumber = Map.of(MapConversions.VALUE, "notANumber");
        Map<String, Object> mapValueNull = Map.of(MapConversions.VALUE, null);
        Map<String, Object> mapVNonNumber = Map.of(MapConversions.V, "notNumber");
        Map<String, Object> mapValueFloat = Map.of(MapConversions.VALUE, new Float(9.87f));
        Map<String, Object> mapValueBigDecimal = Map.of(MapConversions.VALUE, new BigDecimal("123.456"));
        Map<String, Object> mapValueAndV = Map.of(MapConversions.VALUE, "111", MapConversions.V, "222");
        Map<String, Object> mapVAndValueReversed = new HashMap<String, Object>() {

            {
                put(MapConversions.V, "333");
                put(MapConversions.VALUE, "444");
            }
        };
        return Stream.of(Arguments.of("Map with key \"value\" and numeric value", mapValueNumeric, null, new BigDecimal("12345"), null), Arguments.of("Map with key \"_v\" and numeric string", mapVString, null, new BigDecimal("6789.01"), null), Arguments.of("Map without recognised keys → IllegalArgumentException", mapNoKeys, null, null, IllegalArgumentException.class), Arguments.of("Map with recognised key but non‑numeric string → NumberFormatException", mapValueNonNumber, null, null, NumberFormatException.class), Arguments.of("Map with recognised key but null value → IllegalArgumentException", mapValueNull, null, null, IllegalArgumentException.class), Arguments.of("Map with \"_v\" key and non‑numeric string → NumberFormatException", mapVNonNumber, null, null, NumberFormatException.class), Arguments.of("Map value is Float → BigDecimal", mapValueFloat, null, new BigDecimal("9.87"), null), Arguments.of("Map value is already BigDecimal → same value", mapValueBigDecimal, null, new BigDecimal("123.456"), null), Arguments.of("Both \"value\" and \"_v\" present – prefer \"value\"", mapValueAndV, null, new BigDecimal("111"), null), Arguments.of("Both keys present in reverse insertion order – still prefer \"value\"", mapVAndValueReversed, null, new BigDecimal("444"), null));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of("Boolean source → ClassCastException", Boolean.TRUE), Arguments.of("Date source → ClassCastException", new Date()), Arguments.of("Character source → ClassCastException", Character.valueOf('A')), Arguments.of("UUID source → ClassCastException", UUID.randomUUID()), Arguments.of("LocalDate source → ClassCastException", java.time.LocalDate.now()), Arguments.of("LocalDateTime source → ClassCastException", java.time.LocalDateTime.now()), Arguments.of("Instant source → ClassCastException", java.time.Instant.now()), Arguments.of("ZonedDateTime source → ClassCastException", java.time.ZonedDateTime.now()), Arguments.of("Duration source → ClassCastException", java.time.Duration.ofHours(5)));
    }

    private static Stream<Arguments> additionalNumericProvider() {
        return Stream.of(Arguments.of("Float → BigDecimal (string based)", Float.valueOf(12.34f), new BigDecimal("12.34")), Arguments.of("Short → BigDecimal", Short.valueOf((short) 7), new BigDecimal("7")), Arguments.of("Byte (negative) → BigDecimal", Byte.valueOf((byte) -3), new BigDecimal("-3")), Arguments.of("AtomicInteger → BigDecimal", new AtomicInteger(12345), new BigDecimal("12345")), Arguments.of("AtomicLong → BigDecimal", new AtomicLong(9876543210L), new BigDecimal("9876543210")));
    }

    private static Stream<Arguments> mapUnsupportedValueProvider() {
        return Stream.of(Arguments.of("Map value Boolean → ClassCastException", Map.of(MapConversions.VALUE, Boolean.TRUE)), Arguments.of("Map value Date → ClassCastException", Map.of(MapConversions.V, new Date())), Arguments.of("Map value arbitrary Object → ClassCastException", Map.of(MapConversions.VALUE, new Object())));
    }

    @Test
    void toBigDecimal_fromMapWithValueKey_returnsConvertedBigDecimal() {
        // GIVEN a map containing a numeric string under the "value" key
        // AND a mocked Converter that can turn that string into a BigDecimal
        // WHEN the focal method is invoked
        BigDecimal result = MapConversions.toBigDecimal(valueMap, mockConverter);
        // THEN the result should be the expected BigDecimal representation
        assertNotNull(result, "Result should not be null");
        assertEquals(new BigDecimal("123.45"), result, "Converted BigDecimal does not match expected value");
        // Verify that the converter was asked to perform the conversion.
        verify(mockConverter, times(1)).convert(eq("123.45"), eq(BigDecimal.class));
    }

    @Test
    void toBigDecimal_whenSourceIsAlreadyBigDecimal_returnsSameInstance() {
        // GIVEN a source that is already a BigDecimal
        BigDecimal source = new BigDecimal("987.65");
        // The mock converter should simply return the argument it receives.
        when(mockConverter.convert(eq(source), eq(BigDecimal.class))).thenReturn(source);
        // WHEN the method is called with the BigDecimal directly (not a map)
        BigDecimal result = MapConversions.toBigDecimal(source, mockConverter);
        // THEN the same instance should be returned (no new object creation)
        assertSame(source, result, "When source is a BigDecimal, the same instance should be returned");
        verify(mockConverter, times(1)).convert(eq(source), eq(BigDecimal.class));
    }

    @Test
    void toBigDecimal_withNullSource_returnsNull() {
        // GIVEN a null source object
        // WHEN the method is invoked
        BigDecimal result = MapConversions.toBigDecimal(null, mockConverter);
        // THEN the result should be null (no conversion attempted)
        assertNull(result, "Result should be null when source is null");
        verifyNoInteractions(mockConverter);
    }

    /* ---------- Null and Identity Cases ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullAndIdentityProvider")
    void nullAndIdentityCases(String description, Object from, Converter converter, Object expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            Throwable ex = assertThrows(expectedException, () -> MapConversions.toBigDecimal(from, converter), description);
            // No further assertions needed for exception cases.
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, converter);
            assertEquals(expectedResult, result, description);
        }
    }

    /* ---------- Numeric Types ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericTypesProvider")
    void numericTypes(String description, Object from, BigDecimal expected) {
        // No converter required for pure numeric types
        BigDecimal result = MapConversions.toBigDecimal(from, null);
        assertEquals(expected, result, description);
    }

    /* ---------- String Values ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringValuesProvider")
    void stringValues(String description, String from, Object expected, Class<? extends Throwable> exception) {
        if (exception != null) {
            assertThrows(exception, () -> MapConversions.toBigDecimal(from, null), description);
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, null);
            assertEquals(expected, result, description);
        }
    }

    /* ---------- Map Based Conversions ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapBasedProvider")
    void mapBasedConversions(String description, Map<String, Object> map, Converter converter, Object expected, Class<? extends Throwable> exception) {
        if (exception != null) {
            assertThrows(exception, () -> MapConversions.toBigDecimal(map, converter), description);
        } else {
            BigDecimal result = MapConversions.toBigDecimal(map, converter);
            assertEquals(expected, result, description);
        }
    }

    /* ---------- Unsupported Types ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedTypesProvider")
    void unsupportedTypes(String description, Object from) {
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(from, null), description);
    }

    /* ---------- Additional Numeric Types (Atomic, etc.) ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalNumericProvider")
    void additionalNumericTypes(String description, Object from, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(from, null);
        assertEquals(expected, result, description);
    }

    /* ---------- Converter Interaction – Normal Paths ---------- */
    @Test
    void converterInteraction_simpleString() {
        // Mock that simply returns the original string (no transformation)
        Converter converter = mock(Converter.class);
        when(converter.convert(eq("123"), eq(BigDecimal.class))).thenAnswer(inv -> new BigDecimal("123"));
        BigDecimal result = MapConversions.toBigDecimal("123", converter);
        assertEquals(new BigDecimal("123"), result);
        verify(converter, times(1)).convert(eq("123"), eq(BigDecimal.class));
    }

    @Test
    void converterInteraction_mapValueTransformed() {
        // Converter transforms the map's value from "456" to "789"
        Converter converter = mock(Converter.class);
        when(converter.convert(eq("456"), eq(BigDecimal.class))).thenAnswer(inv -> new BigDecimal("789"));
        Map<String, Object> map = Map.of(MapConversions.VALUE, "456");
        BigDecimal result = MapConversions.toBigDecimal(map, converter);
        assertEquals(new BigDecimal("789"), result);
        verify(converter, times(1)).convert(eq("456"), eq(BigDecimal.class));
    }

    /* ---------- Converter Interaction – Error Paths ---------- */
    @Test
    void converterInteraction_converterThrowsIllegalArgumentException() {
        Converter converter = mock(Converter.class);
        when(converter.convert(eq("999"), eq(BigDecimal.class))).thenThrow(new IllegalArgumentException("bad conversion"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBigDecimal("999", converter), "Exception from converter must be propagated");
        verify(converter, times(1)).convert(eq("999"), eq(BigDecimal.class));
    }

    @Test
    void converterInteraction_converterReturnsNullForMapEntry() {
        Converter converter = mock(Converter.class);
        // Simulate a converter that returns null for the map value
        when(converter.convert(eq("123"), eq(BigDecimal.class))).thenReturn(null);
        Map<String, Object> map = Map.of(MapConversions.VALUE, "123");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBigDecimal(map, converter), "Null from converter should cause IllegalArgumentException");
        verify(converter, times(1)).convert(eq("123"), eq(BigDecimal.class));
    }

    @Test
    void converterInteraction_converterThrowsRuntimeException() {
        Converter converter = mock(Converter.class);
        when(converter.convert(eq("123"), eq(BigDecimal.class))).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> MapConversions.toBigDecimal("123", converter), "Unchecked RuntimeException must be propagated");
        verify(converter, times(1)).convert(eq("123"), eq(BigDecimal.class));
    }

    @Test
    void converterInteraction_converterReturnsWrongType() {
        Converter converter = mock(Converter.class);
        // Return an Integer where a Number/String is expected
        when(converter.convert(eq("456"), eq(BigDecimal.class))).thenReturn(Integer.valueOf(789));
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal("456", converter), "Wrong return type from converter should cause ClassCastException");
        verify(converter, times(1)).convert(eq("456"), eq(BigDecimal.class));
    }

    /* ---------- Map with Unsupported Value Types ---------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapUnsupportedValueProvider")
    void mapWithUnsupportedValueTypes(String description, Map<String, Object> map) {
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(map, null), description);
    }
}
