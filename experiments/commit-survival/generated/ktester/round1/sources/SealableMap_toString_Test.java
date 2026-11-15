package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_toString_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    private static final class KeyObj {

        private final String name;

        KeyObj(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Key(" + name + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof KeyObj && Objects.equals(name, ((KeyObj) o).name);
        }
    }

    private static final class ValueObj {

        private final String name;

        ValueObj(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Value(" + name + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ValueObj && Objects.equals(name, ((ValueObj) o).name);
        }
    }

    private static final class ThrowingMap<K, V> implements Map<K, V> {

        @Override
        public String toString() {
            throw new IllegalStateException("map toString failure");
        }

        // All other Map methods throw UnsupportedOperationException – not needed for this test
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<K> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<V> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class BadKey {

        @Override
        public String toString() {
            throw new RuntimeException("bad key");
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BadKey;
        }
    }

    private static final class BadValue {

        @Override
        public String toString() {
            throw new RuntimeException("bad value");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with deterministic data
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the template tidy
        map = null;
    }

    private static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(// empty map
        Arguments.of(new HashMap<>(), "{}", "An empty underlying map should produce an empty braces string when toString() is called."), // single entry
        Arguments.of(Map.of("a", 1), "{a=1}", "A map with a single entry should return the standard Map toString representation."), // LinkedHashMap preserves insertion order
        Arguments.of(new LinkedHashMap<String, Integer>() {

            {
                put("x", 10);
                put("y", 20);
                put("z", 30);
            }
        }, "{x=10, y=20, z=30}", "A map with multiple entries preserves insertion order in its toString() output (LinkedHashMap)."), // null key and null value
        Arguments.of(new HashMap<Object, Object>() {

            {
                put(null, "nullKey");
                put("nullValue", null);
            }
        }, "{null=nullKey, nullValue=null}", "When the underlying map contains a null key and/or null value, toString() should reflect the literal \"null\" for those positions."));
    }

    @Test
    void testToStringContainsAllEntriesWhenUnsealed() {
        String s = map.toString();
        assertNotNull(s, "toString should never return null");
        // The string representation of a map contains each entry in the form key=value
        assertTrue(s.contains("one=1"), "String should contain entry one=1");
        assertTrue(s.contains("two=2"), "String should contain entry two=2");
        assertTrue(s.contains("three=3"), "String should contain entry three=3");
        // Null key/value are rendered as null
        assertTrue(s.contains("null=null"), "String should contain entry null=null");
        // The string must start with '{' and end with '}'
        assertTrue(s.startsWith("{") && s.endsWith("}"), "String representation of a map should be wrapped in braces");
    }

    @Test
    void testToStringWhenSealed() {
        // Seal the map – the underlying data does not change, only mutability does
        sealedState = true;
        String s = map.toString();
        assertNotNull(s);
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.contains("three=3"));
        assertTrue(s.contains("null=null"));
    }

    @Test
    void testToStringOnEmptyMap() {
        // Create an empty map (different instance to avoid the data added in @BeforeEach)
        SealableMap<String, Integer> empty = new SealableMap<>(sealedSupplier);
        assertEquals("{}", empty.toString(), "An empty SealableMap should render as {}");
    }

    @Test
    void testToStringReflectsModificationsBeforeSealing() {
        // start from the default map created in @BeforeEach
        map.put("four", 4);
        map.remove("two");
        // seal after modifications
        sealedState = true;
        String s = map.toString();
        assertTrue(s.contains("four=4"));
        assertFalse(s.contains("two=2"));
    }

    @Test
    void testToStringDoesNotThrowWhenMapContainsComplexValues() {
        // Add a value that has its own toString implementation
        map.put("list", java.util.Arrays.asList(1, 2, 3));
        String s = map.toString();
        assertNotNull(s);
        assertTrue(s.contains("list=[1, 2, 3]"), "toString should delegate to the value's toString implementation");
    }

    /**
     * Replace the private {@code map} field inside {@link SealableMap} with the supplied
     * underlying map.  Used to inject custom maps for specific test scenarios.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void injectUnderlyingMap(SealableMap<K, V> sealableMap, Map<K, V> underlying) {
        try {
            Field f = SealableMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(sealableMap, underlying);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject underlying map", e);
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicScenariosProvider")
    void testToStringBasicScenarios(Map<String, Integer> underlying, String expected, String description) {
        // inject the custom underlying map
        injectUnderlyingMap(map, underlying);
        assertEquals(expected, map.toString(), description);
    }

    @Test
    void testToStringAfterPutAll() {
        // start with empty underlying map
        SealableMap<String, Integer> empty = new SealableMap<>(sealedSupplier);
        injectUnderlyingMap(empty, new HashMap<>());
        empty.putAll(Map.of("p", 5, "q", 6));
        assertEquals("{p=5, q=6}", empty.toString(), "putAll adds all entries from another map; toString() should reflect the combined entries.");
    }

    @Test
    void testToStringAfterClearOperation() {
        // underlying map with two entries
        Map<String, Integer> base = new HashMap<>() {

            {
                put("x", 10);
                put("y", 20);
            }
        };
        injectUnderlyingMap(map, base);
        map.clear();
        assertEquals("{}", map.toString(), "Calling clear() empties the map; toString() must return empty braces.");
    }

    @Test
    void testToStringReplaceExistingKey() {
        Map<String, Integer> base = new HashMap<>() {

            {
                put("k", 100);
            }
        };
        injectUnderlyingMap(map, base);
        map.put("k", 200);
        assertEquals("{k=200}", map.toString(), "Putting a value for an existing key overwrites the previous value; toString() shows the new value.");
    }

    @Test
    void testToStringSortedOrderWithTreeMap() {
        TreeMap<String, Integer> tree = new TreeMap<>() {

            {
                put("b", 2);
                put("a", 1);
                put("c", 3);
            }
        };
        injectUnderlyingMap(map, tree);
        assertEquals("{a=1, b=2, c=3}", map.toString(), "TreeMap sorts keys; toString() should reflect sorted order regardless of insertion order.");
    }

    @Test
    void testToStringCustomObjectKeysAndValues() {
        Map<Object, Object> base = new HashMap<>() {

            {
                put(new KeyObj("alpha"), new ValueObj("beta"));
            }
        };
        injectUnderlyingMap(map, base);
        assertEquals("{Key(alpha)=Value(beta)}", map.toString(), "Keys and values with overridden toString() should appear using their custom string representations.");
    }

    @Test
    void testToStringUnmodifiableUnderlyingMap() {
        Map<String, Integer> unmod = Collections.unmodifiableMap(Map.of("u", 9, "v", 10));
        injectUnderlyingMap(map, unmod);
        assertEquals("{u=9, v=10}", map.toString(), "SealableMap should delegate toString() correctly even when the wrapped map is unmodifiable.");
    }

    @Test
    void testToStringLargeMapSnapshot() {
        Map<Integer, Integer> large = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            large.put(i, i * i);
        }
        // Use a SealableMap with Integer keys/values for this test
        SealableMap<Integer, Integer> largeMap = new SealableMap<>(sealedSupplier);
        injectUnderlyingMap(largeMap, large);
        String s = largeMap.toString();
        assertTrue(s.startsWith("{0=0"), "Large map string should start with the first entry");
        assertTrue(s.endsWith("99=9801}"), "Large map string should end with the last entry");
    }

    @Test
    void testToStringNullUnderlyingMap() {
        // Construct a SealableMap and then inject a null underlying map
        SealableMap<String, Integer> nullMap = new SealableMap<>(sealedSupplier);
        injectUnderlyingMap(nullMap, null);
        assertThrows(NullPointerException.class, nullMap::toString, "If the SealableMap is constructed with a null underlying map, invoking toString() should throw NPE.");
    }

    @Test
    void testToStringUnderlyingMapThrows() {
        SealableMap<String, Integer> bad = new SealableMap<>(sealedSupplier);
        injectUnderlyingMap(bad, new ThrowingMap<>());
        IllegalStateException ex = assertThrows(IllegalStateException.class, bad::toString, "Exception from underlying map's toString() must propagate unchanged.");
        assertEquals("map toString failure", ex.getMessage());
    }

    @Test
    void testToStringKeyToStringThrows() {
        Map<Object, String> base = new HashMap<>() {

            {
                put(new BadKey(), "value");
            }
        };
        injectUnderlyingMap(map, base);
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "RuntimeException from a key's toString() should propagate.");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void testToStringValueToStringThrows() {
        Map<String, Object> base = new HashMap<>() {

            {
                put("key", new BadValue());
            }
        };
        injectUnderlyingMap(map, base);
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "RuntimeException from a value's toString() should propagate.");
        assertEquals("bad value", ex.getMessage());
    }
}
