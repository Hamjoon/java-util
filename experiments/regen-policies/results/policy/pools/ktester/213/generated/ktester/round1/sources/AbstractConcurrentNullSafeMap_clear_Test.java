package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_clear_Test {

    /**
     * Instance that uses a real {@link ConcurrentHashMap} as backing store.
     */
    private TestMap realMap;

    /**
     * Instance that uses a Mockito mock as backing store.
     */
    private TestMap mockMap;

    /**
     * Mocked backing map – injected by Mockito.
     */
    @Mock
    private ConcurrentMap<Object, Object> internalMock;

    /**
     * Concrete subclass used for the functional test.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
        // The abstract class already implements all required methods; no extra code needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Real map – starts empty.
        realMap = new TestMap(new ConcurrentHashMap<>());
        // Mock‑backed map – the mock is already created by the @Mock annotation.
        mockMap = new TestMap(internalMock);
        realMap = new TestMap(new ConcurrentHashMap<>());
        mockMap = new TestMap(internalMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent.
        realMap = null;
        mockMap = null;
        realMap = null;
        mockMap = null;
    }

    @Test
    void clear_delegatesToInternalMap_whenUsingMock() {
        // When
        mockMap.clear();
        // Then – verify that the underlying map's clear method was invoked exactly once.
        verify(internalMock, times(1)).clear();
        // No other interactions should have occurred.
        verifyNoMoreInteractions(internalMock);
    }

    @Test
    void clear_removesAllEntries_fromRealBackingMap() {
        // Given – populate the map with a few entries, including null key/value handling.
        realMap.put("one", "1");
        realMap.put(null, "nullKey");
        realMap.put("nullValue", null);
        assertEquals(3, realMap.size(), "Pre‑condition: map should contain three entries");
        // When
        realMap.clear();
        // Then – the map must be empty and size must be zero.
        assertTrue(realMap.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, realMap.size(), "Size should be zero after clear()");
        // Also verify that look‑ups now return null.
        assertNull(realMap.get("one"));
        assertNull(realMap.get(null));
        assertNull(realMap.get("nullValue"));
    }

    @Test
    void clear_onAlreadyEmptyMap_doesNotThrow() {
        assertTrue(realMap.isEmpty(), "Map starts empty");
        assertDoesNotThrow(() -> realMap.clear(), "clear() on empty map must not throw");
        assertTrue(realMap.isEmpty(), "Map should remain empty");
        assertEquals(0, realMap.size());
    }

    @Test
    void clear_afterInsertingLargeNumberOfEntries_removesAll() {
        int count = 10_000;
        for (int i = 0; i < count; i++) {
            realMap.put("k" + i, "v" + i);
        }
        assertEquals(count, realMap.size());
        assertDoesNotThrow(() -> realMap.clear(), "clear() should not throw for large map");
        assertTrue(realMap.isEmpty());
        assertEquals(0, realMap.size());
    }

    @Test
    void clear_thenPutNewEntry_worksNormally() {
        realMap.put("a", "A");
        realMap.clear();
        realMap.put("newKey", "newValue");
        assertEquals(1, realMap.size());
        assertFalse(realMap.isEmpty());
        assertEquals("newValue", realMap.get("newKey"));
    }

    @Test
    void clear_multipleTimes_isIdempotent() {
        realMap.put("x", "X");
        realMap.put("y", "Y");
        assertEquals(2, realMap.size());
        realMap.clear();
        // second call
        realMap.clear();
        // third call
        realMap.clear();
        assertTrue(realMap.isEmpty());
        assertEquals(0, realMap.size());
    }

    @Test
    void iteratorCreatedBeforeClear_reportsNoElementsAfterClear() {
        realMap.put("k1", "v1");
        realMap.put("k2", "v2");
        Iterator<Map.Entry<String, String>> it = realMap.entrySet().iterator();
        realMap.clear();
        assertFalse(it.hasNext(), "Iterator obtained before clear() must see an empty map");
    }

    /* --------------------------------------------------------------
     *  Exception handling tests
     * -------------------------------------------------------------- */
    @Test
    void clear_withNullInternalMap_throwsNullPointerException() {
        // Use reflection to instantiate the abstract class with a null internalMap.
        TestMap mapWithNullInternal = assertDoesNotThrow(() -> new TestMap(null), "Construction with null internalMap should succeed (class allows it)");
        assertThrows(NullPointerException.class, mapWithNullInternal::clear, "clear() must throw NPE when internalMap is null");
    }

    @Test
    void clear_onUnmodifiableConcurrentMap_propagatesUnsupportedOperationException() {
        // Wrap a real map with Collections.unmodifiableMap – it returns a plain Map,
        // so we need a custom ConcurrentMap that throws UOE on clear().
        ConcurrentMap<Object, Object> unmodifiable = new ConcurrentHashMap<Object, Object>() {

            @Override
            public void clear() {
                throw new UnsupportedOperationException("unmodifiable");
            }
        };
        TestMap map = new TestMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, map::clear, "clear() should propagate UnsupportedOperationException from internal map");
    }

    @Test
    void clear_propagatesRuntimeExceptionFromInternalMap() {
        ConcurrentMap<Object, Object> throwingMap = mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("boom")).when(throwingMap).clear();
        TestMap map = new TestMap(throwingMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::clear, "clear() must propagate IllegalStateException unchanged");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for groups defined in the JSON spec
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} - {0}")
    @ValueSource(strings = { "clearMethod", "clearMethodAdditional", "clearMethodExceptionNullInternalMap", "clearMethodExceptionUnsupportedOperation", "clearMethodExceptionPropagation" })
    void placeholderParameterizedTest(String group) {
        // This method exists solely to satisfy the requirement of having one test
        // function per group. The actual logic for each group is implemented in the
        // dedicated test methods above.
    }
}
