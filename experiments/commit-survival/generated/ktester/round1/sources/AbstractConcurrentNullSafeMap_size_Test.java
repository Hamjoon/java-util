package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_size_Test {

    /**
     * Mocked internal map that backs the AbstractConcurrentNullSafeMap instance.
     */
    @Mock
    private ConcurrentMap<Object, Object> mockInternalMap;

    /**
     * Concrete instance of the abstract class under test.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapUnderTest;

    /**
     * Instance used for the functional tests.
     */
    private RealNullSafeMap realMap;

    /**
     * Concrete subclass that delegates to a real {@link ConcurrentHashMap}.
     */
    private static class RealNullSafeMap extends AbstractConcurrentNullSafeMap<String, String> {

        RealNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete subclass that deliberately passes a null internal map.
     */
    private static class NullInternalMap extends AbstractConcurrentNullSafeMap<String, String> {

        NullInternalMap() {
            // internalMap will be null
            super(null);
        }
    }

    /**
     * A {@link ConcurrentMap} implementation whose {@code size()} throws {@link UnsupportedOperationException}.
     */
    private static class UnsupportedSizeMap implements ConcurrentMap<Object, Object> {

        private final ConcurrentMap<Object, Object> delegate = new ConcurrentHashMap<>();

        @Override
        public int size() {
            throw new UnsupportedOperationException("size() not supported");
        }

        // All other methods delegate to the underlying map.
        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return delegate.get(key);
        }

        @Override
        public Object put(Object key, Object value) {
            return delegate.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<?, ?> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public Object putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            return delegate.remove(key, value);
        }

        @Override
        public boolean replace(Object key, Object oldValue, Object newValue) {
            return delegate.replace(key, oldValue, newValue);
        }

        @Override
        public Object replace(Object key, Object value) {
            return delegate.replace(key, value);
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
            return null;
        }

        @Override
        public V computeIfPresent(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        @Override
        public V compute(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return null;
        }

        @Override
        public V merge(K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            return null;
        }
    }

    /**
     * Concrete subclass that uses the {@link UnsupportedSizeMap}.
     */
    private static class MapWithUnsupportedSize extends AbstractConcurrentNullSafeMap<String, String> {

        MapWithUnsupportedSize() {
            super(new UnsupportedSizeMap());
        }
    }

    /**
     * Initializes a fresh instance and mock before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are already initialised by the MockitoExtension,
        // but we also reset them to guarantee a clean state.
        reset(mockInternalMap);
        // Create a minimal concrete subclass using an anonymous class.
        mapUnderTest = new AbstractConcurrentNullSafeMap<String, String>(mockInternalMap) {
        };
        realMap = new RealNullSafeMap();
    }

    /**
     * No per‑test teardown is required, but the method is kept for symmetry.
     */
    @AfterEach
    void teardownAfterEach() {
        // Ensure no unexpected interactions remain.
        verifyNoMoreInteractions(mockInternalMap);
        verifyNoMoreInteractions(mockInternalMap);
    }

    /**
     * Verifies that {@code size()} simply forwards the call to the internal map.
     */
    @Test
    void size_DelegatesToInternalMap() {
        // GIVEN
        when(mockInternalMap.size()).thenReturn(42);
        // WHEN
        int result = mapUnderTest.size();
        // THEN
        assertEquals(42, result, "size() should return the value supplied by internalMap.size()");
        verify(mockInternalMap, times(1)).size();
    }

    /**
     * Checks the behaviour when the underlying map is empty.
     */
    @Test
    void size_WhenEmpty_ReturnsZero() {
        // GIVEN
        when(mockInternalMap.size()).thenReturn(0);
        // WHEN
        int result = mapUnderTest.size();
        // THEN
        assertEquals(0, result, "size() should return 0 for an empty internal map");
        verify(mockInternalMap, times(1)).size();
    }

    /**
     * Checks the behaviour when the underlying map contains several entries.
     */
    @Test
    void size_WithMultipleEntries_ReturnsCorrectCount() {
        // GIVEN
        when(mockInternalMap.size()).thenReturn(5);
        // WHEN
        int result = mapUnderTest.size();
        // THEN
        assertEquals(5, result, "size() should correctly reflect the number of entries in the internal map");
        verify(mockInternalMap, times(1)).size();
    }

    @Test
    void size_NewlyCreatedMap_ReturnsZero() {
        assertEquals(0, realMap.size(), "A newly created map should report size 0.");
    }

    @Test
    void size_AfterSinglePut_ReturnsOne() {
        realMap.put("key1", "value1");
        assertEquals(1, realMap.size(), "After inserting a single (non‑null) key/value pair, size() must return 1.");
    }

    @Test
    void size_AfterThreePuts_ReturnsThree() {
        realMap.put("k1", "v1");
        realMap.put("k2", "v2");
        realMap.put("k3", "v3");
        assertEquals(3, realMap.size(), "After inserting three distinct entries, size() must return 3.");
    }

    @Test
    void size_AfterRemove_ReflectsRemainingEntries() {
        realMap.put("a", "1");
        realMap.put("b", "2");
        realMap.put("c", "3");
        realMap.remove("b");
        assertEquals(2, realMap.size(), "After inserting three entries and then removing one of them, size() must reflect the remaining two entries.");
    }

    @Test
    void size_AfterClear_ReturnsZero() {
        realMap.put("x", "x");
        realMap.put("y", "y");
        realMap.clear();
        assertEquals(0, realMap.size(), "After calling clear() on a map that previously contained entries, size() must return 0.");
    }

    @Test
    void size_AfterPutAll_ReturnsCount() {
        Map<String, String> toAdd = new HashMap<>();
        toAdd.put("a", "1");
        toAdd.put("b", "2");
        toAdd.put("c", "3");
        toAdd.put("d", "4");
        realMap.putAll(toAdd);
        assertEquals(4, realMap.size(), "After using putAll() to add a collection of four entries, size() must return 4.");
    }

    @Test
    void size_AfterConcurrentInsert_ReturnsTotal() throws InterruptedException {
        final int THREAD_INSERTS = 5;
        Thread t = new Thread(() -> {
            for (int i = 0; i < THREAD_INSERTS; i++) {
                realMap.put("k" + i, "v" + i);
            }
        });
        t.start();
        // wait for the thread to finish
        t.join();
        assertEquals(THREAD_INSERTS, realMap.size(), "When entries are added concurrently from another thread, size() after the thread finishes must equal the total number of inserted entries (5 in this case).");
    }

    @Test
    void size_NullKeyAndNullValue_CountAsNormalEntries() {
        // null key with a non‑null value
        realMap.put(null, "nullKeyValue");
        // non‑null key with a null value
        realMap.put("nullValueKey", null);
        // two regular entries
        realMap.put("k1", "v1");
        realMap.put("k2", "v2");
        assertEquals(4, realMap.size(), "When a null key and a null value are added (via the map’s null‑masking logic), they count as normal entries; after adding them together with two regular entries, size() must return 4.");
    }

    @Test
    void size_NullInternalMap_ThrowsNullPointerException() {
        AbstractConcurrentNullSafeMap<String, String> map = new NullInternalMap();
        assertThrows(NullPointerException.class, map::size, "Invoking size() when internalMap is null should throw NullPointerException.");
    }

    @Test
    void size_UnderlyingMapThrowsUnsupportedOperationException_Propagates() {
        AbstractConcurrentNullSafeMap<String, String> map = new MapWithUnsupportedSize();
        assertThrows(UnsupportedOperationException.class, map::size, "Calling size() on the wrapper map must propagate UnsupportedOperationException from the underlying map.");
    }
}
