package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
class MapConversions_toClass_Test {

    /**
     * The {@link Converter} used by the tests.
     */
    private static Converter converter;

    /**
     * The default {@link Converter} used for the majority of test cases.
     */
    private static Converter defaultConverter;

    /**
     * Simple stub that forces every conversion to return {@link String#class}.
     */
    private static final class AlwaysStringConverter extends Converter {

        AlwaysStringConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            // Regardless of the input, always return String.class when a Class is requested.
            if (toClass == Class.class) {
                return (T) String.class;
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * Stub that throws a {@link ClassNotFoundException} for any class‑name conversion.
     */
    private static final class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object from, Class<T> toClass) {
            if (toClass == Class.class) {
                throw new ClassConversionException("Unable to locate class", new ClassNotFoundException());
            }
            return super.convert(from, toClass);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a no‑arg constructor for Converter that works with default options.
        converter = new Converter();
        defaultConverter = converter;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter references.
        converter = null;
        defaultConverter = null;
    }

    private static Stream<Arguments> invalidStringInputs() {
        return Stream.of(Arguments.of("com.nonexistent.FooBar"), Arguments.of(""));
    }

    private static Stream<Arguments> mapWithUnsupportedValues() {
        return Stream.of(Arguments.of(Map.of("class", 12345)), Arguments.of(Map.of("value", true)));
    }

    @Test
    void toClass_givenClassObject_returnsSameClass() {
        Class<?> result = MapConversions.toClass(String.class, defaultConverter);
        assertEquals(String.class, result, "When the source object is a Class instance the same instance must be returned.");
    }

    @Test
    void toClass_givenFullyQualifiedNameString_returnsCorrespondingClass() {
        Class<?> result = MapConversions.toClass("java.lang.Integer", defaultConverter);
        assertEquals(Integer.class, result, "A fully‑qualified class name string must be resolved to the correct Class.");
    }

    @Test
    void toClass_givenMapWithClassKey_returnsCorrespondingClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.lang.Double");
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(Double.class, result, "Map with key \"class\" should be interpreted as a class name");
    }

