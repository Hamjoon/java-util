package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_isEmpty_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test, pre‑populated with a few entries
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map reference is cleared after each test
        map = null;
    }

    @Test
    void isEmptyOnFreshEmptyMap() {
        SealableMap<String, Integer> emptyMap = new SealableMap<>(sealedSupplier);
        assertTrue(emptyMap.isEmpty(), "A newly created SealableMap should be empty");
    }

    @Test
    void isEmptyReturnsFalseWhenMapHasEntries() {
        assertFalse(map.isEmpty(), "Map with pre‑populated entries should not be empty");
    }

    @Test
    void isEmptyAfterClear() {
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
    }

    @Test
    void isEmptyAfterRemovingAllEntries() {
        map.remove("one");
        map.remove("two");
        map.remove("three");
        map.remove(null);
        assertTrue(map.isEmpty(), "Map should be empty after all entries are removed");
    }

    @Test
    void isEmptyWhenSealed() {
        // Seal the map – mutating operations are now prohibited,
        // but isEmpty() should still correctly reflect the underlying state.
        sealedState = true;
        assertFalse(map.isEmpty(), "Sealed map with entries should report not empty");
        // clear() is also prohibited when sealed, so we expect an exception
        map.clear();
        // The above line would throw UnsupportedOperationException if executed.
        // Instead, we verify that isEmpty() works without modifying the map.
    }

    @Test
    void isEmptyOnSealedMapCreatedWithPrepopulatedBackingMap() {
        // Construct a SealableMap that already contains data, then seal it.
        Map<String, Integer> backing = Map.of("alpha", 10, "beta", 20);
        SealableMap<String, Integer> prePopulated = new SealableMap<>(backing, sealedSupplier);
        // seal it
        sealedState = true;
        assertFalse(prePopulated.isEmpty(), "Sealed map constructed with a non‑empty backing map should not be empty");
    }

    @Test
    void isEmptyOnSealedEmptyMap() {
        // Construct an empty backing map, seal it and verify emptiness.
        SealableMap<String, Integer> emptySealed = new SealableMap<>(Map.of(), sealedSupplier);
        sealedState = true;
        assertTrue(emptySealed.isEmpty(), "Sealed map with no entries should be empty");
    }

    /**
     * Group: When underlying map is empty
     * A newly created SealableMap wrapping an empty HashMap should report isEmpty() == true.
     */
    @Test
    void isEmpty_WhenUnderlyingMapIsEmpty_ShouldReturnTrue() {
        SealableMap<String, Integer> emptyMap = new SealableMap<>(sealedSupplier);
        assertTrue(emptyMap.isEmpty(), "A SealableMap wrapping an empty backing map must report empty");
    }

    /**
     * Group: When underlying map is not empty – basic non‑empty case.
     */
    @Test
    void isEmpty_WhenUnderlyingMapHasEntries_ShouldReturnFalse() {
        assertFalse(map.isEmpty(), "A SealableMap with at least one entry must report not empty");
    }

    /**
     * Group: When underlying map is not empty – clear after add.
     */
    @Test
    void isEmpty_AfterAddingEntryAndClearing_ShouldReturnTrue() {
        SealableMap<String, Integer> temp = new SealableMap<>(sealedSupplier);
        temp.put("temp", 42);
        temp.clear();
        assertTrue(temp.isEmpty(), "After adding an entry and calling clear(), the map should be empty");
    }

    /**
     * Group: When underlying map is not empty – null key/value still counts as entry.
     */
    @Test
    void isEmpty_WithNullKeyOrValue_ShouldReturnFalse() {
        SealableMap<String, Integer> nullEntryMap = new SealableMap<>(sealedSupplier);
        nullEntryMap.put(null, null);
        assertFalse(nullEntryMap.isEmpty(), "Map containing a null key/value is still non‑empty");
    }

    /**
     * Group: When underlying map is not empty – remove all entries individually.
     */
    @Test
    void isEmpty_AfterRemovingAllEntriesIndividually_ShouldReturnTrue() {
        SealableMap<String, Integer> removable = new SealableMap<>(sealedSupplier);
        removable.put("a", 1);
        removable.put("b", 2);
        removable.remove("a");
        removable.remove("b");
        assertTrue(removable.isEmpty(), "After removing all entries one‑by‑one, the map should be empty");
    }

    /**
     * Group: When underlying map is null
     * Expect a NullPointerException when isEmpty() is invoked.
     */
    @Test
    void isEmpty_WhenBackingMapIsNull_ShouldThrowNullPointerException() {
        SealableMap<String, Integer> nullBacking = new SealableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, nullBacking::isEmpty, "Calling isEmpty() on a SealableMap with a null backing map must throw NPE");
    }

    /**
     * Group: When underlying map throws UnsupportedOperationException on isEmpty
     * The exception should propagate unchanged.
     */
    @Test
    void isEmpty_WhenBackingMapThrowsUnsupportedOperationException_ShouldPropagate() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.isEmpty()).thenThrow(new UnsupportedOperationException("unsupported"));
        SealableMap<String, Integer> sm = new SealableMap<>(mockMap, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, sm::isEmpty, "UnsupportedOperationException from backing map must propagate");
    }

    /**
     * Group: When underlying map throws generic RuntimeException on isEmpty
     * The exception should propagate unchanged.
     */
    @Test
    void isEmpty_WhenBackingMapThrowsRuntimeException_ShouldPropagate() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.isEmpty()).thenThrow(new RuntimeException("runtime failure"));
        SealableMap<String, Integer> sm = new SealableMap<>(mockMap, sealedSupplier);
        assertThrows(RuntimeException.class, sm::isEmpty, "RuntimeException from backing map must propagate");
    }
}
