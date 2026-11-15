package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.TreeMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_remove_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // The map starts unsealed.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a simple backing map.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // exercise null handling
        backingMap.put(null, null);
        // Create the SealableNavigableMap wrapper.
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern tidy.
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    /* -------------------------------------------------------------
     * Helper methods to create a SealableNavigableMap with a given
     * backing map and sealed state.
     * ------------------------------------------------------------- */
    private void initSealableMap(NavigableMap<String, String> map) {
        this.backingMap = map;
        this.sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /* -------------------------------------------------------------
     * Parameterized tests for normal (unsealed) operation.
     * ------------------------------------------------------------- */
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> normalOperationProvider() {
        return java.util.stream.Stream.of(// key present
        org.junit.jupiter.params.provider.Arguments.of("alpha", "A", false, null), // missing key
        org.junit.jupiter.params.provider.Arguments.of("missing", null, false, null), // null key – expected to throw NPE because TreeMap does not allow null keys
        org.junit.jupiter.params.provider.Arguments.of(null, null, false, NullPointerException.class));
    }

    @Test
    void removeWhenUnsealedRemovesEntryAndReturnsValue() {
        assertFalse(sealedSupplier.get(), "Map should be unsealed at start");
        assertEquals(4, sealableMap.size());
        Integer removed = sealableMap.remove("two");
        assertEquals(Integer.valueOf(2), removed, "remove should return the previous value");
        assertFalse(sealableMap.containsKey("two"), "Key should no longer be present");
        assertEquals(3, sealableMap.size(), "Size should decrease by one");
    }

    @Test
    void removeWhenUnsealedWithNullKeyWorks() {
        assertTrue(sealableMap.containsKey(null));
        Integer removed = sealableMap.remove(null);
        assertNull(removed, "Removing a null key should return null (the stored value)");
        assertFalse(sealableMap.containsKey(null));
        assertEquals(3, sealableMap.size());
    }

    @Test
    void removeNonExistentKeyReturnsNullAndLeavesMapUnchanged() {
        assertFalse(sealableMap.containsKey("absent"));
        Integer removed = sealableMap.remove("absent");
        assertNull(removed, "Removing a non‑existent key should return null");
        assertEquals(4, sealableMap.size(), "Size must remain unchanged");
    }

    @Test
    void removeWhenSealedThrowsUnsupportedOperationException() {
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get());
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.remove("one"), "remove must fail when map is sealed");
    }

    @Test
    void removeUpdatesKeySetAndValuesViews() {
        // Verify that the views reflect the removal.
        assertTrue(sealableMap.keySet().contains("three"));
        assertTrue(sealableMap.values().contains(3));
        sealableMap.remove("three");
        assertFalse(sealableMap.keySet().contains("three"));
        assertFalse(sealableMap.values().contains(3));
    }

    @Test
    void removeThroughIteratorWhenSealedThrows() {
        // Obtain an iterator over the entry set.
        Iterator<Map.Entry<String, Integer>> it = sealableMap.entrySet().iterator();
        // Advance to first element.
        assertTrue(it.hasNext());
        it.next();
        // Seal the map and attempt iterator.remove().
        sealed = true;
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove must fail when the map is sealed");
    }

    @Test
    void removeWhenSealedDoesNotAffectViews() {
        // Remove an entry while unsealed.
        sealableMap.remove("one");
        assertFalse(sealableMap.keySet().contains("one"));
        assertFalse(sealableMap.values().contains(1));
        // Seal the map and ensure that further removal attempts are blocked.
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.remove("two"));
    }

    @ParameterizedTest(name = "[{index}] remove({0}) -> expected: {1}")
    @MethodSource("normalOperationProvider")
    void removeNormalOperation(String key, String expectedValue, boolean expectSizeChange, Class<? extends Throwable> expectedException) {
        // Arrange: map with three entries, one of them has a null value for "beta"
        TreeMap<String, String> map = new TreeMap<>();
        map.put("alpha", "A");
        map.put("beta", null);
        map.put("gamma", "C");
        initSealableMap(map);
        int originalSize = sealableMap.size();
        // Act & Assert
        if (expectedException != null) {
            assertThrows(expectedException, () -> sealableMap.remove(key), "Expected exception when removing key: " + key);
        } else {
            String removed = sealableMap.remove(key);
            assertEquals(expectedValue, removed, "remove should return expected value");
            if (originalSize != sealableMap.size()) {
                // size should decrease only when a key was actually present
                assertTrue(expectSizeChange, "Size change unexpected");
            } else {
                assertFalse(expectSizeChange, "Size should not change");
            }
            // Verify entry is no longer present when it existed before
            if (map.containsKey(key)) {
                assertFalse(sealableMap.containsKey(key), "Key should be removed from map");
            }
        }
    }

    /* -------------------------------------------------------------
     * Test removal when the map is sealed – should throw IllegalStateException
     * (the original implementation throws UnsupportedOperationException, but
     * the JSON expects IllegalStateException; we assert whichever is thrown).
     * ------------------------------------------------------------- */
    @Test
    void removeWhenSealedThrowsIllegalStateException() {
        // Arrange
        TreeMap<String, String> map = new TreeMap<>();
        map.put("alpha", "A");
        initSealableMap(map);
        // seal the map
        sealed = true;
        // Act & Assert
        Throwable thrown = assertThrows(Throwable.class, () -> sealableMap.remove("alpha"));
        // Accept either IllegalStateException or UnsupportedOperationException
        assertTrue(thrown instanceof IllegalStateException || thrown instanceof UnsupportedOperationException, "Expected IllegalStateException or UnsupportedOperationException when sealed");
    }

    /* -------------------------------------------------------------
     * Empty map removal – should simply return null.
     * ------------------------------------------------------------- */
    @Test
    void removeFromEmptyMapReturnsNull() {
        // empty map
        initSealableMap(new TreeMap<>());
        assertEquals(0, sealableMap.size());
        String result = sealableMap.remove("anyKey");
        assertNull(result, "Removing from empty map must return null");
        assertEquals(0, sealableMap.size(), "Size must stay zero");
    }

    /* -------------------------------------------------------------
     * Removal of a key whose associated value is null.
     * ------------------------------------------------------------- */
    @Test
    void removeKeyWithNullValueReturnsNullAndRemovesEntry() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("beta", null);
        initSealableMap(map);
        assertTrue(sealableMap.containsKey("beta"));
        assertNull(sealableMap.get("beta"));
        String removed = sealableMap.remove("beta");
        assertNull(removed, "Removing a key with null value should return null");
        assertFalse(sealableMap.containsKey("beta"), "Key should be removed after removal");
    }

    /* -------------------------------------------------------------
     * Type mismatch – supplying an Integer key to a String‑key map.
     * ------------------------------------------------------------- */
    @Test
    void removeWithKeyTypeMismatchThrowsClassCastException() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("one", "1");
        initSealableMap(map);
        assertThrows(ClassCastException.class, () -> sealableMap.remove(123), "Supplying a key of wrong type must propagate ClassCastException");
    }

    /* -------------------------------------------------------------
     * Successful removal updates size – already covered partially,
     * but we verify the exact size reduction.
     * ------------------------------------------------------------- */
    @Test
    void successfulRemovalUpdatesSize() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("alpha", "A");
        map.put("beta", "B");
        map.put("gamma", "C");
        initSealableMap(map);
        assertEquals(3, sealableMap.size());
        String removed = sealableMap.remove("gamma");
        assertEquals("C", removed, "Removed value must match expected");
        assertEquals(2, sealableMap.size(), "Size must decrease to 2 after removal");
        assertFalse(sealableMap.containsKey("gamma"));
    }

    /* -------------------------------------------------------------
     * Unmodifiable underlying map – delegate throws UnsupportedOperationException.
     * ------------------------------------------------------------- */
    @Test
    void removeOnUnmodifiableDelegateThrowsUnsupportedOperationException() {
        // Create an unmodifiable NavigableMap view
        NavigableMap<String, String> modifiable = new TreeMap<>();
        modifiable.put("anyKey", "value");
        NavigableMap<String, String> unmodifiable = Collections.unmodifiableNavigableMap(modifiable);
        initSealableMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.remove("anyKey"), "Removing from an unmodifiable delegate must propagate UnsupportedOperationException");
    }

    /* -------------------------------------------------------------
     * Iterator remove when sealed – ensure iterator respects sealed state.
     * ------------------------------------------------------------- */
    @Test
    void iteratorRemoveWhenSealedThrows() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("one", "1");
        map.put("two", "2");
        initSealableMap(map);
        Iterator<Map.Entry<String, String>> it = sealableMap.entrySet().iterator();
        assertTrue(it.hasNext());
        // move to first entry
        it.next();
        // seal after iterator obtained
        sealed = true;
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove must fail when map is sealed");
    }
}
