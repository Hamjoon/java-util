package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toMap_Test {

    private Converter converter;

    // used for the default conversion path
    private Converter realConverter;

    /**
     * Simple POJO used for the custom‑object test.
     */
    private static final class CustomObject {

        private final String payload;

        CustomObject(String payload) {
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "CustomObject{" + payload + '}';
        }
    }

    /**
     * Container for parameterized test data.
     */
    private static final class BasicCase {

        final Object input;

        final Object expected;

        final String description;

        BasicCase(Object input, Object expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a no‑arg constructor that creates default options.
        // Using the real Converter ensures that the built‑in CharBuffer → String
        // conversion is exercised.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<BasicCase> basicCases() {
        return Stream.of(new BasicCase(null, null, "null source"), new BasicCase("simple string", "simple string", "plain String"), new BasicCase(CharBuffer.wrap("char buffer"), "char buffer", "CharBuffer"));
    }

    @Test
    void toMap_convertsCharBufferToString_andStoresUnderValueKey() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("Hello World");
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(source, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(VALUE), "Map must contain the VALUE key");
        assertEquals("Hello World", result.get(VALUE), "The VALUE entry should be the String representation of the CharBuffer");
    }

    @Test
    void toMap_withNullSource_returnsMapWithNullValue() {
        // GIVEN
        Object source = null;
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(source, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null even when source is null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(VALUE), "Map must contain the VALUE key");
        assertNull(result.get(VALUE), "The VALUE entry should be null when the source is null");
    }

    @Test
    void toMap_withString_returnsSameString() {
        // GIVEN
        String source = "simple string";
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(source, realConverter);
        // THEN
        assertEquals("simple string", result.get(VALUE));
    }

    @Test
    void toMap_withCharBuffer_returnsStringRepresentation() {
        // GIVEN
        CharBuffer source = CharBuffer.wrap("char buffer");
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(source, realConverter);
        // THEN
        assertEquals("char buffer", result.get(VALUE));
    }

    @Test
    void toMap_withCustomObject_usesProvidedConverter(@Mock Converter mockConverter) {
        // GIVEN
        CustomObject source = new CustomObject("payload");
        when(mockConverter.convert(eq(source), any())).thenReturn("converted");
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(source, mockConverter);
        // THEN
        verify(mockConverter, times(1)).convert(eq(source), any());
        assertEquals("converted", result.get(VALUE));
    }

    @Test
    void toMap_whenConverterThrowsRuntimeException_propagatesException(@Mock Converter mockConverter) {
        // GIVEN
        String source = "any";
        when(mockConverter.convert(eq(source), any())).thenThrow(new RuntimeException("boom"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CharBufferConversions.toMap(source, mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toMap_withNullConverter_throwsNullPointerException() {
        // GIVEN
        String source = "any string";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toMap(source, null));
    }

    @Test
    void toMap_whenConverterThrowsIllegalArgumentException_propagates(@Mock Converter mockConverter) {
        // GIVEN
        Object source = new Object();
        when(mockConverter.convert(eq(source), any())).thenThrow(new IllegalArgumentException("bad arg"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toMap(source, mockConverter));
        assertEquals("bad arg", ex.getMessage());
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicCases")
    void toMap_parameterizedBasicCases(Object input, Object expectedValue, String description) {
        // WHEN
        Map<?, ?> result = CharBufferConversions.toMap(input, realConverter);
        // THEN
        assertEquals(expectedValue, result.get(VALUE), description);
    }
}
