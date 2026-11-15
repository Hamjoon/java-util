package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_clear_Test {

    /**
     * A mock of the backing {@code ConcurrentMap}. Used to verify interaction.
     */
    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Instance of a concrete subclass that forwards to {@code mockInternalMap}.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapWithMock;

    /**
     * Instance of a concrete subclass that uses a real {@link ConcurrentHashMap}.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapWithReal;

    /**
     * Minimal concrete implementation of {@link AbstractConcurrentNullSafeMap}
     * that simply forwards the provided {@code internalMap} to the superclass.
     *
     * @param <K> key type
     * @param <V> value type
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the map that delegates to the mock – this is the object under test for the interaction test.
        mapWithMock = new TestMap<>(mockInternalMap);
        // Create a map backed by a real ConcurrentHashMap for the functional test.
        ConcurrentMap<Object, Object> realBacking = new ConcurrentHashMap<>();
        mapWithReal = new TestMap<>(realBacking);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock after each test to avoid cross‑test interference.
        reset(mockInternalMap);
    }

    @Test
    void clear_DelegatesToInternalMap() {
        // When
        mapWithMock.clear();
        // Then – verify that the underlying map's clear() method was invoked exactly once.
        verify(mockInternalMap, times(1)).clear();
    }

    @Test
    void clear_EmptiesBackingMap() {
        // Populate the map with a few entries.
        mapWithReal.put("a", "alpha");
        mapWithReal.put("b", "beta");
        // null key handling is part of the abstract class
        mapWithReal.put(null, "nullKey");
        // null value handling
        mapWithReal.put("c", null);
        // Sanity check – map should contain 4 entries (nulls are masked internally).
        assertEquals(4, mapWithReal.size(), "Pre‑condition: map should contain the inserted entries");
        // When
        mapWithReal.clear();
        // Then – the map must be empty.
        assertTrue(mapWithReal.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapWithReal.size(), "Size should be zero after clear()");
    }

    @Test
    void clear_AlreadyEmptyMap_NoException() {
        // Pre‑condition: map is empty.
        assertTrue(mapWithReal.isEmpty(), "Map should start empty");
        // When
        assertDoesNotThrow(() -> mapWithReal.clear(), "Calling clear on an empty map must not throw");
        // Then
        assertTrue(mapWithReal.isEmpty(), "Map remains empty after clear()");
        assertEquals(0, mapWithReal.size(), "Size stays zero after clear()");
    }

    @Test
    void clear_SingleNullKeyEntry_RemovesEntry() {
        // Insert a single entry where the key is null.
        mapWithReal.put(null, "valueForNullKey");
        assertEquals(1, mapWithReal.size(), "Pre‑condition: map should contain the null‑key entry");
        assertTrue(mapWithReal.containsKey(null), "Map should contain the null key before clear");
        assertTrue(mapWithReal.containsValue("valueForNullKey"), "Map should contain the value before clear");
        // When
        mapWithReal.clear();
        // Then
        assertTrue(mapWithReal.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapWithReal.size(), "Size must be zero after clear()");
        assertFalse(mapWithReal.containsKey(null), "Null key must no longer be present");
        assertFalse(mapWithReal.containsValue("valueForNullKey"), "Value must no longer be present");
    }

    @Test
    void clear_SingleNullValueEntry_RemovesEntry() {
        // Insert a single entry where the value is null.
        mapWithReal.put("keyForNullValue", null);
        assertEquals(1, mapWithReal.size(), "Pre‑condition: map should contain the null‑value entry");
        assertTrue(mapWithReal.containsKey("keyForNullValue"), "Map should contain the key before clear");
        assertTrue(mapWithReal.containsValue(null), "Map should contain a null value before clear");
        // When
        mapWithReal.clear();
        // Then
        assertTrue(mapWithReal.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapWithReal.size(), "Size must be zero after clear()");
        assertFalse(mapWithReal.containsKey("keyForNullValue"), "Key must no longer be present");
        assertFalse(mapWithReal.containsValue(null), "Null value must no longer be present");
    }

    @Test
    void clear_NullInternalMap_ThrowsNullPointerException() {
        // Create a map whose internalMap reference is null.
        AbstractConcurrentNullSafeMap<String, String> mapWithNullBacking = new TestMap<>(null);
        // Expect NPE when clear() is invoked.
        assertThrows(NullPointerException.class, mapWithNullBacking::clear, "clear() should throw NullPointerException when internalMap is null");
    }

    @Test
    void clear_UnderlyingMapUnsupportedOperation_PropagatesException() {
        // Configure the mock to throw UnsupportedOperationException on clear().
        doThrow(new UnsupportedOperationException("clear not supported")).when(mockInternalMap).clear();
        // When clear() is called, the exception should propagate.
        assertThrows(UnsupportedOperationException.class, () -> mapWithMock.clear(), "clear() must propagate UnsupportedOperationException from the backing map");
    }
}