    @Test
    void toClass_givenMapWithValueKey_returnsCorrespondingClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "java.time.LocalDate");
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(java.time.LocalDate.class, result, "Map with key \"value\" should be interpreted as a class name");
    }

    @Test
    void toClass_givenMapWithVKey_returnsCorrespondingClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "java.util.UUID");
        Class<?> result = MapConversions.toClass(map, converter);
        assertEquals(java.util.UUID.class, result, "Map with key \"_v\" should be interpreted as a class name");
    }

    @Test
    void toClass_givenNull_returnsNull() {
        Class<?> result = MapConversions.toClass(null, converter);
        assertNull(result, "Null input should result in null output");
    }

    @Test
    void toClass_givenCharSequence_returnsCorrespondingClass() {
        StringBuilder sb = new StringBuilder("java.time.LocalDate");
        Class<?> result = MapConversions.toClass(sb, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "Any CharSequence containing a class name should be treated like a String.");
    }

    @Test
    void toClass_givenUnsupportedType_throwsClassConversionException() {
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(12345, defaultConverter), "An unsupported source type (e.g., Integer) must cause a ClassConversionException.");
    }

    @Test
    void toClass_mapWithClassKey_ClassInstance_returnsSameClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", java.util.Date.class);
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.util.Date.class, result, "Map key \"class\" holding a Class instance should be returned directly.");
    }

    @Test
    void toClass_mapWithValueKey_StringClassName_returnsCorrespondingClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "java.time.LocalDate");
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "Map key \"value\" holding a fully‑qualified class name string must be resolved.");
    }

    @Test
    void toClass_mapWithVKey_ClassInstance_returnsSameClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", java.math.BigDecimal.class);
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.math.BigDecimal.class, result, "Legacy key \"_v\" holding a Class instance must be recognised.");
    }

    @Test
    void toClass_mapWithoutAnyClassKeys_returnsNullOrThrows() {
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        // The contract says null or a conversion exception; we accept either.
        assertAll(() -> assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, defaultConverter), "Map without recognised keys should throw ClassConversionException."), () -> assertNull(MapConversions.toClass(map, defaultConverter), "Map without recognised keys may also return null."));
    }

    @Test
    void toClass_precedence_classKeyOverridesValueKey_ClassInstance() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", java.time.LocalDate.class);
        map.put("value", "java.time.LocalDateTime");
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "\"class\" key takes precedence over \"value\" when both are present.");
    }

    @Test
    void toClass_precedence_classKeyStringOverridesValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", "java.time.LocalDate");
        map.put("value", "java.time.LocalDateTime");
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "\"class\" key (even as a String) overrides the \"value\" key.");
    }

    @Test
    void toClass_fallback_classKeyNullFallsBackToValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", null);
        map.put("value", "java.time.LocalDate");
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "When the \"class\" key is present but null, the method must fall back to \"value\".");
    }

    @Test
    void toClass_legacyVKeyStringResolution() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "java.time.LocalDate");
        Class<?> result = MapConversions.toClass(map, defaultConverter);
        assertEquals(java.time.LocalDate.class, result, "Legacy key \"_v\" containing a class name string must be resolved.");
    }

    @Test
    void toClass_invalidClassName_throwsClassConversionException() {
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass("com.nonexistent.FooBar", defaultConverter), "Non‑existent class name must trigger ClassConversionException.");
    }

    @Test
    void toClass_emptyString_throwsClassConversionException() {
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass("", defaultConverter), "Empty string cannot be resolved to a class.");
    }

    @Test
    void toClass_mapClassKeyWithUnsupportedType_throwsClassConversionException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", 12345);
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, defaultConverter), "Map entry \"class\" with an unsupported value type must fail.");
    }

    @Test
    void toClass_mapValueKeyWithUnsupportedType_throwsClassConversionException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", true);
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, defaultConverter), "Map entry \"value\" with an unsupported value type must fail.");
    }

    @Test
    void toClass_mapBothKeysNull_throwsClassConversionException() {
        Map<String, Object> map = new HashMap<>();
        map.put("class", null);
        map.put("value", null);
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, defaultConverter), "Both recognised keys null should result in ClassConversionException.");
    }

    @Test
    void toClass_emptyCharSequence_throwsClassConversionException() {
        StringBuilder sb = new StringBuilder("");
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(sb, defaultConverter), "Empty CharSequence must cause ClassConversionException.");
    }

    @Test
    void toClass_whitespaceCharSequence_throwsClassConversionException() {
        StringBuilder sb = new StringBuilder("   ");
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(sb, defaultConverter), "Whitespace‑only CharSequence must cause ClassConversionException.");
    }

    @Test
    void toClass_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toClass("java.time.LocalDate", null), "Passing a null Converter must result in NullPointerException.");
    }

    @Test
    void toClass_customConverterAlwaysString_returnsStringClass() {
        // Mock a Converter that always returns String.class when converting to Class
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), ArgumentMatchers.eq(Class.class))).thenReturn(String.class);
        Class<?> result = MapConversions.toClass("com.example.UnknownClass", custom);
        assertEquals(String.class, result, "Custom converter that always returns String.class should override default resolution.");
    }

    @Test
    void toClass_customConverterThrows_propagatesClassConversionException() {
        // Mock a Converter that throws ClassConversionException for any conversion to Class
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), ArgumentMatchers.eq(Class.class))).thenThrow(new ClassConversionException("Unable to locate class", new ClassNotFoundException()));
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass("com.example.UnknownClass", throwing), "A converter that throws ClassConversionException must have that exception propagated.");
    }

    @Test
    void toClass_customConverterWithMapValue_throwsClassConversionException() {
        // Mock a Converter that throws when asked to convert a map value to Class
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), ArgumentMatchers.eq(Class.class))).thenThrow(new ClassConversionException("Unable to locate class", new ClassNotFoundException()));
        Map<String, Object> map = new HashMap<>();
        map.put("value", "com.example.UnknownClass");
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, throwing), "When a custom converter throws while handling a map value, the exception must be propagated.");
    }

    @ParameterizedTest(name = "Invalid class name \"{0}\" should throw ClassConversionException")
    @MethodSource("invalidStringInputs")
    void toClass_invalidStringInputs_throwsException(String input) {
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(input, defaultConverter));
    }

    @ParameterizedTest(name = "Map {0} should cause ClassConversionException")
    @MethodSource("mapWithUnsupportedValues")
    void toClass_mapsWithUnsupportedValues_throwsException(Map<String, Object> map) {
        assertThrows(ClassConversionException.class, () -> MapConversions.toClass(map, defaultConverter));
    }
}
