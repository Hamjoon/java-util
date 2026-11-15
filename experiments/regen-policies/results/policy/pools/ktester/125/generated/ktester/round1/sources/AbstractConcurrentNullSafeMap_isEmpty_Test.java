package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_isEmpty_Test {

    /**
     * Mocked backing map used for interaction‑based tests.
     */
    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Instance under test.
     */
    private AbstractConcurrentNullSafeMap<String, Integer> mapUnderTest;

    /**
     * Auto‑closeable for Mockito annotations.
     */
    private AutoCloseable mocks;

    /**
     * Concrete test implementation of the abstract class.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    class NullMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        NullMap() {
            super(null);
        }
    }

    class NullMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        NullMap() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations and create the map under test.
        mocks = MockitoAnnotations.openMocks(this);
        mapUnderTest = new TestMap<>(mockInternalMap);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Reset the mock to avoid cross‑test interference.
        reset(mockInternalMap);
        mocks.close();
    }

    @Test
    @DisplayName("isEmpty() returns true when the internal map reports empty")
    void isEmptyReturnsTrueWhenInternalMapEmpty() {
        // GIVEN
        when(mockInternalMap.isEmpty()).thenReturn(true);
        // WHEN
        boolean result = mapUnderTest.isEmpty();
        // THEN
        assertTrue(result, "isEmpty() should return true when the backing map is empty");
        verify(mockInternalMap, times(1)).isEmpty();
    }

    @Test
    @DisplayName("isEmpty() returns false when the internal map reports non‑empty")
    void isEmptyReturnsFalseWhenInternalMapNotEmpty() {
        // GIVEN
        when(mockInternalMap.isEmpty()).thenReturn(false);
        // WHEN
        boolean result = mapUnderTest.isEmpty();
        // THEN
        assertFalse(result, "isEmpty() should return false when the backing map contains entries");
        verify(mockInternalMap, times(1)).isEmpty();
    }

    @Test
    @DisplayName("isEmpty() works with a real ConcurrentHashMap backing implementation")
    void isEmptyWithRealBackingMap() {
        // Use a real ConcurrentHashMap to ensure the delegation works end‑to‑end.
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        // Initially empty.
        assertTrue(realMap.isEmpty(), "Newly created map should be empty");
        // Add a regular entry.
        realMap.put("key", 1);
        assertFalse(realMap.isEmpty(), "Map should not be empty after adding an entry");
        // Clear the map.
        realMap.clear();
        assertTrue(realMap.isEmpty(), "Map should be empty after clear()");
    }

    @Test
    @DisplayName("isEmpty() returns false after inserting a single entry")
    void isEmptyAfterSinglePut() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        realMap.put("k1", 10);
        assertFalse(realMap.isEmpty(), "Map must be non‑empty after a single put");
    }

    @Test
    @DisplayName("isEmpty() returns true after clear() following inserts")
    void isEmptyAfterClear() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        realMap.put("a", 1);
        realMap.put("b", 2);
        assertFalse(realMap.isEmpty(), "Map should be non‑empty before clear()");
        realMap.clear();
        assertTrue(realMap.isEmpty(), "Map should be empty after clear()");
    }

    @Test
    @DisplayName("isEmpty() stays false when only a subset of entries is removed")
    void isEmptyAfterPartialRemoval() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        realMap.put("x", 100);
        realMap.put("y", 200);
        realMap.put("z", 300);
        // Remove one entry.
        realMap.remove("y");
        assertFalse(realMap.isEmpty(), "Map must remain non‑empty when at least one entry exists");
    }

    @Test
    @DisplayName("isEmpty() becomes true after removing all entries one‑by‑one")
    void isEmptyAfterRemovingAllIndividually() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        realMap.put("p", 1);
        realMap.put("q", 2);
        realMap.put("r", 3);
        // Remove each key.
        realMap.remove("p");
        realMap.remove("q");
        realMap.remove("r");
        assertTrue(realMap.isEmpty(), "Map should be empty after all entries are removed");
    }

    @Test
    @DisplayName("isEmpty() becomes false after putIfAbsent on an empty map")
    void isEmptyAfterPutIfAbsentOnEmpty() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        assertTrue(realMap.isEmpty(), "Pre‑condition: map should start empty");
        realMap.putIfAbsent("newKey", 42);
        assertFalse(realMap.isEmpty(), "Map must be non‑empty after successful putIfAbsent");
    }

    @Test
    @DisplayName("isEmpty() stays false when putIfAbsent is called with an existing key")
    void isEmptyAfterPutIfAbsentWithExistingKey() {
        AbstractConcurrentNullSafeMap<String, Integer> realMap = new TestMap<>(new ConcurrentHashMap<>());
        realMap.put("existing", 5);
        assertFalse(realMap.isEmpty(), "Pre‑condition: map should be non‑empty");
        // Attempt to add the same key with a different value – should not change emptiness.
        realMap.putIfAbsent("existing", 99);
        assertFalse(realMap.isEmpty(), "Map must remain non‑empty after putIfAbsent with existing key");
    }

    @Test
    @DisplayName("isEmpty() throws NullPointerException when internalMap is null")
    void isEmptyThrowsWhenInternalMapIsNull() {
        // Subclass that deliberately passes null to the super‑constructor.
        class NullMap extends AbstractConcurrentNullSafeMap<String, Integer> {

            NullMap() {
                super(null);
            }
        }
        AbstractConcurrentNullSafeMap<String, Integer> mapWithNullInternal = new NullMap();
        assertThrows(NullPointerException.class, mapWithNullInternal::isEmpty, "Calling isEmpty() with a null internalMap should throw NPE");
    }

    @Test
    @DisplayName("isEmpty() propagates IllegalStateException from internal map")
    void isEmptyPropagatesIllegalStateException() {
        when(mockInternalMap.isEmpty()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, mapUnderTest::isEmpty, "Exception from internalMap.isEmpty() must be propagated unchanged");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("isEmpty() propagates UnsupportedOperationException from internal map")
    void isEmptyPropagatesUnsupportedOperationException() {
        when(mockInternalMap.isEmpty()).thenThrow(new UnsupportedOperationException("unsupported"));
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, mapUnderTest::isEmpty, "Exception from internalMap.isEmpty() must be propagated unchanged");
        assertEquals("unsupported", ex.getMessage());
    }
}
