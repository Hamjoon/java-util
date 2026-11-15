package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toClass_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter does not need any special configuration for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference; helps GC in larger test suites.
        converter = null;
    }

    private static Stream<Arguments> provideBranchCoverageCases() {
        return Stream.of(// null source → null result
        Arguments.of(null, new Converter(new ConverterOptions()), null, null, "When the source object is null the method should return null (no conversion performed)."), // source already a Class instance → unchanged
        Arguments.of(String.class, new Converter(new ConverterOptions()), String.class, null, "If the source object is already a Class instance, the method should return it unchanged."), // map with "class" key holding a Class object
        Arguments.of(map(MapConversions.CLASS, ArrayList.class), new Converter(new ConverterOptions()), ArrayList.class, null, "Map contains a 'class' entry whose value is a Class object – the method should extract and return that Class."), // map with "class" key holding a fully‑qualified name
        Arguments.of(map(MapConversions.CLASS, "java.util.HashMap"), new Converter(new ConverterOptions()), HashMap.class, null, "Map contains a 'class' entry whose value is a fully‑qualified class name string – the method should resolve it via Class.forName and return the Class."), // map with only "value" key holding a class name
        Arguments.of(map(MapConversions.VALUE, "java.time.LocalDate"), new Converter(new ConverterOptions()), java.time.LocalDate.class, null, "Map does not have a 'class' key but has a 'value' key with a class name – the fallback conversion path should treat the value as a class name and return the corresponding Class."), // plain String containing a class name
        Arguments.of("java.time.LocalDateTime", new Converter(new ConverterOptions()), java.time.LocalDateTime.class, null, "Source is a plain String containing a fully‑qualified class name – the method should resolve and return that Class."), // non‑existent class name → ClassNotFoundException
        Arguments.of("non.existent.ClassName", new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "String does not correspond to any loadable class – the method should propagate a ClassNotFoundException."), // unsupported source type (Integer) → ConversionException
        Arguments.of(12345, new Converter(new ConverterOptions()), null, ConversionException.class, "Source is of an unsupported type (Integer) – the method should fail conversion and throw a ConversionException."));
    }

    private static Stream<Arguments> provideAdditionalBranchCases() {
        return Stream.of(// both "class" and "value" keys – "class" takes precedence
        Arguments.of(map(MapConversions.CLASS, "java.util.LinkedList", MapConversions.VALUE, "java.util.ArrayList"), new Converter(new ConverterOptions()), java.util.LinkedList.class, null, "Map contains both 'class' and 'value' keys; the method should prioritize the 'class' entry and return the Class represented by that value."), // "class" key with null value → null result
        Arguments.of(map(MapConversions.CLASS, null), new Converter(new ConverterOptions()), null, null, "Map has a 'class' key whose value is null; the conversion should result in null rather than throwing an exception."), // "value" key holding a Class object
        Arguments.of(map(MapConversions.VALUE, java.time.LocalTime.class), new Converter(new ConverterOptions()), java.time.LocalTime.class, null, "Map lacks a 'class' key but provides a 'value' key holding a Class object; the method should return that Class directly."), // "value" key holding unsupported Integer → ConversionException
        Arguments.of(map(MapConversions.VALUE, 9876), new Converter(new ConverterOptions()), null, ConversionException.class, "Map contains a 'value' key with an unsupported type (Integer); conversion should fail with a ConversionException."), // empty map → ConversionException
        Arguments.of(new HashMap<>(), new Converter(new ConverterOptions()), null, ConversionException.class, "Empty map provides no clues for class resolution; the method should throw a ConversionException."), // string with surrounding whitespace → ClassNotFoundException (no trimming)
        Arguments.of("  java.lang.Integer  ", new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "String contains leading/trailing whitespace; without trimming the lookup fails, resulting in a ClassNotFoundException."), // primitive type name → primitive class
        Arguments.of("int", new Converter(new ConverterOptions()), int.class, null, "String represents a primitive type name; the method should resolve it to the corresponding primitive Class."), // array class name → array Class
        Arguments.of("java.lang.String[]", new Converter(new ConverterOptions()), String[].class, null, "String denotes an array class name; the method should return the array Class object."), // null Converter argument → NullPointerException
        Arguments.of(map(MapConversions.CLASS, "java.util.HashSet"), null, null, NullPointerException.class, "Converter argument is null; the method should throw a NullPointerException before attempting conversion."));
    }

    private static Stream<Arguments> provideExceptionScenarioCases() {
        return Stream.of(// "class" entry with Boolean → ConversionException
        Arguments.of(map(MapConversions.CLASS, true), new Converter(new ConverterOptions()), null, ConversionException.class, "Map contains a 'class' entry with an unsupported Boolean value; conversion should fail with a ConversionException."), // "value" entry with Boolean → ConversionException
        Arguments.of(map(MapConversions.VALUE, false), new Converter(new ConverterOptions()), null, ConversionException.class, "Map contains a 'value' entry with an unsupported Boolean value; conversion should fail with a ConversionException."), // empty string source → ClassNotFoundException
        Arguments.of("", new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Source is an empty string; class lookup should fail and throw a ClassNotFoundException."), // whitespace‑only string → ClassNotFoundException
        Arguments.of("   ", new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Source is a string containing only whitespace; without trimming the lookup fails, resulting in a ClassNotFoundException."), // malformed class name → ClassNotFoundException
        Arguments.of("java..lang.String", new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Malformed class name string should cause a ClassNotFoundException during resolution."), // Boolean source object → ConversionException
        Arguments.of(true, new Converter(new ConverterOptions()), null, ConversionException.class, "Source is a Boolean object (unsupported type); the method should throw a ConversionException."), // List source object → ConversionException
        Arguments.of(Arrays.asList(1, 2, 3), new Converter(new ConverterOptions()), null, ConversionException.class, "Source is a List (unsupported type for class conversion); the method should throw a ConversionException."), // "class" entry with empty string → ClassNotFoundException
        Arguments.of(map(MapConversions.CLASS, ""), new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Map contains a 'class' entry with an empty string; class resolution should fail with a ClassNotFoundException."), // "value" entry with empty string → ConversionException
        Arguments.of(map(MapConversions.VALUE, ""), new Converter(new ConverterOptions()), null, ConversionException.class, "Map contains a 'value' entry with an empty string; conversion should fail with a ConversionException."), // "class" entry with whitespace only → ClassNotFoundException
        Arguments.of(map(MapConversions.CLASS, "   "), new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Map contains a 'class' entry with only whitespace; class lookup should fail and throw a ClassNotFoundException."), // "value" entry with whitespace only → ClassNotFoundException
        Arguments.of(map(MapConversions.VALUE, "   "), new Converter(new ConverterOptions()), null, ClassNotFoundException.class, "Map contains a 'value' entry with only whitespace; conversion should fail with a ClassNotFoundException."));
    }

    /**
     * Verify that a map containing the key {@code "class"} with a fully‑qualified class name
     *  is converted to the appropriate {@link Class} object.
     */
    @Test
    void shouldConvertMapWithClassKeyToClassObject() {
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.CLASS, "java.lang.String");
        Class<?> result = MapConversions.toClass(source, converter);
        assertNotNull(result, "Result should not be null for a valid class name");
        assertEquals(String.class, result, "Expected java.lang.String.class");
    }

    /**
     * Verify that a map containing the generic {@code "value"} key is also recognised
     *  as a class descriptor.
     */
    @Test
    void shouldConvertMapWithValueKeyToClassObject() {
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.VALUE, "java.util.ArrayList");
        Class<?> result = MapConversions.toClass(source, converter);
        assertNotNull(result, "Result should not be null for a valid class name");
        assertEquals(java.util.ArrayList.class, result, "Expected java.util.ArrayList.class");
    }

    /**
     * Passing {@code null} as the source should result in {@code null}.
     */
    @Test
    void shouldReturnNullWhenSourceIsNull() {
        Class<?> result = MapConversions.toClass(null, converter);
        assertNull(result, "Result should be null when the source map is null");
    }

    /**
     * Passing a non‑map object (e.g., a plain {@link String}) should also yield {@code null}
     *  because {@code fromMap} expects a map structure.
     */
    @Test
    void shouldReturnNullWhenSourceIsNotAMap() {
        Class<?> result = MapConversions.toClass("java.lang.Integer", converter);
        assertNull(result, "Result should be null when the source is not a map");
    }

    /**
     * If the map contains an unknown key, conversion should fail gracefully and return {@code null}.
     */
    @Test
    void shouldReturnNullWhenMapDoesNotContainRecognisedKey() {
        Map<String, Object> source = new HashMap<>();
        source.put("unknownKey", "java.lang.Boolean");
        Class<?> result = MapConversions.toClass(source, converter);
        assertNull(result, "Result should be null when the map lacks recognised class keys");
    }

    /**
     * When the class name cannot be resolved, the conversion should return {@code null}
     *  rather than throwing an exception (the underlying implementation catches ClassNotFoundException).
     */
    @Test
    void shouldReturnNullWhenClassNameIsInvalid() {
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.CLASS, "com.this.does.not.Exist");
        Class<?> result = MapConversions.toClass(source, converter);
        assertNull(result, "Result should be null for an unresolvable class name");
    }

    /**
     * Parameterised test for the first group – basic branch coverage.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideBranchCoverageCases")
    void testBranchCoverage(Object from, Converter conv, Class<?> expectedClass, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> MapConversions.toClass(from, conv), description);
        } else {
            Class<?> result = MapConversions.toClass(from, conv);
            assertEquals(expectedClass, result, description);
        }
    }

    /**
     * Parameterised test for the second group – additional branch & edge coverage.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideAdditionalBranchCases")
    void testAdditionalBranchCoverage(Object from, Converter conv, Class<?> expectedClass, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> MapConversions.toClass(from, conv), description);
        } else {
            Class<?> result = MapConversions.toClass(from, conv);
            assertEquals(expectedClass, result, description);
        }
    }

    /**
     * Parameterised test for the third group – additional exception scenarios.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionScenarioCases")
    void testExceptionScenarios(Object from, Converter conv, Class<?> expectedClass, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> MapConversions.toClass(from, conv), description);
        } else {
            Class<?> result = MapConversions.toClass(from, conv);
            assertEquals(expectedClass, result, description);
        }
    }

    /**
     * Small utility to create a {@link HashMap} from an even number of arguments.
     * Example: {@code map("class", "java.util.List", "value", 123)}.
     */
    @SafeVarargs
    private static Map<String, Object> map(Object... entries) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            String key = (String) entries[i];
            Object value = entries[i + 1];
            m.put(key, value);
        }
        return m;
    }
}
