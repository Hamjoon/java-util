package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_pollFirstEntry_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Runs before each test – initialise a clean map instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /**
     * Runs after each test – clear the reference to help GC (not strictly required).
     */
    @AfterEach
    void teardownAfterEach() {
        if (map != null) {
            map.clear();
        }
        map = null;
    }

    private static Stream<Arguments> basicScenarios() {
        return Stream.of(Arguments.of(Collections.emptyMap(), null, "Empty map should return null"), Arguments.of(Map.of("apple", 10), new AbstractMap.SimpleEntry<>("apple", 10), "Single entry map should return that entry"), Arguments.of(Map.of("beta", 2, "alpha", 1, "gamma", 3), new AbstractMap.SimpleEntry<>("alpha", 1), "Multiple entries – smallest key (alpha) should be returned"));
    }

    /**
     * Verify that {@code pollFirstEntry()} returns the smallest (according to natural order)
     * entry, removes it from the map and never returns a {@code null} key before other keys.
     */
    @Test
    void pollFirstEntryReturnsAndRemovesSmallestEntry() {
        // given
        map.put("b", 2);
        map.put("c", 3);
        // smallest key
        map.put("a", 1);
        // null key is considered last
        map.put(null, 99);
        // when
        Entry<String, Integer> first = map.pollFirstEntry();
        // then
        assertNotNull(first, "pollFirstEntry should return a non‑null entry when map is non‑empty");
        assertEquals("a", first.getKey(), "The smallest key should be returned");
        assertEquals(1, first.getValue());
        // The returned entry must have been removed from the map
        assertNull(map.get("a"), "The entry returned by pollFirstEntry must be removed from the map");
        assertEquals(3, map.size(), "Map size should be reduced by one");
    }

    /**
     * When the map contains only a {@code null} key, {@code pollFirstEntry()} must return that entry.
     */
    @Test
    void pollFirstEntryReturnsNullKeyWhenItIsTheOnlyEntry() {
        // given
        map.put(null, 42);
        // when
        Entry<String, Integer> entry = map.pollFirstEntry();
        // then
        assertNotNull(entry, "Entry should be returned when map contains a null key");
        assertNull(entry.getKey(), "Key of the returned entry must be null");
        assertEquals(42, entry.getValue());
        // map must now be empty
        assertTrue(map.isEmpty(), "Map should be empty after polling the only entry");
    }

    /**
     * When the map is empty, {@code pollFirstEntry()} must return {@code null}.
     */
    @Test
    void pollFirstEntryOnEmptyMapReturnsNull() {
        // given – map is freshly created and therefore empty
        // when
        Entry<String, Integer> entry = map.pollFirstEntry();
        // then
        assertNull(entry, "pollFirstEntry should return null for an empty map");
    }

    /**
     * Verify that after polling the first entry, the map’s ordering for subsequent calls
     * remains correct (i.e., the next smallest key is returned).
     */
    @Test
    void pollFirstEntryMultipleTimesPreservesOrdering() {
        // given
        map.put("x", 24);
        map.put("m", 13);
        map.put("a", 1);
        map.put("z", 26);
        // first poll – should return "a"
        Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("a", first.getKey());
        // second poll – should return "m"
        Entry<String, Integer> second = map.pollFirstEntry();
        assertEquals("m", second.getKey());
        // third poll – should return "x"
        Entry<String, Integer> third = map.pollFirstEntry();
        assertEquals("x", third.getKey());
        // final poll – should return "z"
        Entry<String, Integer> fourth = map.pollFirstEntry();
        assertEquals("z", fourth.getKey());
        // map should now be empty
        assertTrue(map.isEmpty());
    }

    @Test
    void pollFirstEntryReturnsSingleEntry() {
        map.put("apple", 10);
        Map.Entry<String, Integer> entry = map.pollFirstEntry();
        assertNotNull(entry, "Entry must be non‑null when map contains one element");
        assertEquals("apple", entry.getKey());
        assertEquals(10, entry.getValue());
        assertTrue(map.isEmpty(), "Map should be empty after polling its only entry");
    }

    @Test
    void pollFirstEntryReturnsSmallestKeyInNaturalOrder() {
        map.put("beta", 2);
        map.put("gamma", 3);
        // smallest according to natural order
        map.put("alpha", 1);
        Map.Entry<String, Integer> entry = map.pollFirstEntry();
        assertNotNull(entry);
        assertEquals("alpha", entry.getKey());
        assertEquals(1, entry.getValue());
    }

    @Test
    void pollFirstEntrySkipsNullKeyWhenOtherKeysExist() {
        // According to the library contract, null keys are ordered last in the
        // ascending view. Therefore the first poll should return the smallest
        // non‑null key.
        map.put(null, 99);
        map.put("beta", 2);
        map.put("alpha", 1);
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("alpha", first.getKey());
        // second poll should now return "beta"
        Map.Entry<String, Integer> second = map.pollFirstEntry();
        assertEquals("beta", second.getKey());
        // third poll finally returns the null key
        Map.Entry<String, Integer> third = map.pollFirstEntry();
        assertNull(third.getKey());
        assertEquals(99, third.getValue());
    }

    @Test
    void pollFirstEntryRemovesReturnedEntryAndPreservesOrder() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        // first poll – alpha
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("alpha", first.getKey());
        assertEquals(2, map.size());
        assertNull(map.get("alpha"));
        // second poll – beta
        Map.Entry<String, Integer> second = map.pollFirstEntry();
        assertEquals("beta", second.getKey());
        assertEquals(1, map.size());
        assertNull(map.get("beta"));
    }

    @Test
    void pollFirstEntryRespectsCustomComparator() {
        // Comparator: shorter strings first, then natural order
        Comparator<String> lengthThenNatural = Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());
        // Use the constructor that accepts a comparator (available in the library)
        map = new ConcurrentNavigableMapNullSafe<>(lengthThenNatural);
        map.put("bbb", 200);
        // length 2 – should be first
        map.put("aa", 100);
        // length 1 – actually shortest, so will become first
        map.put("c", 300);
        // First poll – "c" (length 1)
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("c", first.getKey());
        assertEquals(300, first.getValue());
        // Second poll – "aa" (length 2)
        Map.Entry<String, Integer> second = map.pollFirstEntry();
        assertEquals("aa", second.getKey());
        assertEquals(100, second.getValue());
        // Remaining entry should be "bbb"
        assertEquals("bbb", map.firstKey());
    }

    @Test
    void pollFirstEntryHandlesNullValue() {
        map.put("delta", null);
        map.put("epsilon", 5);
        Map.Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("delta", first.getKey());
        assertNull(first.getValue(), "Value should be null");
        // second poll returns the next entry
        Map.Entry<String, Integer> second = map.pollFirstEntry();
        assertEquals("epsilon", second.getKey());
        assertEquals(5, second.getValue());
    }

    @Test
    void pollFirstEntryIsThreadSafeWhenCalledFromMultipleThreads() throws Exception {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Map.Entry<String, Integer>> task1 = () -> map.pollFirstEntry();
            Callable<Map.Entry<String, Integer>> task2 = () -> map.pollFirstEntry();
            Future<Map.Entry<String, Integer>> f1 = executor.submit(task1);
            Future<Map.Entry<String, Integer>> f2 = executor.submit(task2);
            Map.Entry<String, Integer> e1 = f1.get();
            Map.Entry<String, Integer> e2 = f2.get();
            // Collect the two keys we obtained; order is not important for the assertion
            Set<String> keys = new HashSet<>(Arrays.asList(e1.getKey(), e2.getKey()));
            assertTrue(keys.containsAll(Set.of("first", "second")), "The two polls should have returned \"first\" and \"second\" (order may vary)");
            // After two removals, only one entry should remain
            assertEquals(1, map.size());
            assertEquals("third", map.firstKey());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void pollFirstEntryThrowsWhenInternalMapIsNull() throws Exception {
        // Force the protected field 'internalMap' to null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.pollFirstEntry(), "Calling pollFirstEntry when internalMap is null should throw NPE");
    }

    @Test
    void pollFirstEntryThrowsWhenInternalMapHasWrongType() throws Exception {
        // Replace the internal map with a plain HashMap (not a ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.pollFirstEntry(), "A ClassCastException is expected when internalMap is not a ConcurrentSkipListMap");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicScenarios")
    void pollFirstEntryParameterized(Map<String, Integer> initialData, AbstractMap.SimpleEntry<String, Integer> expected, String description) {
        // Populate map
        initialData.forEach(map::put);
        Map.Entry<String, Integer> result = map.pollFirstEntry();
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expected.getKey(), result.getKey(), description);
            assertEquals(expected.getValue(), result.getValue(), description);
        }
    }
}
