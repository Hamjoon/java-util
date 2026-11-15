package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class AbstractConcurrentNullSafeMap_keySet_Test {

    private TestConcurrentNullSafeMap<String, Integer> map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestConcurrentNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestConcurrentNullSafeMap<>();
        // Populate with a mix of normal, null‑key and null‑value entries
        map.put("alpha", 1);
        // null key
        map.put(null, 2);
        // null value
        map.put("beta", null);
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    private static Stream<Arguments> provideContainsCases() {
        return Stream.of(Arguments.of("alpha", true), Arguments.of(null, true), Arguments.of("missing", false));
    }

    private static Stream<Arguments> provideRemoveCases() {
        return Stream.of(Arguments.of("beta", true), Arguments.of(null, true), Arguments.of("nonexistent", false));
    }

    @Test
    @DisplayName("keySet size reflects the underlying map size")
    void keySetSizeMatchesMapSize() {
        assertEquals(map.size(), map.keySet().size(), "keySet().size() should be equal to map.size()");
    }

    @Test
    @DisplayName("keySet contains all keys, including null")
    void keySetContainsAllKeysIncludingNull() {
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("alpha"));
        assertTrue(ks.contains("beta"));
        assertTrue(ks.contains(null), "keySet should contain the null key");
    }

    @Test
    @DisplayName("Iterator of keySet returns unmasked keys")
    void iteratorReturnsUnmaskedKeys() {
        Set<String> expected = new HashSet<>(Arrays.asList("alpha", "beta", null));
        Set<String> actual = new HashSet<>();
        for (String k : map.keySet()) {
            actual.add(k);
        }
        assertEquals(expected, actual, "Iterator should yield the original keys (null included)");
    }

    @Test
    @DisplayName("Removing a key via keySet removes it from the map")
    void removeViaKeySetUpdatesMap() {
        boolean removed = map.keySet().remove("alpha");
        assertTrue(removed, "keySet.remove should return true for an existing key");
        assertFalse(map.containsKey("alpha"), "Map should no longer contain the removed key");
        assertEquals(2, map.size(), "Map size should decrease after removal");
    }

    @Test
    @DisplayName("Removing a null key via keySet works")
    void removeNullKeyViaKeySet() {
        boolean removed = map.keySet().remove(null);
        assertTrue(removed, "keySet.remove should handle null keys");
        assertFalse(map.containsKey(null), "Map should no longer contain the null key");
        assertEquals(2, map.size(), "Map size should reflect the removal of the null key");
    }

    @Test
    @DisplayName("clear() on keySet clears the underlying map")
    void clearViaKeySetEmptiesMap() {
        map.keySet().clear();
        assertTrue(map.isEmpty(), "Map should be empty after keySet().clear()");
        assertEquals(0, map.keySet().size(), "keySet size should be zero after clear");
    }

    // ---------- Group: AbstractConcurrentNullSafeMap.keySet() behavior ----------
    @Test
    @DisplayName("keySet size equals the number of entries (including null key)")
    void keySetSizeEqualsNumberOfEntries() {
        map.clear();
        map.put(null, 42);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(3, map.keySet().size(), "keySet().size() should reflect three entries (null key included)");
    }

    @Test
    @DisplayName("Iterator yields keys with null unmasked (order not guaranteed)")
    void iteratorYieldsUnmaskedKeysIncludingNull() {
        map.clear();
        map.put(null, 99);
        map.put("a", 1);
        map.put("b", 2);
        List<String> collected = new ArrayList<>();
        for (String k : map.keySet()) {
            collected.add(k);
        }
        assertEquals(3, collected.size(), "Iterator should return three keys");
        assertTrue(collected.containsAll(Arrays.asList(null, "a", "b")), "Iterator must contain null, \"a\", and \"b\"");
    }

    // Parameterized contains tests
    @ParameterizedTest(name = "keySet.contains({0}) => {1}")
    @MethodSource("provideContainsCases")
    void keySetContainsVariousKeys(Object keyToCheck, boolean expected) {
        assertEquals(expected, map.keySet().contains(keyToCheck), "contains result mismatch for key: " + keyToCheck);
    }

    // Parameterized remove tests
    @ParameterizedTest(name = "keySet.remove({0}) => {1}")
    @MethodSource("provideRemoveCases")
    void keySetRemoveVariousKeys(Object keyToRemove, boolean expected) {
        boolean result = map.keySet().remove(keyToRemove);
        assertEquals(expected, result, "remove result mismatch for key: " + keyToRemove);
        // verify map state when removal succeeded
        if (expected) {
            assertFalse(map.containsKey(keyToRemove), "Map should no longer contain the removed key");
        }
    }

    @Test
    @DisplayName("keySet.clear() empties the map and size becomes zero")
    void keySetClearEmptiesMap() {
        map.keySet().clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear");
        assertEquals(0, map.keySet().size(), "keySet size must be zero after clear");
    }

    // ---------- Group: iterator remove behavior ----------
    @Test
    @DisplayName("Iterator remove after next updates underlying map")
    void iteratorRemoveAfterNextUpdatesMap() {
        map.clear();
        map.put("x", 1);
        map.put("y", 2);
        Iterator<String> it = map.keySet().iterator();
        // could be "x" or "y"
        String first = it.next();
        it.remove();
        assertFalse(map.containsKey(first), "Key should be removed from map after iterator.remove()");
        assertEquals(1, map.keySet().size(), "keySet size should decrease after iterator.remove()");
    }

    @Test
    @DisplayName("Iterator.remove() before next() throws IllegalStateException")
    void iteratorRemoveBeforeNextThrows() {
        Iterator<String> it = map.keySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("keySet.contains with invalid type throws ClassCastException")
    void containsInvalidTypeThrows() {
        assertThrows(ClassCastException.class, () -> map.keySet().contains(123), "contains() with wrong type should throw ClassCastException");
    }

    @Test
    @DisplayName("keySet.remove with invalid type throws ClassCastException")
    void removeInvalidTypeThrows() {
        assertThrows(ClassCastException.class, () -> map.keySet().remove(123), "remove() with wrong type should throw ClassCastException");
    }

    @Test
    @DisplayName("Iterator.next() on empty map throws NoSuchElementException")
    void iteratorNextOnEmptyThrows() {
        map.clear();
        Iterator<String> it = map.keySet().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    @DisplayName("keySet view is live – size reflects external put")
    void liveViewReflectsExternalPut() {
        map.clear();
        Set<String> ks = map.keySet();
        assertEquals(0, ks.size(), "Initially keySet size should be zero");
        map.put("newKey", 5);
        assertEquals(1, ks.size(), "keySet size must reflect newly added entry");
        assertTrue(ks.contains("newKey"));
    }

    @Test
    @DisplayName("clear() on an already empty keySet does not fail")
    void clearOnAlreadyEmptyKeySet() {
        map.clear();
        assertDoesNotThrow(() -> map.keySet().clear(), "Clearing an already empty keySet should not throw");
        assertEquals(0, map.keySet().size(), "keySet size should remain zero after clear on empty map");
    }

    @Test
    @DisplayName("Removing null key when absent returns false")
    void removeNullKeyWhenAbsentReturnsFalse() {
        map.clear();
        map.put("x", 1);
        boolean result = map.keySet().remove(null);
        assertFalse(result, "Removing a non‑existent null key must return false");
        assertEquals(1, map.size(), "Map size must stay unchanged");
    }

    // ---------- Group: unsupported operations ----------
    @Test
    @DisplayName("keySet.add throws UnsupportedOperationException")
    void keySetAddUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> map.keySet().add("someKey"), "add() on keySet must throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("keySet.addAll throws UnsupportedOperationException")
    void keySetAddAllUnsupported() {
        assertThrows(UnsupportedOperationException.class, () -> map.keySet().addAll(Arrays.asList("k1", "k2")), "addAll() on keySet must throw UnsupportedOperationException");
    }

    // ---------- Group: iterator remove illegal state after clear ----------
    @Test
    @DisplayName("iterator.remove() after clear without prior next throws IllegalStateException")
    void iteratorRemoveAfterClearIllegalState() {
        Iterator<String> it = map.keySet().iterator();
        // clear the map via the view
        map.keySet().clear();
        assertThrows(IllegalStateException.class, it::remove, "remove() after clear without next() must throw IllegalStateException");
    }

    // ---------- Group: iterator next after concurrent removal ----------
    @Test
    @DisplayName("iterator.next() after concurrent removal throws NoSuchElementException")
    void iteratorNextAfterConcurrentRemovalThrows() {
        map.clear();
        map.put("solo", 99);
        Iterator<String> it = map.keySet().iterator();
        // Remove directly from the map before calling next()
        map.remove("solo");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after the underlying entry was removed must throw NoSuchElementException");
    }
}
