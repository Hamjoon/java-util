package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicLong_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance for the default‑conversion tests (no custom behaviour).
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        // If ConverterOptions has no-arg constructor, this will work; otherwise adjust accordingly.
        converter = new Converter(new ConverterOptions());
        // The real Converter can be instantiated with default options.
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
        defaultConverter = null;
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – valid conversions without a custom Converter
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// AtomicLong – should be returned unchanged (or an equivalent instance)
        Arguments.of(new AtomicLong(123L), 123L, "AtomicLong source"), // Long – numeric widening
        Arguments.of(Long.valueOf(456L), 456L, "Long source"), // Integer – numeric widening
        Arguments.of(Integer.valueOf(789), 789L, "Integer source"), // Numeric String – parsed
        Arguments.of("101112", 101112L, "String numeric source"), // Map with "value" key
        Arguments.of(Map.of(MapConversions.VALUE, 131415L), 131415L, "Map with \"value\" key"), // Map with short "_v" key
        Arguments.of(Map.of(MapConversions.V, "161718"), 161718L, "Map with short \"_v\" key"), // CompactLinkedMap containing a proper "value" entry
        Arguments.of(new CompactLinkedMap<String, Object>() {

            {
                put("value", 192021L);
            }
        }, 192021L, "CompactLinkedMap source"));
    }

    /* --------------------------------------------------------------
     *  Helper – create a simple map with the short key "_v"
     * -------------------------------------------------------------- */
    private static Map<String, Object> mapWithShortKey(Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, value);
        return map;
    }

    @Test
    void shouldConvertMapWithValueKeyToAtomicLong() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // the numeric value to be wrapped
        source.put("value", 123L);
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(123L, result.get(), "AtomicLong should contain the supplied value");
    }

    @Test
    void shouldReturnNullWhenMapDoesNotContainValueKey() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        // no "value" entry
        source.put("otherKey", 999L);
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNull(result, "Result should be null when the map lacks a \"value\" key");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, converter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void shouldConvertVariousSourcesToAtomicLong(Object source, long expected, String description) {
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, null);
        // THEN
        assertNotNull(result, "Result must not be null for: " + description);
        assertEquals(expected, result.get(), "Incorrect AtomicLong value for: " + description);
    }

    /* --------------------------------------------------------------
     *  Custom Converter – value returned directly
     * -------------------------------------------------------------- */
    @Test
    void shouldUseCustomConverterReturningLong() {
        // GIVEN: a source object that the default logic cannot handle
        Object source = new Object();
        // Mock a Converter that simply returns a Long value 123L
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(source), eq(Long.class))).thenReturn(123L);
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, custom);
        // THEN
        assertNotNull(result);
        assertEquals(123L, result.get());
        verify(custom).convert(eq(source), eq(Long.class));
    }

    /* --------------------------------------------------------------
     *  Custom Converter – adds an offset after map extraction
     * -------------------------------------------------------------- */
    @Test
    void shouldApplyCustomConverterAddingOffset() {
        // GIVEN: map with a numeric string value
        Map<String, Object> source = Map.of("value", "999");
        // Mock a Converter that adds 10 to any numeric input and returns a Long
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), eq(Long.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            long val = Long.parseLong(arg.toString());
            // add offset
            return val + 10L;
        });
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, custom);
        // THEN
        assertNotNull(result);
        assertEquals(1009L, result.get());
        verify(custom).convert(eq("999"), eq(Long.class));
    }

    /* --------------------------------------------------------------
     *  Custom Converter – returns null (propagation test)
     * -------------------------------------------------------------- */
    @Test
    void shouldPropagateNullFromCustomConverter() {
        // GIVEN
        Object source = "555";
        Converter custom = mock(Converter.class);
        when(custom.convert(eq(source), eq(Long.class))).thenReturn(null);
        // WHEN
        AtomicLong result = MapConversions.toAtomicLong(source, custom);
        // THEN
        assertNull(result, "Result should be null when the custom converter returns null");
        verify(custom).convert(eq(source), eq(Long.class));
    }

    /* --------------------------------------------------------------
     *  Null source – expect NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> MapConversions.toAtomicLong(null, null), "Method should throw NPE when source is null");
    }

    /* --------------------------------------------------------------
     *  Missing map keys – expect IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenMapLacksValueKeys() {
        Map<String, Object> source = Map.of("other", 123L);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicLong(source, null), "Method should throw IAE when map lacks \"value\" or \"_v\" keys");
    }

    /* --------------------------------------------------------------
     *  Non‑numeric map value – expect NumberFormatException
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenMapValueIsNonNumericString() {
        Map<String, Object> source = Map.of(MapConversions.VALUE, "abc");
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong(source, null), "Parsing a non‑numeric string from map should raise NFE");
    }

    /* --------------------------------------------------------------
     *  Non‑numeric plain string source – expect NumberFormatException
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenStringSourceIsNonNumeric() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toAtomicLong("notANumber", null), "Parsing a non‑numeric plain string should raise NFE");
    }

    /* --------------------------------------------------------------
     *  Custom Converter throws RuntimeException – expect propagation
     * -------------------------------------------------------------- */
    @Test
    void shouldPropagateExceptionFromThrowingConverter() {
        Object source = new Object();
        Converter throwing = mock(Converter.class);
        when(throwing.convert(eq(source), eq(Long.class))).thenThrow(new IllegalStateException("converter failure"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> MapConversions.toAtomicLong(source, throwing), "Exception from converter must be propagated unchanged");
        assertEquals("converter failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Custom Converter returns unsupported type – expect ClassCastException
     * -------------------------------------------------------------- */
    @Test
    void shouldThrowWhenConverterReturnsUnsupportedType() {
        Object source = new Object();
        Converter bad = mock(Converter.class);
        // wrong type
        when(bad.convert(eq(source), eq(Long.class))).thenReturn(Boolean.TRUE);
        assertThrows(ClassCastException.class, () -> MapConversions.toAtomicLong(source, bad), "Returning an unsupported type should cause CCE");
    }
}
