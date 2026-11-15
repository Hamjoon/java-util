package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.NavigableMap;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_clear_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test with an unsealed map containing a few entries
        sealed = false;
        sealedSupplier = () -> sealed;
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @Test
    void clearWhenUnsealedEmptiesMap() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed");
        assertEquals(3, sealableMap.size(), "Pre‑condition: map contains three entries");
        sealableMap.clear();
        assertTrue(sealedSupplier.get() == false, "Map remains unsealed after clear");
        assertEquals(0, sealableMap.size(), "Map should be empty after clear");
        assertTrue(sealableMap.isEmpty(), "isEmpty() must report true after clear");
        assertNull(sealableMap.get("one"), "No entry should be present after clear");
    }

    @Test
    void clearWhenSealedThrowsUnsupportedOperationException() {
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        assertThrows(UnsupportedOperationException.class, sealableMap::clear, "clear() must throw when the map is sealed");
    }

    @Test
    void clearAfterSealingThenUnsealingAllowsMutationAgain() {
        // 1. Seal and verify that clear() fails
        sealed = true;
        assertThrows(IllegalStateException.class, sealableMap::clear);
        // 2. Unseal and verify that clear() now succeeds
        sealed = false;
        assertDoesNotThrow(sealableMap::clear);
        assertEquals(0, sealableMap.size(), "Map should be empty after successful clear");
    }

    @Test
    void clearWhenSealedThrowsIllegalStateException() {
        // seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        // According to the API, a sealed map should throw IllegalStateException
        assertThrows(IllegalStateException.class, sealableMap::clear, "clear() must throw IllegalStateException when the map is sealed");
    }

    @Test
    void clearOnAlreadyEmptyUnsealedMapIsNoOp() {
        // Arrange: create an empty backing map and a corresponding sealable map
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        assertTrue(sealableMap.isEmpty(), "Pre‑condition: map is already empty");
        assertFalse(sealedSupplier.get(), "Map should be unsealed");
        // Act & Assert: clear() should not throw and the map stays empty
        assertDoesNotThrow(sealableMap::clear, "clear() on an empty, unsealed map must not throw");
        assertTrue(sealableMap.isEmpty(), "Map should remain empty after clear()");
    }

    @Test
    void clearOnAlreadyEmptySealedMapThrowsIllegalStateException() {
        // Arrange: empty map, then seal it
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        assertTrue(sealableMap.isEmpty(), "Pre‑condition: map is empty");
        // Act & Assert: clear() must throw and the map stays empty
        assertThrows(IllegalStateException.class, sealableMap::clear, "clear() on an empty, sealed map must throw IllegalStateException");
        assertTrue(sealableMap.isEmpty(), "Map should still be empty after failed clear()");
    }

    @Test
    void clearWhenSealedWithEntriesThrowsAndLeavesMapIntact() {
        // Seal the map that already contains entries (setup from @BeforeEach)
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed");
        assertEquals(3, sealableMap.size(), "Pre‑condition: map contains three entries");
        // Act & Assert
        assertThrows(IllegalStateException.class, sealableMap::clear, "clear() on a sealed map with entries must throw IllegalStateException");
        // Verify that the underlying map still holds its original entries
        assertEquals(3, sealableMap.size(), "Map size must remain unchanged after failed clear()");
        assertEquals(1, sealableMap.get("one"));
        assertEquals(2, sealableMap.get("two"));
        assertEquals(3, sealableMap.get("three"));
    }

    @Test
    void clearDoesNotAlterSealedState() {
        // Seal the map
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed before clear()");
        // clear() should throw but must not change the sealed flag
        assertThrows(IllegalStateException.class, sealableMap::clear);
        assertTrue(sealedSupplier.get(), "Map must remain sealed after clear() throws");
    }
}
