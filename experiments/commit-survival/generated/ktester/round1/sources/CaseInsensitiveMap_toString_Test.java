package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_toString_Test {

    /**
     * Instance under test, created fresh for each test method.
     */
    private CaseInsensitiveMap<String, Integer> ciMap;

    /**
     * Simple source map used for constructor‑with‑source tests.
     */
    private Map<String, Integer> sourceMap;

    /**
     * Key whose {@code toString()} throws a {@link RuntimeException}.
     */
    private static final class BadToStringKey {

        @Override
        public String toString() {
            throw new RuntimeException("Bad key toString");
        }
    }

    /**
     * Value whose {@code toString()} throws a {@link RuntimeException}.
     */
    private static final class BadToStringValue {

        @Override
        public String toString() {
            throw new RuntimeException("Bad value toString");
        }
    }

    /**
     * Map implementation whose {@code toString()} deliberately throws.
     */
    private static final class ThrowingMap<K, V> implements Map<K, V> {

        @Override
        public String toString() {
            throw new UnsupportedOperationException("Map toString broken");
        }

        // Minimal stub implementations – only those needed for compilation.
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor which creates an internal LinkedHashMap.
        ciMap = new CaseInsensitiveMap<>();
        // Populate a source map for the constructor that accepts a source map.
        sourceMap = new LinkedHashMap<>();
        sourceMap.put("Alpha", 10);
        sourceMap.put("Beta", 20);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse between tests.
        ciMap = null;
        sourceMap = null;
    }

    /**
     * Uses reflection to replace the private {@code map} field inside a
     * {@link CaseInsensitiveMap} instance.  This is needed for tests that want
     * to supply a specific backing map implementation (e.g., TreeMap,
     * ConcurrentSkipListMap, etc.).
     */
    private static void setInternalMap(CaseInsensitiveMap<?, ?> ciMap, Map<?, ?> backing) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(ciMap, backing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> simpleStringCases() {
        return Stream.of(// Empty map
        Arguments.of("Empty map", (Runnable) () -> {
            // nothing to do – map already empty
        }, "{}"), // Single entry
        Arguments.of("Single entry", (Runnable) () -> {
            ciMap.put("Key", 1);
        }, "{Key=1}"), // Multiple entries – order not important for HashMap based map
        Arguments.of("Multiple entries", (Runnable) () -> {
            ciMap.put("A", 1);
            ciMap.put("B", 2);
            ciMap.put("C", 3);
        }, "{A=1, B=2, C=3}"), // Null key and null value
        Arguments.of("Null key and null value", (Runnable) () -> {
            ciMap.put(null, null);
        }, "{null=null}"), // Removal reflects in toString
        Arguments.of("Removal reflects in toString", (Runnable) () -> {
            ciMap.put("X", 10);
            ciMap.put("Y", 20);
            ciMap.remove("Y");
        }, "{X=10}"), // Case‑insensitive key handling
        Arguments.of("Case‑insensitive key handling", (Runnable) () -> {
            ciMap.put("Key", 1);
            // replaces previous because of case‑insensitivity
            ciMap.put("key", 2);
        }, "{Key=2}"), // Null key with non‑null value
        Arguments.of("Null key with non‑null value", (Runnable) () -> {
            ciMap.put(null, 99);
        }, "{null=99}"), // Non‑null key with null value
        Arguments.of("Non‑null key with null value", (Runnable) () -> {
            ciMap.put("Missing", null);
        }, "{Missing=null}"), // PutAll operation reflects in toString
        Arguments.of("PutAll operation reflects in toString", (Runnable) () -> {
            Map<String, Integer> src = new LinkedHashMap<>();
            src.put("X", 1);
            src.put("Y", 2);
            src.put("Z", 3);
            ciMap.putAll(src);
        }, "{X=1, Y=2, Z=3}"), // Clear operation results in empty representation
        Arguments.of("Clear operation results in empty representation", (Runnable) () -> {
            ciMap.put("A", 1);
            ciMap.put("B", 2);
            ciMap.clear();
        }, "{}"), // Case‑insensitive removal
        Arguments.of("Case‑insensitive removal", (Runnable) () -> {
            ciMap.put("Key", 1);
            // now value = 2
            ciMap.put("key", 2);
            // should remove the entry
            ciMap.remove("KEY");
        }, "{}"));
    }

    @Test
    void toString_EmptyMap_ReturnsEmptyBraces() {
        // Given an empty CaseInsensitiveMap (already set up in @BeforeEach)
        // When
        String result = ciMap.toString();
        // Then
        assertEquals("{}", result, "toString() of an empty map should be {}");
    }

    @Test
    void toString_WithEntries_ReturnsUnderlyingMapString() {
        // Given a map with a few entries, using mixed‑case keys
        ciMap.put("FirstKey", 1);
        ciMap.put("secondkey", 2);
        ciMap.put("THIRDKEY", 3);
        // When
        String result = ciMap.toString();
        // Then
        // The underlying LinkedHashMap preserves insertion order and original case.
        assertEquals("{FirstKey=1, secondkey=2, THIRDKEY=3}", result);
    }

    @Test
    void toString_AfterRemoval_ReflectsCurrentState() {
        // Given a map with entries
        ciMap.put("One", 100);
        ciMap.put("Two", 200);
        ciMap.put("Three", 300);
        // When an entry is removed
        ciMap.remove("Two");
        String result = ciMap.toString();
        // Then the string reflects the removal
        assertEquals("{One=100, Three=300}", result);
    }

    @Test
    void toString_ConstructedFromSourceMap_MatchesSourceString() {
        // Given a source map (already prepared in @BeforeEach)
        // When constructing a CaseInsensitiveMap from the source map
        CaseInsensitiveMap<String, Integer> fromSource = new CaseInsensitiveMap<>(sourceMap);
        String result = fromSource.toString();
        // Then the string representation matches that of the source map
        assertEquals(sourceMap.toString(), result);
    }

    @Test
    void toString_CaseInsensitiveBehaviourDoesNotAffectString() {
        // Given keys that differ only by case
        ciMap.put("Case", 1);
        // second put replaces the first because of case‑insensitivity
        ciMap.put("case", 2);
        // When
        String result = ciMap.toString();
        // Then the map contains only one entry, and its string reflects the stored key
        // The implementation retains the case of the *first* key inserted.
        assertEquals("{Case=2}", result);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleStringCases")
    void toString_SimpleScenarios(String description, Runnable setup, String expected) {
        // Arrange
        setup.run();
        // Act
        String result = ciMap.toString();
        // Assert
        // For the “Multiple entries” case the order is deterministic because the
        // default backing map is a LinkedHashMap (insertion order).  For safety we
        // compare the strings directly; if a different map implementation is used,
        // a dedicated test (see later) will verify ordering.
        assertEquals(expected, result, description);
    }

    @Test
    void toString_UnderlyingLinkedHashMapPreservesInsertionOrder() {
        // LinkedHashMap is the default, but we explicitly set it to be clear.
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);
        setInternalMap(ciMap, linked);
        assertEquals("{first=1, second=2, third=3}", ciMap.toString(), "LinkedHashMap should preserve insertion order");
    }

    @Test
    void toString_UnderlyingConcurrentHashMapWorks() {
        Map<String, Integer> concurrent = new ConcurrentHashMap<>();
        concurrent.put("B", 2);
        setInternalMap(ciMap, concurrent);
        // The string representation of a ConcurrentHashMap does not guarantee order.
        // We therefore assert that it contains the expected entry substring.
        String result = ciMap.toString();
        assertTrue(result.contains("B=2"));
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    void toString_TreeMapOrdersKeysAlphabetically() {
        Map<String, Integer> tree = new TreeMap<>();
        tree.put("c", 3);
        tree.put("a", 1);
        tree.put("b", 2);
        setInternalMap(ciMap, tree);
        assertEquals("{a=1, b=2, c=3}", ciMap.toString(), "TreeMap should sort keys alphabetically");
    }

    @Test
    void toString_ConcurrentSkipListMapOrdersNumericKeys() {
        Map<Integer, String> skip = new ConcurrentSkipListMap<>();
        skip.put(20, "twenty");
        skip.put(10, "ten");
        skip.put(30, "thirty");
        // Need a raw CaseInsensitiveMap because key type is Integer (non‑String)
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        setInternalMap(intMap, skip);
        assertEquals("{10=ten, 20=twenty, 30=thirty}", intMap.toString(), "ConcurrentSkipListMap should sort numeric keys");
    }

    @Test
    void toString_WeakHashMapBackingMap() {
        Map<String, String> weak = new WeakHashMap<>();
        weak.put("temp", "alive");
        // Use raw type to avoid generic mismatch
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> rawMap = new CaseInsensitiveMap<>();
        setInternalMap(rawMap, weak);
        assertEquals("{temp=alive}", rawMap.toString(), "WeakHashMap should expose its entries via toString");
    }

    @Test
    void toString_NullUnderlyingMap_throwsNullPointerException() throws Exception {
        // Create an instance via the default constructor and then nullify the internal map.
        setInternalMap(ciMap, null);
        assertThrows(NullPointerException.class, ciMap::toString, "Calling toString() when the internal map is null should NPE");
    }

    @Test
    void toString_KeyToStringThrows_throwsRuntimeException() {
        // Use a raw map because the key type is not String.
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<BadToStringKey, Integer> map = new CaseInsensitiveMap<>();
        map.put(new BadToStringKey(), 1);
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from key.toString() must propagate");
        assertEquals("Bad key toString", ex.getMessage());
    }

    @Test
    void toString_ValueToStringThrows_throwsRuntimeException() {
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<Integer, BadToStringValue> map = new CaseInsensitiveMap<>();
        map.put(1, new BadToStringValue());
        RuntimeException ex = assertThrows(RuntimeException.class, map::toString, "Exception from value.toString() must propagate");
        assertEquals("Bad value toString", ex.getMessage());
    }

    @Test
    void toString_UnderlyingMapToStringThrows_throwsUnsupportedOperationException() {
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setInternalMap(map, new ThrowingMap<>());
        assertThrows(UnsupportedOperationException.class, map::toString, "Exception from backing map's toString() must propagate");
    }

    @Test
    void toString_ConcurrentModificationDuringToString_throwsConcurrentModificationException() throws Exception {
        // Use a plain HashMap (non‑concurrent) as the backing map.
        Map<Integer, Integer> hash = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            hash.put(i, i);
        }
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<Integer, Integer> map = new CaseInsensitiveMap<>();
        setInternalMap(map, hash);
        // Signal when the main thread has started the toString call.
        CountDownLatch startLatch = new CountDownLatch(1);
        // Signal when the modifier thread has finished its work.
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean exceptionSeen = new AtomicBoolean(false);
        Thread modifier = new Thread(() -> {
            try {
                // wait until iteration begins
                startLatch.await();
                // Continuously modify the map while the main thread is iterating.
                for (int i = 1000; i < 2000; i++) {
                    hash.put(i, i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        modifier.start();
        // Main thread: invoke toString while the modifier is running.
        try {
            // allow modifier to start
            startLatch.countDown();
            assertThrows(ConcurrentModificationException.class, map::toString, "HashMap.toString() should detect concurrent modification");
            exceptionSeen.set(true);
        } finally {
            // Ensure the modifier thread finishes before the test exits.
            doneLatch.await();
        }
        assertTrue(exceptionSeen.get(), "Expected ConcurrentModificationException was not thrown");
    }
}
