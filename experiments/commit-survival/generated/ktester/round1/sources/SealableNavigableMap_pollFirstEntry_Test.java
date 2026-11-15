package com.cedarsoftware.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.TreeMap;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_pollFirstEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // The map starts un‑sealed.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use the same concrete implementation that the production code expects.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy.
        backingMap.clear();
    }

    @Test
    void pollFirstEntry_Unsealed_ReturnsAndRemovesFirstEntry() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed.");
        // The natural ordering of the keys makes "one" the first entry.
        Map.Entry<String, Integer> first = sealableMap.pollFirstEntry();
        assertNotNull(first, "pollFirstEntry should return the first entry when map is non‑empty");
        assertEquals("one", first.getKey(), "First key should be the lowest (lexicographically) key");
        assertEquals(Integer.valueOf(1), first.getValue(), "First value should match the stored value");
        // The entry must have been removed from the map.
        assertEquals(2, sealableMap.size(), "Size should decrease after polling the first entry");
        assertFalse(sealableMap.containsKey("one"), "The polled key must no longer be present");
    }

    @Test
    void pollFirstEntry_Sealed_ThrowsUnsupportedOperationException() {
        // switch to sealed state
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should now be sealed");
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.pollFirstEntry(), "pollFirstEntry must throw when the map is sealed");
    }

    @Test
    void pollFirstEntry_EmptyMap_ReturnsNull() {
        // Remove all entries via successive polls while unsealed.
        while (sealableMap.size() > 0) {
            sealableMap.pollFirstEntry();
        }
        assertEquals(0, sealableMap.size(), "Map should be empty after removing all entries");
        // Polling an empty map should simply return null (no exception).
        Map.Entry<String, Integer> result = sealableMap.pollFirstEntry();
        assertNull(result, "pollFirstEntry should return null when the map is empty");
    }

    @Test
    void pollFirstEntry_NormalMap_WithAppleAndBanana() {
        // Replace the default backing map with a fresh TreeMap containing the
        // exact data described in the JSON test case.
        TreeMap<String, Integer> map = new TreeMap<>();
        map.put("apple", 10);
        map.put("banana", 20);
        sealableMap = new SealableNavigableMap<>(map, sealedSupplier);
        Map.Entry<String, Integer> entry = sealableMap.pollFirstEntry();
        assertNotNull(entry, "Entry should be returned for a non‑empty map");
        assertEquals("apple", entry.getKey(), "The lowest key (lexicographically) is 'apple'");
        assertEquals(Integer.valueOf(10), entry.getValue(), "Value should match the stored value");
        assertEquals(1, sealableMap.size(), "Map size should be reduced by one after polling");
        assertFalse(sealableMap.containsKey("apple"), "Polled key must be removed from the map");
    }

    @Test
    void pollFirstEntry_Sealed_ThrowsIllegalStateException() {
        // Switch the supplier to report a sealed state.
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should now be sealed");
        // The contract states that a sealed map must throw IllegalStateException.
        assertThrows(IllegalStateException.class, () -> sealableMap.pollFirstEntry(), "pollFirstEntry must throw IllegalStateException when the map is sealed");
    }

    @Test
    void pollFirstEntry_UnderlyingUnsupported_PropagatesException() {
        // Create a mock NavigableMap that throws UnsupportedOperationException
        // when pollFirstEntry() is invoked.
        @SuppressWarnings("unchecked")
        NavigableMap<String, Integer> mockMap = Mockito.mock(NavigableMap.class);
        Mockito.when(mockMap.pollFirstEntry()).thenThrow(new UnsupportedOperationException("pollFirstEntry not supported"));
        // The sealed supplier reports an un‑sealed state.
        sealableMap = new SealableNavigableMap<>(mockMap, sealedSupplier);
        // Verify that the exception from the delegate is propagated unchanged.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> sealableMap.pollFirstEntry(), "UnsupportedOperationException should be propagated from the underlying map");
        assertEquals("pollFirstEntry not supported", ex.getMessage(), "Exception message should match the delegate's message");
    }
}
