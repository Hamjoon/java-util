package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_firstKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    class BrokenMaskMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected Object maskNullKey(String key) {
            // Incorrectly return null for any non‑null key.
            return null;
        }
    }

    class BrokenMaskMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

        @Override
        protected Object maskNullKey(String key) {
            // Incorrectly return null for any non‑null key.
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to avoid cross‑test contamination.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map explicitly (not strictly required because a new instance is created each time).
        map.clear();
    }

    /**
     * Sets the {@code internalMap} field of the supplied map instance via reflection.
     *
     * @param target the map whose internal map should be replaced
     * @param newMap the new map to assign (may be {@code null})
     */
    private static void setInternalMap(ConcurrentNavigableMapNullSafe<?, ?> target, ConcurrentMap<?, ?> newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    private static Stream<Arguments> basicScenarioProvider() {
        return Stream.of(// empty map – will actually throw; handled separately
        Arguments.of(Collections.emptyList(), null), Arguments.of(Collections.singletonList("a"), "a"), Arguments.of(Arrays.asList("b", "c", "a"), "a"), Arguments.of(Collections.singletonList(null), null));
    }

    @Test
    void firstKeyReturnsLowestNonNullKey() {
        // Arrange: insert keys in non‑sorted order, including a null key.
        map.put("b", 2);
        map.put("a", 1);
        map.put(null, 100);
        map.put("c", 3);
        // Act & Assert: the smallest (according to natural ordering) non‑null key should be returned.
        assertEquals("a", map.firstKey(), "firstKey should return the lowest non‑null key");
        // Remove the current first key and verify the next one is returned.
        map.remove("a");
        assertEquals("b", map.firstKey(), "After removing 'a', firstKey should be 'b'");
        // Remove all non‑null keys; only the null key remains.
        map.remove("b");
        map.remove("c");
        assertNull(map.firstKey(), "When only a null key is present, firstKey should return null");
    }

    @Test
    void firstKeyThrowsWhenMapIsEmpty() {
        // The map is empty at this point.
        assertThrows(NoSuchElementException.class, () -> map.firstKey(), "firstKey should throw NoSuchElementException on an empty map");
    }

    @Test
    void firstKeyConsidersNullKeyAsLastInAscendingOrder() {
        // Insert only a null key.
        map.put(null, 42);
        // Even though the map has a single entry, null is treated as the greatest key,
        // therefore firstKey should still return null (the only key present).
        assertNull(map.firstKey(), "When the only key is null, firstKey should return null");
    }

    /**
     * Retrieves the current {@code internalMap} for verification / manipulation.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> ConcurrentMap<K, V> getInternalMap(ConcurrentNavigableMapNullSafe<K, V> target) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        return (ConcurrentMap<K, V>) f.get(target);
    }

    @Test
    @DisplayName("firstKey() with a single non‑null key returns that key")
    void firstKeySingleEntry() {
        map.put("a", 1);
        assertEquals("a", map.firstKey(), "firstKey should return the only key present in the map");
    }

    @Test
    @DisplayName("firstKey() with multiple keys returns the smallest according to natural ordering")
    void firstKeyMultipleEntries() {
        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);
        assertEquals("a", map.firstKey(), "firstKey must return the lowest key (\"a\") among \"a\",\"b\",\"c\"");
    }

    @Test
    @DisplayName("firstKey() with only a null key returns null")
    void firstKeyOnlyNullKey() {
        map.put(null, 42);
        // According to the contract, null keys are considered last in the ascending view,
        // but they are still a valid key and should be returned (unmasked) when they are the only key.
        assertNull(map.firstKey(), "When the only key is null, firstKey should return null after unmasking");
    }

    @Test
    @DisplayName("firstKey() with null key mixed with regular keys returns the smallest non‑null key")
    void firstKeyNullMixedWithNonNull() {
        map.put(null, 100);
        map.put("b", 2);
        map.put("a", 1);
        assertEquals("a", map.firstKey(), "When null and non‑null keys exist, firstKey should return the smallest non‑null key");
    }

    @Test
    @DisplayName("firstKey() after pollFirstEntry() reflects the new smallest key")
    void firstKeyAfterPollFirstEntry() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Remove the smallest entry via pollFirstEntry()
        Map.Entry<String, Integer> removed = map.pollFirstEntry();
        assertNotNull(removed);
        assertEquals("a", removed.getKey());
        // Now the first key should be "b"
        assertEquals("b", map.firstKey(), "After removing the smallest entry, firstKey must return the next smallest key");
    }

    @Test
    @DisplayName("firstKey() after explicit removal of the smallest key")
    void firstKeyAfterExplicitRemoval() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.remove("a");
        assertEquals("b", map.firstKey(), "After removing \"a\", firstKey should be \"b\"");
    }

    @Test
    @DisplayName("firstKey() on a map that becomes empty after removing the sole null key")
    void firstKeyAfterRemovingOnlyNullKey() {
        map.put(null, 99);
        // pollFirstEntry() removes the null key (treated as the greatest key in ascending order)
        Map.Entry<String, Integer> removed = map.pollFirstEntry();
        assertNotNull(removed);
        assertNull(removed.getKey());
        // Now the map is empty – expect NoSuchElementException
        assertThrows(NoSuchElementException.class, map::firstKey, "After removing the only entry, firstKey should throw NoSuchElementException");
    }

    @Test
    @DisplayName("firstKey() works with Integer keys (natural ordering)")
    void firstKeyIntegerKeys() {
        // Use a map instance typed for Integer keys via raw type to avoid generic warnings.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, "ten");
        intMap.put(5, "five");
        intMap.put(20, "twenty");
        assertEquals(5, intMap.firstKey(), "firstKey must return the smallest Integer key (5)");
    }

    @Test
    @DisplayName("firstKey() with a reverse‑order comparator returns the highest key")
    void firstKeyWithReverseComparator() throws Exception {
        // Build a ConcurrentSkipListMap that orders strings in descending order.
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentSkipListMap<Object, Object> skipList = new ConcurrentSkipListMap<>(reverse);
        // Populate via reflection (bypass the null‑masking logic for simplicity)
        skipList.put("a", 1);
        skipList.put("b", 2);
        skipList.put("c", 3);
        setInternalMap(map, (ConcurrentMap<Object, Object>) skipList);
        // Because the underlying map uses reverse order, the "first" key according to that order
        // is the greatest element, i.e., "c".
        assertEquals("c", map.firstKey(), "With a reverse comparator, firstKey should return the highest key (\"c\")");
    }

    @Test
    @DisplayName("firstKey() throws NullPointerException when internalMap is null")
    void firstKeyWhenInternalMapIsNull() throws Exception {
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, map::firstKey, "If internalMap is null, firstKey should throw NullPointerException");
    }

    @Test
    @DisplayName("firstKey() throws ClassCastException when internalMap is not a ConcurrentSkipListMap")
    void firstKeyWhenInternalMapWrongType() throws Exception {
        // Replace internalMap with a plain ConcurrentHashMap.
        ConcurrentMap<Object, Object> hashMap = new ConcurrentHashMap<>();
        setInternalMap(map, hashMap);
        // The cast inside firstKey() will fail.
        assertThrows(ClassCastException.class, map::firstKey, "Casting a non‑ConcurrentSkipListMap internalMap should cause ClassCastException");
    }

    @Test
    @DisplayName("firstKey() throws ClassCastException when underlying map contains a key of incompatible type")
    void firstKeyWithCorruptedKeyType() throws Exception {
        // Obtain the real ConcurrentSkipListMap backing the map and inject an Integer key.
        ConcurrentMap<Object, Object> internal = getInternalMap(map);
        // The map expects String keys, but we insert an Integer directly.
        internal.put(42, 999);
        // The firstKey() call will attempt to unmask the Integer as a String and fail.
        assertThrows(ClassCastException.class, map::firstKey, "If the underlying map contains a key of the wrong type, firstKey should throw ClassCastException");
    }

    @Test
    @DisplayName("firstKey() propagates RuntimeException thrown by a faulty comparator")
    void firstKeyComparatorThrows() throws Exception {
        // Comparator that throws when comparing the value "bad".
        Comparator<String> exploding = (o1, o2) -> {
            if ("bad".equals(o1) || "bad".equals(o2)) {
                throw new IllegalArgumentException("boom");
            }
            return o1.compareTo(o2);
        };
        ConcurrentSkipListMap<Object, Object> skipList = new ConcurrentSkipListMap<>(exploding);
        skipList.put("good", 1);
        // insertion triggers comparator -> exception
        skipList.put("bad", 2);
        // The exception is thrown during the put; we need to catch it to continue.
        // Therefore we construct the map without the offending entry and then add it via reflection.
        skipList = new ConcurrentSkipListMap<>(exploding);
        skipList.put("good", 1);
        setInternalMap(map, (ConcurrentMap<Object, Object>) skipList);
        // Insert the problematic key directly into the internal map (bypassing comparator check)
        skipList.put("bad", 2);
        // Now calling firstKey() will cause the comparator to be invoked.
        assertThrows(IllegalArgumentException.class, map::firstKey, "Comparator that throws should cause firstKey to propagate the exception");
    }

    @Test
    @DisplayName("firstKey() throws NullPointerException when maskNullKey is overridden to return null")
    void firstKeyMaskNullKeyReturnsNull() {
        // Subclass that deliberately breaks the contract.
        class BrokenMaskMap extends ConcurrentNavigableMapNullSafe<String, Integer> {

            @Override
            protected Object maskNullKey(String key) {
                // Incorrectly return null for any non‑null key.
                return null;
            }
        }
        BrokenMaskMap broken = new BrokenMaskMap();
        broken.put("x", 1);
        // The internal unmaskNullKey will receive null and attempt to cast, leading to NPE.
        assertThrows(NullPointerException.class, broken::firstKey, "If maskNullKey returns null for a non‑null key, firstKey should eventually throw NPE");
    }

    @ParameterizedTest(name = "{index} => keys={0}, expected={1}")
    @MethodSource("basicScenarioProvider")
    @DisplayName("Parameterized basic firstKey scenarios")
    void parameterizedBasicFirstKey(List<String> keys, String expected) {
        // Populate the map with the supplied keys (value is irrelevant)
        for (String k : keys) {
            map.put(k, 1);
        }
        if (expected == null) {
            // Expected null (only possible when the only key is null)
            assertNull(map.firstKey());
        } else {
            assertEquals(expected, map.firstKey(), "firstKey did not match the expected value for keys " + keys);
        }
    }
}
