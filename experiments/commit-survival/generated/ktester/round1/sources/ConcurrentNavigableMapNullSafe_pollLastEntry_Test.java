package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_pollLastEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to guarantee isolation.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no accidental sharing between tests.
        map = null;
    }

    /**
     * Uses reflection to replace the {@code internalMap} field in the superclass.
     */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    private static Stream<Arguments> nonEmptyMapProvider() {
        return Stream.of(// case 1 – multiple entries, highest key "c"
        Arguments.of(Map.of("a", 1, "b", 2, "c", 3), "c", 3, Set.of("a", "b")), // case 2 – single entry
        Arguments.of(Map.of("42", 0), "42", 0, Collections.emptySet()), // case 3 – null key only
        Arguments.of(Collections.singletonMap(null, (Integer) null), null, null, Collections.emptySet()));
    }

    @Test
    void testPollLastEntryReturnsHighestNonNullKey() {
        // GIVEN
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        assertNotNull(last, "pollLastEntry should return an entry when map is non‑empty");
        assertEquals("c", last.getKey(), "Highest key according to natural ordering should be returned");
        assertEquals(3, last.getValue());
        // Entry must be removed.
        assertFalse(map.containsKey("c"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    void testPollLastEntryReturnsNullKeyWhenPresent() {
        // GIVEN
        map.put("x", 10);
        // null key is considered last according to class contract
        map.put(null, 99);
        map.put("y", 20);
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        assertNotNull(last, "pollLastEntry should return an entry when map contains a null key");
        assertNull(last.getKey(), "When a null key is present it should be returned as the last entry");
        assertEquals(99, last.getValue());
        // Verify that the null key entry has been removed but other entries remain.
        assertFalse(map.containsKey(null));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertEquals(2, map.size());
    }

    @Test
    void testPollLastEntryOnEmptyMapReturnsNull() {
        // GIVEN an empty map (setupBeforeEach already provides this)
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        assertNull(last, "pollLastEntry should return null when the map is empty");
    }

    /**
     * Retrieves the {@code internalMap} for verification (reflection).
     */
    @SuppressWarnings("unchecked")
    private static <K, V> ConcurrentSkipListMap<K, V> getInternalMap(ConcurrentNavigableMapNullSafe<K, V> target) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        return (ConcurrentSkipListMap<K, V>) f.get(target);
    }

    /**
     * Group: pollLastEntry - single entry map.
     */
    @Test
    void testPollLastEntrySingleEntry() {
        // GIVEN
        map.put("42", 0);
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        assertNotNull(last);
        assertEquals("42", last.getKey());
        assertEquals(0, last.getValue());
        assertTrue(map.isEmpty(), "Map should be empty after polling the sole entry");
    }

    /**
     * Group: pollLastEntry - custom comparator (reverse order).
     */
    @Test
    void testPollLastEntryWithReverseComparator() throws Exception {
        // Comparator that orders integers descending (largest natural becomes smallest according to comparator)
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        // Build a map with the custom comparator via reflection.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentSkipListMap<Integer, String> internal = new ConcurrentSkipListMap<>(reverse);
        setInternalMap(intMap, internal);
        // Populate directly into internal map to avoid comparator being applied by map.put (which would work anyway)
        internal.put(3, "three");
        internal.put(2, "two");
        // According to reverse comparator, key 1 is the "greatest"
        internal.put(1, "one");
        // WHEN
        Map.Entry<Integer, String> last = intMap.pollLastEntry();
        // THEN
        assertNotNull(last);
        assertEquals(1, last.getKey());
        assertEquals("one", last.getValue());
        // Remaining keys should be 2 and 3 (according to reverse order they are "lower")
        Set<Integer> remaining = intMap.keySet();
        assertEquals(Set.of(2, 3), remaining);
        assertEquals(2, intMap.size());
    }

    /**
     * Group: pollLastEntry - map containing a null key and another regular key where null is lowest.
     */
    @Test
    void testPollLastEntryNullKeyIsLowest() throws Exception {
        // Build map with natural ordering (null is considered highest by class contract,
        // but this test checks the scenario described in the JSON where null is lowest – we achieve it
        // by using a comparator that treats null as lowest).
        Comparator<String> nullLowest = (a, b) -> {
            if (a == null)
                return -1;
            if (b == null)
                return 1;
            return a.compareTo(b);
        };
        ConcurrentNavigableMapNullSafe<String, String> strMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentSkipListMap<String, String> internal = new ConcurrentSkipListMap<>(nullLowest);
        setInternalMap(strMap, internal);
        internal.put(null, "NULL");
        internal.put("a", "A");
        // WHEN
        Map.Entry<String, String> last = strMap.pollLastEntry();
        // THEN
        assertNotNull(last);
        assertEquals("a", last.getKey());
        assertEquals("A", last.getValue());
        // Null key should remain.
        assertTrue(strMap.containsKey(null));
        assertEquals(1, strMap.size());
    }

    /**
     * Group: returned entry is a snapshot – mutating it does not affect the map.
     */
    @Test
    void testPollLastEntryReturnedEntryIsSnapshot() {
        // GIVEN
        map.put("x", 10);
        map.put("y", 20);
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        assertNotNull(last);
        assertEquals("y", last.getKey());
        assertEquals(20, last.getValue());
        // Mutate the returned entry.
        last.setValue(999);
        // The map must still contain the original value for "x" and must not contain "y".
        assertEquals(1, map.size());
        assertTrue(map.containsKey("x"));
        assertEquals(10, map.get("x"));
        assertFalse(map.containsKey("y"));
    }

    /**
     * Group: internalMap is null – expect NullPointerException.
     */
    @Test
    void testPollLastEntryInternalMapNullThrowsNPE() throws Exception {
        // Set internalMap to null via reflection.
        setInternalMap(map, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> map.pollLastEntry(), "Calling pollLastEntry when internalMap is null should throw NPE");
    }

    /**
     * Group: internalMap of wrong type – expect ClassCastException.
     */
    @Test
    void testPollLastEntryInternalMapWrongTypeThrowsCCE() throws Exception {
        // Replace internalMap with a plain HashMap (does not implement ConcurrentSkipListMap).
        Map<Object, Object> wrongMap = new HashMap<>();
        setInternalMap(map, wrongMap);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> map.pollLastEntry(), "Casting a non‑ConcurrentSkipListMap internalMap should cause CCE");
    }

    /**
     * Group: comparator throws RuntimeException during pollLastEntry.
     */
    @Test
    void testPollLastEntryComparatorThrowsRuntimeException() throws Exception {
        // Comparator that throws IllegalArgumentException for any comparison.
        Comparator<Integer> exploding = (a, b) -> {
            throw new IllegalArgumentException("Comparator explosion");
        };
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentSkipListMap<Integer, String> internal = new ConcurrentSkipListMap<>(exploding);
        setInternalMap(intMap, internal);
        // Populate internal map directly (bypassing comparator checks on put).
        internal.put(1, "one");
        internal.put(2, "two");
        internal.put(3, "three");
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> intMap.pollLastEntry(), "The comparator exception should propagate as a RuntimeException");
        assertEquals("Comparator explosion", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] map={0}")
    @MethodSource("nonEmptyMapProvider")
    void testPollLastEntryJsonDriven(Map<String, Integer> initial, String expectedKey, Integer expectedValue, Set<String> remainingKeys) {
        // GIVEN
        initial.forEach(map::put);
        // WHEN
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // THEN
        if (expectedKey == null) {
            assertNotNull(last);
            assertNull(last.getKey());
            assertNull(last.getValue());
        } else {
            assertNotNull(last);
            assertEquals(expectedKey, last.getKey());
            assertEquals(expectedValue, last.getValue());
        }
        // Verify remaining keys
        Set<String> actualRemaining = map.keySet();
        assertEquals(remainingKeys, actualRemaining);
        assertEquals(remainingKeys.size(), map.size());
    }
}
