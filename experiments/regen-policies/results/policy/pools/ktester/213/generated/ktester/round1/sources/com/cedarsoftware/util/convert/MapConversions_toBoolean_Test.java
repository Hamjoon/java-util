package com.cedarsoftware.util.convert;

import java.util.*;
import java.util.concurrent.atomic.*;
// import org.junit.*;
import java.util.stream.Stream;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class MapConversions_toBoolean_Test {

    private static Converter converter;

    /**
     * Simple {@code Map} implementation that throws on any {@code get} call.
     */
    private static final class ThrowingMap implements Map<String, Object> {

        @Override
        public Object get(Object key) {
            throw new RuntimeException("forced exception");
        }

        // The rest of the Map contract is delegated to an empty map.
        private final Map<String, Object> delegate = Collections.emptyMap();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public Object put(String key, Object value) {
            return delegate.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<String> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return delegate.entrySet();
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions is abstract; instantiate via an anonymous subclass.
        converter = new Converter(new ConverterOptions() {
        });
    }

    //     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Builds a {@code Map<String,Object>} from a var‑args list of key/value pairs.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    /* ---------- Valid Conversions ---------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// String literals (case‑insensitive)
        Arguments.of("String literal \"true\" → TRUE", "true", Boolean.TRUE), Arguments.of("String literal \"FALSE\" → FALSE", "FALSE", Boolean.FALSE), // Yes / No tokens
        Arguments.of("\"yes\" → TRUE", "yes", Boolean.TRUE), Arguments.of("\"no\" → FALSE", "no", Boolean.FALSE), // Numeric values
        Arguments.of("Integer 1 → TRUE", 1, Boolean.TRUE), Arguments.of("Integer 0 → FALSE", 0, Boolean.FALSE), // String numeric tokens
        Arguments.of("\"1\" → TRUE", "1", Boolean.TRUE), Arguments.of("\"0\" → FALSE", "0", Boolean.FALSE), // Direct Boolean objects
        Arguments.of("Boolean TRUE object → TRUE", Boolean.TRUE, Boolean.TRUE), Arguments.of("Boolean FALSE object → FALSE", Boolean.FALSE, Boolean.FALSE), // Map with key "value"
        Arguments.of("Map {\"value\":true} → TRUE", mapOf("value", true), Boolean.TRUE), // Map with shortcut key "_v"
        Arguments.of("Map {\"_v\":false} → FALSE", mapOf("_v", false), Boolean.FALSE), // Map with true‑ish string
        Arguments.of("Map {\"value\":\"yes\"} → TRUE", mapOf("value", "yes"), Boolean.TRUE));
    }

    /* ---------- Edge Cases ---------- */
    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of("Null source → null", null, null), Arguments.of("Empty string → null", "", null), Arguments.of("String with surrounding whitespace \"   true   \" → TRUE", "   true   ", Boolean.TRUE), Arguments.of("String with surrounding whitespace \"   FALSE   \" → FALSE", "   FALSE   ", Boolean.FALSE));
    }

    /* ---------- Invalid Conversions ---------- */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of("\"maybe\" (unrecognized token) → null", "maybe", null), Arguments.of("Integer 2 (unsupported numeric) → null", 2, null), Arguments.of("Integer -1 (unsupported numeric) → null", -1, null), Arguments.of("String \"[true]\" (collection literal) → null", "[true]", null));
    }

    /* ---------- Whitespace and Case Sensitivity ---------- */
    private static Stream<Arguments> caseSensitivityProvider() {
        return Stream.of(Arguments.of("\"TrUe\" mixed case → TRUE", "TrUe", Boolean.TRUE), Arguments.of("\"fAlSe\" mixed case → FALSE", "fAlSe", Boolean.FALSE), Arguments.of("\"YES\" upper case → TRUE", "YES", Boolean.TRUE), Arguments.of("\"No\" capitalized → FALSE", "No", Boolean.FALSE), Arguments.of("\"on\" treated as true", "on", Boolean.TRUE), Arguments.of("\"off\" treated as false", "off", Boolean.FALSE));
    }

    /* ---------- Numeric Strings Beyond 0/1 ---------- */
    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("\"2\" not a true‑ish token → null", "2", null), Arguments.of("\"-1\" negative numeric string → null", "-1", null), Arguments.of("\" 1 \" with spaces → TRUE", " 1 ", Boolean.TRUE));
    }

    /* ---------- Map Edge Cases ---------- */
    private static Stream<Arguments> mapEdgeCaseProvider() {
        return Stream.of(// When both keys exist, shortcut "_v" takes precedence
        Arguments.of("Empty map → null", mapOf(), null), Arguments.of("Map with unrelated key → null", mapOf("other", true), null), Arguments.of("Map {\"value\":\"off\"} → FALSE", mapOf("value", "off"), Boolean.FALSE), Arguments.of("Map {\"_v\":\"yes\"} → TRUE", mapOf("_v", "yes"), Boolean.TRUE), Arguments.of("Map {\"value\":null} → null", mapOf("value", null), null), Arguments.of("Map {\"value\":0, \"_v\":1} → TRUE (shortcut wins)", mapOf("value", 0, "_v", 1), Boolean.TRUE));
    }

    /* ---------- Non‑String, Non‑Number Types ---------- */
    private static Stream<Arguments> nonStringNumberProvider() {
        return Stream.of(Arguments.of("java.util.Date instance → null", new Date(), null), Arguments.of("java.util.ArrayList instance → null", new ArrayList<>(), null), Arguments.of("AtomicBoolean(true) → TRUE", new AtomicBoolean(true), Boolean.TRUE), Arguments.of("AtomicInteger(0) → FALSE", new AtomicInteger(0), Boolean.FALSE));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void validConversions(String description, Object from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCaseProvider")
    void edgeCases(String description, Object from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void invalidConversions(String description, Object from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("caseSensitivityProvider")
    void caseSensitivity(String description, String from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericStringProvider")
    void numericStringBeyondZeroOne(String description, String from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapEdgeCaseProvider")
    void mapEdgeCases(String description, Map<String, Object> map, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(map, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonStringNumberProvider")
    void nonStringNumberTypes(String description, Object from, Boolean expected) {
        Boolean actual = MapConversions.toBoolean(from, converter);
        assertEquals(expected, actual, description);
    }

    /* ---------- Exception Scenarios ---------- */
    @Test
    void nullConverterThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toBoolean("true", null));
    }

    @Test
    void throwingMapPropagatesRuntimeException() {
        Map<String, Object> throwingMap = new ThrowingMap();
        assertThrows(RuntimeException.class, () -> MapConversions.toBoolean(throwingMap, converter));
    }

    @Test
    void arraySourceThrowsIllegalArgumentException() {
        boolean[] array = new boolean[] { true };
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBoolean(array, converter));
    }

    /* ---------- Individual Simple Tests (kept from original file) ---------- */
    @Test
    void toBooleanFromMapWithValueKey_booleanTrue() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, true);
        Boolean result = MapConversions.toBoolean(map, converter);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void toBooleanFromMapWithValueKey_stringTrue() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "true");
        Boolean result = MapConversions.toBoolean(map, converter);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void toBooleanFromMapWithVKey_stringFalse() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, "false");
        Boolean result = MapConversions.toBoolean(map, converter);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBooleanFromPlainBoolean() {
        Boolean source = Boolean.FALSE;
        Boolean result = MapConversions.toBoolean(source, converter);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBooleanFromNullReturnsNull() {
        Boolean result = MapConversions.toBoolean(null, converter);
        assertNull(result);
    }

    @Test
    void toBooleanFromMapMissingKeysReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "true");
        Boolean result = MapConversions.toBoolean(map, converter);
        assertNull(result);
    }

    @Test
    void toBooleanFromMapWithNonBooleanValue() {
        Map<String, Object> map = new HashMap<>();
        // Integer 1 should be interpreted as true
        map.put(MapConversions.VALUE, 1);
        Boolean result = MapConversions.toBoolean(map, converter);
        assertEquals(Boolean.TRUE, result);
    }
}
