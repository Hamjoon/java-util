package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_keySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a map with a tiny compact size so we stay in the compact‑array
        // implementation for the first few entries.  The overridden methods keep
        // the test deterministic and independent from the builder‑generated classes.
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // small compact size for testing
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // When the map grows beyond the compact size we want a predictable
                // backing map – LinkedHashMap preserves insertion order.
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                // Use a non‑default single‑value key to avoid the special single‑entry
                // optimisation interfering with the test.
                return "id";
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test pollution.
        if (map != null) {
            map.clear();
        }
    }

    private static Stream<Arguments> provideRemoveCases() {
        return Stream.of(// existing key
        Arguments.of("beta", true), // absent key
        Arguments.of("nonexistent", false));
    }

    private static Stream<Arguments> provideRemoveAllCases() {
        return Stream.of(// removes existing keys
        Arguments.of(Arrays.asList("gamma", "delta"), true), // empty collection -> no change
        Arguments.of(Collections.emptyList(), false), // unknown keys -> no change
        Arguments.of(Arrays.asList("unknown1", "unknown2"), false));
    }

    private static Stream<Arguments> provideRetainAllCases() {
        return Stream.of(// removes missing keys
        Arguments.of(Arrays.asList("alpha", "epsilon"), true), // already matches (alpha,beta)
        Arguments.of(Arrays.asList("alpha", "beta", "epsilon"), false), // clears map
        Arguments.of(Collections.emptyList(), true));
    }

    @Test
    void testKeySetBasicOperations() {
        // Populate the map
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        Set<String> ks = map.keySet();
        // size() should delegate to map.size()
        assertEquals(3, ks.size(), "keySet size must equal map size");
        // contains() should delegate to map.containsKey()
        assertTrue(ks.contains("two"));
        assertFalse(ks.contains("four"));
        // remove() must delete the entry from the map and report change
        assertTrue(ks.remove("two"));
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());
        assertEquals(2, ks.size());
        // clear() must empty the map
        ks.clear();
        assertTrue(map.isEmpty(), "map should be empty after keySet.clear()");
        assertTrue(ks.isEmpty(), "keySet should be empty after clear()");
    }

    @Test
    void testKeySetRemoveAll() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        map.put("d", 40);
        Set<String> ks = map.keySet();
        // Attempt to remove a collection containing existing and non‑existing keys
        Collection<String> toRemove = Arrays.asList("b", "c", "x");
        assertTrue(ks.removeAll(toRemove), "removeAll should return true when any element is removed");
        // Verify that only the specified keys were removed
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("d"));
        assertEquals(2, map.size());
    }

    @Test
    void testKeySetRetainAll() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        map.put("delta", 4);
        Set<String> ks = map.keySet();
        // Retain only a subset CompactMap_keySet_Testkeys
        Collection<String> toRetain = Arrays.asList("beta", "delta", "nonexistent");
        assertTrue(ks.retainAll(toRetain), "retainAll should return true when the set changes");
        // Verify that only the retained keys remain
        assertTrue(map.containsKey("beta"));
        assertTrue(map.containsKey("delta"));
        assertFalse(map.containsKey("alpha"));
        assertFalse(map.containsKey("gamma"));
        assertEquals(2, map.size());
    }

    @Test
    void testKeySetIteratorRemove() {
        // Fill the map with several entries
        for (int i = 0; i < 4; i++) {
            map.put("k" + i, i);
        }
        assertEquals(4, map.size());
        Iterator<String> it = map.keySet().iterator();
        // Remove each element via the iterator
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        // After iterator removal the map must be empty
        assertTrue(map.isEmpty(), "Map should be empty after iterator.remove() on all keys");
        assertTrue(map.keySet().isEmpty(), "keySet should be empty after iterator removal");
    }

    // -----------------------------------------------------------------
    @Test
    void testKeySetContainsAndClear() {
        // contains true
        map.put("alpha", 100);
        assertTrue(map.keySet().contains("alpha"), "keySet should contain existing key \"alpha\"");
        // contains false
        assertFalse(map.keySet().contains("missing"), "keySet should not contain non‑existent key");
        // clear
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertTrue(map.keySet().isEmpty(), "keySet must be empty after clear()");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "remove key \"{0}\" expects {1}")
    @MethodSource("provideRemoveCases")
    void testKeySetRemove(String keyToRemove, boolean expectedResult) {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        boolean result = map.keySet().remove(keyToRemove);
        assertEquals(expectedResult, result, "remove() return value mismatch");
        assertEquals(expectedResult, !map.containsKey(keyToRemove), "Map state after remove() mismatch");
    }

    // -----------------------------------------------------------------
    @Test
    void testKeySetIteratorContents() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("epsilon", 3);
        Set<String> expected = new HashSet<>(Arrays.asList("alpha", "beta", "epsilon"));
        Set<String> actual = new HashSet<>();
        for (Iterator<String> it = map.keySet().iterator(); it.hasNext(); ) {
            actual.add(it.next());
        }
        assertEquals(expected, actual, "Iterator must return exactly the map's keys (order not significant)");
    }

    // -----------------------------------------------------------------
    @Test
    void testKeySetSizeAndIsEmpty() {
        // case 1 – three entries
        map.putAll(Map.of("x", 1, "y", 2, "z", 3));
        Set<String> ks = map.keySet();
        assertEquals(3, ks.size());
        assertFalse(ks.isEmpty());
        // case 2 – empty map
        map.clear();
        ks = map.keySet();
        assertEquals(0, ks.size());
        assertTrue(ks.isEmpty());
        // case 3 – single entry then remove via keySet
        map.put("a", 10);
        assertTrue(map.keySet().remove("a"));
        ks = map.keySet();
        assertEquals(0, ks.size());
        assertTrue(ks.isEmpty());
    }

    // -----------------------------------------------------------------
    @Test
    void testKeySetContainsNullKey() {
        // Insert a null key
        map.put(null, 42);
        assertTrue(map.keySet().contains(null), "keySet should report presence of null key");
        // Verify that a map without null key returns false
        map.clear();
        map.put("x", 1);
        assertFalse(map.keySet().contains(null), "keySet should not contain null when none was added");
    }

    // -----------------------------------------------------------------
    @Test
    void testIteratorNextBeyondEndThrowsNoSuchElementException() {
        map.put("k1", 1);
        Iterator<String> it = map.keySet().iterator();
        assertEquals("k1", it.next());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after iterator is exhausted must throw NoSuchElementException");
    }

    @Test
    void testIteratorRemoveUnsupportedOperationException() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        // According to the specification for this test case, remove() is not supported.
        assertThrows(UnsupportedOperationException.class, it::remove, "iterator.remove() should throw UnsupportedOperationException");
    }

    @Test
    void testIteratorDetectsConcurrentModification() {
        map.put("k1", 1);
        map.put("k2", 2);
        Iterator<String> it = map.keySet().iterator();
        // Modify the map after iterator creation
        map.put("k3", 3);
        // The next call must detect the concurrent modification
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw ConcurrentModificationException after underlying map modification");
    }

    // -----------------------------------------------------------------
    @Test
    void testRemoveAllNullCollectionThrowsNPE() {
        map.put("p", 1);
        map.put("q", 2);
        assertThrows(NullPointerException.class, () -> map.keySet().removeAll(null), "removeAll(null) must throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void testRetainAllNullCollectionThrowsNPE() {
        map.put("x", 1);
        map.put("y", 2);
        assertThrows(NullPointerException.class, () -> map.keySet().retainAll(null), "retainAll(null) must throw NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void testRemoveAllWithDuplicates() {
        map.put("dup", 1);
        map.put("unique", 2);
        Collection<String> coll = Arrays.asList("dup", "dup", "missing");
        boolean changed = map.keySet().removeAll(coll);
        assertTrue(changed, "removeAll should report change when a duplicate key is removed");
        assertFalse(map.keySet().contains("dup"), "Key \"dup\" must be removed");
        assertTrue(map.keySet().contains("unique"), "Key \"unique\" must remain");
    }

    // -----------------------------------------------------------------
    @Test
    void testRetainAllWithNulls() {
        map.put("a", 1);
        map.put(null, 2);
        map.put("b", 3);
        Collection<String> coll = Arrays.asList(null, "b");
        boolean changed = map.keySet().retainAll(coll);
        assertTrue(changed, "retainAll should report change when keys are removed");
        assertEquals(2, map.keySet().size(), "Resulting keySet size must be 2");
        assertTrue(map.keySet().contains(null), "Null key must be retained");
        assertTrue(map.keySet().contains("b"), "Key \"b\" must be retained");
        assertFalse(map.keySet().contains("a"), "Key \"a\" must be removed");
    }

    // -----------------------------------------------------------------
    @Test
    void testRetainAllIncompatibleTypeThrowsClassCastException() {
        map.put("alpha", 1);
        map.put("beta", 2);
        // 123 is not a String
        Collection mixed = Arrays.asList("alpha", 123);
        assertThrows(ClassCastException.class, () -> map.keySet().retainAll(mixed), "retainAll with an element of incompatible type must throw ClassCastException");
    }
}
