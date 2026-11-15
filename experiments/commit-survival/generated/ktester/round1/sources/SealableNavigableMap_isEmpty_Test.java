package com.cedarsoftware.util;

import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentNavigableMapNullSafe;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_isEmpty_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // The map starts unsealed; the supplier reflects the current value of `sealed`
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use the same concrete implementation as the production tests
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and ensure a clean state for the next test
        backingMap.clear();
        sealed = false;
    }

    @Test
    void isEmpty_returnsTrue_whenBackingMapIsEmpty() {
        assertTrue(sealableMap.isEmpty(), "Map should be empty when no entries have been added");
    }

    @Test
    void isEmpty_returnsFalse_whenBackingMapHasEntries() {
        sealableMap.put("one", 1);
        sealableMap.put("two", 2);
        assertFalse(sealableMap.isEmpty(), "Map should not be empty after adding entries");
    }

    @Test
    void isEmpty_reflectsChangesAfterClear_whenUnsealed() {
        sealableMap.put("alpha", 10);
        sealableMap.put("beta", 20);
        assertFalse(sealableMap.isEmpty(), "Map should not be empty before clear");
        // allowed because map is unsealed
        sealableMap.clear();
        assertTrue(sealableMap.isEmpty(), "Map should be empty after clear");
    }

    @Test
    void isEmpty_behavesCorrectly_whenSealed_andMapIsEmpty() {
        // make the map read‑only
        sealed = true;
        assertTrue(sealableMap.isEmpty(), "isEmpty should still report true when the map is sealed and contains no entries");
    }

    @Test
    void isEmpty_behavesCorrectly_whenSealed_andMapHasEntries() {
        sealableMap.put("x", 100);
        sealableMap.put("y", 200);
        // seal after populating
        sealed = true;
        assertFalse(sealableMap.isEmpty(), "isEmpty should still report false when the map is sealed but contains entries");
    }

    @Test
    void isEmpty_doesNotAllowModificationWhenSealed_butQueryRemainsFunctional() {
        sealableMap.put("k", 1);
        sealed = true;
        // Attempting to modify should throw, but isEmpty must still be callable
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put("new", 2));
        assertFalse(sealableMap.isEmpty(), "isEmpty should still correctly reflect the non‑empty state");
    }

    @Test
    void isEmpty_afterRemoveNonExistingKey_onNonEmptyMap_returnsFalse() {
        sealableMap.put("a", 1);
        sealableMap.put("b", 2);
        // remove a key that does not exist
        sealableMap.remove("nonexistent");
        assertFalse(sealableMap.isEmpty(), "Removing a non‑existing key should not affect the non‑empty state");
    }

    @Test
    void isEmpty_afterRemoveNonExistingKey_onEmptyMap_returnsTrue() {
        // map is already empty
        sealableMap.remove("nothing");
        assertTrue(sealableMap.isEmpty(), "Removing a non‑existing key from an empty map must keep it empty");
    }

    @Test
    void isEmpty_afterPutAllWithEmptyCollection_returnsTrue() {
        Map<String, Integer> empty = Collections.emptyMap();
        sealableMap.putAll(empty);
        assertTrue(sealableMap.isEmpty(), "putAll with an empty collection should leave the map empty");
    }

    @Test
    void isEmpty_afterPutAllWithMultipleEntries_returnsFalse() {
        Map<String, Integer> many = Map.of("one", 1, "two", 2, "three", 3);
        sealableMap.putAll(many);
        assertFalse(sealableMap.isEmpty(), "putAll with entries must make the map non‑empty");
    }

    @Test
    void isEmpty_afterClearOnAlreadyEmptyMap_returnsTrue() {
        // map is already empty
        sealableMap.clear();
        assertTrue(sealableMap.isEmpty(), "Calling clear on an empty map should keep it empty");
    }

    @Test
    void isEmpty_afterIteratorRemovalOfAllEntries_returnsTrue() {
        sealableMap.put("k1", 10);
        sealableMap.put("k2", 20);
        sealableMap.put("k3", 30);
        Iterator<Map.Entry<String, Integer>> it = sealableMap.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            // remove via iterator
            it.remove();
        }
        assertTrue(sealableMap.isEmpty(), "After removing all entries via iterator, map should be empty");
    }

    @Test
    void isEmpty_afterReplacingValueForExistingKey_returnsFalse() {
        sealableMap.put("dup", 1);
        // replace value
        sealableMap.put("dup", 99);
        assertFalse(sealableMap.isEmpty(), "Replacing a value for an existing key still leaves one entry in the map");
    }

    @Test
    void isEmpty_afterSubMapViewClear_makesOriginalEmpty() {
        sealableMap.put("first", 1);
        sealableMap.put("second", 2);
        sealableMap.put("third", 3);
        // subMap that includes all keys
        NavigableMap<String, Integer> sub = sealableMap.subMap("first", true, "third", true);
        // clear via view
        sub.clear();
        assertTrue(sealableMap.isEmpty(), "Clearing the subMap view should empty the original map");
    }

    @RepeatedTest(3)
    void isEmpty_repeatedCallsOnEmptyMap_consistentlyTrue() {
        assertTrue(sealableMap.isEmpty(), "Repeated isEmpty() on an empty map must always return true");
    }

    @RepeatedTest(3)
    void isEmpty_repeatedCallsAfterSinglePut_consistentlyFalse() {
        sealableMap.put("single", 42);
        assertFalse(sealableMap.isEmpty(), "Repeated isEmpty() after a put must always return false");
    }

    @Test
    void isEmpty_whenUnderlyingMapIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the private navMap field to null
        Field f = SealableNavigableMap.class.getDeclaredField("navMap");
        f.setAccessible(true);
        f.set(sealableMap, null);
        assertThrows(NullPointerException.class, () -> sealableMap.isEmpty(), "Calling isEmpty() when the delegate map is null should throw NPE");
    }

    @Test
    void isEmpty_whenUnderlyingMapThrowsUnsupportedOperationException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.isEmpty()).thenThrow(new UnsupportedOperationException("unsupported"));
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, map::isEmpty, "UnsupportedOperationException from delegate must propagate");
        assertEquals("unsupported", ex.getMessage());
    }

    @Test
    void isEmpty_whenUnderlyingMapThrowsIllegalStateException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.isEmpty()).thenThrow(new IllegalStateException("illegal state"));
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::isEmpty, "IllegalStateException from delegate must propagate");
        assertEquals("illegal state", ex.getMessage());
    }

    @Test
    void isEmpty_whenUnderlyingMapThrowsConcurrentModificationException_propagates() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = mock(NavigableMap.class);
        when(mockMap.isEmpty()).thenThrow(new ConcurrentModificationException("concurrent"));
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(mockMap, sealedSupplier);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, map::isEmpty, "ConcurrentModificationException from delegate must propagate");
        assertEquals("concurrent", ex.getMessage());
    }
}
