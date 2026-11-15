package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_clear_Test {

    /**
     * The map under test.
     */
    private SealableMap<String, Integer> map;

    /**
     * Mutable flag used by the {@link Supplier} to indicate sealed state.
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that returns the current {@code sealedState}.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * Mocked delegate map – injected only in the tests that need it.
     */
    @Mock
    private Map<String, Integer> mockedDelegate;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure each test starts with an unsealed map containing a few entries.
        sealedState = false;
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag to avoid cross‑test contamination.
        sealedState = false;
        map = null;
    }

    @Test
    void clearWhenUnsealedEmptiesMap() {
        // Pre‑condition: map contains four entries (including a null key/value).
        assertEquals(4, map.size(), "Pre‑condition failed: map should contain 4 entries");
        // Exercise the method under test.
        map.clear();
        // Verify that the map is now empty.
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be 0 after clear()");
        assertNull(map.get("one"), "All previous keys should return null after clear()");
        assertNull(map.get(null), "Null key should return null after clear()");
        assertTrue(map.entrySet().isEmpty(), "Entry set should be empty after clear()");
        assertTrue(map.keySet().isEmpty(), "Key set should be empty after clear()");
        assertTrue(map.values().isEmpty(), "Values collection should be empty after clear()");
    }

    @Test
    void clearWhenSealedThrowsUnsupportedOperationException() {
        // Seal the map.
        sealedState = true;
        // The clear operation must fail because the map is sealed.
        assertThrows(UnsupportedOperationException.class, () -> map.clear(), "clear() should throw UnsupportedOperationException when the map is sealed");
    }

    @Test
    void clearDoesNotAffectSealedState() {
        // Seal the map, attempt clear (which should fail), then unseal and clear successfully.
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.clear());
        // Unseal and clear again – this time it should succeed.
        sealedState = false;
        assertDoesNotThrow(() -> map.clear());
        // Verify map is empty after successful clear.
        assertTrue(map.isEmpty(), "Map should be empty after successful clear()");
        // The sealed flag must still be false.
        assertFalse(sealedState, "Sealed flag must remain false after a successful clear");
    }

    @Test
    void clearIdempotentOnAlreadyEmptyUnsealedMap() {
        // First clear to make the map empty.
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after first clear()");
        // Second clear – should be a no‑op and must not throw.
        assertDoesNotThrow(() -> map.clear(), "Calling clear() on an empty map should not throw");
        assertTrue(map.isEmpty(), "Map should remain empty after second clear()");
        assertEquals(0, map.size(), "Size must stay 0 after repeated clear()");
    }

    @Test
    void clearWhenSealedDoesNotModifyUnderlyingData() {
        // Capture snapshot of current entries.
        Map<String, Integer> snapshot = new HashMap<>(map);
        // Seal the map and attempt clear.
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.clear());
        // Verify that the original data is untouched.
        assertEquals(snapshot, map, "Map contents must remain unchanged when clear() is blocked by seal");
        assertFalse(map.isEmpty(), "Map should not become empty after failed clear()");
    }

    @Test
    void clearDoesNotChangeSealedStateAfterSuccessfulClear() {
        // Ensure map is unsealed.
        assertFalse(sealedState, "Pre‑condition: map must start unsealed");
        // Perform clear.
        map.clear();
        // The sealed flag must still be false.
        assertFalse(sealedState, "Sealed state must stay false after a successful clear()");
    }

    @Test
    void clearPropagatesExceptionFromUnderlyingMap() throws Exception {
        // Prepare a mock delegate that throws a RuntimeException from clear().
        doThrow(new UnsupportedOperationException("delegate does not support clear")).when(mockedDelegate).clear();
        // Inject the mock into the SealableMap instance via reflection.
        Field mapField = SealableMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, mockedDelegate);
        // The clear call should propagate the RuntimeException.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.clear(), "Exception from delegate map.clear() must propagate");
        assertEquals("delegate does not support clear", ex.getMessage());
    }

    @Test
    void clearWithNullUnderlyingMapThrowsNullPointerException() throws Exception {
        // Force the internal delegate map reference to null.
        Field mapField = SealableMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, null);
        // Expect a NullPointerException when clear() tries to delegate.
        assertThrows(NullPointerException.class, () -> map.clear(), "Calling clear() with a null delegate map should throw NullPointerException");
    }
}
