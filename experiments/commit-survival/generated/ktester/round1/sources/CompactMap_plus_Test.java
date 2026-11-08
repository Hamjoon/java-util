package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_plus_Test {

    private CompactMap<String, Integer> map;

    /**
     * Minimal custom Map implementation used for the “CustomMap” test case.
     * The implementation deliberately throws {@link UnsupportedOperationException}
     * for all mutating operations because the map is never actually used –
     * {@code CompactMap.plus} throws before any interaction.
     */
    private static class CustomMap<K, V> implements Map<K, V> {

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
        // Create a fresh instance for every test method
        map = new CompactMap<>();
        // Populate the map with a few entries to verify that the state is unchanged after the call
        map.put("one", 1);
        map.put("two", 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the reference – helps GC when many tests are executed
        map = null;
    }

    /**
     * Supplies all argument variations described in the JSON test cases.
     */
    static Stream<Arguments> plusArgumentProvider() {
        return Stream.of(// ---- simple literals & collections ----
        Arguments.of("null", null), Arguments.of("empty HashMap", new HashMap<>()), Arguments.of("singletonMap", Collections.singletonMap("key", "value")), Arguments.of("String", "someString"), Arguments.of("Integer", 42), Arguments.of("new CompactMap", new CompactMap<>()), // ---- additional map implementations ----
        Arguments.of("empty immutable map", Collections.emptyMap()), Arguments.of("ConcurrentHashMap", new ConcurrentHashMap<>()), Arguments.of("TreeMap", new TreeMap<>()), Arguments.of("Hashtable", new Hashtable<>()), Arguments.of("Properties", new Properties()), Arguments.of("singletonMap(null,null)", Collections.singletonMap(null, null)), Arguments.of("populated HashMap (100 entries)", createPopulatedHashMap(100)), Arguments.of("List", Collections.singletonList("elem")), Arguments.of("int array", new int[] { 1, 2, 3 }), Arguments.of("plain Object", new Object()), // ---- user‑defined map ----
        Arguments.of("custom Map implementation", new CustomMap<>()), // ---- edge map types ----
        Arguments.of("EnumMap", new EnumMap<>(DayOfWeek.class)), Arguments.of("IdentityHashMap", new IdentityHashMap<>()), Arguments.of("WeakHashMap", new WeakHashMap<>()), Arguments.of("ConcurrentSkipListMap", new ConcurrentSkipListMap<>()), Arguments.of("unmodifiableMap", Collections.unmodifiableMap(new HashMap<>())), Arguments.of("StringBuilder", new StringBuilder("test")), Arguments.of("Optional", Optional.of("value")), Arguments.of("Date", new Date()), Arguments.of("UUID", UUID.randomUUID()), Arguments.of("AtomicReference", new AtomicReference<>("ref")), Arguments.of("emptySortedMap", Collections.emptySortedMap()), Arguments.of("emptyNavigableMap", Collections.emptyNavigableMap()), Arguments.of("multi‑dimensional int array", new int[][] { { 1, 2 }, { 3, 4 } }), Arguments.of("lambda Function<String,String>", (Function<String, String>) s -> s.toUpperCase()));
    }

    /**
     * Helper to create a HashMap with a given number of dummy entries.
     */
    private static Map<String, String> createPopulatedHashMap(int count) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < count; i++) {
            m.put("k" + i, "v" + i);
        }
        return m;
    }

    @Test
    void plus_ShouldThrowUnsupportedOperationException_WithExpectedMessage() {
        // GIVEN – a CompactMap instance already populated in @BeforeEach
        // WHEN / THEN – invoking plus must throw the expected exception
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.plus(new CompactMap<>()));
        assertEquals("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.", ex.getMessage(), "Exception message should match the specification");
    }

    @Test
    void plus_ShouldNotModifyOriginalMap_WhenExceptionIsThrown() {
        // GIVEN – capture the current state of the map
        int originalSize = map.size();
        // immutable snapshot
        var originalEntries = Map.of("one", 1, "two", 2);
        // WHEN – call plus and ignore the thrown exception
        assertThrows(UnsupportedOperationException.class, () -> map.plus(null));
        // THEN – the map must remain unchanged
        assertEquals(originalSize, map.size(), "Size must stay the same after plus");
        assertTrue(map.entrySet().containsAll(originalEntries.entrySet()), "All original entries must still be present");
    }

    /**
     * Parameterized test that supplies a large collection of argument types.
     * For each argument the method must throw the same {@link UnsupportedOperationException}
     * with the exact message defined in the implementation.
     */
    @ParameterizedTest(name = "{index}: plus({0}) throws UnsupportedOperationException")
    @MethodSource("plusArgumentProvider")
    void plus_ShouldThrowUnsupportedOperationException_ForVariousArguments(String description, Object right) {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.plus(right), () -> "plus(" + description + ") did not throw the expected exception");
        assertEquals("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.", ex.getMessage(), "Exception message must be identical for all argument types");
    }
}
