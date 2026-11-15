package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBoolean_Test {

    /**
     * Shared Converter instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared default converter for normal test cases.
     */
    private static Converter defaultConverter;

    /**
     * A {@link Converter} that always throws {@link IllegalArgumentException}.
     */
    private static class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> to) {
            throw new IllegalArgumentException("Converter forced failure");
        }
    }

    /**
     * A {@link Map} whose {@code entrySet()} throws {@link UnsupportedOperationException}.
     */
    private static class BrokenMap implements Map<String, Object> {

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException("entrySet not supported");
        }

        // All other methods delegate to an empty HashMap (not used in the test).
        private final Map<String, Object> delegate = new HashMap<>();

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
        public Object get(Object key) {
            return delegate.get(key);
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
        public void putAll(Map<? extends String, ? extends Object> m) {
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
    }

    /**
     * An object whose {@code toString()} throws a {@link RuntimeException}.
     */
    private static class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("Bad toString");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
        defaultConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Allow GC to reclaim the Converter; not strictly necessary but keeps the pattern symmetrical.
        converter = null;
    }

    private static Stream<Arguments> normalCases() {
        return Stream.of(// null input
        Arguments.of(null, null, null), // direct Boolean
        Arguments.of(Boolean.TRUE, null, true), Arguments.of(Boolean.FALSE, null, false), // String values (case‑insensitive, trimmed)
        Arguments.of("true", null, true), Arguments.of("FALSE", null, false), Arguments.of("yes", null, true), Arguments.of("no", null, false), Arguments.of("  true  ", null, true), Arguments.of("TrUe", null, true), Arguments.of("  FALSE  ", null, false), Arguments.of("YES", null, true), Arguments.of("No", null, false), // numeric values
        Arguments.of(1, null, true), Arguments.of(0, null, false), Arguments.of(2, null, true), Arguments.of(-1, null, true), Arguments.of(123L, null, true), Arguments.of(0L, null, false), // numeric strings
        Arguments.of("1", null, true), Arguments.of("0", null, false), // BigInteger / BigDecimal
        Arguments.of(new BigInteger("0"), null, false), Arguments.of(new BigInteger("42"), null, true), Arguments.of(new BigDecimal("0.0"), null, false), Arguments.of(new BigDecimal("-3.14"), null, true), // maps with recognised keys
        Arguments.of(mapOf(MapConversions.VALUE, true), null, true), Arguments.of(mapOf(MapConversions.VALUE, false), null, false), Arguments.of(mapOf(MapConversions._v, true), null, true), Arguments.of(mapOf(MapConversions._v, false), null, false), Arguments.of(mapOf(MapConversions.VALUE, "yes"), null, true), Arguments.of(mapOf(MapConversions._v, "no"), null, false), Arguments.of(mapOf(MapConversions.VALUE, "  yes  "), null, true), Arguments.of(mapOf(MapConversions._v, "NO"), null, false), Arguments.of(mapOf(MapConversions.VALUE, 0), null, false), Arguments.of(mapOf(MapConversions._v, 5), null, true), // nested maps
        Arguments.of(mapOf(MapConversions.VALUE, mapOf(MapConversions.VALUE, false)), null, false), Arguments.of(mapOf(MapConversions._v, mapOf(MapConversions._v, "yes")), null, true), // unsupported types → null
        Arguments.of(List.of(true), null, null), Arguments.of("", null, null), Arguments.of("   ", null, null), Arguments.of("randomString", null, null));
    }

    /**
     * Convenience method to create a mutable map with a single entry.
     */
    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(key, value);
        return m;
    }

    /**
     * Creates a map that contains itself under the recognised key, causing infinite recursion.
     */
    private static Map<String, Object> createCyclicMap() {
        Map<String, Object> outer = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        outer.put(MapConversions.VALUE, inner);
        // cycle
        inner.put(MapConversions.VALUE, outer);
        return outer;
    }

    @Test
    void toBoolean_givenBoolean_returnsSameValue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Boolean result = MapConversions.toBoolean(source, converter);
        // THEN
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void toBoolean_givenStringTrue_returnsTrue() {
        // GIVEN
        String source = "true";
        // WHEN
        Boolean result = MapConversions.toBoolean(source, converter);
        // THEN
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void toBoolean_givenStringFalse_returnsFalse() {
        // GIVEN
        String source = "false";
        // WHEN
        Boolean result = MapConversions.toBoolean(source, converter);
        // THEN
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBoolean_givenMapWithValueKey_returnsCorrespondingBoolean() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // key "value" as defined in MapConversions
        map.put(MapConversions.VALUE, Boolean.FALSE);
        // WHEN
        Boolean result = MapConversions.toBoolean(map, converter);
        // THEN
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBoolean_givenMapWithStringValue_returnsParsedBoolean() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "true");
        // WHEN
        Boolean result = MapConversions.toBoolean(map, converter);
        // THEN
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void toBoolean_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Boolean result = MapConversions.toBoolean(source, converter);
        // THEN
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={2}")
    @MethodSource("normalCases")
    void toBoolean_normalCases(Object from, Converter converter, Boolean expected) {
        // Use the supplied converter if not null, otherwise the shared default.
        Converter conv = converter != null ? converter : defaultConverter;
        Boolean result = MapConversions.toBoolean(from, conv);
        assertEquals(expected, result);
    }

    @Test
    void toBoolean_mapValueNull_throwsNullPointerException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, null);
        assertThrows(NullPointerException.class, () -> MapConversions.toBoolean(map, defaultConverter));
    }

    @Test
    void toBoolean_converterThrowsIllegalArgumentException() {
        Converter throwingConverter = new ThrowingConverter();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBoolean("true", throwingConverter));
    }

    @Test
    void toBoolean_brokenMap_throwsUnsupportedOperationException() {
        Map<String, Object> broken = new BrokenMap();
        assertThrows(UnsupportedOperationException.class, () -> MapConversions.toBoolean(broken, defaultConverter));
    }

    @Test
    void toBoolean_badToString_throwsRuntimeException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, new BadToString());
        assertThrows(RuntimeException.class, () -> MapConversions.toBoolean(map, defaultConverter));
    }

    @Test
    void toBoolean_cyclicMap_throwsStackOverflowError() {
        Map<String, Object> cyclic = createCyclicMap();
        assertThrows(StackOverflowError.class, () -> MapConversions.toBoolean(cyclic, defaultConverter));
    }
}
