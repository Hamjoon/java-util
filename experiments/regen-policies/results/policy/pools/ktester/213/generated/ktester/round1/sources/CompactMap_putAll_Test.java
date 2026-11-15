package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactMap_putAll_Test {

    /**
     * The map instance under test.
     */
    private TestCompactMap<String, Integer> map;

    /**
     * Simple concrete subclass that lets us control the configuration.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final Map<K, V> backingMap;

        // counts calls to {@code put}
        private int putCallCount = 0;

        // records arguments
        private final List<Entry<K, V>> putEntries = new ArrayList<>();

        TestCompactMap(int compactSize, Map<K, V> backingMap) {
            this.compactSize = compactSize;
            this.backingMap = backingMap;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return backingMap;
        }

        @Override
        public V put(K key, V value) {
            putCallCount++;
            putEntries.add(new AbstractMap.SimpleEntry<>(key, value));
            return super.put(key, value);
        }

        int getPutCallCount() {
            return putCallCount;
        }

        List<Entry<K, V>> getPutEntries() {
            return Collections.unmodifiableList(putEntries);
        }
    }

    /**
     * Helper map whose iterator always throws {@link ConcurrentModificationException}.
     */
    private static class ConcurrentModificationMap<K, V> extends AbstractMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {

                        @Override
                        public boolean hasNext() {
                            // always claim there is a next element
                            return true;
                        }

                        @Override
                        public Entry<K, V> next() {
                            throw new ConcurrentModificationException("Simulated CME");
                        }
                    };
                }

                @Override
                public int size() {
                    // size is irrelevant for the test
                    return 1;
                }
            };
        }
    }

    /**
     * Create a fresh map before each test.
     */
//     @BeforeEach
    void setupBeforeEach() {
        // Use a small compact size so we can easily test transition to a backing map.
        // Backing map is a LinkedHashMap to preserve insertion order for deterministic assertions.
        map = new TestCompactMap<>(3, new LinkedHashMap<>());
    }

    /**
     * No special teardown required.
     */
//     @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared so that a failing test does not affect the next one.
        map.clear();
    }

    /**
     * Test that calling {@code putAll} with {@code null} does nothing.
     */
//     @Test
//     void putAll_NullMap_NoChange() {
//         map.put("a", 1);
//         assertEquals("pre‑condition: map should contain one entry", 1, map.size());
//         // should be a no‑op
//         map.putAll(null);
//         assertEquals("size must stay unchanged when null is passed", 1, map.size());
//         assertEquals(1, (int) map.get("a"));
//         assertEquals("put should not be invoked for a null argument", 0, map.getPutCallCount());
//     }

    /**
     * Test that calling {@code putAll} with an empty map does nothing.
     */
//     @Test
//     void putAll_EmptyMap_NoChange() {
//         map.put("x", 42);
//         assertEquals(1, map.size());
//         // no‑op
//         map.putAll(Collections.emptyMap());
//         assertEquals("size must stay unchanged for an empty argument map", 1, map.size());
//         assertEquals("put should not be invoked for an empty argument map", 0, map.getPutCallCount());
//     }

    /**
     * Basic functionality: entries are added and existing keys are overwritten.
     */
//     @Test
//     void putAll_AddAndOverwriteEntries() {
//         // initial content
//         map.put("one", 1);
//         map.put("two", 2);
//         assertEquals(2, map.size());
//         // source map contains a new key and an existing key with a different value
//         Map<String, Integer> source = new LinkedHashMap<>();
//         // overwrite
//         source.put("two", 22);
//         // new entry
//         source.put("three", 3);
//         map.putAll(source);
//         // verify final state
//         assertEquals("map should contain three distinct keys after putAll", 3, map.size());
//         assertEquals(1, (int) map.get("one"));
//         assertEquals("existing key should be overwritten", 22, (int) map.get("two"));
//         assertEquals(3, (int) map.get("three"));
//         // verify that put was called exactly once per entry in the source map
//         assertEquals("put should be invoked for each entry of the argument map", 2, map.getPutCallCount());
//         List<Map.Entry<String, Integer>> recorded = map.getPutEntries();
//         assertTrue(recorded.contains(new AbstractMap.SimpleEntry<>("two", 22)));
//         assertTrue(recorded.contains(new AbstractMap.SimpleEntry<>("three", 3)));
//     }

    /**
     * Ensure that {@code putAll} does not modify the source map.
     */
//     @Test
//     void putAll_DoesNotAlterSourceMap() {
//         Map<String, Integer> source = new HashMap<>();
//         source.put("alpha", 10);
//         source.put("beta", 20);
//         // capture a snapshot before the call
//         Map<String, Integer> before = new HashMap<>(source);
//         map.putAll(source);
//         // source map must remain unchanged
//         assertEquals("source map must not be mutated by putAll", before, source);
//     }

    /**
     * Verify that {@code putAll} works when the target map transitions from compact array to backing map.
     */
