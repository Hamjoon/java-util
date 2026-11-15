package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

// @ExtendWith(MockitoExtension.class)
class CompactMap_builder_Test {

    /**
     * Verify that the default builder creates a functional {@link CompactMap}
     * that behaves like a normal map.
     */
    @Test
    void defaultBuilderCreatesUsableMap() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        Assertions.assertTrue(map.isEmpty());
        Assertions.assertEquals(0, map.size());
        Assertions.assertNull(map.put("one", 1));
        Assertions.assertEquals(1, map.size());
        Assertions.assertFalse(map.isEmpty());
        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertTrue(map.containsKey("one"));
        Assertions.assertTrue(map.containsValue(1));
        Assertions.assertEquals(Integer.valueOf(1), map.remove("one"));
        Assertions.assertTrue(map.isEmpty());
    }

    /**
     * Builder with {@code insertionOrder()} must preserve the order in which
     * entries are added.
     */
    @Test
    void insertionOrderPreservesEntrySequence() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");
        List<String> observed = new ArrayList<>(map.keySet());
        List<String> expected = List.of("first", "second", "third");
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Builder with {@code sortedOrder()} must return keys in their natural
     * sorted order regardless of insertion order.
     */
    @Test
    void sortedOrderReturnsNaturallySortedKeys() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().build();
        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);
        List<String> observed = new ArrayList<>(map.keySet());
        List<String> expected = List.of("a", "b", "c");
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Builder with {@code caseSensitive(false)} must treat string keys
     * case‑insensitively.
     */
    @Test
    void caseInsensitiveKeysAreHandledCorrectly() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).build();
        map.put("Key", "value");
        Assertions.assertEquals("value", map.get("key"));
        Assertions.assertEquals("value", map.get("KEY"));
        Assertions.assertTrue(map.containsKey("kEy"));
        Assertions.assertEquals("value", map.remove("keY"));
        Assertions.assertTrue(map.isEmpty());
    }

    /**
     * Builder with a custom backing map type (e.g., {@link TreeMap}) should
     * produce a map whose internal storage is of that type.  The only reliable
     * way to observe this is to rely on behaviour that differs between map
     * implementations – for example, {@link TreeMap} sorts keys while
     * {@link LinkedHashMap} preserves insertion order.  Here we verify that
     * specifying {@code LinkedHashMap.class} together with {@code insertionOrder()}
     * yields insertion order semantics.
     */
    @Test
    void customMapTypeWithInsertionOrderWorks() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().mapType(LinkedHashMap.class).build();
        map.put("x", 24);
        map.put("y", 25);
        map.put("z", 26);
        List<String> observed = new ArrayList<>(map.keySet());
        List<String> expected = List.of("x", "y", "z");
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Builder with {@code singleValueKey(...)} should still behave like a normal
     * map when more than one entry is added.  The test simply ensures that the
     * configuration does not break normal map operations.
     */
    @Test
    void singleValueKeyConfigurationDoesNotBreakMapBehaviour() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().singleValueKey("id").build();
        map.put("id", "123");
        map.put("name", "Alice");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("123", map.get("id"));
        Assertions.assertEquals("Alice", map.get("name"));
    }

    /**
     * Verify that {@code CompactMap.builder()} returns a non‑null Builder instance.
     */
    @Test
    void builderReturnsNonNullInstance() {
        CompactMap.Builder<String, Integer> builder = CompactMap.<String, Integer>builder();
        Assertions.assertNotNull(builder);
    }

    /**
     * Ensure each call to {@code CompactMap.builder()} returns a distinct Builder object.
     */
    @Test
    void successiveCallsReturnDistinctBuilders() {
        CompactMap.Builder<String, Integer> b1 = CompactMap.<String, Integer>builder();
        CompactMap.Builder<String, Integer> b2 = CompactMap.<String, Integer>builder();
        Assertions.assertNotSame(b1, b2);
    }

    /**
     * Verify that a map built with the default builder behaves like a normal map
     * and uses the library's default configuration (insertion order, case‑sensitive).
     */
    @Test
    void defaultBuilderCreatesMapWithDefaultOptions() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        Assertions.assertTrue(map.isEmpty());
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(1), map.get("a"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("b"));
        List<String> keys = new ArrayList<>(map.keySet());
        Assertions.assertEquals(List.of("a", "b"), keys);
        Assertions.assertNull(map.get("A"));
    }

    /**
     * Builder configured with a custom compactSize should reflect that size via the
     * protected {@code compactSize()} method (accessed by reflection).
     */
    @Test
    void customCompactSizeIsReflectedInMap() throws Exception {
        int customSize = 128;
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(customSize).build();
        java.lang.reflect.Method compactSizeMethod = CompactMap.class.getDeclaredMethod("compactSize");
        compactSizeMethod.setAccessible(true);
        int reportedSize = (int) compactSizeMethod.invoke(map);
        Assertions.assertEquals(customSize, reportedSize);
    }

    /**
     * Builder must reject non‑positive compactSize values.
     */
    @Test
    void builderRejectsInvalidCompactSize() {
        CompactMap.Builder<String, String> builder = CompactMap.<String, String>builder();
        try {
            builder.compactSize(0);
            Assertions.fail("compactSize of 0 should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            builder.compactSize(-5);
            Assertions.fail("Negative compactSize should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Case‑insensitive configuration should make key look‑ups ignore case.
     */
    @Test
    void caseInsensitiveOptionWorks() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).build();
        map.put("Key", "value");
        Assertions.assertEquals("value", map.get("key"));
        Assertions.assertEquals("value", map.get("KEY"));
        Assertions.assertTrue(map.containsKey("kEy"));
        Assertions.assertEquals("value", map.remove("KeY"));
        Assertions.assertTrue(map.isEmpty());
    }

    /**
     * Sorted ordering should return keys in their natural order irrespective of insertion order.
     */
    @Test
    void sortedOrderingProducesNaturallySortedKeySet() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().build();
        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);
        List<String> observed = new ArrayList<>(map.keySet());
        Assertions.assertEquals(List.of("a", "b", "c"), observed);
    }

    /**
     * Reverse ordering should iterate keys in reverse of their natural order.
     */
    @Test
    void reverseOrderingIteratesInReverseNaturalOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<String> observed = new ArrayList<>(map.keySet());
        Assertions.assertEquals(List.of("c", "b", "a"), observed);
    }

    /**
     * Builder initialized with a source map should pre‑populate the CompactMap.
     */
    @Test
    void sourceMapPrepopulatesCompactMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sourceMap(source).build();
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(1), map.get("a"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("b"));
    }

    /**
     * Single‑value key optimization should expose the key via {@code getSingleValueKey()}
     * and behave as a map with exactly one entry.
     */
    @Test
    void singleKeyOptimizationExposesKeyAndBehavesCorrectly() throws Exception {
        CompactMap<String, String> map = CompactMap.<String, String>builder().singleValueKey("only").build();
        map.put("only", "single");
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals("single", map.get("only"));
        java.lang.reflect.Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        Object key = m.invoke(map);
        Assertions.assertEquals("only", key);
    }

    /**
     * Verify that different {@code mapType} options result in the expected
     * observable behaviour (unordered → HashMap, sorted → TreeMap, insertion → LinkedHashMap).
     */
    @Test
    void mapTypeVariationsYieldExpectedBehaviour() {
        // UNORDERED (default) – map works, order not asserted
        CompactMap<String, Integer> unordered = CompactMap.<String, Integer>builder().mapType(HashMap.class).build();
        unordered.put("x", 1);
        unordered.put("y", 2);
        Assertions.assertEquals(2, unordered.size());
        // SORTED – keys should be in natural order
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder().mapType(TreeMap.class).sortedOrder().build();
        sorted.put("b", 2);
        sorted.put("a", 1);
        List<String> sortedKeys = new ArrayList<>(sorted.keySet());
        Assertions.assertEquals(List.of("a", "b"), sortedKeys);
        // INSERTION – keys should preserve insertion order
        CompactMap<String, Integer> insertion = CompactMap.<String, Integer>builder().mapType(LinkedHashMap.class).insertionOrder().build();
        insertion.put("first", 1);
        insertion.put("second", 2);
        List<String> insertionKeys = new ArrayList<>(insertion.keySet());
        Assertions.assertEquals(List.of("first", "second"), insertionKeys);
    }

    /**
     * Ensure that concurrent calls to {@code CompactMap.builder()} from multiple threads
     * each receive an independent Builder instance without shared mutable state.
     */
    @Test
    void builderIsThreadSafeAndReturnsDistinctInstances() throws Exception {
        int threadCount = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            Callable<CompactMap.Builder<String, Integer>> task = () -> CompactMap.<String, Integer>builder();
            List<Future<CompactMap.Builder<String, Integer>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(exec.submit(task));
            }
            Set<CompactMap.Builder<String, Integer>> builderSet = new HashSet<>();
            for (Future<CompactMap.Builder<String, Integer>> f : futures) {
                builderSet.add(f.get(2, TimeUnit.SECONDS));
            }
            Assertions.assertEquals(threadCount, builderSet.size());
        } finally {
            exec.shutdownNow();
        }
    }
}