//     @Test
//     void putAll_TriggersBackingMapTransition() {
//         // The test map uses compactSize = 3. Adding three entries stays in compact mode.
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         assertEquals(3, map.size());
//         // Adding a fourth entry via putAll should cause the internal representation to switch
//         // to a standard Map (the backingMap we supplied). We cannot inspect the private field,
//         // but we can assert that all entries are present and that put was called for each new entry.
//         Map<String, Integer> extra = new HashMap<>();
//         extra.put("k4", 4);
//         extra.put("k5", 5);
//         map.putAll(extra);
//         assertEquals(5, map.size());
//         assertEquals(4, (int) map.get("k4"));
//         assertEquals(5, (int) map.get("k5"));
//         assertEquals("put should be invoked for each entry added via putAll", 2, map.getPutCallCount());
//     }

    /**
     * Single entry putAll.
     */
//     @Test
//     void putAll_SingleEntry() {
//         Map<String, Integer> src = Collections.singletonMap("key1", 100);
//         map.putAll(src);
//         assertEquals(1, map.size());
//         assertEquals(100, (int) map.get("key1"));
//         assertEquals(1, map.getPutCallCount());
//     }

    /**
     * Multiple entries putAll.
     */
//     @Test
//     void putAll_MultipleEntries() {
//         Map<String, Integer> src = new LinkedHashMap<>();
//         src.put("a", 1);
//         src.put("b", 2);
//         src.put("c", 3);
//         map.putAll(src);
//         assertEquals(3, map.size());
//         assertEquals(1, (int) map.get("a"));
//         assertEquals(2, (int) map.get("b"));
//         assertEquals(3, (int) map.get("c"));
//         assertEquals(3, map.getPutCallCount());
//     }

    /**
     * Overwrite existing keys across two putAll calls.
     */
//     @Test
//     void putAll_DuplicateKeys_Overwrite() {
//         // first putAll
//         Map<String, Integer> first = new LinkedHashMap<>();
//         first.put("x", 10);
//         first.put("y", 20);
//         map.putAll(first);
//         assertEquals(2, map.size());
//         assertEquals(10, (int) map.get("x"));
//         assertEquals(20, (int) map.get("y"));
//         assertEquals(2, map.getPutCallCount());
//         // second putAll with an overlapping key
//         Map<String, Integer> second = Collections.singletonMap("x", 99);
//         map.putAll(second);
//         assertEquals("size should stay the same after overwriting", 2, map.size());
//         assertEquals("value for key 'x' must be updated", 99, (int) map.get("x"));
//         assertEquals(20, (int) map.get("y"));
//         assertEquals("put should be called for the overwriting entry", 3, map.getPutCallCount());
//     }

    /**
     * Map containing a null key.
     */
//     @Test
//     void putAll_NullKey() {
//         Map<String, Integer> src = new HashMap<>();
//         src.put(null, 555);
//         map.putAll(src);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey(null));
//         assertEquals(555, (int) map.get(null));
//         assertEquals(1, map.getPutCallCount());
//     }

    /**
     * Map containing a null value.
     */
//     @Test
//     void putAll_NullValue() {
//         Map<String, Integer> src = new HashMap<>();
//         src.put("nullValKey", null);
//         map.putAll(src);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("nullValKey"));
//         assertNull(map.get("nullValKey"));
//         assertEquals(1, map.getPutCallCount());
//     }

    /**
     * Map containing both null key and null value.
     */
//     @Test
//     void putAll_NullKeyAndValue() {
//         Map<String, Integer> src = new HashMap<>();
//         src.put(null, null);
//         map.putAll(src);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey(null));
//         assertNull(map.get(null));
//         assertEquals(1, map.getPutCallCount());
//     }

    /**
     * Stress test with a map larger than the default compact size (100 entries).
     */
//     @Test
//     void putAll_LargeMap_Boundary() {
//         Map<String, Integer> large = new LinkedHashMap<>();
//         for (int i = 0; i < 100; i++) {
//             large.put("k" + i, i);
//         }
//         map.putAll(large);
//         assertEquals(100, map.size());
//         for (int i = 0; i < 100; i++) {
//             assertEquals(i, (int) map.get("k" + i));
//         }
//         assertEquals(100, map.getPutCallCount());
//     }

    /**
     * Map with a subclass (Date) as a key.
     */
//     @Test
//     void putAll_SubclassKey() {
//         Date dateKey = new Date(0L);
//         Map<Date, Integer> src = new HashMap<>();
//         src.put(dateKey, 12345);
//         // Need a CompactMap that accepts Date as key; we can reuse the same TestCompactMap with raw types.
//         @SuppressWarnings("unchecked")
//         TestCompactMap<Date, Integer> dateMap = new TestCompactMap<>(3, new LinkedHashMap<>());
//         dateMap.putAll(src);
//         assertEquals(1, dateMap.size());
//         assertTrue(dateMap.containsKey(dateKey));
//         assertEquals(12345, (int) dateMap.get(dateKey));
//         assertEquals(1, dateMap.getPutCallCount());
//     }

    /**
     * Verify that a ConcurrentModificationException thrown by the source map propagates.
     */
//     @Test
//     void putAll_IteratorThrowsConcurrentModificationException() {
//         Map<String, Integer> badMap = new ConcurrentModificationMap<>();
//         try {
//             map.putAll(badMap);
//             fail("Expected ConcurrentModificationException to be thrown");
//         } catch (ConcurrentModificationException e) {
//             // expected
//         }
//     }
}